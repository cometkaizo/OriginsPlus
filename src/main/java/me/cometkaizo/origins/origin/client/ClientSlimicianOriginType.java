package me.cometkaizo.origins.origin.client;

import com.mojang.blaze3d.matrix.MatrixStack;
import me.cometkaizo.origins.network.C2SSlimicianAction;
import me.cometkaizo.origins.network.Packets;
import me.cometkaizo.origins.origin.Origin;
import me.cometkaizo.origins.origin.SlimicianOriginType;
import me.cometkaizo.origins.util.DataKey;
import me.cometkaizo.origins.util.DataManager;
import me.cometkaizo.origins.util.TimeTracker;
import net.minecraft.block.BlockState;
import net.minecraft.block.SnowBlock;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.player.ClientPlayerEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.MovementInput;
import net.minecraft.util.SoundEvents;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.shapes.VoxelShape;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraft.world.World;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.event.RenderPlayerEvent;
import net.minecraftforge.event.TickEvent;

import java.util.Random;

import static me.cometkaizo.origins.origin.SlimicianOriginType.SIZE_SHRINK_FACTOR;
import static me.cometkaizo.origins.origin.SlimicianOriginType.getShrinkCount;
import static net.minecraft.util.math.MathHelper.lerp;

@OnlyIn(Dist.CLIENT)
public class ClientSlimicianOriginType {
    private static final Random RANDOM = new Random();
    public static final double MIN_FALL_DIST_TO_BOUNCE = 0.75;
    public static final double WALL_SLIDE_DISTANCE = 0.2;
    public static final double SLIDE_SPEED = -0.05D;
    public static final double SMOOTH_SLIDE_SPEED = -0.2D;
    public static final DataKey<MatrixStack> PREV_RENDER_MATRIX_STACK = DataKey.create(MatrixStack.class);
    protected static final DataKey<Boolean> PREV_ON_GROUND = DataKey.create(Boolean.class);
    protected static final DataKey<Boolean> PREV_JUMPING = DataKey.create(Boolean.class);
    protected static final DataKey<Double> PREV_NEG_MOTION_Y = DataKey.create(Double.class);
    protected static final DataKey<Double> PREV_POS_X = DataKey.create(Double.class);
    protected static final DataKey<Double> PREV_POS_Z = DataKey.create(Double.class);
    protected static final DataKey<Boolean> NEXT_TO_WALL = DataKey.create(Boolean.class);
    protected static final DataKey<Double> PREV_BOUNCE_MOTION_Y = DataKey.create(Double.class);
    protected static final DataKey<Float> PREV_FALL_DISTANCE = DataKey.create(Float.class);
    private static final DataKey<BlockPos.Mutable> WALL_CHECK_POS = DataKey.create(BlockPos.Mutable.class);


    public static double getBounceHeightAmp(Origin origin) {
        switch (getShrinkCount(origin)) {
            case 0: return 0.75;
            case 1: return 0.8;
            case 3:
            default: return 0.9;
        }
    }
    public static double getSuperBounceHeightAmp(Origin origin) {
        switch (getShrinkCount(origin)) {
            case 0: return 1.1;
            case 1: return 1.2;
            case 3:
            default: return 1.3;
        }
    }
    public static double getLateSuperBounceYAmp(Origin origin) {
        return 1 / getBounceHeightAmp(origin) * getSuperBounceHeightAmp(origin);
    }


    public static void onFirstActivate(Origin origin) {
        if (origin.isServerSide()) return;
        origin.getTypeData().register(PREV_ON_GROUND, true);
        origin.getTypeData().register(PREV_JUMPING, true);
        origin.getTypeData().register(PREV_NEG_MOTION_Y, 0D);
        origin.getTypeData().register(PREV_POS_X, 0D);
        origin.getTypeData().register(PREV_POS_Z, 0D);
        origin.getTypeData().register(NEXT_TO_WALL, false);
        origin.getTypeData().register(PREV_BOUNCE_MOTION_Y, 0D);
        origin.getTypeData().register(PREV_FALL_DISTANCE, 0F);
        origin.getTypeData().register(WALL_CHECK_POS, new BlockPos.Mutable());
        origin.getTypeData().register(PREV_RENDER_MATRIX_STACK, null);
    }

    public static void onPlayerSensitiveEvent(Object event, Origin origin) {
        if (event instanceof TickEvent.ClientTickEvent) {
            onClientTick((TickEvent.ClientTickEvent) event, origin);
        }
    }


