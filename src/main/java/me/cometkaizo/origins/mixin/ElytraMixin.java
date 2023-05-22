package me.cometkaizo.origins.mixin;

import com.mojang.authlib.GameProfile;
import com.mojang.blaze3d.matrix.MatrixStack;
import me.cometkaizo.origins.animation.SimpleEaseInOut;
import me.cometkaizo.origins.animation.SimpleEaseOut;
import me.cometkaizo.origins.animation.SimpleTransition;
import me.cometkaizo.origins.animation.Transition;
import me.cometkaizo.origins.origin.ElytrianOriginType;
import me.cometkaizo.origins.origin.Origin;
import me.cometkaizo.origins.origin.OriginTypes;
import me.cometkaizo.origins.origin.PhoenixOriginType;
import net.minecraft.client.entity.player.AbstractClientPlayerEntity;
import net.minecraft.client.entity.player.ClientPlayerEntity;
import net.minecraft.client.renderer.IRenderTypeBuffer;
import net.minecraft.client.renderer.entity.layers.CapeLayer;
import net.minecraft.client.renderer.entity.layers.ElytraLayer;
import net.minecraft.client.renderer.entity.model.BipedModel;
import net.minecraft.client.renderer.entity.model.ElytraModel;
import net.minecraft.client.renderer.entity.model.PlayerModel;
import net.minecraft.client.renderer.model.ModelRenderer;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

public final class ElytraMixin {

    @OnlyIn(Dist.CLIENT)
    @Mixin(PlayerModel.class)
    public static abstract class MixedPlayerModel<T extends LivingEntity> extends BipedModel<T> {

        private static final double FLIGHT_MOVEMENT_REDUCTION = 0.3;

        public MixedPlayerModel(float modelSize) {
            super(modelSize);
        }

        @Inject(at = @At(value = "INVOKE",
                target = "Lnet/minecraft/client/renderer/entity/model/BipedModel;setRotationAngles(Lnet/minecraft/entity/LivingEntity;FFFFF)V",
                shift = At.Shift.AFTER),
                method = "setRotationAngles(Lnet/minecraft/entity/LivingEntity;FFFFF)V")
        protected void setRotationAngles(T entity, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch, CallbackInfo info) {
            Origin origin = Origin.getOrigin(entity);
            if (origin != null && origin.hasProperty(ElytrianOriginType.Property.PERMANENT_WINGS)) {
                bipedLeftLeg.rotateAngleX *= FLIGHT_MOVEMENT_REDUCTION;
                bipedLeftLeg.rotateAngleY *= FLIGHT_MOVEMENT_REDUCTION;
                bipedLeftLeg.rotateAngleZ *= FLIGHT_MOVEMENT_REDUCTION;

                bipedRightLeg.rotateAngleX *= FLIGHT_MOVEMENT_REDUCTION;
                bipedRightLeg.rotateAngleY *= FLIGHT_MOVEMENT_REDUCTION;
                bipedRightLeg.rotateAngleZ *= FLIGHT_MOVEMENT_REDUCTION;

                bipedLeftArm.rotateAngleX *= FLIGHT_MOVEMENT_REDUCTION;
                bipedLeftArm.rotateAngleY *= FLIGHT_MOVEMENT_REDUCTION;
                bipedLeftArm.rotateAngleZ *= FLIGHT_MOVEMENT_REDUCTION;

                bipedRightArm.rotateAngleX *= FLIGHT_MOVEMENT_REDUCTION;
                bipedRightArm.rotateAngleY *= FLIGHT_MOVEMENT_REDUCTION;
                bipedRightArm.rotateAngleZ *= FLIGHT_MOVEMENT_REDUCTION;
            }
        }
    }

    @OnlyIn(Dist.CLIENT)
    @Mixin(ElytraModel.class)
    public static abstract class MixedElytraModel {
        @Unique
        private final Transition UP_BOOST_ANIMATION = new SimpleTransition(0, -1.4, SimpleEaseOut.CUBIC, 3)
                .andThen(new SimpleTransition(-1.4, 0, SimpleEaseInOut.CUBIC, 8));
        @Unique
        private final Transition FORWARD_BOOST_RETURN_ANIMATION = new SimpleTransition(0, -1.45, SimpleEaseOut.CUBIC, 3)
                .andThen(new SimpleTransition(-1.45, 0, SimpleEaseInOut.QUAD, 4));

        @Unique
        private int animationStartTick = -1;
        @Unique
        private int prevBoostCooldown = 0;


        @Shadow @Final private ModelRenderer rightWing;
        @Shadow @Final private ModelRenderer leftWing;

        @Inject(method = "setRotationAngles(Lnet/minecraft/entity/LivingEntity;FFFFF)V", at = @At("TAIL"))
        protected void setRotationAngles(LivingEntity entity, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch, CallbackInfo info) {
            Origin origin = Origin.getOrigin(entity);
            if (origin != null &&
                    origin.hasProperty(ElytrianOriginType.Property.PERMANENT_WINGS)) {
                int upBoostTime;
                if (origin.getType() == OriginTypes.ELYTRIAN.get())
                    upBoostTime = origin.getTimeTracker().getTimerLeft(ElytrianOriginType.Cooldown.UP_BOOST);
                else upBoostTime = origin.getTimeTracker().getTimerLeft(PhoenixOriginType.Cooldown.UP_BOOST);

                if (upBoostTime > 0) {
                    animateTransition((int) ageInTicks, upBoostTime, UP_BOOST_ANIMATION);
                } else {
                    int forwardBoostTime;
                    if (origin.getType() == OriginTypes.ELYTRIAN.get())
                        forwardBoostTime = origin.getTimeTracker().getTimerLeft(ElytrianOriginType.Cooldown.FORWARD_BOOST);
                    else forwardBoostTime = origin.getTimeTracker().getTimerLeft(PhoenixOriginType.Cooldown.FORWARD_BOOST);

                    if (forwardBoostTime > 0) {
                        animateTransition((int) ageInTicks, forwardBoostTime, FORWARD_BOOST_RETURN_ANIMATION);
                    }
                }

            }
        }

