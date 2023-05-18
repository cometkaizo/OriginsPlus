package me.cometkaizo.origins.property;

import me.cometkaizo.origins.Main;
import me.cometkaizo.origins.origin.Origin;
import me.cometkaizo.origins.util.SoundUtils;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.SoundEvent;
import net.minecraftforge.event.entity.EntityEvent;
import net.minecraftforge.eventbus.api.Event;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.function.*;

public class EventInterceptProperty extends AbstractProperty {
    protected BiConsumerMap<Event, Origin> map;
    protected final Supplier<BiConsumerMap<Event, Origin>> mapSup;

    protected EventInterceptProperty(String name, BiConsumerMap<Event, Origin> map) {
        super(name);
        this.mapSup = () -> map;
    }
    protected EventInterceptProperty(String name) {
        this(name, new BiConsumerMap<>());
    }


    protected static boolean isInstanceOf(Object object, Class<?> type) {
        if (object != null && type == null) throw new NullPointerException("Type cannot be null");
        return object == null || type.isAssignableFrom(object.getClass());
    }


    @Override
    public void onEvent(Object event, Origin origin) {
        if (event == null) return;
        if (map == null) init();

        for (Class<? extends Event> blockedEventType : map.keys()) {
            if (blockedEventType.isAssignableFrom(event.getClass())) {
                Event blockedEvent = (Event) event;

                BiConsumer<Event, Origin> task = map.get(blockedEventType);
                intercept(blockedEvent, origin, task);
            }
        }
    }

    protected void init() {
        map = mapSup.get();
    }

    private <T extends Event> void intercept(T event, Origin origin, BiConsumer<T, Origin> task) {
        task.accept(event, origin);
    }

    public static class Builder {
        protected String name = "Event Intercept";
        protected final BiConsumerMap<Event, Origin> map = new BiConsumerMap<>();

        public Builder setName(String name) {
            this.name = name;
            return this;
        }

        public <T extends Event> Builder withAction(Class<? extends T> type, Consumer<T> task) {
            this.map.put(type, event -> {
                if (!event.isCanceled()) task.accept(event);
            });
            return this;
        }

        public <T extends Event> Builder withAction(Class<? extends T> type, BiConsumer<T, Origin> task) {
            this.map.put(type, (event, origin) -> {
                if (!event.isCanceled()) task.accept(event, origin);
            });
            return this;
        }

        public <T extends EntityEvent> Builder withPlayerSensitiveAction(Class<? extends T> type, Consumer<? super T> task) {
            this.map.put(type, (event, origin) -> {
                if (!event.isCanceled() && origin.getPlayer().equals(event.getEntity()))
                    task.accept(event);
            });
            return this;
        }

        public <T extends EntityEvent> Builder withPlayerSensitiveAction(Class<? extends T> type, BiConsumer<? super T, Origin> task) {
            this.map.put(type, (event, origin) -> {
                if (!event.isCanceled() && origin.getPlayer().equals(event.getEntity()))
                    task.accept(event, origin);
            });
            return this;
        }

        public <T extends EntityEvent> Builder withPlayerSensitiveSound(Class<? extends T> type, SoundEvent sound, SoundCategory category) {
            return withPlayerSensitiveSound(type, sound, category, 1, 1);
        }

        public <T extends EntityEvent> Builder withPlayerSensitiveSound(Class<? extends T> type, SoundEvent sound, SoundCategory category, float volume, float pitch) {
            this.map.put(type, (event, origin) -> {
                if (!event.isCanceled() && origin.getPlayer().equals(event.getEntity()))
                    SoundUtils.playSound(origin.getPlayer(), sound, category, volume, pitch);
            });
            return this;
        }

        public <T extends Event> Builder withCancelAction(Class<? extends T> type, Predicate<T> task) {
            this.map.put(type, event -> {
                if (task.test(event)) tryCancel(event);
            });
            return this;
        }

        private static void tryCancel(Event event) {
            if (!event.isCancelable()) Main.LOGGER.warn("Cannot cancel non-cancellable event '" + event.getClass().getName() + "'");
            else event.setCanceled(true);
        }

        public <T extends Event> Builder withCancelAction(Class<? extends T> type, BiPredicate<T, Origin> task) {
            this.map.put(type, (event, origin) -> {
                if (task.test(event, origin)) tryCancel(event);
            });
            return this;
        }

        public <T extends Event> Builder withUncancelAction(Class<? extends T> type, Predicate<T> task) {
            this.map.put(type, event -> {
                if (task.test(event)) tryUncancel(event);
            });
            return this;
        }

        private static void tryUncancel(Event event) {
            if (!event.isCancelable()) Main.LOGGER.warn("Redundant uncancellation on non-cancellable event '" + event.getClass().getName() + "'");
            else event.setCanceled(true);
        }

        public <T extends Event> Builder withUncancelAction(Class<? extends T> type, BiPredicate<T, Origin> task) {
            this.map.put(type, (event, origin) -> {
                if (task.test(event, origin)) tryUncancel(event);
            });
            return this;
        }

        public EventInterceptProperty build() {
            return new EventInterceptProperty(name, map);
        }
    }

    protected static class BiConsumerMap<K, O> {
        private final Map<Class<? extends K>, BiConsumer<? extends K, O>> interceptors = new HashMap<>(1);

        public <T extends K> void put(Class<? extends T> type, Consumer<T> task) {
            interceptors.put(type, (T event, O origin) -> task.accept(event));
        }
        public <T extends K> void put(Class<? extends T> type, BiConsumer<T, O> task) {
            interceptors.put(type, task);
        }

        @SuppressWarnings("unchecked")
        private <T extends K> BiConsumer<T, O> get(Class<? extends T> type) {
            return (BiConsumer<T, O>) interceptors.get(type);
        }

        private Set<Class<? extends K>> keys() {
            return interceptors.keySet();
        }

    }

}
