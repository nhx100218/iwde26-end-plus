package com.example.enderplus.client.particle;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;
import net.minecraft.client.Camera;
import net.minecraft.client.renderer.CoreShaders;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;

import java.util.List;

/**
 * Custom OpenGL particle renderer that bypasses Minecraft's vanilla particle
 * engine limitations.
 *
 * Uses direct BufferBuilder rendering with custom blend modes and billboarding.
 * The renderer supports:
 * - Unlimited particle count (only bounded by heap memory, vs vanilla's 16,384 cap)
 * - Additive blending for glow / energy effects
 * - Per-particle rotation (for spinning runes)
 * - Camera-facing billboard sprites computed from camera basis vectors
 * - Configurable per-particle colors, alpha, and scale
 *
 * Rendered in two passes: additive (glow) first, then standard alpha blending.
 */
public class CustomParticleRenderer {

    /**
     * Renders all particles from the given manager into the world.
     * Called each frame from Fabric API's AFTER_ENTITIES render event.
     */
    public void render(ParticleManager manager, WorldRenderContext context) {
        List<CustomParticle> particles = manager.getParticles();
        if (particles.isEmpty()) return;

        Camera camera = context.camera();
        Vec3 cameraPos = camera.getPosition();
        float tickDelta = context.tickCounter().getGameTimeDeltaPartialTick(false);

        // Compute camera right/up vectors for billboarding
        CameraBasis basis = computeCameraBasis(camera);

        // Current model-view-projection matrix from the render pipeline
        PoseStack poseStack = context.matrixStack();
        Matrix4f mvpMatrix = poseStack.last().pose();

        Tessellator tessellator = Tessellator.getInstance();

        // === PASS 1: Additive-blend particles (glow, energy, runes) ===
        renderPass(particles, tessellator, mvpMatrix, basis, tickDelta, true);

        // === PASS 2: Standard alpha-blend particles (smoke, debris) ===
        renderPass(particles, tessellator, mvpMatrix, basis, tickDelta, false);
    }

    /**
     * Renders all particles matching the given blend mode in a single draw call.
     */
    private void renderPass(
        List<CustomParticle> particles,
        Tessellator tessellator,
        Matrix4f mvpMatrix,
        CameraBasis basis,
        float tickDelta,
        boolean additive
    ) {
        // Begin building geometry (MC 1.21.4+ API: tessellator.begin() returns BufferBuilder)
        BufferBuilder builder = tessellator.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);
        int rendered = 0;

        for (CustomParticle p : particles) {
            if (p.additiveBlend != additive) continue;

            float alpha = p.getAlpha();
            if (alpha <= 0.001f) continue;

            // World-space particle position (interpolated for smooth movement)
            float px = p.getRenderX(tickDelta);
            float py = p.getRenderY(tickDelta);
            float pz = p.getRenderZ(tickDelta);
            float halfSize = p.getRenderScale(tickDelta) * 0.5f;

            // Apply per-particle rotation for spinning rune effects
            float cosR = (float) Math.cos(p.rotation);
            float sinR = (float) Math.sin(p.rotation);

            // Rotated right vector
            float rrx = basis.rx * cosR - basis.ux * sinR;
            float rry = basis.ry * cosR - basis.uy * sinR;
            float rrz = basis.rz * cosR - basis.uz * sinR;

            // Rotated up vector
            float urx = basis.rx * sinR + basis.ux * cosR;
            float ury = basis.ry * sinR + basis.uy * cosR;
            float urz = basis.rz * sinR + basis.uz * cosR;

            // Four corners of the billboard quad in WORLD SPACE
            // Bottom-left, Bottom-right, Top-right, Top-left (CCW order from camera view)
            float blx = px - rrx * halfSize - urx * halfSize;
            float bly = py - rry * halfSize - ury * halfSize;
            float blz = pz - rrz * halfSize - urz * halfSize;

            float brx = px + rrx * halfSize - urx * halfSize;
            float bry = py + rry * halfSize - ury * halfSize;
            float brz = pz + rrz * halfSize - urz * halfSize;

            float trx = px + rrx * halfSize + urx * halfSize;
            float t_ry = py + rry * halfSize + ury * halfSize;
            float trz = pz + rrz * halfSize + urz * halfSize;

            float tlx = px - rrx * halfSize + urx * halfSize;
            float tly = py - rry * halfSize + ury * halfSize;
            float tlz = pz - rrz * halfSize + urz * halfSize;

            // Pack color as ARGB int (MC 1.21.4+ setColor uses packed int)
            int color = packARGB(p.r, p.g, p.b, alpha);

            // Add vertices transformed by the MVP matrix
            builder.addVertex(mvpMatrix, blx, bly, blz).setColor(color);
            builder.addVertex(mvpMatrix, brx, bry, brz).setColor(color);
            builder.addVertex(mvpMatrix, trx, t_ry, trz).setColor(color);
            builder.addVertex(mvpMatrix, tlx, tly, tlz).setColor(color);

            rendered++;
        }

