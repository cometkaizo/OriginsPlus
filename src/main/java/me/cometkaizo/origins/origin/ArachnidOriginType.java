package me.cometkaizo.origins.origin;

import me.cometkaizo.origins.origin.client.ClientArachnidOriginType;
import me.cometkaizo.origins.property.SpeciesProperty;
import me.cometkaizo.origins.util.AttributeUtils;
import me.cometkaizo.origins.util.TimeTracker;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.enchantment.Enchantments;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.ai.attributes.Attributes;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.potion.EffectInstance;
import net.minecraft.potion.Effects;
import net.minecraft.util.DamageSource;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.world.World;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.eventbus.api.Event;

import java.util.List;

import static net.minecraftforge.fml.DistExecutor.unsafeRunWhenOn;

public class ArachnidOriginType extends AbstractOriginType {

    public static final double MAX_HEALTH = 8 * 2;
    public static final float ARACHNID_AGGRO_RANGE = 10;
    public static final int PROJ_SLOW_BASE_DURATION = 2 * 20;
    public static final double PROJ_SLOW_DURATION_DISTANCE_AMP = 0.2;
    public static final int PROJ_SLOW_BASE_AMP = 0;
    public static final double PROJ_SLOW_AMP_DISTANCE_AMP = 0.003;
    public static final int AOE_SLOW_PLAYER_DURATION = 4 * 20;
    public static final int AOE_SLOW_PLAYER_AMP = 1;
    public static final int AOE_SLOW_MOB_DURATION = 8 * 20;
    public static final int AOE_SLOW_MOB_AMP = 2;
    public static final int AOE_SLOW_RANGE = 5;
    public static final int AOE_SLOW_RANGE_SQ = AOE_SLOW_RANGE * AOE_SLOW_RANGE;
    public static final float BANE_OF_ARTHROPODS_DAMAGE_LEVEL_AMP = 2.5F;

    public static final SpeciesProperty SPIDER_SPECIES = new SpeciesProperty.Builder()
            .setMobSpecies(EntityType.SPIDER)
            .setRallyRadius(ARACHNID_AGGRO_RANGE).build();

    public static final SpeciesProperty CAVE_SPIDER_SPECIES = new SpeciesProperty.Builder()
            .setMobSpecies(EntityType.CAVE_SPIDER)
            .setRallyRadius(ARACHNID_AGGRO_RANGE).build();


    protected ArachnidOriginType() {
        super("Arachnid",
                SPIDER_SPECIES,
                CAVE_SPIDER_SPECIES
        );
    }

    public enum Property {
        NO_COBWEB_SLOWDOWN
    }

    public enum Cooldown implements TimeTracker.Timer {
        AOE_SLOWNESS(30 * 20);

        private final int duration;
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
        return super.hasMixinProperty(property, origin) || property == Property.NO_COBWEB_SLOWDOWN;
    }

    @Override
    public void onFirstActivate(Origin origin) {
        super.onFirstActivate(origin);

        unsafeRunWhenOn(Dist.CLIENT, () -> () -> ClientArachnidOriginType.onFirstActivate(origin));
    }

    @Override
    public void onActivate(Origin origin) {
        super.onActivate(origin);
        PlayerEntity player = origin.getPlayer();
        AttributeUtils.setAttribute(player, Attributes.MAX_HEALTH, MAX_HEALTH);
    }

    @Override
    public void onDeactivate(Origin origin) {
        super.onDeactivate(origin);
        PlayerEntity player = origin.getPlayer();
        AttributeUtils.setAttribute(player, Attributes.MAX_HEALTH, Attributes.MAX_HEALTH.getDefaultValue());
    }

    @Override
    public void performAction(Origin origin) {
        super.performAction(origin);
        TimeTracker cooldowns = origin.getTimeTracker();

        if (cooldowns.hasTimer(Cooldown.AOE_SLOWNESS)) return;

        applyAOESlowness(origin);

        cooldowns.addTimer(Cooldown.AOE_SLOWNESS);
    }

