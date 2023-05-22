package me.cometkaizo.origins.origin;

import me.cometkaizo.origins.origin.client.ClientSharkOriginType;
import me.cometkaizo.origins.util.TimeTracker;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.*;
import net.minecraft.potion.*;
import net.minecraft.tags.FluidTags;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.entity.living.LivingEntityUseItemEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import static net.minecraftforge.fml.DistExecutor.unsafeRunWhenOn;

public class SharkOriginType extends AbstractOriginType {
    public static final Logger LOGGER = LogManager.getLogger();

    public static final Object WATER_BREATHING_PROPERTY = new Object();
    public static final int WATER_BOTTLE_WATER_BREATHING_DUR = 5 * 20;

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

    @Override
    public void onEvent(Object event, Origin origin) {
        if (event instanceof LivingEntityUseItemEvent.Finish) {
            onFinishUseItem((LivingEntityUseItemEvent.Finish) event, origin);
        } else if (event instanceof PlayerEvent.BreakSpeed) {
            onBreakSpeed((PlayerEvent.BreakSpeed) event, origin);
        }

        unsafeRunWhenOn(Dist.CLIENT, () -> () -> ClientSharkOriginType.onEvent(event, origin));
    }

    @Override
    public boolean hasMixinProperty(Object property, Origin origin) {
        return property == WATER_BREATHING_PROPERTY;
    }

    @Override
    public void onFirstActivate(Origin origin) {
        unsafeRunWhenOn(Dist.CLIENT, () -> () -> ClientSharkOriginType.onFirstActivate(origin));
    }

    public void onBreakSpeed(PlayerEvent.BreakSpeed event, Origin origin) {
        if (event.isCanceled()) return;
        if (!origin.isServerSide()) return;
        if (!origin.getPlayer().equals(event.getEntity())) return;
        PlayerEntity player = origin.getPlayer();

        if (player.areEyesInFluid(FluidTags.WATER)) {
            event.setNewSpeed(event.getNewSpeed() * 5);
        } else {
            event.setNewSpeed(event.getNewSpeed() / 2);
        }
        if (player.isInWater() && !player.isOnGround()) {
            event.setNewSpeed(event.getNewSpeed() * 3);
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
        return item == Items.COD ||
                item == Items.SALMON ||
                item == Items.PUFFERFISH ||
                item == Items.TROPICAL_FISH ||
                item == Items.COOKED_COD ||
                item == Items.COOKED_SALMON ||
                item == Items.DRIED_KELP;
    }

}
