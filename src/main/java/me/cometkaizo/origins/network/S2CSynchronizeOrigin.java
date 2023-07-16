package me.cometkaizo.origins.network;

import me.cometkaizo.origins.Main;
import me.cometkaizo.origins.client.ClientUtils;
import me.cometkaizo.origins.origin.Origin;
import me.cometkaizo.origins.origin.OriginType;
import me.cometkaizo.origins.origin.OriginTypes;
import me.cometkaizo.origins.origin.client.ClientOrigin;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.nbt.CompoundNBT;
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
    public final CompoundNBT typeData;

    public S2CSynchronizeOrigin(PlayerEntity player, OriginType type, CompoundNBT typeData) {
        this.playerId = player.getEntityId();
        this.originType = String.valueOf(OriginTypes.getKey(type));
        this.typeData = typeData;
    }

    public S2CSynchronizeOrigin(PacketBuffer buffer) {
        playerId = buffer.readInt();
        originType = buffer.readString();
        typeData = buffer.readCompoundTag();
    }

    public void toBytes(PacketBuffer buffer) {
        buffer.writeInt(playerId);
        buffer.writeString(originType);
        buffer.writeCompoundTag(typeData);
    }

    public boolean handle(Supplier<NetworkEvent.Context> supplier) {
        supplier.get().enqueueWork(() -> {
            World world = DistExecutor.safeCallWhenOn(Dist.CLIENT, () -> ClientUtils::getClientLevel);
            if (world == null) {
                Main.LOGGER.info("No world, sending removal packet");
                DistExecutor.safeRunWhenOn(Dist.CLIENT, () -> ClientOrigin::sendRemovalOriginPacket);
                return;
            }

            Entity entity = world.getEntityByID(playerId);
            if (entity == null) {
                Main.LOGGER.error("No entity with id {}; sending removal packet", playerId);
                DistExecutor.safeRunWhenOn(Dist.CLIENT, () -> ClientOrigin::sendRemovalOriginPacket);
                return;
            }
            if (!(entity instanceof PlayerEntity)) {
                Main.LOGGER.info("Entity {} is not player; sending removal packet", entity);
                DistExecutor.safeRunWhenOn(Dist.CLIENT, () -> ClientOrigin::sendRemovalOriginPacket);
                return;
            }
            PlayerEntity player = (PlayerEntity) entity;

            OriginType type = OriginTypes.of(originType);
            Origin origin = Origin.getOrigin(player);

            if (origin != null) {
                origin.acceptSynchronization(player, type, typeData);
            } else {
                Main.LOGGER.info("No origin, sending removal packet");
                DistExecutor.safeRunWhenOn(Dist.CLIENT, () -> ClientOrigin::sendRemovalOriginPacket);
            }
        });
        return true;
    }

}
