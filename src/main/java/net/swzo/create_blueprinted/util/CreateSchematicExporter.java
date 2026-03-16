package net.swzo.create_blueprinted.util;

import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.pipeline.TextureTarget;
import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import com.simibubi.create.content.schematics.client.SchematicRenderer;
import net.createmod.catnip.levelWrappers.SchematicLevel;
import net.createmod.catnip.render.SuperRenderTypeBuffer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtAccounter;
import net.minecraft.nbt.NbtIo;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.Style;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import net.swzo.create_blueprinted.CB;
import org.jetbrains.annotations.NotNull;
import org.joml.Matrix4f;
import org.joml.Vector3f;

import java.io.File;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public class CreateSchematicExporter {

    public static CompletableFuture<NativeImage> renderSchematicImage(CompoundTag tag, int pixelWidthPerBlock, String orientation) {
        CompletableFuture<NativeImage> renderFuture = new CompletableFuture<>();
        Minecraft mc = Minecraft.getInstance();

        RenderSystem.recordRenderCall(() -> {
            try {
                StructureTemplate template = new StructureTemplate();
                template.load(Objects.requireNonNull(mc.level).holderLookup(Registries.BLOCK), tag);

                Vec3i size = template.getSize();
                int maxDim = Math.max(size.getX(), Math.max(size.getY(), size.getZ()));

                int maxSupportedSize = Math.min(16384, RenderSystem.maxSupportedTextureSize());

                int theoreticalFbSize = maxDim * pixelWidthPerBlock * 4;

                int fbSize;
                float effectivePixelWidth = pixelWidthPerBlock;

                if (theoreticalFbSize > maxSupportedSize) {
                    fbSize = maxSupportedSize;
                    effectivePixelWidth = (float) maxSupportedSize / (maxDim * 4.0f);
                    CB.LOGGER.info("Schematic render exceeds max texture size. Downscaling pixelWidthPerBlock to " + effectivePixelWidth);
                } else {
                    fbSize = Math.max(512, theoreticalFbSize);
                }

                RenderTarget renderTarget = new TextureTarget(fbSize, fbSize, true, Minecraft.ON_OSX);
                renderTarget.setClearColor(0.0F, 0.0F, 0.0F, 0.0F);
                renderTarget.clear(Minecraft.ON_OSX);
                renderTarget.bindWrite(true);

                SchematicLevel schematicLevel = new SchematicLevel(BlockPos.ZERO, mc.level);
                StructurePlaceSettings settings = new StructurePlaceSettings();
                template.placeInWorld(schematicLevel, BlockPos.ZERO, BlockPos.ZERO, settings, mc.level.random, Block.UPDATE_CLIENTS);

                SchematicRenderer renderer = new SchematicRenderer(schematicLevel);

                Matrix4f projectionMatrix = new Matrix4f().setOrtho(-fbSize / 2f, fbSize / 2f, -fbSize / 2f, fbSize / 2f, -10000f, 10000f);
                RenderSystem.setProjectionMatrix(projectionMatrix, RenderSystem.getVertexSorting());

                PoseStack poseStack = new PoseStack();
                poseStack.pushPose();

                Vector3f light0 = new Vector3f(-1.0f, 1.2f, -0.8f).normalize();
                Vector3f light1 = new Vector3f(0.5f, -0.2f, 1.0f).normalize();
                RenderSystem.setShaderLights(light0, light1);

                float scale = effectivePixelWidth / (float) Math.sqrt(2);
                poseStack.scale(scale, scale, scale);

                float yRot = orientation.equalsIgnoreCase("left") ? 45f : -45f;
                poseStack.mulPose(Axis.XP.rotationDegrees(35.264f));
                poseStack.mulPose(Axis.YP.rotationDegrees(yRot));

                poseStack.translate(-size.getX() / 2f, -size.getY() / 2f, -size.getZ() / 2f);

                MultiBufferSource.BufferSource mcBuffers = mc.renderBuffers().bufferSource();
                SuperRenderTypeBuffer buffers = new SuperRenderTypeBuffer() {
                    @Override public @NotNull VertexConsumer getEarlyBuffer(@NotNull RenderType type) { return mcBuffers.getBuffer(type); }
                    @Override public @NotNull VertexConsumer getBuffer(@NotNull RenderType type) { return mcBuffers.getBuffer(type); }
                    @Override public @NotNull VertexConsumer getLateBuffer(@NotNull RenderType type) { return mcBuffers.getBuffer(type); }
                    @Override public void draw() { mcBuffers.endBatch(); }
                    @Override public void draw(@NotNull RenderType type) { mcBuffers.endBatch(type); }
                };

                renderer.render(poseStack, buffers);
                buffers.draw();
                poseStack.popPose();

                NativeImage fullImage = new NativeImage(fbSize, fbSize, false);
                RenderSystem.bindTexture(renderTarget.getColorTextureId());
                fullImage.downloadTexture(0, false);
                fullImage.flipY();

                renderTarget.destroyBuffers();
                mc.getMainRenderTarget().bindWrite(true);

                renderFuture.complete(fullImage);
            } catch (Exception e) {
                renderFuture.completeExceptionally(e);
            }
        });

        return renderFuture.thenApplyAsync(fullImage -> {
            try (fullImage) {
                int fbSize = fullImage.getWidth();
                int minX = fbSize, minY = fbSize, maxX = -1, maxY = -1;

                for (int y = 0; y < fbSize; y++) {
                    for (int x = 0; x < fbSize; x++) {
                        if (((fullImage.getPixelRGBA(x, y) >> 24) & 0xFF) > 0) {
                            if (x < minX) minX = x;
                            if (x > maxX) maxX = x;
                            if (y < minY) minY = y;
                            if (y > maxY) maxY = y;
                        }
                    }
                }

                if (maxX >= minX && maxY >= minY) {
                    int cropWidth = maxX - minX + 1;
                    int cropHeight = maxY - minY + 1;
                    NativeImage croppedImage = new NativeImage(cropWidth, cropHeight, false);

                    for (int y = 0; y < cropHeight; y++) {
                        for (int x = 0; x < cropWidth; x++) {
                            croppedImage.setPixelRGBA(x, y, fullImage.getPixelRGBA(minX + x, minY + y));
                        }
                    }
                    return croppedImage;
                } else {
                    throw new RuntimeException("Schematic render was entirely transparent.");
                }
            } catch (Exception e) {
                throw new RuntimeException("Failed to process and crop image.", e);
            }
        });
    }

    public static void export(CommandSourceStack source, String filename, String orientation, int pixelWidthPerBlock) {
        Minecraft mc = Minecraft.getInstance();
        Path schematicPath = mc.gameDirectory.toPath().resolve("schematics").resolve(filename + ".nbt");

        if (!Files.exists(schematicPath)) {
            source.sendFailure(Component.translatable("create_blueprinted.command.renderschem.not_found", filename)
                    .withColor(UiHelpers.ERROR_PRIMARY));
            return;
        }

        source.sendSuccess(() -> Component.translatable("create_blueprinted.command.renderschem.starting", filename)
                .withColor(UiHelpers.LIGHT_BLUE_TEXT_COLOR), false);

        CompletableFuture.supplyAsync(() -> {
                    try (InputStream inputStream = Files.newInputStream(schematicPath)) {
                        return NbtIo.readCompressed(inputStream, NbtAccounter.unlimitedHeap());
                    } catch (Exception e) {
                        throw new RuntimeException("Failed to read NBT file.", e);
                    }
                })
                .thenComposeAsync(tag -> renderSchematicImage(tag, pixelWidthPerBlock, orientation))
                .thenAcceptAsync(croppedImage -> {
                    try (croppedImage) {
                        File outputFile = new File(mc.gameDirectory, "schematics/" + filename + ".png");
                        croppedImage.writeToFile(outputFile);

                        mc.execute(() -> {
                            Component successMsg = Component.translatable("create_blueprinted.command.renderschem.success")
                                    .withColor(UiHelpers.LIGHT_BLUE_TEXT_COLOR)
                                    .append(Component.literal(outputFile.getName())
                                            .withStyle(Style.EMPTY
                                                    .withColor(UiHelpers.DARK_BLUE_TEXT_COLOR)
                                                    .withUnderlined(true)
                                                    .withClickEvent(new ClickEvent(ClickEvent.Action.OPEN_FILE, outputFile.getAbsolutePath()))
                                                    .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, Component.translatable("create_blueprinted.command.renderschem.click_to_open")))));
                            source.sendSuccess(() -> successMsg, false);
                        });
                    } catch (Exception e) {
                        throw new RuntimeException("Failed to save cropped image.", e);
                    }
                }).orTimeout(1, TimeUnit.MINUTES).exceptionally(ex -> {
                    mc.execute(() -> {
                        if (ex instanceof TimeoutException || ex.getCause() instanceof TimeoutException) {
                            source.sendFailure(Component.translatable("create_blueprinted.command.renderschem.timeout")
                                    .withColor(UiHelpers.ERROR_PRIMARY));
                        } else {
                            source.sendFailure(Component.translatable("create_blueprinted.command.renderschem.error", ex.getMessage())
                                    .withColor(UiHelpers.ERROR_PRIMARY));
                            CB.LOGGER.error("Exception Thrown: ", ex);
                        }
                    });
                    return null;
                });
    }
}