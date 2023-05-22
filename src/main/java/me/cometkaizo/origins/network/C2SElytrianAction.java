package me.cometkaizo.origins.network;

import me.cometkaizo.origins.origin.CapabilityOrigin;
import me.cometkaizo.origins.origin.ElytrianOriginType;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.network.PacketBuffer;
import net.minecraftforge.fml.network.NetworkEvent;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.function.Supplier;

public class C2SElytrianAction {

    public static final Logger LOGGER = LogManager.getLogger();
    private final ElytrianOriginType.Action action;

    public C2SElytrianAction(ElytrianOriginType.Action action) {
        this.action = action;
    }

    public C2SElytrianAction(PacketBuffer packetBuffer) {
        this.action = packetBuffer.readEnumValue(ElytrianOriginType.Action.class);
    }

    public static C2SElytrianAction upBoost() {
        return new C2SElytrianAction(ElytrianOriginType.Action.UP_BOOST);
    }
    public static C2SElytrianAction forwardBoost() {
        return new C2SElytrianAction(ElytrianOriginType.Action.FORWARD_BOOST);
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
