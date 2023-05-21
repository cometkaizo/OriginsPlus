package me.cometkaizo.origins.origin;

import me.cometkaizo.origins.Main;
import me.cometkaizo.origins.property.SpeciesProperty;
import me.cometkaizo.origins.util.DataKey;
import me.cometkaizo.origins.util.DataManager;
import me.cometkaizo.origins.util.TimeTracker;
import net.minecraft.client.entity.player.ClientPlayerEntity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.MovementInput;
import net.minecraft.util.SoundEvents;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraft.world.World;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingFallEvent;
import net.minecraftforge.eventbus.api.Event;

import static net.minecraft.util.math.MathHelper.lerp;

public class SlimicianOriginType extends AbstractOriginType {

    public static final float SLIME_AGGRO_RANGE = 10;

    public static final SpeciesProperty SLIME_SPECIES = new SpeciesProperty.Builder()
            .setMobSpecies(EntityType.SLIME)
            .setRallyRadius(SLIME_AGGRO_RANGE).build();
    public static final double FALL_BOUNCE_HEIGHT_AMP = 0.75;
    public static final float BOUNCE_SUPPRESS_FALL_DAMAGE_AMP = 0.5F;
    private static final double FALL_SUPER_BOUNCE_HEIGHT_AMP = 1.1;
    public static final double LATE_SUPER_BOUNCE_Y_AMP = 2;//FALL_SUPER_BOUNCE_HEIGHT_AMP / FALL_BOUNCE_HEIGHT_AMP;
    public static final double WALL_SLIDE_DISTANCE = 0.2;
    public static final double SLIDE_SPEED = -0.05D;
    public static final double SMOOTH_SLIDE_SPEED = -0.2D;

