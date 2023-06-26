package me.cometkaizo.origins.origin;

import me.cometkaizo.origins.Main;
import me.cometkaizo.origins.network.Packets;
import me.cometkaizo.origins.network.S2CSlimicianAction;
import me.cometkaizo.origins.network.S2CSlimicianSynchronize;
import me.cometkaizo.origins.origin.client.ClientSlimicianOriginType;
import me.cometkaizo.origins.property.SpeciesProperty;
import me.cometkaizo.origins.util.AttributeUtils;
import me.cometkaizo.origins.util.DataKey;
import me.cometkaizo.origins.util.DataManager;
import me.cometkaizo.origins.util.TimeTracker;
import net.minecraft.entity.EntitySize;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.ai.attributes.Attributes;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Items;
import net.minecraft.util.DamageSource;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderPlayerEvent;
import net.minecraftforge.common.ForgeMod;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.EntityEvent;
import net.minecraftforge.event.entity.living.LivingFallEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.Event;
import net.minecraftforge.fml.network.PacketDistributor;

import static net.minecraftforge.fml.DistExecutor.unsafeRunWhenOn;

public class SlimicianOriginType extends AbstractOriginType {

    public static final String SHRINK_COUNT_KEY = "shrink_count";
    public static final float SLIME_AGGRO_RANGE = 30;
    public static final DataKey<Integer> SHRINK_COUNT = DataKey.create(Integer.class);
    public static final DataKey<Boolean> BOUNCED_THIS_TICK = DataKey.create(Boolean.class);
    public static final DataKey<Boolean> SHOULD_UPDATE = DataKey.create(Boolean.class);
    public static final int MAX_SHRINK_COUNT = 2;
    public static final double SIZE_SHRINK_FACTOR = 0.5;

    public static final SpeciesProperty SLIME_SPECIES = new SpeciesProperty.Builder()
            .setMobSpecies(EntityType.SLIME)
            .setRallyRadius(SLIME_AGGRO_RANGE).build();
    public static final float BOUNCE_SUPPRESS_FALL_DAMAGE_AMP = 0.5F;


    public enum Timer implements TimeTracker.Timer {
        JUMPED(0.2 * 20),
        BOUNCED(0.15 * 20);
        private final int duration;
        Timer(double duration) {
            this.duration = (int) duration;
        }
        @Override
        public int getDuration() {
            return duration;
        }
    }

    public enum Action {
        BEFORE_DEATH,
        RESET_FALL_DISTANCE,
        RESPAWN
    }

    public SlimicianOriginType() {
        super("Slimician", Items.SLIME_BALL, type -> new Origin.Description(type,
                        new Origin.Description.Entry(type, "bounce"),
                        new Origin.Description.Entry(type, "shrink"),
                        new Origin.Description.Entry(type, "species")
                ),
                SLIME_SPECIES
        );
    }

    public boolean bounced(Origin origin) {
        return origin.getTypeData().get(BOUNCED_THIS_TICK);
    }

    public EntitySize modifySize(EntitySize size, Origin origin) {
        float sizeFactor = getSizeFactor(origin);
        return size.scale(sizeFactor);
    }

    public float modifyEyeHeight(float eyeHeight, Origin origin) {
        float sizeFactor = getSizeFactor(origin);
        return eyeHeight * sizeFactor;
    }

    public void onRenderPlayer(RenderPlayerEvent.Pre renderEvent, Origin origin) {
        unsafeRunWhenOn(Dist.CLIENT, () -> () -> ClientSlimicianOriginType.onRenderPlayer(renderEvent, origin));
    }

    public double modifyCameraDistance(double startingDistance, Origin origin) {
        float sizeFactor = getSizeFactor(origin);
        return startingDistance * sizeFactor;
    }

    @Override
    public void onFirstActivate(Origin origin) {
        super.onFirstActivate(origin);
        origin.getTypeData().registerSaved(SHRINK_COUNT, 0, new ResourceLocation(Main.MOD_ID, SHRINK_COUNT_KEY));
        origin.getTypeData().register(BOUNCED_THIS_TICK, false);
        origin.getTypeData().register(SHOULD_UPDATE, true);

        unsafeRunWhenOn(Dist.CLIENT, () -> () -> ClientSlimicianOriginType.onFirstActivate(origin));
    }

