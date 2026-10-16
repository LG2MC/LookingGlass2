package io.github.lg2mc.lookingglass2.fabric.client;

import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.minecraft.network.chat.Component;

import static net.fabricmc.fabric.api.client.command.v2.ClientCommandManager.literal;
import static net.fabricmc.fabric.api.client.command.v2.ClientCommandManager.argument;
import static com.mojang.brigadier.arguments.IntegerArgumentType.integer;
import static com.mojang.brigadier.arguments.IntegerArgumentType.getInteger;

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
                        .then(literal("range")
                                .executes(context -> {
                                    context.getSource().sendFeedback(Component.literal(
                                            "Camera radius: " + DebugCamera.radius() + " blocks per axis. Use /lg2 range <8-32>."));
                                    return 1;
                                })
                                .then(argument("blocks", integer(DebugCamera.MIN_RADIUS, DebugCamera.MAX_RADIUS))
                                        .executes(context -> {
                                            int radius = getInteger(context, "blocks");
                                            DebugCamera.setRadius(radius);
                                            context.getSource().sendFeedback(Component.literal(
                                                    "Camera radius set to " + radius + " blocks per axis ("
                                                            + (radius * 2 + 1) + " blocks across). Loaded chunks only."));
                                            return 1;
                                        })))
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
