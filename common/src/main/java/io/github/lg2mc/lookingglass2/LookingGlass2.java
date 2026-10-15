package io.github.lg2mc.lookingglass2;

import dev.architectury.event.events.common.LifecycleEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Shared bootstrap. Remote world rendering is not implemented yet. */
public final class LookingGlass2 {
    public static final String MOD_ID = "lookingglass2";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    private LookingGlass2() {}

    public static void init() {
        LOGGER.info("LookingGlass 2 development bootstrap initialized");
        LifecycleEvent.SERVER_STARTED.register(server ->
                LOGGER.info("LookingGlass 2 server bootstrap ready; remote views are not implemented"));
    }
}
