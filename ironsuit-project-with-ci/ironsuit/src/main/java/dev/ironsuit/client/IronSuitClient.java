package dev.ironsuit.client;

import com.mojang.blaze3d.platform.InputConstants;
import dev.ironsuit.IronSuit;
import dev.ironsuit.SuitCombat;
import dev.ironsuit.network.ToggleHelmetPayload;
import dev.ironsuit.network.ToggleSuitPayload;
import dev.ironsuit.network.WeaponInputPayload;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.Identifier;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.client.event.ScreenEvent;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;
import net.neoforged.neoforge.common.util.Lazy;

@EventBusSubscriber(modid = IronSuit.MODID, value = Dist.CLIENT)
public final class IronSuitClient {
    public static final KeyMapping.Category CATEGORY =
            new KeyMapping.Category(Identifier.fromNamespaceAndPath(IronSuit.MODID, "main"));

    /** Default key: R. Rebindable in Controls. */
    public static final Lazy<KeyMapping> TOGGLE_SUIT =
            Lazy.of(() -> new KeyMapping("key.ironsuit.toggle_suit", InputConstants.KEY_R, CATEGORY));

    /** Default key: V. Hold to fire the palm repulsors. */
    public static final Lazy<KeyMapping> FIRE_REPULSOR =
            Lazy.of(() -> new KeyMapping("key.ironsuit.fire_repulsor", InputConstants.KEY_V, CATEGORY));

    /** Default key: B. Hold to charge the chest reactor beam, release to fire. */
    public static final Lazy<KeyMapping> FIRE_BEAM =
            Lazy.of(() -> new KeyMapping("key.ironsuit.fire_beam", InputConstants.KEY_B, CATEGORY));

    /** Default key: M. Takes the helmet off / puts it on; the suit keeps working without it. */
    public static final Lazy<KeyMapping> TOGGLE_HELMET =
            Lazy.of(() -> new KeyMapping("key.ironsuit.toggle_helmet", InputConstants.KEY_M, CATEGORY));

    private static int heartbeat;
    private static boolean repulsorWasDown;
    private static boolean beamWasDown;
    private static boolean screenOpen;

    private IronSuitClient() {}

    @SubscribeEvent // mod event bus
    public static void registerKeys(RegisterKeyMappingsEvent event) {
        event.registerCategory(CATEGORY);
        event.register(TOGGLE_SUIT.get());
        event.register(TOGGLE_HELMET.get());
        event.register(FIRE_REPULSOR.get());
        event.register(FIRE_BEAM.get());
    }

    @SubscribeEvent // mod event bus
    public static void registerHud(RegisterGuiLayersEvent event) {
        event.registerAboveAll(Identifier.fromNamespaceAndPath(IronSuit.MODID, "suit_hud"), SuitHud::render);
    }

    @SubscribeEvent // game event bus
    public static void onScreenOpening(ScreenEvent.Opening event) {
        screenOpen = event.getNewScreen() != null;
    }

    @SubscribeEvent // game event bus
    public static void onClientTick(ClientTickEvent.Post event) {
        while (TOGGLE_SUIT.get().consumeClick()) {
            if (Minecraft.getInstance().player != null) {
                ClientPacketDistributor.sendToServer(new ToggleSuitPayload());
            }
        }

        while (TOGGLE_HELMET.get().consumeClick()) {
            if (Minecraft.getInstance().player != null) {
                ClientPacketDistributor.sendToServer(new ToggleHelmetPayload());
            }
        }

        // Weapons. The state is re-sent every few ticks while a key is held (the server releases the weapon if it
        // stops hearing from us), and a quick tap is caught through consumeClick() so it still fires once.
        Minecraft mc = Minecraft.getInstance();
        boolean canFire = mc.player != null && !screenOpen;
        boolean repulsorTapped = false;
        while (FIRE_REPULSOR.get().consumeClick()) {
            repulsorTapped = true;
        }
        while (FIRE_BEAM.get().consumeClick()) {
            // drained so clicks do not pile up; the beam is driven by isDown()
        }
        boolean repulsorDown = canFire && (FIRE_REPULSOR.get().isDown() || repulsorTapped);
        boolean beamDown = canFire && FIRE_BEAM.get().isDown();
        heartbeat++;
        if (mc.player != null) {
            boolean beat = heartbeat % 4 == 0;
            if (repulsorDown != repulsorWasDown || (repulsorDown && beat)) {
                ClientPacketDistributor.sendToServer(new WeaponInputPayload(SuitCombat.WEAPON_REPULSOR, repulsorDown));
            }
            if (beamDown != beamWasDown || (beamDown && beat)) {
                ClientPacketDistributor.sendToServer(new WeaponInputPayload(SuitCombat.WEAPON_BEAM, beamDown));
            }
        }
        repulsorWasDown = repulsorDown;
        beamWasDown = beamDown;
    }
}
