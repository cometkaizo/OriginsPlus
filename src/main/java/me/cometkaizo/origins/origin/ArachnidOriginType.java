package me.cometkaizo.origins.origin;

import me.cometkaizo.origins.origin.client.ClientArachnidOriginType;
import me.cometkaizo.origins.property.SpeciesProperty;
import me.cometkaizo.origins.util.AttributeUtils;
import me.cometkaizo.origins.util.DataKey;
import me.cometkaizo.origins.util.TimeTracker;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.enchantment.Enchantments;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.ai.attributes.Attributes;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.potion.EffectInstance;
import net.minecraft.potion.Effects;
import net.minecraft.util.DamageSource;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.world.World;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.event.entity.living.PotionEvent;
import net.minecraftforge.eventbus.api.Event;

import java.util.List;

import static net.minecraftforge.fml.DistExecutor.unsafeRunWhenOn;

public class ArachnidOriginType extends AbstractOriginType {

    public static final DataKey<Boolean> IN_COBWEB = DataKey.create(Boolean.class);
    public static final double MAX_HEALTH = 8 * 2;
    public static final float ARACHNID_RALLY_RANGE = 25;
    public static final int PROJ_POISON_BASE_DURATION = 2 * 20;
    public static final double PROJ_POISON_DURATION_DISTANCE_AMP = 0.2;
    public static final int PROJ_POISON_BASE_AMP = 0;
    public static final double PROJ_POISON_AMP_DISTANCE_AMP = 0.003;
    public static final int AOE_POISON_PLAYER_DURATION = 9 * 20;
    public static final int AOE_POISON_PLAYER_AMP = 1;
    public static final int AOE_POISON_MOB_DURATION = 14 * 20;
    public static final int AOE_POISON_MOB_AMP = 2;
    public static final int AOE_POISON_RANGE = 5;
    public static final int AOE_POISON_RANGE_SQ = AOE_POISON_RANGE * AOE_POISON_RANGE;
    public static final float BANE_OF_ARTHROPODS_DAMAGE_LEVEL_AMP = 1.5F;

    public static final SpeciesProperty SPIDER_SPECIES = new SpeciesProperty.Builder()
            .setMobSpecies(EntityType.SPIDER)
            .setRallyRadius(ARACHNID_RALLY_RANGE).build();

    public static final SpeciesProperty CAVE_SPIDER_SPECIES = new SpeciesProperty.Builder()
            .setMobSpecies(EntityType.CAVE_SPIDER)
            .setRallyRadius(ARACHNID_RALLY_RANGE).build();


    protected ArachnidOriginType() {
        super("Arachnid", Items.COBWEB,
                type -> new Origin.Description(type,
                        new Origin.Description.Entry(type, "climb"),
                        new Origin.Description.Entry(type, "cobweb_immunity"),
                        new Origin.Description.Entry(type, "species"),
                        new Origin.Description.Entry(type, "poison"),
                        new Origin.Description.Entry(type, "bane_of_arthropods"),
                        new Origin.Description.Entry(type, "health")
                ),
                SPIDER_SPECIES,
                CAVE_SPIDER_SPECIES
        );
    }

    public enum Property {
        NO_COBWEB_SLOWDOWN
    }

    public enum Action {
        RESET_FALL_DISTANCE
    }

    public enum Cooldown implements TimeTracker.Timer {
        AOE_POISON(30 * 20);

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
        player.removePotionEffect(Effects.POISON);
        unsafeRunWhenOn(Dist.CLIENT, () -> () -> ClientArachnidOriginType.onActivate(origin));
    }

    @Override
    public void onDeactivate(Origin origin) {
        super.onDeactivate(origin);
        PlayerEntity player = origin.getPlayer();
        AttributeUtils.setAttribute(player, Attributes.MAX_HEALTH, Attributes.MAX_HEALTH.getDefaultValue());
        unsafeRunWhenOn(Dist.CLIENT, () -> () -> ClientArachnidOriginType.onDeactivate(origin));
    }

    @Override
    public void performAction(Origin origin) {
        super.performAction(origin);
        TimeTracker cooldowns = origin.getTimeTracker();

        if (cooldowns.hasTimer(Cooldown.AOE_POISON)) return;

        applyAOEPoison(origin);

        cooldowns.addTimer(Cooldown.AOE_POISON);
    }

