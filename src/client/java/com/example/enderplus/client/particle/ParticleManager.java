package com.example.enderplus.client.particle;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Random;
import java.util.function.Consumer;

/**
 * Manages the lifecycle of all custom particles.
 * Handles spawning, ticking, and automatic cleanup of dead particles.
 */
public class ParticleManager {

    private final List<CustomParticle> particles = new ArrayList<>(4096);
    private static final Random RANDOM = new Random();

    /**
     * Spawns a new particle at the given position.
     *
     * @return the spawned particle for further customization
     */
    public CustomParticle spawn(double x, double y, double z) {
        CustomParticle p = new CustomParticle(x, y, z);
        synchronized (particles) {
            particles.add(p);
        }
        return p;
    }

    /**
     * Spawns a particle with a builder-style configurator.
     */
    public CustomParticle spawn(double x, double y, double z, Consumer<CustomParticle> configurator) {
        CustomParticle p = spawn(x, y, z);
        configurator.accept(p);
        return p;
    }

    /**
     * Advances all particles, removing dead ones.
     */
    public void tick() {
        synchronized (particles) {
            Iterator<CustomParticle> it = particles.iterator();
            while (it.hasNext()) {
                CustomParticle p = it.next();
                p.tick();
                if (!p.isAlive()) {
                    it.remove();
                }
            }
        }
    }

    /**
     * Returns a snapshot of all active particles (thread-safe).
     */
    public List<CustomParticle> getParticles() {
        synchronized (particles) {
            return new ArrayList<>(particles);
        }
    }

    /**
     * Returns the current number of active particles.
     */
    public int getCount() {
        synchronized (particles) {
            return particles.size();
        }
    }

    /**
     * Clears all particles immediately.
     */
    public void clear() {
        synchronized (particles) {
            particles.clear();
        }
    }

    /**
     * Returns the shared Random instance for particle randomization.
     */
    public static Random random() {
        return RANDOM;
    }
}
