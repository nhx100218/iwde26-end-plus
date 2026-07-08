package com.example.enderplus.mixin;

import com.example.enderplus.EnderPlusMod;
import com.example.enderplus.network.DragonRespawnPayload;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.dimension.end.DragonRespawnStage;
import net.minecraft.world.level.dimension.end.EnderDragonFight;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Mixin into EnderDragonFight to detect when the dragon resurrection ritual reaches
 * the SUMMONING_DRAGON stage. This is triggered when:
 * - A player places end crystals on the exit portal
 * - The resurrection sequence completes all preparation stages
 * - The dragon is about to be spawned
 *
 * We inject into setRespawnStage() and broadcast a packet to all players in the
 * End dimension when the stage transitions to SUMMONING_DRAGON.
 */
@Mixin(EnderDragonFight.class)
public class EndDragonFightMixin {

    @Shadow
    @Final
    private ServerLevel level;

    @Shadow
    @Final
    private BlockPos origin;

    /**
     * Inject at the HEAD of setRespawnStage — fires when the fight transitions
     * to a new stage. We check if it's SUMMONING_DRAGON (the dragon is being summoned).
     */
    @Inject(method = "setRespawnStage", at = @At("HEAD"))
    private void onSetRespawnStage(DragonRespawnStage stage, CallbackInfo ci) {
        // Only trigger effects when the dragon is actually being summoned
        if (stage != DragonRespawnStage.SUMMONING_DRAGON) {
            return;
        }

        if (this.level == null || this.origin == null) {
            EnderPlusMod.LOGGER.warn("EnderDragonFight level or origin is null, skipping effect");
            return;
        }

        EnderPlusMod.LOGGER.info("Dragon resurrection stage: SUMMONING_DRAGON at {}", this.origin);

        // Build the payload with the portal center position
        DragonRespawnPayload payload = new DragonRespawnPayload(this.origin);

        // Send to all players currently in the End dimension
        for (ServerPlayer player : this.level.players()) {
            ServerPlayNetworking.send(player, payload);
        }
    }
}