    private static void onClientTick(TickEvent.ClientTickEvent event, Origin origin) {
        if (event.isCanceled()) return;
        if (origin.isServerSide()) return;
        if (event.phase == TickEvent.Phase.START) return;
        ClientPlayerEntity player = (ClientPlayerEntity) origin.getPlayer();
        DataManager dataManager = origin.getTypeData();
        MovementInput input = player.movementInput;

        dataManager.set(SlimicianOriginType.BOUNCED_THIS_TICK, false);

        applySlide(player, origin);
        applyBounce(player, origin);

        if (horizontalPosChanged(origin)) dataManager.set(NEXT_TO_WALL, player.collidedHorizontally);
        dataManager.set(PREV_JUMPING, input.jump);
        dataManager.set(PREV_ON_GROUND, player.isOnGround());
        dataManager.set(PREV_FALL_DISTANCE, player.fallDistance);
        if (player.getMotion().y <= 0) dataManager.set(PREV_NEG_MOTION_Y, player.getMotion().y);
        dataManager.set(PREV_POS_X, player.getPosX());
        dataManager.set(PREV_POS_Z, player.getPosZ());
    }

    private static boolean horizontalPosChanged(Origin origin) {
        DataManager dataManager = origin.getTypeData();
        PlayerEntity player = origin.getPlayer();
        double prevPosX = dataManager.get(PREV_POS_X);
        double prevPosZ = dataManager.get(PREV_POS_Z);
        return player.getPosX() != prevPosX || player.getPosX() != prevPosZ;
    }

    private static void applyBounce(ClientPlayerEntity player, Origin origin) {
        DataManager dataManager = origin.getTypeData();
        TimeTracker timeTracker = origin.getTimeTracker();
        MovementInput input = player.movementInput;

        if (player.isSuppressingBounce()) return;

        if (input.jump && !prevJumped(dataManager))
            timeTracker.addTimer(SlimicianOriginType.Timer.JUMPED);

        if (player.isOnGround() && !prevOnGround(dataManager)) {
            if (timeTracker.hasTimer(SlimicianOriginType.Timer.JUMPED)) {
                superBounce(origin);
                timeTracker.remove(SlimicianOriginType.Timer.JUMPED);
            } else {
                bounce(player, dataManager, getBounceHeightAmp(origin));
            }
            timeTracker.addTimer(SlimicianOriginType.Timer.BOUNCED);
        } else if (timeTracker.hasTimer(SlimicianOriginType.Timer.JUMPED) && timeTracker.hasTimer(SlimicianOriginType.Timer.BOUNCED)) {
            lateSuperBounce(origin);
        }
    }

    private static void lateSuperBounce(Origin origin) {
        PlayerEntity player = origin.getPlayer();
        DataManager dataManager = origin.getTypeData();
        TimeTracker timeTracker = origin.getTimeTracker();
        double prevBounceMotionY = dataManager.get(PREV_BOUNCE_MOTION_Y);
        player.setMotion(new Vector3d(player.getMotion().x, prevBounceMotionY * getLateSuperBounceYAmp(origin), player.getMotion().z));
        timeTracker.remove(SlimicianOriginType.Timer.JUMPED);
    }


    private static Boolean prevOnGround(DataManager dataManager) {
        return dataManager.get(PREV_ON_GROUND);
    }
    private static Boolean prevJumped(DataManager dataManager) {
        return dataManager.get(PREV_JUMPING);
    }

    private static void superBounce(Origin origin) {
        PlayerEntity player = origin.getPlayer();
        DataManager dataManager = origin.getTypeData();
        bounce(player, dataManager, getSuperBounceHeightAmp(origin));
    }

    private static void bounce(PlayerEntity player, DataManager dataManager, double fallBounceHeightAmp) {
        Vector3d motion = player.getMotion();
        double prevMotionY = dataManager.get(PREV_NEG_MOTION_Y);
        float fallDistance = dataManager.get(PREV_FALL_DISTANCE);
        if (prevMotionY < -0.2 && fallDistance >= MIN_FALL_DIST_TO_BOUNCE) {
            double newMotionY = -prevMotionY * fallBounceHeightAmp;
            player.setMotion(motion.x, newMotionY, motion.z);

            dataManager.set(PREV_BOUNCE_MOTION_Y, newMotionY);
            dataManager.set(SlimicianOriginType.BOUNCED_THIS_TICK, true);
            resetFallDistance(player);
        }
    }

