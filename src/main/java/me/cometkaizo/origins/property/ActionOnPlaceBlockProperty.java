package me.cometkaizo.origins.property;

import me.cometkaizo.origins.origin.Origin;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraftforge.event.world.BlockEvent;
import net.minecraftforge.eventbus.api.Event;

import java.util.function.BiConsumer;
import java.util.function.Consumer;

public class ActionOnPlaceBlockProperty extends EventInterceptProperty {
    protected ActionOnPlaceBlockProperty(String name, BiConsumerMap<Event, Origin> map) {
        super(name, map);
    }

    public static class Builder {
        protected String name = "Action On Place Block";
        protected final BiConsumerMap<Event, Origin> map = new BiConsumerMap<>();

        public Builder setName(String name) {
            this.name = name;
            return this;
        }

        public Builder withActionForBlock(Class<? extends Block> type, Consumer<BlockState> task) {
            return withActionForBlock(type, (blockState, origin) -> task.accept(blockState));
        }

        public Builder withActionForBlock(Class<? extends Block> type, BiConsumer<BlockState, Origin> task) {
            return withPlayerSensitiveAction(type, (event, origin) -> task.accept(event.getState(), origin));
        }

        public Builder withActionForPlacedAgainst(Class<? extends Block> type, Consumer<BlockState> task) {
            return withActionForPlacedAgainst(type, (blockState, origin) -> task.accept(blockState));
        }

        public Builder withActionForPlacedAgainst(Class<? extends Block> type, BiConsumer<BlockState, Origin> task) {
            return withPlayerSensitiveAction(type, (event, origin) -> task.accept(event.getPlacedAgainst(), origin));
        }

        public Builder withPlayerSensitiveAction(Class<? extends Block> type, Consumer<BlockEvent.EntityPlaceEvent> task) {
            return withPlayerSensitiveAction(type, (event, origin) -> task.accept(event));
        }

        public Builder withPlayerSensitiveAction(Class<? extends Block> type, BiConsumer<BlockEvent.EntityPlaceEvent, Origin> task) {
            return withActionForEvent(type, (event, origin) -> {
                if (origin.getPlayer().equals(event.getEntity())) task.accept(event, origin);
            });
        }

        public Builder withActionForEvent(Class<? extends Block> type, Consumer<BlockEvent.EntityPlaceEvent> task) {
            return withActionForEvent(type, (event, origin) -> task.accept(event));
        }

        public Builder withActionForEvent(Class<? extends Block> type, BiConsumer<BlockEvent.EntityPlaceEvent, Origin> task) {
            this.map.put(BlockEvent.EntityPlaceEvent.class, (event, origin) -> {
                if (event == null) return;
                if (event.isCanceled()) return;
                Block block = event.getState().getBlock();
                if (isInstanceOf(block, type))
                    task.accept(event, origin);
            });
            return this;
        }

        public ActionOnPlaceBlockProperty build() {
            return new ActionOnPlaceBlockProperty(name, map);
        }
    }
}
