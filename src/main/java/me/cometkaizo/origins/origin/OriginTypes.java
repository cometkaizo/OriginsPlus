package me.cometkaizo.origins.origin;

import me.cometkaizo.origins.Main;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.RegistryObject;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.IForgeRegistry;
import net.minecraftforge.registries.RegistryBuilder;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.function.Supplier;

@Mod.EventBusSubscriber(modid = Main.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD)
@SuppressWarnings("unused")
public final class OriginTypes {

    public static final DeferredRegister<OriginType> ORIGINS = DeferredRegister.create(OriginType.class, Main.MOD_ID);
    public static final Supplier<IForgeRegistry<OriginType>> ORIGINS_REGISTRY = ORIGINS.makeRegistry("origins", RegistryBuilder::new);

    public static final RegistryObject<OriginType> HUMAN = ORIGINS.register("human_origin", HumanOriginType::new);
    public static final RegistryObject<ElytrianOriginType> ELYTRIAN = ORIGINS.register("elytrian_origin", ElytrianOriginType::new);
    public static final RegistryObject<EnderianOriginType> ENDERIAN = ORIGINS.register("enderian_origin", EnderianOriginType::new);
    public static final RegistryObject<SharkOriginType> SHARK = ORIGINS.register("shark_origin", SharkOriginType::new);
    public static final RegistryObject<PhoenixOriginType> PHOENIX = ORIGINS.register("phoenix_origin", PhoenixOriginType::new);
    public static final RegistryObject<ArachnidOriginType> ARACHNID = ORIGINS.register("arachnid_origin", ArachnidOriginType::new);
    public static final RegistryObject<SlimicianOriginType> SLIMICIAN = ORIGINS.register("slimician_origin", SlimicianOriginType::new);
    public static final RegistryObject<FoxOriginType> FOX = ORIGINS.register("fox_origin", FoxOriginType::new);
    public static final RegistryObject<PhantomOriginType> PHANTOM = ORIGINS.register("phantom_origin", PhantomOriginType::new);


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

    @SubscribeEvent
    public static void onRegisterRegistries(RegistryEvent.Register<?> event) {
        if (event.getRegistry() == ORIGINS_REGISTRY.get()) ORIGINS_REGISTRY.get().getValues().forEach(OriginType::init);
    }

    private OriginTypes() {
        throw new AssertionError("No OriginTypes instances for you!");
    }

}
