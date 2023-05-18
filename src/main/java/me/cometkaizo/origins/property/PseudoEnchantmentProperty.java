package me.cometkaizo.origins.property;

import me.cometkaizo.origins.Main;
import me.cometkaizo.origins.mixin.BlockDropMixin;
import net.minecraft.advancements.criterion.EnchantmentPredicate;
import net.minecraft.advancements.criterion.MinMaxBounds;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.CompoundNBT;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.ArrayList;
import java.util.List;

import static me.cometkaizo.origins.util.ClassUtils.getFieldOrThrow;

/**
 * Used to modify {@link net.minecraft.advancements.criterion.ItemPredicate} to match
 * enchantments even if the item does not have it.
 * <p>
 * For instance, using {@link Builder#withEnchantment(Enchantment)} for Silk Touch will
 * make items act as though they were enchanted with silk touch even they are not.
 * <p>
 * Using {@link Builder#withCounterEnchantment(Enchantment)} means the "enchantment effect" will
 * not be applied if this enchantment is found on the item. For instance, you can apply the Silk
 * Touch effect to all items but only if they do not have Fortune.
 */
@Requires(BlockDropMixin.class)
public class PseudoEnchantmentProperty extends AbstractProperty {
    public static final String PSEUDO_ENCHANTMENTS_PREFIX = Main.MOD_ID + "_pseudo_enchantments_";
    private static final String ENCHANTMENT_PREDICATE_ENCH_FIELD = "enchantment";
    private static final String ENCHANTMENT_PREDICATE_LEVEL_FIELD = "levels";
    private static final String INT_BOUND_MIN_FIELD = "minSquared";
    private static final String INT_BOUND_MAX_FIELD = "maxSquared";
    private final List<Enchantment> enchantments;
    private final List<Enchantment> counterEnchantments;

    protected PseudoEnchantmentProperty(String name, List<Enchantment> enchantments, List<Enchantment> counterEnchantments) {
        super(name);
        this.enchantments = enchantments;
        this.counterEnchantments = counterEnchantments;
    }

    public void injectEnchantmentInTool(ItemStack tool) {
        CompoundNBT data = tool.getOrCreateTag();
        if (hasCounterEnchantments(tool)) return;
        for (Enchantment enchantment : enchantments) {
            data.putBoolean(PSEUDO_ENCHANTMENTS_PREFIX + enchantment.getName(), true);
        }
    }

    public void injectValidation(EnchantmentPredicate predicate, ItemStack tool, CallbackInfoReturnable<Boolean> info) {
        if (tool == null) return;
        if (!isTestingForExistence(predicate)) return;
        if (hasCounterEnchantments(tool)) return;

        Enchantment enchantment = (Enchantment) getFieldOrThrow(ENCHANTMENT_PREDICATE_ENCH_FIELD, predicate);

        boolean hasEnchantmentTag = getAndResetBoolean(tool, PSEUDO_ENCHANTMENTS_PREFIX + enchantment);
        if (hasEnchantmentTag) info.setReturnValue(true);
    }

    private static boolean isTestingForExistence(EnchantmentPredicate predicate) {
        MinMaxBounds.IntBound bound = (MinMaxBounds.IntBound) getFieldOrThrow(ENCHANTMENT_PREDICATE_LEVEL_FIELD, predicate);
        Long boundMinSqr = (Long) getFieldOrThrow(INT_BOUND_MIN_FIELD, bound);
        Long boundMaxSqr = (Long) getFieldOrThrow(INT_BOUND_MAX_FIELD, bound);

        if (boundMinSqr == null || boundMinSqr <= 0) return false;
        return boundMaxSqr == null || boundMaxSqr >= 1;
    }

    private static boolean getAndResetBoolean(ItemStack tool, String key) {
        boolean result = tool.getOrCreateTag().getBoolean(key);
        tool.removeChildTag(key);
        return result;
    }

    private boolean hasCounterEnchantments(ItemStack tool) {
        for (Enchantment counterEnchantment : counterEnchantments) {
            if (EnchantmentHelper.getEnchantmentLevel(counterEnchantment, tool) > 0) return true;
        }
        return false;
    }

    public static class Builder {
        private String name = "Pseudo Enchantment";
        private final List<Enchantment> enchantments = new ArrayList<>(1);
        private final List<Enchantment> counterEnchantments = new ArrayList<>(1);

        public Builder setName(String name) {
            this.name = name;
            return this;
        }

        public Builder withEnchantment(Enchantment enchantment) {
            this.enchantments.add(enchantment);
            return this;
        }

        public Builder withCounterEnchantment(Enchantment enchantment) {
            this.counterEnchantments.add(enchantment);
            return this;
        }

        public PseudoEnchantmentProperty build() {
            return new PseudoEnchantmentProperty(name, enchantments, counterEnchantments);
        }
    }

}
