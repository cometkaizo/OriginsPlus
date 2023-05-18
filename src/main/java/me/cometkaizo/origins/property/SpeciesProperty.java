package me.cometkaizo.origins.property;

import me.cometkaizo.origins.Main;
import me.cometkaizo.origins.origin.Origin;
import me.cometkaizo.origins.util.TagUtils;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.IAngerable;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.world.World;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.event.entity.living.LivingSetAttackTargetEvent;

import javax.annotation.Nonnull;
import java.util.List;
import java.util.function.Predicate;

public class SpeciesProperty extends EventInterceptProperty {
    public static final String NO_AGGRO_LIST_KEY = Main.MOD_ID + "_no_aggro";
    @Nonnull
    private final Predicate<? super LivingEntity> speciesPredicate;
    private final double rallyRadius;
    private final double rallyRadiusSqr;

    protected SpeciesProperty(String name, @Nonnull Predicate<? super LivingEntity> speciesPredicate, double rallyRadius) {
        super(name);
        this.speciesPredicate = speciesPredicate;
        this.rallyRadius = rallyRadius;
        this.rallyRadiusSqr = rallyRadius * rallyRadius;
    }

    @Override
    protected void init() {
        super.init();
        map.put(LivingSetAttackTargetEvent.class, this::onAggro);
        map.put(LivingHurtEvent.class, this::onLivingHurt);
    }

    public void onAggro(LivingSetAttackTargetEvent event, Origin origin) {
        if (event.isCanceled()) return;

        LivingEntity entity = event.getEntityLiving();
        if (!(entity instanceof IAngerable)) return;
        if (isSameSpecies(entity)) return;
        IAngerable angerable = (IAngerable) entity;

        LivingEntity target = event.getTarget();
        if (origin.getPlayer().equals(target) || isOnNoAggroList(entity, target)) {
            angerable.setAttackTarget(null);
        }
    }


    private boolean isOnNoAggroList(LivingEntity entity, LivingEntity target) {
        if (target == null) return false;
        CompoundNBT data = entity.getPersistentData();
        if (!data.contains(NO_AGGRO_LIST_KEY)) return false;

        int targetId = target.getEntityId();
        for (int id : data.getIntArray(NO_AGGRO_LIST_KEY)) {
            if (id == targetId) return true;
        }
        return false;
    }

    public void onLivingHurt(LivingHurtEvent event, Origin origin) {
        if (event.isCanceled()) return;
        if (!origin.isServerSide()) return;
        if (!origin.getPlayer().equals(event.getSource().getTrueSource())) return;

        LivingEntity target = event.getEntityLiving();
        if (!(target instanceof IAngerable)) return;

        if (isSameSpecies(target)) {
            IAngerable angerable = (IAngerable) target;
            LivingEntity targetAttackTarget = angerable.getAttackTarget();
            addToNoAggroList(target, targetAttackTarget);
        } else {
            aggroNearbyEntities(origin.getPlayer(), target);
        }
    }

    private boolean isSameSpecies(LivingEntity entity) {
        return speciesPredicate.test(entity);
    }

    private static void addToNoAggroList(LivingEntity entity, LivingEntity attackTarget) {
        if (attackTarget != null) {
            int aggroId = attackTarget.getEntityId();
            CompoundNBT data = entity.getPersistentData();
            TagUtils.appendOrCreate(data, NO_AGGRO_LIST_KEY, aggroId);
        }
    }

    private void aggroNearbyEntities(PlayerEntity player, LivingEntity target) {
        World world = player.world;

        AxisAlignedBB affectedArea = player.getBoundingBox().grow(rallyRadius);
        List<Entity> affectedEntities = world.getEntitiesWithinAABB(Entity.class, affectedArea);

        for (Entity entity : affectedEntities) {
            if (entity.getDistanceSq(player) > rallyRadiusSqr) return;
            if (!(entity instanceof IAngerable)) return;

            ((IAngerable) entity).setAttackTarget(target);
        }
    }

    public static class Builder {
        private String name = "Species";
        private Predicate<? super LivingEntity> speciesPredicate;
        private double rallyRadius = 0;

        public Builder(Predicate<? super LivingEntity> speciesPredicate) {
            setSpecies(speciesPredicate);
        }

        public Builder(EntityType<? extends IAngerable> entityType) {
            setSpecies(entityType);
        }

        public Builder() {

        }

        public Builder setName(String name) {
            this.name = name;
            return this;
        }

        public Builder setSpecies(Predicate<? super LivingEntity> speciesPredicate) {
            this.speciesPredicate = speciesPredicate;
            return this;
        }

        public Builder setSpecies(EntityType<? extends IAngerable> entityType) {
            this.speciesPredicate = entity -> entity.getType().equals(entityType);
            return this;
        }

        public Builder setRallyRadius(double range) {
            this.rallyRadius = range;
            return this;
        }

        public SpeciesProperty build() {
            return new SpeciesProperty(name, speciesPredicate, rallyRadius);
        }
    }
}
