package dev.ironsuit;

import net.minecraft.core.component.DataComponents;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.equipment.ArmorType;
import net.minecraft.world.item.equipment.Equippable;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModItems {
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(IronSuit.MODID);

    // ---- Material ----
    public static final DeferredItem<Item> GOLD_TITANIUM_INGOT =
            ITEMS.registerItem("gold_titanium_ingot", Item::new);

    // ---- Nano-Reactor: worn on the chest, generates / stores the nano-suit ----
    public static final DeferredItem<Item> NANO_REACTOR = ITEMS.registerItem("nano_reactor",
            props -> new Item(props
                    .stacksTo(1)
                    .component(ModComponents.ENERGY.get(), SuitManager.START_ENERGY)
                    .component(DataComponents.EQUIPPABLE, Equippable.builder(EquipmentSlot.CHEST)
                            .setEquipSound(SoundEvents.ARMOR_EQUIP_NETHERITE)
                            .setAsset(ModArmorMaterials.REACTOR_ASSET)
                            .build())));

    // ---- Mark 85 armor pieces (normally created by the reactor) ----
    public static final DeferredItem<Item> MARK85_HELMET = ITEMS.registerItem("mark85_helmet",
            props -> new Item(props.humanoidArmor(ModArmorMaterials.MARK85, ArmorType.HELMET)));
    public static final DeferredItem<Item> MARK85_CHESTPLATE = ITEMS.registerItem("mark85_chestplate",
            props -> new Item(props.humanoidArmor(ModArmorMaterials.MARK85, ArmorType.CHESTPLATE)));
    public static final DeferredItem<Item> MARK85_LEGGINGS = ITEMS.registerItem("mark85_leggings",
            props -> new Item(props.humanoidArmor(ModArmorMaterials.MARK85, ArmorType.LEGGINGS)));
    public static final DeferredItem<Item> MARK85_BOOTS = ITEMS.registerItem("mark85_boots",
            props -> new Item(props.humanoidArmor(ModArmorMaterials.MARK85, ArmorType.BOOTS)));

    private ModItems() {}
}
