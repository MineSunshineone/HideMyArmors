package top.mrxiaom.hidemyarmors;

import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.reflect.StructureModifier;
import com.comphenix.protocol.wrappers.EnumWrappers;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

public class ModifierVeryOldPacket {
    public static final int SLOT_HAND = 0;
    public static final int SLOT_HEAD = 1;
    public static final int SLOT_CHEST = 2;
    public static final int SLOT_LEGS = 3;
    public static final int SLOT_FEET = 4;
    public static final int SLOT_OFF_HAND = 5;

    /**
     * 1.8.X
     */
    public static void modify(PacketContainer packet, Player targetPlayer, EntityPacketAdapter.TriFunction<Player, Integer, ItemStack, ItemStack> modifyItem) {
        StructureModifier<Short> itemSlots = packet.getShorts();
        StructureModifier<ItemStack> itemModifier = packet.getItemModifier();

        short slot = itemSlots.readSafely(0);
        ItemStack item = itemModifier.readSafely(0);
        ItemStack newItem = modifyItem.apply(targetPlayer, (int) slot, item);
        if (newItem != null) {
            itemModifier.writeSafely(0, newItem);
        }
    }
}
