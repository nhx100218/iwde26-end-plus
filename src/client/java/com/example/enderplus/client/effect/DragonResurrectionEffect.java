package com.example.enderplus.client.effect;

import com.example.enderplus.client.EnderPlusModClient;
import com.example.enderplus.client.particle.CustomParticle;
import com.example.enderplus.client.particle.ParticleManager;

import java.util.Random;

/**
 * Orchestrates a spectacular Ender Dragon resurrection visual effect.
 *
 * Timeline (10 seconds = 200 ticks):
 *   Phase 1  (0-2s):   Rune circles materialize around the portal
 *   Phase 2  (1-4s):   Light beams erupt upward from the portal
 *   Phase 3  (2-6s):   Runic explosion — particles burst outward
 *   Phase 4  (4-8s):   Massive vortex spiral, energy convergence
 *   Phase 5  (6-10s):  Shockwave rings, final flash, fade out
 */
public class DragonResurrectionEffect {

    private static final int TOTAL_DURATION = 200; // 10 seconds
    private static final Random RANDOM = new Random();

    private boolean active = false;
    private int tick = 0;
    private double originX, originY, originZ;

    // Purple color palette
    private static final float[][] PURPLE_PALETTE = {
        { 0.55f, 0.05f, 0.55f }, // dark purple
        { 0.60f, 0.10f, 0.70f }, // medium purple
        { 0.70f, 0.20f, 0.85f }, // bright purple
        { 0.85f, 0.35f, 1.00f }, // violet
        { 0.95f, 0.50f, 1.00f }, // light violet
        { 0.40f, 0.00f, 0.50f }, // deep purple
        { 0.90f, 0.10f, 0.95f }, // magenta-purple
        { 0.75f, 0.00f, 1.00f }, // electric purple
    };

    /**
     * Triggers the effect at the given world coordinates.
     */
    public void trigger(double x, double y, double z) {
        this.active = true;
        this.tick = 0;
        this.originX = x;
        this.originY = y;
        this.originZ = z;
    }

    /**
     * Called each client tick. Advances the effect timeline and spawns particles.
     */
    public void tick() {
        if (!active) return;

        ParticleManager pm = EnderPlusModClient.PARTICLE_MANAGER;

        if (tick < TOTAL_DURATION) {
            // ---- Phase 1: Rune materialization (0-60 ticks) ----
            if (tick < 60) {
                spawnRuneCircle(pm, tick);
                spawnFloatingRunes(pm, tick);
            }

            // ---- Phase 2: Light beams (20-90 ticks) ----
            if (tick >= 20 && tick < 90) {
                spawnLightBeams(pm, tick);
            }

            // ---- Phase 3: Runic explosion (40-130 ticks) ----
            if (tick >= 40 && tick < 130) {
                spawnRunicExplosion(pm, tick);
                spawnBurstParticles(pm, tick);
            }

            // ---- Phase 4: Vortex spiral (80-180 ticks) ----
            if (tick >= 80 && tick < 180) {
                spawnVortex(pm, tick);
                spawnEnergyConvergence(pm, tick);
            }

            // ---- Phase 5: Shockwave & finale (120-200 ticks) ----
            if (tick >= 120 && tick < TOTAL_DURATION) {
                spawnShockwave(pm, tick);
                spawnFinalFlash(pm, tick);
            }

            // Ambient sparkles throughout
            spawnAmbientSparkles(pm);
        }

        tick++;

        if (tick >= TOTAL_DURATION + 20) {
            active = false;
            tick = 0;
        }
    }

    public boolean isActive() {
        return active;
    }

    // ================================================================
    // Phase 1: Rune Circles
    // ================================================================

