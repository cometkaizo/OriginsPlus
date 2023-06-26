package me.cometkaizo.origins.mixin;

import me.cometkaizo.origins.origin.Origin;
import me.cometkaizo.origins.origin.SlimicianOriginType;
import net.minecraft.entity.EntitySize;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.Pose;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Map;

public final class SlimicianSizeMixin {

    @Mixin(PlayerEntity.class)
    public static abstract class MixedPlayerEntity extends LivingEntity {

        @Shadow @Final private static Map<Pose, EntitySize> SIZE_BY_POSE;

        @Shadow @Final public static EntitySize STANDING_SIZE;

        protected MixedPlayerEntity(EntityType<? extends LivingEntity> type, World worldIn) {
            super(type, worldIn);
        }

        @Inject(at = @At("HEAD"), method = "getSize", cancellable = true)
        protected void getModifiedSize(Pose pose, CallbackInfoReturnable<EntitySize> info) {
            Origin origin = Origin.getOrigin(this);
            if (origin != null && origin.getType() instanceof SlimicianOriginType) {
                SlimicianOriginType type = (SlimicianOriginType) origin.getType();
                info.setReturnValue(type.modifySize(SIZE_BY_POSE.getOrDefault(pose, STANDING_SIZE), origin));
            }
        }

        @Inject(at = @At("RETURN"), method = "getStandingEyeHeight", cancellable = true)
        protected void getModifiedEyeHeight(Pose poseIn, EntitySize sizeIn, CallbackInfoReturnable<Float> info) {
            Origin origin = Origin.getOrigin(this);
            if (origin != null && origin.getType() instanceof SlimicianOriginType) {
                SlimicianOriginType type = (SlimicianOriginType) origin.getType();
                info.setReturnValue(type.modifyEyeHeight(info.getReturnValueF(), origin));
            }
        }
    }

}
