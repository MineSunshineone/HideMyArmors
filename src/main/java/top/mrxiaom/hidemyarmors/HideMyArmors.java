package top.mrxiaom.hidemyarmors;

import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.ProtocolManager;
import de.tr7zw.changeme.nbtapi.utils.MinecraftVersion;
import org.bukkit.NamespacedKey;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class HideMyArmors extends JavaPlugin implements TabCompleter {
    ProtocolManager protocolManager;
    public boolean eraseEquipmentsInfo = false;
    protected boolean newVersion;
    protected boolean supportCMD;
    protected boolean twoHands;
    
    // PDC keys for each slot
    private NamespacedKey keyHead;
    private NamespacedKey keyChest;
    private NamespacedKey keyLegs;
    private NamespacedKey keyFeet;
    
    private static final List<String> MAIN_COMMANDS = Arrays.asList("hide", "show", "status", "reload");
    private static final List<String> SLOTS = Arrays.asList("head", "chest", "legs", "feet", "all");

    @Override
    public void onLoad() {
        MinecraftVersion.replaceLogger(getLogger());
        MinecraftVersion.disableUpdateCheck();
        MinecraftVersion.disableBStats();
        MinecraftVersion.getVersion();
    }

    @Override
    public void onEnable() {
        // Initialize PDC keys (不包括主手和副手)
        keyHead = new NamespacedKey(this, "hide_head");
        keyChest = new NamespacedKey(this, "hide_chest");
        keyLegs = new NamespacedKey(this, "hide_legs");
        keyFeet = new NamespacedKey(this, "hide_feet");
        
        protocolManager = ProtocolLibrary.getProtocolManager();
        protocolManager.addPacketListener(new EntityPacketAdapter(this));
        com.comphenix.protocol.utility.MinecraftVersion ver = protocolManager.getMinecraftVersion();
        try {
            newVersion = ver.isAtLeast(com.comphenix.protocol.utility.MinecraftVersion.NETHER_UPDATE);
        } catch (Throwable t) {
            newVersion = false;
        }
        try {
            supportCMD = ver.isAtLeast(com.comphenix.protocol.utility.MinecraftVersion.VILLAGE_UPDATE);
        } catch (Throwable t) {
            supportCMD = false;
        }
        try {
            twoHands = ver.isAtLeast(com.comphenix.protocol.utility.MinecraftVersion.COMBAT_UPDATE);
        } catch (Throwable t) {
            twoHands = false;
        }
        reloadConfig();
        
        // 注册命令补全
        getCommand("hidemyarmors").setTabCompleter(this);
        
        getLogger().info("HideMyArmors 插件已启用");
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command cmd, @NotNull String label, @NotNull String[] args) {
        if (args.length == 0) {
            sender.sendMessage("§c用法: /hidemyarmors <hide|show|status|reload> [slot|all]");
            sender.sendMessage("§c可用槽位: head, chest, legs, feet, all");
            return true;
        }
        
        String subCmd = args[0].toLowerCase();
        
        if (subCmd.equals("reload")) {
            if (sender.isOp()) {
                reloadConfig();
                sender.sendMessage("§a配置文件已重载");
            } else {
                sender.sendMessage("§c你没有权限执行此命令");
            }
            return true;
        }
        
        if (!(sender instanceof Player)) {
            sender.sendMessage("§c此命令只能由玩家执行");
            return true;
        }
        
        Player player = (Player) sender;
        
        if (subCmd.equals("hide")) {
            if (args.length < 2) {
                sender.sendMessage("§c用法: /hidemyarmors hide <slot|all>");
                sender.sendMessage("§c可用槽位: head, chest, legs, feet, all");
                return true;
            }
            
            String slot = args[1].toLowerCase();
            hideSlot(player, slot);
            return true;
        } else if (subCmd.equals("show")) {
            if (args.length < 2) {
                sender.sendMessage("§c用法: /hidemyarmors show <slot|all>");
                sender.sendMessage("§c可用槽位: head, chest, legs, feet, all");
                return true;
            }
            
            String slot = args[1].toLowerCase();
            showSlot(player, slot);
            return true;
        } else if (subCmd.equals("status")) {
            showStatus(player);
            return true;
        } else {
            sender.sendMessage("§c未知的子命令: " + subCmd);
            sender.sendMessage("§c可用命令: hide, show, status, reload");
        }
        
        return true;
    }
    
    @Override
    @Nullable
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String alias, @NotNull String[] args) {
        List<String> completions = new ArrayList<>();
        
        if (args.length == 1) {
            // 补全主命令
            return MAIN_COMMANDS.stream()
                    .filter(cmd -> cmd.startsWith(args[0].toLowerCase()))
                    .collect(Collectors.toList());
        } else if (args.length == 2) {
            String subCmd = args[0].toLowerCase();
            // 如果是 hide 或 show 命令，补全槽位
            if (subCmd.equals("hide") || subCmd.equals("show")) {
                return SLOTS.stream()
                        .filter(slot -> slot.startsWith(args[1].toLowerCase()))
                        .collect(Collectors.toList());
            }
        }
        
        return completions;
    }
    
    private void hideSlot(Player player, String slot) {
        switch (slot) {
            case "head":
                setPDC(player, keyHead, true);
                player.sendMessage("§a已隐藏头盔");
                player.sendMessage("§e提示: 需要退出重进才能生效");
                break;
            case "chest":
                setPDC(player, keyChest, true);
                player.sendMessage("§a已隐藏胸甲");
                player.sendMessage("§e提示: 需要退出重进才能生效");
                break;
            case "legs":
                setPDC(player, keyLegs, true);
                player.sendMessage("§a已隐藏护腿");
                player.sendMessage("§e提示: 需要退出重进才能生效");
                break;
            case "feet":
                setPDC(player, keyFeet, true);
                player.sendMessage("§a已隐藏靴子");
                player.sendMessage("§e提示: 需要退出重进才能生效");
                break;
            case "all":
                setPDC(player, keyHead, true);
                setPDC(player, keyChest, true);
                setPDC(player, keyLegs, true);
                setPDC(player, keyFeet, true);
                player.sendMessage("§a已隐藏所有装备");
                player.sendMessage("§e提示: 需要退出重进才能生效");
                break;
            default:
                player.sendMessage("§c未知的槽位: " + slot);
                player.sendMessage("§c可用槽位: head, chest, legs, feet, all");
                break;
        }
    }
    
    private void showSlot(Player player, String slot) {
        switch (slot) {
            case "head":
                setPDC(player, keyHead, false);
                player.sendMessage("§a已显示头盔");
                player.sendMessage("§e提示: 需要退出重进才能生效");
                break;
            case "chest":
                setPDC(player, keyChest, false);
                player.sendMessage("§a已显示胸甲");
                player.sendMessage("§e提示: 需要退出重进才能生效");
                break;
            case "legs":
                setPDC(player, keyLegs, false);
                player.sendMessage("§a已显示护腿");
                player.sendMessage("§e提示: 需要退出重进才能生效");
                break;
            case "feet":
                setPDC(player, keyFeet, false);
                player.sendMessage("§a已显示靴子");
                player.sendMessage("§e提示: 需要退出重进才能生效");
                break;
            case "all":
                setPDC(player, keyHead, false);
                setPDC(player, keyChest, false);
                setPDC(player, keyLegs, false);
                setPDC(player, keyFeet, false);
                player.sendMessage("§a已显示所有装备");
                player.sendMessage("§e提示: 需要退出重进才能生效");
                break;
            default:
                player.sendMessage("§c未知的槽位: " + slot);
                player.sendMessage("§c可用槽位: head, chest, legs, feet, all");
                break;
        }
    }
    
    private void setPDC(Player player, NamespacedKey key, boolean value) {
        if (value) {
            player.getPersistentDataContainer().set(key, PersistentDataType.BYTE, (byte) 1);
        } else {
            player.getPersistentDataContainer().remove(key);
        }
    }
    
    private void showStatus(Player player) {
        player.sendMessage("§6========== 装备隐藏状态 ==========");
        player.sendMessage("§e头盔: " + (isSlotHidden(player, keyHead) ? "§c隐藏" : "§a显示"));
        player.sendMessage("§e胸甲: " + (isSlotHidden(player, keyChest) ? "§c隐藏" : "§a显示"));
        player.sendMessage("§e护腿: " + (isSlotHidden(player, keyLegs) ? "§c隐藏" : "§a显示"));
        player.sendMessage("§e靴子: " + (isSlotHidden(player, keyFeet) ? "§c隐藏" : "§a显示"));
        player.sendMessage("§6==================================");
    }
    
    public boolean isSlotHidden(Player player, int slot) {
        NamespacedKey key = getKeyForSlot(slot);
        if (key == null) return false;
        return isSlotHidden(player, key);
    }
    
    private boolean isSlotHidden(Player player, NamespacedKey key) {
        return player.getPersistentDataContainer().has(key, PersistentDataType.BYTE);
    }
    
    private NamespacedKey getKeyForSlot(int slot) {
        switch (slot) {
            case 0: return null; // 主手不允许隐藏
            case 1: return keyHead;
            case 2: return keyChest;
            case 3: return keyLegs;
            case 4: return keyFeet;
            case 5: return null; // 副手不允许隐藏
            default: return null;
        }
    }

    @Override
    public void onDisable() {
        if (protocolManager != null) protocolManager.removePacketListeners(this);
    }

    @Override
    public void reloadConfig() {
        this.saveDefaultConfig();
        super.reloadConfig();

        FileConfiguration config = getConfig();
        eraseEquipmentsInfo = config.getBoolean("erase-entity-equipments-information", false);
    }
}
