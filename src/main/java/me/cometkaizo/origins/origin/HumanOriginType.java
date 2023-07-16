package me.cometkaizo.origins.origin;

import net.minecraft.item.Items;

public class HumanOriginType extends AbstractOriginType {
    public HumanOriginType() {
        super("Human", Items.PLAYER_HEAD, Origin.Description::new);
    }
}
