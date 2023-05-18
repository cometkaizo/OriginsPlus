package me.cometkaizo.origins.property;

import me.cometkaizo.origins.origin.Origin;

public interface Property {

    void onEvent(Object event, Origin origin);

    void performAction(Origin origin);

    void onFirstActivate(Origin origin);
    void onActivate(Origin origin);
    void onDeactivate(Origin origin);

    String getName();

}
