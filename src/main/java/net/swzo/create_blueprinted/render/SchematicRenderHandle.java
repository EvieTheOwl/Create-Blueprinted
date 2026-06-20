package net.swzo.create_blueprinted.render;

import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.pipeline.TextureTarget;
import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.VertexSorting;
import com.mojang.math.Axis;
import com.simibubi.create.content.schematics.client.SchematicRenderer;
import net.createmod.catnip.levelWrappers.SchematicLevel;
import net.createmod.catnip.render.SuperRenderTypeBuffer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.ItemBlockRenderTypes;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.BlockRenderDispatcher;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import net.minecraft.world.level.material.FluidState;
import net.swzo.create_blueprinted.mixin.SchematicRendererAccessor;
import org.jetbrains.annotations.NotNull;
import org.joml.Matrix3f;
import org.joml.Matrix4f;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public final class SchematicRenderHandle implements AutoCloseable {

    private static final int MARGIN_PX = 4;
    private static final int MAX_FB_SIZE = 16384;

    private final SchematicRenderer renderer;
    private final SchematicLevel schematicLevel;
    private final List<BlockPos> fluidPositions;
    private final Vec3i contentMin;
    private final Vec3i contentMax;

    private final PoseAppliedVertexConsumer fluidConsumer = new PoseAppliedVertexConsumer();

    private RenderTarget target;
    private int targetW;
    private int targetH;

    public record RenderResult(int textureId, int width, int height) {}

    private SchematicRenderHandle(SchematicRenderer renderer, SchematicLevel schematicLevel,
                                 List<BlockPos> fluidPositions, Vec3i contentMin, Vec3i contentMax) {
        this.renderer = renderer;
        this.schematicLevel = schematicLevel;
        this.fluidPositions = fluidPositions;
        this.contentMin = contentMin;
        this.contentMax = contentMax;
    }

    public static SchematicRenderHandle bake(CompoundTag tag) {
        Level level = Minecraft.getInstance().level;
        if (level == null) {
            throw new IllegalStateException("Cannot render a schematic without a loaded level.");
        }

        StructureTemplate template = new StructureTemplate();
        template.load(level.holderLookup(Registries.BLOCK), tag);

        SchematicLevel schematicLevel = new SchematicLevel(BlockPos.ZERO, level);
        StructurePlaceSettings placeSettings = new StructurePlaceSettings();
        template.placeInWorld(schematicLevel, BlockPos.ZERO, BlockPos.ZERO, placeSettings,
                RandomSource.create(), Block.UPDATE_CLIENTS);

        int minX = Integer.MAX_VALUE, minY = Integer.MAX_VALUE, minZ = Integer.MAX_VALUE;
        int maxX = Integer.MIN_VALUE, maxY = Integer.MIN_VALUE, maxZ = Integer.MIN_VALUE;
        List<BlockPos> fluidPositions = new ArrayList<>();
        for (Map.Entry<BlockPos, BlockState> entry : schematicLevel.getBlockMap().entrySet()) {
            BlockState state = entry.getValue();
            if (state.isAir()) continue;
            BlockPos pos = entry.getKey();
            minX = Math.min(minX, pos.getX()); maxX = Math.max(maxX, pos.getX());
            minY = Math.min(minY, pos.getY()); maxY = Math.max(maxY, pos.getY());
            minZ = Math.min(minZ, pos.getZ()); maxZ = Math.max(maxZ, pos.getZ());
            if (!state.getFluidState().isEmpty()) {
                fluidPositions.add(pos.immutable());
            }
        }
        if (maxX < minX) {
            throw new IllegalStateException("Schematic has no visible blocks to render.");
        }

        SchematicRenderer renderer = new SchematicRenderer(schematicLevel);
        SchematicRendererAccessor accessor = (SchematicRendererAccessor) renderer;
        accessor.create_blueprinted$redraw();
        accessor.create_blueprinted$setChanged(false);

        return new SchematicRenderHandle(renderer, schematicLevel, fluidPositions,
                new Vec3i(minX, minY, minZ), new Vec3i(maxX, maxY, maxZ));
    }

    public RenderResult renderToTexture(SchematicRenderSettings settings) {
        float loX = contentMin.getX(), loY = contentMin.getY(), loZ = contentMin.getZ();
        float hiX = contentMax.getX() + 1f, hiY = contentMax.getY() + 1f, hiZ = contentMax.getZ() + 1f;
        float cx = (loX + hiX) / 2f, cy = (loY + hiY) / 2f, cz = (loZ + hiZ) / 2f;
        float hx = (hiX - loX) / 2f, hy = (hiY - loY) / 2f, hz = (hiZ - loZ) / 2f;

        Matrix4f rot = new Matrix4f();
        rot.rotateX((float) Math.toRadians(settings.pitch()));
        rot.rotateY((float) Math.toRadians(settings.yaw()));
        rot.rotateZ((float) Math.toRadians(settings.roll()));

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

        return renderCore(settings, scale, needW, needH, cx, cy, cz);
    }

    private RenderResult renderCore(SchematicRenderSettings settings, float scale, int needW, int needH,
                                    float centerX, float centerY, float centerZ) {
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
            float[] bg = unpackArgb(settings.backgroundColor());
            target.setClearColor(bg[0], bg[1], bg[2], bg[3]);
            target.clear(Minecraft.ON_OSX);
            target.bindWrite(true);

            Matrix4f projection = new Matrix4f().setOrtho(-targetW / 2f, targetW / 2f, -targetH / 2f, targetH / 2f, -10000f, 10000f);
            RenderSystem.setProjectionMatrix(projection, VertexSorting.ORTHOGRAPHIC_Z);

            RenderSystem.setShaderLights(
                    new Vector3f(-1.0f, 1.2f, -0.8f).normalize(),
                    new Vector3f(0.5f, -0.2f, 1.0f).normalize());

            PoseStack poseStack = new PoseStack();
            poseStack.pushPose();
            poseStack.scale(scale, scale, scale);
            poseStack.mulPose(Axis.XP.rotationDegrees(settings.pitch()));
            poseStack.mulPose(Axis.YP.rotationDegrees(settings.yaw()));
            if (settings.roll() != 0f) {
                poseStack.mulPose(Axis.ZP.rotationDegrees(settings.roll()));
            }
            poseStack.translate(-centerX, -centerY, -centerZ);

            MultiBufferSource.BufferSource mcBuffers = mc.renderBuffers().bufferSource();
            SuperRenderTypeBuffer buffers = new SuperRenderTypeBuffer() {
                @Override public @NotNull VertexConsumer getEarlyBuffer(@NotNull RenderType type) { return mcBuffers.getBuffer(type); }
                @Override public @NotNull VertexConsumer getBuffer(@NotNull RenderType type) { return mcBuffers.getBuffer(type); }
                @Override public @NotNull VertexConsumer getLateBuffer(@NotNull RenderType type) { return mcBuffers.getBuffer(type); }
                @Override public void draw() { mcBuffers.endBatch(); }
                @Override public void draw(@NotNull RenderType type) { mcBuffers.endBatch(type); }
            };

            renderer.render(poseStack, buffers);
            renderFluids(poseStack, buffers);
            buffers.draw();
            poseStack.popPose();
        } finally {
            mc.getMainRenderTarget().bindWrite(true);
            RenderSystem.setProjectionMatrix(prevProjection, prevSorting);
        }
        return new RenderResult(target.getColorTextureId(), targetW, targetH);
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

    public NativeImage renderAndDownload(SchematicRenderSettings settings) {
        RenderResult result = renderToTexture(settings);
        NativeImage image = new NativeImage(result.width(), result.height(), false);
        RenderSystem.bindTexture(result.textureId());
        image.downloadTexture(0, false);
        image.flipY();
        return image;
    }

    @Override
    public void close() {
        if (target != null) {
            RenderTarget toFree = target;
            target = null;
            if (RenderSystem.isOnRenderThread()) {
                toFree.destroyBuffers();
            } else {
                RenderSystem.recordRenderCall(toFree::destroyBuffers);
            }
        }
    }

    private void ensureTarget(int needW, int needH) {
        if (target != null && targetW >= needW && targetH >= needH) {
            return;
        }
        int newW = Math.max(targetW, needW);
        int newH = Math.max(targetH, needH);
        if (target != null) {
            target.destroyBuffers();
        }
        target = new TextureTarget(newW, newH, true, Minecraft.ON_OSX);
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
}
