package net.swzo.create_blueprinted.command;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.arguments.coordinates.Coordinates;
import net.minecraft.commands.arguments.coordinates.RotationArgument;
import net.minecraft.world.phys.Vec2;
import net.swzo.create_blueprinted.api.SchematicRenderSettings;
import net.swzo.create_blueprinted.util.SchematicUtils;
import net.swzo.create_blueprinted.SchematicImageHandler;

import java.util.concurrent.CompletableFuture;

import static net.minecraft.commands.Commands.literal;
import static net.minecraft.commands.Commands.argument;
import static net.swzo.create_blueprinted.api.SchematicRenderSettings.*;

public class CBSchematicCommands {

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(literal("schematic")
                .then(buildArguments("export"))
                .then(buildArguments("share"))
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
        return exportOrShare(ctx, SchematicRenderSettings.builder().imageWidth(DEFAULT_WIDTH));
    }

    private static int withWidth(CommandContext<CommandSourceStack> ctx) {
        return exportOrShare(ctx, SchematicRenderSettings.builder()
                .imageWidth(IntegerArgumentType.getInteger(ctx, "width")));
    }

    private static int withOrientation(CommandContext<CommandSourceStack> ctx) {
        return exportOrShare(ctx, SchematicRenderSettings.builder()
                .imageWidth(IntegerArgumentType.getInteger(ctx, "width"))
                .orientation(getOrientation(ctx)));
    }

    private static int withAntialiasingFactor(CommandContext<CommandSourceStack> ctx) {
        return exportOrShare(ctx, SchematicRenderSettings.builder()
                .imageWidth(IntegerArgumentType.getInteger(ctx, "width"))
                .orientation(getOrientation(ctx))
                .antialiasingFactor(IntegerArgumentType.getInteger(ctx, "antialiasingFactor")));
    }
    
    private static int exportOrShare(CommandContext<CommandSourceStack> ctx, SchematicRenderSettings.Builder settingsBuilder) {
        String fileName = StringArgumentType.getString(ctx, "fileName");
        var imageHandler = new SchematicImageHandler(ctx.getSource(), fileName, settingsBuilder);

        for (var node : ctx.getNodes()) {
            String nodeName = node.getNode().getName();

            if (nodeName.equals("export")) {
                imageHandler.export();
                break;
            } else if (nodeName.equals("share")) {
                imageHandler.share();
                break;
            }
        }
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
}
