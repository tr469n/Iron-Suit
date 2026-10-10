package dev.ironsuit.network;

import dev.ironsuit.IronSuit;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/** Client -> server: the player pressed the "helmet on / off" key. */
public record ToggleHelmetPayload() implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<ToggleHelmetPayload> TYPE =
            new CustomPacketPayload.Type<>(Identifier.fromNamespaceAndPath(IronSuit.MODID, "toggle_helmet"));

    public static final StreamCodec<ByteBuf, ToggleHelmetPayload> STREAM_CODEC =
            StreamCodec.unit(new ToggleHelmetPayload());

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
