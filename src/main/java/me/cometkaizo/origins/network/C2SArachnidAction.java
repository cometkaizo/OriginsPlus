package me.cometkaizo.origins.network;

import me.cometkaizo.origins.origin.CapabilityOrigin;
import me.cometkaizo.origins.origin.ArachnidOriginType;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.network.PacketBuffer;
import net.minecraftforge.fml.network.NetworkEvent;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.function.Supplier;

public class C2SArachnidAction {

    public static final Logger LOGGER = LogManager.getLogger();
    private final ArachnidOriginType.Action action;

    public C2SArachnidAction(ArachnidOriginType.Action action) {
        this.action = action;
    }

    public C2SArachnidAction(PacketBuffer packetBuffer) {
        this.action = packetBuffer.readEnumValue(ArachnidOriginType.Action.class);
    }

    public void toBytes(PacketBuffer buf) {
        buf.writeEnumValue(action);
    }

    public boolean handle(Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context ctx = supplier.get();

        ctx.enqueueWork(() -> {
            ServerPlayerEntity sender = ctx.getSender();
            if (sender == null) {
                LOGGER.warn("No sender found in Direction: {}", ctx.getDirection());
                return;
            }

            sender.getCapability(CapabilityOrigin.ORIGIN_CAPABILITY).ifPresent(o -> o.onEvent(action));
        });
        return true;
    }

}
