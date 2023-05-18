package me.cometkaizo.origins.common;

import me.cometkaizo.origins.Main;
import me.cometkaizo.origins.network.PacketUtils;
import me.cometkaizo.origins.origin.CapabilityOrigin;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import org.apache.logging.log4j.LogManager;

@Mod.EventBusSubscriber(modid = Main.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD)
public class ForgeEvents {

    @SubscribeEvent
    public static void onCommonSetup(FMLCommonSetupEvent event) {
        LogManager.getLogger().info("FMLCommonSetupEvent was fired");

        PacketUtils.init();
        CapabilityOrigin.register();
    }

    @SubscribeEvent
    public static void onRegistryRegister(RegistryEvent.NewRegistry event) {
        LogManager.getLogger().info("RegistryEvent.NewRegistry was posted");
    }

}
