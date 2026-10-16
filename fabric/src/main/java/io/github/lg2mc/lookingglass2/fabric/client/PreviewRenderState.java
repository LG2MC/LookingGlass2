package io.github.lg2mc.lookingglass2.fabric.client;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.VertexSorting;
import net.minecraft.client.renderer.ShaderInstance;
import org.joml.Matrix4f;
import org.lwjgl.opengl.*;
import org.lwjgl.system.MemoryStack;

/** Saves the state touched by the limited terrain pass. Vanilla rendering only. */
final class PreviewRenderState implements AutoCloseable {
    private final int draw = GL11.glGetInteger(GL30.GL_DRAW_FRAMEBUFFER_BINDING);
    private final int read = GL11.glGetInteger(GL30.GL_READ_FRAMEBUFFER_BINDING);
    private final int active = GL11.glGetInteger(GL13.GL_ACTIVE_TEXTURE);
    private final int vao = GL11.glGetInteger(GL30.GL_VERTEX_ARRAY_BINDING);
    private final int arrayBuffer = GL11.glGetInteger(GL15.GL_ARRAY_BUFFER_BINDING);
    private final int program = GL11.glGetInteger(GL20.GL_CURRENT_PROGRAM);
    private final int[] viewport = new int[4];
    private final int[] bindings = new int[3];
    private final int[] shaderTextures = new int[12];
    private final boolean[] colorMask = new boolean[4];
    private final boolean depth = GL11.glIsEnabled(GL11.GL_DEPTH_TEST);
    private final boolean depthMask = GL11.glGetBoolean(GL11.GL_DEPTH_WRITEMASK);
    private final int depthFunc = GL11.glGetInteger(GL11.GL_DEPTH_FUNC);
    private final boolean blend = GL11.glIsEnabled(GL11.GL_BLEND);
    private final boolean cull = GL11.glIsEnabled(GL11.GL_CULL_FACE);
    private final boolean scissor = GL11.glIsEnabled(GL11.GL_SCISSOR_TEST);
    private final int srcRgb = GL11.glGetInteger(GL14.GL_BLEND_SRC_RGB);
    private final int dstRgb = GL11.glGetInteger(GL14.GL_BLEND_DST_RGB);
    private final int srcAlpha = GL11.glGetInteger(GL14.GL_BLEND_SRC_ALPHA);
    private final int dstAlpha = GL11.glGetInteger(GL14.GL_BLEND_DST_ALPHA);
    private final int blendEquation = GL11.glGetInteger(GL20.GL_BLEND_EQUATION_RGB);
    private final ShaderInstance shader = RenderSystem.getShader();
    private final Matrix4f projection = new Matrix4f(RenderSystem.getProjectionMatrix());
    private final Matrix4f textureMatrix = new Matrix4f(RenderSystem.getTextureMatrix());
    private final VertexSorting sorting = RenderSystem.getVertexSorting();
    private final float fogStart = RenderSystem.getShaderFogStart();
    private final float fogEnd = RenderSystem.getShaderFogEnd();
    private final float[] color = RenderSystem.getShaderColor().clone();

    PreviewRenderState() {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            var v = stack.mallocInt(4);
            GL11.glGetIntegerv(GL11.GL_VIEWPORT, v);
            var mask = stack.malloc(4);
            GL11.glGetBooleanv(GL11.GL_COLOR_WRITEMASK, mask);
            for (int i = 0; i < 4; i++) {
                viewport[i] = v.get(i);
                colorMask[i] = mask.get(i) != 0;
            }
        }
        for (int i = 0; i < bindings.length; i++) {
            GlStateManager._activeTexture(GL13.GL_TEXTURE0 + i);
            bindings[i] = GL11.glGetInteger(GL11.GL_TEXTURE_BINDING_2D);
        }
        GlStateManager._activeTexture(active);
        for (int i = 0; i < shaderTextures.length; i++) shaderTextures[i] = RenderSystem.getShaderTexture(i);
        RenderSystem.getModelViewStack().pushPose();
    }

    @Override
    public void close() {
        BufferUploader.reset();
        GlStateManager._glBindVertexArray(vao);
        GlStateManager._glBindBuffer(GL15.GL_ARRAY_BUFFER, arrayBuffer);
        GlStateManager._glBindFramebuffer(GL30.GL_DRAW_FRAMEBUFFER, draw);
        GlStateManager._glBindFramebuffer(GL30.GL_READ_FRAMEBUFFER, read);
        RenderSystem.viewport(viewport[0], viewport[1], viewport[2], viewport[3]);
        RenderSystem.getModelViewStack().popPose();
        RenderSystem.applyModelViewMatrix();
        RenderSystem.setProjectionMatrix(projection, sorting);
        RenderSystem.setTextureMatrix(textureMatrix);
        RenderSystem.setShader(() -> shader);
        GlStateManager._glUseProgram(program);
        RenderSystem.setShaderFogStart(fogStart);
        RenderSystem.setShaderFogEnd(fogEnd);
        RenderSystem.setShaderColor(color[0], color[1], color[2], color[3]);
        for (int i = 0; i < shaderTextures.length; i++) RenderSystem.setShaderTexture(i, shaderTextures[i]);
        for (int i = 0; i < bindings.length; i++) {
            GlStateManager._activeTexture(GL13.GL_TEXTURE0 + i);
            GlStateManager._bindTexture(bindings[i]);
        }
        GlStateManager._activeTexture(active);
        RenderSystem.colorMask(colorMask[0], colorMask[1], colorMask[2], colorMask[3]);
        RenderSystem.depthMask(depthMask);
        RenderSystem.depthFunc(depthFunc);
        RenderSystem.blendFuncSeparate(srcRgb, dstRgb, srcAlpha, dstAlpha);
        RenderSystem.blendEquation(blendEquation);
        if (depth) RenderSystem.enableDepthTest(); else RenderSystem.disableDepthTest();
        if (blend) RenderSystem.enableBlend(); else RenderSystem.disableBlend();
        if (cull) RenderSystem.enableCull(); else RenderSystem.disableCull();
        if (scissor) GlStateManager._enableScissorTest(); else RenderSystem.disableScissor();
    }
}
