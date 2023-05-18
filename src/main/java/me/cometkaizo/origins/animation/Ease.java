package me.cometkaizo.origins.animation;

import java.util.function.Function;

public interface Ease extends Function<Double, Double> {

    /**
     * Returns a new progress value for the given progress.
     * @param progress the progress through the ease between 0 and 1, 0 being the beginning and 1 being the end
     * @return the new progress between 0 and 1
     */
    double apply(double progress);

    @Override
    default Double apply(Double progress) {
        return apply((double) progress);
    }

}
