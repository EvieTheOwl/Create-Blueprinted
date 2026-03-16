package net.swzo.create_blueprinted.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.minecraft.client.Minecraft;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.swzo.create_blueprinted.util.CreateSchematicExporter;

import java.io.File;
import java.util.Objects;

public class RenderSchemCommand {
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("renderschem")
                .then(Commands.argument("filename", StringArgumentType.string())
                        .suggests((context, builder) -> {
                            File dir = new File(Minecraft.getInstance().gameDirectory, "schematics");
                            if (dir.exists() && dir.isDirectory()) {
                                for (File file : Objects.requireNonNull(dir.listFiles())) {
                                    if (file.getName().endsWith(".nbt")) {
                                        builder.suggest(file.getName().replace(".nbt", ""));
                                    }
                                }
                            }
                            return builder.buildFuture();
                        })
                        .executes(context -> {
                            String filename = StringArgumentType.getString(context, "filename");
                            CreateSchematicExporter.export(context.getSource(), filename, "right", 256);
                            return 1;
                        })
                        .then(Commands.argument("orientation", StringArgumentType.word())
                                .suggests((context, builder) -> SharedSuggestionProvider.suggest(new String[]{"left", "right"}, builder))
                                .executes(context -> {
                                    String filename = StringArgumentType.getString(context, "filename");
                                    String orientation = StringArgumentType.getString(context, "orientation");
                                    CreateSchematicExporter.export(context.getSource(), filename, orientation, 256);
                                    return 1;
                                })
                                .then(Commands.argument("quality", IntegerArgumentType.integer(1, 2048))
                                        .executes(context -> {
                                            String filename = StringArgumentType.getString(context, "filename");
                                            String orientation = StringArgumentType.getString(context, "orientation");
                                            int quality = IntegerArgumentType.getInteger(context, "quality");
                                            CreateSchematicExporter.export(context.getSource(), filename, orientation, quality);
                                            return 1;
                                        })
                                )
                        )
                )
        );
    }
}
