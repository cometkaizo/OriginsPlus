package me.cometkaizo.origins.origin;

import me.cometkaizo.origins.origin.client.ClientElytrianOriginType;
import me.cometkaizo.origins.potion.OriginEffects;
import me.cometkaizo.origins.util.ParticleSpawner;
import me.cometkaizo.origins.util.SoundUtils;
import me.cometkaizo.origins.util.TimeTracker;
import net.minecraft.block.BlockState;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.inventory.EquipmentSlotType;
import net.minecraft.item.*;
import net.minecraft.particles.ParticleTypes;
import net.minecraft.potion.EffectInstance;
import net.minecraft.potion.Effects;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.SoundEvents;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraft.world.World;
import net.minecraft.world.server.ServerWorld;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingEquipmentChangeEvent;
import net.minecraftforge.event.entity.living.LivingFallEvent;
import net.minecraftforge.eventbus.api.Event;
import net.minecraftforge.fml.LogicalSide;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import static net.minecraftforge.fml.DistExecutor.unsafeRunWhenOn;

public class ElytrianOriginType extends AbstractOriginType {

    public static final Logger LOGGER = LogManager.getLogger();
    public static final int FULL_ARMOR_FLIGHT_WEAKNESS_AMP = 3;
    public static final int FLIGHT_WEAKNESS_DURATION = 3 * 20;
    public static final double FLAP_AMPLIFIER = 1.1;
    public static final double SUPER_FLAP_AMPLIFIER = 9;
    public static final double BOOST_AMPLIFIER = 0.85;
    public static final double BOOST_OLD_MOVEMENT_REDUCTION = 0.3;
    public static final float BOOST_EXHAUSTION = 0.06F;
    public static final float SNEAK_BOOST_REDUCTION = 0.3F;
    public static final float XP_BONUS_AMP = 0.05F;
    public static final int MAX_ARMOR_VALUE = 30;
    public static final float HEAVINESS_AMP = 0.5F;
    public static final double WEAKNESS_CURVE_AMT = 3.5;
    public static final ParticleSpawner SUPER_BOOST_PARTICLE_SPAWNER = new ParticleSpawner()
            .withParticles(ParticleTypes.CLOUD)
            .withRandomCount(20, 35)
            .withRandomDirection(-3, 0.1, -3, 3, -0.5, 3)
            .withRandomSpeed(0.5, 0.7);
    public static final int LIGHTNING_SPAWN_CHANCE = 50000;
    public static final int LIGHTNING_STRIKE_MIN_Y = 120;

    public ElytrianOriginType() {
        super(Items.ELYTRA, type -> new Origin.Description(type,
                new Origin.Description.Entry(type, "winged"),
                new Origin.Description.Entry(type, "mobility"),
                new Origin.Description.Entry(type, "extra_damage"),
                new Origin.Description.Entry(type, "super_boost"),
                new Origin.Description.Entry(type, "experience"),
                new Origin.Description.Entry(type, "lightness"),
                new Origin.Description.Entry(type, "claustrophobia"),
                new Origin.Description.Entry(type, "lightning")
        ));
    }

    @Override
    public boolean hasMixinProperty(Object property, Origin origin) {
        return property == Property.WINGS;
    }

    public enum Property {
        WINGS
    }

    public enum Cooldown implements TimeTracker.Timer {
        SUPER_BOOST(60 * 20),
        UP_BOOST(0.6 * 20),
        FORWARD_BOOST(5);
        public final int duration;

        Cooldown(double duration) {
            this.duration = (int) duration;
        }

        @Override
        public int getDuration() {
            return duration;
        }
    }

    public enum Action {
        UP_BOOST,
        FORWARD_BOOST
    }

    public static boolean canWearElytra(Origin origin) {
        return origin == null ||
                origin.getType() instanceof HumanOriginType ||
                origin.hasProperty(ElytrianOriginType.Property.WINGS);
    }

    protected int getWeaknessAmplifier(int armorValue) {
        float armorPercentage = armorValue / (float) MAX_ARMOR_VALUE;
        return (int) (Math.pow(armorPercentage, WEAKNESS_CURVE_AMT) * FULL_ARMOR_FLIGHT_WEAKNESS_AMP) - 1;
    }

    private static void updateFlightWeakness(PlayerEntity player, int amplifier) {
        EffectInstance activeWeakness = player.getActivePotionEffect(OriginEffects.FLIGHT_WEAKNESS.get());
        int previousAmplifier = activeWeakness != null ? activeWeakness.getAmplifier() : 0;

        if (amplifier < 0) player.removePotionEffect(OriginEffects.FLIGHT_WEAKNESS.get());
        else {
            if (amplifier < previousAmplifier) player.removePotionEffect(OriginEffects.FLIGHT_WEAKNESS.get());
            player.addPotionEffect(new EffectInstance(OriginEffects.FLIGHT_WEAKNESS.get(), Integer.MAX_VALUE, amplifier));
        }
    }

