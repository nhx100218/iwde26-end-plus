package com.example.enderplus.client.effect;

import com.example.enderplus.client.particle.Particle;
import com.example.enderplus.client.particle.ParticleRenderer;
import com.mojang.blaze3d.vertex.*;
import net.minecraft.client.Camera;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.core.BlockPos;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Manages the dragon resurrection visual effect.
 *
 * Visual elements:
 * 1. Runic Burst — 30 runes spiraling outward, rotating and fading
 * 2. Purple Light Pillar — vertical beam 70 blocks tall, pulsing brightness
 * 3. Particle Explosion — 400+ purple particles rising around the pillar
 *
 * Each effect instance runs for ~7 seconds (140 ticks).
 */
public class DragonResurrectionEffect {

    private static final int EFFECT_DURATION = 140;
    private static final int PILLAR_HEIGHT = 70;
    private static final float PILLAR_WIDTH = 2.5f;
    private static final int PILLAR_SEGMENTS = 12;
    private static final int RUNE_COUNT = 30;

    private final List<ActiveEffect> activeEffects = new ArrayList<>();

    /** Cached RenderType for the light pillar (additive glow effect) */
    private RenderType cachedPillarRenderType;

    private static class ActiveEffect {
        final BlockPos pos;
        int age;
        boolean runesSpawned;

        ActiveEffect(BlockPos pos) {
            this.pos = pos;
            this.age = 0;
            this.runesSpawned = false;
        }

        boolean isExpired() {
            return age >= EFFECT_DURATION;
        }
    }

    public void trigger(BlockPos pos) {
        activeEffects.add(new ActiveEffect(pos));
    }

    public void tick(ParticleRenderer particleRenderer) {
        Iterator<ActiveEffect> it = activeEffects.iterator();
        while (it.hasNext()) {
            ActiveEffect effect = it.next();
            effect.age++;

            if (effect.isExpired()) {
                it.remove();
                continue;
            }

            double cx = effect.pos.getX() + 0.5;
            double cy = effect.pos.getY() + 1.0;
            double cz = effect.pos.getZ() + 0.5;
            float progress = (float) effect.age / EFFECT_DURATION;

            if (!effect.runesSpawned) {
                effect.runesSpawned = true;
                spawnRunicBurst(particleRenderer, cx, cy, cz);
            }

            int particlesPerTick = (int) (15 * (1.0f - progress * 0.7f));
            for (int i = 0; i < particlesPerTick; i++) {
                spawnPillarParticle(particleRenderer, cx, cy, cz);
            }
        }
    }

    /**
     * Render light pillars. Uses RenderTypes.beaconBeam-style additive blending.
     */
    public void render(Camera camera, float tickDelta) {
        if (activeEffects.isEmpty()) return;

        // Lazy-init the pillar render type with a solid white texture
        if (cachedPillarRenderType == null) {
            // Use entityTranslucent for the pillar with soft circle texture
            cachedPillarRenderType = RenderTypes.entityTranslucent(
                com.example.enderplus.client.particle.ParticleTexture.getTextureId());
        }

        VertexFormat format = cachedPillarRenderType.format();
        BufferBuilder builder = Tesselator.getInstance().begin(VertexFormat.Mode.QUADS, format);

        double camX = camera.position().x;
        double camY = camera.position().y;
        double camZ = camera.position().z;

        // Purple base color: #9B59B6 → (0.608, 0.349, 0.714)
        float baseR = 0.608f;
        float baseG = 0.349f;
        float baseB = 0.714f;

        for (ActiveEffect effect : activeEffects) {
            float progress = (float) effect.age / EFFECT_DURATION;
            if (progress < 0.05f) progress = 0.05f;

            double cx = effect.pos.getX() + 0.5 - camX;
            double cy = effect.pos.getY() - camY;
            double cz = effect.pos.getZ() + 0.5 - camZ;

            // Pulsing alpha
            float pulse = 0.5f + 0.5f * (float) Math.sin(effect.age * 0.15);
            float fadeAlpha = 1.0f;
            if (progress < 0.15f) {
                fadeAlpha = progress / 0.15f;
            } else if (progress > 0.7f) {
                fadeAlpha = (1.0f - progress) / 0.3f;
            }
            float alpha = 0.35f * pulse * fadeAlpha;

            renderPillarCylinder(builder, cx, cy, cz, PILLAR_WIDTH, PILLAR_HEIGHT,
                PILLAR_SEGMENTS, alpha, baseR, baseG, baseB);
        }

        MeshData mesh = builder.build();
        cachedPillarRenderType.draw(mesh);
        mesh.close();
    }

