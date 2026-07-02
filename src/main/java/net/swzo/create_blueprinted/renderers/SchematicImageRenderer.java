package net.swzo.create_blueprinted.renderers;

import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.pipeline.TextureTarget;
import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.platform.Lighting;
import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexSorting;
import com.mojang.math.Axis;
import com.simibubi.create.content.schematics.client.SchematicRenderer;
import net.createmod.catnip.levelWrappers.SchematicLevel;
import net.createmod.catnip.render.DefaultSuperRenderTypeBuffer;
import net.createmod.catnip.render.SuperRenderTypeBuffer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.ItemBlockRenderTypes;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.BlockRenderDispatcher;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import net.minecraft.world.level.material.FluidState;
import net.swzo.create_blueprinted.CreateBlueprinted;
import net.swzo.create_blueprinted.api.SchematicRenderSettings;
import net.swzo.create_blueprinted.api.SchematicRenderSettings.Orientation;
import net.swzo.create_blueprinted.exception.SchematicImageRenderException;
import net.swzo.create_blueprinted.util.DebugTimer;
import org.jetbrains.annotations.NotNull;
import org.joml.Matrix3f;
import org.joml.Matrix4f;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static net.swzo.create_blueprinted.CreateBlueprinted.DEBUG_TIMER;

public final class SchematicImageRenderer {

    private static final int MARGIN_PX = 4;
    private static final int MAX_FB_SIZE = 16384;
    private static final float DEFAULT_NEAR_AND_FAR = 10_000f;

    private final SchematicLevel schematicLevel;
    private final List<BlockPos> fluidPositions;
    private final PoseAppliedVertexConsumer fluidConsumer;

    private RenderTarget renderTarget;
    private int targetW, targetH;

    private SchematicImageRenderer(SchematicLevel schematicLevel, List<BlockPos> fluidPositions) {
        this.schematicLevel = schematicLevel;
        this.fluidPositions = fluidPositions;
        this.fluidConsumer = new PoseAppliedVertexConsumer();
    }

    public static Optional<SchematicImageRenderer> bakeFromBlocks(Map<BlockPos, StructureTemplate.StructureBlockInfo> blocks, SchematicLevel schematicLevel) {
        if (blocks.isEmpty()) return Optional.empty();
        List<BlockPos> fluidPositions = new ArrayList<>();

        for (var entry : blocks.entrySet()) {
            BlockPos pos = entry.getKey();
            StructureTemplate.StructureBlockInfo blockInfo = entry.getValue();
            BlockState state = blockInfo.state();

            if (state.isAir()) continue;
            if (!state.getFluidState().isEmpty()) fluidPositions.add(pos);
            schematicLevel.setBlock(pos, blockInfo.state(), Block.UPDATE_CLIENTS);

            CompoundTag tag = blockInfo.nbt();
            if (tag != null) {
                var blockEntity = schematicLevel.getBlockEntity(pos);
                blockEntity.loadWithComponents(tag, schematicLevel.registryAccess());
            }
        }
        return Optional.of(new SchematicImageRenderer(schematicLevel, fluidPositions));
    }

    public static Optional<SchematicImageRenderer> bakeFromTemplate(StructureTemplate template, SchematicLevel schematicLevel) {
        var structurePlaceSettings = new StructurePlaceSettings();

        boolean placedInWorld = template.placeInWorld(schematicLevel, BlockPos.ZERO, BlockPos.ZERO,
                structurePlaceSettings, schematicLevel.random, Block.UPDATE_CLIENTS);
        if (!placedInWorld) return Optional.empty();

        List<BlockPos> fluidPositions = new ArrayList<>();
        for (var blockEntry : schematicLevel.getBlockMap().entrySet()) {
            BlockPos pos = blockEntry.getKey();
            BlockState state = blockEntry.getValue();

            if (!state.isAir() && !state.getFluidState().isEmpty())
                fluidPositions.add(pos.immutable());
        }
        return Optional.of(new SchematicImageRenderer(schematicLevel, fluidPositions));
    }

