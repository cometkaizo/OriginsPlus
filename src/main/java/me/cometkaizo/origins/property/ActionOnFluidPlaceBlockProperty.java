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

public class ActionOnFluidPlaceBlockProperty extends EventInterceptProperty {
    protected ActionOnFluidPlaceBlockProperty(String name, BiConsumerMap<Event, Origin> map) {
        super(name, map);
    }

    public static class Builder {
        protected String name = "Action On Fluid Place Block";
        protected final BiConsumerMap<Event, Origin> map = new BiConsumerMap<>();

        public Builder setName(String name) {
            this.name = name;
            return this;
        }

        public Builder withActionForBlock(Class<? extends Block> type, Function<BlockState, BlockState> task) {
            return withActionForBlock(type, (blockState, origin) -> task.apply(blockState));
        }

        public Builder withActionForBlock(Class<? extends Block> type, BiFunction<BlockState, Origin, BlockState> task) {
            return withActionForEvent(type, (event, origin) -> event.setNewState(task.apply(event.getNewState(), origin)));
        }

        public Builder withActionForOldBlock(Class<? extends Block> type, Function<BlockState, BlockState> task) {
            return withActionForOldBlock(type, (blockState, origin) -> task.apply(blockState));
        }

        public Builder withActionForOldBlock(Class<? extends Block> type, BiFunction<BlockState, Origin, BlockState> task) {
            return withActionForEventOldBlock(type, (event, origin) -> event.setNewState(task.apply(event.getOriginalState(), origin)));
        }

        public Builder withActionForEventOldBlock(Class<? extends Block> type, Consumer<BlockEvent.FluidPlaceBlockEvent> task) {
            return withActionForEventOldBlock(type, (event, origin) -> task.accept(event));
        }

        public Builder withActionForEventOldBlock(Class<? extends Block> type, BiConsumer<BlockEvent.FluidPlaceBlockEvent, Origin> task) {
            this.map.put(BlockEvent.FluidPlaceBlockEvent.class, (event, origin) -> {
                if (event == null) return;
                if (event.isCanceled()) return;
                Block block = event.getOriginalState().getBlock();
                if (isInstanceOf(block, type))
                    task.accept(event, origin);
            });
            return this;
        }

        public Builder withActionForEvent(Class<? extends Block> type, Consumer<BlockEvent.FluidPlaceBlockEvent> task) {
            return withActionForEvent(type, (event, origin) -> task.accept(event));
        }

        public Builder withActionForEvent(Class<? extends Block> type, BiConsumer<BlockEvent.FluidPlaceBlockEvent, Origin> task) {
            this.map.put(BlockEvent.FluidPlaceBlockEvent.class, (event, origin) -> {
                if (event == null) return;
                if (event.isCanceled()) return;
                Block block = event.getNewState().getBlock();
                if (isInstanceOf(block, type))
                    task.accept(event, origin);
            });
            return this;
        }

        public ActionOnFluidPlaceBlockProperty build() {
            return new ActionOnFluidPlaceBlockProperty(name, map);
        }
    }
}
