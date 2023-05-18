package me.cometkaizo.origins.client;

import me.cometkaizo.origins.network.PacketUtils;
import me.cometkaizo.origins.network.C2SUsePower;
import net.minecraft.client.settings.KeyBinding;
import net.minecraft.client.util.InputMappings;
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.client.settings.KeyConflictContext;
import net.minecraftforge.fml.client.registry.ClientRegistry;
import org.lwjgl.glfw.GLFW;

public final class KeyBindings {

    public static final String ORIGINS_CATEGORY = "key.categories.origins";

    public static final KeyBinding POWER = new KeyBinding("key.power", KeyConflictContext.UNIVERSAL, InputMappings.getInputByName("key.keyboard.y"), ORIGINS_CATEGORY);

    public static void init() {
        ClientRegistry.registerKeyBinding(POWER);
    }

    public static void onKeyPressed(InputEvent.KeyInputEvent event) {
        int keyCode = event.getKey();
        int action = event.getAction();
        if (action == GLFW.GLFW_PRESS && keyCode == POWER.getKey().getKeyCode()) {
            PacketUtils.sendToServer(new C2SUsePower());
        }
    }

    private KeyBindings() {
        throw new AssertionError("No KeyBindings instances for you!");
    }
}
