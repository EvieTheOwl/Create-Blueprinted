package net.swzo.create_blueprinted.command;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import net.createmod.catnip.theme.Color;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.arguments.ColorArgument;
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
                .then(literal("export")
                        .then(argument("fileName", StringArgumentType.string())
                                .suggests((ctx, builder) -> suggestSchematics(builder))
                                .executes(CBSchematicCommands::exportDefault))
                        .then(argument("width", IntegerArgumentType.integer(MIN_WIDTH, MAX_WIDTH))
                                .executes(CBSchematicCommands::exportWithWidth))
                        .then(argument("rotation", RotationArgument.rotation())
                                .executes(CBSchematicCommands::exportWithOrientation))
                        .then(argument("antialiasingFactor", IntegerArgumentType.integer(1, MAX_ANTIALIASING))
                                .executes(CBSchematicCommands::exportWithAntialiasingFactor))
                        .then(argument("backgroundColor", ColorArgument.color())
                                .executes(CBSchematicCommands::exportWithBackground))
                ).then(literal("share")
                        .then(argument("fileName", StringArgumentType.string())

                        )
                )
        );
    }

    private static int exportDefault(CommandContext<CommandSourceStack> ctx) {
        return export(ctx, SchematicRenderSettings.builder().imageWidth(DEFAULT_WIDTH));
    }

    private static int exportWithWidth(CommandContext<CommandSourceStack> ctx) {
        return export(ctx, SchematicRenderSettings.builder()
                .imageWidth(IntegerArgumentType.getInteger(ctx, "width")));
    }

    private static int exportWithOrientation(CommandContext<CommandSourceStack> ctx) {
        return export(ctx, SchematicRenderSettings.builder()
                .imageWidth(IntegerArgumentType.getInteger(ctx, "width"))
                .orientation(getOrientation(ctx)));
    }

    private static int exportWithAntialiasingFactor(CommandContext<CommandSourceStack> ctx) {
        return export(ctx, SchematicRenderSettings.builder()
                .imageWidth(IntegerArgumentType.getInteger(ctx, "width"))
                .orientation(getOrientation(ctx))
                .antialiasingFactor(IntegerArgumentType.getInteger(ctx, "antialiasingFactor")));
    }

    private static int exportWithBackground(CommandContext<CommandSourceStack> ctx) {
        return export(ctx, SchematicRenderSettings.builder()
                .imageWidth(IntegerArgumentType.getInteger(ctx, "width"))
                .orientation(getOrientation(ctx))
                .antialiasingFactor(IntegerArgumentType.getInteger(ctx, "antialiasingFactor"))
                .backgroundColor(getColor(ctx)));
    }
    
    private static int export(CommandContext<CommandSourceStack> ctx, SchematicRenderSettings.Builder settingsBuilder) {
        String fileName = StringArgumentType.getString(ctx, "fileName");
        var imageHandler = new SchematicImageHandler(ctx.getSource(), fileName, settingsBuilder);
        imageHandler.export();
        return Command.SINGLE_SUCCESS;
    }

    private static Orientation getOrientation(CommandContext<CommandSourceStack> ctx) {
        Coordinates coordinates = RotationArgument.getRotation(ctx, "rotation");
        Vec2 rotation = coordinates.getRotation(ctx.getSource());
        return new Orientation(rotation.y, rotation.x);
    }

    private static Color getColor(CommandContext<CommandSourceStack> ctx) {
        Integer colorInt = ColorArgument.getColor(ctx, "backgroundColor").getColor();
        return colorInt != null ? new Color(colorInt) : DEFAULT_BG_COLOR;
    }

    private static CompletableFuture<Suggestions> suggestSchematics(SuggestionsBuilder builder) {
        SchematicUtils.getAllSchematicNames().forEach(builder::suggest);
        return builder.buildFuture();
    }
}
