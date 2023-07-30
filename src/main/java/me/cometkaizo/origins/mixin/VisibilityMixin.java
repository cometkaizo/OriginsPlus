package me.cometkaizo.origins.mixin;

import com.mojang.blaze3d.matrix.MatrixStack;
import com.mojang.blaze3d.systems.RenderSystem;
import me.cometkaizo.origins.animation.SimpleEaseInOut;
import me.cometkaizo.origins.animation.SimpleTransition;
import me.cometkaizo.origins.animation.Transition;
import me.cometkaizo.origins.origin.EnderianOriginType;
import me.cometkaizo.origins.origin.FoxOriginType;
import me.cometkaizo.origins.origin.Origin;
import me.cometkaizo.origins.origin.PhantomOriginType;
import me.cometkaizo.origins.potion.OriginEffects;
import me.cometkaizo.origins.util.DataManager;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.player.AbstractClientPlayerEntity;
import net.minecraft.client.entity.player.ClientPlayerEntity;
import net.minecraft.client.gui.IngameGui;
import net.minecraft.client.renderer.FirstPersonRenderer;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.IRenderTypeBuffer;
import net.minecraft.client.renderer.entity.IEntityRenderer;
import net.minecraft.client.renderer.entity.LivingRenderer;
import net.minecraft.client.renderer.entity.layers.BipedArmorLayer;
import net.minecraft.client.renderer.entity.layers.HeadLayer;
import net.minecraft.client.renderer.entity.layers.LayerRenderer;
import net.minecraft.client.renderer.entity.model.BipedModel;
import net.minecraft.client.renderer.entity.model.EntityModel;
import net.minecraft.client.renderer.entity.model.IHasHead;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.potion.Effect;
import net.minecraft.potion.EffectInstance;
import net.minecraft.potion.Effects;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Map;

import static me.cometkaizo.origins.origin.EnderianOriginType.isWearingPumpkin;

public class VisibilityMixin {

    @Mixin(IngameGui.class)
    public static abstract class MixedIngameGui {

        @Unique
        private static final float originsPlus$R = 1 - 20 / 255F, originsPlus$G = 1 - 26 / 255F, originsPlus$B = 1 - 20 / 255F;
        @Unique
        private static final Transition VIGNETTE_TRANSITION_IN = new SimpleTransition(0, 1, SimpleEaseInOut.CUBIC, (int)(0.35 * 20)),
                VIGNETTE_TRANSITION_OUT = new SimpleTransition(1, 0, SimpleEaseInOut.CUBIC, (int)(0.75 * 20));

        @SuppressWarnings("deprecation")
        @Inject(method = "renderVignette",
                at = @At(value = "INVOKE", target = "Lnet/minecraft/client/Minecraft;getTextureManager()Lnet/minecraft/client/renderer/texture/TextureManager;"))
        protected void changeVignetteColor(Entity entity, CallbackInfo info) {
            Origin origin = Origin.getOrigin(entity);
            float smoothing;
            if (entity instanceof LivingEntity && ((LivingEntity) entity).isPotionActive(OriginEffects.CAMOUFLAGE.get())) {
                if (origin != null) smoothing = originsPlus$getSmoothingIn(entity, origin);
                else smoothing = 1;

                RenderSystem.color4f(originsPlus$R * smoothing, originsPlus$G * smoothing, originsPlus$B * smoothing, 1);
            } else if (origin != null) {
                smoothing = originsPlus$getSmoothingOut(entity, origin);
                if (smoothing > 0) RenderSystem.color4f(originsPlus$R * smoothing, originsPlus$G * smoothing, originsPlus$B * smoothing, 1);
            }
        }

        @Unique
        private static float originsPlus$getSmoothingIn(Entity entity, Origin origin) {
            DataManager data = origin.getTypeData();
            if (!data.contains(FoxOriginType.CAMOUFLAGE_START_TICK)) return 1;
            long startTick = data.get(FoxOriginType.CAMOUFLAGE_START_TICK);
            if (startTick == -1) {
                data.set(FoxOriginType.CAMOUFLAGE_START_TICK, entity.world.getGameTime());
                data.set(FoxOriginType.CAMOUFLAGE_END_TICK, -1L);
                startTick = entity.world.getGameTime();
            }
            return (float) VIGNETTE_TRANSITION_IN.apply(startTick, entity.world.getGameTime());
        }

