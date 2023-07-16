package me.cometkaizo.origins.mixin;

import me.cometkaizo.origins.origin.Origin;
import me.cometkaizo.origins.origin.SharkOriginType;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.enchantment.Enchantments;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.projectile.AbstractArrowEntity;
import net.minecraft.entity.projectile.TridentEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.EntityRayTraceResult;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

public final class ImpalingMixin {

    @Mixin(TridentEntity.class)
    public static abstract class MixedTridentEntity extends AbstractArrowEntity {
        @Shadow private ItemStack thrownStack;

        protected MixedTridentEntity(EntityType<? extends AbstractArrowEntity> type, World worldIn) {
            super(type, worldIn);
        }

        @ModifyVariable(method = "onEntityHit", at = @At("STORE"), ordinal = 1)
        protected float getCustomDamageModifier(float f, EntityRayTraceResult result) {
            Entity target = result.getEntity();
            return 8 + getExtraDamage(target);
        }

        private float getExtraDamage(Entity target) {
            int impalingLevel = EnchantmentHelper.getEnchantmentLevel(Enchantments.IMPALING, thrownStack);
            if (shouldApplyExtraDamage(impalingLevel, target))
                return impalingLevel * 2.5F;
            return 0;
        }

        private boolean shouldApplyExtraDamage(int level, Entity target) {
            return !isExtraDamageAppliedByVanilla(level, target) &&
                    target.isWet() || Origin.hasProperty(target, SharkOriginType.Property.VULNERABLE_TO_IMPALING);
        }

        private boolean isExtraDamageAppliedByVanilla(int level, Entity target) {
            if (!(target instanceof LivingEntity)) return false;
            LivingEntity livingEntity = (LivingEntity) target;
            return Enchantments.IMPALING.calcDamageByCreature(level, livingEntity.getCreatureAttribute()) > 0;
        }
    }

}
