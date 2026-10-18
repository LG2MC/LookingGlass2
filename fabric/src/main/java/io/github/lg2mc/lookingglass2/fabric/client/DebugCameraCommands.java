package io.github.lg2mc.lookingglass2.fabric.client;

import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.minecraft.network.chat.Component;
import io.github.lg2mc.lookingglass2.api.client.CameraSettings;
import static com.mojang.brigadier.arguments.StringArgumentType.word;
import static com.mojang.brigadier.arguments.StringArgumentType.getString;
import static com.mojang.brigadier.arguments.FloatArgumentType.floatArg;
import static com.mojang.brigadier.arguments.FloatArgumentType.getFloat;
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
                    context.getSource().sendFeedback(Component.literal("Fixed world camera captured. Terrain may take a few updates to compile."));
                    return 1;
                }).then(argument("name", word()).executes(context -> {
                    String name = getString(context, "name");
                    if (!DebugCamera.capture(name)) return 0;
                    context.getSource().sendFeedback(Component.literal("Captured and selected camera: " + name));
                    return 1;
                })))
                .then(literal("use").then(argument("name", word()).executes(context -> {
                    String name = getString(context, "name");
                    if (!DebugCamera.use(name)) {
                        context.getSource().sendError(Component.literal("Unknown camera: " + name)); return 0;
                    }
                    context.getSource().sendFeedback(Component.literal("Unbound displays now show: " + name)); return 1;
                })))
                .then(literal("bind").then(argument("name", word()).executes(context -> {
                    if (!DebugCamera.bind(getString(context, "name"))) {
                        context.getSource().sendError(Component.literal("Look at a debug display and use an existing camera name.")); return 0;
                    }
                    context.getSource().sendFeedback(Component.literal("Display bound. Binding is client-only for this session.")); return 1;
                })))
                .then(literal("unbind").executes(context -> {
                    if (!DebugCamera.unbind()) { context.getSource().sendError(Component.literal("Look at a bound debug display.")); return 0; }
                    context.getSource().sendFeedback(Component.literal("Display follows the selected camera again.")); return 1;
                }))
                .then(literal("remove").then(argument("name", word()).executes(context -> {
                    if (!DebugCamera.remove(getString(context, "name"))) { context.getSource().sendError(Component.literal("Unknown camera.")); return 0; }
                    context.getSource().sendFeedback(Component.literal("Camera closed and resources released.")); return 1;
                })))
                .then(literal("list").executes(context -> {
                    context.getSource().sendFeedback(Component.literal(DebugCamera.list())); return 1;
                }))
                .then(literal("enabled").then(argument("enabled", bool()).executes(context -> {
                    var view = DebugCamera.selected();
                    if (view == null) { context.getSource().sendError(Component.literal("Capture a camera first.")); return 0; }
                    boolean value = getBool(context, "enabled");
                    view.setEnabled(value);
                    context.getSource().sendFeedback(Component.literal("Selected camera enabled: " + value)); return 1;
                })))
                .then(literal("movehere").executes(context -> {
                    if (!DebugCamera.moveHere()) { context.getSource().sendError(Component.literal("Capture a camera first.")); return 0; }
                    context.getSource().sendFeedback(Component.literal("Selected camera moved to your eye position; rotation kept.")); return 1;
                }))
                .then(literal("rotate").then(argument("yaw", floatArg(-360, 360))
                    .then(argument("pitch", floatArg(-90, 90)).executes(context -> {
                        if (!DebugCamera.rotate(getFloat(context, "yaw"), getFloat(context, "pitch"))) {
                            context.getSource().sendError(Component.literal("Capture a camera first.")); return 0;
                        }
                        context.getSource().sendFeedback(Component.literal("Selected camera rotation updated.")); return 1;
                    }))))
                .then(literal("fps")
                    .executes(context -> {
                        context.getSource().sendFeedback(Component.literal("Camera target: " + DebugCamera.settings().updateFps()
                                + " FPS. Use /lg2 fps <1-60>. Actual updates depend on game FPS and render cost."));
                        return 1;
                    })
                    .then(argument("rate", integer(1, 60)).executes(context -> {
                        CameraSettings s = DebugCamera.settings();
                        int value = getInteger(context, "rate");
                        DebugCamera.configure(new CameraSettings(s.distanceChunks(), s.fovDegrees(), s.particles(), value));
                        context.getSource().sendFeedback(Component.literal("Camera target set to " + value + " FPS."));
                        return 1;
                    })))
                .then(literal("distance")
                    .executes(context -> {
                        context.getSource().sendFeedback(Component.literal("Use /lg2 distance <2-8> (chunks), or /lg2 distance player."));
                        return 1;
                    })
                    .then(literal("player").executes(context -> {
                        CameraSettings s = DebugCamera.settings();
                        DebugCamera.configure(new CameraSettings(0, s.fovDegrees(), s.particles(), s.updateFps()));
                        context.getSource().sendFeedback(Component.literal("Camera distance follows the player setting, capped at 8 chunks for this prototype."));
                        return 1;
                    }))
                    .then(argument("chunks", integer(2, 8)).executes(context -> {
                        CameraSettings s = DebugCamera.settings();
                        int value = getInteger(context, "chunks");
                        DebugCamera.configure(new CameraSettings(value, s.fovDegrees(), s.particles(), s.updateFps()));
                        context.getSource().sendFeedback(Component.literal("Camera distance: " + value + " chunks. Already-loaded terrain only."));
                        return 1;
                    })))
                .then(literal("fov")
                    .then(literal("player").executes(context -> {
                        CameraSettings s = DebugCamera.settings();
                        DebugCamera.configure(new CameraSettings(s.distanceChunks(), 0, s.particles(), s.updateFps()));
                        context.getSource().sendFeedback(Component.literal("Camera FOV follows your FOV setting."));
                        return 1;
                    }))
                    .then(argument("degrees", integer(30, 110)).executes(context -> {
                        CameraSettings s = DebugCamera.settings();
                        int value = getInteger(context, "degrees");
                        DebugCamera.configure(new CameraSettings(s.distanceChunks(), value, s.particles(), s.updateFps()));
                        context.getSource().sendFeedback(Component.literal("Camera FOV: " + value + " degrees."));
                        return 1;
                    })))
                .then(literal("particles").then(argument("enabled", bool()).executes(context -> {
                    CameraSettings s = DebugCamera.settings();
                    boolean value = getBool(context, "enabled");
                    DebugCamera.configure(new CameraSettings(s.distanceChunks(), s.fovDegrees(), value, s.updateFps()));
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
