package me.cometkaizo.origins.origin;

import me.cometkaizo.origins.common.OriginTags;
import me.cometkaizo.origins.event.PlayerItemReceivedEvent;
import me.cometkaizo.origins.origin.client.ClientSharkOriginType;
import me.cometkaizo.origins.util.TimeTracker;
import net.minecraft.entity.ai.attributes.AttributeModifier;
import net.minecraft.entity.ai.attributes.Attributes;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.inventory.EquipmentSlotType;
import net.minecraft.item.*;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.nbt.INBT;
import net.minecraft.nbt.ListNBT;
import net.minecraft.potion.*;
import net.minecraft.tags.FluidTags;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingEntityUseItemEvent;
import net.minecraftforge.eventbus.api.Event;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Iterator;
import java.util.UUID;

import static net.minecraftforge.fml.DistExecutor.unsafeRunWhenOn;

public class SharkOriginType extends AbstractOriginType {
    public static final Logger LOGGER = LogManager.getLogger();

    public static final double ITEM_PULL_AMP = 0.07;
    public static final int WATER_BOTTLE_WATER_BREATHING_DUR = 5 * 20;
    public static final int SEAFOOD_HEALING_AMP = 3;
    private static final UUID TRIDENT_MODIFIER_UUID = UUID.fromString("728547c8-936c-4042-b656-08545878d49f");
    public static final int TRIDENT_SPEED_MODIFIER = 2;
    public static final int TRIDENT_DAMAGE_MODIFIER = 2;

    public SharkOriginType() {
        super(Items.COD, type -> new Origin.Description(type,
                new Origin.Description.Entry(type, "gills"),
                new Origin.Description.Entry(type, "mobility"),
                new Origin.Description.Entry(type, "riptide"),
                new Origin.Description.Entry(type, "trident"),
                new Origin.Description.Entry(type, "water_vision")
        ));
        MinecraftForge.EVENT_BUS.register(this);
    }

    public enum Cooldown implements TimeTracker.Timer {
        RIPTIDE_BOOST(1.25 * 20);
        public final int duration;

        Cooldown(double duration) {
            this.duration = (int) duration;
        }

        @Override
        public int getDuration() {
            return duration;
        }
    }

    public enum Property {
        WATER_BREATHING_PROPERTY,
        AQUA_AFFINITY,
        PULL_DROPPED_ITEMS_UNDERWATER
    }

    @Override
    public boolean hasMixinProperty(Object property, Origin origin) {
        return property == Property.WATER_BREATHING_PROPERTY ||
                property == Property.AQUA_AFFINITY ||
                property == Property.PULL_DROPPED_ITEMS_UNDERWATER;
    }

    @Override
    public void onFirstActivate(Origin origin) {
        unsafeRunWhenOn(Dist.CLIENT, () -> () -> ClientSharkOriginType.onFirstActivate(origin));
    }

    @Override
    public void onActivate(Origin origin) {
        super.onActivate(origin);
        origin.getPlayer().inventory.mainInventory.forEach(SharkOriginType::tryAddTridentModifiers);
        origin.getPlayer().inventory.offHandInventory.forEach(SharkOriginType::tryAddTridentModifiers);
        unsafeRunWhenOn(Dist.CLIENT, () -> () -> ClientSharkOriginType.onActivate(origin));
    }

    @Override
    public void onDeactivate(Origin origin) {
        super.onDeactivate(origin);
        origin.getPlayer().inventory.mainInventory.forEach(SharkOriginType::tryRemoveTridentModifiers);
        origin.getPlayer().inventory.offHandInventory.forEach(SharkOriginType::tryRemoveTridentModifiers);
        unsafeRunWhenOn(Dist.CLIENT, () -> () -> ClientSharkOriginType.onDeactivate(origin));
    }

    @Override
    public void onEvent(Object event, Origin origin) {
        super.onEvent(event, origin);
        if (event instanceof LivingEntityUseItemEvent.Finish) {
            onFinishUseItem((LivingEntityUseItemEvent.Finish) event, origin);
        }

        unsafeRunWhenOn(Dist.CLIENT, () -> () -> ClientSharkOriginType.onEvent(event, origin));
    }

    @Override
    public void onPlayerSensitiveEvent(Object event, Origin origin) {
        super.onPlayerSensitiveEvent(event, origin);
        if (event instanceof Event && ((Event) event).isCanceled()) return;
        if (event instanceof TickEvent.PlayerTickEvent) {
            onPlayerTick((TickEvent.PlayerTickEvent) event, origin);
        }
    }

