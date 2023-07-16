package me.cometkaizo.origins.origin;

import me.cometkaizo.origins.common.OriginTags;
import me.cometkaizo.origins.potion.OriginEffects;
import me.cometkaizo.origins.property.SpeciesProperty;
import me.cometkaizo.origins.util.AttributeUtils;
import me.cometkaizo.origins.util.DataKey;
import me.cometkaizo.origins.util.DataManager;
import me.cometkaizo.origins.util.EffectApplier;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.ai.attributes.Attributes;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Food;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.potion.Effects;
import net.minecraft.util.DamageSource;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingEntityUseItemEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;

public class FoxOriginType extends AbstractOriginType {
    public static final float FOX_RALLY_RANGE = 25;
    public static final SpeciesProperty FOX_SPECIES = new SpeciesProperty.Builder()
            .setMobSpecies(EntityType.FOX)
            .setRallyRadius(FOX_RALLY_RANGE).build();
    public static final DataKey<Integer> SNEAK_TIME = DataKey.create(Integer.class);
    public static final DataKey<Long> CAMOUFLAGE_START_TICK = DataKey.create(Long.class),
            CAMOUFLAGE_END_TICK = DataKey.create(Long.class);
    public static final DataKey<Double> LAST_X = DataKey.create(Double.class),
            LAST_Y = DataKey.create(Double.class),
            LAST_Z = DataKey.create(Double.class);
    public static final DataKey<Long> LAST_STOP_SNEAK_TICK = DataKey.create(Long.class);

    protected double maxHealth = 8 * 2;
    protected double speedBoostHealthThreshold = 3.5 * 2;
    protected EffectApplier speedBoostApplier = new EffectApplier()
            .withEffect(Effects.SPEED, 10 * 20, 1);
    protected float berryHealAmp = 2;
    protected EffectApplier berryRegenApplier = new EffectApplier()
            .withEffect(Effects.REGENERATION, 5 * 20, 0);
    protected double camouflageSneakTime = 4 * 20;
    protected double stopCamouflageStandTime = 2.7 * 20;
    protected EffectApplier camouflageApplier = new EffectApplier()
            .withEffect(OriginEffects.CAMOUFLAGE.get(), 999999, 0, false, false);

    public enum Property {
        IGNORE_HUNGER_FOR_SWEET_BERRIES,
        NIGHT_VISION
    }

    protected FoxOriginType() {
        super("Fox", Items.SWEET_BERRIES,
                type -> new Origin.Description(type,
                        new Origin.Description.Entry(type, "stealth"),
                        new Origin.Description.Entry(type, "scram"),
                        new Origin.Description.Entry(type, "no_fall_damage"),
                        new Origin.Description.Entry(type, "health"),
                        new Origin.Description.Entry(type, "night_vision"),
                        new Origin.Description.Entry(type, "berries"),
                        new Origin.Description.Entry(type, "species")
                ),
                FOX_SPECIES
        );
    }

    @Override
    public boolean hasMixinProperty(Object property, Origin origin) {
        return property == Property.NIGHT_VISION ||
                property == Property.IGNORE_HUNGER_FOR_SWEET_BERRIES ||
                super.hasMixinProperty(property, origin);
    }

    @Override
    public void onFirstActivate(Origin origin) {
        super.onFirstActivate(origin);
        if (origin.isServerSide()) {
            origin.getTypeData().register(SNEAK_TIME, -1);
            origin.getTypeData().register(LAST_X, 0D);
            origin.getTypeData().register(LAST_Y, 0D);
            origin.getTypeData().register(LAST_Z, 0D);
            origin.getTypeData().register(LAST_STOP_SNEAK_TICK, -1L);
        } else {
            origin.getTypeData().register(CAMOUFLAGE_START_TICK, -1L);
            origin.getTypeData().register(CAMOUFLAGE_END_TICK, -1L);
        }
    }

    @Override
    public void onActivate(Origin origin) {
        super.onActivate(origin);
        PlayerEntity player = origin.getPlayer();
        AttributeUtils.setAttribute(player, Attributes.MAX_HEALTH, maxHealth);
        AttributeUtils.setAttribute(player, Attributes.MOVEMENT_SPEED, 0.13);
    }

    @Override
    public void onDeactivate(Origin origin) {
        super.onDeactivate(origin);
        PlayerEntity player = origin.getPlayer();
        AttributeUtils.setAttribute(player, Attributes.MAX_HEALTH, Attributes.MAX_HEALTH.getDefaultValue());
        AttributeUtils.setAttribute(player, Attributes.MOVEMENT_SPEED, player.abilities.getWalkSpeed());
        player.removePotionEffect(OriginEffects.CAMOUFLAGE.get());
    }

