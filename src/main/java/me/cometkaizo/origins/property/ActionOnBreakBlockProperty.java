package me.cometkaizo.origins.property;

import me.cometkaizo.origins.origin.Origin;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraftforge.event.world.BlockEvent;
import net.minecraftforge.eventbus.api.Event;

import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;

public class ActionOnBreakBlockProperty extends EventInterceptProperty {
    protected ActionOnBreakBlockProperty(String name, BiConsumerMap<Event, Origin> map) {
        super(name, map);
    }

    public static class Builder {
        protected String name = "Action On Break Block";
        protected final BiConsumerMap<Event, Origin> map = new BiConsumerMap<>();

        public Builder setName(String name) {
            this.name = name;
            return this;
        }

        public Builder withActionForBlock(Class<? extends Block> type, Consumer<BlockState> task) {
            return withActionForBlock(type, (blockState, origin) -> task.accept(blockState));
        }

        public Builder withActionForBlock(Class<? extends Block> type, BiConsumer<BlockState, Origin> task) {
            return withActionForEvent(type, (event, origin) -> task.accept(event.getState(), origin));
        }

        public Builder withPlayerSensitiveActionForBlock(Class<? extends Block> type, Consumer<BlockState> task) {
            return withPlayerSensitiveActionForBlock(type, (blockState, origin) -> task.accept(blockState));
        }

        public Builder withPlayerSensitiveActionForBlock(Class<? extends Block> type, BiConsumer<BlockState, Origin> task) {
            return withPlayerSensitiveAction(type, (event, origin) -> task.accept(event.getState(), origin));
        }

        public Builder withActionForExp(Class<? extends Block> type, Function<Integer, Integer> task) {
            return withActionForExp(type, (exp, origin) -> task.apply(exp));
        }

        public Builder withActionForExp(Class<? extends Block> type, BiFunction<Integer, Origin, Integer> task) {
            return withActionForEvent(type, (event, origin) -> event.setExpToDrop(task.apply(event.getExpToDrop(), origin)));
        }

        public Builder withPlayerSensitiveActionForExp(Class<? extends Block> type, Function<Integer, Integer> task) {
            return withPlayerSensitiveActionForExp(type, (exp, origin) -> task.apply(exp));
        }

        public Builder withPlayerSensitiveActionForExp(Class<? extends Block> type, BiFunction<Integer, Origin, Integer> task) {
            return withPlayerSensitiveAction(type, (event, origin) -> event.setExpToDrop(task.apply(event.getExpToDrop(), origin)));
        }

        public Builder withPlayerSensitiveAction(Class<? extends Block> type, Consumer<BlockEvent.BreakEvent> task) {
            return withPlayerSensitiveAction(type, (event, origin) -> task.accept(event));
        }

        public Builder withPlayerSensitiveAction(Class<? extends Block> type, BiConsumer<BlockEvent.BreakEvent, Origin> task) {
            return withActionForEvent(type, (event, origin) -> {
                if (origin.getPlayer().equals(event.getPlayer())) task.accept(event, origin);
            });
        }

        public Builder withActionForEvent(Class<? extends Block> type, Consumer<BlockEvent.BreakEvent> task) {
            return withActionForEvent(type, (event, origin) -> task.accept(event));
        }

        public Builder withActionForEvent(Class<? extends Block> type, BiConsumer<BlockEvent.BreakEvent, Origin> task) {
            this.map.put(BlockEvent.BreakEvent.class, (event, origin) -> {
                if (event == null) return;
                if (event.isCanceled()) return;
                Block block = event.getState().getBlock();
                if (isInstanceOf(block, type))
                    task.accept(event, origin);
            });
            return this;
        }

        public ActionOnBreakBlockProperty build() {
            return new ActionOnBreakBlockProperty(name, map);
        }
    }
}