    /**
     * Spawns particles in concentric circles around the portal, like
     * summoning runes appearing on the ground.
     */
    private void spawnRuneCircle(ParticleManager pm, int effectTick) {
        float progress = Math.min(1f, effectTick / 45f); // expand over 2.25s
        float alpha = easeInOut(progress);

        int numRings = 3 + effectTick / 15;
        for (int ring = 0; ring < numRings; ring++) {
            float ringRadius = (1.5f + ring * 1.2f) * progress;
            int particlesPerRing = 16 + ring * 8;
            float ringY = originY + 0.1f + ring * 0.15f;

            // Each ring has rune-like gaps (not a full circle)
            for (int i = 0; i < particlesPerRing; i++) {
                // Skip some particles to create "rune" gaps
                int segment = i / 4;
                if (i % 4 == 3 && RANDOM.nextFloat() < 0.5f) continue;

                float angle = (float) (2.0 * Math.PI * i / particlesPerRing);
                angle += effectTick * 0.02f; // slow rotation

                float px = (float) (originX + Math.cos(angle) * ringRadius);
                float pz = (float) (originZ + Math.sin(angle) * ringRadius);
                float py = ringY + RANDOM.nextFloat() * 0.1f;

                float[] color = PURPLE_PALETTE[RANDOM.nextInt(PURPLE_PALETTE.length)];

                CustomParticle p = pm.spawn(px, (float) py, pz);
                p.r = color[0]; p.g = color[1]; p.b = color[2];
                p.a = alpha * 0.9f;
                p.scale = 0.15f + RANDOM.nextFloat() * 0.2f;
                p.lifetime = 40 + RANDOM.nextInt(30);
                p.maxAge = p.lifetime;
                p.gravity = 0f;
                p.velY = 0.02f + RANDOM.nextFloat() * 0.03f;
                p.additiveBlend = true;
                p.fadeInTicks = 5;
                p.fadeOutTicks = 15;
            }
        }

        // Inner rune symbols (floating above the rings)
        if (effectTick > 15 && effectTick % 4 == 0) {
            float symbolRadius = 1.0f + progress * 2.5f;
            float symbolY = originY + 0.3f + progress * 1.5f;
            int symbolParticles = 6 + (int)(progress * 10);

            for (int i = 0; i < symbolParticles; i++) {
                float angle = (float) (2.0 * Math.PI * i / symbolParticles + effectTick * 0.05f);
                float px = (float) (originX + Math.cos(angle) * symbolRadius);
                float pz = (float) (originZ + Math.sin(angle) * symbolRadius);

                float[] color = PURPLE_PALETTE[RANDOM.nextInt(PURPLE_PALETTE.length)];

                CustomParticle p = pm.spawn(px, symbolY, pz);
                p.r = color[0]; p.g = color[1]; p.b = color[2];
                p.a = alpha * 0.8f;
                p.scale = 0.2f + RANDOM.nextFloat() * 0.3f;
                p.lifetime = 30 + RANDOM.nextInt(25);
                p.maxAge = p.lifetime;
                p.velY = 0.05f;
                p.additiveBlend = true;
                p.fadeOutTicks = 12;
            }
        }
    }

    /**
     * Floating rune-like particles drifting upward.
     */
    private void spawnFloatingRunes(ParticleManager pm, int effectTick) {
        if (effectTick % 3 != 0) return;

        int count = 2 + RANDOM.nextInt(3);
        for (int i = 0; i < count; i++) {
            float angle = RANDOM.nextFloat() * (float)(2 * Math.PI);
            float radius = 1.5f + RANDOM.nextFloat() * 4f;
            float px = (float) (originX + Math.cos(angle) * radius);
            float pz = (float) (originZ + Math.sin(angle) * radius);
            float py = originY + 0.2f + RANDOM.nextFloat() * 2f;

            float[] color = PURPLE_PALETTE[RANDOM.nextInt(PURPLE_PALETTE.length)];

            CustomParticle p = pm.spawn(px, py, pz);
            p.r = color[0]; p.g = color[1]; p.b = color[2];
            p.a = 0.85f;
            p.scale = 0.08f + RANDOM.nextFloat() * 0.15f;
            p.lifetime = 50 + RANDOM.nextInt(40);
            p.maxAge = p.lifetime;
            p.velY = 0.03f + RANDOM.nextFloat() * 0.08f;
            p.velX = (RANDOM.nextFloat() - 0.5f) * 0.03f;
            p.velZ = (RANDOM.nextFloat() - 0.5f) * 0.03f;
            p.gravity = -0.0005f; // slight upward float
            p.additiveBlend = true;
            p.fadeInTicks = 8;
            p.fadeOutTicks = 20;
            p.rotationSpeed = (RANDOM.nextFloat() - 0.5f) * 0.15f;
        }
    }

