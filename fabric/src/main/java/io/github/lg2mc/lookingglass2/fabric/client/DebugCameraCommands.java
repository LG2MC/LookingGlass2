package io.github.lg2mc.lookingglass2.fabric.client;

import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.minecraft.network.chat.Component;

import static net.fabricmc.fabric.api.client.command.v2.ClientCommandManager.literal;

final class DebugCameraCommands {
    private DebugCameraCommands() {}

    static void register() {
        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) ->
                dispatcher.register(literal("lg2")
                        .then(literal("camera").executes(context -> {
                            if (!DebugCamera.capture()) return 0;
                            DebugDisplayTexture.requestCameraUpdate();
                            context.getSource().sendFeedback(Component.literal(
                                    "Fixed terrain camera captured. Nearby blocks only; no entities, fluids or sky yet."));
                            return 1;
                        }))
                        .then(literal("color").executes(context -> {
                            DebugCamera.clear();
                            context.getSource().sendFeedback(Component.literal("Returned to blue/cyan test mode."));
                            return 1;
                        }))
                        .then(literal("status").executes(context -> {
                            context.getSource().sendFeedback(Component.literal(DebugCamera.status()));
                            return 1;
                        }))));
    }
}
