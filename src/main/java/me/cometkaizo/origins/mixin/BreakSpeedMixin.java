package me.cometkaizo.origins.mixin;

import me.cometkaizo.origins.origin.Origin;
import me.cometkaizo.origins.origin.SharkOriginType;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.fluid.Fluid;
import net.minecraft.tags.ITag;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.Redirect;

public final class BreakSpeedMixin {

    @Mixin(PlayerEntity.class)
    public static abstract class MixedPlayerEntity extends LivingEntity {

        protected MixedPlayerEntity(EntityType<? extends LivingEntity> type, World worldIn) {
            super(type, worldIn);
        }

        @Redirect(at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/player/PlayerEntity;areEyesInFluid(Lnet/minecraft/tags/ITag;)Z"),
                method = "getDigSpeed(Lnet/minecraft/block/BlockState;Lnet/minecraft/util/math/BlockPos;)F")
        protected boolean areEyesInFluid(PlayerEntity player, ITag<Fluid> fluidTag) {
            Origin origin = Origin.getOrigin(player);
            if (origin != null && origin.hasProperty(SharkOriginType.Property.AQUA_AFFINITY)) {
                return !areEyesInFluid(fluidTag);
            }
            return areEyesInFluid(fluidTag);
        }

        @ModifyVariable(at = @At(value = "STORE", ordinal = 5),
                method = "getDigSpeed(Lnet/minecraft/block/BlockState;Lnet/minecraft/util/math/BlockPos;)F",
                ordinal = 0,
                remap = false)
        protected float changeBreakSpeed(float f) {
            Origin origin = Origin.getOrigin(this);
            if (origin != null && origin.hasProperty(SharkOriginType.Property.AQUA_AFFINITY)) {
                if (isInWater()) {
                    return f * 5;
                }
            }
            return f;
        }
    }

}
