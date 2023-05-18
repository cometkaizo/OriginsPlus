package me.cometkaizo.origins.property;

import me.cometkaizo.origins.origin.Origin;

import java.util.function.Consumer;

public class ActionOnActionKeyProperty extends AbstractProperty {
    private final Consumer<Origin> action;

    public ActionOnActionKeyProperty(Consumer<Origin> action) {
        super("Action On Action Key");
        this.action = action;
    }

    @Override
    public void performAction(Origin origin) {
        super.performAction(origin);
        action.accept(origin);
    }
}
