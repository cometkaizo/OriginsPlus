package me.cometkaizo.origins.network;

import me.cometkaizo.origins.Main;
import me.cometkaizo.origins.origin.Origin;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.network.PacketBuffer;
import net.minecraftforge.fml.network.NetworkEvent;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.function.Supplier;

public class C2SRemoveOrigin {

    public static final Logger LOGGER = LogManager.getLogger();

    public C2SRemoveOrigin() {

    }

    public C2SRemoveOrigin(PacketBuffer buffer) {

    }

    public void toBytes(PacketBuffer buffer) {

    }

    public boolean handle(Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> {
            ServerPlayerEntity sender = context.getSender();
            if (sender == null) {
                Main.LOGGER.error("Invalid remove packet: no sender");
                return;
            }

            Origin origin = Origin.getOrigin(sender);
            if (origin != null) origin.remove();
        });
        return true;
    }

}
