package io.github.lg2mc.lookingglass2.fabric.client;

import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.minecraft.network.chat.Component;
import static net.fabricmc.fabric.api.client.command.v2.ClientCommandManager.literal;
import static net.fabricmc.fabric.api.client.command.v2.ClientCommandManager.argument;
import static com.mojang.brigadier.arguments.IntegerArgumentType.integer;
import static com.mojang.brigadier.arguments.IntegerArgumentType.getInteger;
import static com.mojang.brigadier.arguments.BoolArgumentType.bool;
import static com.mojang.brigadier.arguments.BoolArgumentType.getBool;

final class DebugCameraCommands {
    private DebugCameraCommands() {}
    static void register() {
        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) ->
            dispatcher.register(literal("lg2")
                .then(literal("camera").executes(context -> {
                    if (!DebugCamera.capture()) return 0;
                    DebugDisplayTexture.requestCameraUpdate();
                    context.getSource().sendFeedback(Component.literal("Fixed world camera captured. Terrain may take a few updates to compile."));
                    return 1;
                }))
                .then(literal("distance")
                    .executes(context -> {
                        context.getSource().sendFeedback(Component.literal("Use /lg2 distance <2-8> (chunks), or /lg2 distance player."));
                        return 1;
                    })
                    .then(literal("player").executes(context -> {
                        CameraSettings s = DebugCamera.settings();
                        DebugCamera.configure(new CameraSettings(0, s.fovDegrees(), s.particles()));
                        context.getSource().sendFeedback(Component.literal("Camera distance follows the player setting, capped at 8 chunks for this prototype."));
                        return 1;
                    }))
                    .then(argument("chunks", integer(2, 8)).executes(context -> {
                        CameraSettings s = DebugCamera.settings();
                        int value = getInteger(context, "chunks");
                        DebugCamera.configure(new CameraSettings(value, s.fovDegrees(), s.particles()));
                        context.getSource().sendFeedback(Component.literal("Camera distance: " + value + " chunks. Already-loaded terrain only."));
                        return 1;
                    })))
                .then(literal("fov")
                    .then(literal("player").executes(context -> {
                        CameraSettings s = DebugCamera.settings();
                        DebugCamera.configure(new CameraSettings(s.distanceChunks(), 0, s.particles()));
                        context.getSource().sendFeedback(Component.literal("Camera FOV follows your FOV setting."));
                        return 1;
                    }))
                    .then(argument("degrees", integer(30, 110)).executes(context -> {
                        CameraSettings s = DebugCamera.settings();
                        int value = getInteger(context, "degrees");
                        DebugCamera.configure(new CameraSettings(s.distanceChunks(), value, s.particles()));
                        context.getSource().sendFeedback(Component.literal("Camera FOV: " + value + " degrees."));
                        return 1;
                    })))
                .then(literal("particles").then(argument("enabled", bool()).executes(context -> {
                    CameraSettings s = DebugCamera.settings();
                    boolean value = getBool(context, "enabled");
                    DebugCamera.configure(new CameraSettings(s.distanceChunks(), s.fovDegrees(), value));
                    context.getSource().sendFeedback(Component.literal("Camera particles: " + value));
                    return 1;
                })))
                .then(literal("range").executes(context -> {
                    context.getSource().sendFeedback(Component.literal("Block-radius mode was replaced. Use /lg2 distance <2-8> in chunks."));
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
