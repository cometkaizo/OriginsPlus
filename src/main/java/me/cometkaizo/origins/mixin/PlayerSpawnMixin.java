package me.cometkaizo.origins.mixin;

import me.cometkaizo.origins.origin.Origin;
import me.cometkaizo.origins.origin.PhoenixOriginType;
import me.cometkaizo.origins.util.VersionConstants;
import net.minecraft.block.BlockState;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.network.IPacket;
import net.minecraft.network.play.ServerPlayNetHandler;
import net.minecraft.server.management.PlayerInteractionManager;
import net.minecraft.server.management.PlayerList;
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
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

import java.util.Optional;

public final class PlayerSpawnMixin {

    @Mixin(PlayerList.class)
    public static abstract class MixedPlayerList {

        @Shadow @Final private static Logger LOGGER;

        @Inject(method = "func_232644_a_",
                at = @At(
                        shift = At.Shift.BEFORE,
                        value = "INVOKE",
                        target = "Lnet/minecraft/world/server/ServerWorld;hasNoCollisions(Lnet/minecraft/entity/Entity;)Z"),
                locals = LocalCapture.CAPTURE_FAILHARD
        )
        protected void tryRespawnAtDeath(ServerPlayerEntity oldPlayer,
                                             boolean endConquered,
                                             CallbackInfoReturnable<ServerPlayerEntity> info,
                                             BlockPos blockpos,
                                             float f,
                                             boolean flag,
                                             ServerWorld serverworld,
                                             Optional<Vector3d> bedPos,
                                             ServerWorld serverworld1,
                                             PlayerInteractionManager playerinteractionmanager,
                                             ServerPlayerEntity newPlayer) {
            Origin origin = Origin.getOrigin(newPlayer);
            if (origin != null && origin.hasProperty(PhoenixOriginType.Property.RESPAWN_AT_DEATH)) {
                tryRespawnAtDeath(oldPlayer, newPlayer);
            }
        }

        private static void tryRespawnAtDeath(ServerPlayerEntity oldPlayer, ServerPlayerEntity newPlayer) {
            if (shouldNotRespawn(oldPlayer)) return;
            Vector3d deathPos = oldPlayer.getPositionVec();
            double x = deathPos.x, y = deathPos.y, z = deathPos.z;
            float yaw = oldPlayer.rotationYaw, pitch = oldPlayer.rotationPitch;
            ServerWorld deathWorld = oldPlayer.getServerWorld();

            Origin origin = Origin.getOrigin(newPlayer);
            if (origin != null) {
                origin.getTypeData().set(PhoenixOriginType.LAST_DEATH_DIMENSION, deathWorld);
                origin.getTypeData().set(PhoenixOriginType.LAST_DEATH_POS, deathPos);
            }

/*
            deathWorld.getChunkProvider().registerTicket(TicketType.POST_TELEPORT, new ChunkPos(new BlockPos(deathPos)), 1, newPlayer.getEntityId());

            //newPlayer.func_184210_p();
            newPlayer.revive();
            try {
                Field entityBlockPosition = Entity.class.getDeclaredField("entityBlockPosition");
                entityBlockPosition.setAccessible(true);
                Main.LOGGER.info("entity block position for {}: {}", newPlayer, entityBlockPosition.get(newPlayer));
                entityBlockPosition.set(newPlayer, new BlockPos(deathPos));
            } catch (NoSuchFieldException | IllegalAccessException e) {
                throw new RuntimeException(e);
            }
            if (newPlayer.isSleeping())
                newPlayer.stopSleepInBed(true, true);
            newPlayer.changeDimension(deathWorld);
            newPlayer.teleport(deathWorld, x, y, z, yaw, pitch);
            newPlayer.teleportKeepLoaded(x, y, z);
            newPlayer.setRotationYawHead(yaw);*/
            //newPlayer.func_71016_p();

            /*

            if (deathWorld == newPlayer.deathWorld) {
                newPlayer.connection.setPlayerLocation(deathPos.x, deathPos.y, deathPos.z, oldPlayer.rotationYaw, oldPlayer.rotationPitch);
            } else {
                //newPlayer.deathWorld.getChunk(newPlayer.chunkCoordX, newPlayer.chunkCoordZ).removeEntity(newPlayer);
                deathWorld.addEntityIfNotDuplicate(newPlayer);
                newPlayer.teleport(deathWorld, deathPos.x, deathPos.y, deathPos.z, oldPlayer.rotationYaw, oldPlayer.rotationPitch);
                newPlayer.teleportKeepLoaded(deathPos.x, deathPos.y, deathPos.z);
                newPlayer.revive();
            }*/

            /*
//            oldPlayer.getServerWorld().getChunkProvider().registerTicket(TicketType.POST_TELEPORT, new ChunkPos(oldPlayer.getPosition()), 1, newPlayer.getEntityId());
            newPlayer.teleport(oldPlayer.getServerWorld(), deathPos.x, deathPos.y, deathPos.z, oldPlayer.rotationYaw, oldPlayer.rotationPitch);
            newPlayer.changeDimension(oldPlayer.getServerWorld());
            newPlayer.setLocationAndAngles(deathPos.x, deathPos.y, deathPos.z, oldPlayer.rotationYaw, oldPlayer.rotationPitch);*/
        }

        private static boolean shouldNotRespawn(ServerPlayerEntity oldPlayer) {
            return isAboveVoid(oldPlayer) && World.THE_END.equals(oldPlayer.world.getDimensionKey());
        }

        private static boolean isAboveVoid(PlayerEntity player) {
            World world = player.world;
            for (int searchOffset = 0; ; searchOffset++) {
                BlockPos searchPos = player.getPosition().down(searchOffset);
                if (searchPos.getY() < VersionConstants.LOW_BUILD_LIMIT) return true;

                BlockState searchBlock = world.getBlockState(searchPos);
                if (searchBlock.isSolid()) return false;
            }
        }

        @Redirect(method = "func_232644_a_",
                at = @At(value = "INVOKE",
                        target = "Lnet/minecraft/network/play/ServerPlayNetHandler;sendPacket(Lnet/minecraft/network/IPacket;)V",
                        ordinal = 0))
        protected void trySendInvalidSpawnPacket(ServerPlayNetHandler connection, IPacket<?> packet) {
            Origin origin = Origin.getOrigin(connection.player);
            if (origin == null || !origin.hasProperty(PhoenixOriginType.Property.RESPAWN_AT_DEATH)) {
                connection.sendPacket(packet);
            }
        }
    }
}
