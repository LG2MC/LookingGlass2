package io.github.lg2mc.lookingglass2.api.client;

import java.util.Objects;
import net.minecraft.world.phys.Vec3;

/** Minecraft yaw/pitch in degrees, with an absolute eye position. Pitch is -90..90. */
public record CameraPose(Vec3 position, float yaw, float pitch) {
    public CameraPose {
        Objects.requireNonNull(position, "position");
        if (!Double.isFinite(position.x) || !Double.isFinite(position.y) || !Double.isFinite(position.z)
                || !Float.isFinite(yaw) || !Float.isFinite(pitch) || pitch < -90 || pitch > 90) {
            throw new IllegalArgumentException("Camera pose must be finite; pitch must be -90..90");
        }
    }
}
