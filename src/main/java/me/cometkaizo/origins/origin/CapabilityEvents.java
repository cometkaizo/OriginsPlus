package me.cometkaizo.origins.origin;

import me.cometkaizo.origins.Main;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.event.AttachCapabilitiesEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.Objects;

@Mod.EventBusSubscriber(modid = Main.MOD_ID)
public class CapabilityEvents {

    @SubscribeEvent
    public static void onAttachCapabilitiesPlayer(AttachCapabilitiesEvent<Entity> event) {
        if (event.getObject() instanceof PlayerEntity) {
            PlayerEntity player = (PlayerEntity) event.getObject();
            if (!player.getCapability(CapabilityOrigin.ORIGIN_CAPABILITY).isPresent()) {
                OriginCapProvider cap = new OriginCapProvider(player);
                cap.getCapability(CapabilityOrigin.ORIGIN_CAPABILITY).ifPresent(o -> {
                    if (o.getPlayer() == null) {
                        o.setPlayer(player);
                    }
                });
                event.addCapability(new ResourceLocation(Main.MOD_ID, "player_origin"), cap);
            }
        }
    }

    @SubscribeEvent
    public static void onPlayerClone(PlayerEvent.Clone event) {
        event.getOriginal().revive();

        event.getOriginal().getCapability(CapabilityOrigin.ORIGIN_CAPABILITY).ifPresent(oldOrigin ->
                event.getPlayer().getCapability(CapabilityOrigin.ORIGIN_CAPABILITY).ifPresent(newOrigin -> {
                    newOrigin.transferDataFrom(oldOrigin);
                    newOrigin.trySynchronize();
                    newOrigin.setShouldSynchronize();
                    Main.LOGGER.info("Updated new origin to be {}, {}", event.getPlayer(), oldOrigin.getType());
                })
        );

        event.getOriginal().getCapability(CapabilityOrigin.ORIGIN_CAPABILITY).ifPresent(Origin::remove);
        event.getOriginal().remove(false);
    }

    @SubscribeEvent
    public static void onLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getPlayer() instanceof ServerPlayerEntity) {
            synchronizeAndChooseOrigin((ServerPlayerEntity) event.getPlayer());
        }
    }

    @SubscribeEvent
    public static void playerRespawn(PlayerEvent.PlayerRespawnEvent event) {
        if (event.getPlayer() instanceof ServerPlayerEntity) {
            synchronizeAndChooseOrigin((ServerPlayerEntity) event.getPlayer());
        }
    }

    private static void synchronizeOrigin(ServerPlayerEntity player) {
        Objects.requireNonNull(player.getServer()).runAsync(() -> {
            Origin origin = Origin.getOrigin(player);
            if (origin != null) {
                origin.trySynchronize();
                origin.setShouldSynchronize();
            }
        });
    }

    private static void synchronizeAndChooseOrigin(ServerPlayerEntity player) {
        Objects.requireNonNull(player.getServer()).runAsync(() -> {
            Origin origin = Origin.getOrigin(player);
            if (origin != null) {
                origin.trySynchronize();
                origin.setShouldSynchronize();

                if (!origin.hasChosenType()) origin.setShouldOpenOriginScreen();
            }
        });
    }

    @SubscribeEvent
    public static void playerChangedDimension(PlayerEvent.PlayerChangedDimensionEvent event) {
        if (event.getPlayer() instanceof ServerPlayerEntity) {
            synchronizeOrigin((ServerPlayerEntity) event.getPlayer());
        }
    }


    @SubscribeEvent
    public static void onStartTracking(PlayerEvent.StartTracking event) {
        if (event.getTarget() instanceof PlayerEntity &&
                event.getPlayer() instanceof ServerPlayerEntity/* &&
                !event.getPlayer().world.isRemote*/) {
            ServerPlayerEntity player = (ServerPlayerEntity) event.getPlayer();
            PlayerEntity target = (PlayerEntity) event.getTarget();

            Objects.requireNonNull(player.getServer()).runAsync(() -> {
                Origin origin = Origin.getOrigin(target);
                if (origin != null) {
                    origin.trySynchronize();
                    origin.setShouldSynchronize();
                }
            });
        }
    }

}
