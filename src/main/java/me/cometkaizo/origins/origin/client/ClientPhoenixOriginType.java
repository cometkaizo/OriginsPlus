package me.cometkaizo.origins.origin.client;

import me.cometkaizo.origins.network.C2SElytrianAction;
import me.cometkaizo.origins.network.PacketUtils;
import me.cometkaizo.origins.origin.Origin;
import me.cometkaizo.origins.origin.PhoenixOriginType;
import me.cometkaizo.origins.util.TimeTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.player.ClientPlayerEntity;
import net.minecraft.util.MovementInput;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;

import static me.cometkaizo.origins.origin.PhoenixOriginType.boostForward;
import static me.cometkaizo.origins.origin.PhoenixOriginType.boostUp;

@OnlyIn(Dist.CLIENT)
public class ClientPhoenixOriginType {


    public static void onPlayerSensitiveEvent(Object event, Origin origin) {
        if (event instanceof TickEvent.ClientTickEvent) {
            onClientTick((TickEvent.ClientTickEvent) event, origin);
        } else if (event instanceof PlayerInteractEvent.RightClickEmpty) {
            onEmptyClick((PlayerInteractEvent.RightClickEmpty) event, origin);
        }
    }


    public static void onEmptyClick(PlayerInteractEvent.RightClickEmpty event, Origin origin) {
        if (event.isCanceled()) return;

        ClientPlayerEntity player = Minecraft.getInstance().player;
        if (player == null) return;
        TimeTracker cooldownTracker = origin.getTimeTracker();

        if (!player.isElytraFlying()) return;

        if (!cooldownTracker.hasTimer(PhoenixOriginType.Cooldown.UP_BOOST) && !cooldownTracker.hasTimer(PhoenixOriginType.Cooldown.FORWARD_BOOST)) {
            boostForward(origin);
            PacketUtils.sendToServer(C2SElytrianAction.forwardBoost());
        }
    }

    public static void onClientTick(TickEvent.ClientTickEvent event, Origin origin) {
        if (event.phase == TickEvent.Phase.START) return;
        if (origin.isServerSide()) return;

        ClientPlayerEntity player = Minecraft.getInstance().player;
        if (player == null) return;
        if (!player.isElytraFlying()) return;

        MovementInput input = player.movementInput;
        TimeTracker cooldownTracker = origin.getTimeTracker();

        if (input.jump && !cooldownTracker.hasTimer(PhoenixOriginType.Cooldown.UP_BOOST) && !cooldownTracker.hasTimer(PhoenixOriginType.Cooldown.FORWARD_BOOST)) {
            boostUp(origin);
            PacketUtils.sendToServer(C2SElytrianAction.upBoost());
        }
    }


}
