package me.cometkaizo.origins.origin.client;

import me.cometkaizo.origins.origin.Origin;
import me.cometkaizo.origins.origin.SlimicianOriginType;
import me.cometkaizo.origins.util.DataKey;
import me.cometkaizo.origins.util.DataManager;
import me.cometkaizo.origins.util.TimeTracker;
import net.minecraft.client.entity.player.ClientPlayerEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.MovementInput;
import net.minecraft.util.SoundEvents;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraft.world.World;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.event.TickEvent;

import java.util.Random;

import static net.minecraft.util.math.MathHelper.lerp;

@OnlyIn(Dist.CLIENT)
public class ClientSlimicianOriginType {
    private static final Random RANDOM = new Random();
    public static final double FALL_BOUNCE_HEIGHT_AMP = 0.75;
    private static final double FALL_SUPER_BOUNCE_HEIGHT_AMP = 1.1;
    public static final double LATE_SUPER_BOUNCE_Y_AMP = 2;//FALL_SUPER_BOUNCE_HEIGHT_AMP / FALL_BOUNCE_HEIGHT_AMP;
    public static final double WALL_SLIDE_DISTANCE = 0.2;
    public static final double SLIDE_SPEED = -0.05D;
    public static final double SMOOTH_SLIDE_SPEED = -0.2D;
    protected static final DataKey<Boolean> CLIENT_PREV_ON_GROUND = DataKey.create(Boolean.class);
    protected static final DataKey<Boolean> CLIENT_PREV_JUMPING = DataKey.create(Boolean.class);
    protected static final DataKey<Double> CLIENT_PREV_MOTION_Y = DataKey.create(Double.class);
    private static final DataKey<BlockPos.Mutable> WALL_CHECK_POS = DataKey.create(BlockPos.Mutable.class);


    public static void onFirstActivate(Origin origin) {
        if (origin.isServerSide()) return;
        origin.getTypeDataManager().register(CLIENT_PREV_ON_GROUND, true);
        origin.getTypeDataManager().register(CLIENT_PREV_JUMPING, true);
        origin.getTypeDataManager().register(CLIENT_PREV_MOTION_Y, 0D);
        origin.getTypeDataManager().register(WALL_CHECK_POS, new BlockPos.Mutable());
    }

    public static void onPlayerSensitiveEvent(Object event, Origin origin) {
        if (event instanceof TickEvent.ClientTickEvent) {
            onClientTick((TickEvent.ClientTickEvent) event, origin);
        }
    }


    private static void onClientTick(TickEvent.ClientTickEvent event, Origin origin) {
        if (event.phase == TickEvent.Phase.START) return;
        if (origin.isServerSide()) return;
        ClientPlayerEntity player = (ClientPlayerEntity) origin.getPlayer();
        DataManager dataManager = origin.getTypeDataManager();
        MovementInput input = player.movementInput;

        applySlide(player, origin);
        applyBounce(player, origin);

        dataManager.set(CLIENT_PREV_JUMPING, input.jump);
        dataManager.set(CLIENT_PREV_ON_GROUND, player.isOnGround());
        dataManager.set(CLIENT_PREV_MOTION_Y, player.getMotion().y);
    }

    private static void applyBounce(ClientPlayerEntity player, Origin origin) {
        DataManager dataManager = origin.getTypeDataManager();
        TimeTracker timeTracker = origin.getTimeTracker();
        MovementInput input = player.movementInput;

        if (player.isSuppressingBounce()) return;

        if (player.isOnGround() && !prevOnGround(dataManager)) {
            if (timeTracker.hasTimer(SlimicianOriginType.Timer.JUMPED)) {
                superBounce(player, dataManager);
                timeTracker.remove(SlimicianOriginType.Timer.JUMPED);
            } else {
                bounce(player, dataManager, FALL_BOUNCE_HEIGHT_AMP);
            }
            timeTracker.addTimer(SlimicianOriginType.Timer.BOUNCED);
        } else if (timeTracker.hasTimer(SlimicianOriginType.Timer.JUMPED) && timeTracker.hasTimer(SlimicianOriginType.Timer.BOUNCED)) {
            player.setMotion(player.getMotion().mul(1, LATE_SUPER_BOUNCE_Y_AMP, 1));
            timeTracker.remove(SlimicianOriginType.Timer.JUMPED);
        } else {
            if (input.jump && !prevJumped(dataManager))
                timeTracker.addTimer(SlimicianOriginType.Timer.JUMPED);
        }
    }


