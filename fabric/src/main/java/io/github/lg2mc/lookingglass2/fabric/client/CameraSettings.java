package io.github.lg2mc.lookingglass2.fabric.client;

/** Per-view options. Zero distance/FOV inherit the player's settings. Not a stable API yet. */
public record CameraSettings(int distanceChunks, int fovDegrees, boolean particles) {
    public static final CameraSettings DEFAULT = new CameraSettings(0, 0, true);

    public CameraSettings {
        if (distanceChunks != 0 && (distanceChunks < 2 || distanceChunks > 8)) throw new IllegalArgumentException("Distance must be 0 or 2–8 chunks");
        if (fovDegrees != 0 && (fovDegrees < 30 || fovDegrees > 110)) throw new IllegalArgumentException("FOV must be 0 or 30–110 degrees");
    }
}
