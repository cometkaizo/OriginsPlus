package me.cometkaizo.origins.origin;

import me.cometkaizo.origins.Main;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.RegistryObject;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.IForgeRegistry;
import net.minecraftforge.registries.RegistryBuilder;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.function.Supplier;

public final class OriginTypes {

    public static final DeferredRegister<OriginType> ORIGINS = DeferredRegister.create(OriginType.class, Main.MOD_ID);
    public static final Supplier<IForgeRegistry<OriginType>> ORIGINS_REGISTRY = OriginTypes.ORIGINS.makeRegistry("origins", RegistryBuilder::new);

    public static final RegistryObject<OriginType> HUMAN = ORIGINS.register("human_origin", () -> new AbstractOriginType("Human") {});
    public static final RegistryObject<OriginType> ELYTRIAN = ORIGINS.register("elytrian_origin", ElytrianOriginType::new);
    public static final RegistryObject<OriginType> ENDERIAN = ORIGINS.register("enderian_origin", EnderianOriginType::new);
    public static final RegistryObject<OriginType> SHARK = ORIGINS.register("shark_origin", SharkOriginType::new);
    public static final RegistryObject<OriginType> PHOENIX = ORIGINS.register("phoenix_origin", PhoenixOriginType::new);
    public static final RegistryObject<OriginType> ARACHNID = ORIGINS.register("arachnid_origin", ArachnidOriginType::new);

    @Nullable
    public static ResourceLocation getKey(OriginType originType) {
        return ORIGINS_REGISTRY.get().getKey(originType);
    }
    @Nullable
    public static OriginType of(ResourceLocation key) {
        return ORIGINS_REGISTRY.get().getValue(key);
    }
    @Nullable
    public static OriginType of(@Nonnull String namespace) {
        if (namespace.contains(":")) {
            String[] s = namespace.split(":");
            return of(new ResourceLocation(s[0], s[1]));
        }
        return of(new ResourceLocation(Main.MOD_ID, namespace));
    }

    public static void register(IEventBus bus) {
        ORIGINS.register(bus);
    }

    private OriginTypes() {
        throw new AssertionError("No OriginTypes instances for you!");
    }
}
