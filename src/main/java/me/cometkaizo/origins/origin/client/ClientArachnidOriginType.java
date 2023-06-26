package me.cometkaizo.origins.origin.client;

import me.cometkaizo.origins.network.C2SArachnidAction;
import me.cometkaizo.origins.network.Packets;
import me.cometkaizo.origins.origin.ArachnidOriginType;
import me.cometkaizo.origins.origin.Origin;
import me.cometkaizo.origins.util.DataKey;
import me.cometkaizo.origins.util.TimeTracker;
import net.minecraft.block.BlockState;
import net.minecraft.block.SnowBlock;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.player.ClientPlayerEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.shapes.VoxelShape;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraft.world.World;
import net.minecraftforge.event.TickEvent;

import static me.cometkaizo.origins.origin.ArachnidOriginType.IN_COBWEB;

public class ClientArachnidOriginType {
    public static final double WALL_CLIMB_SPEED = 0.12;
    public static final double WALL_SLOW_FALL_SPEED_AMP = 0.7;
    public static final double WALL_CLIMB_DISTANCE = 0.2;
    public static final double COBWEB_CLIMB_SPEED = 0.14;
    public static final double COBWEB_SLOW_FALL_SPEED_AMP = 0.6;
    public static final OriginBarOverlayGui barOverlay = new OriginBarOverlayGui.Builder(OriginBarOverlayGui.Bar.POISON)
            .disappearWhenFull()
            .build();
    private static final DataKey<BlockPos.Mutable> WALL_CHECK_POS = DataKey.create(BlockPos.Mutable.class);


    public static void onFirstActivate(Origin origin) {
        if (origin.isServerSide()) return;
        origin.getTypeData().register(IN_COBWEB, false);
        origin.getTypeData().register(WALL_CHECK_POS, new BlockPos.Mutable());
    }

    public static void onActivate(Origin origin) {
        barOverlay.start();
    }

    public static void onDeactivate(Origin origin) {
        barOverlay.stop();
    }

    public static void onPlayerSensitiveEvent(Object event, Origin origin) {
        if (event instanceof TickEvent.ClientTickEvent) {
            onClientTick((TickEvent.ClientTickEvent) event, origin);
        }
    }

    private static void onClientTick(TickEvent.ClientTickEvent event, Origin origin) {
        if (event.phase == TickEvent.Phase.START) return;
        if (origin.isServerSide()) return;
        ClientPlayerEntity player = Minecraft.getInstance().player;
        if (!origin.getPlayer().equals(player)) return;

        updateBarOverlay(origin.getTimeTracker());
        tryClimb(origin, player);
    }

    private static void updateBarOverlay(TimeTracker timeTracker) {
        barOverlay.setBarPercent(timeTracker.getTimerPercentage(ArachnidOriginType.Cooldown.AOE_POISON));
    }

    private static void tryClimb(Origin origin, ClientPlayerEntity player) {
        Vector3d motion = player.getMotion();
        if (player.collidedHorizontally || isNextToWall(origin)) {
            if (shouldStopOnWall(player)) {
                player.setMotion(Vector3d.ZERO);
            } else if (shouldClimbWall(player)) {
                player.setMotion(motion.x, Math.max(motion.y, WALL_CLIMB_SPEED), motion.z);
            } else {
                player.setMotion(motion.x, motion.y * WALL_SLOW_FALL_SPEED_AMP, motion.z);
            }
            resetFallDistance(player);
        }
        if (isInCobweb(origin)) {
            if (shouldStopOnWall(player)) {
                player.setMotion(motion.x, 0, motion.z);
            } else if (shouldClimbWall(player)) {
                player.setMotion(motion.x, Math.max(motion.y, COBWEB_CLIMB_SPEED), motion.z);
            } else {
                player.setMotion(motion.x, motion.y * COBWEB_SLOW_FALL_SPEED_AMP, motion.z);
            }
            resetFallDistance(player);
        }

        origin.getTypeData().set(IN_COBWEB, false);
    }

    private static void resetFallDistance(ClientPlayerEntity player) {
        player.fallDistance = 0;
        Packets.sendToServer(new C2SArachnidAction(ArachnidOriginType.Action.RESET_FALL_DISTANCE));
    }

    private static boolean isInCobweb(Origin origin) {
        return origin.getTypeData().get(IN_COBWEB);
    }

    private static boolean isNextToWall(Origin origin) {
        PlayerEntity player = origin.getPlayer();
        double x = player.getPosX();
        double y = player.getPosY();
        double z = player.getPosZ();
        AxisAlignedBB boundingBox = player.getBoundingBox();
        double halfWidth = (boundingBox.maxX - boundingBox.minX) / 2;
        double halfDepth = (boundingBox.maxZ - boundingBox.minZ) / 2;
        World world = player.world;

//        Main.LOGGER.info("origin: {}, dimension: {}", origin, player.world.getDimensionKey());

        BlockPos.Mutable wallCheckPos = origin.getTypeData().get(WALL_CHECK_POS);

        return isNextTo(player, world, wallCheckPos.setPos(x + halfWidth + WALL_CLIMB_DISTANCE, y, z + halfDepth + WALL_CLIMB_DISTANCE)) ||
                isNextTo(player, world, wallCheckPos.setPos(x + halfWidth + WALL_CLIMB_DISTANCE, y, z - halfDepth - WALL_CLIMB_DISTANCE)) ||
                isNextTo(player, world, wallCheckPos.setPos(x - halfWidth - WALL_CLIMB_DISTANCE, y, z + halfDepth + WALL_CLIMB_DISTANCE)) ||
                isNextTo(player, world, wallCheckPos.setPos(x - halfWidth - WALL_CLIMB_DISTANCE, y, z - halfDepth - WALL_CLIMB_DISTANCE));
    }

    private static boolean isNextTo(PlayerEntity player, World world, BlockPos position) {
        BlockState blockState = world.getBlockState(position);
        if (shouldNotSlideOn(blockState)) return false;

        VoxelShape collisionShape = blockState.getCollisionShape(world, position);
        if (collisionShape.isEmpty()) return false;

        return collisionShape.getBoundingBox()
                .offset(position)
                .grow(WALL_CLIMB_DISTANCE)
                .intersects(player.getBoundingBox());
    }

    private static boolean shouldNotSlideOn(BlockState blockState) {
        return blockState.getBlock() instanceof SnowBlock && blockState.get(SnowBlock.LAYERS) == 1;
    }

    private static boolean shouldClimbWall(ClientPlayerEntity player) {
        return (player.collidedHorizontally || player.movementInput.jump) && !player.isSwimming() && !player.isElytraFlying();
    }

    private static boolean shouldStopOnWall(ClientPlayerEntity player) {
        return player.isSneaking();
    }
}
