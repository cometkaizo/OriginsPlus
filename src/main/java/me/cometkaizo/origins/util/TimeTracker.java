package me.cometkaizo.origins.util;

import net.minecraft.util.math.MathHelper;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class TimeTracker {
    private final List<Object> keys = new ArrayList<>(1);
    private final List<Entry> entries = new ArrayList<>(1);
    private int tick;

    public void tick() {
        tick++;
        for (int index = 0; index < entries.size(); index++) {
            Entry entry = entries.get(index);
            if (entry.shouldBeRemoved(tick)) remove(index);
        }
    }

    public boolean hasTimer(Object key) {
        return hasKeyOf(key, TimerEntry.class);
    }

    public boolean hasCooldownOf(Class<?> keyType) {
        return hasEntryOf(keyType, TimerEntry.class);
    }

    public boolean hasKeyOf(Object key, Class<?> entryType) {
        int index = keys.indexOf(key);
        return index > -1 && entryType.isAssignableFrom(entries.get(index).getClass());
    }

    public boolean hasEntryOf(Class<?> keyType, Class<?> entryType) {
        for (int index = 0; index < keys.size(); index++) {
            Object key = keys.get(index);

            if (key != null) {
                Entry entry = entries.get(index);
                if (entryType.isAssignableFrom(entry.getClass()) &&
                        keyType.isAssignableFrom(key.getClass())) return true;
            } else if (keyType == Object.class) return true;
        }
        return false;
    }

    public int getTimerLeft(Object key) {
        TimerEntry timer = getTimer(key);
        if (timer == null) return 0;
        return timer.getTimeLeft(tick);
    }

    public int getTimerDuration(Object key) {
        TimerEntry timer = getTimer(key);
        if (timer == null) return 0;
        return timer.duration;
    }

    public float getTimerPercentage(Object key) {
        TimerEntry timer = getTimer(key);
        if (timer == null) return 1;
        return timer.getPercentageElapsed(tick);
    }

    private TimerEntry getTimer(Object key) {
        int index = keys.indexOf(key);
        if (index == -1) return null;
        Entry entry = entries.get(index);
        return entry instanceof TimerEntry ? (TimerEntry) entry : null;
    }

    public int getStopwatchTime(Object key) {
        StopwatchEntry cooldown = getStopwatch(key);
        if (cooldown == null) return 0;
        return cooldown.getTicksElapsed(tick);
    }

    private StopwatchEntry getStopwatch(Object key) {
        Entry entry = entries.get(keys.indexOf(key));
        return entry instanceof StopwatchEntry ? (StopwatchEntry) entry : null;
    }

    public void addTimer(Object key, int duration) {
        if (duration <= 0) return;
        addOrOverwrite(key, new TimerEntry(tick, tick + duration));
    }
    public void addTimer(Timer timer) {
        if (timer == null) return;
        if (timer.getDuration() <= 0) return;
        addOrOverwrite(timer, new TimerEntry(tick, tick + timer.getDuration()));
    }

    public void addStopwatch(Object key) {
        addOrOverwrite(key, new StopwatchEntry(tick));
    }

    public void setStopwatch(Object key, int time) {
        StopwatchEntry cooldown = getStopwatch(key);
        if (cooldown == null) return;
        cooldown.creationTick = tick - time;
    }

    public void resetStopwatch(Object key) {
        setStopwatch(key, 0);
    }

    private void addOrOverwrite(Object key, Entry entry) {
        Objects.requireNonNull(key, "Key cannot be null; Entry: " + entry);

        int indexOfKey = keys.indexOf(key);
        if (indexOfKey > -1) {
            entries.set(indexOfKey, entry);
        } else {
            keys.add(key);
            entries.add(entry);
        }
    }

    public void remove(Object key) {
        remove(keys.indexOf(key));
    }

    private void remove(int index) {
        if (index > -1 && index < entries.size()) {
            keys.remove(index);
            entries.remove(index);
        }
    }


    public interface Timer {
        int getDuration();
    }

    private abstract static class Entry {

        protected int creationTick;

        private Entry(int creationTick) {
            this.creationTick = creationTick;
        }

        abstract boolean shouldBeRemoved(int currentTick);

        @Override
        public String toString() {
            return "Entry{" +
                    "creationTick=" + creationTick +
                    '}';
        }
    }

    private static class TimerEntry extends Entry {
        private final int expiryTick;
        private final int duration;

        TimerEntry(int creationTick, int expiryTick) {
            super(creationTick);
            this.expiryTick = expiryTick;
            this.duration = expiryTick - creationTick;
        }

        boolean shouldBeRemoved(int currentTick) {
            return currentTick >= expiryTick;
        }

        int getTimeLeft(int currentTick) {
            return expiryTick - currentTick;
        }

        float getPercentageElapsed(int currentTick) {
            int elapsedTicks = currentTick - creationTick;
            return MathHelper.clamp(elapsedTicks / (float) duration, 0, 1);
        }

        @Override
        public String toString() {
            return "TimerEntry{" +
                    "expiryTick=" + expiryTick +
                    ", duration=" + duration +
                    '}';
        }
    }
    private static class StopwatchEntry extends Entry {

        StopwatchEntry(int creationTick) {
            super(creationTick);
        }

        @Override
        boolean shouldBeRemoved(int currentTick) {
            return false;
        }

        int getTicksElapsed(int currentTick) {
            return currentTick - creationTick;
        }

    }
}
