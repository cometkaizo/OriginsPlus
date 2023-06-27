package me.cometkaizo.origins.origin.client;

import me.cometkaizo.origins.Main;
import me.cometkaizo.origins.network.C2SArachnidAction;
import me.cometkaizo.origins.network.Packets;
import me.cometkaizo.origins.origin.ArachnidOriginType;
import me.cometkaizo.origins.origin.Origin;
import me.cometkaizo.origins.util.DataKey;
import me.cometkaizo.origins.util.DataManager;
import me.cometkaizo.origins.util.PhysicsUtils;
import me.cometkaizo.origins.util.TimeTracker;
import net.minecraft.block.BlockState;
import net.minecraft.block.SnowBlock;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.player.ClientPlayerEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.BlockRayTraceResult;
import net.minecraft.util.math.shapes.VoxelShape;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraft.world.World;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;

import static me.cometkaizo.origins.origin.ArachnidOriginType.IN_COBWEB;

public class ClientArachnidOriginType {
    public static final double WALL_CLIMB_SPEED = 0.12;
    public static final double WALL_SLOW_FALL_SPEED_AMP = 0.7;
    public static final double WALL_CLIMB_DISTANCE = 0.2;
    public static final double COBWEB_CLIMB_SPEED = 0.14;
    public static final double COBWEB_SLOW_FALL_SPEED_AMP = 0.6;
    public static final int GRAPPLE_SHOOT_TICK = 7;
    public static final int GRAPPLE_REACH = 30;
    public static final double GRAPPLE_PREV_MOTION_AMP = 0.75;
    public static final double GRAPPLE_DISENGAGE_DIST_SQ = 3 * 3;
    public static final OriginBarOverlayGui barOverlay = new OriginBarOverlayGui.Builder(OriginBarOverlayGui.Bar.POISON)
            .disappearWhenFull()
            .build();
    private static final DataKey<BlockPos.Mutable> WALL_CHECK_POS = DataKey.create(BlockPos.Mutable.class);
    private static final DataKey<Integer> GRAPPLE_TICK = DataKey.create(Integer.class);
    private static final DataKey<Vector3d> GRAPPLE_POS = DataKey.create(Vector3d.class);
    private static final DataKey<Double> PREV_GRAPPLE_DISTANCE = DataKey.create(Double.class);


    public static void onFirstActivate(Origin origin) {
        if (origin.isServerSide()) return;
        origin.getTypeData().register(IN_COBWEB, false);
        origin.getTypeData().register(WALL_CHECK_POS, new BlockPos.Mutable());
        origin.getTypeData().register(GRAPPLE_TICK, -1);
        origin.getTypeData().register(GRAPPLE_POS, null);
        origin.getTypeData().register(PREV_GRAPPLE_DISTANCE, 0D);
    }

    public static void onActivate(Origin origin) {
        if (origin.isServerSide()) return;
        barOverlay.start();
    }

    public static void onDeactivate(Origin origin) {
        if (origin.isServerSide()) return;
        barOverlay.stop();
    }

    public static void onPlayerSensitiveEvent(Object event, Origin origin) {
        if (event instanceof TickEvent.ClientTickEvent) {
            onClientTick((TickEvent.ClientTickEvent) event, origin);
        } else if (event instanceof PlayerInteractEvent.RightClickEmpty) {
            onEmptyClick((PlayerInteractEvent.RightClickEmpty) event, origin);
        }
    }

    private static void onEmptyClick(PlayerInteractEvent.RightClickEmpty event, Origin origin) {
        if (origin.isServerSide()) return;
        if (origin.getTypeData().get(GRAPPLE_TICK) < 0)
            origin.getTypeData().set(GRAPPLE_TICK, 0);
    }

