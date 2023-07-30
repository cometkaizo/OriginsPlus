package me.cometkaizo.origins.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import me.cometkaizo.origins.origin.CapabilityOrigin;
import me.cometkaizo.origins.origin.Origin;
import me.cometkaizo.origins.origin.OriginType;
import me.cometkaizo.origins.origin.OriginTypes;
import net.minecraft.command.CommandSource;
import net.minecraft.command.Commands;
import net.minecraft.command.ISuggestionProvider;
import net.minecraft.command.arguments.EntityArgument;
import net.minecraft.command.arguments.MessageArgument;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.StringTextComponent;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.registries.IForgeRegistry;

import java.util.Collection;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

public class OriginCommand {

    public OriginCommand(CommandDispatcher<CommandSource> dispatcher) {

        dispatcher.register(
                Commands.literal("origin").requires((source) -> source.hasPermissionLevel(2))
                        .then(Commands.literal("set")
                                .then(Commands.argument("targets", EntityArgument.players())
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
                        .then(Commands.literal("invalidate")
                                .then(Commands.argument("target", EntityArgument.player())
                                        .executes(this::invalidateOrigin)
                                )
                        )
                        .then(Commands.literal("sync")
                                .then(Commands.argument("target", EntityArgument.player())
                                    .executes(this::syncOrigin)
                                )
                        )
                        .then(Commands.literal("list")
                                .executes(this::listOrigins)
                        )
        );

    }

    private CompletableFuture<Suggestions> getTypeSuggestions(CommandContext<CommandSource> context, SuggestionsBuilder suggestionsBuilder) {
        return ISuggestionProvider.suggest(
                OriginTypes.ORIGINS_REGISTRY.get().getKeys().stream().map(ResourceLocation::toString),
                suggestionsBuilder);
    }

    private int setOrigin(CommandContext<CommandSource> context) throws CommandSyntaxException {
        final CommandSource source = context.getSource();
        final Collection<? extends PlayerEntity> targets = EntityArgument.getPlayers(context, "targets");

        final String typeNamespace = MessageArgument.getMessage(context, "type").getString();
        final OriginType newType = OriginTypes.of(typeNamespace);
        if (newType == null) {
            error(source, "Could not find origin '" + typeNamespace + '\'');
            return 1;
        }

        for (PlayerEntity target : targets) {
            if (target == null) continue;
            LazyOptional<Origin> capability = target.getCapability(CapabilityOrigin.ORIGIN_CAPABILITY);

            capability.ifPresent(origin -> {
                OriginType oldType = origin.getType();
                String oldTypeName = oldType == null ? "null" : oldType.getName();
                String targetName = target.getName().getString();

                try {
                    if (origin.isRemoved()) origin.revive();
                    origin.setType(newType);
                    origin.setShouldSynchronize();
                    origin.tempSetPlayer(target);
                } catch (Exception e) {
                    error(source, "Could not change " + targetName + "'s origin from '" + oldTypeName +
                            "' to '" + newType.getName() + "' because an error occurred: " + e);
                    return;
                }

                feedback(source, "Changed " + targetName + "'s origin from '" +
                        oldTypeName + "' to '" + newType.getName() + '\'');
            });

            if (!capability.isPresent()) {
                error(source, target.getName().getString() + " does not have an origin capability");
            }
        }

        return 1;
    }

    private int getOrigin(CommandContext<CommandSource> context) throws CommandSyntaxException {
        CommandSource source = context.getSource();
        ServerPlayerEntity target = EntityArgument.getPlayer(context, "target");
        String targetName = target.getName().getString();

        LazyOptional<Origin> capability = target.getCapability(CapabilityOrigin.ORIGIN_CAPABILITY);
        Objects.requireNonNull(capability, "Contract violation: capability optional is null");

        capability.ifPresent(origin -> {
            if (origin.getType() == null) feedback(source, targetName + " has an origin but no type: " + origin);
            else {
                feedback(source, targetName + "'s origin is '" +
                        origin.getType().getName() + "'" + (origin.isRemoved() ? " (Inactive)" : ""));
            }
        });

        if (!capability.isPresent())
            feedback(source, targetName + " does not have an origin");

        return 1;
    }

    private int invalidateOrigin(CommandContext<CommandSource> context) throws CommandSyntaxException {
        CommandSource source = context.getSource();
        PlayerEntity target = EntityArgument.getPlayer(context, "target");

        LazyOptional<Origin> capability = target.getCapability(CapabilityOrigin.ORIGIN_CAPABILITY);
        Objects.requireNonNull(capability, "Contract violation: capability optional is null");

        capability.ifPresent(Origin::remove);

        if (!capability.isPresent()) feedback(source, target.getName().getString() + " does not have an origin");
        else feedback(source, "Invalidated " + target.getName().getString() + "'s origin");

        return 1;
    }

    private int listOrigins(CommandContext<CommandSource> context) {
        CommandSource source = context.getSource();
        IForgeRegistry<OriginType> originTypes = OriginTypes.ORIGINS_REGISTRY.get();

        if (originTypes == null) {
            error(source, "No OriginTypes registry loaded");
        } else {
            feedback(source, originTypes.getEntries().stream()
                    .map(entry -> entry.getValue() == null ? null :
                            entry.getKey().getLocation() + entry.getValue().getName())
                    .collect(Collectors.joining("\n")));
        }

        return 1;
    }

    private int syncOrigin(CommandContext<CommandSource> context) throws CommandSyntaxException {
        CommandSource source = context.getSource();
        PlayerEntity target = EntityArgument.getPlayer(context, "target");

        LazyOptional<Origin> capability = target.getCapability(CapabilityOrigin.ORIGIN_CAPABILITY);
        Objects.requireNonNull(capability, "Contract violation: capability optional is null");

        capability.ifPresent(Origin::setShouldSynchronize);

        if (!capability.isPresent()) feedback(source, target.getName().getString() + " does not have an origin");
        else feedback(source, "Scheduled " + target.getName().getString() + "'s origin for synchronization");
        return 1;
    }

    private static void feedback(CommandSource source, String message) {
        source.sendFeedback(new StringTextComponent(message), true);
    }
    private static void error(CommandSource source, String message) {
        source.sendErrorMessage(new StringTextComponent(message));
    }

}
