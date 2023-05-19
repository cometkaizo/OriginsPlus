package me.cometkaizo.origins.origin;

import me.cometkaizo.origins.property.Property;
import net.minecraft.entity.Entity;
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
    boolean hasMixinProperty(Object property, Origin origin);
    void performAction(Origin origin);
    void onFirstActivate(Origin origin);
    void onActivate(Origin origin);
    void onDeactivate(Origin origin);
    String getName();

}
