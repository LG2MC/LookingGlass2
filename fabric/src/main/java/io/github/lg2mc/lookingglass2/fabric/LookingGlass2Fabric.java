package io.github.lg2mc.lookingglass2.fabric;

import io.github.lg2mc.lookingglass2.LookingGlass2;
import net.fabricmc.api.ModInitializer;

public final class LookingGlass2Fabric implements ModInitializer {
    @Override
    public void onInitialize() {
        LookingGlass2.init();
        io.github.lg2mc.lookingglass2.fabric.debug.DebugDisplay.register();
    }
}
