package me.cometkaizo.origins.animation;

public class SimpleEaseIn implements Ease {
    public static final SimpleEaseIn QUAD = new SimpleEaseIn(2);
    public static final SimpleEaseIn CUBIC = new SimpleEaseIn(3);


    private final double ease;

    public SimpleEaseIn(double ease) {
        this.ease = ease;
    }

    @Override
    public double apply(double progress) {
        return Math.pow(progress, ease);
    }
}
