package me.cometkaizo.origins.origin;

import me.cometkaizo.origins.Main;
import me.cometkaizo.origins.common.OriginDamageSources;
import me.cometkaizo.origins.origin.client.ClientPhoenixOriginType;
import me.cometkaizo.origins.util.ColorUtils;
import me.cometkaizo.origins.util.ParticleSpawner;
import me.cometkaizo.origins.util.SoundUtils;
import me.cometkaizo.origins.util.TimeTracker;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.particles.ParticleTypes;
import net.minecraft.potion.EffectInstance;
import net.minecraft.potion.Effects;
import net.minecraft.util.DamageSource;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.SoundEvents;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraft.world.server.ServerWorld;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.EntityViewRenderEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingAttackEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.living.LivingFallEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.event.entity.player.CriticalHitEvent;
import net.minecraftforge.eventbus.api.Event;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.awt.*;
import java.util.List;

import static net.minecraftforge.fml.DistExecutor.unsafeRunWhenOn;

@Mod.EventBusSubscriber(modid = Main.MOD_ID)
public class PhoenixOriginType extends AbstractOriginType {

    public static final int FIRE_POWER_TIME_MIN = 2;
    public static final int FIRE_POWER_TIME_MAX = 4;
    public static final int FIRE_POWER_RANGE = 4;
    private static final double DEATH_FIRE_RANGE = 8;
    public static final int WATER_DAMAGE = 2;
    public static final TimeTracker SKY_EFFECTS_TIME_TRACKER = new TimeTracker();
    public static final Color SKY_DEATH_COLOR = new Color(112, 14, 10);
    public static final double FLAP_AMPLIFIER = 0.7;
    public static final double BOOST_AMPLIFIER = 0.4;
    public static final double BOOST_OLD_MOVEMENT_REDUCTION = 0.4;
    public static final float BOOST_EXHAUSTION = 0.08F;
    public static final float FIRE_TICK_FLY_SPEED_AMP = 0.022F;
    public static final ParticleSpawner FIRE_POWER_PARTICLE_SPAWNER = new ParticleSpawner()
            .withParticles(ParticleTypes.SMOKE, ParticleTypes.FLAME)
            .withRandomCount(10, 20)
            .withRandomDirection(-3, 0.3, -3, 3, 0.5, 3)
            .withRandomSpeed(0.1, 0.2);
    public static final ParticleSpawner DEATH_PARTICLE_SPAWNER = new ParticleSpawner()
            .withParticles(ParticleTypes.SMOKE, ParticleTypes.CAMPFIRE_SIGNAL_SMOKE, ParticleTypes.LARGE_SMOKE)
            .withRange(5, 2, 5)
            .withRandomCount(40, 50)
            .withDirection(0, -1, 0)
            .withRandomSpeed(0.3, 0.5);
    public static final ParticleSpawner DEATH_ATMOSPHERE_PARTICLE_SPAWNER = new ParticleSpawner()
            .withParticles(ParticleTypes.ASH, ParticleTypes.WHITE_ASH)
            .withRange(5, 5, 5)
            .withRandomCount(20, 30)
            .withRandomDirection()
            .withRandomSpeed(0.05, 0.1);

    public static final Logger LOGGER = LogManager.getLogger();



    @SubscribeEvent
    public static void onTick(TickEvent.RenderTickEvent event) {
        if (event.phase == TickEvent.Phase.START) return;
        SKY_EFFECTS_TIME_TRACKER.tick();
    }


    public enum Property {
        RESPAWN_AT_DEATH
    }

    public enum Cooldown implements TimeTracker.Timer {
        FIRE_POWER(30 * 20),
        WATER_DAMAGE(1.5 * 20),
        UP_BOOST(0.75 * 20),
        FORWARD_BOOST(7),
        SKY_EFFECT(5 * 20);
        public final int duration;

        Cooldown(double duration) {
            this.duration = (int) duration;
        }

        @Override
        public int getDuration() {
            return duration;
        }
    }

    @Override
    public boolean hasMixinProperty(Object property, Origin origin) {
        return property == Property.RESPAWN_AT_DEATH || property == ElytrianOriginType.Property.PERMANENT_WINGS;
    }

    @Override
    public void performAction(Origin origin) {
        TimeTracker cooldownTracker = origin.getTimeTracker();

        if (cooldownTracker.hasTimer(Cooldown.FIRE_POWER)) return;

        PlayerEntity player = origin.getPlayer();
        setNearbyEntitiesOnFire(player, FIRE_POWER_RANGE);

        if (player.world instanceof ServerWorld)
            FIRE_POWER_PARTICLE_SPAWNER.spawnAt((ServerWorld) player.world, player);
        cooldownTracker.addTimer(Cooldown.FIRE_POWER);
    }