    @Override
    public void onActivate(Origin origin) {
        super.onActivate(origin);
        if (origin.isServerSide()) sendSyncShrinkCountPacket(origin);
        updateAttributes(origin);
    }

    private void sendSyncShrinkCountPacket(Origin origin) {
        PlayerEntity player = origin.getPlayer();
        Packets.CHANNEL.send(PacketDistributor.TRACKING_ENTITY_AND_SELF.with(() -> player), new S2CSlimicianSynchronize(player, getShrinkCount(origin)));
    }

    public void acceptSynchronization(Origin origin, int shrinkCount) {
        origin.getTypeData().set(SHRINK_COUNT, shrinkCount);
        origin.getPlayer().recalculateSize();
    }

    private void updateAttributes(Origin origin) {
        updateMaxHealth(origin);
        updateReach(origin);
    }

    private void updateReach(Origin origin) {
        AttributeUtils.setAttribute(origin.getPlayer(), ForgeMod.REACH_DISTANCE.get(), getReach(origin));
    }

    protected double getReach(Origin origin) {
        double defaultValue = ForgeMod.REACH_DISTANCE.get().getDefaultValue();
        switch (getShrinkCount(origin)) {
            case 0: return defaultValue;
            case 1: return defaultValue * 3.5/5;
            case 2:
            default: return defaultValue * 2/5;
        }
    }

    private void updateMaxHealth(Origin origin) {
        AttributeUtils.setAttribute(origin.getPlayer(), Attributes.MAX_HEALTH, getMaxHealth(origin));
    }

    protected double getMaxHealth(Origin origin) {
        switch (getShrinkCount(origin)) {
            case 0: return 20;
            case 1: return 14;
            case 2:
            default: return 8;
        }
    }

    @Override
    public void onDeactivate(Origin origin) {
        super.onDeactivate(origin);
        PlayerEntity player = origin.getPlayer();
        AttributeUtils.setAttribute(player, Attributes.MAX_HEALTH, Attributes.MAX_HEALTH.getDefaultValue());
        AttributeUtils.setAttribute(player, ForgeMod.REACH_DISTANCE.get(), ForgeMod.REACH_DISTANCE.get().getDefaultValue());
        AttributeUtils.setAttribute(player, ForgeMod.ENTITY_GRAVITY.get(), ForgeMod.ENTITY_GRAVITY.get().getDefaultValue());
    }

    @Override
    public void onEvent(Object event, Origin origin) {
        super.onEvent(event, origin);
        if (isCanceled(event)) return;
        if (event == Action.BEFORE_DEATH) {
            tryIncreaseShrinkCount(origin);
        } else if (event == Action.RESPAWN) {
            resetShrinkCount(origin);
        } else if (event == Action.RESET_FALL_DISTANCE) {
            origin.getPlayer().fallDistance = 0;
        } else if (event instanceof EntityEvent.Size) {
            onRecalcSize((EntityEvent.Size) event, origin);
        } else if (event instanceof LivingHurtEvent) {
            onLivingHurt((LivingHurtEvent) event, origin);
        }
    }

    @Override
    public void onPlayerSensitiveEvent(Object event, Origin origin) {
        super.onPlayerSensitiveEvent(event, origin);
        if (isCanceled(event)) return;
        if (event instanceof LivingFallEvent) {
            onPlayerFall((LivingFallEvent) event, origin);
        } else if (event instanceof LivingHurtEvent) {
            onPlayerHurt((LivingHurtEvent) event, origin);
        } else if (event instanceof PlayerEvent.PlayerRespawnEvent) {
            onPlayerRespawn(origin);
        } else if (event instanceof TickEvent.PlayerTickEvent) {
            //if (!origin.isServerSide()) Main.LOGGER.info("Player: " + origin.getPlayer());
            if (origin.getTypeData().get(SHOULD_UPDATE)) {
                if (origin.isServerSide()) sendSyncShrinkCountPacket(origin);
                origin.getPlayer().recalculateSize();
                origin.getTypeData().set(SHOULD_UPDATE, false);
            }
        }

        unsafeRunWhenOn(Dist.CLIENT, () -> () -> ClientSlimicianOriginType.onPlayerSensitiveEvent(event, origin));
    }

