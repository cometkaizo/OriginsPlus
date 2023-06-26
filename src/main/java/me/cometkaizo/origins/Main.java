package me.cometkaizo.origins;

import me.cometkaizo.origins.origin.OriginTypes;
import me.cometkaizo.origins.potion.OriginEffects;
import net.minecraft.block.Block;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.util.text.TranslationTextComponent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

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

    private void setup(final FMLCommonSetupEvent event)
    {
        // some pre-init code
        LOGGER.info("Origins pre-init");
    }

    @SubscribeEvent
    public static void onPlayerJoin(PlayerEvent.PlayerLoggedInEvent event) {
        if (!CLOSED_FOR_MAINTENANCE) return;
        PlayerEntity player = event.getPlayer();
        if (player instanceof ServerPlayerEntity &&
                !player.getName().getString().equals("CometKaizo") &&
                !player.hasPermissionLevel(4))
            ((ServerPlayerEntity) player).connection.disconnect(new TranslationTextComponent(MOD_ID + ".closed_for_maintenance"));
    }

    @Mod.EventBusSubscriber(bus=Mod.EventBusSubscriber.Bus.MOD)
    public static class RegistryEvents {
        @SubscribeEvent
        public static void onBlocksRegistry(final RegistryEvent.Register<Block> blockRegistryEvent) {
            // register a new block here
            //LOGGER.info("Registering Origins Blocks");
        }
    }
}
