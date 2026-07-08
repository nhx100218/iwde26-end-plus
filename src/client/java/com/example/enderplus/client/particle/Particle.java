package com.example.enderplus.client.particle;

/**
 * Lightweight particle data class.
 * Each particle has position, velocity, color, size, and lifetime.
 * Designed for efficient batch rendering via the custom ParticleRenderer.
 */
public class Particle {
    // Position (world coordinates)
    public double x, y, z;

    // Velocity (blocks per tick)
    public double vx, vy, vz;

    // Color (RGB, 0.0 - 1.0)
    public float r, g, b;

    // Alpha (0.0 - 1.0), modified by lifetime gradient
    public float alpha;

    // Size in blocks (rendered as a billboarded quad)
    public float size;

    // Lifetime (ticks)
    public int maxLife;
    public int life;

    // Optional: rotation for rune-like particles (radians)
    public float rotation;
    public float rotationSpeed;

    // Whether this particle uses gravity
    public boolean hasGravity;

    public Particle(double x, double y, double z,
                    double vx, double vy, double vz,
                    float r, float g, float b, float alpha,
                    float size, int maxLife) {
        this.x = x;
        this.y = y;
        this.z = z;
        this.vx = vx;
        this.vy = vy;
        this.vz = vz;
        this.r = r;
        this.g = g;
        this.b = b;
        this.alpha = alpha;
        this.size = size;
        this.maxLife = maxLife;
        this.life = maxLife;
        this.rotation = 0.0f;
        this.rotationSpeed = 0.0f;
        this.hasGravity = false;
    }

    /**
     * Update particle state for one tick.
     * @return true if the particle is still alive
     */
    public boolean tick() {
        life--;
        if (life <= 0) {
            return false;
        }

        // Update position
        x += vx;
        y += vy;
        z += vz;

        // Apply gravity if enabled
        if (hasGravity) {
            vy -= 0.04; // standard Minecraft gravity per tick
        }

        // Apply drag / damping for more natural motion
        vx *= 0.98;
        vy *= 0.98;
        vz *= 0.98;

        // Update rotation
        rotation += rotationSpeed;

        // Fade out: alpha decreases over lifetime
        float lifeRatio = (float) life / (float) maxLife;
        // Use a smoothstep-like curve for nice fade
        alpha = lifeRatio * lifeRatio;

        return true;
    }

    public boolean isAlive() {
        return life > 0;
    }

    public float getLifeRatio() {
        return (float) life / (float) maxLife;
    }
}