    private static Boolean prevOnGround(DataManager dataManager) {
        return dataManager.get(CLIENT_PREV_ON_GROUND);
    }
    private static Boolean prevJumped(DataManager dataManager) {
        return dataManager.get(CLIENT_PREV_JUMPING);
    }

    private static void superBounce(ClientPlayerEntity player, DataManager dataManager) {
        bounce(player, dataManager, FALL_SUPER_BOUNCE_HEIGHT_AMP);
    }

    private static void bounce(PlayerEntity player, DataManager dataManager, double fallBounceHeightAmp) {
        Vector3d motion = player.getMotion();
        double prevMotionY = dataManager.get(CLIENT_PREV_MOTION_Y);
        if (prevMotionY < -0.2) {
            player.setMotion(motion.x, -prevMotionY * fallBounceHeightAmp, motion.z);
        }
    }

    private static void applySlide(PlayerEntity player, Origin origin) {
        if (player.collidedHorizontally || isNextToWall(player, origin)) {
            if (player.isSneaking()) {
                applySlideVelocity(player);
                applySlideEffects(player);
            } else {
                setSmoothSlideVelocity(player);
                smoothSlideEffects(player);
            }
        }
    }

    private static boolean isNextToWall(PlayerEntity player, Origin origin) {
        double x = player.getPosX();
        double y = player.getPosY();
        double z = player.getPosZ();
        AxisAlignedBB boundingBox = player.getBoundingBox();
        double halfWidth = (boundingBox.maxX - boundingBox.minX) / 2;
        double halfDepth = (boundingBox.maxZ - boundingBox.minZ) / 2;
        World world = player.world;

        BlockPos.Mutable wallCheckPos = origin.getTypeDataManager().get(WALL_CHECK_POS);

        return world.getBlockState(wallCheckPos.setPos(x + halfWidth + WALL_SLIDE_DISTANCE, y, z + halfDepth + WALL_SLIDE_DISTANCE)).isSolid() ||
                world.getBlockState(wallCheckPos.setPos(x + halfWidth + WALL_SLIDE_DISTANCE, y, z - halfDepth - WALL_SLIDE_DISTANCE)).isSolid() ||
                world.getBlockState(wallCheckPos.setPos(x - halfWidth - WALL_SLIDE_DISTANCE, y, z + halfDepth + WALL_SLIDE_DISTANCE)).isSolid() ||
                world.getBlockState(wallCheckPos.setPos(x - halfWidth - WALL_SLIDE_DISTANCE, y, z - halfDepth - WALL_SLIDE_DISTANCE)).isSolid();
    }

    private static void applySlideVelocity(PlayerEntity player) {
        Vector3d motion = player.getMotion();
        if (motion.y < -0.13D) {
            double d0 = SLIDE_SPEED / motion.y;
            player.setMotion(motion.x * d0, SLIDE_SPEED, motion.z * d0);
        } else {
            player.setMotion(motion.x, SLIDE_SPEED, motion.z);
        }

        player.fallDistance = 0.0F;
    }

    private static void applySlideEffects(PlayerEntity player) {
        if (RANDOM.nextInt(5) == 0) {
            player.playSound(SoundEvents.BLOCK_HONEY_BLOCK_SLIDE, 1.0F, 1.0F);
        }

        if (!player.world.isRemote && RANDOM.nextInt(5) == 0) {
            player.world.setEntityState(player, (byte)53);
        }
    }

    private static void setSmoothSlideVelocity(PlayerEntity player) {
        Vector3d motion = player.getMotion();
        if (motion.y < -0.1D) {
            double d0 = SMOOTH_SLIDE_SPEED / motion.y;
            player.setMotion(motion.x * d0, SMOOTH_SLIDE_SPEED, motion.z * d0);
        } else {
            player.setMotion(motion.x, lerp(0.01, motion.y, SMOOTH_SLIDE_SPEED), motion.z);
        }

        player.fallDistance = 0.0F;
    }

    private static void smoothSlideEffects(PlayerEntity player) {
        if (RANDOM.nextInt(20) == 0) {
            player.playSound(SoundEvents.BLOCK_HONEY_BLOCK_SLIDE, 1.0F, 1.0F);
        }

        if (!player.world.isRemote && RANDOM.nextInt(5) == 0) {
            player.world.setEntityState(player, (byte)53);
        }
    }


}
