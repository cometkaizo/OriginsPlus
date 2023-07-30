package me.cometkaizo.origins.mixin;

import com.mojang.authlib.GameProfile;
import me.cometkaizo.origins.network.C2SUpdatePhasing;
import me.cometkaizo.origins.network.Packets;
import me.cometkaizo.origins.origin.Origin;
import me.cometkaizo.origins.origin.OriginTypes;
import me.cometkaizo.origins.origin.PhantomOriginType;
import net.minecraft.client.entity.player.AbstractClientPlayerEntity;
import net.minecraft.client.entity.player.ClientPlayerEntity;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.MovementInput;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

public final class PhaseMixin {

    @Mixin(ClientPlayerEntity.class)
    public static abstract class MixedClientPlayerEntity extends AbstractClientPlayerEntity {

        @Shadow public MovementInput movementInput;

        public MixedClientPlayerEntity(ClientWorld world, GameProfile profile) {
            super(world, profile);
        }

        @Inject(method = "livingTick", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/entity/player/ClientPlayerEntity;isRidingHorse()Z"))
        protected void tryPhase(CallbackInfo ci) {
            if (abilities.isFlying) return;
            Origin origin = Origin.getOrigin(this);
            if (origin == null || !origin.hasLabel(PhantomOriginType.Label.PHASE_THROUGH_BLOCKS)) return;

            boolean phasing = OriginTypes.PHANTOM.get().isPhasing(origin);
            if (phasing) {
                Vector3d forwardVec = getLookVec().normalize();
                Vector3d strafeVec = forwardVec.rotateYaw(90);
                setMotion(getMotion().scale(0.3).add(new Vector3d(
                        forwardVec.x * movementInput.moveForward * abilities.getFlySpeed() +
                                strafeVec.x * movementInput.moveStrafe * abilities.getFlySpeed(),
                        movementInput.jump ? abilities.getFlySpeed() * 3 : 0,
                        forwardVec.z * movementInput.moveForward * abilities.getFlySpeed() +
                                strafeVec.z * movementInput.moveStrafe * abilities.getFlySpeed()
                )));
            } if (OriginTypes.PHANTOM.get().shouldPhase(origin)) {
                setMotion(getMotion().add(0.0D, -abilities.getFlySpeed() * 3, 0.0D));
                phasing = true;
            } if (origin.getTypeData().get(PhantomOriginType.PREV_IS_PHASING) != phasing) {
                Packets.sendToServer(new C2SUpdatePhasing(phasing));
            }

            setNoGravity(phasing);
            noClip = phasing;
            if (phasing) {
                fallDistance = 0;
                onGround = true;
            }

            origin.getTypeData().set(PhantomOriginType.PREV_IS_PHASING, phasing);
        }
    }

    @Mixin(PlayerEntity.class)
    public static abstract class MixedPlayerEntity extends LivingEntity {
        @Shadow public abstract boolean isSpectator();

        protected MixedPlayerEntity(EntityType<? extends LivingEntity> type, World worldIn) {
            super(type, worldIn);
        }

        @Redirect(method = "tick", at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/player/PlayerEntity;isSpectator()Z"))
        protected boolean accountForPhantomForm(PlayerEntity instance) {
            if (isSpectator()) return true;
            Origin origin = Origin.getOrigin(instance);
            return origin.hasLabel(PhantomOriginType.Label.PHASE_THROUGH_BLOCKS) &&
                    OriginTypes.PHANTOM.get().isPhasing(origin);
        }
    }

    @Mixin(Entity.class)
    public static abstract class MixedEntity {
        @Inject(method = "isEntityInsideOpaqueBlock", at = @At("HEAD"), cancellable = true)
        protected void accountForPhantomForm(CallbackInfoReturnable<Boolean> cir) {
            if (PhantomOriginType.isInPhantomForm(Origin.getOrigin((Entity) (Object)this)))
                cir.setReturnValue(false);
        }
    }

}
