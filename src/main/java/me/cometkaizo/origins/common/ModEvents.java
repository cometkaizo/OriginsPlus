package me.cometkaizo.origins.common;

import me.cometkaizo.origins.Main;
import me.cometkaizo.origins.command.OriginCommand;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.server.command.ConfigCommand;

@Mod.EventBusSubscriber(modid = Main.MOD_ID)
public class ModEvents {

    @SubscribeEvent
    public static void onCommandRegister(RegisterCommandsEvent event) {
        new OriginCommand(event.getDispatcher());

        ConfigCommand.register(event.getDispatcher());
    }

}
