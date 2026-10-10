package dev.ironsuit;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import net.minecraft.core.particles.ColorParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

/**
 * Server-side logic: assembling / disassembling the nano-suit on a player.
 *
 * Folded state:   chest slot = Nano-Reactor, its SUIT_STORAGE = [head, chest, legs, feet].
 * Deployed state: armor slots hold the Mark 85 pieces, the chestplate's SUIT_STORAGE = [reactor].
 * Items are moved piece by piece, so nothing is lost if the process is interrupted.
 */
public final class SuitManager {
    public static final int MAX_ENERGY = 10_000;
    public static final int START_ENERGY = 2_500;
    private static final int REGEN_PER_SECOND = 40;
    private static final int STEP_TICKS = 8;

    // Indices inside SuitStorage on the reactor
    private static final int HEAD = 0, CHEST = 1, LEGS = 2, FEET = 3;
    private static final int[] ASSEMBLE_ORDER = {FEET, LEGS, HEAD, CHEST};
    private static final int[] DISASSEMBLE_ORDER = {HEAD, LEGS, FEET, CHEST};

    private static final Map<UUID, Task> TASKS = new HashMap<>();

    private static final class Task {
        final boolean assembling;
        int step = 0;
        int timer = 0;

        Task(boolean assembling) {
            this.assembling = assembling;
        }
    }

    private SuitManager() {}

    // ------------------------------------------------------------------ public API

    /** Called when the player presses the suit key. */
    public static void toggle(ServerPlayer player) {
        if (TASKS.containsKey(player.getUUID())) {
            return;
        }
        ItemStack chest = player.getItemBySlot(EquipmentSlot.CHEST);
        if (isReactor(chest)) {
            for (EquipmentSlot slot : new EquipmentSlot[] {EquipmentSlot.HEAD, EquipmentSlot.LEGS, EquipmentSlot.FEET}) {
                if (!player.getItemBySlot(slot).isEmpty()) {
                    player.sendSystemMessage(Component.translatable("message.ironsuit.clear_armor"), true);
                    return;
                }
            }
            TASKS.put(player.getUUID(), new Task(true));
        } else if (isSuitChest(chest) && hasCore(chest)) {
            TASKS.put(player.getUUID(), new Task(false));
        } else {
            player.sendSystemMessage(Component.translatable("message.ironsuit.need_reactor"), true);
        }
    }

    public static void cancel(UUID id) {
        TASKS.remove(id);
    }

    public static boolean isActive(Player player) {
        ItemStack chest = player.getItemBySlot(EquipmentSlot.CHEST);
        return isSuitChest(chest) && hasCore(chest)
                && player.getItemBySlot(EquipmentSlot.HEAD).is(ModItems.MARK85_HELMET.get());
    }

    public static int getEnergy(Player player) {
        ItemStack chest = player.getItemBySlot(EquipmentSlot.CHEST);
        return isSuitChest(chest) || isReactor(chest)
                ? chest.getOrDefault(ModComponents.ENERGY.get(), 0)
                : 0;
    }

    public static boolean consumeEnergy(Player player, int amount) {
        ItemStack chest = player.getItemBySlot(EquipmentSlot.CHEST);
        if (!isSuitChest(chest) && !isReactor(chest)) {
            return false;
        }
        int energy = chest.getOrDefault(ModComponents.ENERGY.get(), 0);
        if (energy < amount) {
            return false;
        }
        chest.set(ModComponents.ENERGY.get(), energy - amount);
        return true;
    }

    /** Called every server tick for every player. */
    public static void tick(ServerPlayer player) {
        Task task = TASKS.get(player.getUUID());
        if (task == null) {
            regenerate(player);
            return;
        }
        if (task.timer-- > 0) {
            return;
        }
        int piece = (task.assembling ? ASSEMBLE_ORDER : DISASSEMBLE_ORDER)[task.step];
        boolean ok = task.assembling ? assembleStep(player, piece, task.step) : disassembleStep(player, piece, task.step);
        task.step++;
        task.timer = STEP_TICKS;
        if (!ok || task.step >= 4) {
            TASKS.remove(player.getUUID());
            if (ok) {
                finishEffect(player, task.assembling);
            }
        }
    }

