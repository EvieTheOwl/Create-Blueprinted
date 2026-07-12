package net.swzo.create_blueprinted.handler;

import com.mojang.blaze3d.platform.NativeImage;
import net.createmod.catnip.levelWrappers.SchematicLevel;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.PngInfo;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import net.neoforged.neoforge.common.NeoForge;
import net.swzo.create_blueprinted.CreateBlueprinted;
import net.swzo.create_blueprinted.api.RenderSchematicImageEvent;
import net.swzo.create_blueprinted.api.RenderSchematicImageEvent.Action;
import net.swzo.create_blueprinted.render.ImageActionProgress;
import net.swzo.create_blueprinted.render.SchematicRenderSettings;
import net.swzo.create_blueprinted.api.ShareProvider;
import net.swzo.create_blueprinted.api.ShareProviderRegistry;
import net.swzo.create_blueprinted.api.exception.EmptyImageBakeException;
import net.swzo.create_blueprinted.api.exception.EventCancelledException;
import net.swzo.create_blueprinted.api.exception.SchematicImageRenderException;
import net.swzo.create_blueprinted.render.SchematicImageRenderer;
import net.swzo.create_blueprinted.util.IOUtils;
import net.swzo.create_blueprinted.util.SchematicUtils;
import net.swzo.create_blueprinted.util.UIHelpers;

import javax.annotation.Nullable;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.Map;
import java.util.concurrent.*;
import java.util.function.Supplier;

import static net.swzo.create_blueprinted.CreateBlueprinted.*;
import static net.swzo.create_blueprinted.render.SchematicImageRenderer.downsample;
import static net.swzo.create_blueprinted.util.ThreadUtils.onClientThread;
import static net.swzo.create_blueprinted.util.ThreadUtils.onRenderThread;

public class SchematicImageHandler {

    public static final int RENDER_TIMEOUT_SECS = 60;

    private static final Component RENDER_ERROR = translatableError("schematic_render");
    private static final Component EXPORT_ERROR = translatableError("schematic_export");
    private static final Component RENDER_FAILED = translatableError("schematic_render.render_failed");
    private static final Component EMPTY_IMAGE_BAKE = translatableError("schematic_render.empty_image_bake");
    private static final Component CONVERT_AND_VALIDATE_FAILED = translatableError("schematic_render.convert_and_validate_failed");
    private static final Component TIMED_OUT = translatableError("schematic_render.timed_out", RENDER_TIMEOUT_SECS);
    private static final Component CLICK_TO_OPEN = translatable("command.renderschem.click_to_open");

    public static final ExecutorService PIPELINE = Executors.newSingleThreadExecutor(runnable -> {
        Thread thread = new Thread(runnable, "create-blueprinted-image-handler");
        thread.setDaemon(true);
        return thread;
    });

    private final ResourceLocation handlerId;
    private final CommandSourceStack source;
    private final String schematicName;
    private final SchematicRenderSettings.Builder settingsBuilder;
    private final SchematicLevel schematicLevel;
    private final ShareProvider shareProvider;

    private @Nullable Supplier<SchematicImageRenderer> renderSupplier;

    public SchematicImageHandler(String schematicName, CommandSourceStack source, SchematicRenderSettings.Builder settingsBuilder) {
        this(rl("default"), schematicName, source, settingsBuilder);
    }

    public SchematicImageHandler(ResourceLocation handlerId, String schematicName, CommandSourceStack source, SchematicRenderSettings.Builder settingsBuilder) {
        this.handlerId = handlerId;
        this.source = source;
        this.schematicName = schematicName;
        this.settingsBuilder = settingsBuilder;
        this.shareProvider = ShareProviderRegistry.getMainProvider().orElse(null);

        ClientLevel level = Minecraft.getInstance().level;
        if (level == null)
            throw new IllegalStateException("Cannot construct schematic image handler without a loaded level.");

        this.schematicLevel = new SchematicLevel(BlockPos.ZERO, level);
    }

    private void attachToSchematicName() {
        StructureTemplate template = SchematicUtils.loadTemplateFromSchematicName(schematicName);
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
                .whenComplete((file, e) -> onExportFinish(file, e, client));
    }

    public void share() {
        if (shareProvider == null) return;

        Minecraft client = Minecraft.getInstance();
        renderAndDownsample(Action.SHARE)
                .whenComplete((imageByteArray, __) -> onShareFinish(imageByteArray, client));
    }

