package me.cometkaizo.origins.mixin;

import me.cometkaizo.origins.origin.Origin;
import me.cometkaizo.origins.origin.SharkOriginType;
import net.minecraft.block.AbstractBlock;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.entity.Entity;
import net.minecraft.entity.item.ItemEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.tags.FluidTags;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraft.world.World;
import net.minecraft.world.server.ServerWorld;
import org.apache.logging.log4j.Logger;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

import javax.annotation.Nullable;
import java.util.Collections;
import java.util.List;

import static me.cometkaizo.origins.util.TagUtils.getVector;
import static me.cometkaizo.origins.util.TagUtils.putVector;

public final class BlockDropDirectionMixin {

    private static final String MIXIN_TAG_KEY = "BlockDropDirectionMixin_mixin";

    @Mixin(Block.class)
    public static abstract class MixedBlock extends AbstractBlock {
        private static final String ITEM_MOTION_KEY = MIXIN_TAG_KEY + "_itemMotion";

        @Shadow
        @Final
        protected static Logger LOGGER;

        @Shadow public abstract String toString();

        @Shadow
        public static List<ItemStack> getDrops(BlockState state, ServerWorld worldIn, BlockPos pos, @Nullable TileEntity tileEntityIn, @Nullable Entity entityIn, ItemStack stack) {
            return null;
        }

        @Shadow
        public static void spawnAsEntity(World worldIn, BlockPos pos, ItemStack stack) {
        }

        public MixedBlock(Properties properties) {
            super(properties);
        }

        @Redirect(at = @At(value = "INVOKE", target = "Lnet/minecraft/block/Block;getDrops(Lnet/minecraft/block/BlockState;Lnet/minecraft/world/server/ServerWorld;Lnet/minecraft/util/math/BlockPos;Lnet/minecraft/tileentity/TileEntity;Lnet/minecraft/entity/Entity;Lnet/minecraft/item/ItemStack;)Ljava/util/List;"),
                method = "spawnDrops(Lnet/minecraft/block/BlockState;Lnet/minecraft/world/World;Lnet/minecraft/util/math/BlockPos;Lnet/minecraft/tileentity/TileEntity;Lnet/minecraft/entity/Entity;Lnet/minecraft/item/ItemStack;)V"
        )
        private static List<ItemStack> setDropsMotion(BlockState state, ServerWorld world, BlockPos pos, TileEntity tileEntity, Entity entity, ItemStack stack) {
            List<ItemStack> drops = getDrops(state, world, pos, tileEntity, entity, stack);
            if (drops == null || entity == null) return drops;

            Origin origin = Origin.getOrigin(entity);
            if (origin == null || !origin.hasProperty(SharkOriginType.Property.PULL_DROPPED_ITEMS_UNDERWATER) ||
                    !entity.areEyesInFluid(FluidTags.WATER)) return drops;

            double amp = SharkOriginType.ITEM_PULL_AMP;
            drops.forEach((stackToSpawn) -> {
                Vector3d difference = entity.getEyePosition(0).subtract(pos.getX(), pos.getY(), pos.getZ());
                Vector3d motion = new Vector3d(difference.x * amp, difference.y * amp, difference.z * amp);

                CompoundNBT motionData = stackToSpawn.getOrCreateChildTag(ITEM_MOTION_KEY);
                putVector(motionData, motion);
                spawnAsEntity(world, pos, stackToSpawn);
            });

            return Collections.emptyList();
        }

        @Inject(at = @At(value = "INVOKE", target = "Lnet/minecraft/world/World;addEntity(Lnet/minecraft/entity/Entity;)Z"),
                method = "spawnAsEntity",
                locals = LocalCapture.CAPTURE_FAILHARD)
        private static void giveItemEntityMotion(World worldIn, BlockPos pos, ItemStack stack, CallbackInfo info,
                                                   float f, double d0, double d1, double d2, ItemEntity itementity) {
            if (stack.getTag() == null || !stack.getOrCreateTag().contains(ITEM_MOTION_KEY)) return;
            CompoundNBT motionData = stack.getOrCreateChildTag(ITEM_MOTION_KEY);
            itementity.setMotion(getVector(motionData));
            Vector3d position = itementity.getPositionVec();
            itementity.setPosition(position.x, Math.floor(position.y) + 0.65, position.z);
            stack.removeChildTag(ITEM_MOTION_KEY);
        }
    }
}
