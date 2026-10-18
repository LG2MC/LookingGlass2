package io.github.lg2mc.lookingglass2.fabric.client;

import io.github.lg2mc.lookingglass2.api.client.*;
import io.github.lg2mc.lookingglass2.fabric.debug.DebugDisplay;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.BlockHitResult;
import java.util.LinkedHashMap;
import java.util.HashMap;
import java.util.Map;

/** Development consumer of the public API. No renderer ownership or mixin hooks live here. */
final class DebugCamera {
    private static final Map<String, CameraView> VIEWS = new LinkedHashMap<>();
    private static final Map<BlockPos, String> BINDINGS = new HashMap<>();
    private static String selected = "main";
    private static CameraSettings defaults = CameraSettings.DEFAULT;
    private DebugCamera() {}
    private static void prune() { VIEWS.values().removeIf(CameraView::isClosed); }
    static CameraView selected() { prune(); return VIEWS.get(selected); }
    static boolean capture() { return capture(selected); }
    static boolean capture(String name) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) return false;
        prune();
        CameraPose pose = new CameraPose(mc.player.getEyePosition(), mc.player.getYRot(), mc.player.getXRot());
        CameraView view = VIEWS.get(name);
        if (view == null) {
            view = LookingGlassClient.createView(256, 256, pose, defaults);
            VIEWS.put(name, view);
        } else { view.setPose(pose); view.retry(); }
        selected = name;
        return true;
    }
    static boolean use(String name) {
        prune(); if (!VIEWS.containsKey(name)) return false;
        selected = name; return true;
    }
    static boolean remove(String name) {
        CameraView view = VIEWS.remove(name);
        if (view == null) return false;
        view.close(); BINDINGS.values().removeIf(name::equals); return true;
    }
    static String list() {
        prune(); return "Cameras: " + String.join(", ", VIEWS.keySet()) + " | selected: " + selected;
    }
    static boolean bind(String name) {
        prune(); if (!VIEWS.containsKey(name)) return false;
        BlockPos pos = targetedDisplay();
        if (pos == null) return false;
        BINDINGS.put(pos.immutable(), name); return true;
    }
    static boolean unbind() {
        BlockPos pos = targetedDisplay(); return pos != null && BINDINGS.remove(pos) != null;
    }
    private static BlockPos targetedDisplay() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level != null && mc.hitResult instanceof BlockHitResult hit
                && mc.level.getBlockEntity(hit.getBlockPos()) instanceof DebugDisplay.DisplayEntity) return hit.getBlockPos();
        return null;
    }
    static CameraView viewFor(BlockPos pos) {
        prune(); return VIEWS.get(BINDINGS.getOrDefault(pos, selected));
    }
    static CameraSettings settings() { CameraView view = selected(); return view == null ? defaults : view.settings(); }
    static void configure(CameraSettings settings) {
        defaults = settings;
        CameraView view = selected(); if (view != null) view.setSettings(settings);
    }
    static boolean rotate(float yaw, float pitch) {
        CameraView view = selected(); if (view == null) return false;
        view.rotateTo(yaw, pitch); return true;
    }
    static boolean moveHere() {
        CameraView view = selected();
        Minecraft mc = Minecraft.getInstance();
        if (view == null || mc.player == null) return false;
        view.moveTo(mc.player.getEyePosition()); return true;
    }
    static void clear() {
        for (CameraView view : VIEWS.values()) view.close();
        VIEWS.clear(); BINDINGS.clear(); selected = "main";
    }
    static String status() {
        CameraView view = selected();
        if (view == null) return "Color mode. Capture /lg2 camera <name>. " + list();
        CameraStatistics stats = view.statistics();
        CameraSettings s = view.settings();
        return String.format(java.util.Locale.ROOT,
                "%s | %.1f %.1f %.1f | yaw %.1f pitch %.1f | target %d FPS / %.1f actual | CPU %.2f avg / %.2f max ms (%d samples) | distance %d (0=player) | FOV %d (0=player) | particles %s%s",
                selected, view.pose().position().x, view.pose().position().y, view.pose().position().z,
                view.pose().yaw(), view.pose().pitch(), s.updateFps(), stats.actualFps(), stats.averageMillis(), stats.maximumMillis(), stats.samples(),
                s.distanceChunks(), s.fovDegrees(), s.particles(), view.failure().map(f -> " | FAILED: " + f).orElse(""));
    }
}