    private static void onClientTick(TickEvent.ClientTickEvent event, Origin origin) {
        if (event.phase == TickEvent.Phase.START) return;
        if (origin.isServerSide()) return;
        ClientPlayerEntity player = Minecraft.getInstance().player;
        if (!origin.getPlayer().equals(player)) return;

        updateBarOverlay(origin.getTimeTracker());
        tryClimb(origin, player);
        updateGrapple(origin, player);
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



    private static void updateGrapple(Origin origin, ClientPlayerEntity player) {
        DataManager data = origin.getTypeData();
        final int tick = data.get(GRAPPLE_TICK);
        if (tick == -1) return;
        Vector3d grapplePos = data.get(GRAPPLE_POS);
        double prevDistance = data.get(PREV_GRAPPLE_DISTANCE);

        Vector3d playerPos = player.getPositionVec();

        if (!Minecraft.getInstance().gameSettings.keyBindUseItem.isKeyDown()) {
            stopGrapple(player, data);
            return;
        }

        if (tick < GRAPPLE_SHOOT_TICK) {
            // slow motion effect because it helps you aim and also is cool
            player.setMotion(player.getMotion().scale(0.75));
        } else if (tick == GRAPPLE_SHOOT_TICK) {
            BlockRayTraceResult hitResult = PhysicsUtils.rayCastFromEntity(player.world, player, GRAPPLE_REACH);
            if (hitResult == null) {
                Main.LOGGER.info("Cancelled because BlockRayTraceResult was found to be null during ray-casting");
                stopGrapple(player, data);
                return;
            }
            grapplePos = hitResult.getHitVec();

            Main.LOGGER.info("X: " +
                    grapplePos.getX() + ", Y: " + grapplePos.getY() + ", Z: " + grapplePos.getZ()
            );
        }

        if (tick >= GRAPPLE_SHOOT_TICK) { // change to wait for web to actually hit the block
            Vector3d prevMotion = player.getMotion();

            if (!player.isSneaking()) {
                if (playerPos.squareDistanceTo(grapplePos) <= GRAPPLE_DISENGAGE_DIST_SQ) {
                    stopGrapple(player, data);
                    return;
                }
                player.setNoGravity(true);
                Vector3d pullMotion = PhysicsUtils.getVelocityTowards(playerPos, grapplePos, 0.3);
                player.setMotion(prevMotion
                        .scale(GRAPPLE_PREV_MOTION_AMP)
                        .add(pullMotion));
            } else {
                player.setNoGravity(false);

                Vector3d oldDeltaMovement = player.getMotion();

                double distance = playerPos.distanceTo(grapplePos);

                /*double correction = prevDistance == -1 ? 0 : distance - prevDistance;
                Main.LOGGER.info(
                        "JC distance to center: {}, Previous distance to center: {}, Difference: {}",
                        distance,
                        prevDistance,
                        correction
                );
*/
                //if (prevDistance == -1) {
                prevDistance = distance;
                //}

                Vector3d nextVectorFromCenter = playerPos
                        .add(player.getMotion().scale(1.08))
                        .subtract(grapplePos);
                Vector3d nextSwingPosition = grapplePos
                        .add(nextVectorFromCenter
                                .normalize()
                                .scale(distance * 0.95 /*- correction*/)
                        );
                player.setMotion(nextSwingPosition.subtract(playerPos).scale(1.05));


                Main.LOGGER.info("Distance from center: " + distance +
                        ", Delta movement (this tick): " + oldDeltaMovement +
                        ", Next vector from center: " + nextVectorFromCenter +
                        ", Next vector from center normalized: " + nextVectorFromCenter.normalize() +
                        ", Next swing position: " + nextSwingPosition +
                        ", Delta movement: " + player.getMotion());

            }

        }

        data.increase(GRAPPLE_TICK, 1);
        data.set(GRAPPLE_POS, grapplePos);
        data.set(PREV_GRAPPLE_DISTANCE, prevDistance);
    }

    private static void stopGrapple(ClientPlayerEntity player, DataManager data) {
        player.setNoGravity(false);
        data.set(GRAPPLE_TICK, -1);
        data.set(GRAPPLE_POS, null);
        data.set(PREV_GRAPPLE_DISTANCE, 0D);
    }
}
