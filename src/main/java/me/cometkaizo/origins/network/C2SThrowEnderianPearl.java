package me.cometkaizo.origins.network;

import net.minecraft.entity.item.EnderPearlEntity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.item.Items;
import net.minecraft.network.PacketBuffer;
import net.minecraft.stats.Stats;
import net.minecraft.world.server.ServerWorld;
import net.minecraftforge.fml.network.NetworkEvent;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.function.Supplier;

public class C2SThrowEnderianPearl {

    public static final Logger LOGGER = LogManager.getLogger();

    public C2SThrowEnderianPearl() {

    }

    public C2SThrowEnderianPearl(PacketBuffer packetBuffer) {

    }

    public void toBytes(PacketBuffer buf) {

    }

    public boolean handle(Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context ctx = supplier.get();

        ctx.enqueueWork(() -> {
            ServerPlayerEntity sender = ctx.getSender();
            if (sender == null) {
                LOGGER.warn("No sender found in Direction: {}", ctx.getDirection());
                return;
            }

            ServerWorld world = sender.getServerWorld();
            sender.getCooldownTracker().setCooldown(Items.ENDER_PEARL, 20);

            EnderPearlEntity pearl = new EnderPearlEntity(world, sender);
            pearl.setItem(Items.ENDER_PEARL.getDefaultInstance());
            pearl.setDirectionAndMovement(sender, sender.rotationPitch, sender.rotationYaw, 0.0F, 1.5F, 1.0F);
            world.addEntity(pearl);

            sender.addStat(Stats.ITEM_USED.get(Items.ENDER_PEARL));
        });
        return true;
    }

}
