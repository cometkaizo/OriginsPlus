package me.cometkaizo.origins.network;

import me.cometkaizo.origins.origin.Origin;
import me.cometkaizo.origins.origin.OriginType;
import me.cometkaizo.origins.origin.OriginTypes;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.network.PacketBuffer;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.network.NetworkEvent;
import net.minecraftforge.fml.network.PacketDistributor;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.function.Supplier;

public class C2SChooseOrigin {

    public static final Logger LOGGER = LogManager.getLogger();
    private final ResourceLocation typeNamespace;

    public C2SChooseOrigin(OriginType type) {
        this.typeNamespace = type.getRegistryName();
    }

    public C2SChooseOrigin(PacketBuffer buffer) {
        this.typeNamespace = buffer.readResourceLocation();
    }

    public void toBytes(PacketBuffer buffer) {
        buffer.writeResourceLocation(typeNamespace);
    }

    public boolean handle(Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context ctx = supplier.get();

        ctx.enqueueWork(() -> {
            ServerPlayerEntity player = ctx.getSender();
            if (player == null) {
                LOGGER.warn("No sender found in Direction: {}", ctx.getDirection());
                return;
            }

            Origin origin = Origin.getOrigin(player);
            if (origin != null) {
                OriginType type = OriginTypes.of(typeNamespace);
                origin.setType(type);
                origin.setShouldSynchronize();

                origin.setHasChosenType(true);
                Packets.CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), new S2CAcknowledgeChooseOrigin(player));
            } else {
                LOGGER.warn("No origin found on player {}", player);
            }

        });
        return true;
    }

}
