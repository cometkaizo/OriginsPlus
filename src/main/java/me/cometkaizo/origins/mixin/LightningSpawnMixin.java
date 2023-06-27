package me.cometkaizo.origins.mixin;

import me.cometkaizo.origins.origin.ElytrianOriginType;
import me.cometkaizo.origins.origin.Origin;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.effect.LightningBoltEntity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.profiler.IProfiler;
import net.minecraft.util.RegistryKey;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.DimensionType;
import net.minecraft.world.GameRules;
import net.minecraft.world.World;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.server.ServerWorld;
import net.minecraft.world.storage.ISpawnWorldInfo;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.stream.Collectors;

public final class LightningSpawnMixin {

    @Mixin(ServerWorld.class)
    public static abstract class MixedServerWorld extends World {

        @Shadow public abstract List<ServerPlayerEntity> getPlayers(Predicate<? super ServerPlayerEntity> predicateIn);

        protected MixedServerWorld(ISpawnWorldInfo worldInfo, RegistryKey<World> dimension, DimensionType dimensionType, Supplier<IProfiler> profiler, boolean isRemote, boolean isDebug, long seed) {
            super(worldInfo, dimension, dimensionType, profiler, isRemote, isDebug, seed);
        }

        @Inject(method = "tickEnvironment", at = @At(value = "INVOKE", target = "Lnet/minecraft/profiler/IProfiler;startSection(Ljava/lang/String;)V", shift = At.Shift.AFTER, ordinal = 0))
        protected void spawnLightningOnElytrian(Chunk chunk, int randomTickSpeed, CallbackInfo info) {
            if (isRaining() && isThundering() && rand.nextInt(ElytrianOriginType.LIGHTNING_SPAWN_CHANCE) == 0) {
                List<ServerPlayerEntity> players = getStrikeablePlayers();
                ServerPlayerEntity player = players.get(rand.nextInt(players.size()));
                BlockPos position = player.getPosition();

                spawnLightning(position);
            }
        }

        private void spawnLightning(BlockPos position) {
            DifficultyInstance difficultyinstance = this.getDifficultyForLocation(position);
            boolean isEffectOnly = this.getGameRules().getBoolean(GameRules.DO_MOB_SPAWNING) && this.rand.nextDouble() < (double)difficultyinstance.getAdditionalDifficulty() * 0.01D;

            LightningBoltEntity lightningboltentity = EntityType.LIGHTNING_BOLT.create(this);
            lightningboltentity.moveForced(Vector3d.copyCenteredHorizontally(position));
            lightningboltentity.setEffectOnly(isEffectOnly);
            this.addEntity(lightningboltentity);
        }

        private List<ServerPlayerEntity> getStrikeablePlayers() {
            return getPlayers(ServerPlayerEntity::isElytraFlying).stream()
                    .filter(p -> {
                        Origin origin = Origin.getOrigin(p);
                        return origin != null && origin.getType() instanceof ElytrianOriginType;
                    })
                    .filter(p -> p.getPosY() >= ElytrianOriginType.LIGHTNING_STRIKE_MIN_Y)
                    .collect(Collectors.toList());
        }
    }

}
