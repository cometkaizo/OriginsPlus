package me.cometkaizo.origins;

import me.cometkaizo.origins.network.Packets;
import me.cometkaizo.origins.network.S2CCheckModVersion;
import me.cometkaizo.origins.origin.OriginTypes;
import me.cometkaizo.origins.potion.OriginEffects;
import net.minecraft.block.Block;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ModContainer;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.fml.loading.moddiscovery.ModInfo;
import net.minecraftforge.fml.network.PacketDistributor;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.maven.artifact.versioning.ArtifactVersion;

import java.util.Collection;
import java.util.Optional;

// The value here should match an entry in the META-INF/mods.toml file
@Mod(Main.MOD_ID)
public class Main {

    public static final String MOD_ID = "origins";
    public static final String NAME = "Origins";
    public static final Logger LOGGER = LogManager.getLogger(NAME);
    public static final boolean CLOSED_FOR_MAINTENANCE = false;

    public Main() {
        IEventBus modEventBus = FMLJavaModLoadingContext.get().getModEventBus();
        // Register the setup method for modloading
        modEventBus.addListener(this::setup);
        OriginTypes.register(modEventBus);
        OriginEffects.register(modEventBus);

        // Register ourselves for server and other game events we are interested in
        IEventBus forgeEventBus = MinecraftForge.EVENT_BUS;
        forgeEventBus.register(this);
    }

    public static ModInfo getOriginsMod(Collection<ModInfo> mods) {
        return mods.stream().filter(m -> Main.MOD_ID.equals(m.getModId())).findAny().orElse(null);
    }

    private void setup(final FMLCommonSetupEvent event)
    {
        // some pre-init code
        LOGGER.info("Origins pre-init");
    }

    @SubscribeEvent
    public void onPlayerJoin(PlayerEvent.PlayerLoggedInEvent event) {
        Optional<? extends ModContainer> modContainer = ModList.get().getModContainerById(MOD_ID);
        if (!modContainer.isPresent()) return;
        ArtifactVersion version = modContainer.get().getModInfo().getVersion();
        Packets.CHANNEL.send(PacketDistributor.TRACKING_ENTITY_AND_SELF.with(event::getPlayer), new S2CCheckModVersion(event.getPlayer(), version));

        /*
        if (!CLOSED_FOR_MAINTENANCE) return;
        PlayerEntity player = event.getPlayer();
        if (player instanceof ServerPlayerEntity &&
                !player.getName().getString().equals("CometKaizo") &&
                !player.hasPermissionLevel(4))
            ((ServerPlayerEntity) player).connection.disconnect(new TranslationTextComponent("multiplayer.disconnect.closed_for_maintenance"));
*/    }

    @Mod.EventBusSubscriber(bus=Mod.EventBusSubscriber.Bus.MOD)
    public static class RegistryEvents {
        @SubscribeEvent
        public static void onBlocksRegistry(final RegistryEvent.Register<Block> blockRegistryEvent) {
            // register a new block here
            //LOGGER.info("Registering Origins Blocks");
        }
    }
}
