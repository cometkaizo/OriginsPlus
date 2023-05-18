package me.cometkaizo.origins.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import me.cometkaizo.origins.origin.CapabilityOrigin;
import me.cometkaizo.origins.origin.OriginType;
import me.cometkaizo.origins.origin.OriginTypes;
import net.minecraft.command.CommandSource;
import net.minecraft.command.Commands;
import net.minecraft.command.ISuggestionProvider;
import net.minecraft.command.arguments.EntityArgument;
import net.minecraft.command.arguments.MessageArgument;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.StringTextComponent;

import java.util.Collection;
import java.util.concurrent.CompletableFuture;

public class OriginCommand {

    public OriginCommand(CommandDispatcher<CommandSource> dispatcher) {

        dispatcher.register(
                Commands.literal("origin")
                        .then(Commands.literal("set")
                                .then(Commands.argument("target", EntityArgument.players())
                                        .then(Commands.argument("type", MessageArgument.message()).suggests(this::getTypeSuggestions)
                                                .executes(this::setOrigin)
                                        )
                                )
                        )
                        .then(Commands.literal("get")
                                .then(Commands.argument("target", EntityArgument.player())
                                        .executes(this::getOrigin)
                                )
                        )
        );

    }

    private CompletableFuture<Suggestions> getTypeSuggestions(CommandContext<CommandSource> context, SuggestionsBuilder suggestionsBuilder) {
        return ISuggestionProvider.suggest(
                OriginTypes.ORIGINS_REGISTRY.get().getKeys().stream().map(ResourceLocation::toString),
                suggestionsBuilder);
    }

    private int setOrigin(CommandContext<CommandSource> context) throws CommandSyntaxException {
        CommandSource source = context.getSource();
        Collection<? extends PlayerEntity> targets = EntityArgument.getPlayers(context, "target");

        String typeNamespace = MessageArgument.getMessage(context, "type").getString();
        OriginType newType = OriginTypes.of(typeNamespace);
        if (newType == null) {
            error(source, "Could not find origin '" + typeNamespace + '\'');
            return 1;
        }

        for (PlayerEntity target : targets) {
            target.getCapability(CapabilityOrigin.ORIGIN_CAPABILITY).ifPresent(origin -> {
                OriginType oldType = origin.getType();
                origin.setType(newType);
                origin.setShouldSynchronize();
                feedback(source, "Changed " + target.getName().getString() + "'s origin from '" + oldType.getName() + "' to '" + newType.getName() + '\'');
            });

            if (!target.getCapability(CapabilityOrigin.ORIGIN_CAPABILITY).isPresent()) {
                error(source, target.getName().getString() + " does not have an origin capability");
            }
        }

        return 1;
    }

    private int getOrigin(CommandContext<CommandSource> context) throws CommandSyntaxException {
        CommandSource source = context.getSource();
        PlayerEntity target = EntityArgument.getPlayer(context, "target");

        target.getCapability(CapabilityOrigin.ORIGIN_CAPABILITY).ifPresent(origin ->
                feedback(source, target.getName().getString() + "'s origin is '" +
                        origin.getType().getName() + '\'')
        );

        if (!target.getCapability(CapabilityOrigin.ORIGIN_CAPABILITY).isPresent())
            feedback(source, target.getName().getString() + " does not have an origin");

        return 1;
    }

    private static void feedback(CommandSource source, String message) {
        source.sendFeedback(new StringTextComponent(message), true);
    }
    private static void error(CommandSource source, String message) {
        source.sendErrorMessage(new StringTextComponent(message));
    }

}
