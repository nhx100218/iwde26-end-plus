package com.example.enderplus.client.particle;

/**
 * Lightweight particle data class for the custom OpenGL particle system.
 * Bypasses Minecraft's vanilla Particle class to avoid engine limitations
 * like the 16,384 particle cap and restricted rendering modes.
 */
public class CustomParticle {

    // --- Position ---
    public double x, y, z;
    public double prevX, prevY, prevZ;

    // --- Velocity ---
    public double velX, velY, velZ;

    // --- Color (0.0 - 1.0) ---
    public float r, g, b, a;

    // --- Size ---
    public float scale;
    public float prevScale;

    // --- Lifetime (in ticks, 20 = 1 second) ---
    public int lifetime;
    public int age;
    public int maxAge;

    // --- Physics ---
    public float gravity = 0.0f;
    public float airFriction = 0.98f;
    public float fadeInTicks = 0;   // ticks to fade in at start
    public float fadeOutTicks = 10; // ticks to fade out at end

    // --- Rendering ---
    public boolean additiveBlend = true; // glow effect via additive blending
    public boolean noDepthTest = true;   // render through walls
    public ParticleShape shape = ParticleShape.QUAD;

    // --- Rotation (for rune-like effects) ---
    public float rotation = 0f;
    public float rotationSpeed = 0f;

    public CustomParticle(double x, double y, double z) {
        this.x = x;
        this.y = y;
        this.z = z;
        this.prevX = x;
        this.prevY = y;
        this.prevZ = z;
        this.r = 1.0f;
        this.g = 1.0f;
        this.b = 1.0f;
        this.a = 1.0f;
        this.scale = 0.2f;
        this.prevScale = 0.2f;
        this.lifetime = 40;
        this.maxAge = 40;
    }

    /**
     * Advances the particle by one tick.
     */
    public void tick() {
        if (age >= maxAge) return;

        prevX = x;
        prevY = y;
        prevZ = z;
        prevScale = scale;
        rotation += rotationSpeed;

        x += velX;
        y += velY;
        z += velZ;

        velX *= airFriction;
        velY *= airFriction;
        velZ *= airFriction;
        velY -= gravity;

        age++;
    }

    public boolean isAlive() {
        return age < maxAge;
    }

    /**
     * Returns the interpolated alpha based on fade-in / fade-out.
     */
    public float getAlpha() {
        float baseAlpha = a;

        // Fade in
        if (age < fadeInTicks && fadeInTicks > 0) {
            baseAlpha *= (float) age / fadeInTicks;
        }

        // Fade out
        int remainingLife = maxAge - age;
        if (remainingLife < fadeOutTicks && fadeOutTicks > 0) {
            baseAlpha *= (float) remainingLife / fadeOutTicks;
        }

        return Math.clamp(baseAlpha, 0f, 1f);
    }

    /**
     * Returns life progress from 0.0 (spawn) to 1.0 (death).
     */
    public float getLifeProgress() {
        return maxAge > 0 ? (float) age / maxAge : 0f;
    }

    public float getRenderX(float tickDelta) {
        return (float) (prevX + (x - prevX) * tickDelta);
    }

    public float getRenderY(float tickDelta) {
        return (float) (prevY + (y - prevY) * tickDelta);
    }

    public float getRenderZ(float tickDelta) {
        return (float) (prevZ + (z - prevZ) * tickDelta);
    }

    public float getRenderScale(float tickDelta) {
        return prevScale + (scale - prevScale) * tickDelta;
    }

    // --- Particle Shapes ---
    public enum ParticleShape {
        QUAD,    // camera-facing square
        LINE     // 1-pixel GL point
    }
}
