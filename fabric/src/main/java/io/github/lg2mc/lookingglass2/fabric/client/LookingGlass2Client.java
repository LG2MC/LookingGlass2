package io.github.lg2mc.lookingglass2.fabric.client;

import io.github.lg2mc.lookingglass2.fabric.debug.DebugDisplay;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.resource.ResourceManagerHelper;
import net.fabricmc.fabric.api.resource.SimpleSynchronousResourceReloadListener;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderers;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.resources.ResourceManager;

public final class LookingGlass2Client implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        DebugCameraCommands.register();
        BlockEntityRenderers.register(DebugDisplay.ENTITY_TYPE, DebugDisplayRenderer::new);
        ClientTickEvents.END_CLIENT_TICK.register(client -> DebugCamera.tick());
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> DebugDisplayTexture.reset());
        ClientLifecycleEvents.CLIENT_STOPPING.register(client -> DebugDisplayTexture.reset());
        ResourceManagerHelper.get(PackType.CLIENT_RESOURCES).registerReloadListener(
                new SimpleSynchronousResourceReloadListener() {
                    @Override
                    public ResourceLocation getFabricId() {
                        return new ResourceLocation("lookingglass2", "debug_display_reset");
                    }

                    @Override
                    public void onResourceManagerReload(ResourceManager manager) {
                        DebugDisplayTexture.reset();
                    }
                });
    }
}
