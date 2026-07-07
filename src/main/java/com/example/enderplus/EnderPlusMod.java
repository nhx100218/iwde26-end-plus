package com.example.enderplus;

import net.fabricmc.api.ModInitializer;
import net.minecraft.resources.ResourceLocation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class EnderPlusMod implements ModInitializer {
    public static final String MOD_ID = "enderplus";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    /**
     * Set to true by the server-side mixin when the Ender Dragon respawn ritual begins.
     * The client reads this flag each tick to trigger the visual effect.
     * Works in singleplayer (integrated server shares the JVM).
     */
    public static volatile boolean dragonRespawnTriggered = false;
    public static double respawnX, respawnY, respawnZ;

    @Override
    public void onInitialize() {
        LOGGER.info("EnderPlus initialized - custom particle system ready!");
    }

    public static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath(MOD_ID, path);
    }
}
