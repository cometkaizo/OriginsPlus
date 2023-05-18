package me.cometkaizo.origins.property;

import me.cometkaizo.origins.origin.Origin;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraftforge.event.world.BlockEvent;
import net.minecraftforge.eventbus.api.Event;

import java.util.function.BiFunction;
import java.util.function.Function;

public class ActionOnPlaceFluidProperty extends EventInterceptProperty {
    protected ActionOnPlaceFluidProperty(String name, BiConsumerMap<Event, Origin> map) {
        super(name, map);
    }

    public static class Builder {
        protected String name = "Action On Place Fluid";
        protected final BiConsumerMap<Event, Origin> map = new BiConsumerMap<>();

        public Builder setName(String name) {
            this.name = name;
            return this;
        }

        public Builder withActionForBlock(Class<? extends Block> type, Function<BlockState, Event.Result> task) {
            return withActionForBlock(type, (blockState, origin) -> task.apply(blockState));
        }

        public Builder withActionForBlock(Class<? extends Block> type, BiFunction<BlockState, Origin, Event.Result> task) {
            return withActionForEvent(type, (event, origin) -> task.apply(event.getState(), origin));
        }

        public Builder withActionForEvent(Class<? extends Block> type, Function<BlockEvent.CreateFluidSourceEvent, Event.Result> task) {
            return withActionForEvent(type, (event, origin) -> task.apply(event));
        }

        public Builder withActionForEvent(Class<? extends Block> type, BiFunction<BlockEvent.CreateFluidSourceEvent, Origin, Event.Result> task) {
            this.map.put(BlockEvent.CreateFluidSourceEvent.class, (event, origin) -> {
                if (event == null) return;
                if (event.isCanceled()) return;
                Block block = event.getState().getBlock();
                if (isInstanceOf(block, type))
                    event.setResult(task.apply(event, origin));
            });
            return this;
        }

        public ActionOnPlaceFluidProperty build() {
            return new ActionOnPlaceFluidProperty(name, map);
        }
    }
}
