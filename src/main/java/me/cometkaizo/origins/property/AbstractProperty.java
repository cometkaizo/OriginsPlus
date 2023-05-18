package me.cometkaizo.origins.property;

import me.cometkaizo.origins.origin.Origin;

public abstract class AbstractProperty implements Property {

    protected final String name;

    protected AbstractProperty(String name) {
        this.name = name;
    }
    protected AbstractProperty() {
        this.name = getClass().getSimpleName().replaceAll("(?<=.)" + Property.class.getSimpleName() + "$", "");
    }

    @Override
    public void onEvent(Object event, Origin origin) {

    }

    @Override
    public void performAction(Origin origin) {

    }

    @Override
    public void onFirstActivate(Origin origin) {

    }

    @Override
    public void onActivate(Origin origin) {

    }

    @Override
    public void onDeactivate(Origin origin) {

    }

    @Override
    public String getName() {
        return name;
    }
}
