package com.example.enderplus;

import com.example.enderplus.network.DragonRespawnPayload;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.minecraft.resources.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class EnderPlusMod implements ModInitializer {
    public static final String MOD_ID = "enderplus";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    @Override
    public void onInitialize() {
        LOGGER.info("EnderPlus mod initializing...");

        // Register the dragon respawn packet for server→client communication
        PayloadTypeRegistry.clientboundPlay().register(
            DragonRespawnPayload.TYPE,
            DragonRespawnPayload.STREAM_CODEC
        );

        LOGGER.info("EnderPlus mod initialized successfully!");
    }

    public static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath(MOD_ID, path);
    }
}
