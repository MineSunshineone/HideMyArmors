package top.mrxiaom.hidemyarmors;

import org.bukkit.inventory.meta.ItemMeta;

public class CustomModelData {
    public static void transfer(ItemMeta oldMeta, ItemMeta newMeta, boolean modelComponents) {
        if (oldMeta.hasCustomModelData()) {
            if (modelComponents) {
                newMeta.setCustomModelDataComponent(oldMeta.getCustomModelDataComponent());
            } else {
                newMeta.setCustomModelData(oldMeta.getCustomModelData());
            }
        }
    }
}
