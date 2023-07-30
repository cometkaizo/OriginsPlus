package me.cometkaizo.origins.client;

import me.cometkaizo.origins.Main;
import me.cometkaizo.origins.network.C2SUsePower;
import me.cometkaizo.origins.network.Packets;
import me.cometkaizo.origins.origin.Origin;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.player.ClientPlayerEntity;
import net.minecraft.client.gui.screen.ChatScreen;
import net.minecraft.client.gui.screen.EditSignScreen;
import net.minecraft.client.gui.screen.inventory.ContainerScreen;
import net.minecraft.client.settings.KeyBinding;
import net.minecraft.client.util.InputMappings;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.client.settings.KeyConflictContext;
import net.minecraftforge.fml.client.registry.ClientRegistry;
import org.lwjgl.glfw.GLFW;

@OnlyIn(Dist.CLIENT)
public final class KeyBindings {

    public static final String ORIGINS_CATEGORY = "key.categories.origins";

    public static final KeyBinding POWER = new KeyBinding("key.power", KeyConflictContext.UNIVERSAL, InputMappings.getInputByName("key.keyboard.y"), ORIGINS_CATEGORY);

    public static void init() {
        ClientRegistry.registerKeyBinding(POWER);
    }

    public static void onKeyPressed(InputEvent.KeyInputEvent event) {
        int keyCode = event.getKey();
        int action = event.getAction();
        if (canPerformAction() && action == GLFW.GLFW_PRESS && keyCode == POWER.getKey().getKeyCode()) {
            performAction();
        }
    }

    private static void performAction() {
        ClientPlayerEntity player = Minecraft.getInstance().player;
        if (player == null) {
            Main.LOGGER.warn("KeyInputEvent received when Minecraft#player is null");
            return;
        }

        Packets.sendToServer(new C2SUsePower());

        Origin origin = Origin.getOrigin(player);
        if (origin != null) origin.performAction();
    }

    private static boolean canPerformAction() {
        Minecraft minecraft = Minecraft.getInstance();

        if (minecraft.player == null) return false;

        boolean chatOpen = minecraft.currentScreen instanceof ChatScreen;
        boolean containerOpen = minecraft.currentScreen instanceof ContainerScreen<?>;
        boolean signOpen = minecraft.currentScreen instanceof EditSignScreen;

        return !chatOpen && !containerOpen && !signOpen;
    }

    private KeyBindings() {
        throw new AssertionError("No KeyBindings instances for you!");
    }
}
