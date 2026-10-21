package top.mrxiaom.hidemyarmors;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.events.PacketEvent;
import com.comphenix.protocol.wrappers.EnumWrappers;
import com.comphenix.protocol.wrappers.Pair;
import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.GameMode;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.LeatherArmorMeta;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.plugin.java.JavaPlugin;

import java.lang.reflect.Proxy;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/** Run only in an isolated server with no real players. */
public class ServerCompatibilityProbe extends JavaPlugin {
    @Override
    public void onEnable() {
        Bukkit.getGlobalRegionScheduler().runDelayed(this, task -> {
            CompletableFuture.runAsync(() -> {
                try {
                    checkPackets();
                    getLogger().info("COMPATIBILITY PROBE PASSED: async equipment isolation, self packets, unknown entities, metadata, model data, glint, hands unchanged");
                } catch (Throwable error) {
                    getLogger().log(java.util.logging.Level.SEVERE, "COMPATIBILITY PROBE FAILED", error);
                } finally {
                    if (!Boolean.getBoolean("hidemyarmors.probe.keepRunning")) {
                        Bukkit.getGlobalRegionScheduler().run(this, stop -> Bukkit.shutdown());
                    }
                }
            });
        }, 40L);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) return true;
        if (args.length > 0 && args[0].equals("permission")) {
            player.addAttachment(this, "hidemyarmors.hide.head", true);
        } else if (args.length > 0 && args[0].equals("erase")) {
            ((HideMyArmors) Bukkit.getPluginManager().getPlugin("HideMyArmors")).eraseEquipmentsInfo = true;
        } else {
            player.setGameMode(GameMode.CREATIVE);
            player.getInventory().setHelmet(new ItemStack(
                    player.getInventory().getHelmet() == null || player.getInventory().getHelmet().getType() != Material.DIAMOND_HELMET
                            ? Material.DIAMOND_HELMET : Material.NETHERITE_HELMET));
        }
        player.sendMessage("probe-ready");
        return true;
    }

    @SuppressWarnings("unchecked")
    private void checkPackets() throws Exception {
        HideMyArmors plugin = (HideMyArmors) Bukkit.getPluginManager().getPlugin("HideMyArmors");
        EntityPacketAdapter adapter = new EntityPacketAdapter(plugin);
        var field = HideMyArmors.class.getDeclaredField("hiddenSlots");
        field.setAccessible(true);
        Map<Integer, Integer> masks = (Map<Integer, Integer>) field.get(plugin);
        Player viewer = (Player) Proxy.newProxyInstance(Player.class.getClassLoader(), new Class<?>[]{Player.class},
                (proxy, method, args) -> switch (method.getName()) {
                    case "getEntityId" -> 111;
                    case "hashCode" -> 111;
                    case "equals" -> proxy == args[0];
                    case "toString", "getName" -> "CompatibilityViewer";
                    default -> throw new AssertionError("Unexpected player access from packet thread: " + method);
                });
        ItemStack item = new ItemStack(Material.LEATHER_HELMET);
        LeatherArmorMeta meta = (LeatherArmorMeta) item.getItemMeta();
        meta.setDisplayName("Private name");
        meta.setLore(List.of("Private lore"));
        meta.setColor(Color.RED);
        meta.addEnchant(Enchantment.UNBREAKING, 3, true);
        meta.setCustomModelData(123);
        boolean modernModels = ProtocolLibrary.getProtocolManager().getMinecraftVersion().isAtLeast(
                new com.comphenix.protocol.utility.MinecraftVersion("1.21.5"));
        if (modernModels) {
            var component = meta.getCustomModelDataComponent();
            component.setFloats(List.of(123F, 7F));
            component.setStrings(List.of("test-model"));
            component.setFlags(List.of(true));
            component.setColors(List.of(Color.BLUE));
            meta.setCustomModelDataComponent(component);
        }
        item.setItemMeta(meta);
        PacketContainer source = ProtocolLibrary.getProtocolManager().createPacket(PacketType.Play.Server.ENTITY_EQUIPMENT);
        source.getIntegers().write(0, 222);
        source.getSlotStackPairLists().write(0, List.of(new Pair<>(EnumWrappers.ItemSlot.HEAD, item),
                new Pair<>(EnumWrappers.ItemSlot.MAINHAND, item), new Pair<>(EnumWrappers.ItemSlot.OFFHAND, item)));

        masks.put(222, 63);
        PacketEvent hidden = PacketEvent.fromServer(this, source, viewer);
        adapter.onPacketSending(hidden);
        require(hidden.getPacket() != source, "shared packet mutated");
        require(hidden.getPacket().getSlotStackPairLists().read(0).get(0).getSecond().getType().isAir(), "head not hidden");
        require(item.equals(hidden.getPacket().getSlotStackPairLists().read(0).get(1).getSecond()), "main hand changed while hiding armor");
        require(item.equals(hidden.getPacket().getSlotStackPairLists().read(0).get(2).getSecond()), "off hand changed while hiding armor");
        require(source.getSlotStackPairLists().read(0).get(0).getSecond().getType() == Material.LEATHER_HELMET, "shared equipment mutated");

        masks.put(222, 0);
        plugin.eraseEquipmentsInfo = true;
        PacketEvent erased = PacketEvent.fromServer(this, source, viewer);
        adapter.onPacketSending(erased);
        var erasedMeta = (LeatherArmorMeta) erased.getPacket().getSlotStackPairLists().read(0).get(0).getSecond().getItemMeta();
        require(!erasedMeta.hasLore() && !erasedMeta.hasDisplayName() && !erasedMeta.hasEnchants(), "private metadata leaked");
        require(erasedMeta.getColor().equals(Color.RED), "leather color lost");
        require(Boolean.TRUE.equals(erasedMeta.getEnchantmentGlintOverride()), "glint lost");
        require(modernModels ? meta.getCustomModelDataComponent().equals(erasedMeta.getCustomModelDataComponent())
                : meta.getCustomModelData() == erasedMeta.getCustomModelData(), "model data lost");
        require(item.getItemMeta().hasLore(), "real inventory metadata mutated");
        require(item.equals(erased.getPacket().getSlotStackPairLists().read(0).get(1).getSecond()), "main hand metadata erased");
        require(item.equals(erased.getPacket().getSlotStackPairLists().read(0).get(2).getSecond()), "off hand metadata erased");
        var modifyItem = EntityPacketAdapter.class.getDeclaredMethod("modifyItem", int.class, int.class, ItemStack.class);
        modifyItem.setAccessible(true);
        require(modifyItem.invoke(adapter, 63, 0, item) == null, "legacy main hand changed");
        require(modifyItem.invoke(adapter, 63, 5, item) == null, "legacy off hand changed");

        meta.setEnchantmentGlintOverride(false);
        item.setItemMeta(meta);
        source.getSlotStackPairLists().write(0, List.of(new Pair<>(EnumWrappers.ItemSlot.HEAD, item)));
        PacketEvent noGlint = PacketEvent.fromServer(this, source, viewer);
        adapter.onPacketSending(noGlint);
        require(Boolean.FALSE.equals(noGlint.getPacket().getSlotStackPairLists().read(0)
                .get(0).getSecond().getItemMeta().getEnchantmentGlintOverride()), "disabled glint lost");

        ItemStack skull = new ItemStack(Material.PLAYER_HEAD);
        SkullMeta skullMeta = (SkullMeta) skull.getItemMeta();
        skullMeta.setOwnerProfile(Bukkit.createPlayerProfile(java.util.UUID.randomUUID(), "ProbeSkull"));
        skullMeta.setLore(List.of("Private skull lore"));
        skull.setItemMeta(skullMeta);
        source.getSlotStackPairLists().write(0, List.of(new Pair<>(EnumWrappers.ItemSlot.HEAD, skull)));
        PacketEvent erasedSkull = PacketEvent.fromServer(this, source, viewer);
        adapter.onPacketSending(erasedSkull);
        SkullMeta resultSkull = (SkullMeta) erasedSkull.getPacket().getSlotStackPairLists().read(0).get(0).getSecond().getItemMeta();
        require(!resultSkull.hasLore() && resultSkull.getOwnerProfile() != null, "skull metadata leaked or profile lost");

        Class<?> nbt = Class.forName("top.mrxiaom.hidemyarmors.nbtapi.NBT", true, plugin.getClass().getClassLoader());
        nbt.getMethod("modify", ItemStack.class, java.util.function.Consumer.class).invoke(null, item,
                (java.util.function.Consumer<Object>) tag -> {
                    try {
                        tag.getClass().getMethod("setString", String.class, String.class).invoke(tag, "magic_cosmetic", "test");
                    } catch (ReflectiveOperationException error) {
                        throw new RuntimeException(error);
                    }
                });
        source.getSlotStackPairLists().write(0, List.of(new Pair<>(EnumWrappers.ItemSlot.HEAD, item)));
        PacketEvent cosmetic = PacketEvent.fromServer(this, source, viewer);
        adapter.onPacketSending(cosmetic);
        require(cosmetic.getPacket().getSlotStackPairLists().read(0).get(0).getSecond().getItemMeta().hasLore(), "cosmetic marker ignored");

        source.getIntegers().write(0, 111);
        masks.put(111, 63);
        PacketEvent self = PacketEvent.fromServer(this, source, viewer);
        adapter.onPacketSending(self);
        require(self.getPacket() == source, "own equipment modified");
        source.getIntegers().write(0, 333);
        PacketEvent unknown = PacketEvent.fromServer(this, source, viewer);
        adapter.onPacketSending(unknown);
        require(unknown.getPacket() == source, "non-player equipment modified");
        masks.clear();
        plugin.eraseEquipmentsInfo = false;
    }

    private static void require(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
