package net.swzo.create_blueprinted.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.FloatArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.suggestion.Suggestions;
import net.minecraft.client.Minecraft;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.swzo.create_blueprinted.render.SchematicRenderSettings;
import net.swzo.create_blueprinted.util.CreateSchematicExporter;

import java.io.File;
import java.util.Arrays;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;

public class RenderSchemCommand {

    private static final int DEFAULT_WIDTH = SchematicRenderSettings.DEFAULT_IMAGE_WIDTH;
    private static final int MIN_WIDTH = 64;
    private static final int MAX_WIDTH = 8192;

    private static final int DEFAULT_AA = SchematicRenderSettings.DEFAULT_ANTIALIASING;
    private static final int MAX_AA = SchematicRenderSettings.MAX_ANTIALIASING;

    private static final String[] VIEW_NAMES = Arrays.stream(SchematicRenderSettings.View.values())
            .map(v -> v.name().toLowerCase())
            .toArray(String[]::new);

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("renderschem")
                .then(Commands.argument("filename", StringArgumentType.string())
                        .suggests((context, builder) -> suggestSchematics(builder))
                        .executes(context -> render(context.getSource(),
                                StringArgumentType.getString(context, "filename"),
                                SchematicRenderSettings.builder().imageWidth(DEFAULT_WIDTH).antialiasing(DEFAULT_AA).build()))
                        .then(Commands.argument("view", StringArgumentType.word())
                                .suggests((context, builder) -> SharedSuggestionProvider.suggest(VIEW_NAMES, builder))
                                .executes(context -> render(context.getSource(),
                                        StringArgumentType.getString(context, "filename"),
                                        viewSettings(context, DEFAULT_WIDTH, DEFAULT_AA)))
                                .then(Commands.argument("width", IntegerArgumentType.integer(MIN_WIDTH, MAX_WIDTH))
                                        .executes(context -> render(context.getSource(),
                                                StringArgumentType.getString(context, "filename"),
                                                viewSettings(context, IntegerArgumentType.getInteger(context, "width"), DEFAULT_AA)))
                                        .then(Commands.argument("antialiasing", IntegerArgumentType.integer(1, MAX_AA))
                                                .executes(context -> render(context.getSource(),
                                                        StringArgumentType.getString(context, "filename"),
                                                        viewSettings(context, IntegerArgumentType.getInteger(context, "width"),
                                                                IntegerArgumentType.getInteger(context, "antialiasing")))))))
                        .then(Commands.literal("angle")
                                .then(Commands.argument("yaw", FloatArgumentType.floatArg())
                                        .then(Commands.argument("pitch", FloatArgumentType.floatArg())
                                                .executes(context -> render(context.getSource(),
                                                        StringArgumentType.getString(context, "filename"),
                                                        angleSettings(context, 0f, DEFAULT_WIDTH, DEFAULT_AA)))
                                                .then(rollArg()))))));
    }

    private static RequiredArgumentBuilder<CommandSourceStack, Float> rollArg() {
        return Commands.argument("roll", FloatArgumentType.floatArg())
                .executes(context -> render(context.getSource(),
                        StringArgumentType.getString(context, "filename"),
                        angleSettings(context, FloatArgumentType.getFloat(context, "roll"), DEFAULT_WIDTH, DEFAULT_AA)))
                .then(Commands.argument("width", IntegerArgumentType.integer(MIN_WIDTH, MAX_WIDTH))
                        .executes(context -> render(context.getSource(),
                                StringArgumentType.getString(context, "filename"),
                                angleSettings(context, FloatArgumentType.getFloat(context, "roll"),
                                        IntegerArgumentType.getInteger(context, "width"), DEFAULT_AA)))
                        .then(Commands.argument("antialiasing", IntegerArgumentType.integer(1, MAX_AA))
                                .executes(context -> render(context.getSource(),
                                        StringArgumentType.getString(context, "filename"),
                                        angleSettings(context, FloatArgumentType.getFloat(context, "roll"),
                                                IntegerArgumentType.getInteger(context, "width"),
                                                IntegerArgumentType.getInteger(context, "antialiasing"))))));
    }

    private static SchematicRenderSettings viewSettings(com.mojang.brigadier.context.CommandContext<CommandSourceStack> context, int width, int antialiasing) {
        SchematicRenderSettings.View view = SchematicRenderSettings.parseView(StringArgumentType.getString(context, "view"));
        return SchematicRenderSettings.builder().view(view).imageWidth(width).antialiasing(antialiasing).build();
    }

    private static SchematicRenderSettings angleSettings(com.mojang.brigadier.context.CommandContext<CommandSourceStack> context, float roll, int width, int antialiasing) {
        float yaw = FloatArgumentType.getFloat(context, "yaw");
        float pitch = FloatArgumentType.getFloat(context, "pitch");
        return SchematicRenderSettings.builder().angles(yaw, pitch, roll).imageWidth(width).antialiasing(antialiasing).build();
    }

    private static int render(CommandSourceStack source, String filename, SchematicRenderSettings settings) {
        CreateSchematicExporter.export(source, filename, settings);
        return 1;
    }

    private static CompletableFuture<Suggestions> suggestSchematics(com.mojang.brigadier.suggestion.SuggestionsBuilder builder) {
        File dir = new File(Minecraft.getInstance().gameDirectory, "schematics");
        if (dir.exists() && dir.isDirectory()) {
            for (File file : Objects.requireNonNull(dir.listFiles())) {
                if (file.getName().endsWith(".nbt")) {
                    builder.suggest(file.getName().replace(".nbt", ""));
                }
            }
        }
        return builder.buildFuture();
    }
}
