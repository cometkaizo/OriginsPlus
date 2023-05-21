package me.cometkaizo.origins.mixin;

import me.cometkaizo.origins.origin.PhoenixOriginType;
import me.cometkaizo.origins.util.ColorUtils;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.profiler.IProfiler;
import net.minecraft.util.RegistryKey;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraft.world.DimensionType;
import net.minecraft.world.World;
import net.minecraft.world.storage.ISpawnWorldInfo;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.function.Supplier;

import static me.cometkaizo.origins.origin.PhoenixOriginType.SKY_DEATH_COLOR;
import static me.cometkaizo.origins.origin.PhoenixOriginType.SKY_EFFECTS_TIME_TRACKER;

public final class SkyColorMixin {

    private static final String MIXIN_TAG_KEY = "SkyColorMixin_mixin";

    @OnlyIn(Dist.CLIENT)
    @Mixin(ClientWorld.class)
    public static abstract class MixedClientWorld extends World {

        protected MixedClientWorld(ISpawnWorldInfo worldInfo, RegistryKey<World> dimension, DimensionType dimensionType, Supplier<IProfiler> profiler, boolean isRemote, boolean isDebug, long seed) {
            super(worldInfo, dimension, dimensionType, profiler, isRemote, isDebug, seed);
        }

        @Inject(at = @At(value = "RETURN"),
                method = "getSkyColor", cancellable = true)
        protected void newSkyColorVector(BlockPos blockPosIn, float partialTicks, CallbackInfoReturnable<Vector3d> info) {
            float deathTime = SKY_EFFECTS_TIME_TRACKER.getTimerPercentage(PhoenixOriginType.Cooldown.SKY_EFFECT);
            float effectDuration = SKY_EFFECTS_TIME_TRACKER.getTimerDuration(PhoenixOriginType.Cooldown.SKY_EFFECT);
            if (deathTime == 1) return;

            double red = info.getReturnValue().x;
            double green = info.getReturnValue().y;
            double blue = info.getReturnValue().z;

            float partialTicksPercentage = 1 / effectDuration * partialTicks;
            info.setReturnValue(new Vector3d(
                    ColorUtils.fadeInto(SKY_DEATH_COLOR.getRed(), red, deathTime + partialTicksPercentage),
                    ColorUtils.fadeInto(SKY_DEATH_COLOR.getGreen(), green, deathTime + partialTicksPercentage),
                    ColorUtils.fadeInto(SKY_DEATH_COLOR.getBlue(), blue, deathTime + partialTicksPercentage)
            ));
        }

        @Inject(at = @At(value = "RETURN"),
                method = "getCloudColor", cancellable = true)
        protected void newCloudColorVector(float partialTicks, CallbackInfoReturnable<Vector3d> info) {
            float deathTime = SKY_EFFECTS_TIME_TRACKER.getTimerPercentage(PhoenixOriginType.Cooldown.SKY_EFFECT);
            float effectDuration = SKY_EFFECTS_TIME_TRACKER.getTimerDuration(PhoenixOriginType.Cooldown.SKY_EFFECT);
            if (deathTime == 1) return;

            double red = info.getReturnValue().x;
            double green = info.getReturnValue().y;
            double blue = info.getReturnValue().z;

            float partialTicksPercentage = 1 / effectDuration * partialTicks;
            info.setReturnValue(new Vector3d(
                    ColorUtils.fadeInto(SKY_DEATH_COLOR.getRed(), red, deathTime + partialTicksPercentage),
                    ColorUtils.fadeInto(SKY_DEATH_COLOR.getGreen(), green, deathTime + partialTicksPercentage),
                    ColorUtils.fadeInto(SKY_DEATH_COLOR.getBlue(), blue, deathTime + partialTicksPercentage)
            ));
        }
    }
}
