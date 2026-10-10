package dev.ironsuit;

import java.util.function.Supplier;

import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModCreativeTabs {
    public static final DeferredRegister<CreativeModeTab> TABS =
            DeferredRegister.create(Registries.CREATIVE_MODE_TAB, IronSuit.MODID);

    public static final Supplier<CreativeModeTab> IRON_SUIT_TAB = TABS.register("iron_suit",
            () -> CreativeModeTab.builder()
                    .title(Component.translatable("itemGroup." + IronSuit.MODID))
                    .icon(() -> new ItemStack(ModItems.NANO_REACTOR.get()))
                    .displayItems((parameters, output) -> {
                        output.accept(ModItems.GOLD_TITANIUM_INGOT);
                        output.accept(ModItems.NANO_REACTOR);
                        output.accept(ModItems.MARK85_HELMET);
                        output.accept(ModItems.MARK85_CHESTPLATE);
                        output.accept(ModItems.MARK85_LEGGINGS);
                        output.accept(ModItems.MARK85_BOOTS);
                    })
                    .build());

    private ModCreativeTabs() {}
}
