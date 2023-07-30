package me.cometkaizo.origins.origin;

import me.cometkaizo.origins.Main;
import me.cometkaizo.origins.network.C2SUpdatePhasing;
import me.cometkaizo.origins.network.Packets;
import me.cometkaizo.origins.network.S2CEnumAction;
import me.cometkaizo.origins.origin.client.ClientPhantomOriginType;
import me.cometkaizo.origins.property.Property;
import me.cometkaizo.origins.property.SpeciesProperty;
import me.cometkaizo.origins.util.AttributeUtils;
import me.cometkaizo.origins.util.DataKey;
import me.cometkaizo.origins.util.TimeTracker;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.ai.attributes.Attributes;
import net.minecraft.entity.item.BoatEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.inventory.EquipmentSlotType;
import net.minecraft.item.Items;
import net.minecraft.util.RegistryKey;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.shapes.VoxelShape;
import net.minecraft.world.World;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.fml.network.PacketDistributor;

import static me.cometkaizo.origins.util.PhysicsUtils.*;

public class PhantomOriginType extends AbstractOriginType {
    public static final Property PHANTOM_SPECIES = SpeciesProperty.Builder.withMobSpecies(EntityType.PHANTOM)
            .setRallyRadius(40).build();
    public static final DataKey<Boolean> IS_IN_PHANTOM_FORM = DataKey.create(Boolean.class);
    public static final DataKey<Boolean> IS_PHASING = DataKey.create(Boolean.class);
    public static final DataKey<Boolean> PREV_IS_PHASING = DataKey.create(Boolean.class);
    public static final double MIN_MOB_DEAGGRO_DISTANCE = 1.25;
    protected float phantomFormExhaustion = 0.1F / 20F;

    protected PhantomOriginType() {
        super("Phantom", Items.PHANTOM_MEMBRANE, type -> new Origin.Description(type,
                new Origin.Description.Entry(type, "translucent")),
                PHANTOM_SPECIES);
    }

    @Override
    public boolean hasLabel(Object label, Origin origin) {
        return label == Label.NO_INSOMNIA ||
                label == Label.PHASE_THROUGH_BLOCKS ||
                label == Label.TRANSLUCENT_SKIN ||
                super.hasLabel(label, origin);
    }

    @Override
    public void init(Origin origin) {
        super.init(origin);
        origin.getTypeData().registerSaved(IS_IN_PHANTOM_FORM, false, new ResourceLocation(Main.MOD_ID, "in_phantom_form"));
        if (origin.isServerSide()) origin.getTypeData().register(IS_PHASING, false);
        else if (origin.isClientSide()) origin.getTypeData().register(PREV_IS_PHASING, false);
    }

    @Override
    public void activate(Origin origin) {
        super.activate(origin);
        updateMaxHealth(origin);
        if (origin.isClientSide()) Packets.sendToServer(new C2SUpdatePhasing(isPhasing(origin)));
    }

    @Override
    public void deactivate(Origin origin) {
        super.deactivate(origin);
        PlayerEntity player = origin.getPlayer();
        AttributeUtils.setAttribute(player, Attributes.MAX_HEALTH, Attributes.MAX_HEALTH.getDefaultValue());
    }

    @Override
    public void performAction(Origin origin) {
        super.performAction(origin);
        if (origin.isClientSide()) return;

        TimeTracker timeTracker = origin.getTimeTracker();
        if (timeTracker.hasTimer(Cooldown.TOGGLE_PHANTOM_FORM)) return;

        setPhantomForm(origin, !isInPhantomForm(origin));

        timeTracker.addTimer(Cooldown.TOGGLE_PHANTOM_FORM);
    }

    @Override
    public void onEvent(Object event, Origin origin) {
        super.onEvent(event, origin);
        if (isCanceled(event)) return;
        if (event == Action.PHANTOM_FORM_ON) {
            setPhantomForm(origin, true);
        } else if (event == Action.PHANTOM_FORM_OFF) {
            setPhantomForm(origin, false);
        }
    }

    @Override
    public void onPlayerSensitiveEvent(Object event, Origin origin) {
        super.onPlayerSensitiveEvent(event, origin);
        if (isCanceled(event)) return;
        if (event instanceof PlayerEvent.PlayerRespawnEvent) {
            if (!((PlayerEvent.PlayerRespawnEvent) event).isEndConquered()) setPhantomForm(origin, false);
        } else if (event instanceof TickEvent.PlayerTickEvent) {
            updateFire(origin);
            applyExhaustion(origin);
        } else if (event instanceof PlayerEvent.PlayerChangedDimensionEvent) {
            updateMaxHealth(origin);
        }
    }