    @Override
    public void onPlayerSensitiveEvent(Object event, Origin origin) {
        super.onPlayerSensitiveEvent(event, origin);
        if (isCanceled(event)) return;
        if (event instanceof LivingHurtEvent) {
            LivingHurtEvent hurtEvent = (LivingHurtEvent) event;
            cancelFallDamage(hurtEvent);
            tryApplySpeedBoost(hurtEvent, origin);
        } else if (event instanceof LivingEntityUseItemEvent.Finish) {
            tryApplyBerryEffects((LivingEntityUseItemEvent.Finish) event, origin);
        } else if (event instanceof TickEvent.PlayerTickEvent) {
            updateMovementSpeed(origin);
            updateCamouflage(origin);
        }
    }

    protected void cancelFallDamage(LivingHurtEvent event) {
        if (event.getSource() == DamageSource.FALL) event.setCanceled(true);
    }

    protected void tryApplySpeedBoost(LivingHurtEvent event, Origin origin) {
        PlayerEntity player = origin.getPlayer();
        float health = player.getHealth();
        float damageAmount = event.getAmount();
        if (shouldApplySpeedBoost(health, damageAmount)) {
            speedBoostApplier.applyTo(player);
        }
    }

    protected boolean shouldApplySpeedBoost(float health, float damageAmount) {
        return health > speedBoostHealthThreshold && health - damageAmount <= speedBoostHealthThreshold;
    }


    protected void tryApplyBerryEffects(LivingEntityUseItemEvent.Finish event, Origin origin) {
        PlayerEntity player = origin.getPlayer();
        ItemStack itemStack = event.getItem();

        if (shouldApplyBerryEffects(itemStack)) {
            berryRegenApplier.applyTo(player);

            applyExtraBerryHealing(event, player);
        }
    }

    protected boolean shouldApplyBerryEffects(ItemStack itemStack) {
        return itemStack.getItem() == Items.SWEET_BERRIES;
    }

    protected void applyExtraBerryHealing(LivingEntityUseItemEvent.Finish event, PlayerEntity player) {
        ItemStack itemStack = event.getItem();
        if (!itemStack.isFood()) return;
        Food consumedFood = itemStack.getItem().getFood();
        if (consumedFood == null) return;

        player.getFoodStats().addStats((int) (consumedFood.getHealing() * (berryHealAmp - 1)), consumedFood.getSaturation() * (berryHealAmp - 1));
    }

    protected void updateCamouflage(Origin origin) {
        if (!origin.isServerSide()) return;
        PlayerEntity player = origin.getPlayer();
        DataManager data = origin.getTypeData();

        if (player.isSneaking()) {
            data.set(LAST_STOP_SNEAK_TICK, -1L);
            if (data.get(SNEAK_TIME) >= camouflageSneakTime) applyCamouflage(origin);
            else {
                if (playerMoved(player, data)) {
                    stopCamouflaging(origin);
                } else data.increase(SNEAK_TIME, 1);
            }
        } else {
            long tick = player.world.getGameTime();
            long lastStopSneakTick = data.get(LAST_STOP_SNEAK_TICK);

            if (lastStopSneakTick == -1L) data.set(LAST_STOP_SNEAK_TICK, tick);
            else if (playerMoved(player, data) || tick - lastStopSneakTick > stopCamouflageStandTime) stopCamouflaging(origin);
        }

        data.set(LAST_X, player.getPosX());
        data.set(LAST_Y, player.getPosY());
        data.set(LAST_Z, player.getPosZ());
    }

    protected boolean playerMoved(PlayerEntity player, DataManager data) {
        double x = player.getPosX(),
                y = player.getPosY(),
                z = player.getPosZ(),
                lastX = data.get(LAST_X),
                lastY = data.get(LAST_Y),
                lastZ = data.get(LAST_Z);
        return x != lastX || y != lastY || z != lastZ;
    }

    protected void applyCamouflage(Origin origin) {
        PlayerEntity player = origin.getPlayer();

        camouflageApplier.applyTo(player);
    }

    protected void stopCamouflaging(Origin origin) {
        PlayerEntity player = origin.getPlayer();
        DataManager data = origin.getTypeData();

        data.set(SNEAK_TIME, -1);
        player.removePotionEffect(OriginEffects.CAMOUFLAGE.get());
    }

    protected void updateMovementSpeed(Origin origin) {
        PlayerEntity player = origin.getPlayer();
        if (shouldApplyExtraMovementSpeed(player)) AttributeUtils.setAttribute(player, Attributes.MOVEMENT_SPEED, 0.16);
        else AttributeUtils.setAttribute(player, Attributes.MOVEMENT_SPEED, 0.13);
    }

    protected boolean shouldApplyExtraMovementSpeed(PlayerEntity player) {
        return player.world.getBlockState(new BlockPos(player.getPosX(), player.getPosY() - 0.5000001D, player.getPosZ())).isIn(OriginTags.Blocks.FOX_EXTRA_SPEED);
    }
}
