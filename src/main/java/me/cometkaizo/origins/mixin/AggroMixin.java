package me.cometkaizo.origins.mixin;

import me.cometkaizo.origins.origin.Origin;
import me.cometkaizo.origins.origin.PhantomOriginType;
import me.cometkaizo.origins.potion.OriginEffects;
import me.cometkaizo.origins.util.EntityUtils;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.MobEntity;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
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

        @Inject(method = "setAttackTarget", at = @At("HEAD"), cancellable = true)
        protected void cancelAggroIfNecessary(LivingEntity entity, CallbackInfo ci) {
            if (entity != null && curtain$shouldDeaggro(entity)) ci.cancel();
        }

        @Inject(method = "baseTick", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/World;getProfiler()Lnet/minecraft/profiler/IProfiler;", ordinal = 1))
        protected void deaggroIfNecessary(CallbackInfo info) {
            LivingEntity attackTarget = getAttackTarget();
            if (attackTarget != null && curtain$shouldDeaggro(attackTarget)) {
                setAttackTarget(null);
            }
        }

        @Unique
        private boolean curtain$shouldDeaggro(LivingEntity attackTarget) {
            if (attackTarget.isPotionActive(OriginEffects.CAMOUFLAGE.get())) return true;
            Origin origin = Origin.getOrigin(attackTarget);
            double minMobDeaggroDistance = PhantomOriginType.MIN_MOB_DEAGGRO_DISTANCE;
            return PhantomOriginType.isInPhantomForm(origin) &&
                    !EntityUtils.isWearingArmor(this) &&
                    !EntityUtils.isHoldingItem(this) &&
                    getPositionVec().squareDistanceTo(attackTarget.getPositionVec()) > minMobDeaggroDistance * minMobDeaggroDistance;
        }
    }

}
