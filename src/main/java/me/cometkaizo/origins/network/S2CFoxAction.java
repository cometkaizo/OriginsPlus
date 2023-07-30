package me.cometkaizo.origins.network;

import me.cometkaizo.origins.Main;
import me.cometkaizo.origins.client.ClientUtils;
import me.cometkaizo.origins.origin.FoxOriginType;
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

public class S2CFoxAction {

    public static final Logger LOGGER = LogManager.getLogger();
    public final int playerId;
    private final FoxOriginType.Action action;

    public S2CFoxAction(PlayerEntity player, FoxOriginType.Action action) {
        this.playerId = player.getEntityId();
        this.action = action;
    }

    public S2CFoxAction(PacketBuffer buffer) {
        playerId = buffer.readInt();
        this.action = buffer.readEnumValue(FoxOriginType.Action.class);
    }

    public void toBytes(PacketBuffer buffer) {
        buffer.writeInt(playerId);
        buffer.writeEnumValue(action);
    }


    public boolean handle(Supplier<NetworkEvent.Context> supplier) {
        supplier.get().enqueueWork(() -> {
            World world = DistExecutor.safeCallWhenOn(Dist.CLIENT, () -> ClientUtils::getClientLevel);
            if (world == null) return;

            Entity entity = world.getEntityByID(playerId);
            if (entity == null) Main.LOGGER.error("Invalid action packet: no entity with id {}", playerId);
            if (!(entity instanceof PlayerEntity)) return;

            PlayerEntity player = (PlayerEntity) entity;
            Origin origin = Origin.getOrigin(player);

            if (origin != null) {
                origin.onEvent(action);
            } else {
                Main.LOGGER.error("Invalid action packet: {} does not have origin capability",
                        player.getGameProfile().getName());
            }
        });
        return true;
    }

}
