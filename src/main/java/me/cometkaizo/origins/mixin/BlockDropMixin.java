package me.cometkaizo.origins.mixin;

import me.cometkaizo.origins.Main;
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

import static me.cometkaizo.origins.util.ClassUtils.getFieldOrThrow;

public final class BlockDropMixin {

    private static final String MIXIN_TAG_KEY = "BlockDropMixin_mixin";
    private static final String ORIGIN_SILK_TOUCH = MIXIN_TAG_KEY + "_silkTouch";

    @Mixin(MatchTool.class)
    public static abstract class MixedMatchTool {

        private static final String ITEM_STACK_OWNER = Main.MOD_ID + "_owner";

        @Inject(at = @At(value = "INVOKE",
                target = "Lnet/minecraft/advancements/criterion/ItemPredicate;test(Lnet/minecraft/item/ItemStack;)Z",
                shift = At.Shift.BEFORE),
                method = "test(Lnet/minecraft/loot/LootContext;)Z",
                locals = LocalCapture.CAPTURE_FAILHARD
        )
        protected void test(LootContext lootContext, CallbackInfoReturnable<Boolean> info, ItemStack tool) {
            Entity entity = lootContext.get(LootParameters.THIS_ENTITY);
            if (tool == null) return;

            Origin origin = Origin.getOrigin(entity);/*
            if (origin != null) {
                for (PseudoEnchantmentProperty property : origin.getProperties(PseudoEnchantmentProperty.class)) {
                    property.injectEnchantmentInTool(tool);
                }
                if (entity instanceof PlayerEntity) tool.getOrCreateTag().putString(ITEM_STACK_OWNER, ((PlayerEntity) entity).getGameProfile().getName());
            }*/
            if (origin != null && origin.hasProperty(EnderianOriginType.Property.SILK_TOUCH)) {
                CompoundNBT data = tool.getOrCreateTag();
                data.putBoolean(ORIGIN_SILK_TOUCH, true);
            }
        }
    }

    @Mixin(ItemPredicate.class)
    public static class MixedItemPredicate {

        private static final String ITEM_STACK_OWNER = Main.MOD_ID + "_owner";
        private static final String ENCHANTMENT_PREDICATE_ENCH_FIELD = "enchantment";
        private static final String ENCHANTMENT_PREDICATE_LEVEL_FIELD = "levels";
        private static final String INT_BOUND_MIN_FIELD = "minSquared";
        private static final String INT_BOUND_MAX_FIELD = "maxSquared";

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
                                       EnchantmentPredicate enchantmentpredicate) {/*
            CompoundNBT data = tool.getOrCreateTag();
            if (!data.contains(ITEM_STACK_OWNER)) return;
            String ownerUsername = data.getString(ITEM_STACK_OWNER);
            PlayerEntity player = ServerLifecycleHooks.getCurrentServer().getPlayerList().getPlayerByUsername(ownerUsername);

            Origin origin = Origin.getOrigin(player);
            if (origin != null) {
                for (PseudoEnchantmentProperty property : origin.getProperties(PseudoEnchantmentProperty.class)) {
                    property.injectValidation(enchantmentpredicate, tool, info);
                }
            }*/

            if (tool == null) return;
            if (EnchantmentHelper.getEnchantmentLevel(Enchantments.FORTUNE, tool) > 0) return;

            Enchantment enchantment = (Enchantment) getFieldOrThrow(ENCHANTMENT_PREDICATE_ENCH_FIELD, enchantmentpredicate);
            MinMaxBounds.IntBound bound = (MinMaxBounds.IntBound) getFieldOrThrow(ENCHANTMENT_PREDICATE_LEVEL_FIELD, enchantmentpredicate);
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
            if (origin != null && origin.hasProperty(EnderianOriginType.Property.SILK_TOUCH) &&
                    shouldDropOriginal(blockState, tool)) {
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
        private static boolean shouldDropOriginal(BlockState blockState, ItemStack tool) {
            return blockState.matchesBlock(Blocks.SPAWNER) &&
                    (tool.getItem() instanceof PickaxeItem && ((PickaxeItem) tool.getItem()).getTier().getHarvestLevel() >= 3);
        }
    }
}
