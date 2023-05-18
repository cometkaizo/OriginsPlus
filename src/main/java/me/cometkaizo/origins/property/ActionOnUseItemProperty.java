package me.cometkaizo.origins.property;

import me.cometkaizo.origins.origin.Origin;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraftforge.event.entity.living.LivingEntityUseItemEvent;
import net.minecraftforge.eventbus.api.Event;

import java.util.function.BiConsumer;
import java.util.function.Consumer;

public class ActionOnUseItemProperty extends EventInterceptProperty {
    protected ActionOnUseItemProperty(String name, BiConsumerMap<Event, Origin> map) {
        super(name, map);
    }

    public static class Builder {
        protected String name = "Action On Use Item";
        protected final BiConsumerMap<Event, Origin> map = new BiConsumerMap<>();

        public Builder setName(String name) {
            this.name = name;
            return this;
        }

        public Builder withActionForItem(Class<? extends Item> type, Consumer<ItemStack> task) {
            return withActionForItem(type, (itemStack, origin) -> task.accept(itemStack));
        }

        public Builder withActionForItem(Class<? extends Item> type, BiConsumer<ItemStack, Origin> task) {
            return withActionForEvent(type, (event, origin) -> task.accept(event.getItem(), origin));
        }

        public Builder withPlayerSensitiveActionForItem(Class<? extends Item> type, Consumer<ItemStack> task) {
            return withPlayerSensitiveActionForItem(type, (itemStack, origin) -> task.accept(itemStack));
        }

        public Builder withPlayerSensitiveActionForItem(Class<? extends Item> type, BiConsumer<ItemStack, Origin> task) {
            return withActionForEvent(type, (event, origin) -> {
                if (origin.getPlayer().equals(event.getEntity())) task.accept(event.getItem(), origin);
            });
        }

        public Builder withActionForEvent(Class<? extends Item> type, Consumer<LivingEntityUseItemEvent> task) {
            return withActionForEvent(type, (event, origin) -> task.accept(event));
        }

        public Builder withActionForEvent(Class<? extends Item> type, BiConsumer<LivingEntityUseItemEvent, Origin> task) {
            this.map.put(LivingEntityUseItemEvent.class, (event, origin) -> {
                if (event == null) return;
                if (event.isCanceled()) return;
                Item item = event.getItem().getItem();
                if (isInstanceOf(item, type))
                    task.accept(event, origin);
            });
            return this;
        }

        public Builder withActionForItem(Item item, Consumer<ItemStack> task) {
            return withActionForItem(item, (itemStack, origin) -> task.accept(itemStack));
        }

        public Builder withActionForItem(Item item, BiConsumer<ItemStack, Origin> task) {
            return withActionForEvent(item, (event, origin) -> task.accept(event.getItem(), origin));
        }

        public Builder withPlayerSensitiveActionForItem(Item item, Consumer<ItemStack> task) {
            return withPlayerSensitiveActionForItem(item, (itemStack, origin) -> task.accept(itemStack));
        }

        public Builder withPlayerSensitiveActionForItem(Item item, BiConsumer<ItemStack, Origin> task) {
            return withActionForEvent(item, (event, origin) -> {
                if (origin.getPlayer().equals(event.getEntity())) task.accept(event.getItem(), origin);
            });
        }

        public Builder withActionForEvent(Item item, Consumer<LivingEntityUseItemEvent> task) {
            return withActionForEvent(item, (event, origin) -> task.accept(event));
        }

        public Builder withActionForEvent(Item requiredItem, BiConsumer<LivingEntityUseItemEvent, Origin> task) {
            this.map.put(LivingEntityUseItemEvent.class, (event, origin) -> {
                if (event == null) return;
                if (event.isCanceled()) return;
                Item actualItem = event.getItem().getItem();
                if (actualItem == requiredItem)
                    task.accept(event, origin);
            });
            return this;
        }

        public ActionOnUseItemProperty build() {
            return new ActionOnUseItemProperty(name, map);
        }
    }
}