    private static float getUpdatedArmorValue(LivingEquipmentChangeEvent event, PlayerEntity player) {
        float equippedArmorDamageReduceAmount = getArmorItemValue(player, event.getSlot());
        return getArmorValueExcludingSlot(player, event.getSlot()) + equippedArmorDamageReduceAmount;
    }

    private static float getArmorValueExcludingSlot(PlayerEntity player, EquipmentSlotType slot) {
        float armorValue = 0;

        if (slot != EquipmentSlotType.HEAD) armorValue += getArmorItemValue(player, EquipmentSlotType.HEAD);
        if (slot != EquipmentSlotType.CHEST) armorValue += getArmorItemValue(player, EquipmentSlotType.CHEST);
        if (slot != EquipmentSlotType.LEGS) armorValue += getArmorItemValue(player, EquipmentSlotType.LEGS);
        if (slot != EquipmentSlotType.FEET) armorValue += getArmorItemValue(player, EquipmentSlotType.FEET);

        return armorValue;
    }

    private static float getArmorValue(PlayerEntity player) {
        return getArmorValueExcludingSlot(player, null);
    }

    private static float getArmorItemValue(PlayerEntity player, EquipmentSlotType slot) {
        Item item = player.getItemStackFromSlot(slot).getItem();
        if (item instanceof ArmorItem) {
            ArmorItem armorItem = (ArmorItem) item;
            float armorMaterialValue = getArmorMaterialValue(armorItem.getArmorMaterial());
            return armorItem.getDamageReduceAmount() * armorMaterialValue;
        }
        return 0;
    }

    private static float getArmorMaterialValue(IArmorMaterial material) {
        if (material instanceof ArmorMaterial) switch ((ArmorMaterial) material) {
            case LEATHER:
            case CHAIN:
                return 1F;
            case GOLD:
            case IRON:
                return 1.1F;
            case DIAMOND: return 1.3F;
            case NETHERITE: return 1.5F;
        }
        return 0;
    }

    private static boolean isArmorSlot(EquipmentSlotType slot) {
        return slot == EquipmentSlotType.HEAD ||
                slot == EquipmentSlotType.CHEST ||
                slot == EquipmentSlotType.LEGS ||
                slot == EquipmentSlotType.FEET;
    }

    private static boolean isUnderLowCeiling(PlayerEntity player) {
        World level = player.world;
        BlockPos oneBlockAbove = player.getPosition().up(1);
        BlockPos twoBlocksAbove = player.getPosition().up(2);
        BlockPos threeBlocksAbove = player.getPosition().up(3);

        return getBlockState(level, oneBlockAbove).isSolid() ||
                getBlockState(level, twoBlocksAbove).isSolid() ||
                getBlockState(level, threeBlocksAbove).isSolid();
    }

    private static BlockState getBlockState(World level, BlockPos position) {
        return level.getBlockState(position);
    }

    @Override
    public void onPlayerSensitiveEvent(Object event, Origin origin) {
        super.onPlayerSensitiveEvent(event, origin);
        if (event instanceof Event && ((Event) event).isCanceled()) return;
        if (event instanceof LivingFallEvent) {
            onLivingLand(origin);
        }
    }

    private void onLivingLand(Origin origin) {
        origin.getTimeTracker().remove(Cooldown.UP_BOOST);
    }

    @Override
    public void onEvent(Object event, Origin origin) {
        super.onEvent(event, origin);
        if (isCanceled(event)) return;
        if (event instanceof LivingEquipmentChangeEvent) {
            onEquipmentChange((LivingEquipmentChangeEvent) event, origin);
        } else if (event instanceof TickEvent.PlayerTickEvent) {
            onPlayerTick((TickEvent.PlayerTickEvent) event, origin);
        } else if (event == Action.UP_BOOST) {
            boostUp(origin);
        } else if (event == Action.FORWARD_BOOST) {
            boostForward(origin);
        }

        unsafeRunWhenOn(Dist.CLIENT, () -> () -> ClientElytrianOriginType.onEvent(event, origin));
    }

    @Override
    public void performAction(Origin origin) {
        PlayerEntity player = origin.getPlayer();
        if (canSuperBoost(origin, player)) {
            superBoostUp(origin);
            player.startFallFlying();
        }
    }

    public static boolean canNormalBoost(TimeTracker timeTracker) {
        return !timeTracker.hasTimer(Cooldown.UP_BOOST) &&
                !timeTracker.hasTimer(Cooldown.FORWARD_BOOST);
    }

    private static boolean canSuperBoost(Origin origin, PlayerEntity player) {
        return !player.isInWater() && !player.isPotionActive(Effects.LEVITATION) &&
                !origin.getTimeTracker().hasTimer(Cooldown.SUPER_BOOST);
    }

    public static void superBoostUp(Origin origin) {
        PlayerEntity player = origin.getPlayer();
        boostUp(origin, SUPER_FLAP_AMPLIFIER);
        if (player.world instanceof ServerWorld)
            SUPER_BOOST_PARTICLE_SPAWNER.spawnAt((ServerWorld) player.world, player);
        origin.getTimeTracker().addTimer(Cooldown.SUPER_BOOST);
    }

