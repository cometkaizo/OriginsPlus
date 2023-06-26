package me.cometkaizo.origins.origin.client;

import me.cometkaizo.origins.Main;
import me.cometkaizo.origins.network.C2SRemoveOrigin;
import me.cometkaizo.origins.network.Packets;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class ClientOrigin {
    public static boolean isPhysicalClient(PlayerEntity player) {
        return player != null &&
                Minecraft.getInstance().player != null &&
                player.getGameProfile().equals(Minecraft.getInstance().player.getGameProfile());
    }

    public static void sendRemovalOriginPacket() {
        Packets.sendToServer(new C2SRemoveOrigin());
        Main.LOGGER.info("Removal packet sent");
    }

}
