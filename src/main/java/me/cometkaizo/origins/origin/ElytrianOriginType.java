package me.cometkaizo.origins.origin;

import me.cometkaizo.origins.origin.client.ClientElytrianOriginType;
import me.cometkaizo.origins.potion.OriginEffects;
import me.cometkaizo.origins.util.SoundUtils;
import me.cometkaizo.origins.util.TimeTracker;
import net.minecraft.block.BlockState;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.inventory.EquipmentSlotType;
import net.minecraft.item.ArmorItem;
import net.minecraft.item.ArmorMaterial;
import net.minecraft.item.IArmorMaterial;
import net.minecraft.item.Item;
import net.minecraft.potion.EffectInstance;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.SoundEvents;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraft.world.World;
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
    public static final int MAX_ARMOR_VALUE = 20;
    public static final int FLIGHT_WEAKNESS_DURATION = 3 * 20;
    public static final double FLAP_AMPLIFIER = 1.1;
    public static final double BOOST_AMPLIFIER = 0.85;
    public static final double BOOST_OLD_MOVEMENT_REDUCTION = 0.3;
    public static final float BOOST_EXHAUSTION = 0.06F;
    public static final float SNEAK_BOOST_REDUCTION = 0.3F;
    public static final float XP_BONUS_AMP = 0.05F;
    public static final float HEAVINESS_AMP = 0.25F;

    @Override
    public boolean hasMixinProperty(Object property, Origin origin) {
        return property == Property.PERMANENT_WINGS;
    }

    public enum Property {
        PERMANENT_WINGS
    }

    public enum Cooldown implements TimeTracker.Timer {
        UP_BOOST(0.75 * 20),
        FORWARD_BOOST(7);
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

    private static int getWeaknessAmplifier(int armorValue) {
        return (armorValue - 13) / 2;
    }

    private static void updateFlightWeakness(PlayerEntity player, int amplifier) {
        int previousAmplifier = 0;
        EffectInstance activeWeakness = player.getActivePotionEffect(OriginEffects.FLIGHT_WEAKNESS.get());
        if (activeWeakness != null) previousAmplifier = activeWeakness.getAmplifier();

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
    public void onFirstActivate(Origin origin) {
        unsafeRunWhenOn(Dist.CLIENT, () -> () -> ClientElytrianOriginType.onFirstActivate(origin));
    }

    @Override
    public void performAction(Origin origin) {
        boostUp(origin);
    }

    public static void boostUp(Origin origin) {
        PlayerEntity player = origin.getPlayer();
        float flapAmount = (-player.rotationPitch + 90) / 180;
        float lightness = getLightness(getArmorValue(origin.getPlayer()));

        double yMotion = flapAmount * FLAP_AMPLIFIER * lightness;
        player.setMotion(player.getMotion().add(0, yMotion, 0));

        SoundUtils.playSound(player, SoundEvents.ENTITY_PHANTOM_FLAP, SoundCategory.PLAYERS, 0.4F, 1);
        origin.getTimeTracker().addTimer(ElytrianOriginType.Cooldown.UP_BOOST);
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
        origin.getTimeTracker().addTimer(ElytrianOriginType.Cooldown.FORWARD_BOOST);
        player.addExhaustion(BOOST_EXHAUSTION);
    }

    protected static float getLightness(float armorValue) {
        return 1 - (armorValue / MAX_ARMOR_VALUE) * HEAVINESS_AMP;
    }

    @Override
    public void onActivate(Origin origin) {
        updateFlightWeakness(origin.getPlayer(), getWeaknessAmplifier((int) getArmorValue(origin.getPlayer())));
    }

    @Override
    public void onDeactivate(Origin origin) {
        origin.getPlayer().removePotionEffect(OriginEffects.FLIGHT_WEAKNESS.get());
    }

    public void onPlayerTick(TickEvent.PlayerTickEvent event, Origin origin) {
        if (event.isCanceled()) return;
        if (event.side == LogicalSide.CLIENT) return;
        if (event.phase == TickEvent.Phase.START) return;
        if (!origin.getPlayer().equals(event.player)) return;
        PlayerEntity player = origin.getPlayer();

        if (player.isElytraFlying()) {
            player.addPotionEffect(new EffectInstance(OriginEffects.FLIGHT_STRENGTH.get(), Integer.MAX_VALUE, 0));
        } else {
            player.removePotionEffect(OriginEffects.FLIGHT_STRENGTH.get());
            if (player.isOnGround() && isUnderLowCeiling(player)) {
                player.addPotionEffect(new EffectInstance(OriginEffects.FLIGHT_WEAKNESS.get(), FLIGHT_WEAKNESS_DURATION, 0));
            }
        }
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
