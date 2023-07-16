package me.cometkaizo.origins.origin.client;

import com.mojang.blaze3d.matrix.MatrixStack;
import com.mojang.blaze3d.vertex.IVertexBuilder;
import me.cometkaizo.origins.Main;
import me.cometkaizo.origins.network.C2SArachnidAction;
import me.cometkaizo.origins.network.Packets;
import me.cometkaizo.origins.origin.ArachnidOriginType;
import me.cometkaizo.origins.origin.Origin;
import me.cometkaizo.origins.util.*;
import net.minecraft.block.BlockState;
import net.minecraft.block.SnowBlock;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.player.ClientPlayerEntity;
import net.minecraft.client.renderer.IRenderTypeBuffer;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.SoundEvents;
import net.minecraft.util.math.*;
import net.minecraft.util.math.shapes.VoxelShape;
import net.minecraft.util.math.vector.Matrix4f;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraft.world.LightType;
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
    public static final double MIN_GRAPPLE_DISENGAGE_DIST_SQ = 3 * 3;
    public static final double MAX_GRAPPLE_DISENGAGE_DIST_SQ = GRAPPLE_REACH * GRAPPLE_REACH;
    public static final OriginBarOverlayGui barOverlay = new OriginBarOverlayGui.Builder(OriginBarOverlayGui.Bar.POISON)
            .disappearWhenFull()
            .build();
    private static final DataKey<BlockPos.Mutable> WALL_CHECK_POS = DataKey.create(BlockPos.Mutable.class);
    private static final DataKey<Integer> GRAPPLE_TICK = DataKey.create(Integer.class);
    private static final DataKey<Vector3d> GRAPPLE_BLOCK = DataKey.create(Vector3d.class);
    private static final DataKey<Entity> GRAPPLE_ENTITY = DataKey.create(Entity.class);


    public static void onFirstActivate(Origin origin) {
        if (origin.isServerSide()) return;
        origin.getTypeData().register(IN_COBWEB, false);
        origin.getTypeData().register(WALL_CHECK_POS, new BlockPos.Mutable());
        origin.getTypeData().register(GRAPPLE_TICK, -1);
        origin.getTypeData().register(GRAPPLE_BLOCK, null);
        origin.getTypeData().register(GRAPPLE_ENTITY, null);
    }

    public static void onActivate(Origin origin) {
        if (!origin.isPhysicalClient()) return;
        barOverlay.start();
    }

    public static void onDeactivate(Origin origin) {
        if (!origin.isPhysicalClient()) return;
        barOverlay.stop();
    }

    public static void onEvent(Object event, Origin origin) {

        // TODO: 2023-06-29 Disabled until finished
        /*
        if (event instanceof RenderPlayerEvent.Pre) {
            RenderPlayerEvent.Pre e = (RenderPlayerEvent.Pre) event;
            PlayerEntity player = e.getPlayer();
            MatrixStack stack = e.getMatrixStack();
            IRenderTypeBuffer buffer = e.getBuffers();
            float partialTicks = e.getPartialRenderTick();
            renderGrapple(player, stack, buffer, partialTicks); // (Origin might not match player origin)
        }*/
    }

    public static void onPlayerSensitiveEvent(Object event, Origin origin) {
        if (event instanceof TickEvent.ClientTickEvent) {
            onClientTick((TickEvent.ClientTickEvent) event, origin);
        } else if (event instanceof PlayerInteractEvent.RightClickEmpty) {
            startGrapple(origin);
        }
    }

    private static void startGrapple(Origin origin) {
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

        Vector3d playerPos = player.getPositionVec();

        if (!Minecraft.getInstance().gameSettings.keyBindUseItem.isKeyDown()) {
            stopGrapple(player, data);
            return;
        }

        if (tick < GRAPPLE_SHOOT_TICK) {
            applySlowMotion(player);
        } else if (tick == GRAPPLE_SHOOT_TICK) {
            SoundUtils.playSound(player, SoundEvents.ENTITY_FISHING_BOBBER_RETRIEVE, SoundCategory.PLAYERS);
            SoundUtils.playSound(player, SoundEvents.ENTITY_ARROW_SHOOT, SoundCategory.PLAYERS, 0.7F, 0.8F);
            setGrappleTargetPos(player, data, playerPos);
        }

        Vector3d grapplePos = getGrapplePos(data);

        if (grapplePos != null && tick >= GRAPPLE_SHOOT_TICK) { // TODO: change to wait for web to actually hit the block
            Vector3d prevMotion = player.getMotion();
            double distanceToGrapplePos = playerPos.squareDistanceTo(grapplePos);

            if (distanceToGrapplePos >= MAX_GRAPPLE_DISENGAGE_DIST_SQ) {
                stopGrapple(player, data);
            } else if (!player.isSneaking()) {
                if (distanceToGrapplePos <= MIN_GRAPPLE_DISENGAGE_DIST_SQ) {
                    stopGrapple(player, data);
                } else updateGrapplePullMotion(player, grapplePos, playerPos, prevMotion);
            } else {
                updateGrappleSwingMotion(player, grapplePos, playerPos);
            }
        }

        if (data.get(GRAPPLE_TICK) >= 0) data.increase(GRAPPLE_TICK, 1);
    }

    private static void applySlowMotion(ClientPlayerEntity player) {
        // slow motion effect because it helps you aim and also is cool
        player.setMotion(player.getMotion().scale(0.75));
    }

    private static void setGrappleTargetPos(ClientPlayerEntity player, DataManager data, Vector3d playerPos) {
        BlockRayTraceResult blockHitResult = PhysicsUtils.rayCastBlocksFromEntity(player.world, player, GRAPPLE_REACH);
        EntityRayTraceResult entityHitResult = PhysicsUtils.rayCastEntitiesFromEntity(player, GRAPPLE_REACH, Entity::isLiving);
        if (blockHitResult == null && entityHitResult == null) {
            Main.LOGGER.info("Grapple did not hit anything; stopping grapple");
            stopGrapple(player, data);
            return;
        }
        RayTraceResult hitResult = PhysicsUtils.getClosestRayTraceResult(blockHitResult, entityHitResult, playerPos);
        if (hitResult == blockHitResult) data.set(GRAPPLE_BLOCK, blockHitResult.getHitVec());
        else data.set(GRAPPLE_ENTITY, entityHitResult.getEntity());
    }

    private static void updateGrapplePullMotion(ClientPlayerEntity player, Vector3d grapplePos, Vector3d playerPos, Vector3d prevMotion) {
        player.setNoGravity(true);
        Vector3d pullMotion = PhysicsUtils.getVelocityTowards(playerPos, grapplePos, 0.3);
        player.setMotion(prevMotion
                .scale(GRAPPLE_PREV_MOTION_AMP)
                .add(pullMotion));
    }

    private static void updateGrappleSwingMotion(ClientPlayerEntity player, Vector3d grapplePos, Vector3d playerPos) {
        player.setNoGravity(false);

        Vector3d oldDeltaMovement = player.getMotion();

        double distance = playerPos.distanceTo(grapplePos);

        Vector3d nextVectorFromCenter = playerPos
                .add(player.getMotion().scale(1.08))
                .subtract(grapplePos);
        Vector3d nextSwingPosition = grapplePos
                .add(nextVectorFromCenter
                        .normalize()
                        .scale(distance * 0.95)
                );
        player.setMotion(nextSwingPosition.subtract(playerPos).scale(1.05));


        Main.LOGGER.info("Distance from center: " + distance +
                ", Delta movement (this tick): " + oldDeltaMovement +
                ", Next vector from center: " + nextVectorFromCenter +
                ", Next vector from center normalized: " + nextVectorFromCenter.normalize() +
                ", Next swing position: " + nextSwingPosition +
                ", Delta movement: " + player.getMotion());
    }

    private static Vector3d getGrapplePos(DataManager data) {
        Vector3d grappleBlock = data.get(GRAPPLE_BLOCK);
        if (grappleBlock != null) return grappleBlock;
        Entity grappleEntity = data.get(GRAPPLE_ENTITY);
        if (grappleEntity != null) {
            if (grappleEntity.isAlive()) return grappleEntity.getPositionVec();
            else data.set(GRAPPLE_ENTITY, null);
        }
        return null;
    }

    private static void stopGrapple(ClientPlayerEntity player, DataManager data) {
        player.setNoGravity(false);
        data.set(GRAPPLE_TICK, -1);
        data.set(GRAPPLE_BLOCK, null);
        data.set(GRAPPLE_ENTITY, null);
    }


    private static void renderGrapple(PlayerEntity player, MatrixStack matrixStack, IRenderTypeBuffer buffer, float partialTicks) {
        Origin origin = Origin.getOrigin(player);
        if (origin == null) return;
        if (!(origin.getType() instanceof ArachnidOriginType)) return;
        Vector3d grapplePos = getGrapplePos(origin.getTypeData());
        if (grapplePos == null) return;
        Vector3d holdPosition = player.getLeashPosition(partialTicks);
        Vector3d delta = grapplePos.subtract(holdPosition);

        matrixStack.push();
        double d0 = Math.PI / 2/*(double)(MathHelper.lerp(partialTicks, player.renderYawOffset, player.prevRenderYawOffset) * ((float)Math.PI / 180F)) + (Math.PI / 2D)*/;
        //player.getLeashStartPosition();
        double d1 = Math.cos(d0) * delta.z + Math.sin(d0) * delta.x;
        double d2 = Math.sin(d0) * delta.z - Math.cos(d0) * delta.x;
        double d3 = MathHelper.lerp(partialTicks, player.prevPosX, player.getPosX()) + d1;
        double d4 = MathHelper.lerp(partialTicks, player.prevPosY, player.getPosY()) + delta.y;
        double d5 = MathHelper.lerp(partialTicks, player.prevPosZ, player.getPosZ()) + d2;
        matrixStack.translate(d1, delta.y, d2);
        float f = (float)(holdPosition.x - d3);
        float f1 = (float)(holdPosition.y - d4);
        float f2 = (float)(holdPosition.z - d5);
        float f3 = 0.025F;
        IVertexBuilder ivertexbuilder = buffer.getBuffer(RenderType.getLeash());
        Matrix4f matrix4f = matrixStack.getLast().getMatrix();
        float f4 = MathHelper.fastInvSqrt(f * f + f2 * f2) * f3 / 2.0F;
        float f5 = f2 * f4;
        float f6 = f * f4;
        BlockPos playerEyePos = new BlockPos(holdPosition);
        BlockPos grappleBlockPos = new BlockPos(grapplePos);
        int i = getBlockLight(player.world, grappleBlockPos);
        int j = getBlockLight(player, playerEyePos);
        int k = player.world.getLightFor(LightType.SKY, playerEyePos);
        int l = player.world.getLightFor(LightType.SKY, grappleBlockPos);
        MobRenderer.renderSide(ivertexbuilder, matrix4f, f, f1, f2, i, j, k, l, f3, 0.025F, f5, f6);
        MobRenderer.renderSide(ivertexbuilder, matrix4f, f, f1, f2, i, j, k, l, f3, 0.0F, f5, f6);
        matrixStack.pop();
    }

    private static int getBlockLight(Entity entity, BlockPos pos) {
        return entity.isBurning() ? 15 : entity.world.getLightFor(LightType.BLOCK, pos);
    }

    private static int getBlockLight(World world, BlockPos pos) {
        return world.getLightFor(LightType.BLOCK, pos);
    }

}