    private void applyAOESlowness(Origin origin) {
        PlayerEntity player = origin.getPlayer();

        World world = player.world;

        AxisAlignedBB affectedArea = player.getBoundingBox().grow(AOE_SLOW_RANGE);
        List<LivingEntity> affectedEntities = world.getEntitiesWithinAABB(LivingEntity.class, affectedArea);

        for (LivingEntity entity : affectedEntities) {
            if (entity.getDistanceSq(player) > AOE_SLOW_RANGE_SQ) continue;
            applySlowness(entity);
        }
    }

    private static void applySlowness(LivingEntity entity) {
        if (entity instanceof PlayerEntity) {
            entity.addPotionEffect(new EffectInstance(Effects.SLOWNESS, AOE_SLOW_PLAYER_DURATION, AOE_SLOW_PLAYER_AMP));
        } else {
            entity.addPotionEffect(new EffectInstance(Effects.SLOWNESS, AOE_SLOW_MOB_DURATION, AOE_SLOW_MOB_AMP));
        }
    }

    @Override
    public void onPlayerSensitiveEvent(Object event, Origin origin) {
        super.onPlayerSensitiveEvent(event, origin);
        if (isCanceled(event)) return;

        if (event instanceof LivingHurtEvent) {
            onPlayerHurt((LivingHurtEvent) event, origin);
        }

        unsafeRunWhenOn(Dist.CLIENT, () -> () -> ClientArachnidOriginType.onPlayerSensitiveEvent(event, origin));
    }

    private void onPlayerHurt(LivingHurtEvent event, Origin origin) {
        if (!origin.isServerSide()) return;
        DamageSource damageSource = event.getSource();
        if (!(damageSource.getImmediateSource() instanceof LivingEntity)) return;

        LivingEntity damager = (LivingEntity) damageSource.getImmediateSource();
        ItemStack damagerItem = damager.getHeldItemMainhand();
        int baneOfArthropodsLevel = EnchantmentHelper.getEnchantmentLevel(Enchantments.BANE_OF_ARTHROPODS, damagerItem);
        if (baneOfArthropodsLevel > 0) {
            addBaneOfArthropodsDamage(event, baneOfArthropodsLevel);
        }
    }

    private void addBaneOfArthropodsDamage(LivingHurtEvent event, int baneOfArthropodsLevel) {
        event.setAmount(event.getAmount() + baneOfArthropodsLevel * BANE_OF_ARTHROPODS_DAMAGE_LEVEL_AMP);
    }

    @Override
    public void onEvent(Object event, Origin origin) {
        super.onEvent(event, origin);
        if (isCanceled(event)) return;
        if (event instanceof LivingHurtEvent) {
            onLivingHurt((LivingHurtEvent) event, origin);
        }
    }

    private static boolean isCanceled(Object event) {
        return event instanceof Event && ((Event)event).isCanceled();
    }

    private void onLivingHurt(LivingHurtEvent event, Origin origin) {
        if (!origin.isServerSide()) return;

        DamageSource damageSource = event.getSource();
        PlayerEntity player = origin.getPlayer();
        if (!trueSourceIsPlayer(damageSource, player)) return;
        LivingEntity hurtEntity = event.getEntityLiving();

        applyProjectileSlowness(player, hurtEntity);
    }

    private static void applyProjectileSlowness(PlayerEntity player, LivingEntity hurtEntity) {
        hurtEntity.addPotionEffect(new EffectInstance(Effects.SLOWNESS, getProjSlowDuration(player, hurtEntity), getProjSlowAmp(player, hurtEntity)));
    }

    private static int getProjSlowDuration(PlayerEntity player, LivingEntity hurtEntity) {
        double distanceSq = player.getDistanceSq(hurtEntity);
        return (int) (PROJ_SLOW_BASE_DURATION + distanceSq * PROJ_SLOW_DURATION_DISTANCE_AMP);
    }

    private static int getProjSlowAmp(PlayerEntity player, LivingEntity hurtEntity) {
        double distanceSq = player.getDistanceSq(hurtEntity);
        return (int) (PROJ_SLOW_BASE_AMP + distanceSq * PROJ_SLOW_AMP_DISTANCE_AMP);
    }

    private static boolean trueSourceIsPlayer(DamageSource damageSource, PlayerEntity player) {
        return damageSource.getTrueSource() != null && damageSource.getTrueSource().equals(player);
    }

}
