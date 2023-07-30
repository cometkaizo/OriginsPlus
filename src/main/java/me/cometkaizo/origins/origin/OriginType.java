package me.cometkaizo.origins.origin;

import me.cometkaizo.origins.property.Property;
import net.minecraft.entity.Entity;
import net.minecraft.item.ItemStack;
import net.minecraftforge.registries.IForgeRegistryEntry;

import java.io.Serializable;
import java.util.List;

public interface OriginType extends IForgeRegistryEntry<OriginType>, Serializable {

    static OriginType getOriginType(Entity entity) {
        Origin origin = Origin.getOrigin(entity);
        return origin != null ? origin.getType() : null;
    }


    boolean hasProperty(Property property);
    <T extends Property> List<T> getProperties(Class<T> propertyType);
    void onEvent(Object event, Origin origin);
    void onPlayerSensitiveEvent(Object event, Origin origin);
    boolean hasLabel(Object label, Origin origin);

    /**
     * Performs the hotkey action. Called on both sides when the hotkey is pressed.
     */
    void performAction(Origin origin);
    void init(Origin origin);
    void activate(Origin origin);
    void deactivate(Origin origin);

    /**
     * Initializes this origin type. Called <i>after</i> the registry is registered.
     */
    void init();
    void acceptSynchronization(Origin origin);

    String getName();
    Origin.Description getDescription();
    ItemStack getIcon();

}
