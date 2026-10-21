package top.mrxiaom.hidemyarmors;

import com.comphenix.protocol.wrappers.EnumWrappers;

import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.components.CustomModelDataComponent;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class CompatibilityTest {
    @Test
    void onlyArmorSlotsAreModified() {
        assertEquals(1, ModifierOldPacket.convertSlot(EnumWrappers.ItemSlot.HEAD));
        assertEquals(-1, ModifierOldPacket.convertSlot(EnumWrappers.ItemSlot.MAINHAND));
        assertEquals(-1, ModifierOldPacket.convertSlot(EnumWrappers.ItemSlot.OFFHAND));
        assertEquals(-1, ModifierOldPacket.convertSlot(EnumWrappers.ItemSlot.BODY));
        assertEquals(-1, ModifierOldPacket.convertSlot(null));
    }
    @Test
    void modernCustomModelDataKeepsAllComponents() {
        ItemMeta source = mock(ItemMeta.class);
        ItemMeta target = mock(ItemMeta.class);
        CustomModelDataComponent component = mock(CustomModelDataComponent.class);
        when(source.hasCustomModelData()).thenReturn(true);
        when(source.getCustomModelDataComponent()).thenReturn(component);
        CustomModelData.transfer(source, target, true);
        verify(target).setCustomModelDataComponent(component);
        verify(source, never()).getCustomModelData();
    }

    @Test
    void legacyCustomModelDataDoesNotCallNewApi() {
        ItemMeta source = mock(ItemMeta.class);
        ItemMeta target = mock(ItemMeta.class);
        when(source.hasCustomModelData()).thenReturn(true);
        when(source.getCustomModelData()).thenReturn(123);
        CustomModelData.transfer(source, target, false);
        verify(target).setCustomModelData(123);
        verify(source, never()).getCustomModelDataComponent();
    }
}
