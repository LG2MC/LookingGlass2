package io.github.lg2mc.lookingglass2.fabric.mixin;
import com.mojang.blaze3d.vertex.PoseStack;
import io.github.lg2mc.lookingglass2.fabric.client.CameraViews;
import io.github.lg2mc.lookingglass2.fabric.client.DebugDisplayTexture;
import net.minecraft.client.Camera;
import net.minecraft.client.renderer.GameRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
@Mixin(GameRenderer.class)
public abstract class GameRendererMixin {
    @Inject(method="renderLevel", at=@At("HEAD"))
    private void lg2$beforeWorld(float partial, long endTime, PoseStack poses, CallbackInfo ci) {
        CameraViews.beginFrame();
        DebugDisplayTexture.beginFrame();
    }
    @Inject(method="getMainCamera", at=@At("HEAD"), cancellable=true)
    private void lg2$camera(CallbackInfoReturnable<Camera> cir) {
        if (CameraViews.rendering()) cir.setReturnValue(CameraViews.camera());
    }
    @Inject(method="getRenderDistance", at=@At("HEAD"), cancellable=true)
    private void lg2$distance(CallbackInfoReturnable<Float> cir) {
        if (CameraViews.rendering()) cir.setReturnValue(CameraViews.distance() * 16.0F);
    }
}
