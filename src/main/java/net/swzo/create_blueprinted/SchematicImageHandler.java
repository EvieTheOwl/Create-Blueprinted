package net.swzo.create_blueprinted;

import com.mojang.blaze3d.platform.NativeImage;
import com.simibubi.create.CreateClient;
import net.createmod.catnip.levelWrappers.SchematicLevel;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.*;
import net.minecraft.util.PngInfo;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import net.neoforged.neoforge.common.NeoForge;
import net.swzo.create_blueprinted.api.RenderSchematicImageEvent;
import net.swzo.create_blueprinted.api.RenderSchematicImageEvent.Action;
import net.swzo.create_blueprinted.api.SchematicRenderSettings;
import net.swzo.create_blueprinted.exception.EmptyImageBakeException;
import net.swzo.create_blueprinted.exception.EventCancelledException;
import net.swzo.create_blueprinted.exception.SchematicImageRenderException;
import net.swzo.create_blueprinted.renderers.SchematicImageRenderer;
import net.swzo.create_blueprinted.util.DebugTimer;
import net.swzo.create_blueprinted.util.FileUtils;
import net.swzo.create_blueprinted.util.SchematicUtils;
import net.swzo.create_blueprinted.util.UiHelpers;

import javax.annotation.Nullable;
import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Map;
import java.util.concurrent.*;
import java.util.function.Supplier;
import java.util.stream.Stream;

import static net.swzo.create_blueprinted.CreateBlueprinted.*;
import static net.swzo.create_blueprinted.renderers.SchematicImageRenderer.downsample;
import static net.swzo.create_blueprinted.util.ThreadUtils.onClientThread;
import static net.swzo.create_blueprinted.util.ThreadUtils.onRenderThread;

public class SchematicImageHandler {

    private static final int RENDER_TIMEOUT_SECS = 60;

    private static final Component RENDER_ERROR = translatableError("schematic_render");
    private static final Component EXPORT_ERROR = translatableError("schematic_export");
    private static final Component RENDER_FAILED = translatableError("schematic_render.render_failed");
    private static final Component EMPTY_IMAGE_BAKE = translatableError("schematic_render.empty_image_bake");
    private static final Component CONVERT_AND_VALIDATE_FAILED = translatableError("schematic_render.convert_and_validate_failed");
    private static final Component TIMED_OUT = translatableError("schematic_render.timed_out", RENDER_TIMEOUT_SECS);
    private static final Component CLICK_TO_OPEN = translatable("command.renderschem.click_to_open");

    private static final ExecutorService PIPELINE = Executors.newSingleThreadExecutor(runnable -> {
        Thread thread = new Thread(runnable, "create-blueprinted-image-handler");
        thread.setDaemon(true);
        return thread;
    });

    private final CommandSourceStack source;
    private final String fileName;
    private final SchematicRenderSettings.Builder settingsBuilder;
    private final SchematicLevel schematicLevel;
    private @Nullable Supplier<SchematicImageRenderer> renderSupplier;

    public SchematicImageHandler(CommandSourceStack source, String fileName, SchematicRenderSettings.Builder settingsBuilder) {
        this.source = source;
        this.fileName = fileName;
        this.settingsBuilder = settingsBuilder;

        ClientLevel level = Minecraft.getInstance().level;
        if (level == null)
            throw new IllegalStateException("Cannot construct schematic image handler without a loaded level.");

        this.schematicLevel = new SchematicLevel(BlockPos.ZERO, level);
    }

    private void attachToSchematicName() {
        StructureTemplate template = SchematicUtils.loadTemplateFromSchematic(fileName);
        this.renderSupplier = () -> SchematicImageRenderer.bakeFromTemplate(template, schematicLevel)
                .orElseThrow(() -> new EmptyImageBakeException("Structure template is empty."));
    }

    public void attachToBlockList(Map<BlockPos, StructureTemplate.StructureBlockInfo> blocks) {
        renderSupplier = () -> SchematicImageRenderer.bakeFromBlocks(blocks, schematicLevel)
                .orElseThrow(() -> new EmptyImageBakeException("List of structure template blocks is empty."));
    }

    public void export() {
        Minecraft client = Minecraft.getInstance();
        renderAndDownsample(Action.EXPORT)
                .thenApplyAsync(imageByteArray -> processExport(imageByteArray, client), PIPELINE)
                .handle((path, e) -> onExportFinish(path, e, client));
    }

    public void share() {
        renderAndDownsample(Action.SHARE);

    }

    public static Stream<String> getAllSchematicNames() {
        return CreateClient.SCHEMATIC_SENDER.getAvailableSchematics().stream().map(Component::getString);
    }

