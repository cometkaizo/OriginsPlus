package me.cometkaizo.origins.origin;

import me.cometkaizo.origins.property.Property;
import me.cometkaizo.origins.util.CollUtils;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraftforge.client.event.RenderPlayerEvent;
import net.minecraftforge.eventbus.api.Event;
import net.minecraftforge.registries.ForgeRegistryEntry;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.function.Function;

public abstract class AbstractOriginType extends ForgeRegistryEntry<OriginType> implements OriginType {
    protected static final Random random = new Random();
    protected final String name;
    private final Function<AbstractOriginType, Origin.Description> descriptionFunction;
    protected final Set<Property> properties;
    protected Origin.Description description;
    protected final ItemStack icon;

    protected AbstractOriginType(String name, Item icon, Function<AbstractOriginType, Origin.Description> descriptionFunction, Property... properties) {
        this(name, new ItemStack(icon), descriptionFunction, properties);
    }
    protected AbstractOriginType(Item icon, Function<AbstractOriginType, Origin.Description> descriptionFunction, Property... properties) {
        this(new ItemStack(icon), descriptionFunction, properties);
    }
    protected AbstractOriginType(String name, ItemStack icon, Function<AbstractOriginType, Origin.Description> descriptionFunction, Property... properties) {
        this.name = name;
        this.icon = icon;
        this.descriptionFunction = descriptionFunction == null ? Origin.Description::new : descriptionFunction;
        this.properties = CollUtils.setOf(properties);
    }
    protected AbstractOriginType(ItemStack icon, Function<AbstractOriginType, Origin.Description> descriptionFunction, Property... properties) {
        this.icon = icon;
        this.descriptionFunction = descriptionFunction == null ? Origin.Description::new : descriptionFunction;
        this.name = getClass().getSimpleName()
                .replaceAll("(?<=.)" + OriginType.class.getSimpleName() + "$", "")
                .replaceAll("(.)([A-Z])", "\\1 \\2");
        this.properties = CollUtils.setOf(properties);
    }

    @Override
    public void init() {
        this.description = descriptionFunction.apply(this);
    }

    @Override
    public void acceptSynchronization(Origin origin) {

    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public Origin.Description getDescription() {
        return description;
    }

    @Override
    public ItemStack getIcon() {
        return icon;
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

        if (!origin.isServerSide() && event instanceof RenderPlayerEvent.Pre) {
            RenderPlayerEvent.Pre renderEvent = (RenderPlayerEvent.Pre) event;
            Origin o = Origin.getOrigin(renderEvent.getPlayer());
            if (o != null && o.getType() instanceof SlimicianOriginType) {
                ((SlimicianOriginType) o.getType()).onRenderPlayer(renderEvent, o);
            }
        }
    }

    protected boolean isCanceled(Object event) {
        return event instanceof Event && ((Event)event).isCanceled();
    }

    @Override
    public void onPlayerSensitiveEvent(Object event, Origin origin) {

    }

    @Override
    public boolean hasLabel(Object label, Origin origin) {
        return false;
    }

    @Override
    public void performAction(Origin origin) {
        for (Property property : properties) {
            property.performAction(origin);
        }
    }

    @Override
    public void init(Origin origin) {
        for (Property property : properties) {
            property.onFirstActivate(origin);
        }
    }

    @Override
    public void activate(Origin origin) {
        for (Property property : properties) {
            property.onActivate(origin);
        }
    }

    @Override
    public void deactivate(Origin origin) {
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
