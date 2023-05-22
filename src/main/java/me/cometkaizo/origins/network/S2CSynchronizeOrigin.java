package me.cometkaizo.origins.network;

import me.cometkaizo.origins.Main;
import me.cometkaizo.origins.client.ClientUtils;
import me.cometkaizo.origins.origin.Origin;
import me.cometkaizo.origins.origin.OriginType;
import me.cometkaizo.origins.origin.OriginTypes;
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

public class S2CSynchronizeOrigin {

    public static final Logger LOGGER = LogManager.getLogger();
    public final int playerId;
    public final String originType;

    public S2CSynchronizeOrigin(PlayerEntity player, OriginType type) {
        this.playerId = player.getEntityId();
        this.originType = String.valueOf(OriginTypes.getKey(type));
    }

    public S2CSynchronizeOrigin(PacketBuffer buffer) {
        playerId = buffer.readInt();
        originType = buffer.readString();
    }

    public void toBytes(PacketBuffer buffer) {
        buffer.writeInt(playerId);
        buffer.writeString(originType);
    }

    public boolean handle(Supplier<NetworkEvent.Context> supplier) {
        supplier.get().enqueueWork(() -> {
            World world = DistExecutor.safeCallWhenOn(Dist.CLIENT, () -> ClientUtils::getClientLevel);
            if (world == null) return;

            Entity entity = world.getEntityByID(playerId);
            if (entity == null) Main.LOGGER.error("Invalid synchronization packet: no entity with id {}", playerId);
            if (!(entity instanceof PlayerEntity)) return;
            PlayerEntity player = (PlayerEntity) entity;

            OriginType type = OriginTypes.of(originType);
            Origin origin = Origin.getOrigin(player);

            if (origin != null) {
                origin.acceptSynchronization(player, type);
            } else {
                Main.LOGGER.error("Invalid synchronization packet: {} does not have origin capability",
                        player.getGameProfile().getName());
            }
        });
        return true;
    }

}
