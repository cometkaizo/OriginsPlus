package me.cometkaizo.origins.animation;

import net.minecraft.util.math.MathHelper;

public class SimpleEaseInOut implements Ease {
    public static final SimpleEaseInOut QUAD = new SimpleEaseInOut(2, 2, 0.5);
    public static final SimpleEaseInOut CUBIC = new SimpleEaseInOut(3, 3, 0.5);


    private final double easeIn;
    private final double easeOut;
    private final double switchPoint;
    private final double easeOutMul;

    public SimpleEaseInOut(double easeIn, double easeOut, double switchPoint) {
        this.easeIn = easeIn;
        this.easeOut = easeOut;
        this.switchPoint = MathHelper.clamp(switchPoint, 0, 1);
        this.easeOutMul = 1 / (1 - this.switchPoint);
    }


    @Override
    public double apply(double progress) {
        return progress < switchPoint ?
                Math.pow(2, easeIn - 1) * Math.pow(progress, easeIn) :
                1 - Math.pow(-easeOutMul * progress + easeOutMul, easeOut) / easeOutMul;
    }
}