    private static void applySlide(PlayerEntity player, Origin origin) {
        if (player.collidedHorizontally || isNextToWall(player, origin)) {
            if (player.isSneaking()) {
                applySlideVelocity(player);
                applySlideEffects(player);
            } else {
                applySmoothSlideVelocity(player);
                applySmoothSlideEffects(player);
            }

            resetFallDistance(player);
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

        BlockPos.Mutable wallCheckPos = origin.getTypeData().get(WALL_CHECK_POS);

        return isNextTo(player, world, wallCheckPos.setPos(x + halfWidth + WALL_SLIDE_DISTANCE, y, z + halfDepth + WALL_SLIDE_DISTANCE)) ||
                isNextTo(player, world, wallCheckPos.setPos(x + halfWidth + WALL_SLIDE_DISTANCE, y, z - halfDepth - WALL_SLIDE_DISTANCE)) ||
                isNextTo(player, world, wallCheckPos.setPos(x - halfWidth - WALL_SLIDE_DISTANCE, y, z + halfDepth + WALL_SLIDE_DISTANCE)) ||
                isNextTo(player, world, wallCheckPos.setPos(x - halfWidth - WALL_SLIDE_DISTANCE, y, z - halfDepth - WALL_SLIDE_DISTANCE));
    }

    private static boolean isNextTo(PlayerEntity player, World world, BlockPos position) {
        BlockState blockState = world.getBlockState(position);
        if (shouldNotSlideOn(blockState)) return false;

        VoxelShape collisionShape = blockState.getCollisionShape(world, position);
        if (collisionShape.isEmpty()) return false;

        return collisionShape.getBoundingBox()
                .offset(position)
                .grow(WALL_SLIDE_DISTANCE)
                .intersects(player.getBoundingBox());
    }

    private static boolean shouldNotSlideOn(BlockState blockState) {
        return blockState.getBlock() instanceof SnowBlock && blockState.get(SnowBlock.LAYERS) == 1;
    }

    private static void applySlideVelocity(PlayerEntity player) {
        Vector3d motion = player.getMotion();
        if (motion.y < -0.13D) {
            double d0 = SLIDE_SPEED / motion.y;
            player.setMotion(motion.x * d0, SLIDE_SPEED, motion.z * d0);
        } else {
            player.setMotion(motion.x, SLIDE_SPEED, motion.z);
        }
    }

    private static void resetFallDistance(PlayerEntity player) {
        player.fallDistance = 0;
        if (Minecraft.getInstance().getConnection() != null)
            Packets.sendToServer(new C2SSlimicianAction(SlimicianOriginType.Action.RESET_FALL_DISTANCE));
    }

    private static void applySlideEffects(PlayerEntity player) {
        if (RANDOM.nextInt(5) == 0) {
            player.playSound(SoundEvents.BLOCK_HONEY_BLOCK_SLIDE, 1.0F, 1.0F);
        }

        if (!player.world.isRemote && RANDOM.nextInt(5) == 0) {
            player.world.setEntityState(player, (byte)53);
        }
    }

    private static void applySmoothSlideVelocity(PlayerEntity player) {
        Vector3d motion = player.getMotion();
        if (motion.y < -0.1D) {
            double d0 = SMOOTH_SLIDE_SPEED / motion.y;
            player.setMotion(motion.x * d0, SMOOTH_SLIDE_SPEED, motion.z * d0);
        } else {
            player.setMotion(motion.x, lerp(0.01, motion.y, SMOOTH_SLIDE_SPEED), motion.z);
        }
    }

    private static void applySmoothSlideEffects(PlayerEntity player) {
        if (RANDOM.nextInt(20) == 0) {
            player.playSound(SoundEvents.BLOCK_HONEY_BLOCK_SLIDE, 1.0F, 1.0F);
        }

        if (!player.world.isRemote && RANDOM.nextInt(5) == 0) {
            player.world.setEntityState(player, (byte)53);
        }
    }

    public static void onRenderPlayer(RenderPlayerEvent.Pre event, Origin origin) {
        MatrixStack stack = event.getMatrixStack();
        if (stack == origin.getTypeData().get(PREV_RENDER_MATRIX_STACK)) return;

        int shrinkCount = getShrinkCount(origin);
        float sizeFactor = (float) Math.pow(SIZE_SHRINK_FACTOR, shrinkCount);
        //Main.LOGGER.info("prt: {}, origin: {}, stack: {}, shrink count: {}, factor: {}",
        //        event.getPartialRenderTick(), origin, stack, shrinkCount, sizeFactor);

        stack.scale(sizeFactor, sizeFactor, sizeFactor);
        origin.getTypeData().set(PREV_RENDER_MATRIX_STACK, stack);
    }

}