        @Unique
        private static float originsPlus$getSmoothingOut(Entity entity, Origin origin) {
            DataManager data = origin.getTypeData();
            if (!data.contains(FoxOriginType.CAMOUFLAGE_END_TICK)) return 0;
            long endTick = data.get(FoxOriginType.CAMOUFLAGE_END_TICK);
            if (endTick == -1) {
                data.set(FoxOriginType.CAMOUFLAGE_END_TICK, entity.world.getGameTime());
                data.set(FoxOriginType.CAMOUFLAGE_START_TICK, -1L);
                endTick = entity.world.getGameTime();
            }
            return (float) VIGNETTE_TRANSITION_OUT.apply(endTick, entity.world.getGameTime());
        }
    }

    @Mixin(GameRenderer.class)
    public static abstract class MixedGameRenderer {
        @Inject(method = "getNightVisionBrightness", at = @At("HEAD"), cancellable = true)
        private static void getFoxNightVisionBrightness(LivingEntity entity,
                                                        float partialTicks,
                                                        CallbackInfoReturnable<Float> info) {
            Origin origin = Origin.getOrigin(entity);
            if (origin != null && origin.hasLabel(FoxOriginType.Property.NIGHT_VISION)) {
                info.setReturnValue(0.8F);
            }
        }
    }

    @Mixin(Entity.class)
    public static abstract class MixedEntity {
        @SuppressWarnings("ConstantConditions")
        @Inject(method = "isInvisibleToPlayer", at = @At("HEAD"), cancellable = true)
        protected void isInvisibleToSelf(PlayerEntity player, CallbackInfoReturnable<Boolean> info) {
            if (((Object) this) != player) return;
            if (player.isPotionActive(OriginEffects.CAMOUFLAGE.get()))
                info.setReturnValue(false); // returning false when invisible like here makes the player translucent.
        }
        @SuppressWarnings("ConstantConditions")
        @Inject(method = "isInvisibleToPlayer", at = @At("HEAD"), cancellable = true)
        protected void isInvisibleToEnderian(PlayerEntity player, CallbackInfoReturnable<Boolean> info) {
            if (((Object)this) instanceof LivingEntity) {
                LivingEntity livingEntity = (LivingEntity) (Object)this;
                if (!isWearingPumpkin(livingEntity)) return;

                Origin playerOrigin = Origin.getOrigin(player);
                if (playerOrigin != null && playerOrigin.hasLabel(EnderianOriginType.Label.CANNOT_SEE_ENTITIES_WEARING_PUMPKINS))
                    info.setReturnValue(true);
            }
        }
        @SuppressWarnings("ConstantConditions")
        @Inject(method = "isInvisibleToPlayer", at = @At("HEAD"), cancellable = true)
        protected void isInvisibleTo(PlayerEntity player, CallbackInfoReturnable<Boolean> info) {
            Origin origin = Origin.getOrigin((Entity) (Object)this);
            if (origin != null && origin.hasLabel(PhantomOriginType.Label.TRANSLUCENT_SKIN) &&
                    !PhantomOriginType.isInPhantomForm(origin))
                info.setReturnValue(false);
        }
        @Inject(method = "isInvisible", at = @At("TAIL"), cancellable = true)
        protected void isInvisible(CallbackInfoReturnable<Boolean> cir) {
            if (cir.getReturnValueZ()) return;
            Origin origin = Origin.getOrigin((Entity)(Object)this);
            cir.setReturnValue(origin != null && origin.hasLabel(PhantomOriginType.Label.TRANSLUCENT_SKIN)/* &&
                    (!(origin.getType() instanceof PhantomOriginType) || PhantomOriginType.isInPhantomForm(origin))*/);
        }
    }

