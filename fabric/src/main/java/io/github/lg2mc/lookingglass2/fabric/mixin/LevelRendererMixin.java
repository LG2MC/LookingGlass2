package io.github.lg2mc.lookingglass2.fabric.mixin;
import io.github.lg2mc.lookingglass2.fabric.client.DebugCamera;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.LevelRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
@Mixin(LevelRenderer.class)
public abstract class LevelRendererMixin {
    @Redirect(method="setupRender", at=@At(value="INVOKE", target="Lnet/minecraft/client/player/LocalPlayer;getX()D"))
    private double lg2$x(LocalPlayer player) { return DebugCamera.rendering() ? DebugCamera.camera().getPosition().x : player.getX(); }
    @Redirect(method="setupRender", at=@At(value="INVOKE", target="Lnet/minecraft/client/player/LocalPlayer;getY()D"))
    private double lg2$y(LocalPlayer player) { return DebugCamera.rendering() ? DebugCamera.camera().getPosition().y : player.getY(); }
    @Redirect(method="setupRender", at=@At(value="INVOKE", target="Lnet/minecraft/client/player/LocalPlayer;getZ()D"))
    private double lg2$z(LocalPlayer player) { return DebugCamera.rendering() ? DebugCamera.camera().getPosition().z : player.getZ(); }
    @Redirect(method="renderSky", at=@At(value="INVOKE", target="Lnet/minecraft/client/player/LocalPlayer;getEyePosition(F)Lnet/minecraft/world/phys/Vec3;"))
    private net.minecraft.world.phys.Vec3 lg2$skyPosition(LocalPlayer player, float partial) {
        return DebugCamera.rendering() ? DebugCamera.camera().getPosition() : player.getEyePosition(partial);
    }
    @Inject(method="shouldShowEntityOutlines", at=@At("HEAD"), cancellable=true)
    private void lg2$outlines(CallbackInfoReturnable<Boolean> cir) {
        if (DebugCamera.rendering()) cir.setReturnValue(false);
    }
    @Inject(method="setSectionDirty(IIIZ)V", at=@At("TAIL"))
    private void lg2$dirty(int x, int y, int z, boolean immediate, CallbackInfo ci) {
        DebugCamera.dirty((LevelRenderer)(Object)this, x, y, z, immediate);
    }
}
