package dev.ironsuit.network;

import dev.ironsuit.IronSuit;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/**
 * Client -> server: a weapon key was pressed or released.
 *
 * @param weapon 0 = repulsors (hold to fire), 1 = chest reactor beam (hold to charge, release to fire)
 * @param down   true when the key went down, false when it was released
 */
public record WeaponInputPayload(int weapon, boolean down) implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<WeaponInputPayload> TYPE =
            new CustomPacketPayload.Type<>(Identifier.fromNamespaceAndPath(IronSuit.MODID, "weapon_input"));

    public static final StreamCodec<ByteBuf, WeaponInputPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, WeaponInputPayload::weapon,
            ByteBufCodecs.BOOL, WeaponInputPayload::down,
            WeaponInputPayload::new);

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
