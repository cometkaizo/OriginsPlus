package me.cometkaizo.origins.origin.client;

import me.cometkaizo.origins.origin.ElytrianOriginType;
import me.cometkaizo.origins.origin.Origin;
import me.cometkaizo.origins.util.DataKey;
import me.cometkaizo.origins.util.DataManager;
import me.cometkaizo.origins.util.SoundUtils;
import me.cometkaizo.origins.util.TimeTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.player.ClientPlayerEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.inventory.EquipmentSlotType;
import net.minecraft.item.ArmorItem;
import net.minecraft.item.ArmorMaterial;
import net.minecraft.item.IArmorMaterial;
import net.minecraft.item.Item;
import net.minecraft.util.MovementInput;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.SoundEvents;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.event.EntityViewRenderEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;

@OnlyIn(Dist.CLIENT)
public class ClientElytrianOriginType {
    protected static final DataKey<Float> PREV_ROLL = DataKey.create(Float.class);
    protected static final DataKey<Float> ROLL_FOLLOW_TARGET = DataKey.create(Float.class);
    protected static final DataKey<Double> PREV_ROLL_PARTIAL_TICKS = DataKey.create(Double.class);
    public static final double FLAP_AMPLIFIER = 1.1;
    public static final double BOOST_AMPLIFIER = 0.85;
    public static final double BOOST_OLD_MOVEMENT_REDUCTION = 0.3;
    public static final float BOOST_EXHAUSTION = 0.06F;
    public static final float SNEAK_BOOST_REDUCTION = 0.3F;
    public static final int MAX_ARMOR_VALUE = 20;
    public static final float XP_BONUS_AMP = 0.05F;
    public static final float ROLL_AMP = 1;
    public static final double ROLL_RESPONSIVENESS = 0.2;
    public static final float ROLL_FOLLOW_TARGET_REDUCTION = 0.7F;



    public void onEvent(Object event, Origin origin) {
        if (event instanceof EntityViewRenderEvent.CameraSetup) {
            onCameraSetup((EntityViewRenderEvent.CameraSetup) event, origin);
        } else if (event instanceof TickEvent.ClientTickEvent) {
            TickEvent.ClientTickEvent tickEvent = (TickEvent.ClientTickEvent) event;
            onClientTick(tickEvent, origin);
            updateRollTarget(tickEvent, origin);
        } else if (event instanceof PlayerInteractEvent.RightClickEmpty) {
            onEmptyClick((PlayerInteractEvent.RightClickEmpty) event, origin);
        }
    }


    public static void onClientTick(TickEvent.ClientTickEvent event, Origin origin) {
        if (event.isCanceled()) return;
        if (event.phase == TickEvent.Phase.START) return;
        if (origin.isServerSide()) return;

        ClientPlayerEntity player = Minecraft.getInstance().player;
        if (!origin.getPlayer().equals(player)) return;

        if (!player.isElytraFlying()) return;
        MovementInput input = player.movementInput;
        TimeTracker cooldownTracker = origin.getTimeTracker();

        if (input.jump && !cooldownTracker.hasCooldownOf(ElytrianOriginType.Cooldown.class)) {
            boostUp(origin, player, cooldownTracker);
        }
    }

    private static void boostUp(Origin origin, ClientPlayerEntity player, TimeTracker cooldownTracker) {
        float flapAmount = (-player.rotationPitch + 90) / 180;
        float lightness = getLightness(getArmorValue(origin.getPlayer()));

        double yMotion = flapAmount * FLAP_AMPLIFIER * lightness;
        player.setMotion(player.getMotion().add(0, yMotion, 0));

        SoundUtils.playSound(player, SoundEvents.ENTITY_PHANTOM_FLAP, SoundCategory.PLAYERS, 0.4F, 1);
        cooldownTracker.addTimer(ElytrianOriginType.Cooldown.UP_BOOST);
        player.addExhaustion(BOOST_EXHAUSTION);
    }

    private static void boostForward(Origin origin, ClientPlayerEntity player, TimeTracker cooldownTracker) {
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
        cooldownTracker.addTimer(ElytrianOriginType.Cooldown.FORWARD_BOOST);
        player.addExhaustion(BOOST_EXHAUSTION);
    }

    protected static float getLightness(float armorValue) {
        return 1 - armorValue / MAX_ARMOR_VALUE;
    }

    public static void onEmptyClick(PlayerInteractEvent.RightClickEmpty event, Origin origin) {
        if (event.isCanceled()) return;
        if (!origin.getPlayer().equals(event.getPlayer())) return;

        ClientPlayerEntity player = Minecraft.getInstance().player;
        if (player == null) return;
        TimeTracker cooldownTracker = origin.getTimeTracker();

        if (!player.isElytraFlying()) return;

        if (!cooldownTracker.hasCooldownOf(ElytrianOriginType.Cooldown.class)) {
            boostForward(origin, player, cooldownTracker);
        }
    }

    public static void updateRollTarget(TickEvent.ClientTickEvent event, Origin origin) {
        if (event.isCanceled()) return;
        if (event.phase == TickEvent.Phase.START) return;
        if (origin.isServerSide()) return;

        ClientPlayerEntity player = Minecraft.getInstance().player;
        if (!origin.getPlayer().equals(player)) return;

        DataManager dataManager = origin.getTypeDataManager();

        float yawDiff = player.rotationYawHead - player.prevRotationYawHead;
        float followTarget = dataManager.get(ROLL_FOLLOW_TARGET);
        if (player.isElytraFlying())
            dataManager.set(ROLL_FOLLOW_TARGET, (followTarget + yawDiff) * ROLL_FOLLOW_TARGET_REDUCTION);
        else
            dataManager.set(ROLL_FOLLOW_TARGET, followTarget * ROLL_FOLLOW_TARGET_REDUCTION);

    }

    public static void onCameraSetup(EntityViewRenderEvent.CameraSetup event, Origin origin) {
        if (event.isCanceled()) return;
        if (origin.isServerSide()) return;
        ClientPlayerEntity player = Minecraft.getInstance().player;
        if (!origin.getPlayer().equals(player)) return;
        DataManager dataManager = origin.getTypeDataManager();

        double partialTicks = event.getRenderPartialTicks();
        Double prevPartialTicks = dataManager.get(PREV_ROLL_PARTIAL_TICKS);
        float prevRoll = dataManager.get(PREV_ROLL);
        float followTarget = dataManager.get(ROLL_FOLLOW_TARGET);

        double followAmt = (followTarget - prevRoll) * ROLL_RESPONSIVENESS * prevPartialTicks;

        double newRoll = (prevRoll + followAmt) * ROLL_AMP;
        event.setRoll((float) newRoll);

        if (prevPartialTicks > partialTicks) {
            dataManager.set(PREV_ROLL, event.getRoll());
        }
        dataManager.set(PREV_ROLL_PARTIAL_TICKS, partialTicks);
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
}