    // ------------------------------------------------------------------ steps

    private static boolean assembleStep(ServerPlayer player, int piece, int step) {
        ItemStack worn = player.getItemBySlot(EquipmentSlot.CHEST);
        if (!isReactor(worn)) {
            return false;
        }
        SuitStorage storage = storageOf(worn);

        if (piece != CHEST) {
            ItemStack stack = storage.get(piece);
            if (!stack.isEmpty() && player.getItemBySlot(slotOf(piece)).isEmpty()) {
                player.setItemSlot(slotOf(piece), stack);
                worn.set(ModComponents.SUIT_STORAGE.get(), storage.with(piece, ItemStack.EMPTY));
            }
            stepEffect(player, piece, step, true);
            return true;
        }

        // Final step: the chestplate closes around the reactor.
        ItemStack chestplate = storage.get(CHEST);
        if (chestplate.isEmpty()) {
            chestplate = new ItemStack(ModItems.MARK85_CHESTPLATE.get());
        }
        ItemStack core = worn.copy();
        core.remove(ModComponents.SUIT_STORAGE.get());
        chestplate.set(ModComponents.SUIT_STORAGE.get(), new SuitStorage(List.of(core)));
        chestplate.set(ModComponents.ENERGY.get(), worn.getOrDefault(ModComponents.ENERGY.get(), 0));
        player.setItemSlot(EquipmentSlot.CHEST, chestplate);
        stepEffect(player, piece, step, true);
        return true;
    }

    private static boolean disassembleStep(ServerPlayer player, int piece, int step) {
        ItemStack chestplate = player.getItemBySlot(EquipmentSlot.CHEST);
        if (!isSuitChest(chestplate) || !hasCore(chestplate)) {
            return false;
        }
        ItemStack core = chestplate.get(ModComponents.SUIT_STORAGE.get()).get(0);
        SuitStorage folded = core.get(ModComponents.SUIT_STORAGE.get());
        if (folded == null) {
            folded = new SuitStorage(List.of(ItemStack.EMPTY, ItemStack.EMPTY, ItemStack.EMPTY, ItemStack.EMPTY));
        }

        if (piece != CHEST) {
            ItemStack worn = player.getItemBySlot(slotOf(piece));
            if (isPieceFor(piece, worn)) {
                player.setItemSlot(slotOf(piece), ItemStack.EMPTY);
                core.set(ModComponents.SUIT_STORAGE.get(), folded.with(piece, worn));
                chestplate.set(ModComponents.SUIT_STORAGE.get(), new SuitStorage(List.of(core)));
            }
            stepEffect(player, piece, step, false);
            return true;
        }

        // Final step: fold the chestplate into the reactor and leave only the reactor on the chest.
        ItemStack folding = chestplate.copy();
        folding.remove(ModComponents.SUIT_STORAGE.get());
        folding.remove(ModComponents.ENERGY.get());
        core.set(ModComponents.ENERGY.get(), chestplate.getOrDefault(ModComponents.ENERGY.get(), 0));
        core.set(ModComponents.SUIT_STORAGE.get(), folded.with(CHEST, folding));
        player.setItemSlot(EquipmentSlot.CHEST, core);
        stepEffect(player, piece, step, false);
        return true;
    }

    // ------------------------------------------------------------------ helpers

