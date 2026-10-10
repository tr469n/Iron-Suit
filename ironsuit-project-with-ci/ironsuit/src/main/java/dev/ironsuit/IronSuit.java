package dev.ironsuit;

import com.mojang.logging.LogUtils;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import org.slf4j.Logger;

@Mod(IronSuit.MODID)
public class IronSuit {
    public static final String MODID = "ironsuit";
    public static final Logger LOGGER = LogUtils.getLogger();

    public IronSuit(IEventBus modEventBus) {
        // Data components must be registered before items that use them as defaults.
        ModComponents.COMPONENTS.register(modEventBus);
        ModItems.ITEMS.register(modEventBus);
        ModCreativeTabs.TABS.register(modEventBus);
        LOGGER.info("Iron Suit loaded");
    }
}