    protected static final DataKey<Boolean> CLIENT_PREV_ON_GROUND = DataKey.create(Boolean.class);
    protected static final DataKey<Boolean> CLIENT_PREV_JUMPING = DataKey.create(Boolean.class);
    protected static final DataKey<Double> CLIENT_PREV_MOTION_Y = DataKey.create(Double.class);
    // Minecraft is not multithreaded so should work
    private final BlockPos.Mutable wallCheckPos = new BlockPos.Mutable();

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
        if (origin.isServerSide()) return;
        origin.getTypeDataManager().register(CLIENT_PREV_ON_GROUND, true);
        origin.getTypeDataManager().register(CLIENT_PREV_JUMPING, true);
        origin.getTypeDataManager().register(CLIENT_PREV_MOTION_Y, 0D);
    }

    @Override
    public void onPlayerSensitiveEvent(Object event, Origin origin) {
        super.onPlayerSensitiveEvent(event, origin);
        if (isCanceled(event)) return;
        if (event instanceof LivingFallEvent) {
            onPlayerFall((LivingFallEvent) event, origin);
        } else if (event instanceof TickEvent.ClientTickEvent) {
            onClientTick((TickEvent.ClientTickEvent) event, origin);
        }
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

    private void onClientTick(TickEvent.ClientTickEvent event, Origin origin) {
        if (event.phase == TickEvent.Phase.START) return;
        if (origin.isServerSide()) return;
        ClientPlayerEntity player = (ClientPlayerEntity) origin.getPlayer();
        DataManager dataManager = origin.getTypeDataManager();
        MovementInput input = player.movementInput;

        applySlide(player);
        applyBounce(player, origin);

        dataManager.set(CLIENT_PREV_JUMPING, input.jump);
        dataManager.set(CLIENT_PREV_ON_GROUND, player.isOnGround());
        dataManager.set(CLIENT_PREV_MOTION_Y, player.getMotion().y);
    }

    private static Boolean prevJumped(DataManager dataManager) {
        return dataManager.get(CLIENT_PREV_JUMPING);
    }

    private static void applyBounce(ClientPlayerEntity player, Origin origin) {
        DataManager dataManager = origin.getTypeDataManager();
        TimeTracker timeTracker = origin.getTimeTracker();
        MovementInput input = player.movementInput;

        if (player.isSuppressingBounce()) return;

        if (player.isOnGround() && !prevOnGround(dataManager)) {
            if (timeTracker.hasTimer(Timer.JUMPED)) {
                Main.LOGGER.info("EARLY JUMP");
                superBounce(player, dataManager);
                timeTracker.remove(Timer.JUMPED);
            } else {
                bounce(player, dataManager, FALL_BOUNCE_HEIGHT_AMP);
            }
            timeTracker.addTimer(Timer.BOUNCED);
        } else if (timeTracker.hasTimer(Timer.JUMPED) && timeTracker.hasTimer(Timer.BOUNCED)) {
            Main.LOGGER.info("LATE JUMP");
            player.setMotion(player.getMotion().mul(1, LATE_SUPER_BOUNCE_Y_AMP, 1));
            timeTracker.remove(Timer.JUMPED);
        } else {
            if (input.jump && !prevJumped(dataManager))
                timeTracker.addTimer(Timer.JUMPED);
        }
    }

    private static void superBounce(ClientPlayerEntity player, DataManager dataManager) {
        bounce(player, dataManager, FALL_SUPER_BOUNCE_HEIGHT_AMP);
    }

    private static void bounce(PlayerEntity player, DataManager dataManager, double fallBounceHeightAmp) {
        Vector3d motion = player.getMotion();
        double prevMotionY = dataManager.get(CLIENT_PREV_MOTION_Y);
        if (prevMotionY < -0.2) {
            player.setMotion(motion.x, -prevMotionY * fallBounceHeightAmp, motion.z);
        }
    }

    private static Boolean prevOnGround(DataManager dataManager) {
        return dataManager.get(CLIENT_PREV_ON_GROUND);
    }

    private void applySlide(PlayerEntity player) {
        if (player.collidedHorizontally || isNextToWall(player)) {
            if (player.isSneaking()) {
                applySlideVelocity(player);
                applySlideEffects(player);
            } else {
                setSmoothSlideVelocity(player);
                smoothSlideEffects(player);
            }
        }
    }

    private boolean isNextToWall(PlayerEntity player) {
        double x = player.getPosX();
        double y = player.getPosY();
        double z = player.getPosZ();
        AxisAlignedBB boundingBox = player.getBoundingBox();
        double halfWidth = (boundingBox.maxX - boundingBox.minX) / 2;
        double halfDepth = (boundingBox.maxZ - boundingBox.minZ) / 2;
        World world = player.world;

        return world.getBlockState(wallCheckPos.setPos(x + halfWidth + WALL_SLIDE_DISTANCE, y, z + halfDepth + WALL_SLIDE_DISTANCE)).isSolid() ||
                world.getBlockState(wallCheckPos.setPos(x + halfWidth + WALL_SLIDE_DISTANCE, y, z - halfDepth - WALL_SLIDE_DISTANCE)).isSolid() ||
                world.getBlockState(wallCheckPos.setPos(x - halfWidth - WALL_SLIDE_DISTANCE, y, z + halfDepth + WALL_SLIDE_DISTANCE)).isSolid() ||
                world.getBlockState(wallCheckPos.setPos(x - halfWidth - WALL_SLIDE_DISTANCE, y, z - halfDepth - WALL_SLIDE_DISTANCE)).isSolid();
    }

    private void applySlideVelocity(PlayerEntity player) {
        Vector3d motion = player.getMotion();
        if (motion.y < -0.13D) {
            double d0 = SLIDE_SPEED / motion.y;
            player.setMotion(motion.x * d0, SLIDE_SPEED, motion.z * d0);
        } else {
            player.setMotion(motion.x, SLIDE_SPEED, motion.z);
        }

        player.fallDistance = 0.0F;
    }

    private void applySlideEffects(PlayerEntity player) {
        if (random.nextInt(5) == 0) {
            player.playSound(SoundEvents.BLOCK_HONEY_BLOCK_SLIDE, 1.0F, 1.0F);
        }

        if (!player.world.isRemote && random.nextInt(5) == 0) {
            player.world.setEntityState(player, (byte)53);
        }
    }

    private void setSmoothSlideVelocity(PlayerEntity player) {
        Vector3d motion = player.getMotion();
        if (motion.y < -0.1D) {
            double d0 = SMOOTH_SLIDE_SPEED / motion.y;
            player.setMotion(motion.x * d0, SMOOTH_SLIDE_SPEED, motion.z * d0);
        } else {
            player.setMotion(motion.x, lerp(0.01, motion.y, SMOOTH_SLIDE_SPEED), motion.z);
        }

        player.fallDistance = 0.0F;
    }

    private void smoothSlideEffects(PlayerEntity player) {
        if (random.nextInt(20) == 0) {
            player.playSound(SoundEvents.BLOCK_HONEY_BLOCK_SLIDE, 1.0F, 1.0F);
        }

        if (!player.world.isRemote && random.nextInt(5) == 0) {
            player.world.setEntityState(player, (byte)53);
        }
    }
}
