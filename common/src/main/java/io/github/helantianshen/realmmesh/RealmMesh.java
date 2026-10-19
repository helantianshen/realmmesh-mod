package io.github.helantianshen.realmmesh;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class RealmMesh {
    public static final String MOD_ID = "realmmesh";
    private static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    private RealmMesh() {}

    public static void initialize() {
        LOGGER.info("RealmMesh shared initialization complete");
    }
}
