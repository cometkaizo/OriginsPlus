package me.cometkaizo.origins.property;

import me.cometkaizo.origins.origin.Origin;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.Event;

import java.util.function.BiConsumer;
import java.util.function.Consumer;

public class ActionOnItemRightClickProperty extends EventInterceptProperty {
    protected ActionOnItemRightClickProperty(String name, BiConsumerMap<Event, Origin> map) {
        super(name, map);
    }

    public static class Builder {
        protected String name = "Action On Item Right Click";
        protected final BiConsumerMap<Event, Origin> map = new BiConsumerMap<>();

        public Builder setName(String name) {
            this.name = name;
            return this;
        }

        public Builder withActionForItem(Class<? extends Item> type, Consumer<ItemStack> task) {
            return withActionForItem(type, (itemStack, origin) -> task.accept(itemStack));
        }

        public Builder withActionForItem(Class<? extends Item> type, BiConsumer<ItemStack, Origin> task) {
            return withActionForEvent(type, (event, origin) -> task.accept(event.getItemStack(), origin));
        }

        public Builder withPlayerSensitiveActionForItem(Class<? extends Item> type, Consumer<ItemStack> task) {
            return withPlayerSensitiveActionForItem(type, (itemStack, origin) -> task.accept(itemStack));
        }

        public Builder withPlayerSensitiveActionForItem(Class<? extends Item> type, BiConsumer<ItemStack, Origin> task) {
            return withPlayerSensitiveAction(type, (event, origin) -> task.accept(event.getItemStack(), origin));
        }

        public Builder withPlayerSensitiveAction(Class<? extends Item> type, Consumer<PlayerInteractEvent.RightClickItem> task) {
            return withPlayerSensitiveAction(type, (itemStack, origin) -> task.accept(itemStack));
        }

        public Builder withPlayerSensitiveAction(Class<? extends Item> type, BiConsumer<PlayerInteractEvent.RightClickItem, Origin> task) {
            return withActionForEvent(type, (event, origin) -> {
                if (areDifferentSides(event, origin)) return;
                if (origin.getPlayer().equals(event.getEntity())) task.accept(event, origin);
            });
        }

        public Builder withActionForEvent(Class<? extends Item> type, Consumer<PlayerInteractEvent.RightClickItem> task) {
            return withActionForEvent(type, (event, origin) -> task.accept(event));
        }

        public Builder withActionForEvent(Class<? extends Item> type, BiConsumer<PlayerInteractEvent.RightClickItem, Origin> task) {
            this.map.put(PlayerInteractEvent.RightClickItem.class, (event, origin) -> {
                if (event == null) return;
                if (event.isCanceled()) return;
                Item item = event.getItemStack().getItem();
                if (isInstanceOf(item, type))
                    task.accept(event, origin);
            });
            return this;
        }

        public Builder withActionForItem(Item item, Consumer<ItemStack> task) {
            return withActionForItem(item, (itemStack, origin) -> task.accept(itemStack));
        }

        public Builder withActionForItem(Item item, BiConsumer<ItemStack, Origin> task) {
            return withActionForEvent(item, (event, origin) -> task.accept(event.getItemStack(), origin));
        }

        public Builder withPlayerSensitiveActionForItem(Item item, Consumer<ItemStack> task) {
            return withPlayerSensitiveActionForItem(item, (itemStack, origin) -> task.accept(itemStack));
        }

        public Builder withPlayerSensitiveActionForItem(Item item, BiConsumer<ItemStack, Origin> task) {
            return withPlayerSensitiveAction(item, (event, origin) -> task.accept(event.getItemStack(), origin));
        }

        public Builder withPlayerSensitiveAction(Item item, Consumer<PlayerInteractEvent.RightClickItem> task) {
            return withPlayerSensitiveAction(item, (itemStack, origin) -> task.accept(itemStack));
        }

        public Builder withPlayerSensitiveAction(Item item, BiConsumer<PlayerInteractEvent.RightClickItem, Origin> task) {
            return withActionForEvent(item, (event, origin) -> {
                if (areDifferentSides(event, origin)) return;
                if (origin.getPlayer().equals(event.getEntity())) task.accept(event, origin);
            });
        }

        private static boolean areDifferentSides(PlayerInteractEvent event, Origin origin) {
            return origin.isServerSide() != event.getSide().isServer();
        }

        public Builder withActionForEvent(Item item, Consumer<PlayerInteractEvent.RightClickItem> task) {
            return withActionForEvent(item, (event, origin) -> task.accept(event));
        }

        public Builder withActionForEvent(Item requiredItem, BiConsumer<PlayerInteractEvent.RightClickItem, Origin> task) {
            this.map.put(PlayerInteractEvent.RightClickItem.class, (event, origin) -> {
                if (event == null) return;
                if (event.isCanceled()) return;
                Item actualItem = event.getItemStack().getItem();
                if (actualItem == requiredItem)
                    task.accept(event, origin);
            });
            return this;
        }

        public ActionOnItemRightClickProperty build() {
            return new ActionOnItemRightClickProperty(name, map);
        }
    }
}