    private void setNearbyEntitiesOnFire(PlayerEntity player, double range) {
        AxisAlignedBB fireAffectedArea = player.getBoundingBox().grow(range);
        List<LivingEntity> affectedEntities = player.world.getEntitiesWithinAABB(LivingEntity.class, fireAffectedArea);

        for (LivingEntity entity : affectedEntities) {
            int firePowerTime = getFirePowerTime(player);

            if (player.equals(entity)) firePowerTime *= 4;

            entity.setFire(firePowerTime);
        }

        SoundUtils.playSound(player, SoundEvents.ITEM_FIRECHARGE_USE, SoundCategory.PLAYERS);
    }

    @Override
    public void onEvent(Object event, Origin origin) {
        if (event instanceof CriticalHitEvent) {
            onHit((CriticalHitEvent) event, origin);
        } else if (event instanceof LivingAttackEvent) {
            onLivingAttacked((LivingAttackEvent) event, origin);
        } else if (event instanceof LivingHurtEvent) {
            onLivingHurt((LivingHurtEvent) event, origin);
        } else if (event instanceof TickEvent.PlayerTickEvent) {
            onPlayerTick((TickEvent.PlayerTickEvent) event, origin);
        } else if (event instanceof LivingDeathEvent) {
            onLivingDeath((LivingDeathEvent) event, origin);
        } else if (event instanceof EntityViewRenderEvent.FogColors) {
            onFogColor((EntityViewRenderEvent.FogColors) event);
        }
    }

    @Override
    public void onPlayerSensitiveEvent(Object event, Origin origin) {
        super.onPlayerSensitiveEvent(event, origin);
        if (event instanceof Event && ((Event) event).isCanceled()) return;
        if (event instanceof LivingFallEvent) {
            onLivingLand(origin);
        } else if (event == ElytrianOriginType.Action.UP_BOOST) {
            boostUp(origin);
        } else if (event == ElytrianOriginType.Action.FORWARD_BOOST) {
            boostForward(origin);
        }

        unsafeRunWhenOn(Dist.CLIENT, () -> () -> ClientPhoenixOriginType.onPlayerSensitiveEvent(event, origin));
    }

    private void onLivingLand(Origin origin) {
        origin.getTimeTracker().remove(Cooldown.UP_BOOST);
    }

    public static void boostUp(Origin origin) {
        PlayerEntity player = origin.getPlayer();
        float flapAmount = (-player.rotationPitch + 90) / 180;
        double yMotion = FLAP_AMPLIFIER * flapAmount;

        player.setMotion(player.getMotion().add(0, yMotion, 0));
        SoundUtils.playSound(player, SoundEvents.ENTITY_PHANTOM_FLAP, SoundCategory.PLAYERS, 0.4F, 1);

        origin.getTimeTracker().addTimer(PhoenixOriginType.Cooldown.UP_BOOST);
        player.addExhaustion(BOOST_EXHAUSTION);
    }

    public static void boostForward(Origin origin) {
        PlayerEntity player = origin.getPlayer();
        Vector3d boostAmount = player.getLookVec();
        float fireSpeedBoost = Math.max(origin.getPlayer().getFireTimer(), 0) * FIRE_TICK_FLY_SPEED_AMP + 1;
        Vector3d boost = boostAmount.scale(BOOST_AMPLIFIER).scale(fireSpeedBoost);
        Vector3d oldMotion = player.getMotion().scale(BOOST_OLD_MOVEMENT_REDUCTION);

        player.setMotion(oldMotion.add(boost));
        SoundUtils.playSound(player, SoundEvents.ENTITY_PHANTOM_FLAP, SoundCategory.PLAYERS, 0.3F, 1);

        origin.getTimeTracker().addTimer(PhoenixOriginType.Cooldown.FORWARD_BOOST);
        player.addExhaustion(BOOST_EXHAUSTION);
    }

    public void onPlayerTick(TickEvent.PlayerTickEvent event, Origin origin) {
        if (event.isCanceled()) return;
        if (event.side.isClient()) return;
        if (!origin.getPlayer().equals(event.player)) return;
        PlayerEntity player = origin.getPlayer();
        TimeTracker cooldownTracker = origin.getTimeTracker();

        if (player.isInWaterOrBubbleColumn()) {
            if (!cooldownTracker.hasTimer(Cooldown.WATER_DAMAGE)) {
                applyWaterDamage(player);
                cooldownTracker.addTimer(Cooldown.WATER_DAMAGE);
            }
        } else {
            cooldownTracker.remove(Cooldown.WATER_DAMAGE);
        }
    }

