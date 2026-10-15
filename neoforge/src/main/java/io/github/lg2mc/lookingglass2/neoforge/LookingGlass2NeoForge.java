package io.github.lg2mc.lookingglass2.neoforge;

import dev.architectury.platform.forge.EventBuses;
import io.github.lg2mc.lookingglass2.LookingGlass2;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

@Mod(LookingGlass2.MOD_ID)
public final class LookingGlass2NeoForge {
    public LookingGlass2NeoForge() {
        EventBuses.registerModEventBus(LookingGlass2.MOD_ID, FMLJavaModLoadingContext.get().getModEventBus());
        LookingGlass2.init();
    }
}
