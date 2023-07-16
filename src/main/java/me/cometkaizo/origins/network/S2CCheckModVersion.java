package me.cometkaizo.origins.network;

import me.cometkaizo.origins.Main;
import me.cometkaizo.origins.client.ClientUtils;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.network.PacketBuffer;
import net.minecraft.world.World;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.fml.MavenVersionStringHelper;
import net.minecraftforge.fml.ModContainer;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.network.NetworkEvent;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.maven.artifact.versioning.ArtifactVersion;

import java.util.Optional;
import java.util.function.Supplier;

public class S2CCheckModVersion {

    public static final Logger LOGGER = LogManager.getLogger();
    public static final String NO_MOD_FOUND = "", INCORRECT_MOD_VERSION = "multiplayer.disconnect.incorrect_mod_version";
    public final int playerId;
    public final String serverVersion;

    public S2CCheckModVersion(PlayerEntity player, ArtifactVersion serverVersion) {
        this.playerId = player.getEntityId();
        this.serverVersion = MavenVersionStringHelper.artifactVersionToString(serverVersion);
    }

    public S2CCheckModVersion(PacketBuffer buffer) {
        playerId = buffer.readInt();
        serverVersion = buffer.readString();
    }

    public void toBytes(PacketBuffer buffer) {
        buffer.writeInt(playerId);
        buffer.writeString(serverVersion);
    }


    public boolean handle(Supplier<NetworkEvent.Context> supplier) {
        supplier.get().enqueueWork(() -> {
            World world = DistExecutor.safeCallWhenOn(Dist.CLIENT, () -> ClientUtils::getClientLevel);
            if (world == null) {
                Main.LOGGER.error("No world for packet: {}, {}", playerId, serverVersion);
                return;
            }

            Entity entity = world.getEntityByID(playerId);
            if (entity == null) {
                Main.LOGGER.error("Cannot get mod version; no entity with id {}", playerId);
            }
            PlayerEntity localPlayer = DistExecutor.safeCallWhenOn(Dist.CLIENT, () -> ClientUtils::getClientPlayer);
            if (entity != localPlayer) {
                Main.LOGGER.error("Entity with id {} is not the correct local player; expected: {}, found: {}", playerId, localPlayer, entity);
                return;
            }

            Optional<? extends ModContainer> originsModOp = ModList.get().getModContainerById(Main.MOD_ID);
            if (!originsModOp.isPresent()) {
                Packets.sendToServer(new C2SHandleModVersion(NO_MOD_FOUND, "", serverVersion));
                return;
            }
            ModContainer originsMod = originsModOp.get();
            String clientVersion = MavenVersionStringHelper.artifactVersionToString(originsMod.getModInfo().getVersion());
            Main.LOGGER.info("version: {}", clientVersion);
            if (serverVersion == null) {
                Main.LOGGER.error("Invalid Mod Version packet: null server version; player id: {}", playerId);
                return;
            } if (clientVersion == null) {
                Packets.sendToServer(new C2SHandleModVersion(NO_MOD_FOUND, "", serverVersion));
                return;
            }
            if (!serverVersion.equals(clientVersion)) {
                Packets.sendToServer(new C2SHandleModVersion(INCORRECT_MOD_VERSION, clientVersion, serverVersion));
            }
        });
        return true;
    }

}
