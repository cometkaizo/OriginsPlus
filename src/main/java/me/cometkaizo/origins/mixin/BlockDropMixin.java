package me.cometkaizo.origins.mixin;

import me.cometkaizo.origins.Main;
import me.cometkaizo.origins.common.OriginTags;
import me.cometkaizo.origins.origin.EnderianOriginType;
import me.cometkaizo.origins.origin.Origin;
import me.cometkaizo.origins.util.CollUtils;
import net.minecraft.advancements.criterion.EnchantmentPredicate;
import net.minecraft.advancements.criterion.ItemPredicate;
import net.minecraft.advancements.criterion.MinMaxBounds;
import net.minecraft.block.AbstractBlock;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.enchantment.Enchantments;
import net.minecraft.entity.Entity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.PickaxeItem;
import net.minecraft.loot.LootContext;
import net.minecraft.loot.LootParameters;
import net.minecraft.loot.conditions.MatchTool;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.IItemProvider;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.server.ServerWorld;
import net.minecraftforge.common.Tags;
import net.minecraftforge.common.ToolType;
import net.minecraftforge.common.extensions.IForgeBlock;
import org.apache.logging.log4j.Logger;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

import java.util.List;
import java.util.Map;
import java.util.Set;

import static me.cometkaizo.origins.util.ClassUtils.getFieldOfType;
import static me.cometkaizo.origins.util.ClassUtils.getFieldOrThrow;

public final class BlockDropMixin {

    private static final String MIXIN_TAG_KEY = "BlockDropMixin_mixin";
    private static final String ORIGIN_SILK_TOUCH = MIXIN_TAG_KEY + "_silkTouch";

    @Mixin(MatchTool.class)
    public static abstract class MixedMatchTool {

        @Inject(at = @At(value = "INVOKE",
                target = "Lnet/minecraft/advancements/criterion/ItemPredicate;test(Lnet/minecraft/item/ItemStack;)Z",
                shift = At.Shift.BEFORE),
                method = "test(Lnet/minecraft/loot/LootContext;)Z",
                locals = LocalCapture.CAPTURE_FAILHARD
        )
        protected void test(LootContext lootContext, CallbackInfoReturnable<Boolean> info, ItemStack tool) {
            Entity entity = lootContext.get(LootParameters.THIS_ENTITY);
            if (tool == null) return;

            Origin origin = Origin.getOrigin(entity);
            if (origin != null && origin.hasLabel(EnderianOriginType.Label.SILK_TOUCH)) {
                BlockState block = lootContext.get(LootParameters.BLOCK_STATE);
                if (originsPlus$shouldDropNormally(tool, block)) return;
                CompoundNBT data = tool.getOrCreateTag();
                data.putBoolean(ORIGIN_SILK_TOUCH, true);
            }
        }

        @Unique
        private static boolean originsPlus$shouldDropNormally(ItemStack tool, BlockState block) {
            return block == null ||
                    (!block.matchesBlock(Blocks.CAKE) && tool.canHarvestBlock(block)) ||
                    originsPlus$shouldDropNormally(tool.getToolTypes(), block);
        }

        @Unique
        private static boolean originsPlus$shouldDropNormally(Set<ToolType> toolTypes, BlockState block) {
            for (ToolType toolType : toolTypes) {
                Map<ToolType, Tags.IOptionalNamedTag<Block>> dropTags = OriginTags.Blocks.ENDERIAN_NORMAL_DROP_TAGS;
                if (dropTags.containsKey(toolType) &&
                        block.isIn(dropTags.get(toolType))) {
                    return true;
                }
            }
            return false;
        }
    }

    @Mixin(ItemPredicate.class)
    public static class MixedItemPredicate {

        @Unique
        private static final String INT_BOUND_MIN_FIELD = "field_211348_f";
        @Unique
        private static final String INT_BOUND_MAX_FIELD = "field_211349_g";

        @Inject(at = @At(value = "RETURN", ordinal = 7),
                method = "test",
                locals = LocalCapture.CAPTURE_FAILHARD,
                cancellable = true)
        protected void testEnchantment(ItemStack tool,
                                       CallbackInfoReturnable<Boolean> info,
                                       Map<Enchantment, Integer> map,
                                       EnchantmentPredicate[] var3,
                                       int var4,
                                       int var5,
                                       EnchantmentPredicate enchantmentpredicate) {

            if (tool == null) return;
            if (EnchantmentHelper.getEnchantmentLevel(Enchantments.FORTUNE, tool) > 0) return;

            try {
                Enchantment enchantment = getFieldOfType(Enchantment.class, enchantmentpredicate);
                MinMaxBounds.IntBound bound = getFieldOfType(MinMaxBounds.IntBound.class, enchantmentpredicate);
                Long boundMinSqr = (Long) getFieldOrThrow(INT_BOUND_MIN_FIELD, bound);
                Long boundMaxSqr = (Long) getFieldOrThrow(INT_BOUND_MAX_FIELD, bound);

                if (enchantment != Enchantments.SILK_TOUCH) return;
                if (boundMinSqr == null || boundMinSqr <= 0) return;
                if (boundMaxSqr != null && boundMaxSqr >= 1) return;

                if (!tool.getOrCreateTag().contains(ORIGIN_SILK_TOUCH)) return;
                if (tool.getOrCreateTag().getBoolean(ORIGIN_SILK_TOUCH)) {
                    tool.getOrCreateTag().remove(ORIGIN_SILK_TOUCH);
                    info.setReturnValue(true);
                }
            } catch (UnsupportedOperationException e) {
                Main.LOGGER.error("Unsupported operation: ", e);
            }
        }
    }

    @Mixin(Block.class)
    public static abstract class MixedBlock extends AbstractBlock implements IItemProvider, IForgeBlock {

        @Shadow
        @Final
        protected static Logger LOGGER;

        @Shadow public abstract String toString();

        public MixedBlock(Properties properties) {
            super(properties);
        }

        @Inject(at = @At(value = "RETURN", shift = At.Shift.BEFORE),
                method = "getDrops(Lnet/minecraft/block/BlockState;Lnet/minecraft/world/server/ServerWorld;Lnet/minecraft/util/math/BlockPos;Lnet/minecraft/tileentity/TileEntity;Lnet/minecraft/entity/Entity;Lnet/minecraft/item/ItemStack;)Ljava/util/List;",
                cancellable = true
        )
        private static void getDrops(BlockState blockState,
                                     ServerWorld worldIn,
                                     BlockPos pos,
                                     TileEntity tileEntityIn,
                                     Entity blockBreaker,
                                     ItemStack tool,
                                     CallbackInfoReturnable<List<ItemStack>> info) {

            Origin origin = Origin.getOrigin(blockBreaker);
            if (origin != null && originsPlus$shouldDropOriginal(origin, blockState, tool)) {
                List<ItemStack> returnValue = CollUtils.listOf(new ItemStack(blockState.getBlock().asItem()));

                info.setReturnValue(returnValue);
            }
        }

        /**
         * Determines if a block should drop its item even if you can't collect it with silk touch
         * @param blockState the block to decide
         * @param tool the tool used
         * @return whether the block should drop its item
         */
        @Unique
        private static boolean originsPlus$shouldDropOriginal(Origin origin, BlockState blockState, ItemStack tool) {
            return origin.hasLabel(EnderianOriginType.Label.SILK_TOUCH) &&
                    blockState.matchesBlock(Blocks.SPAWNER) &&
                    (tool.getItem() instanceof PickaxeItem && ((PickaxeItem) tool.getItem()).getTier().getHarvestLevel() >= 3);
        }
    }
}
