package me.cometkaizo.origins.common;

import net.minecraft.entity.Entity;
import net.minecraft.util.DamageSource;
import net.minecraft.util.IndirectEntityDamageSource;

import javax.annotation.Nullable;

public class OriginDamageSources {

    public static final DamageSource TOUCH_WATER = new DamageSource("touchingWater");
    public static final DamageSource DRINK_WATER = new DamageSource("drinkingWater");
    public static final DamageSource SCARE = new DamageSource("scare");


    public static DamageSource causeWaterDamage(Entity target, @Nullable Entity indirectSource) {
        return new IndirectEntityDamageSource("indirectTouchingWater", target, indirectSource);
    }


    private OriginDamageSources() {
        throw new AssertionError("No OriginDamageSources instances for you!");
    }
}