        @Unique
        private void animateTransition(int currentTick, int boostCooldown, Transition animation) {
            if (boostCooldown > prevBoostCooldown) animationStartTick = currentTick;

            if (animationStartTick > -1) {
                double flap = animation.apply(animationStartTick, currentTick);
                leftWing.rotateAngleX += flap;
                rightWing.rotateAngleX += flap;
                if (animation.isFinished(animationStartTick, currentTick))
                    animationStartTick = -1;
            }

            prevBoostCooldown = boostCooldown;
        }
    }

    @OnlyIn(Dist.CLIENT)
    @Mixin(ElytraLayer.class)
    public static abstract class MixedElytraLayer {

        @Inject(method = "shouldRender", at = @At("HEAD"), cancellable = true, remap = false)
        protected void shouldRender(ItemStack stack, LivingEntity entity, CallbackInfoReturnable<Boolean> info) {
            if (entity.isInvisible()) return;

            Origin origin = Origin.getOrigin(entity);
            if (origin != null && origin.hasProperty(ElytrianOriginType.Property.PERMANENT_WINGS))
                info.setReturnValue(true);
        }
    }

    @Mixin(PlayerEntity.class)
    public static abstract class MixedPlayerEntity extends LivingEntity {

        protected MixedPlayerEntity(EntityType<? extends LivingEntity> type, World worldIn) {
            super(type, worldIn);
        }

        @Redirect(at = @At(value = "INVOKE",
                target = "Lnet/minecraft/item/ItemStack;canElytraFly(Lnet/minecraft/entity/LivingEntity;)Z"),
                method = "tryToStartFallFlying")
        protected boolean canElytraFly(ItemStack instance, LivingEntity livingEntity) {
            if (instance.canElytraFly(livingEntity)) return true;

            Origin origin = Origin.getOrigin(livingEntity);
            return origin != null && origin.hasProperty(ElytrianOriginType.Property.PERMANENT_WINGS);
        }
    }

    @Mixin(LivingEntity.class)
    public static abstract class MixedLivingEntity extends Entity {

        public MixedLivingEntity(EntityType<?> entityTypeIn, World worldIn) {
            super(entityTypeIn, worldIn);
        }

        @Redirect(at = @At(value = "INVOKE",
                target = "Lnet/minecraft/item/ItemStack;canElytraFly(Lnet/minecraft/entity/LivingEntity;)Z"),
                method = "updateElytra")
        protected boolean canElytraFly(ItemStack instance, LivingEntity livingEntity) {
            return true;
        }

        @Redirect(at = @At(value = "INVOKE",
                target = "Lnet/minecraft/item/ItemStack;elytraFlightTick(Lnet/minecraft/entity/LivingEntity;I)Z"),
                method = "updateElytra")
        protected boolean elytraFlightTick(ItemStack instance, LivingEntity livingEntity, int flightTicks) {
            if (instance.elytraFlightTick(livingEntity, flightTicks)) return true;

            Origin origin = Origin.getOrigin(livingEntity);
            return origin != null && origin.hasProperty(ElytrianOriginType.Property.PERMANENT_WINGS);
        }
    }

    @OnlyIn(Dist.CLIENT)
    @Mixin(ClientPlayerEntity.class)
    public static abstract class MixedClientPlayerEntity extends AbstractClientPlayerEntity {

        public MixedClientPlayerEntity(ClientWorld world, GameProfile profile) {
            super(world, profile);
        }

        @Redirect(at = @At(value = "INVOKE",
                target = "Lnet/minecraft/item/ItemStack;canElytraFly(Lnet/minecraft/entity/LivingEntity;)Z"),
                method = "livingTick")
        protected boolean canElytraFly(ItemStack instance, LivingEntity livingEntity) {
            return true;
        }
    }

    @OnlyIn(Dist.CLIENT)
    @Mixin(CapeLayer.class)
    public static class MixedCapeLayer {

        @Inject(method = "render(Lcom/mojang/blaze3d/matrix/MatrixStack;Lnet/minecraft/client/renderer/IRenderTypeBuffer;ILnet/minecraft/client/entity/player/AbstractClientPlayerEntity;FFFFFF)V",
                at = @At("HEAD"),
                cancellable = true)
        protected void preventCapeRendering(MatrixStack matrixStackIn,
                                          IRenderTypeBuffer bufferIn,
                                          int packedLightIn,
                                          AbstractClientPlayerEntity clientPlayer,
                                          float limbSwing,
                                          float limbSwingAmount,
                                          float partialTicks,
                                          float ageInTicks,
                                          float netHeadYaw,
                                          float headPitch,
                                          CallbackInfo info) {
            Origin origin = Origin.getOrigin(clientPlayer);
            if (origin != null && origin.hasProperty(ElytrianOriginType.Property.PERMANENT_WINGS))
                info.cancel();
        }
    }
}