    private CompletableFuture<byte[]> renderAndDownsample(Action action) {
        if (renderSupplier == null) attachToSchematicName();

        SchematicRenderSettings settings = settingsBuilder.build();
        int ssaa = settings.antialiasingFactor();

        DEBUG_TIMER.createTask("image-export", "Before schematic bake");

        Minecraft client = Minecraft.getInstance();
        return CompletableFuture.supplyAsync(() -> renderSupplier.get(), PIPELINE)
                .thenCompose(renderer -> firePreRenderEvent(renderer, action, client))
                .thenCompose(renderer -> onRenderThread(() -> renderer.render(settingsBuilder.build())))
                .thenApplyAsync(image -> ssaa == 1 ? image : downsample(image, ssaa), PIPELINE)
                .thenApply(this::convertToByteArray)
                .thenCompose(imageByteArray -> firePostRenderEvent(imageByteArray, action, client))
                .orTimeout(RENDER_TIMEOUT_SECS, TimeUnit.MINUTES)
                .handle((imageByteArray, e) -> handleRenderExceptions(imageByteArray, e, client));
    }

    private CompletableFuture<Void> onExportFinish(@Nullable Path outputFile, @Nullable Throwable e, Minecraft client) {
        Throwable cause = getExceptionCause(e);

        DEBUG_TIMER.finishAndPrint("Export finished");

        if (outputFile == null || cause != null) {
            if (cause instanceof IOException) client.execute(() -> source.sendFailure(EXPORT_ERROR));
            if (cause != null) LOGGER.error("Failed to export schematic image", e);
            return CompletableFuture.completedFuture(null);
        }
        client.execute(() -> {
            Component successMsg = translatable("command.renderschem.success")
                    .withColor(UiHelpers.LIGHT_BLUE_TEXT_COLOR)
                    .append(Component.literal(outputFile.toString())
                            .withStyle(Style.EMPTY
                                    .withColor(UiHelpers.DARK_BLUE_TEXT_COLOR)
                                    .withUnderlined(true)
                                    .withClickEvent(new ClickEvent(ClickEvent.Action.OPEN_FILE, outputFile.toString()))
                                    .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, CLICK_TO_OPEN))));
            source.sendSuccess(() -> successMsg, false);
        });
        return CompletableFuture.completedFuture(null);
    }

    private Path processExport(byte[] imageByteArray, Minecraft client) {
        DEBUG_TIMER.markInstant("Before export");

        if (imageByteArray == null) return null;
        String gameDirectory = client.gameDirectory.getPath();
        File schematicDirectory = new File(gameDirectory, "schematics");

        try {
            return FileUtils.saveImage(schematicDirectory, fileName, "png", imageByteArray);
        } catch (IOException e) {
            throw new CompletionException(e);
        }
    }

    private byte[] convertToByteArray(NativeImage image) {
        DEBUG_TIMER.markInstant("Before convert to byte array");
        byte [] imageByteArray;
        try (image) {
            imageByteArray = image.asByteArray();
            PngInfo.fromBytes(imageByteArray);
        } catch (IOException e) {
            throw new CompletionException(e);
        }
        return imageByteArray;
    }

    private CompletableFuture<SchematicImageRenderer> firePreRenderEvent(SchematicImageRenderer renderer, Action action, Minecraft client) {
        return onClientThread(() -> {
            SchematicLevel schematicLevel = renderer.getSchematicLevel();
            var renderEvent = new RenderSchematicImageEvent.Pre(fileName, schematicLevel, settingsBuilder, action);

            NeoForge.EVENT_BUS.post(renderEvent);
            if (renderEvent.isCanceled()) throw new EventCancelledException();
            return renderer;
        }, client);
    }

    private CompletableFuture<byte[]> firePostRenderEvent(byte[] imageByteArray, Action action, Minecraft client) {
        return onClientThread(() -> {
            var renderEvent = new RenderSchematicImageEvent.Post(fileName, imageByteArray, settingsBuilder, action);

            NeoForge.EVENT_BUS.post(renderEvent);
            if (renderEvent.isCanceled()) throw new EventCancelledException();
            return imageByteArray;
        }, client);
    }

    private byte[] handleRenderExceptions(@Nullable byte[] imageByteArray, @Nullable Throwable e, Minecraft client) {
        Throwable cause = getExceptionCause(e);
        if (cause == null) return imageByteArray;
        else if (cause instanceof EventCancelledException) return null;

        client.execute(() -> {
            MutableComponent renderError = RENDER_ERROR.copy().append(" ");

            if (cause instanceof EmptyImageBakeException)
                source.sendFailure(renderError.append(EMPTY_IMAGE_BAKE));
            else if (cause instanceof SchematicImageRenderException)
                source.sendFailure(renderError.append(RENDER_FAILED));
            else if (cause instanceof IOException)
                source.sendFailure(renderError.append(CONVERT_AND_VALIDATE_FAILED));
            else if (cause instanceof TimeoutException)
                source.sendFailure(renderError.append(TIMED_OUT));
        });
        CreateBlueprinted.LOGGER.error("Failed to render schematic", e);
        return imageByteArray;
    }

    private static Throwable getExceptionCause(@Nullable Throwable e) {
        return (e instanceof CompletionException && e.getCause() != null) ? e.getCause() : e;
    }
}
