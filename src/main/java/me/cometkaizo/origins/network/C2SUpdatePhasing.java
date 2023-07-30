package me.cometkaizo.origins.network;

import me.cometkaizo.origins.origin.Origin;
import me.cometkaizo.origins.origin.PhantomOriginType;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.network.PacketBuffer;
import net.minecraftforge.fml.network.NetworkEvent;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.function.Supplier;

public class C2SUpdatePhasing {

    public static final Logger LOGGER = LogManager.getLogger();
    private final boolean phasing;

    public C2SUpdatePhasing(boolean phasing) {
        this.phasing = phasing;
    }

    public C2SUpdatePhasing(PacketBuffer packetBuffer) {
        this.phasing = packetBuffer.readBoolean();
    }

    public void toBytes(PacketBuffer buf) {
        buf.writeBoolean(phasing);
    }

    public boolean handle(Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context ctx = supplier.get();

        ctx.enqueueWork(() -> {
            ServerPlayerEntity sender = ctx.getSender();
            if (sender == null) {
                LOGGER.warn("No sender found in Direction: {}", ctx.getDirection());
                return;
            }

            sender.setNoGravity(phasing);
            sender.noClip = phasing;
            if (phasing) sender.fallDistance = 0;
            Origin origin = Origin.getOrigin(sender);
            if (origin != null) origin.getTypeData().set(PhantomOriginType.IS_PHASING, phasing);
        });
        return true;
    }

}
