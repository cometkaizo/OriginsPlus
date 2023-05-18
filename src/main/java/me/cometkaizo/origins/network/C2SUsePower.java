package me.cometkaizo.origins.network;

import me.cometkaizo.origins.origin.CapabilityOrigin;
import me.cometkaizo.origins.origin.Origin;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screen.ChatScreen;
import net.minecraft.client.gui.screen.inventory.ContainerScreen;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.network.PacketBuffer;
import net.minecraftforge.fml.network.NetworkEvent;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.function.Supplier;

public class C2SUsePower {

    public static final Logger LOGGER = LogManager.getLogger();

    public C2SUsePower() {

    }

    public C2SUsePower(PacketBuffer packetBuffer) {

    }

    public void toBytes(PacketBuffer buf) {

    }

    public boolean handle(Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context ctx = supplier.get();

        ctx.enqueueWork(() -> {
            ServerPlayerEntity sender = ctx.getSender();
            if (sender == null) {
                LOGGER.warn("No sender found in Direction: {}", ctx.getDirection());
                return;
            }

            Minecraft minecraft = Minecraft.getInstance();

            if (minecraft.player == null) return;
            if (!sender.getGameProfile().equals(minecraft.player.getGameProfile())) return;

            boolean chatOpen = minecraft.currentScreen instanceof ChatScreen;
            boolean containerOpen = minecraft.currentScreen instanceof ContainerScreen<?>;

            if (chatOpen || containerOpen) return;

            sender.getCapability(CapabilityOrigin.ORIGIN_CAPABILITY).ifPresent(Origin::performAction);
        });
        return true;
    }

}
