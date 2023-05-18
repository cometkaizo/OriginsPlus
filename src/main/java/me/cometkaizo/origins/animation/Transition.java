package me.cometkaizo.origins.animation;

public interface Transition {

    default double apply(int startTick, int currentTick) {
        return apply(currentTick - startTick);
    }

    double apply(int lengthPlayed);

    default boolean isFinished(int startTick, int currentTick) {
        int lengthPlayed = currentTick - startTick;
        return lengthPlayed < 0 || lengthPlayed >= getDuration();
    }

    int getDuration();

    default Transition andThen(Transition other) {
        return JoinTransition.of(this, other);
    }

}
