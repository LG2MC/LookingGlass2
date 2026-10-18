package io.github.lg2mc.lookingglass2.fabric.client;

import com.mojang.blaze3d.pipeline.TextureTarget;
import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import io.github.lg2mc.lookingglass2.LookingGlass2;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.texture.AbstractTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL30;
import org.lwjgl.system.MemoryStack;

import java.nio.FloatBuffer;
import java.nio.IntBuffer;

/** Shared blue/cyan fallback for the development display only. Camera views own separate targets. */
public final class DebugDisplayTexture {
    static final ResourceLocation ID = new ResourceLocation("lookingglass2", "debug/display_target");
    private static TextureTarget target;
    private static ClientLevel level;
    private static long frame;
    private static long lastSeen = -1;
    private static long lastUpdated = -1;
    private static long started;
    private DebugDisplayTexture() {}

    public static void beginFrame() {
        RenderSystem.assertOnRenderThread();
        if (CameraViews.rendering()) return;
        ClientLevel current = Minecraft.getInstance().level;
        if (level != current) {
            release();
            DebugCamera.clear();
            level = current;
        }
        frame++;
        // Two-frame grace period: queued block-entity vertices have been flushed before deletion.
        if (target != null && frame - lastSeen > 2) {
            release();
        }
    }

    static void touch() {
        RenderSystem.assertOnRenderThread();
        lastSeen = frame;
        if (target != null && (lastUpdated == frame)) return;

        // RenderTarget allocation/clear changes GL state. Restore the caller's actual targets,
        // viewport, texture binding, and clear color even when initialization fails.
        try (MemoryStack stack = MemoryStack.stackPush()) {
            IntBuffer viewport = stack.mallocInt(4);
            FloatBuffer clearColor = stack.mallocFloat(4);
            GL11.glGetIntegerv(GL11.GL_VIEWPORT, viewport);
            GL11.glGetFloatv(GL11.GL_COLOR_CLEAR_VALUE, clearColor);
            int draw = GL11.glGetInteger(GL30.GL_DRAW_FRAMEBUFFER_BINDING);
            int read = GL11.glGetInteger(GL30.GL_READ_FRAMEBUFFER_BINDING);
            int texture = GL11.glGetInteger(GL11.GL_TEXTURE_BINDING_2D);
            boolean depthMask = GL11.glGetBoolean(GL11.GL_DEPTH_WRITEMASK);
            double clearDepth = GL11.glGetDouble(GL11.GL_DEPTH_CLEAR_VALUE);
            boolean scissor = GL11.glIsEnabled(GL11.GL_SCISSOR_TEST);
            try {
                if (target == null) {
                    target = new TextureTarget(256, 256, true, Minecraft.ON_OSX);
                    Minecraft.getInstance().getTextureManager().register(ID, new BorrowedTexture());
                    started = Util.getMillis();
                    LookingGlass2.LOGGER.info("Debug display target allocated (256x256, shared)");
                }
                float seconds = (Util.getMillis() - started) / 1000.0F;
                float green = seconds < 2.0F ? 0.0F
                        : 0.35F * (1.0F - (float) Math.cos((seconds - 2.0F) * 0.8F));
                target.bindWrite(true);
                if (scissor) RenderSystem.disableScissor();
                // glClearBufferfv clears the GPU attachment without touching the main framebuffer.
                // ClearBuffer honors color masks, so preserve and temporarily enable all channels.
                java.nio.ByteBuffer mask = stack.malloc(4);
                GL11.glGetBooleanv(GL11.GL_COLOR_WRITEMASK, mask);
                try {
                    RenderSystem.colorMask(true, true, true, true);
                    GL30.glClearBufferfv(GL11.GL_COLOR, 0, stack.floats(0.0F, green, 1.0F, 1.0F));
                } finally {
                    RenderSystem.colorMask(mask.get(0) != 0, mask.get(1) != 0,
                            mask.get(2) != 0, mask.get(3) != 0);
                }
                lastUpdated = frame;
            } catch (RuntimeException failure) {
                release();
                throw failure;
            } finally {
                GlStateManager._glBindFramebuffer(GL30.GL_DRAW_FRAMEBUFFER, draw);
                GlStateManager._glBindFramebuffer(GL30.GL_READ_FRAMEBUFFER, read);
                RenderSystem.viewport(viewport.get(0), viewport.get(1), viewport.get(2), viewport.get(3));
                GlStateManager._bindTexture(texture);
                RenderSystem.depthMask(depthMask);
                RenderSystem.clearDepth(clearDepth);
                RenderSystem.clearColor(clearColor.get(0), clearColor.get(1), clearColor.get(2), clearColor.get(3));
                if (scissor) GlStateManager._enableScissorTest();
            }
        }
    }

    static void reset() {
        if (!RenderSystem.isOnRenderThread()) {
            RenderSystem.recordRenderCall(DebugDisplayTexture::reset);
            return;
        }
        CameraViews.reset();
        release();
        DebugCamera.clear();
        level = null;
    }

    private static void release() {
        if (target != null) {
            int draw = GL11.glGetInteger(GL30.GL_DRAW_FRAMEBUFFER_BINDING);
            int read = GL11.glGetInteger(GL30.GL_READ_FRAMEBUFFER_BINDING);
            int boundTexture = GL11.glGetInteger(GL11.GL_TEXTURE_BINDING_2D);
            int oldFramebuffer = target.frameBufferId;
            int oldTexture = target.getColorTextureId();
            try {
                Minecraft.getInstance().getTextureManager().release(ID);
                target.destroyBuffers();
            } finally {
                target = null;
                GlStateManager._glBindFramebuffer(GL30.GL_DRAW_FRAMEBUFFER,
                        draw == oldFramebuffer ? 0 : draw);
                GlStateManager._glBindFramebuffer(GL30.GL_READ_FRAMEBUFFER,
                        read == oldFramebuffer ? 0 : read);
                GlStateManager._bindTexture(boundTexture == oldTexture ? 0 : boundTexture);
            }
            LookingGlass2.LOGGER.info("Debug display target released");
        }
        lastSeen = -1;
        lastUpdated = -1;
    }

    /** TextureManager can reference this attachment, but only TextureTarget owns its GL allocation. */
    private static final class BorrowedTexture extends AbstractTexture {
        @Override
        public int getId() {
            return target == null ? 0 : target.getColorTextureId();
        }

        @Override
        public void load(ResourceManager resources) {}

        @Override
        public void releaseId() {}

        @Override
        public void close() {}
    }
}
