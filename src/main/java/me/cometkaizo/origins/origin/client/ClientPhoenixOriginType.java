package me.cometkaizo.origins.origin.client;

import me.cometkaizo.origins.origin.Origin;
import me.cometkaizo.origins.origin.PhoenixOriginType;
import me.cometkaizo.origins.util.SoundUtils;
import me.cometkaizo.origins.util.TimeTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.player.ClientPlayerEntity;
import net.minecraft.util.MovementInput;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.SoundEvents;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;

@OnlyIn(Dist.CLIENT)
public class ClientPhoenixOriginType {
    public static final double FLAP_AMPLIFIER = 0.7;
    public static final double BOOST_AMPLIFIER = 0.4;
    public static final double BOOST_OLD_MOVEMENT_REDUCTION = 0.4;
    public static final float BOOST_EXHAUSTION = 0.08F;
    public static final float FIRE_TICK_FLY_SPEED_AMP = 0.022F;


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
            boostForward(origin, player, cooldownTracker);
        }
    }

    private static void boostForward(Origin origin, ClientPlayerEntity player, TimeTracker cooldownTracker) {
        Vector3d boostAmount = player.getLookVec();
        float fireSpeedBoost = Math.max(origin.getPlayer().getFireTimer(), 0) * FIRE_TICK_FLY_SPEED_AMP + 1;
        Vector3d boost = boostAmount.scale(BOOST_AMPLIFIER).scale(fireSpeedBoost);
        Vector3d oldMotion = player.getMotion().scale(BOOST_OLD_MOVEMENT_REDUCTION);

        player.setMotion(oldMotion.add(boost));
        SoundUtils.playSound(player, SoundEvents.ENTITY_PHANTOM_FLAP, SoundCategory.PLAYERS, 0.3F, 1);

        cooldownTracker.addTimer(PhoenixOriginType.Cooldown.FORWARD_BOOST);
        player.addExhaustion(BOOST_EXHAUSTION);
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
            float flapAmount = (-player.rotationPitch + 90) / 180;
            double yMotion = FLAP_AMPLIFIER * flapAmount;

            player.setMotion(player.getMotion().add(0, yMotion, 0));
            SoundUtils.playSound(player, SoundEvents.ENTITY_PHANTOM_FLAP, SoundCategory.PLAYERS, 0.4F, 1);

            cooldownTracker.addTimer(PhoenixOriginType.Cooldown.UP_BOOST);
            player.addExhaustion(BOOST_EXHAUSTION);
        }
    }


}