    public void onFinishUseItem(LivingEntityUseItemEvent.Finish event, Origin origin) {
        if (event.isCanceled()) return;
        if (!origin.getPlayer().equals(event.getEntity())) return;
        PlayerEntity player = origin.getPlayer();

        if (isWaterPotion(event.getItem())) {
            applyWaterBottleEffects(player);
        } else if (isSeafood(event.getItem())) {
            if (!event.getItem().isFood()) return;
            Food consumedFood = event.getItem().getItem().getFood();
            if (consumedFood == null) return;

            for (int index = 0; index < SEAFOOD_HEALING_AMP - 1; index ++)
                player.getFoodStats().addStats(consumedFood.getHealing(), consumedFood.getSaturation());
        }
    }

    private static void applyWaterBottleEffects(PlayerEntity player) {
        player.setAir(player.getMaxAir());
        player.addPotionEffect(new EffectInstance(Effects.WATER_BREATHING, WATER_BOTTLE_WATER_BREATHING_DUR));
    }

    private static boolean isWaterPotion(ItemStack itemStack) {
        if (!(itemStack.getItem() instanceof PotionItem)) return false;
        Potion potion = PotionUtils.getPotionFromItem(itemStack);
        return potion == Potions.WATER ||
                potion == Potions.AWKWARD ||
                potion == Potions.MUNDANE ||
                potion == Potions.THICK;
    }

    private static boolean isSeafood(ItemStack itemStack) {
        Item item = itemStack.getItem();
        return item.isIn(OriginTags.Items.SEAFOOD);
    }

    public void onPlayerTick(TickEvent.PlayerTickEvent event, Origin origin) {
        if (event.isCanceled()) return;
        if (event.phase == TickEvent.Phase.START) return;
        PlayerEntity player = origin.getPlayer();

        if (player.isElytraFlying() && player.areEyesInFluid(FluidTags.WATER)) {
            setSwimming(player);
        }
    }

    public static void setSwimming(PlayerEntity player) {
        player.stopFallFlying();
        player.setSwimming(true);
        player.setSprinting(true);
        player.updateSwimming();
    }

    @SubscribeEvent
    public void onPlayerReceiveItem(PlayerItemReceivedEvent event) {
        ItemStack stack = event.getItemStack();
        if (stack != null && stack.getItem() == Items.TRIDENT) {
            Origin origin = Origin.getOrigin(event.getPlayer());
            if (origin != null) {
                if (origin.getType() instanceof SharkOriginType) {
                    tryAddTridentModifiers(stack);
                } else {
                    tryRemoveTridentModifiers(stack);
                }
            }
        }
    }

    protected static void tryAddTridentModifiers(ItemStack stack) {
        if (stack.getItem() != Items.TRIDENT) return;
        stack.addAttributeModifier(Attributes.ATTACK_SPEED,
                new AttributeModifier(TRIDENT_MODIFIER_UUID, "Trident Speed Boost", TRIDENT_SPEED_MODIFIER, AttributeModifier.Operation.ADDITION),
                EquipmentSlotType.MAINHAND);
        stack.addAttributeModifier(Attributes.ATTACK_DAMAGE,
                new AttributeModifier(TRIDENT_MODIFIER_UUID, "Trident Damage Boost", TRIDENT_DAMAGE_MODIFIER, AttributeModifier.Operation.ADDITION),
                EquipmentSlotType.MAINHAND);
    }

    protected static void tryRemoveTridentModifiers(ItemStack stack) {
        if (stack.getItem() == Items.TRIDENT && stack.hasTag()) {
            CompoundNBT tag = stack.getTag();
            assert tag != null;
            if (tag.contains("AttributeModifiers", 9)) {
                ListNBT modifiers = tag.getList("AttributeModifiers", 10);
                for (Iterator<INBT> iterator = modifiers.iterator(); iterator.hasNext(); ) {
                    INBT nbt = iterator.next();
                    CompoundNBT modifier = (CompoundNBT) nbt;
                    if (modifier.getUniqueId("UUID").equals(TRIDENT_MODIFIER_UUID))
                        iterator.remove();
                }
                if (modifiers.isEmpty()) tag.remove("AttributeModifiers");
            }
        }
    }

}
