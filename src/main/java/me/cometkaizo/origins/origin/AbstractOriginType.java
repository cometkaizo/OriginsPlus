package me.cometkaizo.origins.origin;

import me.cometkaizo.origins.property.Property;
import me.cometkaizo.origins.util.CollUtils;
import net.minecraftforge.registries.ForgeRegistryEntry;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.Set;

public abstract class AbstractOriginType extends ForgeRegistryEntry<OriginType> implements OriginType {
    protected static final Random random = new Random();
    protected final String name;
    protected final Set<Property> properties;

    protected AbstractOriginType(String name, Property... properties) {
        this.name = name;
        this.properties = CollUtils.setOf(properties);
    }
    protected AbstractOriginType(Property... properties) {
        this.name = getClass().getSimpleName()
                .replaceAll("(?<=.)" + OriginType.class.getSimpleName() + "$", "")
                .replaceAll("(.)([A-Z])", "\\1 \\2");
        this.properties = CollUtils.setOf(properties);
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public boolean hasProperty(Property property) {
        return properties.contains(property);
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T extends Property> List<T> getProperties(Class<T> propertyType) {
        List<T> result = new ArrayList<>(1);
        for (Property property : properties) {
            if (propertyType.isAssignableFrom(property.getClass())) result.add((T) property);
        }
        return result;
    }

    @Override
    public void onEvent(Object event, Origin origin) {
        for (Property property : properties) {
            property.onEvent(event, origin);
        }
    }

    @Override
    public void onPlayerSensitiveEvent(Object event, Origin origin) {

    }

    @Override
    public boolean hasMixinProperty(Object property, Origin origin) {
        return false;
    }

    @Override
    public void performAction(Origin origin) {
        for (Property property : properties) {
            property.performAction(origin);
        }
    }

    @Override
    public void onFirstActivate(Origin origin) {
        for (Property property : properties) {
            property.onFirstActivate(origin);
        }
    }

    @Override
    public void onActivate(Origin origin) {
        for (Property property : properties) {
            property.onActivate(origin);
        }
    }

    @Override
    public void onDeactivate(Origin origin) {
        for (Property property : properties) {
            property.onDeactivate(origin);
        }
    }

    protected static int randomInt(int origin, int bound) {
        return origin + random.nextInt(bound - origin);
    }
    protected static float randomFloat(float origin, float bound) {
        return random.nextFloat() * (bound - origin) + origin;
    }
    protected static double randomDouble(double origin, double bound) {
        return random.nextDouble() * (bound - origin) + origin;
    }
}
