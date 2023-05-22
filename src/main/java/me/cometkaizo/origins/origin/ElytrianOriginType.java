package me.cometkaizo.origins.origin;

import me.cometkaizo.origins.origin.client.ClientElytrianOriginType;
import me.cometkaizo.origins.potion.OriginEffects;
import me.cometkaizo.origins.util.TimeTracker;
import net.minecraft.block.BlockState;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.inventory.EquipmentSlotType;
import net.minecraft.item.ArmorItem;
import net.minecraft.item.ArmorMaterial;
import net.minecraft.item.IArmorMaterial;
import net.minecraft.item.Item;
import net.minecraft.potion.EffectInstance;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraft.world.World;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingEquipmentChangeEvent;
import net.minecraftforge.fml.LogicalSide;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import static net.minecraftforge.fml.DistExecutor.unsafeRunWhenOn;

public class ElytrianOriginType extends AbstractOriginType {

    public static final Logger LOGGER = LogManager.getLogger();
    public static final int FLIGHT_WEAKNESS_DURATION = 3 * 20;/*
    public static final ActionOnItemRightClickAirProperty FORWARD_BOOST_ON_EMPTY_CLICK = new ActionOnItemRightClickAirProperty.Builder()
            .withPlayerSensitiveAction(Item.class, (__, origin) -> tryBoostForward(origin)).build();
    public static final EventInterceptProperty NO_HEAVY_ARMOR = new EventInterceptProperty.Builder()
            .withPlayerSensitiveAction(LivingEquipmentChangeEvent.class, (event, origin) -> {
                if (!isArmorSlot(event.getSlot())) return;
                PlayerEntity player = origin.getPlayer();

                int updatedArmorValue = (int) getUpdatedArmorValue(event, player);
                int amplifier = getWeaknessAmplifier(updatedArmorValue);

                updateFlightWeakness(player, amplifier);
            }).build();
    private static final EventInterceptProperty UPWARD_BOOST_ON_MIDAIR_JUMP = new EventInterceptProperty.Builder()
            .withAction(TickEvent.ClientTickEvent.class, (event, origin) -> {
                if (event.phase == TickEvent.Phase.START) return;
                if (origin.isServerSide()) return;

                ClientPlayerEntity player = Minecraft.getInstance().player;
                if (!origin.getPlayer().equals(player)) return;

                if (!player.isElytraFlying()) return;
                MovementInput input = player.movementInput;
                TimeTracker cooldownTracker = origin.getTimeTracker();

                if (input.jump && !cooldownTracker.hasCooldownOf(Cooldown.class)) {
                    boostUp(origin, player, cooldownTracker);
                }
            }).build();
    public static final ActionOnPlayerTickProperty FLIGHT_STRENGTH = new ActionOnPlayerTickProperty.Builder()
            .withPlayerSensitiveAction((event, origin) -> {
                if (event.side == LogicalSide.CLIENT) return;
                if (event.phase == TickEvent.Phase.START) return;
                PlayerEntity player = origin.getPlayer();

                if (player.isElytraFlying()) {
                    player.addPotionEffect(new EffectInstance(OriginEffects.FLIGHT_STRENGTH.get(), Integer.MAX_VALUE, 0));
                } else {
                    player.removePotionEffect(OriginEffects.FLIGHT_STRENGTH.get());
                }
            }).build();
    public static final ActionOnPlayerTickProperty CLAUSTROPHOBIA = new ActionOnPlayerTickProperty.Builder()
            .withPlayerSensitiveAction((event, origin) -> {
                if (event.side == LogicalSide.CLIENT) return;
                if (event.phase == TickEvent.Phase.START) return;
                PlayerEntity player = origin.getPlayer();

                if (!player.isElytraFlying() && player.isOnGround() && isUnderLowCeiling(player)) {
                    player.addPotionEffect(new EffectInstance(OriginEffects.FLIGHT_WEAKNESS.get(), FLIGHT_WEAKNESS_DURATION, 0));
                }
            }).build();

    private static void tryBoostForward(Origin origin) {
        if (origin.isServerSide()) return;
        TimeTracker cooldownTracker = origin.getTimeTracker();
        ClientPlayerEntity player = (ClientPlayerEntity) origin.getPlayer();
        if (!player.isElytraFlying()) return;

        if (!cooldownTracker.hasCooldownOf(Cooldown.class)) {
            boostForward(origin, player, cooldownTracker);
        }
    }*/

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
/*
    public ElytrianOriginType() {
        super("Elytrian",
                FORWARD_BOOST_ON_EMPTY_CLICK,
                UPWARD_BOOST_ON_MIDAIR_JUMP,
                NO_HEAVY_ARMOR,
                FLIGHT_STRENGTH,
                CLAUSTROPHOBIA
        );
    }*/

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
        Vector3d twoBlocksAbove = player.getPositionVec().add(0, 2, 0);
        Vector3d threeBlocksAbove = player.getPositionVec().add(0, 3, 0);

        return getBlockState(level, twoBlocksAbove).isSolid() ||
                getBlockState(level, threeBlocksAbove).isSolid();
    }

    private static BlockState getBlockState(World level, Vector3d position) {
        return level.getBlockState(new BlockPos(position));
    }

    @Override
    public void onEvent(Object event, Origin origin) {
        if (event instanceof LivingEquipmentChangeEvent) {
            onEquipmentChange((LivingEquipmentChangeEvent) event, origin);
        } else if (event instanceof TickEvent.PlayerTickEvent) {
            onPlayerTick((TickEvent.PlayerTickEvent) event, origin);
        }

        unsafeRunWhenOn(Dist.CLIENT, () -> () -> ClientElytrianOriginType.onEvent(event, origin));
    }

    @Override
    public void onFirstActivate(Origin origin) {
        unsafeRunWhenOn(Dist.CLIENT, () -> () -> ClientElytrianOriginType.onFirstActivate(origin));
    }

    @Override
    public void performAction(Origin origin) {

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
