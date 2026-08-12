package net.swzo.create_blueprinted.command;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.arguments.ResourceLocationArgument;
import net.minecraft.commands.arguments.coordinates.Coordinates;
import net.minecraft.commands.arguments.coordinates.RotationArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec2;
import net.swzo.create_blueprinted.CreateBlueprinted;
import net.swzo.create_blueprinted.api.ShareProvider;
import net.swzo.create_blueprinted.api.ShareProviderRegistry;
import net.swzo.create_blueprinted.render.SchematicRenderSettings;
import net.swzo.create_blueprinted.util.SchematicUtils;
import net.swzo.create_blueprinted.handler.SchematicImageHandler;
import org.jetbrains.annotations.Nullable;

import java.util.concurrent.CompletableFuture;

import static net.minecraft.commands.Commands.literal;
import static net.minecraft.commands.Commands.argument;
import static net.swzo.create_blueprinted.CreateBlueprinted.rl;
import static net.swzo.create_blueprinted.CreateBlueprinted.translatable;
import static net.swzo.create_blueprinted.CreateBlueprintedConfig.CONFIG;
import static net.swzo.create_blueprinted.render.SchematicRenderSettings.*;
import static net.swzo.create_blueprinted.util.UIHelpers.LIGHT_GREEN_TEXT_COLOR;

public class CBSchematicCommands {

