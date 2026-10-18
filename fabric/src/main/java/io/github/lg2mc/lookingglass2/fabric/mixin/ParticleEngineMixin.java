package io.github.lg2mc.lookingglass2.fabric.mixin;
import io.github.lg2mc.lookingglass2.fabric.client.CameraViews;
import net.minecraft.client.particle.ParticleEngine;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Mixin(ParticleEngine.class)
public abstract class ParticleEngineMixin {
    @Inject(method="render", at=@At("HEAD"), cancellable=true)
    private void lg2$particles(CallbackInfo ci) {
        if (CameraViews.rendering() && !CameraViews.particles()) ci.cancel();
    }
}
