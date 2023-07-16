package me.cometkaizo.origins.animation;

public class NoTransition implements Transition {
    private final double value;
    private final double duration;

    public NoTransition(double value, int duration) {
        throwIfIllegalDuration(duration);
        this.value = value;
        this.duration = duration;
    }

    private static void throwIfIllegalDuration(int duration) {
        if (duration < 0) throw new IllegalArgumentException("Duration " + duration + " cannot be negative");
    }

    public double apply(long lengthPlayed) {
        return value;
    }

    @Override
    public long getDuration() {
        return (int) duration;
    }

}
