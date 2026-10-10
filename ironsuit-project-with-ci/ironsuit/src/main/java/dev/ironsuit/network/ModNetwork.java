package dev.ironsuit.network;

import dev.ironsuit.IronSuit;
import dev.ironsuit.SuitCombat;
import dev.ironsuit.SuitManager;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

@EventBusSubscriber(modid = IronSuit.MODID)
public final class ModNetwork {
    private ModNetwork() {}

    @SubscribeEvent // mod event bus
    public static void register(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar("1");
        registrar.playToServer(ToggleSuitPayload.TYPE, ToggleSuitPayload.STREAM_CODEC, (payload, context) -> {
            if (context.player() instanceof ServerPlayer player) {
                SuitManager.toggle(player);
            }
        });
        registrar.playToServer(ToggleHelmetPayload.TYPE, ToggleHelmetPayload.STREAM_CODEC, (payload, context) -> {
            if (context.player() instanceof ServerPlayer player) {
                SuitManager.toggleHelmet(player);
            }
        });
        registrar.playToServer(WeaponInputPayload.TYPE, WeaponInputPayload.STREAM_CODEC, (payload, context) -> {
            if (context.player() instanceof ServerPlayer player) {
                SuitCombat.setInput(player, payload.weapon(), payload.down());
            }
        });
    }
}
