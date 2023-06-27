package me.cometkaizo.origins.origin.client;

import me.cometkaizo.origins.common.OriginDamageSources;
import me.cometkaizo.origins.network.C2SEnderianAction;
import me.cometkaizo.origins.network.C2SThrowEnderianPearl;
import me.cometkaizo.origins.network.Packets;
import me.cometkaizo.origins.origin.Origin;
import me.cometkaizo.origins.util.SoundUtils;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.player.ClientPlayerEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.SoundEvents;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.BlockRayTraceResult;
import net.minecraft.util.math.RayTraceResult;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;

import java.util.Random;

import static me.cometkaizo.origins.origin.EnderianOriginType.*;

@OnlyIn(Dist.CLIENT)
public class ClientEnderianOriginType {

    private static final Random RANDOM = new Random();
    public static final OriginBarOverlayGui barOverlay = new OriginBarOverlayGui.Builder(OriginBarOverlayGui.Bar.ENDER_PEARL)
            .disappearWhenFull()
            .build();

    public static void onActivate(Origin origin) {
        if (origin.isServerSide()) return;
        barOverlay.start();
    }

    public static void onDeactivate(Origin origin) {
        if (origin.isServerSide()) return;
        barOverlay.stop();
    }

    public static void onEvent(Object event, Origin origin) {
        if (event instanceof TickEvent.ClientTickEvent) {
            onClientTick((TickEvent.ClientTickEvent) event, origin);
        } else if (event instanceof PlayerInteractEvent.RightClickEmpty) {
            onEmptyClick((PlayerInteractEvent.RightClickEmpty) event, origin);
        }
    }

    public static void onClientTick(TickEvent.ClientTickEvent event, Origin origin) {
        if (event.isCanceled()) return;
        if (event.phase == TickEvent.Phase.START) return;

        ClientPlayerEntity player = Minecraft.getInstance().player;
        if (!origin.getPlayer().equals(player)) return;

        updateBarOverlay(player);
        tryApplyPumpkinDamage(origin, player);
    }

    private static void updateBarOverlay(ClientPlayerEntity player) {
        barOverlay.setBarPercent(1 - player.getCooldownTracker().getCooldown(Items.ENDER_PEARL, 0));
    }

    private static void tryApplyPumpkinDamage(Origin origin, ClientPlayerEntity player) {
        BlockRayTraceResult rayTraceResult = (BlockRayTraceResult) player.pick(REACH, 0, false);
        if (rayTraceResult.getType() != RayTraceResult.Type.BLOCK) return;

        BlockPos blockpos = rayTraceResult.getPos();
        BlockState blockstate = player.world.getBlockState(blockpos);
        if (blockstate.getBlock() == Blocks.CARVED_PUMPKIN) {
            pumpkinScare(origin);
            Packets.sendToServer(new C2SEnderianAction(Action.PUMPKIN_SCARE));
        } else if (blockstate.getBlock() == Blocks.JACK_O_LANTERN) {
            jackOLanternScare(origin);
            Packets.sendToServer(new C2SEnderianAction(Action.JACK_O_LANTERN_SCARE));
        }
    }

    private static void jackOLanternScare(Origin origin) {
        boolean damaged = origin.getPlayer().attackEntityFrom(OriginDamageSources.SCARE, JACK_O_LANTERN_SCARE_DAMAGE / 2F);
        if (damaged) SoundUtils.playSound(origin.getPlayer(), SoundEvents.ENTITY_ENDERMAN_HURT, SoundCategory.HOSTILE, 1, 1);
    }

    private static void pumpkinScare(Origin origin) {
        boolean damaged = origin.getPlayer().attackEntityFrom(OriginDamageSources.SCARE, PUMPKIN_SCARE_DAMAGE / 2F);
        if (damaged) SoundUtils.playSound(origin.getPlayer(), SoundEvents.ENTITY_ENDERMAN_HURT, SoundCategory.HOSTILE, 0.7F, 1);
    }

    public static void onEmptyClick(PlayerInteractEvent.RightClickEmpty event, Origin origin) {
        if (event.isCanceled()) return;
        if (origin.isServerSide()) return;
        PlayerEntity player = origin.getPlayer();
        if (!player.equals(event.getPlayer())) return;

        if (event.getItemStack() != ItemStack.EMPTY) return;
        if (hasPearlCooldown(origin)) return;

        SoundUtils.playSound(player, SoundEvents.ENTITY_ENDER_PEARL_THROW, SoundCategory.NEUTRAL, 0.5F, 0.4F / (RANDOM.nextFloat() * 0.4F + 0.8F));
        player.getCooldownTracker().setCooldown(Items.ENDER_PEARL, 20);
        Packets.sendToServer(new C2SThrowEnderianPearl());
    }


}
