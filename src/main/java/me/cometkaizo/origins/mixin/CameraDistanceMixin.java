package me.cometkaizo.origins.mixin;

import me.cometkaizo.origins.origin.Origin;
import me.cometkaizo.origins.origin.SlimicianOriginType;
import net.minecraft.client.renderer.ActiveRenderInfo;
import net.minecraft.entity.Entity;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@OnlyIn(Dist.CLIENT)
public final class CameraDistanceMixin {

    @OnlyIn(Dist.CLIENT)
    @Mixin(ActiveRenderInfo.class)
    public static abstract class MixedActiveRenderInfo {

        @Shadow private Entity renderViewEntity;

        @Shadow protected abstract double calcCameraDistance(double startingDistance);

        @Redirect(at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/ActiveRenderInfo;calcCameraDistance(D)D"), method = "update")
        protected double modifyCameraDistance(ActiveRenderInfo instance, double startingDistance) {
            Origin origin = Origin.getOrigin(renderViewEntity);
            if (origin != null && origin.getType() instanceof SlimicianOriginType) {
                SlimicianOriginType type = (SlimicianOriginType) origin.getType();
                double modifiedDistance = type.modifyCameraDistance(startingDistance, origin);
                return calcCameraDistance(modifiedDistance);
            }
            return calcCameraDistance(startingDistance);
        }
    }

}
