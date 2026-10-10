package dev.ironsuit;

import java.util.function.Supplier;

import com.mojang.serialization.Codec;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.codec.ByteBufCodecs;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModComponents {
    public static final DeferredRegister.DataComponents COMPONENTS =
            DeferredRegister.createDataComponents(Registries.DATA_COMPONENT_TYPE, IronSuit.MODID);

    /** Folded suit pieces (on the reactor) or the reactor inside the deployed suit (on the chestplate). */
    public static final Supplier<DataComponentType<SuitStorage>> SUIT_STORAGE =
            COMPONENTS.registerComponentType("suit_storage",
                    builder -> builder.persistent(SuitStorage.CODEC).networkSynchronized(SuitStorage.STREAM_CODEC));

    /** Stored suit energy. */
    public static final Supplier<DataComponentType<Integer>> ENERGY =
            COMPONENTS.registerComponentType("energy",
                    builder -> builder.persistent(Codec.INT).networkSynchronized(ByteBufCodecs.VAR_INT));

    private ModComponents() {}
}
