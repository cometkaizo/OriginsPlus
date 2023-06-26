package me.cometkaizo.origins.origin;

import me.cometkaizo.origins.common.OriginDamageSources;
import me.cometkaizo.origins.origin.client.ClientEnderianOriginType;
import me.cometkaizo.origins.property.SpeciesProperty;
import me.cometkaizo.origins.util.AttributeUtils;
import me.cometkaizo.origins.util.SoundUtils;
import me.cometkaizo.origins.util.TagUtils;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.enchantment.Enchantments;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.item.EnderPearlEntity;
import net.minecraft.entity.monster.EndermanEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.projectile.PotionEntity;
import net.minecraft.inventory.EquipmentSlotType;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.potion.EffectInstance;
import net.minecraft.potion.Potion;
import net.minecraft.potion.PotionUtils;
import net.minecraft.potion.Potions;
import net.minecraft.stats.Stats;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.SoundEvents;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.world.World;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.common.ForgeMod;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.ProjectileImpactEvent;
import net.minecraftforge.event.entity.living.*;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.event.world.BlockEvent;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.List;

import static net.minecraftforge.fml.DistExecutor.unsafeRunWhenOn;

public class EnderianOriginType extends AbstractOriginType {

    public static final Logger LOGGER = LogManager.getLogger();
    public static final float REACH = 8;
    public static final int PUMPKIN_SCARE_DAMAGE = 7;
    public static final int JACK_O_LANTERN_SCARE_DAMAGE = 17;
    public static final int WATER_DAMAGE = 3;
    public static final float ENDERMAN_RALLY_RANGE = 35;
    public static final String ENDERMAN_NO_AGGRO_LIST_KEY = EnderianOriginType.class.getName() + "_enderman_no_aggro";
    public static final SpeciesProperty ENDERMAN_SPECIES = new SpeciesProperty.Builder()
            .setAngerableSpecies(EntityType.ENDERMAN)
            .setRallyRadius(ENDERMAN_RALLY_RANGE).build();
    public static final int ENDER_PEARL_COOLDOWN_TIME = 20;

    public static boolean hasPearlCooldown(Origin origin) {
        return origin.getPlayer().getCooldownTracker().hasCooldown(Items.ENDER_PEARL);
    }

    @Override
    public boolean hasMixinProperty(Object property, Origin origin) {
        return property == Property.SILK_TOUCH || property == Property.EXTRA_ENTITY_REACH;
    }

    public enum Action {
        PUMPKIN_SCARE,
        JACK_O_LANTERN_SCARE
    }

    public enum Property {
        SILK_TOUCH,
        EXTRA_ENTITY_REACH
    }

    public EnderianOriginType() {
        super("Enderian", Items.ENDER_PEARL,
                type -> new Origin.Description(type,
                        new Origin.Description.Entry(type, "teleportation"),
                        new Origin.Description.Entry(type, "reach"),
                        new Origin.Description.Entry(type, "hydrophobia"),
                        new Origin.Description.Entry(type, "species"),
                        new Origin.Description.Entry(type, "pumpkin")
                ),
                ENDERMAN_SPECIES
        );
    }

    public static void throwEnderPearl(Origin origin) {
        PlayerEntity player = origin.getPlayer();
        World world = player.world;

        SoundUtils.playSound(player, SoundEvents.ENTITY_ENDER_PEARL_THROW, SoundCategory.NEUTRAL, 0.5F, 0.4F / (random.nextFloat() * 0.4F + 0.8F));
        player.getCooldownTracker().setCooldown(Items.ENDER_PEARL, ENDER_PEARL_COOLDOWN_TIME);

        EnderPearlEntity pearl = new EnderPearlEntity(world, player);
        pearl.setItem(Items.ENDER_PEARL.getDefaultInstance());
        pearl.setDirectionAndMovement(player, player.rotationPitch, player.rotationYaw, 0.0F, 1.5F, 1.0F);
        world.addEntity(pearl);

        player.addStat(Stats.ITEM_USED.get(Items.ENDER_PEARL));
    }
//
    @Override
    public void onEvent(Object event, Origin origin) {
        super.onEvent(event, origin);
        if (event instanceof PlayerInteractEvent.RightClickItem) {
            onItemClick((PlayerInteractEvent.RightClickItem) event, origin);
        } else if (event instanceof EntityTeleportEvent.EnderPearl) {
            onPearlLand((EntityTeleportEvent.EnderPearl) event, origin);
        } else if (event instanceof TickEvent.PlayerTickEvent) {
            onPlayerTick((TickEvent.PlayerTickEvent) event, origin);
        }/* else if (event instanceof LivingSetAttackTargetEvent) {
            onAggro((LivingSetAttackTargetEvent) event, origin);
        }*/ else if (event instanceof LivingEquipmentChangeEvent) {
            onEquipmentChange((LivingEquipmentChangeEvent) event, origin);
        } else if (event instanceof ProjectileImpactEvent.Throwable) {
            onProjectileImpact((ProjectileImpactEvent.Throwable) event, origin);
        } else if (event instanceof LivingEntityUseItemEvent) {
            onUseItem((LivingEntityUseItemEvent) event, origin);
        } else if (event instanceof BlockEvent.BreakEvent) {
            onBlockBreak((BlockEvent.BreakEvent) event, origin);
        }/* else if (event instanceof LivingHurtEvent) {
            onLivingHurt((LivingHurtEvent) event, origin);
        }*/ else if (event == Action.PUMPKIN_SCARE) {
            pumpkinScare(origin);
        } else if (event == Action.JACK_O_LANTERN_SCARE) {
            jackOLanternScare(origin);
        }

        unsafeRunWhenOn(Dist.CLIENT, () -> () -> ClientEnderianOriginType.onEvent(event, origin));
    }

