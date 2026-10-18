package io.github.lg2mc.lookingglass2.api.client;

import java.util.Optional;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;

/**
 * Experimental client-only, current-world camera. Call from the render thread, outside a
 * secondary view render. Close when its owning screen/block/controller no longer needs it.
 * World changes and resource reloads close the handle automatically; check isClosed and recreate.
 */
public interface CameraView extends AutoCloseable {
    CameraPose pose();
    /** Changes the next scheduled frame's pose without bypassing the FPS setting. */
    void setPose(CameraPose pose);
    default void moveTo(Vec3 position) { setPose(new CameraPose(position, pose().yaw(), pose().pitch())); }
    default void rotateTo(float yaw, float pitch) { setPose(new CameraPose(pose().position(), yaw, pitch)); }
    CameraSettings settings();
    void setSettings(CameraSettings settings);
    int width();
    int height();
    /**
     * Call each frame the view is displayed. Marks it visible and returns its stable texture ID.
     * The first frame is black until the next scheduled world pass. Framebuffer UVs have
     * top V=1, bottom V=0, left U=0, right U=1. Do not release this texture yourself.
     */
    ResourceLocation texture();
    /** Keeps a view updating without obtaining its texture; call once per displayed game frame. */
    void markVisible();
    boolean isReady();
    boolean isClosed();
    boolean isEnabled();
    /** Disabled views retain their last image but stop updates and release terrain resources. */
    void setEnabled(boolean enabled);
    CameraStatistics statistics();
    Optional<String> failure();
    /** Clears an isolated render failure so the next visible frame can try again. */
    void retry();
    /** Idempotent. GPU and terrain resources are released; the handle cannot be reused. */
    @Override void close();
}
