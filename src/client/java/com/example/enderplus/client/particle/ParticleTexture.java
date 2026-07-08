package com.example.enderplus.client.particle;

import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.Identifier;
import com.example.enderplus.EnderPlusMod;

/**
 * Generates and manages particle textures.
 * Creates a soft white circle gradient texture used as the base for all custom particles.
 * The color is applied via vertex coloring, so a single white texture works for all hues.
 */
public class ParticleTexture {
    private static final int TEXTURE_SIZE = 64;
    private static Identifier textureId;
    private static boolean initialized = false;

    /**
     * Generate a soft radial gradient circle texture and register it.
     * Must be called on the render thread after Minecraft has initialized.
     */
    public static void initialize() {
        if (initialized) return;

        NativeImage image = new NativeImage(TEXTURE_SIZE, TEXTURE_SIZE, false);
        float center = TEXTURE_SIZE / 2.0f;
        float radius = center - 2; // small margin so the circle doesn't clip

        for (int y = 0; y < TEXTURE_SIZE; y++) {
            for (int x = 0; x < TEXTURE_SIZE; x++) {
                float dx = x - center;
                float dy = y - center;
                float dist = (float) Math.sqrt(dx * dx + dy * dy);

                // Soft falloff: 1.0 at center, 0.0 at radius and beyond
                float alpha;
                if (dist >= radius) {
                    alpha = 0.0f;
                } else {
                    // Smoothstep falloff for a soft glow
                    float t = dist / radius;
                    alpha = 1.0f - t * t * (3.0f - 2.0f * t); // smoothstep
                    alpha = alpha * alpha; // extra softening
                }

                int a = (int) (alpha * 255.0f);
                // White texture with alpha channel for soft particles
                int color = (a << 24) | (255 << 16) | (255 << 8) | 255;
                image.setPixelABGR(x, y, color);
            }
        }

        // Register as a dynamic texture (MC 26.1 requires Supplier<String> label)
        var texId = EnderPlusMod.id("particle/soft_circle");
        DynamicTexture dynamicTexture = new DynamicTexture(
            () -> texId.toString(),
            image
        );
        textureId = texId;
        Minecraft.getInstance().getTextureManager().register(textureId, dynamicTexture);

        initialized = true;
        EnderPlusMod.LOGGER.info("ParticleTexture initialized: {}", textureId);
    }

    public static Identifier getTextureId() {
        return textureId;
    }

    public static boolean isInitialized() {
        return initialized;
    }
}
