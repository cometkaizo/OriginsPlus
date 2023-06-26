package me.cometkaizo.origins.mixin;

import me.cometkaizo.origins.origin.ArachnidOriginType;
import me.cometkaizo.origins.origin.Origin;
import me.cometkaizo.origins.origin.OriginTypes;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.WebBlock;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.common.IForgeShearable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

public final class CobwebMixin {

    @Mixin(WebBlock.class)
    public static abstract class MixedWebBlock extends Block implements IForgeShearable {
        public MixedWebBlock(Properties properties) {
            super(properties);
        }

        @Inject(at = @At("HEAD"), method = "onEntityCollision", cancellable = true)
        protected void onEntityCollision(BlockState state, World world, BlockPos pos, Entity entity, CallbackInfo info) {
            Origin origin = Origin.getOrigin(entity);
            if (origin != null && origin.hasProperty(ArachnidOriginType.Property.NO_COBWEB_SLOWDOWN)) {
                if (!origin.isServerSide()) origin.getTypeData(OriginTypes.ARACHNID.get()).set(ArachnidOriginType.IN_COBWEB, true);
                info.cancel();
            }
        }
    }

}
