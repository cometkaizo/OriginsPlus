package me.cometkaizo.origins.network;

import me.cometkaizo.origins.Main;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.network.NetworkDirection;
import net.minecraftforge.fml.network.NetworkRegistry;
import net.minecraftforge.fml.network.simple.SimpleChannel;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class Packets {

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
        CHANNEL.messageBuilder(S2CSlimicianAction.class, nextId(), NetworkDirection.PLAY_TO_CLIENT)
                .encoder(S2CSlimicianAction::toBytes).decoder(S2CSlimicianAction::new)
                .consumer(S2CSlimicianAction::handle).add();
        CHANNEL.messageBuilder(S2CSlimicianSynchronize.class, nextId(), NetworkDirection.PLAY_TO_CLIENT)
                .encoder(S2CSlimicianSynchronize::toBytes).decoder(S2CSlimicianSynchronize::new)
                .consumer(S2CSlimicianSynchronize::handle).add();
        CHANNEL.messageBuilder(S2COpenOriginScreen.class, nextId(), NetworkDirection.PLAY_TO_CLIENT)
                .encoder(S2COpenOriginScreen::toBytes).decoder(S2COpenOriginScreen::new)
                .consumer(S2COpenOriginScreen::handle).add();
        CHANNEL.messageBuilder(S2CAcknowledgeChooseOrigin.class, nextId(), NetworkDirection.PLAY_TO_CLIENT)
                .encoder(S2CAcknowledgeChooseOrigin::toBytes).decoder(S2CAcknowledgeChooseOrigin::new)
                .consumer(S2CAcknowledgeChooseOrigin::handle).add();
        CHANNEL.messageBuilder(S2CCheckModVersion.class, nextId(), NetworkDirection.PLAY_TO_CLIENT)
                .encoder(S2CCheckModVersion::toBytes).decoder(S2CCheckModVersion::new)
                .consumer(S2CCheckModVersion::handle).add();

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
        CHANNEL.messageBuilder(C2SEnderianAction.class, nextId(), NetworkDirection.PLAY_TO_SERVER)
                .encoder(C2SEnderianAction::toBytes).decoder(C2SEnderianAction::new)
                .consumer(C2SEnderianAction::handle).add();
        CHANNEL.messageBuilder(C2SArachnidAction.class, nextId(), NetworkDirection.PLAY_TO_SERVER)
                .encoder(C2SArachnidAction::toBytes).decoder(C2SArachnidAction::new)
                .consumer(C2SArachnidAction::handle).add();
        CHANNEL.messageBuilder(C2SSlimicianAction.class, nextId(), NetworkDirection.PLAY_TO_SERVER)
                .encoder(C2SSlimicianAction::toBytes).decoder(C2SSlimicianAction::new)
                .consumer(C2SSlimicianAction::handle).add();
        CHANNEL.messageBuilder(C2SSetMotion.class, nextId(), NetworkDirection.PLAY_TO_SERVER)
                .encoder(C2SSetMotion::toBytes).decoder(C2SSetMotion::new)
                .consumer(C2SSetMotion::handle).add();
        CHANNEL.messageBuilder(C2SChooseOrigin.class, nextId(), NetworkDirection.PLAY_TO_SERVER)
                .encoder(C2SChooseOrigin::toBytes).decoder(C2SChooseOrigin::new)
                .consumer(C2SChooseOrigin::handle).add();
        CHANNEL.messageBuilder(C2SRemoveOrigin.class, nextId(), NetworkDirection.PLAY_TO_SERVER)
                .encoder(C2SRemoveOrigin::toBytes).decoder(C2SRemoveOrigin::new)
                .consumer(C2SRemoveOrigin::handle).add();
        CHANNEL.messageBuilder(C2SHandleModVersion.class, nextId(), NetworkDirection.PLAY_TO_SERVER)
                .encoder(C2SHandleModVersion::toBytes).decoder(C2SHandleModVersion::new)
                .consumer(C2SHandleModVersion::handle).add();


        LOGGER.info("Initialized Packets on Channel {}", CHANNEL);
    }

    public static void sendToServer(Object message) {
        CHANNEL.sendToServer(message);
    }

}
