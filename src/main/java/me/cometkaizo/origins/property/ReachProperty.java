package me.cometkaizo.origins.property;

import me.cometkaizo.origins.mixin.EntityReachMixin;
import me.cometkaizo.origins.origin.Origin;
import me.cometkaizo.origins.util.AttributeUtils;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraftforge.common.ForgeMod;

@Requires(EntityReachMixin.class)
public class ReachProperty extends AbstractProperty {
    private final float reach;
    private final boolean hasCreativeEntityReach;

    public ReachProperty(float reach, boolean hasCreativeEntityReach) {
        super("Reach");
        this.reach = reach;
        this.hasCreativeEntityReach = hasCreativeEntityReach;
    }

    @Override
    public void onActivate(Origin origin) {
        PlayerEntity player = origin.getPlayer();
        AttributeUtils.setAttribute(player, ForgeMod.REACH_DISTANCE.get(), reach);
    }

    @Override
    public void onDeactivate(Origin origin) {
        PlayerEntity player = origin.getPlayer();
        AttributeUtils.setAttribute(player, ForgeMod.REACH_DISTANCE.get(), ForgeMod.REACH_DISTANCE.get().getDefaultValue());
    }

    public boolean hasCreativeEntityReach() {
        return this.hasCreativeEntityReach;
    }
}
