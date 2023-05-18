package me.cometkaizo.origins.util;

import net.minecraft.util.math.MathHelper;

@SuppressWarnings("unused")
public class ColorUtils {

    public static int fadeInto(int from, int to, float amount) {
        amount = MathHelper.clamp(amount, 0, 1);
        return (int) (amount * to + (1 - amount) * from);
    }
    public static float fadeInto(float from, float to, float amount) {
        from = MathHelper.clamp(from, 0, 1);
        to = MathHelper.clamp(to, 0, 1);
        amount = MathHelper.clamp(amount, 0, 1);
        return amount * to + (1 - amount) * from;
    }
    public static double fadeInto(double from, double to, double amount) {
        from = MathHelper.clamp(from, 0, 1);
        to = MathHelper.clamp(to, 0, 1);
        amount = MathHelper.clamp(amount, 0, 1);
        return amount * to + (1 - amount) * from;
    }

}
