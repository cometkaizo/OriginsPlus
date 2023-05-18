package me.cometkaizo.origins.network;

import me.cometkaizo.origins.Main;
import me.cometkaizo.origins.origin.Origin;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.network.PacketBuffer;
import net.minecraftforge.fml.network.NetworkEvent;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.function.Supplier;

public class C2SAcknowledgeSyncOrigin {

    public static final Logger LOGGER = LogManager.getLogger();

    public C2SAcknowledgeSyncOrigin() {

    }

    public C2SAcknowledgeSyncOrigin(PacketBuffer buffer) {

    }

    public void toBytes(PacketBuffer buffer) {

    }

    public boolean handle(Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> {
            ServerPlayerEntity sender = context.getSender();
            if (sender == null) {
                Main.LOGGER.error("Invalid synchronization packet: no sender");
                return;
            }

            Origin origin = Origin.getOrigin(sender);
            if (origin != null) {
                origin.setSynchronized();
                Main.LOGGER.info("Origins synchronized for {} to be {}",
                        sender.getName().getString(),
                        origin.getType().getName());
            } else {
                Main.LOGGER.error("Invalid synchronization packet: {} does not have origin capability",
                        sender.getGameProfile().getName());
            }
        });
        return true;
    }

}
