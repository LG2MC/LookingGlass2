package io.github.lg2mc.lookingglass2.fabric.client;

import com.mojang.blaze3d.vertex.PoseStack;
import io.github.lg2mc.lookingglass2.api.client.CameraView;
import io.github.lg2mc.lookingglass2.api.client.LookingGlassClient;
import net.minecraft.resources.ResourceLocation;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import io.github.lg2mc.lookingglass2.fabric.debug.DebugDisplay;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.Direction;

public final class DebugDisplayRenderer implements BlockEntityRenderer<DebugDisplay.DisplayEntity> {
    public DebugDisplayRenderer(BlockEntityRendererProvider.Context context) {}

    @Override
    public void render(DebugDisplay.DisplayEntity display, float partialTick, PoseStack poses,
                       MultiBufferSource buffers, int light, int overlay) {
        if (LookingGlassClient.isRenderingView()) return;
        CameraView view = DebugCamera.viewFor(display.getBlockPos());
        ResourceLocation texture;
        if (view != null && view.failure().isEmpty()) texture = view.texture();
        else { DebugDisplayTexture.touch(); texture = DebugDisplayTexture.ID; }
        poses.pushPose();
        try {
            Direction facing = display.getBlockState().getValue(DebugDisplay.DisplayBlock.FACING);
            poses.translate(0.5, 0.5, 0.5);
            float rotation = switch (facing) {
                case SOUTH -> 180.0F;
                case EAST -> -90.0F;
                case WEST -> 90.0F;
                default -> 0.0F;
            };
            poses.mulPose(Axis.YP.rotationDegrees(rotation));
            poses.translate(-0.5, -0.5, -0.5);
            VertexConsumer vertices = buffers.getBuffer(RenderType.entitySolid(texture));
            PoseStack.Pose pose = poses.last();
            // On the north face, local +X is screen-left when viewed from outside.
            // Flip U so the framebuffer reads left-to-right; V=1 remains at the top.
            vertex(vertices, pose, 0.125F, 0.125F, 1.0F, 0.0F);
            vertex(vertices, pose, 0.125F, 0.875F, 1.0F, 1.0F);
            vertex(vertices, pose, 0.875F, 0.875F, 0.0F, 1.0F);
            vertex(vertices, pose, 0.875F, 0.125F, 0.0F, 0.0F);
        } finally {
            poses.popPose();
        }
    }

    private static void vertex(VertexConsumer out, PoseStack.Pose pose, float x, float y, float u, float v) {
        out.vertex(pose.pose(), x, y, -0.002F)
                .color(255, 255, 255, 255).uv(u, v)
                .overlayCoords(OverlayTexture.NO_OVERLAY).uv2(LightTexture.FULL_BRIGHT)
                .normal(pose.normal(), 0.0F, 0.0F, -1.0F).endVertex();
    }
}