    private static void jackOLanternScare(Origin origin) {
        boolean damaged = origin.getPlayer().attackEntityFrom(OriginDamageSources.SCARE, JACK_O_LANTERN_SCARE_DAMAGE / 2F);
        if (damaged) SoundUtils.playSound(origin.getPlayer(), SoundEvents.ENTITY_ENDERMAN_HURT, SoundCategory.HOSTILE, 1, 1);
    }

    private static void pumpkinScare(Origin origin) {
        boolean damaged = origin.getPlayer().attackEntityFrom(OriginDamageSources.SCARE, PUMPKIN_SCARE_DAMAGE / 2F);
        if (damaged) SoundUtils.playSound(origin.getPlayer(), SoundEvents.ENTITY_ENDERMAN_HURT, SoundCategory.HOSTILE, 0.7F, 1);
    }

    @Override
    public void performAction(Origin origin) {
        if (hasPearlCooldown(origin)) return;
        throwEnderPearl(origin);
    }

    @Override
    public void onActivate(Origin origin) {
        PlayerEntity player = origin.getPlayer();
        AttributeUtils.setAttribute(player, ForgeMod.REACH_DISTANCE.get(), REACH);
        unsafeRunWhenOn(Dist.CLIENT, () -> () -> ClientEnderianOriginType.onActivate(origin));
    }

    @Override
    public void onDeactivate(Origin origin) {
        PlayerEntity player = origin.getPlayer();
        AttributeUtils.setAttribute(player, ForgeMod.REACH_DISTANCE.get(), ForgeMod.REACH_DISTANCE.get().getDefaultValue());
        unsafeRunWhenOn(Dist.CLIENT, () -> () -> ClientEnderianOriginType.onDeactivate(origin));
    }

    public void onItemClick(PlayerInteractEvent.RightClickItem event, Origin origin) {
        if (event.isCanceled()) return;
        if (event.getSide().isClient()) return;
        if (!origin.isServerSide()) return;
        if (!origin.getPlayer().equals(event.getPlayer())) return;
        if (event.getItemStack().getItem() != Items.ENDER_PEARL) return;
        if (!origin.getPlayer().abilities.isCreativeMode)
            event.getItemStack().grow(1);
    }

    public void onPearlLand(EntityTeleportEvent.EnderPearl event, Origin origin) {
        if (event.isCanceled()) return;
        if (!origin.getPlayer().equals(event.getPlayer())) return;

        event.setAttackDamage(0);

        PlayerEntity player = origin.getPlayer();
        SoundUtils.playSound(player, SoundEvents.ENTITY_ENDERMAN_TELEPORT, SoundCategory.PLAYERS, 0.7F, 1);
    }

    public void onPlayerTick(TickEvent.PlayerTickEvent event, Origin origin) {
        if (event.isCanceled()) return;
        if (event.side.isClient()) return;
        if (!origin.getPlayer().equals(event.player)) return;
        PlayerEntity player = origin.getPlayer();

        if (player.isInWaterRainOrBubbleColumn()) {
            applyWaterDamage(player);
        } else if (player.getItemStackFromSlot(EquipmentSlotType.HEAD).getItem() == Items.CARVED_PUMPKIN) {
            applyPumpkinDamage(player);
        } else if (player.getItemStackFromSlot(EquipmentSlotType.HEAD).getItem() == Items.JACK_O_LANTERN) {
            applyJackOLanternDamage(player);
        }
    }

    private static void applyJackOLanternDamage(PlayerEntity player) {
        boolean damaged = player.attackEntityFrom(OriginDamageSources.SCARE, JACK_O_LANTERN_SCARE_DAMAGE);
        if (damaged) SoundUtils.playSound(player, SoundEvents.ENTITY_ENDERMAN_HURT, SoundCategory.HOSTILE, 1, 1);
    }