    public static void setPhantomForm(Origin origin, boolean phantomForm) {
        if (!origin.getTypeData().contains(IS_IN_PHANTOM_FORM)) return;
        origin.getTypeData().set(IS_IN_PHANTOM_FORM, phantomForm);

        if (origin.isServerSide()) {
            Packets.CHANNEL.send(PacketDistributor.ALL.noArg(), new S2CEnumAction(origin.getPlayer(),
                    phantomForm ? Action.PHANTOM_FORM_ON : Action.PHANTOM_FORM_OFF));
        } else if (origin.isClientSide() && !phantomForm) Packets.sendToServer(new C2SUpdatePhasing(false));
    }

    public static boolean isInPhantomForm(Origin origin) {
        return origin != null &&
                origin.getTypeData().contains(IS_IN_PHANTOM_FORM) &&
                origin.getTypeData().get(IS_IN_PHANTOM_FORM);
    }

    public boolean shouldPhase(Origin origin) {
        if (origin.isServerSide()) return false;
        if (!isInPhantomForm(origin)) return false;
        PlayerEntity player = origin.getPlayer();
        if (getBlockUnder(player).getCollisionShape(player.world, getPosUnder(player)).isEmpty()) return false;
        return DistExecutor.unsafeCallWhenOn(Dist.CLIENT, () -> () -> ClientPhantomOriginType.shouldPhase(origin));
    }

    public boolean isPhasing(Origin origin) {
        if (origin.isServerSide()) return origin.getTypeData().get(IS_PHASING);
        if (!isInPhantomForm(origin)) return false;
        PlayerEntity player = origin.getPlayer();
        VoxelShape blockCollision = getBlockAt(player).getCollisionShape(player.world, player.getPosition());
        if (blockCollision.isEmpty() || !blockCollision.getBoundingBox().offset(player.getPosition()).intersects(player.getBoundingBox())) return false;
        return DistExecutor.unsafeCallWhenOn(Dist.CLIENT, () -> () -> ClientPhantomOriginType.isPhasing(origin));
    }

    protected void updateFire(Origin origin) {
        PlayerEntity player = origin.getPlayer();
        if (!isWearingHelmet(player) && isInDaylight(player) && !isInPhantomForm(origin))
            player.setFire(8);
    }

    public boolean isWearingHelmet(PlayerEntity player) {
        return !player.getItemStackFromSlot(EquipmentSlotType.HEAD).isEmpty();
    }

    protected boolean isInDaylight(PlayerEntity player) {
        if (player.world.isDaytime() && !player.world.isRemote) {
            float f = player.getBrightness();
            BlockPos blockpos = player.getRidingEntity() instanceof BoatEntity ?
                    (new BlockPos(player.getPosX(), (double)Math.round(player.getPosY()), player.getPosZ())).up() :
                    new BlockPos(player.getPosX(), (double)Math.round(player.getPosY()), player.getPosZ());
            return f > 0.5F && random.nextFloat() * 30.0F < (f - 0.4F) * 2.0F && player.world.canSeeSky(blockpos);
        }
        return false;
    }

    public void updateMaxHealth(Origin origin) {
        PlayerEntity player = origin.getPlayer();
        AttributeUtils.setAttribute(player, Attributes.MAX_HEALTH, getMaxHealth(player));
    }

    public double getMaxHealth(PlayerEntity player) {
        RegistryKey<World> dimension = player.getEntityWorld().getDimensionKey();
        if (dimension == World.OVERWORLD) return 9 * 2;
        if (dimension == World.THE_END) return 8 * 2;
        return 7 * 2;
    }

    public void applyExhaustion(Origin origin) {
        PlayerEntity player = origin.getPlayer();
        if (player.world.isDaytime() && !player.world.isRemote && isInPhantomForm(origin)) {
            player.addExhaustion(phantomFormExhaustion);
        }
    }

    public enum Action {
        PHANTOM_FORM_ON,
        PHANTOM_FORM_OFF
    }

    public enum Label {
        NO_INSOMNIA,
        PHASE_THROUGH_BLOCKS,
        TRANSLUCENT_SKIN
    }

    public enum Cooldown implements TimeTracker.Timer {
        TOGGLE_PHANTOM_FORM(1.5 * 20);
        private final int duration;

        Cooldown(double duration) {
            this.duration = (int) duration;
        }

        @Override
        public int getDuration() {
            return duration;
        }
    }
}
