package top.mrxiaom.hidemyarmors;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.events.ListenerPriority;
import com.comphenix.protocol.events.ListenerOptions;
import com.comphenix.protocol.events.PacketAdapter;
import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.events.PacketEvent;
import de.tr7zw.changeme.nbtapi.*;
import de.tr7zw.changeme.nbtapi.iface.ReadableNBT;
import org.bukkit.Color;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.LeatherArmorMeta;
import org.bukkit.inventory.meta.SkullMeta;
import org.jetbrains.annotations.Nullable;

public class EntityPacketAdapter extends PacketAdapter {
    HideMyArmors plugin;
    public EntityPacketAdapter(HideMyArmors plugin) {
        super(params().plugin(plugin).listenerPriority(ListenerPriority.NORMAL)
                .types(PacketType.Play.Server.ENTITY_EQUIPMENT).options(ListenerOptions.ASYNC));
        this.plugin = plugin;
    }

    @Override
    public void onPacketSending(PacketEvent event) {
        PacketContainer packet = event.getPacket();
        Integer entityId = packet.getIntegers().readSafely(0);
        if (entityId == null || entityId == event.getPlayer().getEntityId()) return;
        Integer hiddenSlots = plugin.hiddenSlots.get(entityId);
        if (hiddenSlots == null || (hiddenSlots == 0 && !plugin.eraseEquipmentsInfo)) return;
        packet = packet.shallowClone();
        event.setPacket(packet);

        if (plugin.newVersion) {
            ModifierNewPacket.modify(packet, hiddenSlots, this::modifyItem);
        } else if (plugin.twoHands) {
            ModifierOldPacket.modify(packet, hiddenSlots, this::modifyItem);
        } else {
            ModifierVeryOldPacket.modify(packet, hiddenSlots, this::modifyItem);
        }
    }

    public interface TriFunction<A, B, C, R> {
        R apply(A a, B b, C c);
    }

    @Nullable
    private ItemStack modifyItem(int hiddenSlots, int slot, ItemStack item) {
        if (slot < ModifierVeryOldPacket.SLOT_HEAD || slot > ModifierVeryOldPacket.SLOT_FEET) return null;
        if ((hiddenSlots & (1 << slot)) != 0) {
             return new ItemStack(Material.AIR);
        }
        if (plugin.eraseEquipmentsInfo) {
            return eraseItemMeta(item);
        }
        return null;
    }

    private ItemStack eraseItemMeta(ItemStack item) {
        if (item == null || item.getType().equals(Material.AIR)) return null;
        ItemMeta oldMeta = item.getItemMeta();
        if (oldMeta == null) return null;

        if (NBT.get(item, nbt -> {
            boolean has = nbt.hasTag("magic_cosmetic");
            if (has) return true;
            ReadableNBT tag = nbt.hasTag("tag", NBTType.NBTTagCompound) ? nbt.getCompound("tag") : null;
            return tag != null && tag.hasTag("magic_cosmetic");
        })) return null;

        ItemStack newItem = new ItemStack(item.getType(), item.getAmount());
        ItemMeta meta = newItem.getItemMeta();
        if (oldMeta instanceof SkullMeta oldSkull && meta instanceof SkullMeta newSkull) {
            newSkull.setOwnerProfile(oldSkull.getOwnerProfile());
        }
        if (meta != null) {
            if (oldMeta instanceof LeatherArmorMeta && meta instanceof LeatherArmorMeta) {
                Color color = ((LeatherArmorMeta) oldMeta).getColor();
                ((LeatherArmorMeta) meta).setColor(color);
            }
            if (oldMeta.hasEnchants()) {
                meta.setEnchantmentGlintOverride(true);
            }
            if (oldMeta.hasEnchantmentGlintOverride()) {
                meta.setEnchantmentGlintOverride(oldMeta.getEnchantmentGlintOverride());
            }
            if (plugin.supportCMD) { // 1.14+
                CustomModelData.transfer(oldMeta, meta, plugin.modelComponents);
            }
            if (plugin.itemModels && oldMeta.hasItemModel()) {
                meta.setItemModel(oldMeta.getItemModel());
            }
        }
        newItem.setItemMeta(meta);
        return newItem;
    }
}
