package dev.ironsuit;

import java.util.ArrayList;
import java.util.List;

import com.mojang.serialization.Codec;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;

/**
 * Fixed list of item stacks stored inside an item.
 * On the Nano-Reactor: index 0..3 = head, chest, legs, feet pieces of the folded suit.
 * On the deployed chestplate: index 0 = the Nano-Reactor worn "inside" the suit.
 */
public record SuitStorage(List<ItemStack> items) {
    public static final Codec<SuitStorage> CODEC =
            ItemStack.OPTIONAL_CODEC.listOf().xmap(SuitStorage::new, SuitStorage::items);

    public static final StreamCodec<RegistryFriendlyByteBuf, SuitStorage> STREAM_CODEC =
            ItemStack.OPTIONAL_STREAM_CODEC.apply(ByteBufCodecs.list()).map(SuitStorage::new, SuitStorage::items);

    public SuitStorage {
        List<ItemStack> copy = new ArrayList<>(items.size());
        for (ItemStack stack : items) {
            copy.add(stack.copy());
        }
        items = List.copyOf(copy);
    }

    /** Returns a copy of the stack at the index, or EMPTY if there is none. */
    public ItemStack get(int index) {
        return index >= 0 && index < items.size() ? items.get(index).copy() : ItemStack.EMPTY;
    }

    /** Returns a new storage with the given slot replaced (list is padded to at least 4 entries). */
    public SuitStorage with(int index, ItemStack stack) {
        List<ItemStack> list = new ArrayList<>(items);
        while (list.size() < Math.max(4, index + 1)) {
            list.add(ItemStack.EMPTY);
        }
        list.set(index, stack);
        return new SuitStorage(list);
    }

    @Override
    public boolean equals(Object other) {
        return other instanceof SuitStorage storage && ItemStack.listMatches(items, storage.items);
    }

    @Override
    public int hashCode() {
        return ItemStack.hashStackList(items);
    }
}
