package me.cometkaizo.origins.origin;

import me.cometkaizo.origins.Main;
import me.cometkaizo.origins.network.C2SAcknowledgeSyncOrigin;
import me.cometkaizo.origins.network.PacketUtils;
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

import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

@Mod.EventBusSubscriber(modid = Main.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class Origin implements INBTSerializable<INBT> {
    private static final Logger LOGGER = LogManager.getLogger();
    public static final int SYNC_COOLDOWN = 20;
    private OriginType type;
    private PlayerEntity player;
    private boolean isServerSide;
    /**
     * Whether the {@code player} is referring to the player instance on the physical client
     */
    private boolean isPhysicalClient;

    protected static final DataKey<TimeTracker> TIME_TRACKER = DataKey.create(TimeTracker.class);

    private final DataManager originData = new DataManager();
    private final Map<OriginType, DataManager> typeSpecificData = new HashMap<>(1);
    private final Set<OriginType> seenTypes = new HashSet<>(1);
    private final AtomicBoolean shouldSync = new AtomicBoolean(false);
    private final AtomicInteger syncCooldown = new AtomicInteger(0);


    public static Origin getOrigin(Entity entity) {
        if (entity == null) return null;
        LazyOptional<Origin> originCap = entity.getCapability(CapabilityOrigin.ORIGIN_CAPABILITY);
        return originCap.isPresent() ? originCap.orElseThrow(IllegalStateException::new) : null;
    }

    public Origin(OriginType type, PlayerEntity player) {
        setType(type);
        setPlayer(player);

        MinecraftForge.EVENT_BUS.register(this);
        originData.register(TIME_TRACKER, new TimeTracker());
    }

    void unregister() {
        MinecraftForge.EVENT_BUS.unregister(this);
    }

    @SubscribeEvent
    public void onEvent(Event event) {
        if (isServerSide) {
            if (event instanceof TickEvent.ServerTickEvent) onTick((TickEvent.ServerTickEvent) event);
        } else {
            if (event instanceof TickEvent.ClientTickEvent) onTick((TickEvent.ClientTickEvent) event);
        }

        if (type != null) {
            tryOnFirstActivate();
            type.onEvent(event, this);
            if (isAboutPlayer(event)) {
                type.onPlayerSensitiveEvent(event, this);
            }
        }
    }

    public void onEvent(Object event) {
        if (event instanceof Event) onEvent((Event) event);
        else if (type != null) {
            tryOnFirstActivate();
            type.onEvent(event, this);
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

    public boolean hasProperty(Object property) {
        if (type != null) {
            tryOnFirstActivate();
            return type.hasMixinProperty(property, this);
        }
        return false;
    }

    public <T extends Property> List<T> getProperties(Class<T> propertyType) {
        if (type != null) {
            tryOnFirstActivate();
            return type.getProperties(propertyType);
        }
        return null;
    }

    private void tryOnFirstActivate() {
        if (!seenTypes.contains(type)) {
            synchronized (seenTypes) {
                if (!seenTypes.contains(type)) {
                    type.onFirstActivate(this);
                    seenTypes.add(type);
                }
            }
        }
    }

    public void performAction() {
        if (type != null) {
            tryOnFirstActivate();
            type.performAction(this);
        }
    }

    protected void onTick(TickEvent event) {
        if (event.isCanceled()) return;
        if (event.phase == TickEvent.Phase.END) return;
        getTimeTracker().tick();
        syncIfNecessary();
    }

    protected void syncIfNecessary() {
        if (shouldSync.get() && isServerSide && syncCooldown.decrementAndGet() <= 0) {
            forceSynchronize();
            syncCooldown.set(SYNC_COOLDOWN);
        }
    }

    public void forceSynchronize() {
        Main.LOGGER.info("Synchronizing Origins for {} to be {}...",
                player == null ? "null" : player.getGameProfile().getName(),
                type == null ? "null" : type.getName());
        PacketUtils.CHANNEL.send(PacketDistributor.TRACKING_ENTITY_AND_SELF.with(() -> player), new S2CSynchronizeOrigin(player, type));
    }

    public void setShouldSynchronize() {
        shouldSync.compareAndSet(false, true);
    }

    public void setSynchronized() {
        shouldSync.compareAndSet(true, false);
        syncCooldown.set(0);
    }

    public void acceptSynchronization(PlayerEntity player, OriginType type) {
        if (player == null) {
            LOGGER.error("Invalid synchronization packet: no player");
            return;
        } if (player instanceof ServerPlayerEntity) {
            LOGGER.error("Invalid synchronization packet: incorrect player type: {}", player);
            return;
        } if (type == null) {
            LOGGER.error("Invalid synchronization packet: no type");
            return;
        }
        setPlayer(player);
        setType(type);
        LOGGER.info("Accepted synchronization packet, player: {}, type: {}", player, type);
        PacketUtils.CHANNEL.send(PacketDistributor.SERVER.noArg(), new C2SAcknowledgeSyncOrigin());
    }


    public CompoundNBT serializeNBT() {
        CompoundNBT nbt = new CompoundNBT();
        nbt.putString("origin_type", String.valueOf(OriginTypes.getKey(type)));

        String uuid = player != null ? PlayerEntity.getUUID(player.getGameProfile()).toString() : "null";
        nbt.putString("origin_player", uuid);

        INBT dataManager = this.originData.serializeNBT();
        nbt.put("origin_data", dataManager);

        return nbt;
    }

    public void deserializeNBT(INBT nbt) {
        CompoundNBT compoundNBT = (CompoundNBT) nbt;

        deserializeType(compoundNBT.getString("origin_type"));

        deserializePlayer(compoundNBT.getString("origin_player"));

        deserializeData(compoundNBT.get("origin_data"));
    }

    private void deserializeData(INBT data) {
        if (data != null) this.originData.deserializeNBT(data);
        else LOGGER.warn("No data found");
    }

    private void deserializePlayer(String playerUUID) {
        if ("".equals(playerUUID)) LOGGER.warn("No player UUID found");
        else if (ServerLifecycleHooks.getCurrentServer().isDedicatedServer()) {
            PlayerList playerList = ServerLifecycleHooks.getCurrentServer().getPlayerList();

            ServerPlayerEntity player = playerList.getPlayerByUUID(UUID.fromString(playerUUID));
            if (player != null) setPlayer(player);
            else LOGGER.error("Could not find player with UUID '{}'", playerUUID);
            LOGGER.info("All players: {}, player count: {}, online: {}", playerList.getPlayers(), playerList.getCurrentPlayerCount(), playerList.getOnlinePlayerNames());
        } else {
            PlayerList playerList = ServerLifecycleHooks.getCurrentServer().getPlayerList();
            LOGGER.info("All players: {}, player count: {}, online: {}", playerList.getPlayers(), playerList.getCurrentPlayerCount(), playerList.getOnlinePlayerNames());
        }
    }

    private void deserializeType(String typeNamespace) {
        type = OriginTypes.of(typeNamespace);
        if (type == null) {
            LOGGER.warn("Could not find origin type '{}'; defaulting to HUMAN", typeNamespace);
            type = OriginTypes.HUMAN.get();
        }
        tryOnFirstActivate();
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

    public boolean isPhysicalClient() {
        return isPhysicalClient;
    }

    public DataManager getDataManager() {
        return originData;
    }
    public DataManager getTypeDataManager(OriginType type) {
        return typeSpecificData.computeIfAbsent(type, o -> new DataManager());
    }
    public DataManager getTypeDataManager() {
        return getTypeDataManager(type);
    }

    public TimeTracker getTimeTracker() {
        return originData.get(TIME_TRACKER);
    }

    public void setType(OriginType type) {
        Objects.requireNonNull(type, "Type cannot be null");
        if (this.type == type) return;
        if (this.type != null) this.type.onDeactivate(this);
        this.type = type;
        tryOnFirstActivate();
        this.type.onActivate(this);
    }

    private void setPlayer(PlayerEntity player) {
        Objects.requireNonNull(player, "Player cannot be null");
        this.player = player;
        this.isServerSide = player instanceof ServerPlayerEntity;
        this.isPhysicalClient = false;
        Boolean isPhysicalClient = DistExecutor.unsafeCallWhenOn(Dist.CLIENT, () -> () -> ClientOrigin.isPhysicalClient(this.player));
        if (isPhysicalClient != null) this.isPhysicalClient = isPhysicalClient;
    }
}
