package me.cometkaizo.origins.util;

import net.minecraft.entity.Entity;
import net.minecraft.util.math.BlockRayTraceResult;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RayTraceContext;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraft.world.World;

public final class PhysicsUtils {

    // Returns a Vector3d based on start and end coordinates, and speed
    // normalize the vector between start and end and then multiply by speed
    public static Vector3d getVelocityTowards(Vector3d start, Vector3d end, double speed) {
        return end.subtract(start)
                .normalize()
                .scale(speed);
    }

    public static BlockRayTraceResult rayCastFromEntity(World world, Entity entity, float maxDistance) {
        float xRot = entity.rotationPitch;
        float yRot = entity.rotationYaw;
        Vector3d eyePosition = entity.getEyePosition(0);

        float f2 = MathHelper.cos(-yRot * ((float)Math.PI / 180F) - (float)Math.PI);
        float f3 = MathHelper.sin(-yRot * ((float)Math.PI / 180F) - (float)Math.PI);
        float f4 = -MathHelper.cos(-xRot * ((float)Math.PI / 180F));
        float f5 = MathHelper.sin(-xRot * ((float)Math.PI / 180F));
        float f6 = f3 * f4;
        float f7 = f2 * f4;

        Vector3d Vector3d1 = eyePosition.add((double)f6 * maxDistance, (double)f5 * maxDistance, (double)f7 * maxDistance);
        BlockRayTraceResult result = world.rayTraceBlocks(new RayTraceContext(eyePosition, Vector3d1, RayTraceContext.BlockMode.COLLIDER, RayTraceContext.FluidMode.NONE, entity));
        return world.getBlockState(result.getPos()).getMaterial().isSolid() ? result : null;
    }

    private PhysicsUtils() {
        throw new AssertionError("No PhysicsUtil instances for you!");
    }

}
