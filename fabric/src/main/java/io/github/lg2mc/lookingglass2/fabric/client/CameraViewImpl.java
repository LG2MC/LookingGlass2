package io.github.lg2mc.lookingglass2.fabric.client;

import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.pipeline.TextureTarget;
import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexSorting;
import com.mojang.math.Axis;
import io.github.lg2mc.lookingglass2.api.client.*;
import io.github.lg2mc.lookingglass2.fabric.mixin.LevelRendererAccess;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.RenderBuffers;
import net.minecraft.client.renderer.texture.AbstractTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL30;
import java.util.Objects;
import java.util.Optional;

final class CameraViewImpl implements CameraView {
    ClientLevel level;
    private final ResourceLocation textureId;
    private final int width, height;
    private CameraPose pose;
    private CameraSettings settings;
    final FixedCamera CAMERA = new FixedCamera();
    LevelRenderer renderer;
    private RenderBuffers buffers;
    TextureTarget target;
    int effectiveDistance;
    private boolean closed, ready, enabled = true;
    private String failure;
    private long lastVisibleFrame = -100, lastVisibleTime, nextUpdate = -1;
    private final long[] updateTimes = new long[60];
    private final double[] updateCosts = new double[60];
    private int sampleCount, sampleCursor;

    CameraViewImpl(ClientLevel level, ResourceLocation id, int width, int height, CameraPose pose, CameraSettings settings) {
        this.level = level; this.textureId = id; this.width = width; this.height = height;
        this.pose = Objects.requireNonNull(pose); this.settings = Objects.requireNonNull(settings);
    }
    private void checkOpen() {
        CameraViews.checkThread();
        if (closed) throw new IllegalStateException("Camera view is closed; create a new view for the current world");
    }
    @Override public CameraPose pose() { return pose; }
    @Override public void setPose(CameraPose value) { checkOpen(); pose = Objects.requireNonNull(value); }
    @Override public CameraSettings settings() { return settings; }
    @Override public void setSettings(CameraSettings value) {
        checkOpen(); Objects.requireNonNull(value);
        if (!settings.equals(value)) { settings = value; resetMeasurements(); }
    }
    @Override public int width() { return width; }
    @Override public int height() { return height; }
    @Override public boolean isClosed() { return closed; }
    @Override public boolean isReady() { return ready && !closed; }
    @Override public boolean isEnabled() { return enabled && !closed; }
    @Override public void setEnabled(boolean value) {
        checkOpen();
        if (enabled != value) { enabled = value; resetMeasurements(); }
        if (!value) disposeRenderer();
    }
    @Override public Optional<String> failure() { return Optional.ofNullable(failure); }
    @Override public ResourceLocation texture() { markVisible(); return textureId; }
    @Override public void markVisible() {
        checkOpen(); ensureTarget();
        lastVisibleFrame = CameraViews.frame;
        lastVisibleTime = System.nanoTime();
    }
    private void ensureTarget() {
        if (target != null) return;
        try (PreviewRenderState state = new PreviewRenderState()) {
            target = new TextureTarget(width, height, true, Minecraft.ON_OSX);
            target.setClearColor(0, 0, 0, 1);
            RenderSystem.disableScissor();
            RenderSystem.colorMask(true, true, true, true);
            RenderSystem.depthMask(true);
            target.clear(Minecraft.ON_OSX);
            Minecraft.getInstance().getTextureManager().register(textureId, new BorrowedTexture());
        }
    }
    @Override public void retry() { checkOpen(); failure = null; resetMeasurements(); }
    private void resetMeasurements() { sampleCount = sampleCursor = 0; nextUpdate = -1; }
    @Override public CameraStatistics statistics() {
        double total = 0, peak = 0;
        for (int i = 0; i < sampleCount; i++) { total += updateCosts[i]; peak = Math.max(peak, updateCosts[i]); }
        int oldest = sampleCount < updateTimes.length ? 0 : sampleCursor;
        int newest = (sampleCursor + updateTimes.length - 1) % updateTimes.length;
        long elapsed = updateTimes[newest] - updateTimes[oldest];
        double actual = sampleCount > 1 && elapsed > 0 ? (sampleCount - 1) * 1_000_000_000.0 / elapsed : 0;
        return new CameraStatistics(actual, sampleCount == 0 ? 0 : total / sampleCount, peak, sampleCount);
    }
    void update() {
        long now = System.nanoTime();
        if (!enabled || failure != null || CameraViews.frame - lastVisibleFrame > 2) {
            if (renderer != null && (!enabled || failure != null || now - lastVisibleTime > 2_000_000_000L)) {
                disposeRenderer(); resetMeasurements();
            }
            return;
        }
        if (nextUpdate >= 0 && now < nextUpdate) return;
        render(target, level);
        ready = true;
        long finished = System.nanoTime();
        updateTimes[sampleCursor] = now;
        updateCosts[sampleCursor] = (finished - now) / 1_000_000.0;
        sampleCursor = (sampleCursor + 1) % updateTimes.length;
        sampleCount = Math.min(sampleCount + 1, updateTimes.length);
        long interval = 1_000_000_000L / settings.updateFps();
        if (nextUpdate < 0) nextUpdate = now;
        nextUpdate += interval;
        if (nextUpdate <= finished) nextUpdate += ((finished - nextUpdate) / interval + 1) * interval;
    }
    void failed(RuntimeException error) {
        failure = error.getClass().getSimpleName() + ": " + error.getMessage();
        ready = false; disposeRenderer();
    }
    private void disposeRenderer() {
        if (renderer == null) return;
        try {
            renderer.setLevel(null); renderer.close();
            LevelRendererAccess access = (LevelRendererAccess) renderer;
            if (access.lg2$stars() != null) access.lg2$stars().close();
            if (access.lg2$sky() != null) access.lg2$sky().close();
            if (access.lg2$dark() != null) access.lg2$dark().close();
            if (access.lg2$clouds() != null) access.lg2$clouds().close();
        } finally {
            renderer = null; buffers = null;
            Minecraft mc = Minecraft.getInstance(); mc.getEntityRenderDispatcher().setLevel(mc.level);
        }
    }
    @Override public void close() {
        CameraViews.checkThread();
        if (closed) return;
        closed = true;
        try { disposeRenderer(); } finally {
            try { releaseTarget(); } finally { CameraViews.remove(this); resetMeasurements(); ready = false; level = null; CAMERA.reset(); }
        }
    }
    private void releaseTarget() {
        if (target == null) return;
        int draw = GL11.glGetInteger(GL30.GL_DRAW_FRAMEBUFFER_BINDING);
        int read = GL11.glGetInteger(GL30.GL_READ_FRAMEBUFFER_BINDING);
        int texture = GL11.glGetInteger(GL11.GL_TEXTURE_BINDING_2D);
        int oldFramebuffer = target.frameBufferId, oldTexture = target.getColorTextureId();
        try {
            Minecraft.getInstance().getTextureManager().release(textureId);
            target.destroyBuffers();
        } finally {
            target = null;
            GlStateManager._glBindFramebuffer(GL30.GL_DRAW_FRAMEBUFFER, draw == oldFramebuffer ? 0 : draw);
            GlStateManager._glBindFramebuffer(GL30.GL_READ_FRAMEBUFFER, read == oldFramebuffer ? 0 : read);
            GlStateManager._bindTexture(texture == oldTexture ? 0 : texture);
        }
    }
    private final class BorrowedTexture extends AbstractTexture {
        @Override public int getId() { return target == null ? 0 : target.getColorTextureId(); }
        @Override public void load(ResourceManager manager) {}
        @Override public void releaseId() {}
        @Override public void close() {}
    }
    private void render(RenderTarget target, ClientLevel level) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || CameraViews.rendering()) return;
        Vec3 position = pose.position();
        float yaw = pose.yaw(), pitch = pose.pitch();
        effectiveDistance = Math.max(2, Math.min(8, settings.distanceChunks() == 0
                ? mc.options.getEffectiveRenderDistance() : settings.distanceChunks()));
        int fov = settings.fovDegrees() == 0 ? mc.options.fov().get() : settings.fovDegrees();
        float partial = mc.getFrameTime();
        CAMERA.place(level, position, yaw, pitch, partial);
        Camera previousCamera = mc.gameRenderer.getMainCamera();
        // Run before the main world pass, not from inside its world-render callbacks.
        try (PreviewRenderState state = new PreviewRenderState()) {
            CameraViews.current = this;
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
            if (renderer == null) {
                disposeRenderer();
                buffers = new RenderBuffers();
                renderer = new LevelRenderer(mc, mc.getEntityRenderDispatcher(), mc.getBlockEntityRenderDispatcher(), buffers);
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
            CameraViews.current = null;
            mc.getEntityRenderDispatcher().prepare(mc.level, previousCamera, mc.crosshairPickEntity);
            mc.getBlockEntityRenderDispatcher().prepare(mc.level, previousCamera, mc.hitResult);
        }
    }
    static final class FixedCamera extends Camera {
        void place(ClientLevel level, Vec3 pos, float yaw, float pitch, float partial) {
            setup(level, Minecraft.getInstance().player, true, false, partial);
            setPosition(pos); setRotation(yaw, pitch);
        }
    }
}
