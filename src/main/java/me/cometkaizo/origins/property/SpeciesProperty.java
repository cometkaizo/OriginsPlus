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
    private final Rallier rallier;
    private final double rallyRadius;
    private final double rallyRadiusSqr;

    protected SpeciesProperty(String name, @Nonnull Rallier rallier, double rallyRadius) {
        super(name);
        this.rallier = rallier;
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
        LivingEntity target = event.getTarget();

        if (origin.getPlayer().equals(target) || isOnNoAggroList(entity, target)) {
            rallier.setAttackTarget(entity, null);
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

        if (rallier.isSameSpecies(target)) {
            rallier.addToNoAggroList(target);
        } else {
            aggroNearbyEntities(origin.getPlayer(), target);
        }
    }

    private void aggroNearbyEntities(PlayerEntity player, LivingEntity target) {
        World world = player.world;

        AxisAlignedBB affectedArea = player.getBoundingBox().grow(rallyRadius);
        List<Entity> ralliedEntities = world.getEntitiesWithinAABB(Entity.class, affectedArea, rallier::isSameSpecies);

        for (Entity entity : ralliedEntities) {
            if (entity.getDistanceSq(player) > rallyRadiusSqr) continue;
            rallier.setAttackTarget(entity, target);
        }
    }

    public static class Builder {
        private String name = "Species";
        private Rallier rallier;
        private double rallyRadius = 0;

        public Builder(Rallier rallier) {
            setRallier(rallier);
        }

        public Builder() {

        }

        public Builder setName(String name) {
            this.name = name;
            return this;
        }

        public Builder setRallier(Rallier rallier) {
            this.rallier = rallier;
            return this;
        }

        public Builder setAngerableSpecies(EntityType<? extends IAngerable> entityType) {
            this.rallier = new AngerableRallier(entityType);
            return this;
        }

        public Builder setMobSpecies(EntityType<? extends MobEntity> entityType) {
            this.rallier = new MobRallier(entityType);
            return this;
        }

        public Builder setRallyRadius(double range) {
            this.rallyRadius = range;
            return this;
        }

        public SpeciesProperty build() {
            return new SpeciesProperty(name, rallier, rallyRadius);
        }
    }


    public interface Rallier {

        boolean isSameSpecies(Entity entity);
        void setAttackTarget(Entity entity, LivingEntity target);
        void addToNoAggroList(LivingEntity entity);

    }

    public static class AngerableRallier implements Rallier {
        private final EntityType<? extends IAngerable> type;

        public AngerableRallier(EntityType<? extends IAngerable> type) {
            this.type = type;
        }

        @Override
        public boolean isSameSpecies(Entity rallier) {
            return rallier.getType() == type;
        }

        @Override
        public void setAttackTarget(Entity entity, LivingEntity target) {
            if (entity instanceof IAngerable) {
                ((IAngerable) entity).setAttackTarget(target);
            }
        }

        @Override
        public void addToNoAggroList(LivingEntity entity) {
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

    public static class MobRallier implements Rallier {
        private final EntityType<? extends MobEntity> type;

        public MobRallier(EntityType<? extends MobEntity> type) {
            this.type = type;
        }

        @Override
        public boolean isSameSpecies(Entity rallier) {
            return rallier.getType() == type;
        }

        @Override
        public void setAttackTarget(Entity entity, LivingEntity target) {
            if (entity instanceof MobEntity) {
                ((MobEntity) entity).setAttackTarget(target);
            }
        }

        @Override
        public void addToNoAggroList(LivingEntity entity) {
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
