package com.example.enderplus.network;

import com.example.enderplus.EnderPlusMod;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/**
 * Custom packet sent from server to client when the ender dragon resurrection begins.
 * Carries the BlockPos of the end portal / dragon fight center for effect positioning.
 */
public record DragonRespawnPayload(BlockPos pos) implements CustomPacketPayload {

    public static final Type<DragonRespawnPayload> TYPE =
        new Type<>(EnderPlusMod.id("dragon_respawn"));

    public static final StreamCodec<RegistryFriendlyByteBuf, DragonRespawnPayload> STREAM_CODEC =
        StreamCodec.composite(
            BlockPos.STREAM_CODEC,
            DragonRespawnPayload::pos,
            DragonRespawnPayload::new
        );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
