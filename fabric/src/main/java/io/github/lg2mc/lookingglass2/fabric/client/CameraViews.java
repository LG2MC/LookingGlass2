package io.github.lg2mc.lookingglass2.fabric.client;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.systems.RenderSystem;
import io.github.lg2mc.lookingglass2.LookingGlass2;
import io.github.lg2mc.lookingglass2.api.client.*;
import io.github.lg2mc.lookingglass2.fabric.mixin.LevelRendererAccess;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.resources.ResourceLocation;

/** Internal scheduler and temporary render context; mixins only override the active view pass. */
public final class CameraViews {
    private static final Set<CameraViewImpl> VIEWS = new LinkedHashSet<>();
    private static long sequence;
    static long frame;
    static CameraViewImpl current;
    private CameraViews() {}
    static void checkThread() {
        RenderSystem.assertOnRenderThread();
        if (rendering()) throw new IllegalStateException("Do not mutate or sample camera views during a secondary render");
    }
    public static CameraView create(int width, int height, CameraPose pose, CameraSettings settings) {
        checkThread();
        if (width < 16 || height < 16 || width > 1024 || height > 1024) throw new IllegalArgumentException("View dimensions must be 16–1024");
        var level = Minecraft.getInstance().level;
        if (level == null) throw new IllegalStateException("A client world must be loaded before creating a view");
        CameraViewImpl view = new CameraViewImpl(level, new ResourceLocation("lookingglass2", "view/" + (++sequence)), width, height, pose, settings);
        VIEWS.add(view); return view;
    }
    static void remove(CameraViewImpl view) { VIEWS.remove(view); }
    public static boolean rendering() { return current != null; }
    public static Camera camera() { return current.CAMERA; }
    public static RenderTarget output() { return current.target; }
    public static int distance() { return current.effectiveDistance; }
    public static boolean particles() { return current.settings().particles(); }
    public static void beginFrame() {
        if (rendering()) return;
        checkThread(); frame++;
        Minecraft mc = Minecraft.getInstance();
        for (CameraViewImpl view : List.copyOf(VIEWS)) {
            if (view.level != mc.level) { view.close(); continue; }
            if (mc.player == null) continue;
            try { view.update(); }
            catch (RuntimeException error) {
                LookingGlass2.LOGGER.error("Camera view failed; other views remain active", error);
                view.failed(error);
            }
        }
    }
    public static void tick() {
        if (rendering()) return;
        for (CameraViewImpl view : VIEWS) if (view.renderer != null) view.renderer.tick();
    }
    public static void dirty(LevelRenderer source, int x, int y, int z, boolean immediate) {
        // Only forward from the main renderer: forwarding from one secondary to another would recurse.
        if (source != Minecraft.getInstance().levelRenderer) return;
        for (CameraViewImpl view : VIEWS) {
            if (view.renderer != null && view.level == Minecraft.getInstance().level)
                ((LevelRendererAccess)view.renderer).lg2$dirty(x, y, z, immediate);
        }
    }
    public static void reset() {
        if (!RenderSystem.isOnRenderThread()) { RenderSystem.recordRenderCall(CameraViews::reset); return; }
        checkThread();
        for (CameraViewImpl view : List.copyOf(VIEWS)) {
            try { view.close(); }
            catch (RuntimeException error) { LookingGlass2.LOGGER.error("Error closing camera view", error); }
        }
    }
}
