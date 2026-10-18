package io.github.lg2mc.lookingglass2.api.client;

import io.github.lg2mc.lookingglass2.fabric.client.CameraViews;

/** Experimental Fabric 1.20.1 entry point. Do not load this class on a dedicated server. */
public final class LookingGlassClient {
    private LookingGlassClient() {}
    /** Creates a world-bound view; each dimension must be 16..1024 pixels. No chunks are requested. */
    public static CameraView createView(int width, int height, CameraPose pose, CameraSettings settings) {
        return CameraViews.create(width, height, pose, settings);
    }
    /** Consumers should skip their screen surfaces during this pass to avoid recursive feeds. */
    public static boolean isRenderingView() { return CameraViews.rendering(); }
}