    private static SuitStorage storageOf(ItemStack reactor) {
        SuitStorage storage = reactor.get(ModComponents.SUIT_STORAGE.get());
        if (storage == null) {
            // A brand-new reactor generates a fresh suit out of nanoparticles.
            storage = new SuitStorage(List.of(
                    new ItemStack(ModItems.MARK85_HELMET.get()),
                    new ItemStack(ModItems.MARK85_CHESTPLATE.get()),
                    new ItemStack(ModItems.MARK85_LEGGINGS.get()),
                    new ItemStack(ModItems.MARK85_BOOTS.get())));
            reactor.set(ModComponents.SUIT_STORAGE.get(), storage);
        }
        return storage;
    }

    private static void regenerate(ServerPlayer player) {
        if (player.tickCount % 20 != 0) {
            return;
        }
        ItemStack chest = player.getItemBySlot(EquipmentSlot.CHEST);
        if (isReactor(chest) || isSuitChest(chest)) {
            int energy = chest.getOrDefault(ModComponents.ENERGY.get(), 0);
            if (energy < MAX_ENERGY) {
                chest.set(ModComponents.ENERGY.get(), Math.min(MAX_ENERGY, energy + REGEN_PER_SECOND));
            }
        }
    }

    public static boolean isReactor(ItemStack stack) {
        return stack.is(ModItems.NANO_REACTOR.get());
    }

    public static boolean isSuitChest(ItemStack stack) {
        return stack.is(ModItems.MARK85_CHESTPLATE.get());
    }

    private static boolean hasCore(ItemStack chestplate) {
        SuitStorage storage = chestplate.get(ModComponents.SUIT_STORAGE.get());
        return storage != null && isReactor(storage.get(0));
    }

    private static boolean isPieceFor(int piece, ItemStack stack) {
        return switch (piece) {
            case HEAD -> stack.is(ModItems.MARK85_HELMET.get());
            case LEGS -> stack.is(ModItems.MARK85_LEGGINGS.get());
            case FEET -> stack.is(ModItems.MARK85_BOOTS.get());
            default -> false;
        };
    }

    private static EquipmentSlot slotOf(int piece) {
        return switch (piece) {
            case HEAD -> EquipmentSlot.HEAD;
            case CHEST -> EquipmentSlot.CHEST;
            case LEGS -> EquipmentSlot.LEGS;
            default -> EquipmentSlot.FEET;
        };
    }

    // ------------------------------------------------------------------ effects

    private static void stepEffect(ServerPlayer player, int piece, int step, boolean assembling) {
        if (!(player.level() instanceof ServerLevel level)) {
            return;
        }
        double height = switch (piece) {
            case FEET -> 0.15;
            case LEGS -> 0.6;
            case CHEST -> 1.15;
            default -> 1.7;
        };
        level.sendParticles(ParticleTypes.END_ROD, player.getX(), player.getY() + height, player.getZ(),
                24, 0.35, 0.18, 0.35, 0.04);
        level.sendParticles(ParticleTypes.ELECTRIC_SPARK, player.getX(), player.getY() + height, player.getZ(),
                10, 0.4, 0.2, 0.4, 0.1);
        float pitch = assembling ? 0.9F + step * 0.15F : 1.5F - step * 0.15F;
        play(level, player, SoundEvents.AMETHYST_BLOCK_CHIME, 1.0F, pitch);
    }

    private static void finishEffect(ServerPlayer player, boolean assembling) {
        if (!(player.level() instanceof ServerLevel level)) {
            return;
        }
        level.sendParticles(ColorParticleOption.create(ParticleTypes.FLASH, 0xFFFFFF),
            player.getX(), player.getY() + 1.0, player.getZ(), 1, 0.0, 0.0, 0.0, 0.0);
        play(level, player, assembling ? SoundEvents.BEACON_ACTIVATE : SoundEvents.BEACON_DEACTIVATE, 1.0F, 1.2F);
    }

    private static void play(ServerLevel level, ServerPlayer player, SoundEvent sound, float volume, float pitch) {
        level.playSound(null, player.getX(), player.getY(), player.getZ(), sound, SoundSource.PLAYERS, volume, pitch);
    }
}
