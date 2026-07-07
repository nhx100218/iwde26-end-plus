package com.example.enderplus.mixin;

import com.example.enderplus.EnderPlusMod;
import net.minecraft.world.level.dimension.end.EnderDragonBattle;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Mixin into EndDragonFight to detect when the Ender Dragon respawn ritual begins.
 * When the player places 4 end crystals on the exit portal, Minecraft calls
 * respawnDragon() to start the resurrection sequence.
 *
 * We hook into this to trigger our spectacular visual effects on the client side.
 */
@Mixin(EnderDragonBattle.class)
public class EnderDragonFightMixin {

    /**
     * Injects at the start of the respawnDragon method.
     * This is called when all 4 end crystals are placed on the exit portal.
     */
    @Inject(method = "respawnDragon", at = @At("HEAD"))
    private void onRespawnDragon(CallbackInfo ci) {
        EnderPlusMod.LOGGER.info("EnderPlus: Dragon respawn detected! Triggering visual effect.");

        // The End exit portal is always at (0, ~y, 0)
        // The exact Y level depends on the terrain, but the portal
        // is typically at Y=60-65. We use 0,0,0 as default and let
        // the effect be visible around the portal area.
        EnderPlusMod.respawnX = 0.0;
        EnderPlusMod.respawnY = 64.0;
        EnderPlusMod.respawnZ = 0.0;
        EnderPlusMod.dragonRespawnTriggered = true;
    }
}