    @Mixin(FirstPersonRenderer.class)
    public static abstract class MixedFirstPersonRenderer {
        @Redirect(method = "renderMapFirstPerson(Lcom/mojang/blaze3d/matrix/MatrixStack;Lnet/minecraft/client/renderer/IRenderTypeBuffer;IFFF)V",
                at = @At(value = "INVOKE", target = "Lnet/minecraft/client/entity/player/ClientPlayerEntity;isInvisible()Z"))
        protected boolean isInvisible0(ClientPlayerEntity player) {
            return originsPlus$isInvisible(player);
        }
        @Redirect(method = "renderItemInFirstPerson(Lnet/minecraft/client/entity/player/AbstractClientPlayerEntity;FFLnet/minecraft/util/Hand;FLnet/minecraft/item/ItemStack;FLcom/mojang/blaze3d/matrix/MatrixStack;Lnet/minecraft/client/renderer/IRenderTypeBuffer;I)V",
                at = @At(value = "INVOKE", target = "Lnet/minecraft/client/entity/player/AbstractClientPlayerEntity;isInvisible()Z"))
        protected boolean isInvisible1(AbstractClientPlayerEntity player) {
            return originsPlus$isInvisible(player);
        }
        @Redirect(method = "renderMapFirstPersonSide",
                at = @At(value = "INVOKE", target = "Lnet/minecraft/client/entity/player/ClientPlayerEntity;isInvisible()Z"))
        protected boolean isInvisible2(ClientPlayerEntity player) {
            return originsPlus$isInvisible(player);
        }

        @Unique
        private static boolean originsPlus$isInvisible(AbstractClientPlayerEntity player) {
            Origin origin = Origin.getOrigin(player);
            if (origin != null && origin.getType() instanceof PhantomOriginType && !PhantomOriginType.isInPhantomForm(origin))
                return false;
            return player.isInvisible();
        }
    }

    @Mixin(LivingRenderer.class)
    public static abstract class MixedLivingRenderer {
        @Redirect(method = "isVisible", at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/LivingEntity;isInvisible()Z"))
        protected boolean isInvisibleToClient(LivingEntity entity) {
            ClientPlayerEntity localPlayer = Minecraft.getInstance().player;
            return entity.isInvisible() ||
                    (isWearingPumpkin(entity) &&
                            Origin.hasLabel(localPlayer, EnderianOriginType.Label.CANNOT_SEE_ENTITIES_WEARING_PUMPKINS)) ||
                    (PhantomOriginType.isInPhantomForm(Origin.getOrigin(entity)));
        }
    }

    @Mixin(LivingEntity.class)
    public static abstract class MixedLivingEntity extends Entity {
        @Shadow @Final private Map<Effect, EffectInstance> activePotionsMap;

        public MixedLivingEntity(EntityType<?> entityTypeIn, World worldIn) {
            super(entityTypeIn, worldIn);
        }

        @Inject(method = "isPotionActive", at = @At("RETURN"), cancellable = true)
        protected void isCamouflageActive(Effect effect, CallbackInfoReturnable<Boolean> info) {
            if (effect == Effects.NIGHT_VISION) {
                Origin origin = Origin.getOrigin(this);
                if (origin != null && origin.hasLabel(FoxOriginType.Property.NIGHT_VISION)) {
                    info.setReturnValue(true);
                    return;
                }
            }
            info.setReturnValue(info.getReturnValueZ() ||
                    (effect == Effects.INVISIBILITY &&
                    activePotionsMap.containsKey(OriginEffects.CAMOUFLAGE.get())));
        }
    }

    @Mixin(value = BipedArmorLayer.class, priority = 400)
    public static abstract class MixedBipedArmorLayer<T extends LivingEntity, M extends BipedModel<T>, A extends BipedModel<T>> extends LayerRenderer<T, M> {
        public MixedBipedArmorLayer(IEntityRenderer<T, M> entityRendererIn) {
            super(entityRendererIn);
        }

