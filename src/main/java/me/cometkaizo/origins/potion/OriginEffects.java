package me.cometkaizo.origins.potion;

import me.cometkaizo.origins.Main;
import net.minecraft.entity.ai.attributes.AttributeModifier;
import net.minecraft.entity.ai.attributes.Attributes;
import net.minecraft.potion.Effect;
import net.minecraft.potion.EffectType;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.RegistryObject;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;

public class OriginEffects {

    public static final DeferredRegister<Effect> EFFECTS = DeferredRegister.create(ForgeRegistries.POTIONS, Main.MOD_ID);

    public static final RegistryObject<Effect> FLIGHT_WEAKNESS = EFFECTS.register("flight_weakness",
            () -> new CustomEffect(EffectType.HARMFUL, 4738376)
                    .addAttributesModifier(Attributes.MOVEMENT_SPEED, "ed185734-aba7-44ae-8ea9-056aabf4c23c", -0.2, AttributeModifier.Operation.MULTIPLY_TOTAL)
                    .addAttributesModifier(Attributes.ATTACK_DAMAGE, "c600abce-d888-473b-8d6f-9296dea48b06", -0.35, AttributeModifier.Operation.MULTIPLY_TOTAL)
                    .addAttributesModifier(Attributes.ATTACK_SPEED, "fc9f23f0-b82f-430a-b25c-3545286695da", -0.35, AttributeModifier.Operation.MULTIPLY_TOTAL)
                    .addAttributesModifier(Attributes.ATTACK_KNOCKBACK, "fc9f23f0-b82f-430a-b25c-3545286695da", -0.45, AttributeModifier.Operation.MULTIPLY_TOTAL)
                    .addAttributesModifier(Attributes.FLYING_SPEED, "fc9f23f0-b82f-430a-b25c-3545286695da", -0.35, AttributeModifier.Operation.MULTIPLY_TOTAL)
    );
    public static final RegistryObject<Effect> FLIGHT_STRENGTH = EFFECTS.register("flight_strength",
            () -> new CustomEffect(EffectType.BENEFICIAL, 9643043)
                    .addAttributesModifier(Attributes.ATTACK_DAMAGE, "39e8c1fb-b6ba-4c8f-9a22-0d6e24e4e7c2", 1, AttributeModifier.Operation.MULTIPLY_TOTAL)
    );
    public static final RegistryObject<Effect> CAMOUFLAGE = EFFECTS.register("camouflage",
            () -> new CustomEffect(EffectType.BENEFICIAL, 3381504)
    );

    public static void register(IEventBus bus) {
        EFFECTS.register(bus);
    }

    private OriginEffects() {
        throw new AssertionError("No OriginsEffects instances for you!");
    }
}
