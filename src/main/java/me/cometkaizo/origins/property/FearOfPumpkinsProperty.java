package me.cometkaizo.origins.property;

import me.cometkaizo.origins.common.OriginDamageSources;
import me.cometkaizo.origins.origin.Origin;
import me.cometkaizo.origins.util.SoundUtils;
import me.cometkaizo.origins.util.TimeTracker;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.player.ClientPlayerEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.inventory.EquipmentSlotType;
import net.minecraft.item.Item;
import net.minecraft.item.Items;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.SoundEvents;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.BlockRayTraceResult;
import net.minecraft.util.math.RayTraceResult;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingEquipmentChangeEvent;

public class FearOfPumpkinsProperty extends EventInterceptProperty {
    public static final int DEFAULT_RANGE = 5;
    public static final int DEFAULT_PUMPKIN_DAMAGE = 7;
    public static final int DEFAULT_JACK_O_LANTERN_DAMAGE = 17;

    private final int range;
    private final int pumpkinScareDamage;
    private final int jackOLanternScareDamage;
    private final TimeTracker.Cooldown pumpkinCooldown;
    private final TimeTracker.Cooldown jackOLanternCooldown;


    public enum Cooldown implements TimeTracker.Cooldown {
        PUMPKIN(0),
        JACK_O_LANTERN(0);
        public final int duration;
        Cooldown(double duration) {
            this.duration = (int) duration;
        }
        @Override
        public int getDuration() {
            return duration;
        }
    }

    protected FearOfPumpkinsProperty(String name, int range, int pumpkinScareDamage, int jackOLanternScareDamage, TimeTracker.Cooldown pumpkinCooldown, TimeTracker.Cooldown jackOLanternCooldown) {
        super(name);
        this.range = range;
        this.pumpkinScareDamage = pumpkinScareDamage;
        this.jackOLanternScareDamage = jackOLanternScareDamage;
        this.pumpkinCooldown = pumpkinCooldown;
        this.jackOLanternCooldown = jackOLanternCooldown;
    }

    @Override
    protected void init() {
        super.init();
        map.put(LivingEquipmentChangeEvent.class, this::onEquipmentChange);
        map.put(TickEvent.PlayerTickEvent.class, this::onPlayerTick);
        map.put(TickEvent.ClientTickEvent.class, this::onClientTick);
    }

    public void onEquipmentChange(LivingEquipmentChangeEvent event, Origin origin) {
        if (event.isCanceled()) return;
        if (!origin.getPlayer().equals(event.getEntity())) return;
        if (event.getSlot() != EquipmentSlotType.HEAD) return;

        Item itemAfter = event.getTo().getItem();
        if (itemAfter == Items.CARVED_PUMPKIN || itemAfter == Items.JACK_O_LANTERN) {
            PlayerEntity player = origin.getPlayer();
            applyShockDamage(player);
        }
    }

    private static void applyShockDamage(PlayerEntity player) {
        boolean damaged = player.attackEntityFrom(OriginDamageSources.SCARE, player.getMaxHealth() * 1.5F);
        if (damaged) SoundUtils.playSound(player, SoundEvents.ENTITY_ENDERMAN_SCREAM, SoundCategory.PLAYERS, 1, 1);
    }

    public void onPlayerTick(TickEvent.PlayerTickEvent event, Origin origin) {
        if (event.isCanceled()) return;
        if (event.side.isClient()) return;
        if (!origin.getPlayer().equals(event.player)) return;
        PlayerEntity player = origin.getPlayer();

        if (player.getItemStackFromSlot(EquipmentSlotType.HEAD).getItem() == Items.CARVED_PUMPKIN) {
            applyPumpkinDamage(player);
        } else if (player.getItemStackFromSlot(EquipmentSlotType.HEAD).getItem() == Items.JACK_O_LANTERN) {
            applyJackOLanternDamage(player);
        }
    }

    public void onClientTick(TickEvent.ClientTickEvent event, Origin origin) {
        if (event.isCanceled()) return;
        if (event.phase == TickEvent.Phase.START) return;
        if (origin.isServerSide()) return;

        ClientPlayerEntity player = Minecraft.getInstance().player;
        if (!origin.getPlayer().equals(player)) return;
        BlockRayTraceResult rayTraceResult = (BlockRayTraceResult) player.pick(range, 0, false);
        if (rayTraceResult.getType() != RayTraceResult.Type.BLOCK) return;

        BlockPos blockpos = rayTraceResult.getPos();
        BlockState blockstate = player.world.getBlockState(blockpos);
        TimeTracker cooldownTracker = origin.getTimeTracker();

        if (blockstate.getBlock() == Blocks.CARVED_PUMPKIN && !cooldownTracker.hasTimer(pumpkinCooldown)) {
            applyPumpkinDamage(player);
            cooldownTracker.addTimer(pumpkinCooldown);
        } else if (blockstate.getBlock() == Blocks.JACK_O_LANTERN && !cooldownTracker.hasTimer(jackOLanternCooldown)) {
            applyJackOLanternDamage(player);
            cooldownTracker.addTimer(jackOLanternCooldown);
        }
    }

    private void applyJackOLanternDamage(PlayerEntity player) {
        boolean damaged = player.attackEntityFrom(OriginDamageSources.SCARE, jackOLanternScareDamage);
        if (damaged) SoundUtils.playSound(player, SoundEvents.ENTITY_ENDERMAN_HURT, SoundCategory.HOSTILE, 1, 1);
    }

    private void applyPumpkinDamage(PlayerEntity player) {
        boolean damaged = player.attackEntityFrom(OriginDamageSources.SCARE, pumpkinScareDamage);
        if (damaged) SoundUtils.playSound(player, SoundEvents.ENTITY_ENDERMAN_HURT, SoundCategory.HOSTILE, 0.7F, 1);
    }

    public static class Builder {
        private String name = "Fear Of Pumpkins";
        private int range = DEFAULT_RANGE;
        private int pumpkinDamage = DEFAULT_PUMPKIN_DAMAGE;
        private int jackOLanternDamage = DEFAULT_JACK_O_LANTERN_DAMAGE;
        private TimeTracker.Cooldown pumpkinCooldown = Cooldown.PUMPKIN;
        private TimeTracker.Cooldown jackOLanternCooldown = Cooldown.JACK_O_LANTERN;

        public Builder setName(String name) {
            this.name = name;
            return this;
        }

        public Builder setRange(int range) {
            this.range = range;
            return this;
        }

        public Builder setPumpkinDamage(int pumpkinDamage) {
            this.pumpkinDamage = pumpkinDamage;
            return this;
        }

        public Builder setJackOLanternDamage(int jackOLanternDamage) {
            this.jackOLanternDamage = jackOLanternDamage;
            return this;
        }

        public Builder setPumpkinCooldown(TimeTracker.Cooldown pumpkinCooldown) {
            this.pumpkinCooldown = pumpkinCooldown;
            return this;
        }

        public Builder setJackOLanternCooldown(TimeTracker.Cooldown jackOLanternCooldown) {
            this.jackOLanternCooldown = jackOLanternCooldown;
            return this;
        }

        public FearOfPumpkinsProperty build() {
            return new FearOfPumpkinsProperty(name, range, pumpkinDamage, jackOLanternDamage, pumpkinCooldown, jackOLanternCooldown);
        }
    }

}