        @Inject(method = "render(Lcom/mojang/blaze3d/matrix/MatrixStack;Lnet/minecraft/client/renderer/IRenderTypeBuffer;ILnet/minecraft/entity/LivingEntity;FFFFFF)V",
                at = @At("HEAD"), cancellable = true)
        protected void cancelRenderIfCamouflaged(MatrixStack matrixStack,
                                                 IRenderTypeBuffer buffer,
                                                 int packedLight,
                                                 T entity,
                                                 float limbSwing,
                                                 float limbSwingAmount,
                                                 float partialTicks,
                                                 float ageInTicks,
                                                 float netHeadYaw,
                                                 float headPitch,
                                                 CallbackInfo info) {
            if (entity.isPotionActive(OriginEffects.CAMOUFLAGE.get())) info.cancel();
        }

        @Inject(method = "render(Lcom/mojang/blaze3d/matrix/MatrixStack;Lnet/minecraft/client/renderer/IRenderTypeBuffer;ILnet/minecraft/entity/LivingEntity;FFFFFF)V",
                at = @At("HEAD"), cancellable = true)
        protected void cancelRenderIfWearingPumpkin(MatrixStack matrixStack,
                                                 IRenderTypeBuffer buffer,
                                                 int packedLight,
                                                 T entity,
                                                 float limbSwing,
                                                 float limbSwingAmount,
                                                 float partialTicks,
                                                 float ageInTicks,
                                                 float netHeadYaw,
                                                 float headPitch,
                                                 CallbackInfo info) {
            ClientPlayerEntity localPlayer = Minecraft.getInstance().player;
            if (localPlayer == null) return;
            if (entity == localPlayer) return;
            if (!isWearingPumpkin(entity)) return;
            Origin origin = Origin.getOrigin(localPlayer);
            if (origin != null && origin.hasLabel(EnderianOriginType.Label.CANNOT_SEE_ENTITIES_WEARING_PUMPKINS)) {
                info.cancel();
            }
        }

    }

    @Mixin(HeadLayer.class)
    public static abstract class MixedHeadLayer<T extends LivingEntity, M extends EntityModel<T> & IHasHead> extends LayerRenderer<T, M> {
        public MixedHeadLayer(IEntityRenderer<T, M> entityRendererIn) {
            super(entityRendererIn);
        }

        @Inject(method = "render(Lcom/mojang/blaze3d/matrix/MatrixStack;Lnet/minecraft/client/renderer/IRenderTypeBuffer;ILnet/minecraft/entity/LivingEntity;FFFFFF)V",
                at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/matrix/MatrixStack;translate(DDD)V", ordinal = 4),
                cancellable = true)
        protected void cancelRenderIfCamouflaged(MatrixStack matrixStack,
                                                 IRenderTypeBuffer buffer,
                                                 int packedLight,
                                                 T entity,
                                                 float limbSwing,
                                                 float limbSwingAmount,
                                                 float partialTicks,
                                                 float ageInTicks,
                                                 float netHeadYaw,
                                                 float headPitch,
                                                 CallbackInfo info) {
            boolean isCamouflaged = entity.isPotionActive(OriginEffects.CAMOUFLAGE.get());
            if (isCamouflaged) {
                matrixStack.pop();
                info.cancel();
            }
        }

        @Inject(method = "render(Lcom/mojang/blaze3d/matrix/MatrixStack;Lnet/minecraft/client/renderer/IRenderTypeBuffer;ILnet/minecraft/entity/LivingEntity;FFFFFF)V",
                at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/matrix/MatrixStack;translate(DDD)V", ordinal = 4),
                cancellable = true)
        protected void cancelRenderIfWearingPumpkin(MatrixStack matrixStack,
                                                    IRenderTypeBuffer buffer,
                                                    int packedLight,
                                                    T entity,
                                                    float limbSwing,
                                                    float limbSwingAmount,
                                                    float partialTicks,
                                                    float ageInTicks,
                                                    float netHeadYaw,
                                                    float headPitch,
                                                    CallbackInfo info) {
            ClientPlayerEntity localPlayer = Minecraft.getInstance().player;
            if (localPlayer == null) return;
            if (entity == localPlayer) return;
            if (!isWearingPumpkin(entity)) return;
            Origin origin = Origin.getOrigin(localPlayer);
            if (origin != null && origin.hasLabel(EnderianOriginType.Label.CANNOT_SEE_ENTITIES_WEARING_PUMPKINS)) {
                matrixStack.pop();
                info.cancel();
            }
        }
    }
}
