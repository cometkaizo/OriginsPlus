package me.cometkaizo.origins.network;

import me.cometkaizo.origins.client.ClientUtils;
import me.cometkaizo.origins.origin.client.ChooseOriginScreen;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.network.PacketBuffer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.fml.network.NetworkEvent;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.function.Supplier;

public class S2CAcknowledgeChooseOrigin {

    public static final Logger LOGGER = LogManager.getLogger();
    public final int playerId;

    public S2CAcknowledgeChooseOrigin(PlayerEntity player) {
        this.playerId = player.getEntityId();
    }

    public S2CAcknowledgeChooseOrigin(PacketBuffer buffer) {
        playerId = buffer.readInt();
    }

    public void toBytes(PacketBuffer buffer) {
        buffer.writeInt(playerId);
    }


    public boolean handle(Supplier<NetworkEvent.Context> supplier) {
        supplier.get().enqueueWork(() -> {
            Screen screen = DistExecutor.safeCallWhenOn(Dist.CLIENT, () -> ClientUtils::getClientScreen);
            if (screen == null) return;
            if (!(screen instanceof ChooseOriginScreen)) return;

            ChooseOriginScreen originScreen = (ChooseOriginScreen) screen;
            originScreen.setServerSynced();
        });
        return true;
    }

}