    private static void applyWaterDamage(PlayerEntity player) {
        boolean damaged = player.attackEntityFrom(OriginDamageSources.TOUCH_WATER, WATER_DAMAGE);
        if (damaged) SoundUtils.playSound(player, SoundEvents.ENTITY_PLAYER_HURT_ON_FIRE, SoundCategory.PLAYERS, 0.7F, 1);
    }

    private void onLivingAttacked(LivingAttackEvent event, Origin origin) {
        if (event.isCanceled()) return;
        if (!origin.getPlayer().equals(event.getEntity())) return;
        DamageSource damageSource = event.getSource();

        if (isImmuneTo(damageSource)) {
            event.setCanceled(true);
        }
    }

    protected boolean isImmuneTo(DamageSource damageSource) {
        return damageSource == DamageSource.HOT_FLOOR ||
                damageSource == DamageSource.ON_FIRE ||
                damageSource == DamageSource.IN_FIRE;
    }

    private void onLivingHurt(LivingHurtEvent event, Origin origin) {
        if (event.isCanceled()) return;
        if (!origin.isServerSide()) return;
        if (!origin.getPlayer().equals(event.getEntity())) return;
        DamageSource damageSource = event.getSource();

        if (damageSource.isFireDamage()) {
            event.setAmount(event.getAmount() * 0.5F);
        } else if (damageSource == DamageSource.DROWN) {
            event.setAmount(event.getAmount() * 2F);
        }
    }

    private void onHit(CriticalHitEvent event, Origin origin) {
        if (event.isCanceled()) return;
        if (!origin.isServerSide()) return;
        if (!origin.getPlayer().equals(event.getEntity())) return;
        PlayerEntity player = origin.getPlayer();

        if (event.getDamageModifier() > 1) {
            event.getTarget().setFire(getFirePowerTime(player));
        }
    }

    protected int getFirePowerTime(PlayerEntity player) {
        EffectInstance strengthEffect = player.getActivePotionEffect(Effects.STRENGTH);
        return strengthEffect != null ?
                randomInt((strengthEffect.getAmplifier() + 2) * FIRE_POWER_TIME_MIN, (strengthEffect.getAmplifier() + 2) * FIRE_POWER_TIME_MAX) :
                randomInt(FIRE_POWER_TIME_MIN, FIRE_POWER_TIME_MAX);
    }

    private void onLivingDeath(LivingDeathEvent event, Origin origin) {
        if (event.isCanceled()) return;
        if (!origin.isServerSide()) return;
        if (!origin.getPlayer().equals(event.getEntity())) return;
        PlayerEntity player = origin.getPlayer();

        if (player.world instanceof ServerWorld) {
            DEATH_PARTICLE_SPAWNER.spawnAt((ServerWorld) player.world, player.getPositionVec().add(0, 10, 0));
            DEATH_ATMOSPHERE_PARTICLE_SPAWNER.spawnAt((ServerWorld) player.world, player.getPositionVec().add(0, 5, 0));
        }

        SKY_EFFECTS_TIME_TRACKER.addTimer(Cooldown.SKY_EFFECT);
        setNearbyEntitiesOnFire(player, DEATH_FIRE_RANGE);
        SoundUtils.playSound(player, SoundEvents.ENTITY_LIGHTNING_BOLT_THUNDER, SoundCategory.WEATHER);
    }

    private void onFogColor(EntityViewRenderEvent.FogColors event) {
        if (event.isCanceled()) return;

        float deathTime = SKY_EFFECTS_TIME_TRACKER.getTimerPercentage(Cooldown.SKY_EFFECT);
        float effectDuration = SKY_EFFECTS_TIME_TRACKER.getTimerDuration(PhoenixOriginType.Cooldown.SKY_EFFECT);
        float partialTicks = (float) event.getRenderPartialTicks();
        float partialTicksPercentage = 1 / effectDuration * partialTicks;

        event.setRed(ColorUtils.fadeInto(SKY_DEATH_COLOR.getRed(), event.getRed(), deathTime + partialTicksPercentage));
        event.setGreen(ColorUtils.fadeInto(SKY_DEATH_COLOR.getGreen(), event.getGreen(), deathTime + partialTicksPercentage));
        event.setBlue(ColorUtils.fadeInto(SKY_DEATH_COLOR.getBlue(), event.getBlue(), deathTime + partialTicksPercentage));
    }
}