    public static void boostUp(Origin origin) {
        boostUp(origin, FLAP_AMPLIFIER);
    }

    public static void boostUp(Origin origin, double amp) {
        PlayerEntity player = origin.getPlayer();
        float flapAmount = (-player.rotationPitch + 90) / 180;
        float lightness = getLightness(getArmorValue(origin.getPlayer()));

        double yMotion = flapAmount * amp * lightness;
        player.setMotion(player.getMotion().add(0, yMotion, 0));

        SoundUtils.playSound(player, SoundEvents.ENTITY_PHANTOM_FLAP, SoundCategory.PLAYERS, 0.4F, 1);
        origin.getTimeTracker().addTimer(Cooldown.UP_BOOST);
        player.addExhaustion(BOOST_EXHAUSTION);
    }

    public static void boostForward(Origin origin) {
        PlayerEntity player = origin.getPlayer();
        Vector3d boostAmount = player.getLookVec();
        float lightness = getLightness(getArmorValue(origin.getPlayer()));
        float xpBonus = Math.max(1F, player.experienceLevel * XP_BONUS_AMP);
        float shiftReduction = player.isSneaking() ? SNEAK_BOOST_REDUCTION : 1;

        Vector3d boost = boostAmount
                .scale(BOOST_AMPLIFIER)
                .scale(lightness)
                .scale(xpBonus)
                .scale(shiftReduction);

        Vector3d oldMotion = player.getMotion().scale(BOOST_OLD_MOVEMENT_REDUCTION);
        player.setMotion(oldMotion.add(boost));

        SoundUtils.playSound(player, SoundEvents.ENTITY_PHANTOM_FLAP, SoundCategory.PLAYERS, 0.3F, 1);
        origin.getTimeTracker().addTimer(Cooldown.FORWARD_BOOST);
        player.addExhaustion(BOOST_EXHAUSTION);
    }

    protected static float getLightness(float armorValue) {
        return 1 - (armorValue / MAX_ARMOR_VALUE) * HEAVINESS_AMP;
    }

    @Override
    public void onFirstActivate(Origin origin) {
        unsafeRunWhenOn(Dist.CLIENT, () -> () -> ClientElytrianOriginType.onFirstActivate(origin));
    }

    @Override
    public void onActivate(Origin origin) {
        super.onActivate(origin);
        updateFlightWeakness(origin.getPlayer(), getWeaknessAmplifier((int) getArmorValue(origin.getPlayer())));
        unsafeRunWhenOn(Dist.CLIENT, () -> () -> ClientElytrianOriginType.onActivate(origin));
    }

    @Override
    public void onDeactivate(Origin origin) {
        super.onDeactivate(origin);
        origin.getPlayer().removePotionEffect(OriginEffects.FLIGHT_STRENGTH.get());
        origin.getPlayer().removePotionEffect(OriginEffects.FLIGHT_WEAKNESS.get());
        unsafeRunWhenOn(Dist.CLIENT, () -> () -> ClientElytrianOriginType.onDeactivate(origin));
    }

    public void onPlayerTick(TickEvent.PlayerTickEvent event, Origin origin) {
        if (event.isCanceled()) return;
        if (event.phase == TickEvent.Phase.START) return;
        if (!origin.getPlayer().equals(event.player)) return;
        PlayerEntity player = origin.getPlayer();

        if (player.isElytraFlying() && player.areEyesInFluid(FluidTags.WATER)) {
            setSwimming(player);
        }

        if (event.side == LogicalSide.CLIENT) return;

        if (player.isElytraFlying()) {
            player.addPotionEffect(new EffectInstance(OriginEffects.FLIGHT_STRENGTH.get(), Integer.MAX_VALUE, 0));
        } else {
            player.removePotionEffect(OriginEffects.FLIGHT_STRENGTH.get());
            if (player.isOnGround() && isUnderLowCeiling(player)) {
                player.addPotionEffect(new EffectInstance(OriginEffects.FLIGHT_WEAKNESS.get(), FLIGHT_WEAKNESS_DURATION, 0));
            }
        }
    }

    public static void setSwimming(PlayerEntity player) {
        player.stopFallFlying();
        player.setSwimming(true);
        player.setSprinting(true);
        player.updateSwimming();
    }

    public void onEquipmentChange(LivingEquipmentChangeEvent event, Origin origin) {
        if (event.isCanceled()) return;
        if (!origin.getPlayer().equals(event.getEntity())) return;
        if (!isArmorSlot(event.getSlot())) return;

        PlayerEntity player = origin.getPlayer();

        int updatedArmorValue = (int) getUpdatedArmorValue(event, player);
        int amplifier = getWeaknessAmplifier(updatedArmorValue);

        updateFlightWeakness(player, amplifier);
    }

}