    private void renderPillarCylinder(BufferBuilder builder, double cx, double cy, double cz,
                                       float radius, int height, int segments,
                                       float alpha, float br, float bg, float bb) {
        int ia = (int) (alpha * 255.0f);
        if (ia < 0) ia = 0; if (ia > 255) ia = 255;

        for (int i = 0; i < segments; i++) {
            double a1 = (2.0 * Math.PI * i) / segments;
            double a2 = (2.0 * Math.PI * (i + 1)) / segments;

            float c1 = (float) Math.cos(a1) * radius;
            float s1 = (float) Math.sin(a1) * radius;
            float c2 = (float) Math.cos(a2) * radius;
            float s2 = (float) Math.sin(a2) * radius;

            // Bottom: bright purple
            int irB = (int) (br * 1.3f * 255); int igB = (int) (bg * 1.3f * 255); int ibB = (int) (bb * 1.3f * 255);
            if (irB > 255) irB = 255; if (igB > 255) igB = 255; if (ibB > 255) ibB = 255;

            // Top: dark/fading
            int irT = (int) (br * 0.2f * 255); int igT = (int) (bg * 0.1f * 255); int ibT = (int) (bb * 0.3f * 255);
            int iaT = (int) (ia * 0.05f);

            builder.addVertex((float)(cx + c1), (float)cy, (float)(cz + s1))
                .setColor(irB, igB, ibB, ia).setUv(0, 0);
            builder.addVertex((float)(cx + c2), (float)cy, (float)(cz + s2))
                .setColor(irB, igB, ibB, ia).setUv(1, 0);
            builder.addVertex((float)(cx + c2), (float)(cy + height), (float)(cz + s2))
                .setColor(irT, igT, ibT, iaT).setUv(1, 1);
            builder.addVertex((float)(cx + c1), (float)(cy + height), (float)(cz + s1))
                .setColor(irT, igT, ibT, iaT).setUv(0, 1);
        }
    }

    private void spawnRunicBurst(ParticleRenderer renderer, double x, double y, double z) {
        for (int i = 0; i < RUNE_COUNT; i++) {
            double angle = (2.0 * Math.PI * i) / RUNE_COUNT + (Math.random() - 0.5) * 0.5;
            double speed = 0.15 + Math.random() * 0.35;
            double vx = Math.cos(angle) * speed;
            double vz = Math.sin(angle) * speed;
            double vy = 0.05 + Math.random() * 0.25;

            float hue = 0.72f + (float) Math.random() * 0.1f;
            float[] rgb = rgbFromHue(hue);

            double ox = x + (Math.random() - 0.5) * 0.5;
            double oy = y + Math.random() * 2.0;
            double oz = z + (Math.random() - 0.5) * 0.5;

            Particle rune = new Particle(ox, oy, oz, vx, vy, vz,
                rgb[0], rgb[1], rgb[2], 0.9f,
                0.4f + (float) Math.random() * 0.6f,
                30 + (int) (Math.random() * 40));
            rune.rotation = (float) (Math.random() * Math.PI * 2);
            rune.rotationSpeed = (float) ((Math.random() - 0.5) * 0.3);
            rune.hasGravity = false;
            renderer.spawnParticle(rune);
        }
    }

    private void spawnPillarParticle(ParticleRenderer renderer, double cx, double cy, double cz) {
        double angle = Math.random() * 2.0 * Math.PI;
        double radius = PILLAR_WIDTH * (0.3 + Math.random() * 0.7);
        double px = cx + Math.cos(angle) * radius;
        double pz = cz + Math.sin(angle) * radius;
        double py = cy + Math.random() * 0.5;

        double ts = 0.03 + Math.random() * 0.06;
        double vx = -Math.sin(angle) * ts;
        double vz = Math.cos(angle) * ts;
        double vy = 0.15 + Math.random() * 0.35;

        float hue = 0.7f + (float) Math.random() * 0.12f;
        float[] rgb = rgbFromHue(hue);

        Particle p = new Particle(px, py, pz, vx, vy, vz,
            rgb[0], rgb[1], rgb[2], 0.8f,
            0.1f + (float) Math.random() * 0.3f,
            20 + (int) (Math.random() * 60));
        p.hasGravity = false;
        renderer.spawnParticle(p);
    }

    private static float[] rgbFromHue(float hue) {
        float sat = 0.6f + (float) Math.random() * 0.4f;
        float bri = 0.7f + (float) Math.random() * 0.3f;
        return hsvToRgb(hue, sat, bri);
    }

    private static float[] hsvToRgb(float h, float s, float v) {
        float[] rgb = new float[3];
        int i = (int) (h * 6.0f);
        float f = h * 6.0f - i;
        float p = v * (1.0f - s);
        float q = v * (1.0f - f * s);
        float t = v * (1.0f - (1.0f - f) * s);
        switch (i % 6) {
            case 0 -> { rgb[0] = v; rgb[1] = t; rgb[2] = p; }
            case 1 -> { rgb[0] = q; rgb[1] = v; rgb[2] = p; }
            case 2 -> { rgb[0] = p; rgb[1] = v; rgb[2] = t; }
            case 3 -> { rgb[0] = p; rgb[1] = q; rgb[2] = v; }
            case 4 -> { rgb[0] = t; rgb[1] = p; rgb[2] = v; }
            case 5 -> { rgb[0] = v; rgb[1] = p; rgb[2] = q; }
        }
        return rgb;
    }

    public boolean hasActiveEffects() {
        return !activeEffects.isEmpty();
    }
}
