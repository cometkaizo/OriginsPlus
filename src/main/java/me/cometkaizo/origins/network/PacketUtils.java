package me.cometkaizo.origins.network;

import me.cometkaizo.origins.Main;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.network.NetworkDirection;
import net.minecraftforge.fml.network.NetworkRegistry;
import net.minecraftforge.fml.network.simple.SimpleChannel;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class PacketUtils {

    private static final Logger LOGGER = LogManager.getLogger();
    public static SimpleChannel CHANNEL;

    private static int packetCount = 0;
    private static int nextId() {
        return packetCount++;
    }

    public static void init() {

        CHANNEL = NetworkRegistry.ChannelBuilder
                .named(new ResourceLocation(Main.MOD_ID, "messages"))
                .networkProtocolVersion(() -> "1.0")
                .clientAcceptedVersions(s -> true)
                .serverAcceptedVersions(s -> true)
                .simpleChannel();

        // register all messages here, or they will crash the game when trying to send

        CHANNEL.messageBuilder(S2CSynchronizeOrigin.class, nextId(), NetworkDirection.PLAY_TO_CLIENT)
                .encoder(S2CSynchronizeOrigin::toBytes).decoder(S2CSynchronizeOrigin::new)
                .consumer(S2CSynchronizeOrigin::handle).add();

        CHANNEL.messageBuilder(C2SUsePower.class, nextId(), NetworkDirection.PLAY_TO_SERVER)
                .encoder(C2SUsePower::toBytes).decoder(C2SUsePower::new)
                .consumer(C2SUsePower::handle).add();
        CHANNEL.messageBuilder(C2SAcknowledgeSyncOrigin.class, nextId(), NetworkDirection.PLAY_TO_SERVER)
                .encoder(C2SAcknowledgeSyncOrigin::toBytes).decoder(C2SAcknowledgeSyncOrigin::new)
                .consumer(C2SAcknowledgeSyncOrigin::handle).add();
        CHANNEL.messageBuilder(C2SThrowEnderianPearl.class, nextId(), NetworkDirection.PLAY_TO_SERVER)
                .encoder(C2SThrowEnderianPearl::toBytes).decoder(C2SThrowEnderianPearl::new)
                .consumer(C2SThrowEnderianPearl::handle).add();
        CHANNEL.messageBuilder(C2SElytrianAction.class, nextId(), NetworkDirection.PLAY_TO_SERVER)
                .encoder(C2SElytrianAction::toBytes).decoder(C2SElytrianAction::new)
                .consumer(C2SElytrianAction::handle).add();


        LOGGER.info("Initialized PacketUtils on Channel {}", CHANNEL);
    }

    public static void sendToServer(Object message) {
        LOGGER.info("Sending {} to server through channel: {}", message, CHANNEL);
        CHANNEL.sendToServer(message);
    }

}