    private static boolean isCanceled(Object event) {
        return event instanceof Event && ((Event)event).isCanceled();
    }

    private void onPlayerFall(LivingFallEvent event, Origin origin) {
        PlayerEntity player = origin.getPlayer();

        if (player.isSuppressingBounce()) {
            event.setDamageMultiplier(event.getDamageMultiplier() * BOUNCE_SUPPRESS_FALL_DAMAGE_AMP);
        } else {
            event.setCanceled(true);
        }
    }

    private void onPlayerRespawn(Origin origin) {
        resetShrinkCount(origin);
        if (origin.isServerSide()) sendRespawnPacket(origin.getPlayer());
    }

    private static void sendRespawnPacket(PlayerEntity player) {
        Packets.CHANNEL.send(PacketDistributor.TRACKING_ENTITY_AND_SELF.with(() -> player), new S2CSlimicianAction(player, Action.RESPAWN));
    }

    private void onPlayerHurt(LivingHurtEvent event, Origin origin) {
        PlayerEntity player = origin.getPlayer();

        if (event.getSource() == DamageSource.FALL && !player.isSuppressingBounce()) {
            event.setCanceled(true);
        }

        if (!origin.isServerSide()) return;

        if (player.getHealth() - event.getAmount() <= 0) {
            // disabled until finished
            beforeDeath(event, origin);
            sendBeforeDeathPacket(player);
        }
    }

    private static void sendBeforeDeathPacket(PlayerEntity player) {
        Packets.CHANNEL.send(PacketDistributor.TRACKING_ENTITY_AND_SELF.with(() -> player), new S2CSlimicianAction(player, Action.BEFORE_DEATH));
    }

    private void beforeDeath(LivingHurtEvent event, Origin origin) {
        PlayerEntity player = origin.getPlayer();

        boolean shrunk = tryIncreaseShrinkCount(origin);
        if (shrunk) {
            event.setAmount(0);
            player.setHealth(player.getMaxHealth());
        }
        player.recalculateSize();
    }

    private boolean tryIncreaseShrinkCount(Origin origin) {
        DataManager dataManager = origin.getTypeData();
        if (dataManager.get(SHRINK_COUNT) < MAX_SHRINK_COUNT) {
            dataManager.increase(SHRINK_COUNT, 1);
            updateAttributes(origin);
            origin.getPlayer().recalculateSize();
            origin.getTypeData().set(SHOULD_UPDATE, true);
            return true;
        } else return false;
    }

    public static Integer getShrinkCount(Origin origin) {
        return origin.getTypeData().get(SHRINK_COUNT);
    }

    private void resetShrinkCount(Origin origin) {
        origin.getTypeData().set(SHRINK_COUNT, 0);
        updateAttributes(origin);
        origin.getPlayer().recalculateSize();
    }

    private void onRecalcSize(EntityEvent.Size event, Origin origin) {
        if (!origin.getPlayer().equals(event.getEntity())) return;
        event.setNewEyeHeight(event.getNewSize().height * 0.85F);
    }

    private void onLivingHurt(LivingHurtEvent event, Origin origin) {
        if (origin.getPlayer().equals(event.getSource().getTrueSource())) {
            onLivingHurtByPlayer(event, origin);
        }
    }

    private void onLivingHurtByPlayer(LivingHurtEvent event, Origin origin) {
        float sizeFactor = getSizeFactor(origin);
        event.setAmount(event.getAmount() * sizeFactor);
    }

    private static float getSizeFactor(Origin origin) {
        int shrinkCount = getShrinkCount(origin);
        return (float) Math.pow(SIZE_SHRINK_FACTOR, shrinkCount);
    }
}
