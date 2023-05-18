package me.cometkaizo.origins.origin;

import me.cometkaizo.origins.util.DataKey;
import me.cometkaizo.origins.util.DataManager;
import me.cometkaizo.origins.util.SoundUtils;
import me.cometkaizo.origins.util.TimeTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.player.ClientPlayerEntity;
import net.minecraft.entity.MoverType;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.*;
import net.minecraft.potion.Potion;
import net.minecraft.potion.PotionUtils;
import net.minecraft.potion.Potions;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.SoundEvent;
import net.minecraft.util.SoundEvents;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraftforge.client.event.EntityViewRenderEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingEntityUseItemEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class SharkOriginType extends AbstractOriginType {
    public static final Logger LOGGER = LogManager.getLogger();
    protected static final DataKey<Integer> RIGHT_CLICK_TIME = DataKey.create(Integer.class);
    protected static final DataKey<Float> PREV_YAW = DataKey.create(Float.class);
    protected static final DataKey<Float> PREV_PITCH = DataKey.create(Float.class);
    protected static final DataKey<Float> PREV_ROLL = DataKey.create(Float.class);
    protected static final DataKey<Float> ROLL_FOLLOW_TARGET = DataKey.create(Float.class);
    protected static final DataKey<Double> PREV_PARTIAL_TICKS = DataKey.create(Double.class);
    public static final int MAX_RIPTIDE_CHARGE_TIME = 6 * 20;
    public static final double SWIM_OLD_MOVEMENT_REDUCTION = 0.2;
    public static final double SWIM_SPEED = 0.3;
    public static final float MAX_SWIM_SPEED_SQUARED = 0.7F * 0.7F;
    public static final float MAX_FAST_SWIM_SPEED_SQUARED = 1.25F * 1.25F;
    public static final float FAST_SWIM_DEGREES = 8;
    public static final float WATER_FOG_REDUCTION_FACTOR = 0.3F;
    public static final float RAIN_FOG_REDUCTION_FACTOR = 0.3F;
    public static final float AIR_FOG_REDUCTION_FACTOR = 0.4F;

    public static final Object WATER_BREATHING_PROPERTY = new Object();
    public static final float ROLL_AMP = 1;
    public static final double ROLL_RESPONSIVENESS = 0.2;
    public static final float ROLL_FOLLOW_TARGET_REDUCTION = 0.7F;

    public enum Cooldown implements TimeTracker.Cooldown {
        RIPTIDE_BOOST(1.25 * 20);
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
    public void onEvent(Object event, Origin origin) {
        if (event instanceof EntityViewRenderEvent.CameraSetup) {
            onCameraSetup((EntityViewRenderEvent.CameraSetup) event, origin);
        } else if (event instanceof EntityViewRenderEvent.FogDensity) {
            onRenderFog((EntityViewRenderEvent.FogDensity) event, origin);
        } else if (event instanceof TickEvent.ClientTickEvent) {
            onClientTick((TickEvent.ClientTickEvent) event, origin);
        } else if (event instanceof PlayerInteractEvent.RightClickEmpty) {
            onEmptyClick((PlayerInteractEvent.RightClickEmpty) event, origin);
        } else if (event instanceof LivingEntityUseItemEvent.Finish) {
            onFinishUseItem((LivingEntityUseItemEvent.Finish) event, origin);
        } else if (event instanceof PlayerEvent.BreakSpeed) {
            onBreakSpeed((PlayerEvent.BreakSpeed) event, origin);
        }
    }

    @Override
    public boolean hasMixinProperty(Object property, Origin origin) {
        return property == WATER_BREATHING_PROPERTY;
    }

    @Override
    public void onFirstActivate(Origin origin) {
        origin.getTypeDataManager().register(RIGHT_CLICK_TIME, 0);
        if (origin.isServerSide()) return;
        origin.getTypeDataManager().register(PREV_PITCH, 0F);
        origin.getTypeDataManager().register(PREV_YAW, 0F);
        origin.getTypeDataManager().register(PREV_ROLL, 0F);
        origin.getTypeDataManager().register(ROLL_FOLLOW_TARGET, 0F);
        origin.getTypeDataManager().register(PREV_PARTIAL_TICKS, 0D);
    }

    public void onRenderFog(EntityViewRenderEvent.FogDensity event, Origin origin) {
        if (event.isCanceled()) return;
        ClientPlayerEntity player = Minecraft.getInstance().player;
        if (player == null || !origin.getPlayer().getGameProfile().equals(player.getGameProfile())) return;

        if (player.areEyesInFluid(FluidTags.WATER)) {
            event.setDensity(event.getDensity() * WATER_FOG_REDUCTION_FACTOR);
        } else if (isInRain(origin.getPlayer())) {
            event.setDensity(event.getDensity() * RAIN_FOG_REDUCTION_FACTOR);
        } else {
            event.setDensity(event.getDensity() * AIR_FOG_REDUCTION_FACTOR);
        }
        event.setCanceled(true); // must cancel for this event to have an effect
    }

    public void onCameraSetup(EntityViewRenderEvent.CameraSetup event, Origin origin) {
        if (event.isCanceled()) return;
        if (origin.isServerSide()) return;

        ClientPlayerEntity player = Minecraft.getInstance().player;
        if (!origin.getPlayer().equals(player)) return;
        DataManager dataManager = origin.getTypeDataManager();

        double partialTicks = event.getRenderPartialTicks();
        Double prevPartialTicks = dataManager.get(PREV_PARTIAL_TICKS);
        float prevRoll = dataManager.get(PREV_ROLL);
        float followTarget = dataManager.get(ROLL_FOLLOW_TARGET);

        double followAmt = (followTarget - prevRoll) * ROLL_RESPONSIVENESS * prevPartialTicks;

        double newRoll = (prevRoll + followAmt) * ROLL_AMP;
        event.setRoll((float) newRoll);

        if (prevPartialTicks > partialTicks) {
            dataManager.set(PREV_ROLL, event.getRoll());
        }
        dataManager.set(PREV_PARTIAL_TICKS, partialTicks);
    }

    public void onBreakSpeed(PlayerEvent.BreakSpeed event, Origin origin) {
        if (event.isCanceled()) return;
        if (!origin.isServerSide()) return;
        if (!origin.getPlayer().equals(event.getEntity())) return;
        PlayerEntity player = origin.getPlayer();

        if (player.areEyesInFluid(FluidTags.WATER)) {
            event.setNewSpeed(event.getNewSpeed() * 5);
        } else {
            event.setNewSpeed(event.getNewSpeed() / 2);
        }
        if (player.isInWater() && !player.isOnGround()) {
            event.setNewSpeed(event.getNewSpeed() * 3);
        }
    }

    public void onFinishUseItem(LivingEntityUseItemEvent.Finish event, Origin origin) {
        if (event.isCanceled()) return;
        if (!origin.getPlayer().equals(event.getEntity())) return;
        PlayerEntity player = origin.getPlayer();

        if (isWaterPotion(event.getItem())) {
            player.setAir(player.getMaxAir());
        } else if (isSeafood(event.getItem())) {
            if (!event.getItem().isFood()) return;
            Food consumedFood = event.getItem().getItem().getFood();
            if (consumedFood == null) return;

            player.getFoodStats().addStats(consumedFood.getHealing(), consumedFood.getSaturation());
        }
    }

    private static boolean isWaterPotion(ItemStack itemStack) {
        if (!(itemStack.getItem() instanceof PotionItem)) return false;
        Potion potion = PotionUtils.getPotionFromItem(itemStack);
        return potion == Potions.WATER ||
                potion == Potions.AWKWARD ||
                potion == Potions.MUNDANE ||
                potion == Potions.THICK;
    }

    private static boolean isSeafood(ItemStack itemStack) {
        Item item = itemStack.getItem();
        return item == Items.COD ||
                item == Items.SALMON ||
                item == Items.PUFFERFISH ||
                item == Items.TROPICAL_FISH ||
                item == Items.COOKED_COD ||
                item == Items.COOKED_SALMON;
    }

    private boolean isInRain(PlayerEntity player) {
        BlockPos blockpos = player.getPosition();
        return player.world.isRainingAt(blockpos) ||
                player.world.isRainingAt(new BlockPos(blockpos.getX(), player.getBoundingBox().maxY, blockpos.getZ()));
    }

    public void onEmptyClick(PlayerInteractEvent.RightClickEmpty event, Origin origin) {
        if (event.isCanceled()) return;

        Minecraft minecraft = Minecraft.getInstance();
        ClientPlayerEntity player = minecraft.player;
        if (player == null || !origin.getPlayer().getGameProfile().equals(player.getGameProfile())) return;
        DataManager dataManager = origin.getTypeDataManager();

        dataManager.set(RIGHT_CLICK_TIME, 0);
    }

    public void onClientTick(TickEvent.ClientTickEvent event, Origin origin) {
        if (event.isCanceled()) return;
        if (event.phase == TickEvent.Phase.START) return;
        if (origin.isServerSide()) return;

        Minecraft minecraft = Minecraft.getInstance();
        ClientPlayerEntity player = minecraft.player;
        if (!origin.getPlayer().equals(player)) return;
        DataManager dataManager = origin.getTypeDataManager();
        TimeTracker cooldownTracker = origin.getTimeTracker();
        Integer prevRightClickTime = dataManager.get(RIGHT_CLICK_TIME);

        boolean canRiptide = canRiptide(player);
        if (prevRightClickTime >= 0 && minecraft.gameSettings.keyBindUseItem.isKeyDown() && !player.isSneaking()) {
            dataManager.increase(RIGHT_CLICK_TIME, 1);
            if (player.isInWaterOrBubbleColumn()) {
                float prevPitch = dataManager.get(PREV_PITCH);
                float prevYaw = dataManager.get(PREV_YAW);
                applyFastSwim(player, prevPitch, prevYaw);
            }
        } else {
            if (canRiptide && !player.isSneaking() && !cooldownTracker.hasTimer(Cooldown.RIPTIDE_BOOST)) {
                if (prevRightClickTime > 0)
                    startRiptide(player, prevRightClickTime);
                cooldownTracker.addTimer(Cooldown.RIPTIDE_BOOST);
            }
            dataManager.set(RIGHT_CLICK_TIME, -1);
        }

        dataManager.set(PREV_PITCH, player.rotationPitch);
        dataManager.set(PREV_YAW, player.rotationYaw);

        float yawDiff = player.rotationYawHead - player.prevRotationYawHead;

        Float followTarget = dataManager.get(ROLL_FOLLOW_TARGET);
        if (canRiptide)
            dataManager.set(ROLL_FOLLOW_TARGET, (followTarget + yawDiff) * ROLL_FOLLOW_TARGET_REDUCTION);
        else
            dataManager.set(ROLL_FOLLOW_TARGET, followTarget * ROLL_FOLLOW_TARGET_REDUCTION);
    }

    private static boolean canRiptide(ClientPlayerEntity player) {
        return player.isInWaterOrBubbleColumn() && (player.isActualySwimming() || player.isElytraFlying());
    }

    private static void applyFastSwim(ClientPlayerEntity player, float prevPitch, float prevYaw) {
        Vector3d swimMotion = player.getMotion().scale(SWIM_OLD_MOVEMENT_REDUCTION).add(player.getLookVec().scale(SWIM_SPEED));
        if (swimMotion.lengthSquared() > MAX_SWIM_SPEED_SQUARED) return;

        double fastSwimBonus = getFastSwimBonus(player, prevPitch, prevYaw);
        Vector3d fastSwimMotion = swimMotion.scale(1 + fastSwimBonus);
        if (swimMotion.lengthSquared() > MAX_FAST_SWIM_SPEED_SQUARED) return;

        player.setMotion(fastSwimMotion);
    }

    private static float getFastSwimBonus(ClientPlayerEntity player, float prevPitch, float prevYaw) {
        return (Math.abs(prevYaw - player.rotationYaw) +
                Math.abs(prevPitch - player.rotationPitch) - FAST_SWIM_DEGREES) / FAST_SWIM_DEGREES + 1;
    }

    private void startRiptide(ClientPlayerEntity player, int rightClickTime) {

        int riptideStrength = MathHelper.clamp(rightClickTime / MAX_RIPTIDE_CHARGE_TIME, 0, 1) * 3;

        float yaw = player.rotationYaw;
        float pitch = player.rotationPitch;
        float velocityX = -MathHelper.sin(yaw * ((float)Math.PI / 180F)) * MathHelper.cos(pitch * ((float)Math.PI / 180F));
        float velocityY = -MathHelper.sin(pitch * ((float)Math.PI / 180F));
        float velocityZ = MathHelper.cos(yaw * ((float)Math.PI / 180F)) * MathHelper.cos(pitch * ((float)Math.PI / 180F));
        float speed = MathHelper.sqrt(velocityX * velocityX + velocityY * velocityY + velocityZ * velocityZ);
        float amount = 3 * ((1 + riptideStrength) / 4F);
        velocityX *= amount / speed;
        velocityY *= amount / speed;
        velocityZ *= amount / speed;
        player.addVelocity(velocityX, velocityY, velocityZ);
        player.startSpinAttack(20);
        if (player.isOnGround()) {
            player.move(MoverType.SELF, new Vector3d(0.0D, 1.1999999F, 0.0D));
        }

        SoundEvent riptideSound;
        if (riptideStrength >= 3) {
            riptideSound = SoundEvents.ITEM_TRIDENT_RIPTIDE_3;
        } else if (riptideStrength == 2) {
            riptideSound = SoundEvents.ITEM_TRIDENT_RIPTIDE_2;
        } else {
            riptideSound = SoundEvents.ITEM_TRIDENT_RIPTIDE_1;
        }

        SoundUtils.playMovingSound(player, riptideSound, SoundCategory.PLAYERS);
    }

}