    public @NotNull NativeImage render(SchematicRenderSettings settings) throws SchematicImageRenderException {
        DEBUG_TIMER.markInstant("After bake");

        RenderSystem.assertOnRenderThread();
        BoundingBox bounds = schematicLevel.getBounds();

        float minX = bounds.minX(), minY = bounds.minY(), minZ = bounds.minZ();
        float maxX = bounds.maxX() + 1f, maxY = bounds.maxY() + 1f, maxZ = bounds.maxZ() + 1f;
        float centerX = (minX + maxX) / 2f, centreY = (minY + maxY) / 2f, centreZ = (minZ + maxZ) / 2f;
        float hx = (maxX - minX) / 2f, hy = (maxY - minY) / 2f, hz = (maxZ - minZ) / 2f;

        Matrix4f rot = new Matrix4f();
        Orientation orientation = settings.orientation();
        rot.rotateX((float) Math.toRadians(orientation.pitch()));
        rot.rotateY((float) Math.toRadians(orientation.yaw()));
        rot.rotateZ((float) Math.toRadians(orientation.roll()));

        float maxPX = 0f, maxPY = 0f;
        for (int sx = -1; sx <= 1; sx += 2) {
            for (int sy = -1; sy <= 1; sy += 2) {
                for (int sz = -1; sz <= 1; sz += 2) {
                    Vector3f v = rot.transformPosition(new Vector3f(sx * hx, sy * hy, sz * hz));
                    maxPX = Math.max(maxPX, Math.abs(v.x()));
                    maxPY = Math.max(maxPY, Math.abs(v.y()));
                }
            }
        }
        float projW = Math.max(1e-3f, 2f * maxPX);
        float projH = Math.max(1e-3f, 2f * maxPY);

        int targetWidth = settings.imageWidth();
        float scale = Math.max(1, targetWidth - MARGIN_PX * 2) / projW;
        int needW = Math.round(projW * scale) + MARGIN_PX * 2;
        int needH = Math.round(projH * scale) + MARGIN_PX * 2;

        return renderCore(settings, scale, needW, needH, new Vector3f(centerX, centreY, centreZ));
    }

