package com.example.enderplus.client.particle;

import com.mojang.blaze3d.vertex.*;
import net.minecraft.client.Camera;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import org.joml.Vector3fc;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Custom particle renderer that bypasses the vanilla particle system entirely.
 * Uses MC 26.1's new rendering pipeline (BufferBuilder → MeshData → RenderType.draw).
 *
 * Features:
 * - Supports up to MAX_PARTICLES (2000) active particles
 * - Billboard rendering (particles always face the camera)
 * - Alpha blending with soft circle texture
 * - Color per-vertex for vibrant purple effects
 * - Batch rendering via RenderTypes.entityTranslucent
 */
public class ParticleRenderer {
    public static final int MAX_PARTICLES = 2000;

    private final List<Particle> particles = new ArrayList<>();
    private final List<Particle> pendingAdditions = new ArrayList<>();

    /** Cached RenderType — created once on first use */
    private RenderType cachedRenderType;

    public void spawnParticle(Particle particle) {
        synchronized (pendingAdditions) {
            pendingAdditions.add(particle);
        }
    }

    public void spawnPurpleParticle(double x, double y, double z,
                                     double vx, double vy, double vz,
                                     float size, int maxLife) {
        float hue = 0.75f + (float) Math.random() * 0.1f;
        float saturation = 0.7f + (float) Math.random() * 0.3f;
        float brightness = 0.7f + (float) Math.random() * 0.3f;
        float[] rgb = hsvToRgb(hue, saturation, brightness);

        Particle p = new Particle(x, y, z, vx, vy, vz,
            rgb[0], rgb[1], rgb[2], 0.9f, size, maxLife);
        spawnParticle(p);
    }

    public void tick() {
        synchronized (pendingAdditions) {
            for (Particle p : pendingAdditions) {
                if (particles.size() >= MAX_PARTICLES) {
                    particles.removeFirst();
                }
                particles.add(p);
            }
            pendingAdditions.clear();
        }

        Iterator<Particle> it = particles.iterator();
        while (it.hasNext()) {
            Particle p = it.next();
            if (!p.tick()) {
                it.remove();
            }
        }
    }

    /**
     * Render all particles using MC 26.1's MeshData + RenderType.draw() pipeline.
     * Must be called during level rendering (e.g., from LevelRenderEvents callback).
     */
    public void render(PoseStack poseStack, Camera camera, float tickDelta) {
        if (particles.isEmpty() || !ParticleTexture.isInitialized()) {
            return;
        }

        // Lazy-init the render type with our particle texture
        if (cachedRenderType == null) {
            cachedRenderType = RenderTypes.entityTranslucent(ParticleTexture.getTextureId());
        }

        // Camera orientation vectors for billboarding
        Vector3fc forward = camera.forwardVector();
        Vector3fc up = camera.upVector();
        Vector3fc left = camera.leftVector();
        // right = -left (cross(forward, up) = -left)
        float rx = -left.x();
        float ry = -left.y();
        float rz = -left.z();
        float ux = up.x();
        float uy = up.y();
        float uz = up.z();

        double camX = camera.position().x;
        double camY = camera.position().y;
        double camZ = camera.position().z;

        // Build mesh data using the render type's vertex format
        VertexFormat format = cachedRenderType.format();
        BufferBuilder builder = Tesselator.getInstance().begin(VertexFormat.Mode.QUADS, format);

        for (Particle p : particles) {
            float lifeRatio = p.getLifeRatio();
            double renderX = p.x + p.vx * tickDelta - camX;
            double renderY = p.y + p.vy * tickDelta - camY;
            double renderZ = p.z + p.vz * tickDelta - camZ;

            float halfSize = p.size * 0.5f;

            // Billboard quad corner offsets
            float crx = rx * halfSize, cry = ry * halfSize, crz = rz * halfSize;
            float cux = ux * halfSize, cuy = uy * halfSize, cuz = uz * halfSize;

            // Apply rotation if set
            if (p.rotation != 0.0f) {
                float cos = (float) Math.cos(p.rotation);
                float sin = (float) Math.sin(p.rotation);
                float rotRx = crx * cos - cux * sin;
                float rotRy = cry * cos - cuy * sin;
                float rotRz = crz * cos - cuz * sin;
                cux = crx * sin + cux * cos;
                cuy = cry * sin + cuy * cos;
                cuz = crz * sin + cuz * cos;
                crx = rotRx; cry = rotRy; crz = rotRz;
            }

            // Four quad corners: (-r-u), (+r-u), (+r+u), (-r+u)
            int ir = (int) (p.r * 255.0f);
            int ig = (int) (p.g * 255.0f);
            int ib = (int) (p.b * 255.0f);
            int ia = (int) (p.alpha * lifeRatio * 255.0f);
            if (ia < 0) ia = 0; if (ia > 255) ia = 255;

            builder.addVertex((float)(renderX - crx - cux), (float)(renderY - cry - cuy), (float)(renderZ - crz - cuz))
                .setColor(ir, ig, ib, ia).setUv(0, 0);
            builder.addVertex((float)(renderX + crx - cux), (float)(renderY + cry - cuy), (float)(renderZ + crz - cuz))
                .setColor(ir, ig, ib, ia).setUv(1, 0);
            builder.addVertex((float)(renderX + crx + cux), (float)(renderY + cry + cuy), (float)(renderZ + crz + cuz))
                .setColor(ir, ig, ib, ia).setUv(1, 1);
            builder.addVertex((float)(renderX - crx + cux), (float)(renderY - cry + cuy), (float)(renderZ - crz + cuz))
                .setColor(ir, ig, ib, ia).setUv(0, 1);
        }

        // Build and draw the mesh
        MeshData mesh = builder.build();
        cachedRenderType.draw(mesh);
        mesh.close(); // free GPU resources
    }

    public void clearAll() {
        particles.clear();
        synchronized (pendingAdditions) {
            pendingAdditions.clear();
        }
    }

    public int getActiveCount() {
        return particles.size();
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
}
