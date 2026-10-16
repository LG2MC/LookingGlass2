package io.github.lg2mc.lookingglass2.fabric.mixin;
import com.mojang.blaze3d.vertex.VertexBuffer;
import net.minecraft.client.renderer.LevelRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;
@Mixin(LevelRenderer.class)
public interface LevelRendererAccess {
    @Invoker("setSectionDirty") void lg2$dirty(int x, int y, int z, boolean immediate);
    @Accessor("starBuffer") VertexBuffer lg2$stars();
    @Accessor("skyBuffer") VertexBuffer lg2$sky();
    @Accessor("darkBuffer") VertexBuffer lg2$dark();
    @Accessor("cloudBuffer") VertexBuffer lg2$clouds();
}
