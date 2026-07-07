package com.example.enderplus.client;

import com.example.enderplus.EnderPlusMod;
import com.example.enderplus.client.effect.DragonResurrectionEffect;
import com.example.enderplus.client.particle.ParticleManager;
import com.example.enderplus.client.particle.CustomParticleRenderer;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.rendering.v1.LevelRenderEvents;

public class EnderPlusModClient implements ClientModInitializer {
    public static final ParticleManager PARTICLE_MANAGER = new ParticleManager();
    public static final CustomParticleRenderer PARTICLE_RENDERER = new CustomParticleRenderer();
    public static final DragonResurrectionEffect DRAGON_EFFECT = new DragonResurrectionEffect();

    @Override
    public void onInitializeClient() {
        // Tick particles every client tick
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            PARTICLE_MANAGER.tick();
            DRAGON_EFFECT.tick();

            // Check for dragon respawn trigger from server mixin
            if (EnderPlusMod.dragonRespawnTriggered) {
                EnderPlusMod.dragonRespawnTriggered = false;
                DRAGON_EFFECT.trigger(
                    EnderPlusMod.respawnX,
                    EnderPlusMod.respawnY,
                    EnderPlusMod.respawnZ
                );
            }
        });

        // Render particles after entities, so they overlay the world
        LevelRenderEvents.AFTER_ENTITIES.register(context -> {
            PARTICLE_RENDERER.render(PARTICLE_MANAGER, context);
        });
    }
}
