package io.github.lg2mc.lookingglass2.fabric.client;

import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexSorting;
import com.mojang.math.Axis;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.RenderBuffers;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import io.github.lg2mc.lookingglass2.fabric.mixin.LevelRendererAccess;

/** One experimental current-world view. Owns its terrain renderer and compiled geometry. */
public final class DebugCamera {
    private static Vec3 position;
    private static float yaw, pitch;
    private static CameraSettings settings = CameraSettings.DEFAULT;
    private static final FixedCamera CAMERA = new FixedCamera();
    private static LevelRenderer renderer;
    private static RenderBuffers buffers;
    private static ClientLevel world;
    private static boolean rendering;
    private static RenderTarget output;
    private static int effectiveDistance;

    private DebugCamera() {}
    static boolean capture() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null) return false;
        position = mc.player.getEyePosition();
        yaw = mc.player.getYRot(); pitch = mc.player.getXRot();
        return true;
    }
    static boolean active() { return position != null; }
    public static boolean rendering() { return rendering; }
    public static Camera camera() { return CAMERA; }
    public static RenderTarget output() { return output; }
    public static int distance() { return effectiveDistance; }
    public static boolean particles() { return settings.particles(); }
    static CameraSettings settings() { return settings; }
    static void configure(CameraSettings value) { settings = value; DebugDisplayTexture.requestCameraUpdate(); }

    static void clear() {
        position = null;
        DebugDisplayTexture.requestCameraUpdate();
        disposeRenderer();
    }
    static void disposeRenderer() {
        if (renderer != null) {
            try {
                renderer.setLevel(null);
                renderer.close();
                LevelRendererAccess access = (LevelRendererAccess) renderer;
                if (access.lg2$stars() != null) access.lg2$stars().close();
                if (access.lg2$sky() != null) access.lg2$sky().close();
                if (access.lg2$dark() != null) access.lg2$dark().close();
                if (access.lg2$clouds() != null) access.lg2$clouds().close();
            } finally {
                renderer = null; buffers = null; world = null;
                Minecraft mc = Minecraft.getInstance();
                mc.getEntityRenderDispatcher().setLevel(mc.level);
            }
        }
    }
    public static void tick() { if (renderer != null && !rendering) renderer.tick(); }
    public static void dirty(LevelRenderer source, int x, int y, int z, boolean immediate) {
        if (renderer != null && source != renderer) ((LevelRendererAccess) renderer).lg2$dirty(x, y, z, immediate);
    }
    static String status() {
        String options = "distance=" + (settings.distanceChunks() == 0 ? "player (max 8)" : settings.distanceChunks())
                + " chunks, FOV=" + (settings.fovDegrees() == 0 ? "player" : settings.fovDegrees())
                + ", particles=" + settings.particles();
        return (!active() ? "Color mode | " : String.format(java.util.Locale.ROOT,
                "Camera %.1f %.1f %.1f | ", position.x, position.y, position.z))
                + options + " | target " + settings.updateFps() + " FPS | " + DebugDisplayTexture.performance() + (renderer == null ? "" : " | " + renderer.getChunkStatistics());
    }
    static void render(RenderTarget target, ClientLevel level) {
        Minecraft mc = Minecraft.getInstance();
        if (!active() || mc.player == null || rendering) return;
        effectiveDistance = Math.max(2, Math.min(8, settings.distanceChunks() == 0
                ? mc.options.getEffectiveRenderDistance() : settings.distanceChunks()));
        int fov = settings.fovDegrees() == 0 ? mc.options.fov().get() : settings.fovDegrees();
        float partial = mc.getFrameTime();
        CAMERA.place(level, position, yaw, pitch, partial);
        Camera previousCamera = mc.gameRenderer.getMainCamera();
        // Run before the main world pass, not from inside its world-render callbacks.
        try (PreviewRenderState state = new PreviewRenderState()) {
            rendering = true; output = target;
            target.bindWrite(true);
            RenderSystem.disableScissor();
            RenderSystem.colorMask(true, true, true, true);
            RenderSystem.depthMask(true);
            RenderSystem.enableDepthTest();
            RenderSystem.enableCull();
            RenderSystem.setShaderColor(1, 1, 1, 1);
            RenderSystem.resetTextureMatrix();
            RenderSystem.getModelViewStack().setIdentity();
            RenderSystem.applyModelViewMatrix();
            if (renderer == null || world != level) {
                disposeRenderer();
                buffers = new RenderBuffers();
                renderer = new LevelRenderer(mc, mc.getEntityRenderDispatcher(), mc.getBlockEntityRenderDispatcher(), buffers);
                world = level;
                renderer.setLevel(level);
            }
            Matrix4f projection = new Matrix4f().perspective((float)Math.toRadians(fov),
                    (float)target.width / target.height, 0.05F, effectiveDistance * 64.0F);
            RenderSystem.setProjectionMatrix(projection, VertexSorting.DISTANCE_TO_ORIGIN);
            PoseStack poses = new PoseStack();
            poses.mulPose(Axis.XP.rotationDegrees(pitch));
            poses.mulPose(Axis.YP.rotationDegrees(yaw + 180.0F));
            renderer.prepareCullFrustum(poses, position, projection);
            mc.getProfiler().push("lookingglass2_camera");
            try {
                renderer.renderLevel(poses, partial, System.nanoTime(), false, CAMERA,
                        mc.gameRenderer, mc.gameRenderer.lightTexture(), projection);
            } finally {
                mc.getProfiler().pop();
            }
        } finally {
            rendering = false; output = null;
            mc.getEntityRenderDispatcher().prepare(mc.level, previousCamera, mc.crosshairPickEntity);
            mc.getBlockEntityRenderDispatcher().prepare(mc.level, previousCamera, mc.hitResult);
        }
    }
    private static final class FixedCamera extends Camera {
        void place(ClientLevel level, Vec3 pos, float yaw, float pitch, float partial) {
            setup(level, Minecraft.getInstance().player, true, false, partial);
            setPosition(pos); setRotation(yaw, pitch);
        }
    }
}