    private @NotNull NativeImage renderCore(SchematicRenderSettings settings, float scale, int needW, int needH, Vector3f centerPos) throws SchematicImageRenderException {
        Minecraft mc = Minecraft.getInstance();

        int maxSupported = Math.min(MAX_FB_SIZE, RenderSystem.maxSupportedTextureSize());
        if (needW > maxSupported || needH > maxSupported) {
            float cap = (float) maxSupported / Math.max(needW, needH);
            scale *= cap;
            needW = Math.min(needW, maxSupported);
            needH = Math.min(needH, maxSupported);
        }
        ensureTarget(needW, needH);

        Matrix4f prevProjection = new Matrix4f(RenderSystem.getProjectionMatrix());
        VertexSorting prevSorting = RenderSystem.getVertexSorting();

        try {
            float[] bg = unpackArgb(settings.backgroundColor().getRGB());
            renderTarget.setClearColor(bg[0], bg[1], bg[2], bg[3]);
            renderTarget.clear(Minecraft.ON_OSX);
            renderTarget.bindWrite(true);

            Matrix4f projection = new Matrix4f().setOrtho(-targetW / 2f, targetW / 2f, -targetH / 2f,
                    targetH / 2f, -DEFAULT_NEAR_AND_FAR, DEFAULT_NEAR_AND_FAR);
            RenderSystem.setProjectionMatrix(projection, VertexSorting.ORTHOGRAPHIC_Z);

            PoseStack poseStack = new PoseStack();
            poseStack.pushPose();

            RenderSystem.enableDepthTest();
            RenderSystem.enableBlend();
            RenderSystem.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA);
            Lighting.setupLevel();

            poseStack.scale(scale, scale, scale);

            Orientation orientation = settings.orientation();
            poseStack.mulPose(Axis.XP.rotationDegrees(orientation.pitch()));
            poseStack.mulPose(Axis.YP.rotationDegrees(orientation.yaw()));
            if (orientation.roll() != 0f)
                poseStack.mulPose(Axis.ZP.rotationDegrees(orientation.roll()));

            poseStack.translate(-centerPos.x, -centerPos.y, -centerPos.z);

            SuperRenderTypeBuffer buffers = DefaultSuperRenderTypeBuffer.getInstance();
            var renderer = new SchematicRenderer(schematicLevel);

            DEBUG_TIMER.markInstant("Before main render");
            renderer.render(poseStack, buffers);
            DEBUG_TIMER.markInstant("Before fluids render");
            renderFluids(poseStack, buffers);
            buffers.draw();
            poseStack.popPose();

            NativeImage image = null;
            try {
                image = new NativeImage(targetW, targetH, false);
                RenderSystem.bindTexture(renderTarget.getColorTextureId());
                DEBUG_TIMER.markInstant("Before download texture");
                image.downloadTexture(0, false);
                image.flipY();
            } catch (Exception e) {
                if (image != null) image.close();
                throw new SchematicImageRenderException("Failed to download rendered schematic image", e);
            }
            return image;
        } catch (IllegalStateException | NullPointerException e) {
            throw new SchematicImageRenderException("Failed to render schematic", e);
        } finally {
            try {
                if (renderTarget != null) renderTarget.destroyBuffers();
            } catch (Exception e) {
                CreateBlueprinted.LOGGER.error("Failed to destroy render target buffers", e);
            }
            mc.getMainRenderTarget().bindWrite(true);
            RenderSystem.setProjectionMatrix(prevProjection, prevSorting);
        }
    }

    private void renderFluids(PoseStack poseStack, SuperRenderTypeBuffer buffers) {
        if (fluidPositions.isEmpty()) {
            return;
        }
        BlockRenderDispatcher dispatcher = Minecraft.getInstance().getBlockRenderer();
        Matrix4f pose = poseStack.last().pose();
        Matrix3f normal = poseStack.last().normal();

        for (BlockPos pos : fluidPositions) {
            BlockState state = schematicLevel.getBlockState(pos);
            FluidState fluid = state.getFluidState();
            if (fluid.isEmpty()) {
                continue;
            }
            RenderType layer = ItemBlockRenderTypes.getRenderLayer(fluid);
            fluidConsumer.prepare(buffers.getBuffer(layer), pose, normal,
                    pos.getX() - (pos.getX() & 15),
                    pos.getY() - (pos.getY() & 15),
                    pos.getZ() - (pos.getZ() & 15));
            dispatcher.renderLiquid(pos, schematicLevel, fluidConsumer, state, fluid);
        }
    }

    public static NativeImage downsample(NativeImage source, int factor) throws SchematicImageRenderException {
        DEBUG_TIMER.markInstant("Before downsample render");
        try (source) {
            int outW = Math.max(1, source.getWidth() / factor);
            int outH = Math.max(1, source.getHeight() / factor);

            NativeImage out = new NativeImage(outW, outH, false);
            int samples = factor * factor;

            try {
                for (int y = 0; y < outH; y++) {
                    for (int x = 0; x < outW; x++) {
                        long aSum = 0, c0 = 0, c1 = 0, c2 = 0;
                        for (int dy = 0; dy < factor; dy++) {
                            for (int dx = 0; dx < factor; dx++) {
                                int p = source.getPixelRGBA(x * factor + dx, y * factor + dy);
                                int a = (p >>> 24) & 0xFF;
                                aSum += a;
                                c0 += (long) (p & 0xFF) * a;
                                c1 += (long) ((p >> 8) & 0xFF) * a;
                                c2 += (long) ((p >> 16) & 0xFF) * a;
                            }
                        }
                        int a = (int) (aSum / samples);
                        int r0 = aSum == 0 ? 0 : (int) (c0 / aSum);
                        int r1 = aSum == 0 ? 0 : (int) (c1 / aSum);
                        int r2 = aSum == 0 ? 0 : (int) (c2 / aSum);
                        out.setPixelRGBA(x, y, (a << 24) | (r2 << 16) | (r1 << 8) | r0);
                    }
                }
            } catch (IllegalArgumentException e) {
                out.close();
                throw new SchematicImageRenderException("Failed to downsample ", e);
            }
            return out;
        }
    }

    private void ensureTarget(int needW, int needH) {
        if (renderTarget != null && targetW >= needW && targetH >= needH) {
            return;
        }
        int newW = Math.max(targetW, needW);
        int newH = Math.max(targetH, needH);
        if (renderTarget != null) {
            renderTarget.destroyBuffers();
        }
        renderTarget = new TextureTarget(newW, newH, true, Minecraft.ON_OSX);
        targetW = newW;
        targetH = newH;
    }

    private static float[] unpackArgb(int argb) {
        float a = ((argb >> 24) & 0xFF) / 255f;
        float r = ((argb >> 16) & 0xFF) / 255f;
        float g = ((argb >> 8) & 0xFF) / 255f;
        float b = (argb & 0xFF) / 255f;
        return new float[]{r, g, b, a};
    }

    public SchematicLevel getSchematicLevel() {
        return schematicLevel;
    }
}
