package top.mrxiaom.hidemyarmors;

import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.reflect.StructureModifier;
import com.comphenix.protocol.wrappers.EnumWrappers;
import com.comphenix.protocol.wrappers.Pair;
import org.bukkit.inventory.ItemStack;

import java.util.List;
import java.util.ArrayList;

import static top.mrxiaom.hidemyarmors.ModifierOldPacket.convertSlot;

public class ModifierNewPacket {
    /**
     * 1.16+ 新版本
     */
    public static void modify(PacketContainer packet, int hiddenSlots, EntityPacketAdapter.TriFunction<Integer, Integer, ItemStack, ItemStack> modifyItem) {
        StructureModifier<List<Pair<EnumWrappers.ItemSlot, ItemStack>>> modifier = packet.getSlotStackPairLists();

        List<Pair<EnumWrappers.ItemSlot, ItemStack>> original = modifier.readSafely(0);
        if (original == null) return;
        List<Pair<EnumWrappers.ItemSlot, ItemStack>> list = new ArrayList<>(original.size());
        for (Pair<EnumWrappers.ItemSlot, ItemStack> pair : original) {
            int slotNum = convertSlot(pair.getFirst());
            ItemStack newItem = slotNum == -1 ? null : modifyItem.apply(hiddenSlots, slotNum, pair.getSecond());
            list.add(new Pair<>(pair.getFirst(), newItem == null ? pair.getSecond() : newItem));
        }
        modifier.write(0, list);
    }
}
