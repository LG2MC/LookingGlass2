package io.github.lg2mc.lookingglass2.fabric.mixin;
import io.github.lg2mc.lookingglass2.fabric.client.DebugCamera;
import net.minecraft.client.Options;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
@Mixin(Options.class)
public abstract class OptionsMixin {
    @Inject(method="getEffectiveRenderDistance", at=@At("HEAD"), cancellable=true)
    private void lg2$distance(CallbackInfoReturnable<Integer> cir) {
        if (DebugCamera.rendering()) cir.setReturnValue(DebugCamera.distance());
    }
}
