package dev.ironsuit.network;

import dev.ironsuit.IronSuit;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/** Client -> server: the player pressed the "deploy / retract suit" key. */
public record ToggleSuitPayload() implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<ToggleSuitPayload> TYPE =
            new CustomPacketPayload.Type<>(Identifier.fromNamespaceAndPath(IronSuit.MODID, "toggle_suit"));

    public static final StreamCodec<ByteBuf, ToggleSuitPayload> STREAM_CODEC =
            StreamCodec.unit(new ToggleSuitPayload());

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
