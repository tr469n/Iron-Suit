package dev.ironsuit;

import java.util.Map;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.equipment.ArmorMaterial;
import net.minecraft.world.item.equipment.ArmorType;
import net.minecraft.world.item.equipment.EquipmentAsset;
import net.minecraft.world.item.equipment.EquipmentAssets;

public final class ModArmorMaterials {
    /** assets/ironsuit/equipment/mark85.json */
    public static final ResourceKey<EquipmentAsset> MARK85_ASSET = ResourceKey.create(
            EquipmentAssets.ROOT_ID, Identifier.fromNamespaceAndPath(IronSuit.MODID, "mark85"));

    /** assets/ironsuit/equipment/reactor.json (the Nano-Reactor worn on the chest) */
    public static final ResourceKey<EquipmentAsset> REACTOR_ASSET = ResourceKey.create(
            EquipmentAssets.ROOT_ID, Identifier.fromNamespaceAndPath(IronSuit.MODID, "reactor"));

    /** data/ironsuit/tags/item/repairs_mark85_armor.json */
    public static final TagKey<Item> REPAIRS_MARK85 = TagKey.create(
            Registries.ITEM, Identifier.fromNamespaceAndPath(IronSuit.MODID, "repairs_mark85_armor"));

    public static final ArmorMaterial MARK85 = new ArmorMaterial(
            50,
                        Map.of(
                                        ArmorType.BOOTS, 4,
                                        ArmorType.LEGGINGS, 7,
                                        ArmorType.CHESTPLATE, 9,
                                        ArmorType.HELMET, 4,
                                        ArmorType.BODY, 11),
            15,
            SoundEvents.ARMOR_EQUIP_NETHERITE,
            4.0F,
            0.15F,
            REPAIRS_MARK85,
            MARK85_ASSET
    );

    private ModArmorMaterials() {}
}