    private static String queuedFileName = "";
    private static @Nullable SchematicRenderSettings.Builder queuedSettingsBuilder = null;

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(literal("schematic")
                .then(buildArguments("export"))
                .then(buildArguments("share"))
                .then(literal("shareprovider")
                        .then(argument("provider", ResourceLocationArgument.id())
                                .suggests((ctx, builder) -> suggestsShareProviders(builder))
                                .executes(CBSchematicCommands::switchShareProvider)
                        ))
                .then(literal("confirm")
                        .executes((ctx) -> confirmShare(ctx, queuedSettingsBuilder))
                )
        );
    }

    private static LiteralArgumentBuilder<CommandSourceStack> buildArguments(String subcommand) {
        return literal(subcommand)
                .then(argument("fileName", StringArgumentType.string())
                        .suggests((ctx, builder) -> suggestSchematics(builder))
                        .executes(CBSchematicCommands::defaultSettings)
                        .then(argument("width", IntegerArgumentType.integer(MIN_WIDTH, MAX_WIDTH))
                                .executes(CBSchematicCommands::withWidth)
                                .then(argument("rotation", RotationArgument.rotation())
                                        .executes(CBSchematicCommands::withOrientation)
                                        .then(argument("antialiasingFactor", IntegerArgumentType.integer(1, MAX_ANTIALIASING))
                                                .executes(CBSchematicCommands::withAntialiasingFactor)
                                        )
                                ))
                );
    }

    private static int defaultSettings(CommandContext<CommandSourceStack> ctx) {
        return dispatch(ctx, SchematicRenderSettings.builder().imageWidth(CONFIG.defaultWidth.get()));
    }

    private static int withWidth(CommandContext<CommandSourceStack> ctx) {
        return dispatch(ctx, SchematicRenderSettings.builder()
                .imageWidth(IntegerArgumentType.getInteger(ctx, "width")));
    }

    private static int withOrientation(CommandContext<CommandSourceStack> ctx) {
        return dispatch(ctx, SchematicRenderSettings.builder()
                .imageWidth(IntegerArgumentType.getInteger(ctx, "width"))
                .orientation(getOrientation(ctx)));
    }

    private static int withAntialiasingFactor(CommandContext<CommandSourceStack> ctx) {
        return dispatch(ctx, SchematicRenderSettings.builder()
                .imageWidth(IntegerArgumentType.getInteger(ctx, "width"))
                .orientation(getOrientation(ctx))
                .antialiasingFactor(IntegerArgumentType.getInteger(ctx, "antialiasingFactor")));
    }
    
    private static int dispatch(CommandContext<CommandSourceStack> ctx, SchematicRenderSettings.Builder settingsBuilder) {
        String fileName = StringArgumentType.getString(ctx, "fileName");
        var imageHandler = new SchematicImageHandler(fileName, ctx.getSource(), settingsBuilder);

        for (var node : ctx.getNodes()) {
            String nodeName = node.getNode().getName();

            if (nodeName.equals("export")) {
                imageHandler.export();
                break;
            } else if (nodeName.equals("share")) {
                ShareProviderRegistry.getActiveShareProvider().ifPresentOrElse(provider -> {
                    queuedFileName = fileName;
                    queuedSettingsBuilder = settingsBuilder;

                    ctx.getSource().sendSuccess(() -> getShareContext(provider, false)
                            .append(translatable("command.shareschem.queue_confirm")
                                    .withStyle(ChatFormatting.GRAY))
                            .append(Component.literal("/schematic confirm.")
                                    .withColor(LIGHT_GREEN_TEXT_COLOR)), false);

                    for (var extraMessage : provider.extras())
                        ctx.getSource().sendSystemMessage(extraMessage);
                }, () -> ctx.getSource().sendFailure(translatable("command.shareschem.no_share_provider")));
                break;
            }
        }
        return Command.SINGLE_SUCCESS;
    }

    private static int confirmShare(CommandContext<CommandSourceStack> ctx, SchematicRenderSettings.Builder settingsBuilder) {
        if (!queuedFileName.isBlank() && settingsBuilder != null) {
            new SchematicImageHandler(queuedFileName, ctx.getSource(), settingsBuilder).share();
            resetQueuedFile();
        } else
            ctx.getSource().sendFailure(translatable("command.shareschem.no_queued_file"));
        return Command.SINGLE_SUCCESS;
    }

    private static Orientation getOrientation(CommandContext<CommandSourceStack> ctx) {
        Coordinates coordinates = RotationArgument.getRotation(ctx, "rotation");
        Vec2 rotation = coordinates.getRotation(ctx.getSource());
        return new Orientation(rotation.y, rotation.x);
    }

    private static CompletableFuture<Suggestions> suggestSchematics(SuggestionsBuilder builder) {
        SchematicUtils.getAllSchematicNames().forEach(builder::suggest);
        return builder.buildFuture();
    }

    private static CompletableFuture<Suggestions> suggestsShareProviders(SuggestionsBuilder builder) {
        ShareProviderRegistry.getAllProviders().forEach(provider ->
                builder.suggest(provider.id().toString()));
        return builder.buildFuture();
    }

    private static int switchShareProvider(CommandContext<CommandSourceStack> ctx) {
        ResourceLocation id = ResourceLocationArgument.getId(ctx, "provider");

        ShareProviderRegistry.getShareProvider(id).ifPresentOrElse(provider -> {
            CreateBlueprinted.State.activeShareProvider.set(id.toString());
            CreateBlueprinted.State.activeShareProvider.save();

            ctx.getSource().sendSuccess(() -> translatable("command.shareschem.switched_provider")
                    .withStyle(ChatFormatting.GRAY).append(Component.literal(id.toString())
                            .withColor(LIGHT_GREEN_TEXT_COLOR)), false);
            ctx.getSource().sendSystemMessage(getShareContext(provider, true));
        }, () -> ctx.getSource().sendFailure(translatable("command.shareschem.switched_provider.invalid")));

        resetQueuedFile();
        return Command.SINGLE_SUCCESS;
    }

    private static MutableComponent getShareContext(ShareProvider provider, boolean plural) {
        return translatable("command.shareschem.queued_" +
                    (provider.includeSchematicData() ? "schematic" : "image") + (plural ? "_plural" : ""))
                .withStyle(ChatFormatting.GRAY)
                .append(provider.destinationName().plainCopy()
                        .withColor(LIGHT_GREEN_TEXT_COLOR))
                .append(Component.literal(" (" + provider.destinationUrl() + "). ")
                        .withColor(LIGHT_GREEN_TEXT_COLOR));
    }

    public static void resetQueuedFile() {
        queuedFileName = "";
        queuedSettingsBuilder = null;
    }
}
