package me.cometkaizo.origins.property;

import me.cometkaizo.origins.common.OriginDamageSources;
import me.cometkaizo.origins.origin.Origin;
import me.cometkaizo.origins.origin.PhoenixOriginType;
import me.cometkaizo.origins.util.SoundUtils;
import me.cometkaizo.origins.util.TimeTracker;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.projectile.PotionEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.potion.EffectInstance;
import net.minecraft.potion.Potion;
import net.minecraft.potion.PotionUtils;
import net.minecraft.potion.Potions;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.SoundEvents;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.ProjectileImpactEvent;

import java.util.List;

public class WaterSensitiveProperty extends EventInterceptProperty {
    public static final int DEFAULT_WATER_DAMAGE = 3;
    private final int waterDamage;
    private final TimeTracker.Timer damageCooldown;

    protected WaterSensitiveProperty(String name, int waterDamage, TimeTracker.Timer damageCooldown) {
        super(name);
        this.waterDamage = waterDamage;
        this.damageCooldown = damageCooldown;
    }

    public enum Cooldown implements TimeTracker.Timer {
        WATER_DAMAGE(0);
        public final int duration;
        Cooldown(double duration) {
            this.duration = (int) duration;
        }
        @Override
        public int getDuration() {
            return duration;
        }
    }

    @Override
    protected void init() {
        super.init();
        map.put(ProjectileImpactEvent.Throwable.class, this::onProjectileImpact);
        map.put(TickEvent.PlayerTickEvent.class, this::onPlayerTick);
    }


    public void onProjectileImpact(ProjectileImpactEvent.Throwable event, Origin origin) {
        if (event.isCanceled()) return;
        if (!(event.getThrowable() instanceof PotionEntity)) return;

        PotionEntity potionEntity = (PotionEntity) event.getThrowable();
        ItemStack potionItem = potionEntity.getItem();
        if (!isWaterPotion(potionItem) || hasEffects(potionItem)) return;

        AxisAlignedBB affectedArea = potionEntity.getBoundingBox().grow(4.0D, 2.0D, 4.0D);
        List<LivingEntity> affectedEntities = potionEntity.world.getEntitiesWithinAABB(LivingEntity.class, affectedArea, e -> true);

        for (LivingEntity entity : affectedEntities) {
            PlayerEntity player = origin.getPlayer();
            Entity shooter = potionEntity.getShooter();
            double distance = potionEntity.getPositionVec().squareDistanceTo(entity.getEyePosition(1));

            if (distance < 16D && entity.equals(player)) {
                boolean damaged = entity.attackEntityFrom(
                        OriginDamageSources.causeWaterDamage(player, shooter),
                        (float) (16 - distance) / 2 // max damage = 4 hearts
                );
                if (damaged) SoundUtils.playSound(player, SoundEvents.ENTITY_ENDERMAN_HURT, SoundCategory.HOSTILE, 1F, 1);
            }
        }
    }

    private static boolean isWaterPotion(ItemStack potionItem) {
        Potion potion = PotionUtils.getPotionFromItem(potionItem);
        return potion == Potions.WATER ||
                potion == Potions.AWKWARD ||
                potion == Potions.MUNDANE ||
                potion == Potions.THICK;
    }

    private static boolean hasEffects(ItemStack potionItem) {
        List<EffectInstance> effects = PotionUtils.getEffectsFromStack(potionItem);
        return !effects.isEmpty();
    }

    public void onPlayerTick(TickEvent.PlayerTickEvent event, Origin origin) {
        if (event.isCanceled()) return;
        if (event.side.isClient()) return;
        if (!origin.getPlayer().equals(event.player)) return;
        PlayerEntity player = origin.getPlayer();
        TimeTracker cooldownTracker = origin.getTimeTracker();

        if (player.isInWaterRainOrBubbleColumn()) {
            if (!cooldownTracker.hasTimer(damageCooldown)) {
                applyWaterDamage(player);
                cooldownTracker.addTimer(damageCooldown);
            }
        } else {
            cooldownTracker.remove(PhoenixOriginType.Cooldown.WATER_DAMAGE);
        }
    }

    private void applyWaterDamage(PlayerEntity player) {
        boolean damaged = player.attackEntityFrom(OriginDamageSources.TOUCH_WATER, waterDamage);
        if (damaged) SoundUtils.playSound(player, SoundEvents.ENTITY_ENDERMAN_HURT, SoundCategory.HOSTILE, 0.7F, 1);
    }

    public static class Builder {
        private String name = "Water Sensitivity";
        private int waterDamage = DEFAULT_WATER_DAMAGE;
        private TimeTracker.Timer damageCooldown = Cooldown.WATER_DAMAGE;

        public Builder setName(String name) {
            this.name = name;
            return this;
        }

        public Builder setWaterDamage(int waterDamage) {
            this.waterDamage = waterDamage;
            return this;
        }

        public Builder setDamageCooldown(TimeTracker.Timer damageCooldown) {
            this.damageCooldown = damageCooldown;
            return this;
        }

        public WaterSensitiveProperty build() {
            return new WaterSensitiveProperty(name, waterDamage, damageCooldown);
        }
    }
}
