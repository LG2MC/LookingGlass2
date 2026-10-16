package io.github.lg2mc.lookingglass2.fabric.client;

import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.shaders.Uniform;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.ItemBlockRenderTypes;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL30;
import org.lwjgl.system.MemoryStack;

import java.nio.FloatBuffer;

/** Limited block-model preview, not a replacement for Minecraft's full world renderer. */
final class DebugCamera {
    static final int MIN_RADIUS = 8;
    static final int MAX_RADIUS = 32;
    private static int radius = 24;
    private static final BufferBuilder OPAQUE = new BufferBuilder(1 << 20);
    private static final BufferBuilder TRANSLUCENT = new BufferBuilder(1 << 18);
    private static final RandomSource RANDOM = RandomSource.create(0);
    private static Vec3 position;
    private static float yaw;
    private static float pitch;
    private static int drawnBlocks;
    private static double renderMillis;

    private DebugCamera() {}

    static boolean capture() {
        Minecraft client = Minecraft.getInstance();
        if (client.level == null || client.player == null) return false;
        position = client.player.getEyePosition();
        yaw = client.player.getYRot();
        pitch = client.player.getXRot();
        return true;
    }

    static int radius() { return radius; }

    static void setRadius(int blocks) {
        if (blocks < MIN_RADIUS || blocks > MAX_RADIUS) {
            throw new IllegalArgumentException("Camera radius must be between 8 and 32 blocks");
        }
        radius = blocks;
        DebugDisplayTexture.requestCameraUpdate();
    }

    static boolean active() { return position != null; }

    static void clear() {
        position = null;
        drawnBlocks = 0;
        renderMillis = 0;
        discard(OPAQUE);
        discard(TRANSLUCENT);
    }

    static String status() {
        if (!active()) return "Color test mode. Use /lg2 camera to capture your eye position and direction.";
        return String.format(java.util.Locale.ROOT,
                "Camera %.1f %.1f %.1f | yaw %.1f pitch %.1f | %d blocks | %.1f ms/update | radius %d | max 5 Hz",
                position.x, position.y, position.z, yaw, pitch, drawnBlocks, renderMillis, radius);
    }

    static void render(RenderTarget target, ClientLevel level) {
        if (!active()) return;
        long start = System.nanoTime();
        // Snapshot is outside allocation and rendering, so the main pass retains its state.
        try (PreviewRenderState state = new PreviewRenderState(); MemoryStack stack = MemoryStack.stackPush()) {
            target.bindWrite(true);
            RenderSystem.disableScissor();
            RenderSystem.colorMask(true, true, true, true);
            RenderSystem.depthMask(true);
            GL30.glClearBufferfv(GL11.GL_COLOR, 0, stack.floats(0.025F, 0.035F, 0.05F, 1.0F));
            GL30.glClearBufferfv(GL11.GL_DEPTH, 0, stack.floats(1.0F));
            RenderSystem.enableDepthTest();
            RenderSystem.depthFunc(GL11.GL_LEQUAL);
            RenderSystem.enableCull();
            RenderSystem.disableBlend();
            RenderSystem.setShaderColor(1, 1, 1, 1);
            RenderSystem.setShaderFogStart(1000.0F);
            RenderSystem.setShaderFogEnd(1001.0F);
            RenderSystem.setProjectionMatrix(new Matrix4f().perspective(
                    (float) Math.toRadians(70.0), 1.0F, 0.05F, 64.0F), VertexSorting.DISTANCE_TO_ORIGIN);
            RenderSystem.resetTextureMatrix();
            RenderSystem.getModelViewStack().setIdentity();
            RenderSystem.applyModelViewMatrix();
            RenderSystem.setShaderTexture(0, TextureAtlas.LOCATION_BLOCKS);
            Minecraft.getInstance().gameRenderer.lightTexture().turnOnLightLayer();

            PoseStack poses = new PoseStack();
            poses.mulPose(Axis.XP.rotationDegrees(pitch));
            poses.mulPose(Axis.YP.rotationDegrees(yaw + 180.0F));
            buildTerrain(level, poses);
            draw(OPAQUE, GameRenderer.getRendertypeCutoutShader());
            RenderSystem.enableBlend();
            RenderSystem.defaultBlendFunc();
            // Keep depth testing, but transparent faces must not hide later transparent faces.
            RenderSystem.depthMask(false);
            draw(TRANSLUCENT, GameRenderer.getRendertypeTranslucentShader());
        } finally {
            // Recover clean builders if a model renderer throws partway through a batch.
            discard(OPAQUE);
            discard(TRANSLUCENT);
            renderMillis = (System.nanoTime() - start) / 1_000_000.0;
        }
    }

    private static void buildTerrain(ClientLevel level, PoseStack poses) {
        OPAQUE.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.BLOCK);
        TRANSLUCENT.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.BLOCK);
        BlockPos center = BlockPos.containing(position);
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        drawnBlocks = 0;
        for (int y = Math.max(level.getMinBuildHeight(), center.getY() - radius);
             y <= Math.min(level.getMaxBuildHeight() - 1, center.getY() + radius); y++) {
            for (int z = center.getZ() - radius; z <= center.getZ() + radius; z++) {
                for (int x = center.getX() - radius; x <= center.getX() + radius; x++) {
                    pos.set(x, y, z);
                    if (!level.hasChunkAt(pos)) continue; // Never request or load remote chunks.
                    BlockState block = level.getBlockState(pos);
                    if (block.getRenderShape() != RenderShape.MODEL) continue;
                    poses.pushPose();
                    try {
                        poses.translate(x - position.x, y - position.y, z - position.z);
                        BufferBuilder output = ItemBlockRenderTypes.getChunkRenderType(block) == RenderType.translucent()
                                ? TRANSLUCENT : OPAQUE;
                        // Contextual model, biome tint, neighbor face culling and client light values.
                        Minecraft.getInstance().getBlockRenderer().renderBatched(
                                block, pos, level, poses, output, true, RANDOM);
                        drawnBlocks++;
                    } finally {
                        poses.popPose();
                    }
                }
            }
        }
        TRANSLUCENT.setQuadSorting(VertexSorting.DISTANCE_TO_ORIGIN);
    }

    private static void discard(BufferBuilder builder) {
        if (builder.building()) {
            BufferBuilder.RenderedBuffer unfinished = builder.endOrDiscardIfEmpty();
            if (unfinished != null) unfinished.release();
        }
        builder.discard();
    }

    private static void draw(BufferBuilder builder, ShaderInstance shader) {
        BufferBuilder.RenderedBuffer rendered = builder.endOrDiscardIfEmpty();
        if (rendered == null) return;
        if (shader == null) {
            rendered.release();
            throw new IllegalStateException("Vanilla terrain shader is unavailable");
        }
        Uniform offset = shader.CHUNK_OFFSET;
        float x = 0, y = 0, z = 0;
        if (offset != null) {
            FloatBuffer previous = offset.getFloatBuffer();
            x = previous.get(0); y = previous.get(1); z = previous.get(2);
            offset.set(0.0F, 0.0F, 0.0F);
        }
        try {
            RenderSystem.setShader(() -> shader);
            BufferUploader.drawWithShader(rendered);
        } finally {
            if (offset != null) offset.set(x, y, z);
        }
    }
}
