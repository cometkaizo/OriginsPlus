package me.cometkaizo.origins.network;

import me.cometkaizo.origins.client.ClientUtils;
import me.cometkaizo.origins.origin.Origin;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.network.PacketBuffer;
import net.minecraft.world.World;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.fml.network.NetworkEvent;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.function.Supplier;

public class S2COpenOriginScreen {

    public static final Logger LOGGER = LogManager.getLogger();
    public final int playerId;

    public S2COpenOriginScreen(PlayerEntity player) {
        this.playerId = player.getEntityId();
    }

    public S2COpenOriginScreen(PacketBuffer buffer) {
        playerId = buffer.readInt();
    }

    public void toBytes(PacketBuffer buffer) {
        buffer.writeInt(playerId);
    }


    public boolean handle(Supplier<NetworkEvent.Context> supplier) {
        supplier.get().enqueueWork(() -> {
            World world = DistExecutor.safeCallWhenOn(Dist.CLIENT, () -> ClientUtils::getClientLevel);
            if (world == null) return;

            Entity entity = world.getEntityByID(playerId);
            if (entity == null) LOGGER.error("Invalid ui packet: no entity with id {}", playerId);
            if (!(entity instanceof PlayerEntity)) return;

            PlayerEntity player = (PlayerEntity) entity;
            Origin origin = Origin.getOrigin(player);

            if (origin != null) {
                origin.setShouldOpenOriginScreen();
            } else {
                LOGGER.error("Invalid ui packet: {} does not have origin capability",
                        player.getGameProfile().getName());
            }
        });
        return true;
    }

}
