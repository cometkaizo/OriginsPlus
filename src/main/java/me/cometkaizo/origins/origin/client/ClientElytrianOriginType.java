package me.cometkaizo.origins.origin.client;

import me.cometkaizo.origins.network.C2SElytrianAction;
import me.cometkaizo.origins.network.Packets;
import me.cometkaizo.origins.origin.Origin;
import me.cometkaizo.origins.util.DataKey;
import me.cometkaizo.origins.util.DataManager;
import me.cometkaizo.origins.util.TimeTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.player.ClientPlayerEntity;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.event.EntityViewRenderEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;

import static me.cometkaizo.origins.origin.ElytrianOriginType.*;

@OnlyIn(Dist.CLIENT)
public class ClientElytrianOriginType {
    protected static final DataKey<Float> PREV_ROLL = DataKey.create(Float.class);
    protected static final DataKey<Float> ROLL_FOLLOW_TARGET = DataKey.create(Float.class);
    protected static final DataKey<Double> PREV_ROLL_PARTIAL_TICKS = DataKey.create(Double.class);
    public static final float ROLL_AMP = 1;
    public static final double ROLL_RESPONSIVENESS = 0.2;
    public static final float ROLL_FOLLOW_TARGET_REDUCTION = 0.7F;
    public static final OriginBarOverlayGui barOverlay = new OriginBarOverlayGui.Builder(OriginBarOverlayGui.Bar.WINGS)
            .disappearWhenFull()
            .build();



    public static void onFirstActivate(Origin origin) {
        if (origin.isServerSide()) return;
        origin.getTypeData().register(PREV_ROLL, 0F);
        origin.getTypeData().register(ROLL_FOLLOW_TARGET, 0F);
        origin.getTypeData().register(PREV_ROLL_PARTIAL_TICKS, 0D);
    }

    public static void onActivate(Origin origin) {
        barOverlay.start();
    }

    public static void onDeactivate(Origin origin) {
        barOverlay.stop();
    }


    public static void onEvent(Object event, Origin origin) {
        if (event instanceof EntityViewRenderEvent.CameraSetup) {
            onCameraSetup((EntityViewRenderEvent.CameraSetup) event, origin);
        } else if (event instanceof TickEvent.ClientTickEvent) {
            TickEvent.ClientTickEvent tickEvent = (TickEvent.ClientTickEvent) event;
            onClientTick(tickEvent, origin);
            updateRollTarget(tickEvent, origin);
        } else if (event instanceof PlayerInteractEvent.RightClickEmpty) {
            onEmptyClick((PlayerInteractEvent.RightClickEmpty) event, origin);
        }
    }


    public static void onClientTick(TickEvent.ClientTickEvent event, Origin origin) {
        if (event.isCanceled()) return;
        if (event.phase == TickEvent.Phase.START) return;
        if (origin.isServerSide()) return;

        ClientPlayerEntity player = Minecraft.getInstance().player;
        if (!origin.getPlayer().equals(player)) return;
        TimeTracker timeTracker = origin.getTimeTracker();

        updateBarOverlay(timeTracker);

        if (!player.isElytraFlying()) return;

        if (player.movementInput.jump && canNormalBoost(timeTracker)) {
            boostUp(origin);
            Packets.sendToServer(C2SElytrianAction.upBoost());
        }
    }

    private static void updateBarOverlay(TimeTracker timeTracker) {
        barOverlay.setBarPercent(timeTracker.getTimerPercentage(Cooldown.SUPER_BOOST));
    }

    public static void onEmptyClick(PlayerInteractEvent.RightClickEmpty event, Origin origin) {
        if (event.isCanceled()) return;
        if (origin.isServerSide()) return;
        if (!origin.getPlayer().equals(event.getPlayer())) return;

        ClientPlayerEntity player = Minecraft.getInstance().player;
        if (player == null) return;
        TimeTracker timeTracker = origin.getTimeTracker();

        if (!player.isElytraFlying()) return;

        if (canNormalBoost(timeTracker)) {
            boostForward(origin);
            Packets.sendToServer(C2SElytrianAction.forwardBoost());
        }
    }

    public static void updateRollTarget(TickEvent.ClientTickEvent event, Origin origin) {
        if (event.isCanceled()) return;
        if (event.phase == TickEvent.Phase.START) return;
        if (origin.isServerSide()) return;

        ClientPlayerEntity player = Minecraft.getInstance().player;
        if (!origin.getPlayer().equals(player)) return;

        DataManager dataManager = origin.getTypeData();

        float yawDiff = player.rotationYawHead - player.prevRotationYawHead;
        float followTarget = dataManager.get(ROLL_FOLLOW_TARGET);
        if (player.isElytraFlying())
            dataManager.set(ROLL_FOLLOW_TARGET, (followTarget + yawDiff) * ROLL_FOLLOW_TARGET_REDUCTION);
        else
            dataManager.set(ROLL_FOLLOW_TARGET, followTarget * ROLL_FOLLOW_TARGET_REDUCTION);

    }

    public static void onCameraSetup(EntityViewRenderEvent.CameraSetup event, Origin origin) {
        if (event.isCanceled()) return;
        if (origin.isServerSide()) return;
        ClientPlayerEntity player = Minecraft.getInstance().player;
        if (!origin.getPlayer().equals(player)) return;
        DataManager dataManager = origin.getTypeData();

        double partialTicks = event.getRenderPartialTicks();
        Double prevPartialTicks = dataManager.get(PREV_ROLL_PARTIAL_TICKS);
        float prevRoll = dataManager.get(PREV_ROLL);
        float followTarget = dataManager.get(ROLL_FOLLOW_TARGET);

        double followAmt = (followTarget - prevRoll) * ROLL_RESPONSIVENESS * prevPartialTicks;

        double newRoll = (prevRoll + followAmt) * ROLL_AMP;
        event.setRoll((float) newRoll);

        if (prevPartialTicks > partialTicks) {
            dataManager.set(PREV_ROLL, event.getRoll());
        }
        dataManager.set(PREV_ROLL_PARTIAL_TICKS, partialTicks);
    }

}
