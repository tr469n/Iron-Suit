package dev.ironsuit;

import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingFallEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

@EventBusSubscriber(modid = IronSuit.MODID)
public final class CommonEvents {
    private CommonEvents() {}

    @SubscribeEvent // game event bus
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            SuitManager.tick(player);
            SuitFlight.tick(player);
            SuitCombat.tick(player);
        }
    }

    /** A powered suit absorbs the landing, so flying until the energy runs out is not lethal. */
    @SubscribeEvent
    public static void onFall(LivingFallEvent event) {
        if (event.getEntity() instanceof ServerPlayer player && SuitFlight.isProtected(player)) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        SuitManager.cancel(event.getEntity().getUUID());
        SuitCombat.cancel(event.getEntity().getUUID());
        if (event.getEntity() instanceof ServerPlayer player) {
            SuitFlight.revoke(player); // otherwise "may fly" would be saved with the player
        }
    }
}
