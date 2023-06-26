package me.cometkaizo.origins.mixin;

import me.cometkaizo.origins.origin.Origin;
import me.cometkaizo.origins.origin.OriginTypes;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

public final class SoftJumpingMixin {

    @Mixin(LivingEntity.class)
    public static abstract class MixedLivingEntity extends Entity {

        public MixedLivingEntity(EntityType<?> entityTypeIn, World worldIn) {
            super(entityTypeIn, worldIn);
        }

        @Inject(at = @At("HEAD"), method = "getJumpUpwardsMotion", cancellable = true)
        protected void getJumpUpwardsMotion(CallbackInfoReturnable<Float> info) {
            //noinspection ConstantConditions
            if (((Entity) this) instanceof PlayerEntity) {
                PlayerEntity player = (PlayerEntity) (Entity) this;
                Origin origin = Origin.getOrigin(player);
                if (origin != null && origin.getType() == OriginTypes.SLIMICIAN.get()) {
                    if (OriginTypes.SLIMICIAN.get().bounced(origin) && getMotion().y > getNormalJumpUpwardsMotion()) {
                        info.setReturnValue((float) getMotion().y);
                    }
                }
            }
        }

        private float getNormalJumpUpwardsMotion() {
            return 0.42F * this.getJumpFactor();
        }

    }

}
