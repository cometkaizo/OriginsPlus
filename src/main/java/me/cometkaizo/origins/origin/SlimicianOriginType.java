package me.cometkaizo.origins.origin;

import me.cometkaizo.origins.Main;
import me.cometkaizo.origins.origin.client.ClientSlimicianOriginType;
import me.cometkaizo.origins.property.SpeciesProperty;
import me.cometkaizo.origins.util.DataKey;
import me.cometkaizo.origins.util.DataManager;
import me.cometkaizo.origins.util.TimeTracker;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.entity.EntityEvent;
import net.minecraftforge.event.entity.living.LivingFallEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.eventbus.api.Event;

import static net.minecraftforge.fml.DistExecutor.unsafeRunWhenOn;

public class SlimicianOriginType extends AbstractOriginType {

    public static final float SLIME_AGGRO_RANGE = 10;
    public static final DataKey<Integer> SHRINK_COUNT = DataKey.create(Integer.class);
    public static final int MAX_SHRINK_COUNT = 3;
    private static final double SIZE_SHRINK_FACTOR = 0.5;

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
        origin.getTypeDataManager().register(SHRINK_COUNT, 0);

        unsafeRunWhenOn(Dist.CLIENT, () -> () -> ClientSlimicianOriginType.onFirstActivate(origin));
    }

    @Override
    public void onPlayerSensitiveEvent(Object event, Origin origin) {
        super.onPlayerSensitiveEvent(event, origin);
        if (isCanceled(event)) return;
        if (event instanceof LivingFallEvent) {
            onPlayerFall((LivingFallEvent) event, origin);
        } else if (event instanceof LivingHurtEvent) {
            onPlayerHurt((LivingHurtEvent) event, origin);
        } else if (event instanceof EntityEvent.Size) {
            // disabled until finished
            // onRecalcSize((EntityEvent.Size) event, origin);
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

    private void onPlayerHurt(LivingHurtEvent event, Origin origin) {
        if (!origin.isServerSide()) return;
        PlayerEntity player = origin.getPlayer();
        if (player.getHealth() - event.getAmount() <= 0) {
            // disabled until finished
            // beforeDeath(event, origin);
        }
    }

    private void beforeDeath(LivingHurtEvent event, Origin origin) {
        DataManager dataManager = origin.getTypeDataManager();
        PlayerEntity player = origin.getPlayer();

        if (dataManager.get(SHRINK_COUNT) < MAX_SHRINK_COUNT) {
            event.setAmount(player.getHealth() - 1);
            dataManager.increase(SHRINK_COUNT, 1);
        } else {
            dataManager.set(SHRINK_COUNT, 0);
        }
    }

    // unfinished, the bounding box shrinks but the rendered model does not
    // - makes it so that you can't take fall damage after dying
    private void onRecalcSize(EntityEvent.Size event, Origin origin) {
        int shrinkCount = origin.getTypeDataManager().get(SHRINK_COUNT);
        float sizeFactor = (float) Math.pow(SIZE_SHRINK_FACTOR, shrinkCount);
        Main.LOGGER.info("shrinkCount: {}, sizeFactor: {}", shrinkCount, sizeFactor);
        event.setNewSize(event.getNewSize().scale(sizeFactor), true);
    }

}