    // ================================================================
    // Phase 2: Light Beams
    // ================================================================

    /**
     * Vertical light beams shooting up from the portal.
     */
    private void spawnLightBeams(ParticleManager pm, int effectTick) {
        int localTick = effectTick - 20;
        float beamProgress = Math.min(1f, localTick / 50f);
        int numBeams = 5 + (int)(beamProgress * 8);

        for (int beam = 0; beam < numBeams; beam++) {
            float beamAngle = (float) (2.0 * Math.PI * beam / numBeams + effectTick * 0.03f);
            float beamRadius = 1.0f + beam * 0.4f;
            float bx = (float) (originX + Math.cos(beamAngle) * beamRadius);
            float bz = (float) (originZ + Math.sin(beamAngle) * beamRadius);

            // Beam column: particles rising from base to height
            float beamHeight = 3f + beamProgress * 12f;
            int particlesInBeam = 4 + (int)(beamHeight / 2);

            for (int j = 0; j < particlesInBeam; j++) {
                float by = originY + (float) j / particlesInBeam * beamHeight;
                float wobbleX = (RANDOM.nextFloat() - 0.5f) * 0.3f;
                float wobbleZ = (RANDOM.nextFloat() - 0.5f) * 0.3f;

                float[] color = PURPLE_PALETTE[RANDOM.nextInt(PURPLE_PALETTE.length)];

                CustomParticle p = pm.spawn(bx + wobbleX, by, bz + wobbleZ);
                p.r = color[0]; p.g = color[1]; p.b = color[2];
                p.a = 0.7f * (1f - (float) j / particlesInBeam); // brighter at base
                p.scale = 0.12f + RANDOM.nextFloat() * 0.18f;
                p.lifetime = 15 + RANDOM.nextInt(20);
                p.maxAge = p.lifetime;
                p.velY = 0.15f + RANDOM.nextFloat() * 0.3f;
                p.velX = wobbleX * 0.5f;
                p.velZ = wobbleZ * 0.5f;
                p.gravity = 0.01f;
                p.additiveBlend = true;
                p.fadeOutTicks = 10;
            }
        }

        // Central massive beam
        if (effectTick % 2 == 0) {
            float centralHeight = beamProgress * 10f;
            for (int j = 0; j < 20; j++) {
                float by = originY + centralHeight * j / 20f;
                float wobble = (RANDOM.nextFloat() - 0.5f) * 0.5f;

                CustomParticle p = pm.spawn(originX + wobble, by, originZ + wobble);
                p.r = 0.9f; p.g = 0.4f; p.b = 1.0f;
                p.a = 0.9f * (1f - (float) j / 20f);
                p.scale = 0.25f;
                p.lifetime = 12 + RANDOM.nextInt(15);
                p.maxAge = p.lifetime;
                p.velY = 0.4f + RANDOM.nextFloat() * 0.3f;
                p.additiveBlend = true;
                p.fadeOutTicks = 8;
            }
        }
    }

    // ================================================================
    // Phase 3: Runic Explosion
    // ================================================================

