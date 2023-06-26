package me.cometkaizo.origins.origin.client;

import me.cometkaizo.origins.Main;
import me.cometkaizo.origins.origin.Origin;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.player.ClientPlayerEntity;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(value = Dist.CLIENT, modid = Main.MOD_ID)
public class ClientOriginEventHandler {

    @SubscribeEvent
    public static void onRenderTick(TickEvent.RenderTickEvent event) {
        if (event.phase == TickEvent.Phase.START) {
            Minecraft mc = Minecraft.getInstance();
            ClientPlayerEntity player = mc.player;
            Origin origin = Origin.getOrigin(player);
            if (origin != null && mc.currentScreen == null && origin.shouldOpenOriginScreen()) {
                mc.displayGuiScreen(new ChooseOriginScreen());
                origin.onOpenedOriginScreen();
            }
        }
    }

}
