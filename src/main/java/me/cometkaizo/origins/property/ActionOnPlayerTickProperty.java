package me.cometkaizo.origins.property;

import me.cometkaizo.origins.origin.Origin;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.Event;

import java.util.function.BiConsumer;
import java.util.function.Consumer;

public class ActionOnPlayerTickProperty extends EventInterceptProperty {
    protected ActionOnPlayerTickProperty(String name, BiConsumerMap<Event, Origin> map) {
        super(name, map);
    }

    public static class Builder {
        protected String name = "Action On Player Tick";
        protected final BiConsumerMap<Event, Origin> map = new BiConsumerMap<>();

        public Builder setName(String name) {
            this.name = name;
            return this;
        }



        public Builder withPlayerSensitiveAction(Consumer<TickEvent.PlayerTickEvent> task) {
            return withPlayerSensitiveAction((event, origin) -> task.accept(event));
        }

        public Builder withPlayerSensitiveAction(BiConsumer<TickEvent.PlayerTickEvent, Origin> task) {
            return withActionForEvent((event, origin) -> {
                if (origin.getPlayer().equals(event.player)) task.accept(event, origin);
            });
        }

        public Builder withActionForEvent(Consumer<TickEvent.PlayerTickEvent> task) {
            return withActionForEvent((event, origin) -> task.accept(event));
        }

        public Builder withActionForEvent(BiConsumer<TickEvent.PlayerTickEvent, Origin> task) {
            this.map.put(TickEvent.PlayerTickEvent.class, (event, origin) -> {
                if (event == null) return;
                if (event.isCanceled()) return;
                task.accept(event, origin);
            });
            return this;
        }

        public ActionOnPlayerTickProperty build() {
            return new ActionOnPlayerTickProperty(name, map);
        }
    }
}