    /**
     * Particles burst outward in rune-like patterns from the center.
     */
    private void spawnRunicExplosion(ParticleManager pm, int effectTick) {
        int localTick = effectTick - 40;
        float intensity = (float) Math.sin(localTick / 90f * Math.PI); // peak then fade

        if (localTick % 3 != 0) return;

        int branches = 8 + localTick / 10; // more branches over time
        for (int branch = 0; branch < branches; branch++) {
            float baseAngle = (float) (2.0 * Math.PI * branch / branches);
            float branchLength = 3f + intensity * 8f;

            // Particles along the branch
            int branchParticles = 5 + (int)(intensity * 10);
            for (int j = 0; j < branchParticles; j++) {
                float dist = (float) j / branchParticles * branchLength;
                float angle = baseAngle + (RANDOM.nextFloat() - 0.5f) * 0.5f;
                float px = (float) (originX + Math.cos(angle) * dist);
                float pz = (float) (originZ + Math.sin(angle) * dist);
                float py = originY + dist * 0.3f + RANDOM.nextFloat() * 2f;

                float[] color = PURPLE_PALETTE[RANDOM.nextInt(PURPLE_PALETTE.length)];

                CustomParticle p = pm.spawn(px, py, pz);
                p.r = color[0]; p.g = color[1]; p.b = color[2];
                p.a = intensity * 0.85f;
                p.scale = 0.1f + (1f - (float) j / branchParticles) * 0.25f;
                p.lifetime = 30 + RANDOM.nextInt(40);
                p.maxAge = p.lifetime;

                // Outward velocity
                float speed = 0.1f + intensity * 0.4f;
                p.velX = (float) Math.cos(angle) * speed * (1f - (float) j / branchParticles);
                p.velZ = (float) Math.sin(angle) * speed * (1f - (float) j / branchParticles);
                p.velY = 0.05f + RANDOM.nextFloat() * 0.2f;
                p.gravity = 0.005f;
                p.additiveBlend = true;
                p.fadeInTicks = 5;
                p.fadeOutTicks = 20;
                p.rotationSpeed = (RANDOM.nextFloat() - 0.5f) * 0.2f;
            }
        }
    }

    /**
     * Omni-directional burst of glowing particles.
     */
    private void spawnBurstParticles(ParticleManager pm, int effectTick) {
        int localTick = effectTick - 40;
        if (localTick % 2 != 0) return;

        int count = 3 + RANDOM.nextInt(5);
        for (int i = 0; i < count; i++) {
            float yaw = RANDOM.nextFloat() * (float)(2 * Math.PI);
            float pitch = (RANDOM.nextFloat() - 0.5f) * (float) Math.PI;
            float speed = 0.15f + RANDOM.nextFloat() * 0.5f;
            float height = originY + 1f + RANDOM.nextFloat() * 4f;

            float[] color = PURPLE_PALETTE[RANDOM.nextInt(PURPLE_PALETTE.length)];

            CustomParticle p = pm.spawn(originX, height, originZ);
            p.r = color[0]; p.g = color[1]; p.b = color[2];
            p.a = 0.8f;
            p.scale = 0.1f + RANDOM.nextFloat() * 0.2f;
            p.lifetime = 35 + RANDOM.nextInt(30);
            p.maxAge = p.lifetime;
            p.velX = (float) (Math.cos(yaw) * Math.cos(pitch) * speed);
            p.velY = (float) (Math.sin(pitch) * speed);
            p.velZ = (float) (Math.sin(yaw) * Math.cos(pitch) * speed);
            p.gravity = 0.003f;
            p.additiveBlend = true;
            p.fadeOutTicks = 15;
        }
    }

    // ================================================================
    // Phase 4: Vortex Spiral
    // ================================================================

