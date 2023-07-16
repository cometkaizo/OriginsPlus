package me.cometkaizo.origins.animation;

public interface Transition {

    default double apply(long startTick, long currentTick) {
        return apply(currentTick - startTick);
    }

    double apply(long lengthPlayed);

    default boolean isFinished(long startTick, long currentTick) {
        long lengthPlayed = currentTick - startTick;
        return lengthPlayed < 0 || lengthPlayed >= getDuration();
    }

    long getDuration();

    default Transition andThen(Transition other) {
        return JoinTransition.of(this, other);
    }

}