    private static void applyPumpkinDamage(PlayerEntity player) {
        boolean damaged = player.attackEntityFrom(OriginDamageSources.SCARE, PUMPKIN_SCARE_DAMAGE);
        if (damaged) SoundUtils.playSound(player, SoundEvents.ENTITY_ENDERMAN_HURT, SoundCategory.HOSTILE, 0.7F, 1);
    }

    private static void applyWaterDamage(PlayerEntity player) {
        boolean damaged = player.attackEntityFrom(OriginDamageSources.TOUCH_WATER, WATER_DAMAGE);
        if (damaged) SoundUtils.playSound(player, SoundEvents.ENTITY_ENDERMAN_HURT, SoundCategory.HOSTILE, 0.7F, 1);
    }

    public void onAggro(LivingSetAttackTargetEvent event, Origin origin) {
        if (event.isCanceled()) return;
        if (!(event.getEntity() instanceof EndermanEntity)) return;

        EndermanEntity enderman = (EndermanEntity) event.getEntity();
        LivingEntity target = event.getTarget();
        if (origin.getPlayer().equals(target) || isOnNoAggroList(enderman, target)) {
            enderman.setAttackTarget(null);
        }
    }

    private boolean isOnNoAggroList(EndermanEntity enderman, LivingEntity target) {
        if (target == null) return false;
        CompoundNBT data = enderman.getPersistentData();
        if (!data.contains(ENDERMAN_NO_AGGRO_LIST_KEY)) return false;

        int targetId = target.getEntityId();
        for (int id : data.getIntArray(ENDERMAN_NO_AGGRO_LIST_KEY)) {
            if (id == targetId) return true;
        }
        return false;
    }

    public void onEquipmentChange(LivingEquipmentChangeEvent event, Origin origin) {
        if (event.isCanceled()) return;
        if (!origin.getPlayer().equals(event.getEntity())) return;
        if (event.getSlot() != EquipmentSlotType.HEAD) return;

        Item itemAfter = event.getTo().getItem();
        if (itemAfter == Items.CARVED_PUMPKIN || itemAfter == Items.JACK_O_LANTERN) {
            PlayerEntity player = origin.getPlayer();
            boolean damaged = player.attackEntityFrom(OriginDamageSources.SCARE, player.getMaxHealth() * 1.5F);
            if (damaged) SoundUtils.playSound(player, SoundEvents.ENTITY_ENDERMAN_SCREAM, SoundCategory.PLAYERS, 1, 1);
        }
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

    public void onUseItem(LivingEntityUseItemEvent event, Origin origin) {
        if (event.isCanceled()) return;
        if (!origin.getPlayer().equals(event.getEntity())) return;
        if (!isWaterPotion(event.getItem()) || hasEffects(event.getItem())) return;

        PlayerEntity player = origin.getPlayer();

        boolean damaged = player.attackEntityFrom(OriginDamageSources.DRINK_WATER, 10);
        if (damaged) SoundUtils.playSound(player, SoundEvents.ENTITY_ENDERMAN_HURT, SoundCategory.HOSTILE);
    }

    public void onBlockBreak(BlockEvent.BreakEvent event, Origin origin) {
        if (event.isCanceled()) return;
        if (!origin.getPlayer().equals(event.getPlayer())) return;

        PlayerEntity player = origin.getPlayer();
        ItemStack tool = player.getHeldItemMainhand();
        if (EnchantmentHelper.getEnchantmentLevel(Enchantments.FORTUNE, tool) == 0)
            event.setExpToDrop(0);
    }

    public void onLivingHurt(LivingHurtEvent event, Origin origin) {
        if (event.isCanceled()) return;
        if (!origin.isServerSide()) return;
        if (!origin.getPlayer().equals(event.getSource().getTrueSource())) return;

        LivingEntity target = event.getEntityLiving();
        if (target instanceof EndermanEntity) {
            EndermanEntity enderman = (EndermanEntity) target;
            LivingEntity attackTarget = enderman.getAttackTarget();
            addToNoAggroList(enderman, attackTarget);
        } else {
            PlayerEntity player = origin.getPlayer();
            aggroNearbyEndermen(player, target);
        }
    }

    private static void addToNoAggroList(EndermanEntity enderman, LivingEntity attackTarget) {
        if (attackTarget != null) {
            int aggroId = attackTarget.getEntityId();
            CompoundNBT data = enderman.getPersistentData();
            TagUtils.appendOrCreate(data, ENDERMAN_NO_AGGRO_LIST_KEY, aggroId);
        }
    }

    private static void aggroNearbyEndermen(PlayerEntity player, LivingEntity target) {
        World world = player.world;

        AxisAlignedBB affectedArea = player.getBoundingBox().grow(ENDERMAN_RALLY_RANGE);
        List<EndermanEntity> affectedEndermen = world.getEntitiesWithinAABB(EndermanEntity.class, affectedArea);

        for (EndermanEntity enderman : affectedEndermen) {
            enderman.setAttackTarget(target);
        }
    }
//
}