    /**
     * Massive spiral vortex rising from the portal.
     */
    private void spawnVortex(ParticleManager pm, int effectTick) {
        int localTick = effectTick - 80;
        float vortexHeight = 2f + (localTick / 100f) * 18f; // grows from 2 to 20 blocks tall
        float vortexRadius = 3f + (float) Math.sin(localTick / 100f * Math.PI) * 6f; // expands then contracts

        int numSpirals = 6;
        for (int s = 0; s < numSpirals; s++) {
            float baseAngle = (float) (2.0 * Math.PI * s / numSpirals);
            int pointsPerSpiral = (int)(vortexHeight * 4);

            for (int i = 0; i < pointsPerSpiral; i++) {
                float t = (float) i / pointsPerSpiral;
                float y = t * vortexHeight;
                float angle = baseAngle + t * (float)(2.5 * Math.PI) // spiral twist
                              + effectTick * 0.08f; // rotation over time
                float r = vortexRadius * (1f - t * 0.4f); // narrows at top

                float px = (float) (originX + Math.cos(angle) * r);
                float pz = (float) (originZ + Math.sin(angle) * r);

                float[] color = PURPLE_PALETTE[Math.min(PURPLE_PALETTE.length - 1,
                    (int)(t * PURPLE_PALETTE.length))];

                CustomParticle p = pm.spawn(px, originY + y, pz);
                p.r = color[0]; p.g = color[1]; p.b = color[2];
                p.a = 0.6f * (1f - t * 0.3f);
                p.scale = 0.08f + (1f - t) * 0.2f;
                p.lifetime = 20 + RANDOM.nextInt(25);
                p.maxAge = p.lifetime;
                p.velY = 0.1f;
                p.velX = (RANDOM.nextFloat() - 0.5f) * 0.1f;
                p.velZ = (RANDOM.nextFloat() - 0.5f) * 0.1f;
                p.additiveBlend = true;
                p.fadeInTicks = 3;
                p.fadeOutTicks = 10;
                p.rotationSpeed = 0.1f + RANDOM.nextFloat() * 0.2f;
            }
        }
    }

    /**
     * Particles converging inward toward the center (energy building up).
     */
    private void spawnEnergyConvergence(ParticleManager pm, int effectTick) {
        int localTick = effectTick - 80;
        if (localTick % 5 != 0) return;

        int count = 8 + RANDOM.nextInt(12);
        for (int i = 0; i < count; i++) {
            float angle = RANDOM.nextFloat() * (float)(2 * Math.PI);
            float startRadius = 6f + RANDOM.nextFloat() * 8f;
            float height = originY + 1f + RANDOM.nextFloat() * 10f;

            float px = (float) (originX + Math.cos(angle) * startRadius);
            float pz = (float) (originZ + Math.sin(angle) * startRadius);

            float[] color = PURPLE_PALETTE[RANDOM.nextInt(PURPLE_PALETTE.length)];

            CustomParticle p = pm.spawn(px, height, pz);
            p.r = color[0]; p.g = color[1]; p.b = color[2];
            p.a = 0.7f;
            p.scale = 0.06f + RANDOM.nextFloat() * 0.12f;
            p.lifetime = 40 + RANDOM.nextInt(30);
            p.maxAge = p.lifetime;

            // Velocity inward toward center
            float inwardSpeed = 0.15f + RANDOM.nextFloat() * 0.25f;
            p.velX = (float) -Math.cos(angle) * inwardSpeed;
            p.velZ = (float) -Math.sin(angle) * inwardSpeed;
            p.velY = -0.05f + RANDOM.nextFloat() * 0.1f; // slight vertical drift
            p.additiveBlend = true;
            p.fadeOutTicks = 18;
            p.rotationSpeed = 0.05f;
        }
    }

    // ================================================================
    // Phase 5: Shockwave & Finale
    // ================================================================

    /**
     * Expanding shockwave rings from the portal.
     */
    private void spawnShockwave(ParticleManager pm, int effectTick) {
        int localTick = effectTick - 120;

        // Spawn new shockwave rings periodically
        if (localTick % 25 == 0 || localTick == 0) {
            float ringRadius = 0.5f;
            int particles = 60;

            for (int i = 0; i < particles; i++) {
                float angle = (float) (2.0 * Math.PI * i / particles);
                float px = (float) (originX + Math.cos(angle) * ringRadius);
                float pz = (float) (originZ + Math.sin(angle) * ringRadius);
                float py = originY + 0.3f + (i % 3) * 0.5f;

                float[] color = PURPLE_PALETTE[RANDOM.nextInt(PURPLE_PALETTE.length)];

                CustomParticle p = pm.spawn(px, py, pz);
                p.r = color[0]; p.g = color[1]; p.b = color[2];
                p.a = 0.9f;
                p.scale = 0.15f;
                p.lifetime = 30;
                p.maxAge = p.lifetime;

                // Outward velocity
                float speed = 0.3f + RANDOM.nextFloat() * 0.3f;
                p.velX = (float) Math.cos(angle) * speed;
                p.velZ = (float) Math.sin(angle) * speed;
                p.velY = 0.02f;
                p.additiveBlend = true;
                p.fadeOutTicks = 20;
                p.fadeInTicks = 2;
            }
        }
    }

