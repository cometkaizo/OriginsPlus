package me.cometkaizo.origins.origin.client;

import me.cometkaizo.origins.origin.Origin;
import me.cometkaizo.origins.util.DataKey;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.player.ClientPlayerEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraft.world.World;
import net.minecraftforge.event.TickEvent;

public class ClientArachnidOriginType {
    public static final double WALL_CLIMB_SPEED = 0.09;
    public static final double WALL_SLOW_FALL_SPEED_AMP = 0.6;
    public static final double WALL_CLIMB_DISTANCE = 0.2;
    private static final DataKey<BlockPos.Mutable> WALL_CHECK_POS = DataKey.create(BlockPos.Mutable.class);

    public static void onPlayerSensitiveEvent(Object event, Origin origin) {
        if (event instanceof TickEvent.ClientTickEvent) {
            onClientTick((TickEvent.ClientTickEvent) event, origin);
        }
    }


    public static void onFirstActivate(Origin origin) {
        if (origin.isServerSide()) return;
        origin.getTypeDataManager().register(WALL_CHECK_POS, new BlockPos.Mutable());
    }

    private static void onClientTick(TickEvent.ClientTickEvent event, Origin origin) {
        if (event.phase == TickEvent.Phase.START) return;
        if (origin.isServerSide()) return;
        ClientPlayerEntity player = Minecraft.getInstance().player;
        boolean equals = origin.getPlayer().equals(player);
        if (!equals) return;

        Vector3d motion = player.getMotion();
        if (player.collidedHorizontally || isNextToWall(player, origin)) {
            if (shouldStopOnWall(player)) {
                player.setMotion(Vector3d.ZERO);
            } else if (shouldClimbWall(player)) {
                player.setMotion(motion.x, Math.max(motion.y, WALL_CLIMB_SPEED), motion.z);
            } else {
                player.setMotion(motion.x, motion.y * WALL_SLOW_FALL_SPEED_AMP, motion.z);
            }
            player.fallDistance = 0;
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

        return world.getBlockState(wallCheckPos.setPos(x + halfWidth + WALL_CLIMB_DISTANCE, y, z + halfDepth + WALL_CLIMB_DISTANCE)).isSolid() ||
                world.getBlockState(wallCheckPos.setPos(x + halfWidth + WALL_CLIMB_DISTANCE, y, z - halfDepth - WALL_CLIMB_DISTANCE)).isSolid() ||
                world.getBlockState(wallCheckPos.setPos(x - halfWidth - WALL_CLIMB_DISTANCE, y, z + halfDepth + WALL_CLIMB_DISTANCE)).isSolid() ||
                world.getBlockState(wallCheckPos.setPos(x - halfWidth - WALL_CLIMB_DISTANCE, y, z - halfDepth - WALL_CLIMB_DISTANCE)).isSolid();
    }

    private static boolean shouldClimbWall(ClientPlayerEntity player) {
        return (player.collidedHorizontally || player.movementInput.jump) && !player.isSwimming() && !player.isElytraFlying();
    }

    private static boolean shouldStopOnWall(ClientPlayerEntity player) {
        return player.isSneaking();
    }
}
