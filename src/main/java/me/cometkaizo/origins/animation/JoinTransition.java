package me.cometkaizo.origins.animation;

import net.minecraft.util.math.MathHelper;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class JoinTransition implements Transition {
    private final List<Transition> animations;
    private final List<Integer> durations;
    private final int totalDuration;

    private JoinTransition(List<Transition> animations) {
        this.animations = animations;
        this.durations = animations.stream().map(Transition::getDuration).collect(Collectors.toList());
        int duration = 0;
        for (Transition animation : this.animations) {
            duration += animation.getDuration();
        }
        this.totalDuration = duration;
    }

    public static JoinTransition of(Transition... animations) {
        return new JoinTransition(new ArrayList<>(Arrays.asList(animations)));
    }

    @Override
    public double apply(int lengthPlayed) {
        int lengthLeft = MathHelper.clamp(lengthPlayed, 0, totalDuration);

        for (int index = 0; index < durations.size(); index++) {
            int duration = durations.get(index);

            lengthLeft -= duration;
            if (lengthLeft <= 0) return applyOnAnimation(index, duration + lengthLeft);
        }

        throw new IllegalStateException("Unexpected state: length played " + lengthPlayed + " is outside range, durations: " + durations + ", total duration: " + totalDuration);
    }

    private double applyOnAnimation(int index, int lengthPlayed) {
        Transition animation = animations.get(index);
        return animation.apply(lengthPlayed);
    }

    @Override
    public int getDuration() {
        return totalDuration;
    }
}
