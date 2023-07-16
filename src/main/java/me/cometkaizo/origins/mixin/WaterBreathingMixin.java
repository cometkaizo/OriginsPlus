package me.cometkaizo.origins.mixin;

import me.cometkaizo.origins.common.OriginDamageSources;
import me.cometkaizo.origins.origin.Origin;
import me.cometkaizo.origins.origin.SharkOriginType;
import net.minecraft.client.gui.AbstractGui;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.fluid.Fluid;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.IRecipe;
import net.minecraft.tags.ITag;
import net.minecraft.util.DamageSource;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.SoundEvent;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.client.gui.ForgeIngameGui;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import javax.annotation.Nonnull;
import java.util.Collection;

public final class WaterBreathingMixin {

    @Mixin(LivingEntity.class)
    public static abstract class IsInWater extends Entity {

        @Shadow public abstract boolean attackEntityFrom(@Nonnull DamageSource source, float amount);

        public IsInWater(EntityType<?> type, World world) {
            super(type, world);
        }

        @Redirect(at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/LivingEntity;areEyesInFluid(Lnet/minecraft/tags/ITag;)Z"), method = "baseTick")
        protected boolean isSubmergedInProxy(LivingEntity entity, ITag<Fluid> fluidTag) {
            boolean submerged = areEyesInFluid(fluidTag);
            Origin origin = Origin.getOrigin(entity);
            if (origin != null) {
                return origin.hasProperty(SharkOriginType.Property.WATER_BREATHING_PROPERTY) != submerged;
            }
            return submerged;
        }

        @Inject(at = @At("RETURN"), method = "decreaseAirSupply", cancellable = true)
        protected void decreaseAirSupply(int air, CallbackInfoReturnable<Integer> info) {
            Origin origin = Origin.getOrigin(this);
            if (origin != null && origin.hasProperty(SharkOriginType.Property.WATER_BREATHING_PROPERTY) && isInRain()) {
                if (rand.nextInt(2) > 0) {
                    info.setReturnValue(air);
                }
            }
        }

        private boolean isInRain() {
            BlockPos blockpos = getPosition();
            return world.isRainingAt(blockpos) ||
                    world.isRainingAt(new BlockPos(blockpos.getX(), getBoundingBox().maxY, blockpos.getZ()));
        }

        @Redirect(at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/LivingEntity;attackEntityFrom(Lnet/minecraft/util/DamageSource;F)Z"), method = "baseTick")
        protected boolean damageByDehydration(LivingEntity entity, DamageSource source, float amount) {
            if (source == DamageSource.DROWN) {
                Origin origin = Origin.getOrigin(entity);
                if (origin != null && origin.hasProperty(SharkOriginType.Property.WATER_BREATHING_PROPERTY)) {
                    return attackEntityFrom(OriginDamageSources.DEHYDRATION, amount);
                }
            }
            return attackEntityFrom(source, amount);
        }
    }

    @Mixin(PlayerEntity.class)
    public static abstract class IsInWaterTurtleShell extends LivingEntity {

        @Shadow public abstract void playSound(@Nonnull SoundEvent soundIn, float volume, float pitch);

        @Shadow public abstract void playSound(SoundEvent p_213823_1_, SoundCategory p_213823_2_, float p_213823_3_, float p_213823_4_);

        @Shadow public abstract int resetRecipes(Collection<IRecipe<?>> p_195069_1_);

        @Shadow public abstract boolean addItemStackToInventory(ItemStack p_191521_1_);

        protected IsInWaterTurtleShell(EntityType<? extends LivingEntity> entityType, World world) {
            super(entityType, world);
        }

        @Redirect(at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/player/PlayerEntity;areEyesInFluid(Lnet/minecraft/tags/ITag;)Z"), method = "updateTurtleHelmet")
        protected boolean isSubmergedInProxy(PlayerEntity player, ITag<Fluid> fluidTag) {
            Origin origin = Origin.getOrigin(player);
            if (origin != null) {
                return origin.hasProperty(SharkOriginType.Property.WATER_BREATHING_PROPERTY) != areEyesInFluid(fluidTag);
            }
            return areEyesInFluid(fluidTag);
        }
    }

    @Mixin(ForgeIngameGui.class)
    public static abstract class MixedForgeIngameGui extends AbstractGui {

        @Redirect(method = "renderAir", at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/player/PlayerEntity;areEyesInFluid(Lnet/minecraft/tags/ITag;)Z"))
        protected boolean isSubmergedInProxy(PlayerEntity player, ITag<Fluid> fluidTag) {
            boolean submerged = player.areEyesInFluid(fluidTag);
            Origin origin = Origin.getOrigin(player);
            if (origin != null) {
                return origin.hasProperty(SharkOriginType.Property.WATER_BREATHING_PROPERTY) != submerged;
            }
            return submerged;
        }

    }
}
