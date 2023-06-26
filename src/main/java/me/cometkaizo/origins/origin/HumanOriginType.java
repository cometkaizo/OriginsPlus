package me.cometkaizo.origins.origin;

import net.minecraft.item.Items;

class HumanOriginType extends AbstractOriginType {
    public HumanOriginType() {
        super("Human", Items.PLAYER_HEAD, Origin.Description::new);
    }
}