    private void applyAOEPoison(Origin origin) {
        PlayerEntity player = origin.getPlayer();

        World world = player.world;

        AxisAlignedBB affectedArea = player.getBoundingBox().grow(AOE_POISON_RANGE);
        List<LivingEntity> affectedEntities = world.getEntitiesWithinAABB(LivingEntity.class, affectedArea);

        for (LivingEntity entity : affectedEntities) {
            if (entity == player) continue;
            if (entity.getDistanceSq(player) > AOE_POISON_RANGE_SQ) continue;
            applyPoison(entity);
        }
    }

    private static void applyPoison(LivingEntity entity) {
        if (entity instanceof PlayerEntity) {
            entity.addPotionEffect(new EffectInstance(Effects.POISON, AOE_POISON_PLAYER_DURATION, AOE_POISON_PLAYER_AMP));
        } else {
            entity.addPotionEffect(new EffectInstance(Effects.POISON, AOE_POISON_MOB_DURATION, AOE_POISON_MOB_AMP));
        }
    }

    @Override
    public void onPlayerSensitiveEvent(Object event, Origin origin) {
        super.onPlayerSensitiveEvent(event, origin);
        if (isCanceled(event)) return;

        if (event instanceof LivingHurtEvent) {
            onPlayerHurt((LivingHurtEvent) event, origin);
        } else if (event instanceof PotionEvent.PotionApplicableEvent) {
            onCheckPotion((PotionEvent.PotionApplicableEvent) event);
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
            applyBaneOfArthropodsDamage(event, baneOfArthropodsLevel);
        }
    }

    private void applyBaneOfArthropodsDamage(LivingHurtEvent event, int baneOfArthropodsLevel) {
        event.setAmount(event.getAmount() * baneOfArthropodsLevel * BANE_OF_ARTHROPODS_DAMAGE_LEVEL_AMP);
    }

    private void onCheckPotion(PotionEvent.PotionApplicableEvent event) {
        if (event.getPotionEffect().getPotion() == Effects.POISON) {
            event.setResult(Event.Result.DENY);
        }
    }

    @Override
    public void onEvent(Object event, Origin origin) {
        super.onEvent(event, origin);
        if (isCanceled(event)) return;
        if (event instanceof LivingHurtEvent) {
            onLivingHurt((LivingHurtEvent) event, origin);
        } else if (event == Action.RESET_FALL_DISTANCE) {
            origin.getPlayer().fallDistance = 0;
        }
    }

    private static boolean isCanceled(Object event) {
        return event instanceof Event && ((Event)event).isCanceled();
    }

    private void onLivingHurt(LivingHurtEvent event, Origin origin) {
        if (!origin.isServerSide()) return;

        DamageSource damageSource = event.getSource();
        PlayerEntity player = origin.getPlayer();
        if (isPlayerCaused(damageSource, player)) {
            LivingEntity hurtEntity = event.getEntityLiving();

            applyProjectilePoison(player, hurtEntity);
        }
    }

    private static boolean isPlayerCaused(DamageSource damageSource, PlayerEntity player) {
        return damageSource.getTrueSource() != null && damageSource.getTrueSource().equals(player);
    }

    private static void applyProjectilePoison(PlayerEntity player, LivingEntity hurtEntity) {
        hurtEntity.addPotionEffect(new EffectInstance(Effects.POISON, getProjPoisonDuration(player, hurtEntity), getProjPoisonAmp(player, hurtEntity)));
    }

    private static int getProjPoisonDuration(PlayerEntity player, LivingEntity hurtEntity) {
        double distanceSq = player.getDistanceSq(hurtEntity);
        return (int) (PROJ_POISON_BASE_DURATION + distanceSq * PROJ_POISON_DURATION_DISTANCE_AMP);
    }

    private static int getProjPoisonAmp(PlayerEntity player, LivingEntity hurtEntity) {
        double distanceSq = player.getDistanceSq(hurtEntity);
        return (int) (PROJ_POISON_BASE_AMP + distanceSq * PROJ_POISON_AMP_DISTANCE_AMP);
    }

}