    /**
     * Bright final flash as the dragon emerges.
     */
    private void spawnFinalFlash(ParticleManager pm, int effectTick) {
        int localTick = effectTick - 120;
        float progress = Math.min(1f, localTick / 80f);
        float intensity = (float) (1.0 - Math.pow(1.0 - progress, 4)); // rapid rise then plateau

        if (localTick % 2 != 0) return;

        int count = 5 + (int)(intensity * 30);
        for (int i = 0; i < count; i++) {
            float angle = RANDOM.nextFloat() * (float)(2 * Math.PI);
            float radius = RANDOM.nextFloat() * intensity * 8f;
            float px = (float) (originX + Math.cos(angle) * radius);
            float pz = (float) (originZ + Math.sin(angle) * radius);
            float py = originY + RANDOM.nextFloat() * 6f * intensity;

            // Shift toward white/violet as intensity increases
            float[] color = PURPLE_PALETTE[RANDOM.nextInt(PURPLE_PALETTE.length)];
            float whiteMix = intensity * 0.5f;

            CustomParticle p = pm.spawn(px, py, pz);
            p.r = color[0] + (1f - color[0]) * whiteMix;
            p.g = color[1] + (1f - color[1]) * whiteMix;
            p.b = color[2] + (1f - color[2]) * whiteMix;
            p.a = 0.6f + intensity * 0.4f;
            p.scale = 0.1f + intensity * 0.5f;
            p.lifetime = 20 + RANDOM.nextInt(25);
            p.maxAge = p.lifetime;

            // Upward + outward
            p.velY = 0.15f + RANDOM.nextFloat() * 0.4f * intensity;
            p.velX = (RANDOM.nextFloat() - 0.5f) * 0.2f;
            p.velZ = (RANDOM.nextFloat() - 0.5f) * 0.2f;
            p.additiveBlend = true;
            p.fadeOutTicks = 12;
            p.fadeInTicks = 3 * (1f - intensity);
        }
    }

    // ================================================================
    // Ambient
    // ================================================================

    /**
     * Tiny sparkles around the area throughout the effect.
     */
    private void spawnAmbientSparkles(ParticleManager pm) {
        if (RANDOM.nextFloat() > 0.4f) return;

        float angle = RANDOM.nextFloat() * (float)(2 * Math.PI);
        float radius = 1f + RANDOM.nextFloat() * 6f;
        float px = (float) (originX + Math.cos(angle) * radius);
        float pz = (float) (originZ + Math.sin(angle) * radius);
        float py = originY + RANDOM.nextFloat() * 5f;

        float[] color = PURPLE_PALETTE[RANDOM.nextInt(PURPLE_PALETTE.length)];

        CustomParticle p = pm.spawn(px, py, pz);
        p.r = color[0]; p.g = color[1]; p.b = color[2];
        p.a = 0.5f;
        p.scale = 0.03f + RANDOM.nextFloat() * 0.07f;
        p.lifetime = 30 + RANDOM.nextInt(30);
        p.maxAge = p.lifetime;
        p.velY = 0.02f + RANDOM.nextFloat() * 0.04f;
        p.additiveBlend = true;
        p.fadeInTicks = 10;
        p.fadeOutTicks = 15;
    }

    // ================================================================
    // Helpers
    // ================================================================

    /**
     * Smooth ease-in-out for transitions.
     */
    private static float easeInOut(float t) {
        return t < 0.5f ? 2f * t * t : -1f + (4f - 2f * t) * t;
    }
}
