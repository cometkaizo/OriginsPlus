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
    @SuppressWarnings("ConstantConditions")
    public static boolean isPhysicalClient(PlayerEntity player) {
        return player != null &&
                Minecraft.getInstance() != null &&
                Minecraft.getInstance().player != null &&
                player.getGameProfile() != null &&
                player == Minecraft.getInstance().player;
    }

    public static void sendRemovalOriginPacket() {
        Packets.sendToServer(new C2SRemoveOrigin());
        Main.LOGGER.info("Sent Origin removal packet");
    }

}
