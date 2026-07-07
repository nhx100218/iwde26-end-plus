package com.example.enderplus.client.mixin;

import com.example.enderplus.client.EnderPlusModClient;
import net.minecraft.client.renderer.LevelRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Backup render hook mixin into LevelRenderer.
 * The primary rendering is done through Fabric API's WorldRenderEvents.AFTER_ENTITIES,
 * but this mixin serves as a fallback and ensures our particles are always cleaned up
 * when the level is unloaded.
 */
@Mixin(LevelRenderer.class)
public class LevelRendererMixin {

    /**
     * Clean up all particles when the level renderer is closed (world unload / disconnect).
     */
    @Inject(method = "close", at = @At("HEAD"))
    private void onClose(CallbackInfo ci) {
        EnderPlusModClient.PARTICLE_MANAGER.clear();
    }
}
