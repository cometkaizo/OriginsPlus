package me.cometkaizo.origins.origin;

import me.cometkaizo.origins.origin.client.ClientSlimicianOriginType;
import me.cometkaizo.origins.property.SpeciesProperty;
import me.cometkaizo.origins.util.TimeTracker;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.entity.living.LivingFallEvent;
import net.minecraftforge.eventbus.api.Event;

import static net.minecraftforge.fml.DistExecutor.unsafeRunWhenOn;

public class SlimicianOriginType extends AbstractOriginType {

    public static final float SLIME_AGGRO_RANGE = 10;

    public static final SpeciesProperty SLIME_SPECIES = new SpeciesProperty.Builder()
            .setMobSpecies(EntityType.SLIME)
            .setRallyRadius(SLIME_AGGRO_RANGE).build();
    public static final float BOUNCE_SUPPRESS_FALL_DAMAGE_AMP = 0.5F;


    public enum Timer implements TimeTracker.Timer {
        JUMPED(0.3 * 20),
        BOUNCED(0.3 * 20);
        private final int duration;
        Timer(double duration) {
            this.duration = (int) duration;
        }
        @Override
        public int getDuration() {
            return duration;
        }
    }

    public SlimicianOriginType() {
        super("Slimician",
                SLIME_SPECIES
        );
    }

    @Override
    public void onFirstActivate(Origin origin) {
        super.onFirstActivate(origin);

        unsafeRunWhenOn(Dist.CLIENT, () -> () -> ClientSlimicianOriginType.onFirstActivate(origin));
    }

    @Override
    public void onPlayerSensitiveEvent(Object event, Origin origin) {
        super.onPlayerSensitiveEvent(event, origin);
        if (isCanceled(event)) return;
        if (event instanceof LivingFallEvent) {
            onPlayerFall((LivingFallEvent) event, origin);
        }

        unsafeRunWhenOn(Dist.CLIENT, () -> () -> ClientSlimicianOriginType.onPlayerSensitiveEvent(event, origin));
    }

    private static boolean isCanceled(Object event) {
        return event instanceof Event && ((Event)event).isCanceled();
    }

    private void onPlayerFall(LivingFallEvent event, Origin origin) {
        if (origin.isServerSide()) return;

        PlayerEntity player = origin.getPlayer();
        if (player.isSuppressingBounce()) {
            event.setDamageMultiplier(event.getDamageMultiplier() * BOUNCE_SUPPRESS_FALL_DAMAGE_AMP);
        } else {
            event.setCanceled(true);
        }
    }
}
