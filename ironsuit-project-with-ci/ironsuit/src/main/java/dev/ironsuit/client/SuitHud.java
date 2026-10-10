package dev.ironsuit.client;

import dev.ironsuit.ModComponents;
import dev.ironsuit.ModItems;
import dev.ironsuit.SuitManager;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;

/** Iron-Man style helmet HUD. Shown while wearing the Mark 85 helmet + chestplate. */
public final class SuitHud {
    private static final int CYAN = 0xFF5FE6FF;
    private static final int CYAN_DIM = 0x9038C8E8;
    private static final int PANEL = 0x90041018;
    private static final int BAR_BG = 0xA0102028;
    private static final int OK = 0xFF6CFFB0;
    private static final int WARN = 0xFFFFB347;
    private static final int DANGER = 0xFFFF4A4A;
    private static final String[] COMPASS = {"N", "NE", "E", "SE", "S", "SW", "W", "NW"};

    private SuitHud() {}

    /** HUD layer callback: (GuiGraphicsExtractor, DeltaTracker). */
    public static void render(GuiGraphicsExtractor g, DeltaTracker deltaTracker) {
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        if (player == null) {
            return;
        }
        ItemStack chest = player.getItemBySlot(EquipmentSlot.CHEST);
        if (chest.is(ModItems.NANO_REACTOR.get())) {
            renderReactorPanel(g, mc, chest);
        } else if (chest.is(ModItems.MARK85_CHESTPLATE.get())) {
            if (player.getItemBySlot(EquipmentSlot.HEAD).is(ModItems.MARK85_HELMET.get())) {
                renderFull(g, mc, player, chest);
            } else {
                renderCompact(g, mc, chest); // helmet taken off: no helmet display, only a small status panel
            }
        }
    }

    // ------------------------------------------------------------------ small panel (reactor only)

    private static void renderReactorPanel(GuiGraphicsExtractor g, Minecraft mc, ItemStack reactor) {
        Font font = mc.font;
        int x = 8, y = 8, w = 128, h = 38;
        g.fill(x, y, x + w, y + h, PANEL);
        g.outline(x, y, w, h, CYAN_DIM);
        g.text(font, Component.translatable("hud.ironsuit.reactor"), x + 6, y + 5, CYAN, true);
        energyBar(g, x + 6, y + 17, w - 12, energyOf(reactor));
        g.text(font, Component.translatable("hud.ironsuit.deploy_hint", IronSuitClient.TOGGLE_SUIT.get().getTranslatedKeyMessage()),
                x + 6, y + 26, CYAN_DIM, false);
    }

    // ------------------------------------------------------------------ compact panel (helmet off)

    private static void renderCompact(GuiGraphicsExtractor g, Minecraft mc, ItemStack chest) {
        Font font = mc.font;
        int x = 8, y = 8, w = 150, h = 62;
        g.fill(x, y, x + w, y + h, PANEL);
        g.outline(x, y, w, h, CYAN_DIM);
        g.text(font, Component.translatable("hud.ironsuit.title"), x + 6, y + 5, CYAN, true);
        energyBar(g, x + 6, y + 17, w - 12, energyOf(chest));
        g.text(font, Component.translatable("hud.ironsuit.repulsor_hint", IronSuitClient.FIRE_REPULSOR.get().getTranslatedKeyMessage()),
                x + 6, y + 27, CYAN_DIM, false);
        g.text(font, Component.translatable("hud.ironsuit.beam_hint", IronSuitClient.FIRE_BEAM.get().getTranslatedKeyMessage()),
                x + 6, y + 37, CYAN_DIM, false);
        g.text(font, Component.translatable("hud.ironsuit.helmet_on_hint", IronSuitClient.TOGGLE_HELMET.get().getTranslatedKeyMessage()),
                x + 6, y + 47, CYAN_DIM, false);
    }

    // ------------------------------------------------------------------ full HUD

