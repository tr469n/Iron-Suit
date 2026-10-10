package dev.ironsuit.client;

import com.mojang.blaze3d.platform.InputConstants;
import dev.ironsuit.IronSuit;
import dev.ironsuit.network.ToggleSuitPayload;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.Identifier;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;
import net.neoforged.neoforge.common.util.Lazy;

@EventBusSubscriber(modid = IronSuit.MODID, value = Dist.CLIENT)
public final class IronSuitClient {
    public static final KeyMapping.Category CATEGORY =
            new KeyMapping.Category(Identifier.fromNamespaceAndPath(IronSuit.MODID, "main"));

    /** Default key: R. Rebindable in Controls. */
    public static final Lazy<KeyMapping> TOGGLE_SUIT =
            Lazy.of(() -> new KeyMapping("key.ironsuit.toggle_suit", InputConstants.KEY_R, CATEGORY));

    private IronSuitClient() {}

    @SubscribeEvent // mod event bus
    public static void registerKeys(RegisterKeyMappingsEvent event) {
        event.registerCategory(CATEGORY);
        event.register(TOGGLE_SUIT.get());
    }

    @SubscribeEvent // mod event bus
    public static void registerHud(RegisterGuiLayersEvent event) {
        event.registerAboveAll(Identifier.fromNamespaceAndPath(IronSuit.MODID, "suit_hud"), SuitHud::render);
    }

    @SubscribeEvent // game event bus
    public static void onClientTick(ClientTickEvent.Post event) {
        while (TOGGLE_SUIT.get().consumeClick()) {
            if (Minecraft.getInstance().player != null) {
                ClientPacketDistributor.sendToServer(new ToggleSuitPayload());
            }
        }
    }
}
