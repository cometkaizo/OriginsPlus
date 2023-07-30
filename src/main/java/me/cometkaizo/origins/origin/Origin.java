package me.cometkaizo.origins.origin;

import me.cometkaizo.origins.Main;
import me.cometkaizo.origins.client.ClientUtils;
import me.cometkaizo.origins.network.C2SAcknowledgeSyncOrigin;
import me.cometkaizo.origins.network.Packets;
import me.cometkaizo.origins.network.S2COpenOriginScreen;
import me.cometkaizo.origins.network.S2CSynchronizeOrigin;
import me.cometkaizo.origins.origin.client.ClientOrigin;
import me.cometkaizo.origins.property.Property;
import me.cometkaizo.origins.util.DataKey;
import me.cometkaizo.origins.util.DataManager;
import me.cometkaizo.origins.util.TimeTracker;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.nbt.INBT;
import net.minecraft.server.management.PlayerList;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.TextComponent;
import net.minecraft.util.text.TranslationTextComponent;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.util.INBTSerializable;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.EntityEvent;
import net.minecraftforge.eventbus.api.Event;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.network.PacketDistributor;
import net.minecraftforge.fml.server.ServerLifecycleHooks;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.annotation.Nonnull;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

@Mod.EventBusSubscriber(modid = Main.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class Origin implements INBTSerializable<INBT> {
    private static final Logger LOGGER = LogManager.getLogger();

    protected static final DataKey<TimeTracker> TIME_TRACKER = DataKey.create(TimeTracker.class);
    protected static final DataKey<Boolean> HAS_CHOSEN_TYPE = DataKey.create(Boolean.class);
    private static final DataKey<Boolean> SHOULD_OPEN_ORIGIN_SCREEN = DataKey.create(Boolean.class);
    public static final int SYNC_COOLDOWN = 20;
    private OriginType type;
    private PlayerEntity player;
    private boolean isServerSide;
    /**
     * Whether the {@code player} is referring to the player instance on the physical client
     */
    private boolean isPhysicalClient;
    private boolean isRemoved;

    private DataManager data;
    private final Map<OriginType, DataManager> typeSpecificData = new HashMap<>(1);
    private final Set<OriginType> seenTypes = new HashSet<>(1);
    private final AtomicBoolean shouldSync = new AtomicBoolean(false);
    private final AtomicInteger syncCooldown = new AtomicInteger(0);


    public static Origin getOrigin(Entity entity) {
        if (entity == null) return null;
        LazyOptional<Origin> originCap = entity.getCapability(CapabilityOrigin.ORIGIN_CAPABILITY);
        return originCap.isPresent() ? originCap.orElseThrow(AssertionError::new) : null;
    }

    public static boolean hasLabel(Entity entity, Object label) {
        Origin origin = getOrigin(entity);
        return origin != null && origin.hasLabel(label);
    }

    public Origin(OriginType type, PlayerEntity player) {
        setType(type);
        Main.LOGGER.info("Setting new origin player to {} because this is the constructor", player);
        setPlayer(player);

        MinecraftForge.EVENT_BUS.register(this);
        data = newDefaultDataManager();
    }

    /**
     * Transfers origin data from one player to another, then removes the original.
     * Does not transfer player related data.
     * @param other the origin to transfer data from
     * @see Origin#setPlayer(PlayerEntity)
     */
    synchronized void transferDataFrom(Origin other) {
        if (other == null) return;
        this.isRemoved = other.isRemoved;
        this.seenTypes.clear();
        this.seenTypes.addAll(other.seenTypes);
        this.typeSpecificData.clear();
        this.typeSpecificData.putAll(other.typeSpecificData);
        this.data = other.data == null ? newDefaultDataManager() : other.data;
        setType(other.type);

        if (isRemoved) {
            LOGGER.info("Removing because copied from origin {} : {}", other.player, other.type);
            remove();
        } else MinecraftForge.EVENT_BUS.register(this);

        LOGGER.info("Removing origin {} : {} because it was transferred into origin {}", other.player, other.type, this);
        other.remove();
    }

    protected DataManager newDefaultDataManager() {
        DataManager data = new DataManager();
        data.register(TIME_TRACKER, new TimeTracker());
        if (isServerSide) data.registerSaved(HAS_CHOSEN_TYPE, false, new ResourceLocation(Main.MOD_ID, "has_chosen_type"));
        if (!isServerSide) data.register(SHOULD_OPEN_ORIGIN_SCREEN, false);
        return data;
    }

    public void remove() {
        LOGGER.info("Removing origin {} : {} ...", player, type);
        isRemoved = true;
        MinecraftForge.EVENT_BUS.unregister(this);
        shouldSync.compareAndSet(true, false);
        LOGGER.info("Removed origin: {} : {}", player, type);
    }

    public void revive() {
        LOGGER.info("Reviving origin {} : {} ...", player, type);
        isRemoved = false;
        MinecraftForge.EVENT_BUS.register(this);
        shouldSync.compareAndSet(false, true);
        LOGGER.info("Revived origin: {} : {}", player, type);
    }

    @SubscribeEvent
    public void onEvent(Event event) {
        if (isRemoved) {
            LOGGER.warn("Removed origin {} : {} is still subscribed to events; unregistering",
                    player == null ? "null" : player.getName().getString(),
                    type == null ? "null" : type.getName());
            MinecraftForge.EVENT_BUS.unregister(this);
            return;
        } else if (shouldNotReceiveEvents()) {
            remove();
            return;
        }

        if (isServerSide) {
            if (event instanceof TickEvent.ServerTickEvent) {
                onTick((TickEvent.ServerTickEvent) event);
            }
        } else if (event instanceof TickEvent.ClientTickEvent) {
            onTick((TickEvent.ClientTickEvent) event);
            removeIfMismatched();
        }

        if (type != null) {
            tryInitCurrentType();
            type.onEvent(event, this);
            if (isAboutPlayer(event)) type.onPlayerSensitiveEvent(event, this);
        }
    }

    protected boolean shouldNotReceiveEvents() {
        PlayerEntity localPlayer = DistExecutor.safeCallWhenOn(Dist.CLIENT, () -> ClientUtils::getClientPlayer);
        return !isServerSide &&
                player != null &&
                localPlayer != null &&
                player.getEntityId() == localPlayer.getEntityId() &&
                player != localPlayer;
    }

    private void removeIfMismatched() {
        Origin actualOrigin = getOrigin(player);
        if (actualOrigin != this/* || DistExecutor.safeCallWhenOn(Dist.CLIENT, () -> ClientUtils::getClientPlayer) != player*/) {
            LOGGER.info("Removing origin {} : {} because a mismatch was detected between this origin and the player's origin {} : {}",
                    player, type,
                    actualOrigin == null ? "{No origin}" : actualOrigin.player,
                    actualOrigin == null ? "{No origin}" : actualOrigin.type);
            remove();
        }
    }

    public void onEvent(Object event) {
        if (event instanceof Event) onEvent((Event) event);
        else if (type != null) {
            try {
                tryInitCurrentType();
                type.onEvent(event, this);
            } catch (Exception e) {
                LOGGER.error("An exception occurred while running Origin#onEvent(Object) in origin {} ", this);
                LOGGER.error("Caught exception: ", e);
            }
        }
    }

    private boolean isAboutPlayer(Event event) {
        return player != null && (
                event instanceof EntityEvent && player.equals(((EntityEvent) event).getEntity()) ||
                event instanceof TickEvent.ClientTickEvent && isPhysicalClient ||
                event instanceof TickEvent.RenderTickEvent && isPhysicalClient ||
                event instanceof TickEvent.PlayerTickEvent && player.equals(((TickEvent.PlayerTickEvent) event).player)
        );
    }

    protected void onTick(TickEvent event) {
        if (event.isCanceled()) return;
        if (event.phase == TickEvent.Phase.END) return;
        getTimeTracker().tick();
        syncIfNecessary();
    }

    public boolean hasLabel(Object label) {
        if (type != null) {
            tryInitCurrentType();
            return type.hasLabel(label, this);
        }
        return false;
    }

    public <T extends Property> List<T> getProperties(Class<T> propertyType) {
        if (type != null) {
            tryInitCurrentType();
            return type.getProperties(propertyType);
        }
        return null;
    }

    private void tryInitCurrentType() {
        if (type != null) {
            synchronized (seenTypes) {
                if (!seenTypes.contains(type)) {
                    type.init(this);
                    seenTypes.add(type);
                }
            }
        }
    }

    public void performAction() {
        if (type != null) {
            tryInitCurrentType();
            type.performAction(this);
        }
    }

    protected void syncIfNecessary() {
        if (shouldSync.get() && isServerSide && syncCooldown.decrementAndGet() <= 0) {
            trySynchronize();
            syncCooldown.set(SYNC_COOLDOWN);
        }
    }

    public void trySynchronize() {
        if (!isServerSide) {
            LOGGER.warn("Wrong side to synchronize origin {} : {}", player, type == null ? "null" : type.getName());
        }/* else if (isRemoved) {
            LOGGER.warn("Cannot synchronize removed origin {} : {}", player, type == null ? "null" : type.getName());
        }*/ else if (player == null) {
            LOGGER.warn("No player to synchronize in origin null : {}", type == null ? "null" : type.getName());
        } else if (!player.isAlive()) {
            LOGGER.warn("Cannot synchronize removed player in origin {} : {}", player, type == null ? "null" : type.getName());
        } else {
            LOGGER.info("Synchronizing origin {} : {} : {}...",
                    player.getGameProfile().getName(),
                    type == null ? "null" : type.getName(),
                    isRemoved);
            Packets.CHANNEL.send(PacketDistributor.ALL.noArg(), new S2CSynchronizeOrigin(player, type, getTypeData().serializeSynced()));
        }
    }

    public void setShouldSynchronize() {
        shouldSync.compareAndSet(false, true);
    }

    public void setSynchronized() {
        shouldSync.compareAndSet(true, false);
        syncCooldown.set(0);
    }

    public void acceptSynchronization(PlayerEntity player, OriginType type, CompoundNBT typeData) {
        if (player == null) {
            LOGGER.error("Invalid synchronization packet: no player; {}, {}", type, typeData);
            return;
        } if (player instanceof ServerPlayerEntity) {
            LOGGER.error("Invalid synchronization packet: incorrect player type: {}", player);
            return;
        } if (getOrigin(player) == null) {
            LOGGER.error("Invalid synchronization packet: player does not have origin capability: {}", player);
            return;
        } if (type == null) {
            LOGGER.error("Invalid synchronization packet: no type");
            return;
        } else if (typeData == null) {
            LOGGER.error("Invalid synchronization packet: no type data; {}, {}", player, type);
            return;
        }

        Main.LOGGER.info("Setting origin {} : {} player to {} because it is being synced", this.player, this.type, player);
        setPlayer(player);
        setType(type);
        getTypeData().deserializeSynced(typeData);
        if (this.type != null) this.type.acceptSynchronization(this);

        LOGGER.info("Accepted synchronization packet, player: {}, type: {}, type data: {}", this.player, this.type, typeData);
        Packets.CHANNEL.send(PacketDistributor.SERVER.noArg(), new C2SAcknowledgeSyncOrigin());
    }


    public CompoundNBT serializeNBT() {/*
        if (this.isRemoved) {
            LOGGER.error("Cannot serialize data for removed origin {}", this);
            throw new IllegalStateException("Cannot serialize data for removed origin " + this);
        }
        CompoundNBT nbt = new CompoundNBT();
        nbt.putString("origin_type", String.valueOf(OriginTypes.getKey(this.type)));
        String uuid = (this.player != null) ? PlayerEntity.getUUID(this.player.getGameProfile()).toString() : "null";
        nbt.putString("origin_player", uuid);
        CompoundNBT compoundNBT1 = this.data.serializeNBT();
        nbt.put("origin_data", compoundNBT1);
        CompoundNBT typeSpecificData = new CompoundNBT();
        for (OriginType type : this.typeSpecificData.keySet()) {
            DataManager dM = this.typeSpecificData.get(type);
            ResourceLocation registryName = type.getRegistryName();
            if (registryName != null)
                typeSpecificData.put(registryName.toString(), dM.serializeNBT());
        }
        nbt.put("origin_type_specific_data", typeSpecificData);
        return nbt;*/
        // TODO: 2023-07-30 this should do the same thing but all the same... if you are having issues try reverting this
        CompoundNBT nbt = new CompoundNBT();

        if (isRemoved) {
            LOGGER.warn("Cannot serialize data for removed origin {}", this);
            return nbt;
        }
        try {
            LOGGER.info("Serializing origin {} : {}", player, type);

            serializeType(nbt);
            serializePlayer(nbt);
            serializeOriginData(nbt);
            serializeTypeSpecificData(nbt);

            LOGGER.info("Serialized origin {} : {} into {}", player, type, nbt);
            return nbt;
        } catch (Exception e) {
            LOGGER.error("Caught exception {}; Could not serialize origin {}", e, this);
            LOGGER.error("Exception: ", e);
            throw new RuntimeException("Could not serialize origin " + this, e);
        }
    }

    private void serializeType(CompoundNBT nbt) {
        LOGGER.info("Serializing type {} in origin {} : {}", type, player, type);
        ResourceLocation typeKey = OriginTypes.getKey(type);
        if (typeKey == null) LOGGER.warn("Serializing origin with unknown type {}: {}; registered types are: {}", type, this, OriginTypes.ORIGINS_REGISTRY.get().getValues());
        nbt.putString("origin_type", String.valueOf(typeKey));
    }

    private void serializePlayer(CompoundNBT nbt) {
        LOGGER.info("Serializing player {} in origin {} : {}", player, player, type);
        if (player == null) LOGGER.warn("Serializing origin with no player: {}", this);
        String uuid = player != null ? PlayerEntity.getUUID(player.getGameProfile()).toString() : "null";
        nbt.putString("origin_player", uuid);
    }

    private void serializeOriginData(CompoundNBT nbt) {
        LOGGER.info("Serializing data {} in origin {} : {}", data, player, type);
        INBT dataManager = this.data.serializeNBT();
        nbt.put("origin_data", dataManager);
    }

    private void serializeTypeSpecificData(CompoundNBT nbt) {
        LOGGER.info("Serializing type specific data {} in origin {} : {}", typeSpecificData, player, type);
        CompoundNBT typeSpecificData = new CompoundNBT();
        for (OriginType type : this.typeSpecificData.keySet()) {
            DataManager dM = this.typeSpecificData.get(type);
            ResourceLocation registryName = type.getRegistryName();
            if (registryName != null) {
                typeSpecificData.put(registryName.toString(), dM.serializeNBT());
            }
        }
        nbt.put("origin_type_specific_data", typeSpecificData);
    }

    public void deserializeNBT(INBT nbt) {
        if (!(nbt instanceof CompoundNBT)) throw new IllegalArgumentException("Illegal NBT: " + nbt);
        CompoundNBT compoundNBT = (CompoundNBT) nbt;

        try {
            LOGGER.info("Deserializing origin from {}", nbt);
            deserializePlayer(compoundNBT);
            deserializeType(compoundNBT);
            deserializeData(compoundNBT);
            deserializeTypeSpecificData(compoundNBT);
            LOGGER.info("Deserialized origin {}", this);
        } catch (Exception e) {
            LOGGER.error("Caught exception {}; Could not fully deserialize origin from {}", e, nbt);
            LOGGER.error("Exception: ", e);
            throw new RuntimeException("Could not fully deserialize origin from " + nbt, e);
        }
    }

    private void deserializePlayer(CompoundNBT nbt) {
        String playerUUID = nbt.getString("origin_player");
        LOGGER.info("Deserializing player {}", playerUUID);
        if ("".equals(playerUUID)) {
            LOGGER.warn("No player UUID found in {}", nbt);
        } else if (ServerLifecycleHooks.getCurrentServer().isDedicatedServer()) {
            PlayerList playerList = ServerLifecycleHooks.getCurrentServer().getPlayerList();

            ServerPlayerEntity player = getPlayerByUUID(playerUUID, playerList);
            if (player != null) {
                Main.LOGGER.info("Setting origin {} : {} player to {} because it is deserialized", this.player, this.type, player);
                setPlayer(player);
            }
            else LOGGER.error("Could not find player with UUID '{}'", playerUUID);
            LOGGER.info("All players: {}, player count: {}, online: {}", playerList.getPlayers(), playerList.getCurrentPlayerCount(), playerList.getOnlinePlayerNames());
        } else {
            PlayerList playerList = ServerLifecycleHooks.getCurrentServer().getPlayerList();
            LOGGER.info("All players: {}, player count: {}, online: {}", playerList.getPlayers(), playerList.getCurrentPlayerCount(), playerList.getOnlinePlayerNames());
        }
    }

    private ServerPlayerEntity getPlayerByUUID(String playerUUID, PlayerList playerList) {
        try {
            return playerList.getPlayerByUUID(UUID.fromString(playerUUID));
        } catch (Exception e) {
            LOGGER.error("Could not get player with UUID " + playerUUID, e);
            return null;
        }
    }

    private void deserializeType(CompoundNBT nbt) {
        String typeNamespace = nbt.getString("origin_type");
        LOGGER.info("Deserializing type {}", typeNamespace);
        OriginType type = OriginTypes.of(typeNamespace);
        if (type == null) {
            LOGGER.warn("Origin data {} contains unknown type '{}'; defaulting to HUMAN", nbt, typeNamespace);
            setType(OriginTypes.HUMAN.get());
        } else setType(type);
    }

    private void deserializeData(CompoundNBT nbt) {
        INBT data = nbt.get("origin_data");
        LOGGER.info("Deserializing data {}", data);
        if (this.data == null) this.data = newDefaultDataManager();
        if (data != null) this.data.deserializeNBT(data);
        else LOGGER.warn("No data found in {}", nbt);
    }

    private void deserializeTypeSpecificData(CompoundNBT nbt) {
        INBT dataNBT = nbt.get("origin_type_specific_data");
        LOGGER.info("Deserializing type specific data {}", dataNBT);
        if (dataNBT == null) return;
        CompoundNBT compound = (CompoundNBT) dataNBT;

        for (String key : compound.keySet()) {
            INBT data = compound.get(key);
            OriginType type = OriginTypes.of(key);
            if (type != null) {
                DataManager dataManager = typeSpecificData.putIfAbsent(type, new DataManager());
                if (dataManager != null) dataManager.deserializeNBT(data);
            } else LOGGER.warn("Origin data {} contains unknown type '{}'; registered types are: {}", nbt, key, OriginTypes.ORIGINS_REGISTRY.get().getValues());
        }
    }

    public OriginType getType() {
        return type;
    }

    public PlayerEntity getPlayer() {
        return player;
    }

    public boolean isServerSide() {
        return isServerSide;
    }
    public boolean isClientSide() {
        return !isServerSide;
    }

    public boolean isPhysicalClient() {
        return isPhysicalClient;
    }

    public DataManager getData() {
        return data;
    }
    public DataManager getTypeData(OriginType type) {
        return typeSpecificData.computeIfAbsent(type, o -> new DataManager());
    }
    public DataManager getTypeData() {
        return getTypeData(type);
    }

    public TimeTracker getTimeTracker() {
        return getData().get(TIME_TRACKER);
    }

    public boolean isRemoved() {
        return isRemoved;
    }

    public boolean hasChosenType() {
        return isServerSide() && getData().get(HAS_CHOSEN_TYPE);
    }

    public void setHasChosenType(boolean hasChosenType) {
        if (isServerSide()) getData().set(HAS_CHOSEN_TYPE, hasChosenType);
    }

    public void setShouldOpenOriginScreen() {
        if (!isServerSide()) getData().set(SHOULD_OPEN_ORIGIN_SCREEN, true);
        else {
            Packets.CHANNEL.send(PacketDistributor.TRACKING_ENTITY_AND_SELF.with(() -> player), new S2COpenOriginScreen(player));
        }
    }

    public void onOpenedOriginScreen() {
        if (!isServerSide()) getData().set(SHOULD_OPEN_ORIGIN_SCREEN, false);
    }

    public boolean shouldOpenOriginScreen() {
        return !isServerSide() && getData().get(SHOULD_OPEN_ORIGIN_SCREEN);
    }

    public void setType(OriginType type) {
        if (this.type == type) return;
        if (this.type != null) this.type.deactivate(this);
        this.type = type;
        tryInitCurrentType();
        if (type != null) type.activate(this);
    }

    protected void setPlayer(PlayerEntity player) {
        LOGGER.info("Player for origin {} : {} is being set to {}", this.player, type, player);

        this.player = player;
        this.isServerSide = player instanceof ServerPlayerEntity;
        this.isPhysicalClient = false;
        Boolean isPhysicalClient = DistExecutor.unsafeCallWhenOn(Dist.CLIENT, () -> () -> ClientOrigin.isPhysicalClient(this.player));
        if (isPhysicalClient != null) this.isPhysicalClient = isPhysicalClient;
    }

    public Description getDescription() {
        return type == null ? null : type.getDescription();
    }

    public void tempSetPlayer(PlayerEntity player) {
        Main.LOGGER.info("Setting origin {} : {} player to {} because it is being updated through tempSetPlayer", this.player, this.type, player);
        setPlayer(player);
    }

    public static class Description {
        public final TextComponent summary;
        public final List<Entry> entries;

        public Description(OriginType type, Entry... entries) {
            this(new TranslationTextComponent(Main.MOD_ID + "." + Objects.requireNonNull(type.getRegistryName()).getPath() + ".summary"), entries);
        }

        public Description(TextComponent summary, List<Entry> entries) {
            this.summary = summary;
            this.entries = entries;
        }

        public Description(TextComponent summary, Entry... entries) {
            this.summary = summary;
            this.entries = new ArrayList<>(Arrays.asList(entries));
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            if (!super.equals(o)) return false;
            Description that = (Description) o;
            return Objects.equals(entries, that.entries);
        }

        @Override
        public int hashCode() {
            return Objects.hash(super.hashCode(), entries);
        }

        @Nonnull
        @Override
        public String toString() {
            return "Description{" +
                    "entries=" + entries +
                    '}';
        }

        public static class Entry {
            public final TextComponent title;
            public final TextComponent description;

            public Entry(TextComponent title, TextComponent description) {
                this.title = title;
                this.description = description;
            }

            public Entry(OriginType type, String key) {
                Objects.requireNonNull(type, "Type cannot be null");
                String namespace = Objects.requireNonNull(type.getRegistryName(),
                                "Registry name is null for origin type '" + type + "' ('" + type.getName() + "'")
                        .getPath();
                this.title = new TranslationTextComponent(Main.MOD_ID + "." + namespace + "." + key + ".title");
                this.description = new TranslationTextComponent(Main.MOD_ID + "." + namespace + "." + key + ".desc");
            }

            @Override
            public boolean equals(Object o) {
                if (this == o) return true;
                if (o == null || getClass() != o.getClass()) return false;
                if (!super.equals(o)) return false;
                Entry entry = (Entry) o;
                return Objects.equals(title, entry.title) && Objects.equals(description, entry.description);
            }

            @Override
            public int hashCode() {
                return Objects.hash(super.hashCode(), title, description);
            }

            @Nonnull
            @Override
            public String toString() {
                return "Entry{" +
                        "title=" + title +
                        ", description=" + description +
                        '}';
            }
        }
    }

    @Override
    public String toString() {
        try {
            return "Origin{" +
                    "type=" + type +
                    ", player=" + player +
                    ", isServerSide=" + isServerSide +
                    ", isPhysicalClient=" + isPhysicalClient +
                    ", isRemoved=" + isRemoved +
                    ", data=" + data +
                    ", typeSpecificData=" + typeSpecificData +
                    ", seenTypes=" + seenTypes +
                    ", shouldSync=" + shouldSync +
                    ", syncCooldown=" + syncCooldown +
                    '}';
        } catch (Exception e) {
            LOGGER.error("Error in Origin#toString()");
            return "No Origin String representation";
        }
    }
}
