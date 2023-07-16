package me.cometkaizo.origins.util;

import net.minecraft.entity.LivingEntity;
import net.minecraft.potion.Effect;
import net.minecraft.potion.EffectInstance;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.function.Supplier;

public class EffectApplier {
    protected final Collection<Supplier<? extends EffectInstance>> effectSuppliers = new ArrayList<>(1);
    protected final Collection<LivingEntity> targets = new ArrayList<>(0);

    public EffectApplier withEffect(Supplier<? extends EffectInstance> effectSup) {
        effectSuppliers.add(effectSup);
        return this;
    }

    public EffectApplier withEffect(Effect effect) {
        return withEffect(() -> new EffectInstance(effect));
    }

    public EffectApplier withEffect(Effect effect, int duration) {
        return withEffect(() -> new EffectInstance(effect, duration));
    }

    public EffectApplier withEffect(Effect effect, int duration, int amplifier) {
        return withEffect(() -> new EffectInstance(effect, duration, amplifier));
    }

    public EffectApplier withEffect(Effect effect, int duration, int amplifier, boolean ambient, boolean showParticles) {
        return withEffect(() -> new EffectInstance(effect, duration, amplifier, ambient, showParticles));
    }

    public EffectApplier withEffects(Collection<? extends Supplier<? extends EffectInstance>> effectSup) {
        effectSuppliers.addAll(effectSup);
        return this;
    }

    public EffectApplier toTarget(LivingEntity target) {
        targets.add(target);
        return this;
    }

    public EffectApplier toTargets(Collection<? extends LivingEntity> target) {
        targets.addAll(target);
        return this;
    }

    public void apply() {
        applyTo(targets);
    }

    public void applyIncluding(Collection<? extends LivingEntity> extraTargets) {
        List<LivingEntity> allTargets = new ArrayList<>(targets);
        if (extraTargets != null) allTargets.addAll(extraTargets);
        applyTo(allTargets);
    }

    public void applyIncluding(LivingEntity extraTarget) {
        List<LivingEntity> allTargets = new ArrayList<>(targets);
        if (extraTarget != null) allTargets.add(extraTarget);
        applyTo(allTargets);
    }

    public void applyTo(Collection<? extends LivingEntity> targets) {
        for (LivingEntity target : targets) applyTo(target);
    }

    public void applyTo(LivingEntity target) {
        if (target != null) {
            for (Supplier<? extends EffectInstance> effectSup : effectSuppliers) {
                EffectInstance effect = effectSup.get();
                target.addPotionEffect(effect);
            }
        }
    }
}