    private CompletableFuture<byte[]> renderAndDownsample(Action action) {
        if (renderSupplier == null) attachToSchematicName();

        SchematicRenderSettings settings = settingsBuilder.build();
        int ssaa = settings.antialiasingFactor();

        ImageActionProgress.start(schematicName);

        Minecraft client = Minecraft.getInstance();
        return CompletableFuture.supplyAsync(() -> renderSupplier.get(), PIPELINE)
                .thenCompose(renderer -> firePreRenderEvent(renderer, action, client))
                .thenCompose(renderer -> onRenderThread(() -> renderer.render(settingsBuilder.build())))
                .thenApplyAsync(image -> ssaa == 1 ? image : downsample(image, ssaa), PIPELINE)
                .thenApply(this::convertToByteArray)
                .thenCompose(imageByteArray -> firePostRenderEvent(imageByteArray, action, client))
                .orTimeout(RENDER_TIMEOUT_SECS, TimeUnit.SECONDS)
                .handle((imageByteArray, e) -> handleRenderExceptions(imageByteArray, e, action, client));
    }

    private File processExport(byte[] imageByteArray, Minecraft client) {
        if (imageByteArray == null) return null;
        String gameDirectory = client.gameDirectory.getPath();
        File schematicDirectory = new File(gameDirectory, "schematics");

        ImageActionProgress.setState(ImageActionProgress.EXPORTING);
        try {
            return IOUtils.saveImage(schematicDirectory, schematicName, "png", imageByteArray);
        } catch (IOException e) {
            throw new CompletionException(e);
        }
    }

    private void onShareFinish(@Nullable byte[] imageByteArray, Minecraft client) {
        if (imageByteArray == null) return;
        ImageActionProgress.setState(ImageActionProgress.SHARING);

        client.execute(() -> {
            URL url = shareProvider.onRender(handlerId, schematicName, settingsBuilder.build(), imageByteArray);
            if (url != null) {
                ImageActionProgress.setState(ImageActionProgress.SHARED);
                LOGGER.info("Schematic image {} sent to: {}", schematicName, url);
            } else {
                ImageActionProgress.setState(ImageActionProgress.SHARE_FAILED);
                LOGGER.error("Failed to share schematic image {}", schematicName);
            }
        });
    }

    private void onExportFinish(@Nullable File outputFile, @Nullable Throwable e, Minecraft client) {
        if (outputFile == null) return; // Event canceled or rendering failed
        Throwable cause = getExceptionCause(e);

        if (cause != null) {
            if (cause instanceof IOException) client.execute(() -> source.sendFailure(EXPORT_ERROR));
            ImageActionProgress.setState(ImageActionProgress.EXPORT_FAILED);
            LOGGER.error("Failed to export schematic image {}", schematicName, e);
            return;
        }
        client.execute(() -> {
            Component finalMessage = translatable("command.renderschem.success")
                    .withColor(UIHelpers.LIGHT_BLUE_TEXT_COLOR)
                    .append(Component.literal(outputFile.toString())
                            .withStyle(Style.EMPTY
                                    .withColor(UIHelpers.DARK_BLUE_TEXT_COLOR)
                                    .withUnderlined(true)
                                    .withClickEvent(new ClickEvent(ClickEvent.Action.OPEN_FILE, outputFile.getAbsolutePath()))
                                    .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, CLICK_TO_OPEN))));
            source.sendSuccess(() -> finalMessage, false);
        });
        ImageActionProgress.setState(ImageActionProgress.EXPORTED);
    }

    private byte[] convertToByteArray(NativeImage image) {
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
            var renderEvent = new RenderSchematicImageEvent.Pre(handlerId, schematicName, settingsBuilder, action, schematicLevel);

            NeoForge.EVENT_BUS.post(renderEvent);
            if (renderEvent.isCanceled()) throw new EventCancelledException();
            return renderer;
        }, client);
    }

    private CompletableFuture<byte[]> firePostRenderEvent(byte[] imageByteArray, Action action, Minecraft client) {
        return onClientThread(() -> {
            var renderEvent = new RenderSchematicImageEvent.Post(handlerId, schematicName, settingsBuilder, action, imageByteArray);

            NeoForge.EVENT_BUS.post(renderEvent);
            if (renderEvent.isCanceled()) throw new EventCancelledException();
            return imageByteArray;
        }, client);
    }

    private byte[] handleRenderExceptions(@Nullable byte[] imageByteArray, @Nullable Throwable e, Action action, Minecraft client) {
        Throwable cause = getExceptionCause(e);
        if (cause == null) return imageByteArray;
        if (cause instanceof EventCancelledException) {
            ImageActionProgress.cancel();
            return null;
        }
        ImageActionProgress.setState(ImageActionProgress.RENDER_FAILED);
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

            if (action == Action.SHARE)
                shareProvider.onRenderFailure(handlerId, schematicName, settingsBuilder.build(), cause, renderError);
        });
        CreateBlueprinted.LOGGER.error("Failed to render schematic", e);
        return null;
    }

    private static Throwable getExceptionCause(@Nullable Throwable e) {
        return (e instanceof CompletionException && e.getCause() != null) ? e.getCause() : e;
    }
}
