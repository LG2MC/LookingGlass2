package io.github.lg2mc.lookingglass2.fabric.mixin;
import com.mojang.blaze3d.pipeline.RenderTarget;
import io.github.lg2mc.lookingglass2.fabric.client.DebugCamera;
import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
@Mixin(Minecraft.class)
public abstract class MinecraftMixin {
    @Inject(method="getMainRenderTarget", at=@At("HEAD"), cancellable=true)
    private void lg2$target(CallbackInfoReturnable<RenderTarget> cir) {
        if (DebugCamera.rendering()) cir.setReturnValue(DebugCamera.output());
    }
    // The camera uses vanilla Fancy-style transparency even if the main view is Fabulous.
    @Inject(method="useShaderTransparency", at=@At("HEAD"), cancellable=true)
    private static void lg2$transparency(CallbackInfoReturnable<Boolean> cir) {
        if (DebugCamera.rendering()) cir.setReturnValue(false);
    }
}
