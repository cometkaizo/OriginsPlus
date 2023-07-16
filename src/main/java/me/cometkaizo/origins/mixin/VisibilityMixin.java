package me.cometkaizo.origins.mixin;

import com.mojang.blaze3d.matrix.MatrixStack;
import com.mojang.blaze3d.systems.RenderSystem;
import me.cometkaizo.origins.Main;
import me.cometkaizo.origins.animation.SimpleEaseInOut;
import me.cometkaizo.origins.animation.SimpleTransition;
import me.cometkaizo.origins.animation.Transition;
import me.cometkaizo.origins.origin.FoxOriginType;
import me.cometkaizo.origins.origin.Origin;
import me.cometkaizo.origins.potion.OriginEffects;
import me.cometkaizo.origins.util.DataManager;
import net.minecraft.client.gui.IngameGui;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.IRenderTypeBuffer;
import net.minecraft.client.renderer.entity.IEntityRenderer;
import net.minecraft.client.renderer.entity.layers.BipedArmorLayer;
import net.minecraft.client.renderer.entity.layers.LayerRenderer;
import net.minecraft.client.renderer.entity.model.BipedModel;
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
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Map;

public class VisibilityMixin {

    @Mixin(IngameGui.class)
    public static abstract class MixedIngameGui {

        private static final float R = 1 - 20 / 255F, G = 1 - 26 / 255F, B = 1 - 20 / 255F;
        private static final Transition VIGNETTE_TRANSITION_IN = new SimpleTransition(0, 1, SimpleEaseInOut.CUBIC, (int)(0.35 * 20)),
                VIGNETTE_TRANSITION_OUT = new SimpleTransition(1, 0, SimpleEaseInOut.CUBIC, (int)(0.75 * 20));

        @SuppressWarnings("deprecation")
        @Inject(method = "renderVignette",
                at = @At(value = "INVOKE", target = "Lnet/minecraft/client/Minecraft;getTextureManager()Lnet/minecraft/client/renderer/texture/TextureManager;"))
        protected void changeVignetteColor(Entity entity, CallbackInfo info) {
            Origin origin = Origin.getOrigin(entity);
            float smoothing;
            if (entity instanceof LivingEntity && ((LivingEntity) entity).isPotionActive(OriginEffects.CAMOUFLAGE.get())) {
                if (origin != null) smoothing = getSmoothingIn(entity, origin);
                else smoothing = 1;

                RenderSystem.color4f(R * smoothing, G * smoothing, B * smoothing, 1);
            } else if (origin != null) {
                smoothing = getSmoothingOut(entity, origin);
                if (smoothing > 0) RenderSystem.color4f(R * smoothing, G * smoothing, B * smoothing, 1);
            }
        }

        private static float getSmoothingIn(Entity entity, Origin origin) {
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

        private static float getSmoothingOut(Entity entity, Origin origin) {
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
            if (origin != null && origin.hasProperty(FoxOriginType.Property.NIGHT_VISION)) {
                info.setReturnValue(0.8F);
            }
        }
    }

    @Mixin(Entity.class)
    public static abstract class MixedEntity {
        @SuppressWarnings("ConstantConditions")
        @Inject(method = "isInvisibleToPlayer", at = @At("HEAD"), cancellable = true)
        protected void isInvisibleToSelf(PlayerEntity player, CallbackInfoReturnable<Boolean> info) {
            if (((Object)this) == player && player.isPotionActive(OriginEffects.CAMOUFLAGE.get()))
                info.setReturnValue(false); // returning false when invisible like here makes the player translucent.
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
                if (origin != null && origin.hasProperty(FoxOriginType.Property.NIGHT_VISION)) {
                    info.setReturnValue(true);
                    return;
                }
            }
            info.setReturnValue(info.getReturnValueZ() ||
                    (effect == Effects.INVISIBILITY &&
                    activePotionsMap.containsKey(OriginEffects.CAMOUFLAGE.get())));
        }
    }

    @Mixin(BipedArmorLayer.class)
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
            boolean isCamouflaged = entity.isPotionActive(OriginEffects.CAMOUFLAGE.get());
            if (entity instanceof PlayerEntity)
                Main.LOGGER.debug("-------- does player have camouflage? {} : {} : {} : {}", entity, OriginEffects.CAMOUFLAGE.get(), entity.getActivePotionEffects(), isCamouflaged);
            if (isCamouflaged) {
                Main.LOGGER.debug("cancelling... don't know if the next debug message will come through");
                info.cancel();
                Main.LOGGER.debug("cancelled! ... well i think so anyway");
            }
        }

    }
}