    private static void renderFull(GuiGraphicsExtractor g, Minecraft mc, LocalPlayer player, ItemStack chest) {
        Font font = mc.font;
        int width = mc.getWindow().getGuiScaledWidth();
        int height = mc.getWindow().getGuiScaledHeight();
        double time = System.currentTimeMillis() / 1000.0;
        int pulseAlpha = 0x60 + (int) (0x40 * (0.5 + 0.5 * Math.sin(time * 3.0)));
        int pulse = (pulseAlpha << 24) | 0x5FE6FF;

        corners(g, width, height, pulse);

        // ---- left panel: status, energy, integrity
        int lx = 10, ly = 10, lw = 138, lh = 74;
        g.fill(lx, ly, lx + lw, ly + lh, PANEL);
        g.outline(lx, ly, lw, lh, CYAN_DIM);
        g.fill(lx + 6, ly + 7, lx + 10, ly + 11, pulse | 0xFF000000);
        g.text(font, Component.translatable("hud.ironsuit.title"), lx + 14, ly + 5, CYAN, true);
        g.text(font, Component.translatable("hud.ironsuit.online"), lx + 6, ly + 17, OK, false);

        g.text(font, Component.translatable("hud.ironsuit.energy"), lx + 6, ly + 29, CYAN_DIM, false);
        energyBar(g, lx + 6, ly + 39, lw - 12, energyOf(chest));

        g.text(font, Component.translatable("hud.ironsuit.integrity"), lx + 6, ly + 52, CYAN_DIM, false);
        EquipmentSlot[] slots = {EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET};
        String[] names = {"H", "C", "L", "B"};
        for (int i = 0; i < 4; i++) {
            int bx = lx + 6 + i * 32;
            int percent = integrity(player.getItemBySlot(slots[i]));
            int color = percent > 50 ? OK : percent > 20 ? WARN : DANGER;
            g.text(font, names[i] + " " + percent, bx, ly + 62, color, false);
        }

        // ---- weapon hints under the left panel
        int wy = ly + lh + 4, wh = 36;
        g.fill(lx, wy, lx + lw, wy + wh, PANEL);
        g.outline(lx, wy, lw, wh, CYAN_DIM);
        g.text(font, Component.translatable("hud.ironsuit.repulsor_hint", IronSuitClient.FIRE_REPULSOR.get().getTranslatedKeyMessage()),
                lx + 6, wy + 4, CYAN, false);
        g.text(font, Component.translatable("hud.ironsuit.beam_hint", IronSuitClient.FIRE_BEAM.get().getTranslatedKeyMessage()),
                lx + 6, wy + 14, CYAN, false);
        g.text(font, Component.translatable("hud.ironsuit.helmet_off_hint", IronSuitClient.TOGGLE_HELMET.get().getTranslatedKeyMessage()),
                lx + 6, wy + 24, CYAN, false);

        // ---- right panel: speed, altitude, heading
        Vec3 motion = player.getDeltaMovement();
        double speed = motion.length() * 20.0;
        int heading = (((int) Math.floor(player.getYRot() + 180.0F)) % 360 + 360) % 360;
        String compass = COMPASS[((heading + 22) / 45) % 8];
        int rw = 118, rh = 52, rx = width - rw - 10, ry = 10;
        g.fill(rx, ry, rx + rw, ry + rh, PANEL);
        g.outline(rx, ry, rw, rh, CYAN_DIM);
        g.text(font, Component.translatable("hud.ironsuit.speed", String.format("%.1f", speed)), rx + 6, ry + 6, CYAN, false);
        g.text(font, Component.translatable("hud.ironsuit.altitude", (int) player.getY()), rx + 6, ry + 20, CYAN, false);
        g.text(font, Component.translatable("hud.ironsuit.heading", heading, compass), rx + 6, ry + 34, CYAN, false);
    }

    // ------------------------------------------------------------------ pieces

    private static void corners(GuiGraphicsExtractor g, int w, int h, int color) {
        int m = 6, len = 26;
        // top-left, top-right, bottom-left, bottom-right
        g.fill(m, m, m + len, m + 1, color);
        g.fill(m, m, m + 1, m + len, color);
        g.fill(w - m - len, m, w - m, m + 1, color);
        g.fill(w - m - 1, m, w - m, m + len, color);
        g.fill(m, h - m - 1, m + len, h - m, color);
        g.fill(m, h - m - len, m + 1, h - m, color);
        g.fill(w - m - len, h - m - 1, w - m, h - m, color);
        g.fill(w - m - 1, h - m - len, w - m, h - m, color);
    }

    private static void energyBar(GuiGraphicsExtractor g, int x, int y, int width, int energy) {
        float fraction = Math.max(0.0F, Math.min(1.0F, energy / (float) SuitManager.MAX_ENERGY));
        int filled = Math.round(width * fraction);
        int color = fraction > 0.5F ? CYAN : fraction > 0.2F ? WARN : DANGER;
        g.fill(x, y, x + width, y + 6, BAR_BG);
        g.fill(x, y, x + filled, y + 6, color);
        g.outline(x, y, width, 6, CYAN_DIM);
    }

    private static int energyOf(ItemStack stack) {
        return stack.getOrDefault(ModComponents.ENERGY.get(), 0);
    }

    private static int integrity(ItemStack stack) {
        if (stack.isEmpty()) {
            return 0;
        }
        int max = stack.getMaxDamage();
        return max <= 0 ? 100 : (max - stack.getDamageValue()) * 100 / max;
    }
}
