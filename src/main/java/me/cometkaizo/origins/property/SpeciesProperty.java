package me.cometkaizo.origins.property;

import me.cometkaizo.origins.Main;
import me.cometkaizo.origins.origin.Origin;
import me.cometkaizo.origins.util.TagUtils;
import net.minecraft.entity.*;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.world.World;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.event.entity.living.LivingSetAttackTargetEvent;

import javax.annotation.Nonnull;
import java.util.List;

public class SpeciesProperty extends EventInterceptProperty {
    public static final String NO_AGGRO_LIST_KEY = Main.MOD_ID + "_no_aggro";
    @Nonnull
    private final Species species;
    private final double rallyRadius;
    private final double rallyRadiusSqr;

    protected SpeciesProperty(String name, @Nonnull Species species, double rallyRadius) {
        super(name);
        this.species = species;
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
        if (event.getTarget() == null) return;
        if (!origin.isServerSide()) return;
        if (!origin.getType().hasProperty(this)) return;

        LivingEntity entity = event.getEntityLiving();
        LivingEntity target = event.getTarget();
        boolean includes = species.includes(entity);
        if (!includes) return;

        boolean shouldNotAttack = shouldNotAttack(entity, target, origin.getPlayer());
        if (shouldNotAttack) {
            species.setAttackTarget(entity, null);
        }
    }

    private boolean shouldNotAttack(LivingEntity entity, LivingEntity target, PlayerEntity player) {
        return target == player || isOnNoAggroList(entity, target);
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
        if (!origin.getType().hasProperty(this)) return;

        if (isPlayerCaused(event, origin)) {
            onPlayerDamageEntity(event, origin);
        } else if (isPlayerVictim(event, origin)) {
            onPlayerDamaged(event, origin);
        }
    }

    private static boolean isPlayerCaused(LivingHurtEvent event, Origin origin) {
        return origin.getPlayer().equals(event.getSource().getTrueSource());
    }

    private void onPlayerDamageEntity(LivingHurtEvent event, Origin origin) {
        LivingEntity target = event.getEntityLiving();

        if (species.includes(target)) {
            species.deaggro(target);
        } else {
            aggroNearbyEntities(origin.getPlayer(), target);
        }
    }

    private boolean isPlayerVictim(LivingHurtEvent event, Origin origin) {
        return origin.getPlayer().equals(event.getEntity());
    }

    private void onPlayerDamaged(LivingHurtEvent event, Origin origin) {
        Entity attacker = event.getSource().getTrueSource();
        if (attacker instanceof LivingEntity) {
            aggroNearbyEntities(origin.getPlayer(), (LivingEntity) attacker);
        }
    }

    private void aggroNearbyEntities(PlayerEntity player, LivingEntity target) {
        World world = player.world;

        AxisAlignedBB affectedArea = player.getBoundingBox().grow(rallyRadius);
        List<Entity> ralliedEntities = world.getEntitiesWithinAABB(Entity.class, affectedArea, species::includes);

        for (Entity entity : ralliedEntities) {
            if (entity.getDistanceSq(player) > rallyRadiusSqr) continue;
            species.setAttackTarget(entity, target);
        }
    }

    public static class Builder {
        private String name = "Species";
        private Species species;
        private double rallyRadius = 0;

        public Builder(Species species) {
            this.species = species;
        }
        protected Builder() {}

        public static Builder withSpecies(Species species) {
            return new Builder(species);
        }
        public static Builder withAngerableSpecies(EntityType<? extends IAngerable> entityType) {
            return new Builder().setAngerableSpecies(entityType);
        }
        public static Builder withMobSpecies(EntityType<? extends MobEntity> entityType) {
            return new Builder().setMobSpecies(entityType);
        }

        public Builder setName(String name) {
            this.name = name;
            return this;
        }

        public Builder setSpecies(Species species) {
            this.species = species;
            return this;
        }

        public Builder setAngerableSpecies(EntityType<? extends IAngerable> entityType) {
            this.species = new AngerableSpecies(entityType);
            return this;
        }

        public Builder setMobSpecies(EntityType<? extends MobEntity> entityType) {
            this.species = new MobSpecies(entityType);
            return this;
        }

        public Builder setRallyRadius(double range) {
            this.rallyRadius = range;
            return this;
        }

        public SpeciesProperty build() {
            if (species == null) throw new IllegalStateException("Species cannot be null");
            return new SpeciesProperty(name, species, rallyRadius);
        }
    }


    public interface Species {

        boolean includes(Entity entity);
        void setAttackTarget(Entity entity, LivingEntity target);
        void deaggro(LivingEntity entity);

    }

    public static class AngerableSpecies implements Species {
        private final EntityType<? extends IAngerable> type;

        public AngerableSpecies(EntityType<? extends IAngerable> type) {
            this.type = type;
        }

        @Override
        public boolean includes(Entity entity) {
            return entity.getType() == type;
        }

        @Override
        public void setAttackTarget(Entity entity, LivingEntity target) {
            if (entity instanceof IAngerable) {
                ((IAngerable) entity).setAttackTarget(target);
            }
        }

        @Override
        public void deaggro(LivingEntity entity) {
            if (entity instanceof IAngerable) {
                LivingEntity attackTarget = ((IAngerable) entity).getAttackTarget();
                if (attackTarget != null) {
                    int aggroId = attackTarget.getEntityId();
                    CompoundNBT data = entity.getPersistentData();
                    TagUtils.appendOrCreate(data, NO_AGGRO_LIST_KEY, aggroId);
                }
            }
        }
    }

    public static class MobSpecies implements Species {
        private final EntityType<? extends MobEntity> type;

        public MobSpecies(EntityType<? extends MobEntity> type) {
            this.type = type;
        }

        @Override
        public boolean includes(Entity entity) {
            return entity.getType() == type;
        }

        @Override
        public void setAttackTarget(Entity entity, LivingEntity target) {
            if (entity instanceof MobEntity) {
                ((MobEntity) entity).setAttackTarget(target);
            }
        }

        @Override
        public void deaggro(LivingEntity entity) {
            if (entity instanceof MobEntity) {
                LivingEntity attackTarget = ((MobEntity) entity).getAttackTarget();
                if (attackTarget != null) {
                    int aggroId = attackTarget.getEntityId();
                    CompoundNBT data = entity.getPersistentData();
                    TagUtils.appendOrCreate(data, NO_AGGRO_LIST_KEY, aggroId);
                }
            }
        }
    }

}
