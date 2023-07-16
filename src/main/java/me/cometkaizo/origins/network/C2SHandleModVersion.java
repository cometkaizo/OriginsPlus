package me.cometkaizo.origins.network;

import me.cometkaizo.origins.Main;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.network.PacketBuffer;
import net.minecraft.util.text.TranslationTextComponent;
import net.minecraftforge.fml.network.NetworkEvent;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.function.Supplier;

public class C2SHandleModVersion {

    public static final Logger LOGGER = LogManager.getLogger();
    public final String disconnectMessageKey;
    public final String serverVersion, clientVersion;

    public C2SHandleModVersion(String disconnectMessageKey, String serverVersion, String clientVersion) {
        this.disconnectMessageKey = disconnectMessageKey;
        this.serverVersion = serverVersion;
        this.clientVersion = clientVersion;
    }

    public C2SHandleModVersion(PacketBuffer buffer) {
        disconnectMessageKey = buffer.readString();
        serverVersion = buffer.readString();
        clientVersion = buffer.readString();
    }

    public void toBytes(PacketBuffer buffer) {
        buffer.writeString(disconnectMessageKey);
        buffer.writeString(serverVersion);
        buffer.writeString(clientVersion);
    }

    public boolean handle(Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> {
            ServerPlayerEntity sender = context.getSender();
            if (sender == null) {
                Main.LOGGER.error("Invalid Mod Version packet: no sender");
                return;
            }

            if (!disconnectMessageKey.isEmpty()) {
                if (clientVersion.isEmpty()) sender.connection.disconnect(new TranslationTextComponent(disconnectMessageKey));
                else sender.connection.disconnect(new TranslationTextComponent(disconnectMessageKey, clientVersion, serverVersion));
            }
        });
        return true;
    }

}
