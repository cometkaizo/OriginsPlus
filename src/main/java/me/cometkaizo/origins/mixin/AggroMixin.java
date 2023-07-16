package me.cometkaizo.origins.mixin;

import me.cometkaizo.origins.potion.OriginEffects;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.MobEntity;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import javax.annotation.Nullable;

public final class AggroMixin {

    @Mixin(MobEntity.class)
    public static abstract class MixedMobEntity extends LivingEntity {
        @Shadow @Nullable public abstract LivingEntity getAttackTarget();

        @Shadow public abstract void setAttackTarget(@Nullable LivingEntity target);

        protected MixedMobEntity(EntityType<? extends LivingEntity> type, World world) {
            super(type, world);
        }

        @Inject(method = "baseTick", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/World;getProfiler()Lnet/minecraft/profiler/IProfiler;", ordinal = 1))
        protected void deaggroIfCamouflaged(CallbackInfo info) {
            LivingEntity attackTarget = getAttackTarget();
            if (attackTarget != null && attackTarget.isPotionActive(OriginEffects.CAMOUFLAGE.get())) {
                setAttackTarget(null);
            }
        }
    }

}
