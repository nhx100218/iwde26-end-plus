package com.example.enderplus.client;

import com.example.enderplus.EnderPlusMod;
import com.example.enderplus.client.effect.DragonResurrectionEffect;
import com.example.enderplus.client.particle.ParticleRenderer;
import com.example.enderplus.client.particle.ParticleTexture;
import com.example.enderplus.network.DragonRespawnPayload;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderEvents;
import net.minecraft.client.Minecraft;

/**
 * Client-side initializer for EnderPlus.
 * Sets up:
 * - Custom particle system (bypasses vanilla particles)
 * - Dragon resurrection visual effects
 * - Networking handler for dragon respawn events
 * - Level render hooks for particle and effect rendering
 */
public class EnderPlusModClient implements ClientModInitializer {

    private final ParticleRenderer particleRenderer = new ParticleRenderer();
    private final DragonResurrectionEffect dragonEffect = new DragonResurrectionEffect();
    private boolean particleTextureReady = false;

    @Override
    public void onInitializeClient() {
        EnderPlusMod.LOGGER.info("EnderPlus client initializing...");

        // --- Networking: Receive dragon respawn events from server ---
        ClientPlayNetworking.registerGlobalReceiver(
            DragonRespawnPayload.TYPE,
            (payload, context) -> {
                // Execute on the main client thread
                context.client().execute(() -> {
                    EnderPlusMod.LOGGER.info("Dragon respawn effect triggered at {}", payload.pos());
                    dragonEffect.trigger(payload.pos());
                });
            }
        );

        // --- Tick handler: Update particles and effects each client tick ---
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (client.isPaused()) return;

            // Lazy texture initialization on first tick (render thread is active)
            if (!particleTextureReady) {
                ParticleTexture.initialize();
                particleTextureReady = true;
            }

            // Update custom particle system
            particleRenderer.tick();

            // Update dragon resurrection effects (spawns particles into the renderer)
            dragonEffect.tick(particleRenderer);
        });

        // --- Render handler: Draw particles and effects in the level ---
        LevelRenderEvents.AFTER_TRANSLUCENT_FEATURES.register(context -> {
            if (!particleTextureReady) return;

            var client = Minecraft.getInstance();
            var camera = client.gameRenderer.getMainCamera();
            float tickDelta = client.getDeltaTracker().getGameTimeDeltaPartialTick(false);

            // Render particles first (standard alpha blend, depth-tested)
            particleRenderer.render(context.poseStack(), camera, tickDelta);

            // Render light pillar (additive blend for glow)
            dragonEffect.render(camera, tickDelta);
        });

        EnderPlusMod.LOGGER.info("EnderPlus client initialized successfully!");
    }

    /**
     * Get the particle renderer for direct access (useful for debugging or external triggers).
     */
    public ParticleRenderer getParticleRenderer() {
        return particleRenderer;
    }
}
