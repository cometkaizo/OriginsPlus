package me.cometkaizo.origins.animation;

public class SimpleEaseOut implements Ease {
    public static final SimpleEaseOut QUAD = new SimpleEaseOut(2);
    public static final SimpleEaseOut CUBIC = new SimpleEaseOut(3);

    private final double ease;

    public SimpleEaseOut(double ease) {
        this.ease = ease;
    }

    @Override
    public double apply(double progress) {
        return 1 - Math.pow(1 - progress, ease);
    }
}