        if (rendered == 0) return;

        // Configure render state for this pass
        RenderSystem.enableBlend();
        RenderSystem.depthMask(false);
        RenderSystem.setShader(CoreShaders.POSITION_COLOR);

        if (additive) {
            // Additive blending: src * alpha + dst * 1.0 — creates bright glow
            RenderSystem.blendFunc(
                GlStateManager.SourceFactor.SRC_ALPHA,
                GlStateManager.DestFactor.ONE
            );
        } else {
            // Standard alpha blending
            RenderSystem.blendFunc(
                GlStateManager.SourceFactor.SRC_ALPHA,
                GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA
            );
        }

        // Upload and draw the mesh
        MeshData meshData = builder.buildOrThrow();
        BufferUploader.drawWithShader(meshData);

        // Restore render state
        RenderSystem.depthMask(true);
        RenderSystem.disableBlend();
    }

    // ================================================================
    // Camera basis for billboarding
    // ================================================================

    /**
     * Computes the camera's right and up vectors from its yaw/pitch rotation.
     * These define the orientation plane that particles face.
     */
    private CameraBasis computeCameraBasis(Camera camera) {
        float yaw = (float) Math.toRadians(camera.getYRot());
        float pitch = (float) Math.toRadians(camera.getXRot());

        float cosPitch = (float) Math.cos(pitch);

        // Forward (look) direction
        float fx = (float) (-Math.sin(yaw) * cosPitch);
        float fy = (float) (-Math.sin(pitch));
        float fz = (float) (Math.cos(yaw) * cosPitch);

        // Right = forward x worldUp(0,1,0)
        float rx = -fz;
        float ry = 0f;
        float rz = fx;

        float rLen = Math.sqrt(rx * rx + ry * ry + rz * rz);
        if (rLen > 0.0001f) {
            rx /= rLen; ry /= rLen; rz /= rLen;
        } else {
            rx = 1f; rz = 0f;
        }

        // Up = right x forward
        float ux = ry * fz - rz * fy;
        float uy = rz * fx - rx * fz;
        float uz = rx * fy - ry * fx;

        float uLen = Math.sqrt(ux * ux + uy * uy + uz * uz);
        if (uLen > 0.0001f) {
            ux /= uLen; uy /= uLen; uz /= uLen;
        } else {
            uy = 1f; ux = 0f; uz = 0f;
        }

        return new CameraBasis(rx, ry, rz, ux, uy, uz);
    }

    // ================================================================
    // Helpers
    // ================================================================

    /**
     * Packs float RGBA (0.0-1.0) into an ARGB int for setColor().
     */
    private static int packARGB(float r, float g, float b, float a) {
        int ir = Math.clamp((int) (r * 255f), 0, 255);
        int ig = Math.clamp((int) (g * 255f), 0, 255);
        int ib = Math.clamp((int) (b * 255f), 0, 255);
        int ia = Math.clamp((int) (a * 255f), 0, 255);
        return (ia << 24) | (ir << 16) | (ig << 8) | ib;
    }

    /**
     * Camera right and up vectors for billboard orientation.
     */
    private record CameraBasis(
        float rx, float ry, float rz,
        float ux, float uy, float uz
    ) {}
}
