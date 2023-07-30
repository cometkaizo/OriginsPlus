package me.cometkaizo.origins.util;

import net.minecraft.block.BlockState;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.projectile.ProjectileHelper;
import net.minecraft.util.math.*;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraft.world.World;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import java.util.function.Predicate;

public final class PhysicsUtils {

    // Returns a Vector3d based on start and end coordinates, and speed
    // normalize the vector between start and end and then multiply by speed
    public static Vector3d getVelocityTowards(Vector3d start, Vector3d end, double speed) {
        return end.subtract(start)
                .normalize()
                .scale(speed);
    }

    public static BlockRayTraceResult rayCastBlocksFromEntity(World world, Entity entity, float maxDistance) {
        float xRot = entity.rotationPitch;
        float yRot = entity.rotationYaw;
        Vector3d eyePosition = entity.getEyePosition(0);

        float f2 = MathHelper.cos(-yRot * ((float)Math.PI / 180F) - (float)Math.PI);
        float f3 = MathHelper.sin(-yRot * ((float)Math.PI / 180F) - (float)Math.PI);
        float f4 = -MathHelper.cos(-xRot * ((float)Math.PI / 180F));
        float f5 = MathHelper.sin(-xRot * ((float)Math.PI / 180F));
        float f6 = f3 * f4;
        float f7 = f2 * f4;

        Vector3d vector3d1 = eyePosition.add((double)f6 * maxDistance, (double)f5 * maxDistance, (double)f7 * maxDistance);
        BlockRayTraceResult result = world.rayTraceBlocks(new RayTraceContext(eyePosition, vector3d1, RayTraceContext.BlockMode.COLLIDER, RayTraceContext.FluidMode.NONE, entity));
        return world.getBlockState(result.getPos()).getMaterial().isSolid() ? result : null;
    }

    @OnlyIn(Dist.CLIENT)
    public static EntityRayTraceResult rayCastEntitiesFromEntity(Entity entity, float maxDistance, Predicate<Entity> condition) {
        Vector3d eyePosition = entity.getEyePosition(0);
        Vector3d look = entity.getLook(1);
        Vector3d scaledLook = look.scale(maxDistance);
        Vector3d rayEnd = eyePosition.add(scaledLook);

        AxisAlignedBB detectionArea = entity.getBoundingBox().expand(scaledLook).grow(1);

        EntityRayTraceResult result = ProjectileHelper.rayTraceEntities(entity, eyePosition, rayEnd, detectionArea, condition, maxDistance * maxDistance);
        return result != null && result.getType() != RayTraceResult.Type.MISS ? result : null;
    }

    public static Vector3d getClosestVector(Vector3d a, Vector3d b, Vector3d target) {
        return b == null || (a != null && a.squareDistanceTo(target) < b.squareDistanceTo(target)) ? a : b;
    }

    public static RayTraceResult getClosestRayTraceResult(RayTraceResult a, RayTraceResult b, Vector3d target) {
        return b == null || a != null && a.getHitVec().squareDistanceTo(target) < b.getHitVec().squareDistanceTo(target) ? a : b;
    }

    public static BlockState getBlockUnder(Entity entity) {
        return entity.world.getBlockState(getPosUnder(entity));
    }

    public static BlockPos getPosUnder(Entity entity) {
        return new BlockPos(entity.getPosX(), entity.getPosY() - 0.5000001D, entity.getPosZ());
    }

    public static BlockState getBlockAt(Entity entity) {
        return entity.world.getBlockState(new BlockPos(entity.getPosX(), entity.getPosY(), entity.getPosZ()));
    }

    public static boolean isInRain(PlayerEntity player) {
        BlockPos blockpos = player.getPosition();
        return player.world.isRainingAt(blockpos) ||
                player.world.isRainingAt(new BlockPos(blockpos.getX(), player.getBoundingBox().maxY, blockpos.getZ()));
    }

    private PhysicsUtils() {
        throw new AssertionError("No PhysicsUtil instances for you!");
    }

}
