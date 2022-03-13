package com.sugar_tree.inventoryshare.api;

import net.minecraft.world.entity.player.PlayerInventory;
import org.bukkit.ChatColor;
import org.bukkit.NamespacedKey;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.Plugin;

import java.io.File;
import java.util.*;

public class variables {
    public static boolean inventory = true;
    public static boolean advancement = true;
    public static boolean AnnounceDeath = false;
    public static boolean teaminventory = false;
    public static final String PREFIX = ChatColor.LIGHT_PURPLE + "[" + ChatColor.AQUA + "InventoryShare" + ChatColor.LIGHT_PURPLE + "] " + ChatColor.RESET;
    public static FileConfiguration invconfig;
    public static FileConfiguration advconfig;
    public static Map<UUID, PlayerInventory> invList = new HashMap<>();
    public static Map<FileConfiguration, File> teamInvFileList = new HashMap<>();
    public static List<NamespacedKey> advlist = new ArrayList<>();
    public static Inventory Inventory;
    public static fileManager fileManager;
    public static File invfile;
    public static File advfile;

    public static void saveConfigs(Plugin p) {
        fileManager.deleteWasteFiles();
        p.saveConfig();
        try { invconfig.save(invfile); } catch (Exception e) { e.printStackTrace(); }
        try { advconfig.save(advfile); } catch (Exception e) { e.printStackTrace(); }
        for (FileConfiguration fileConfiguration : teamInvFileList.keySet()) {
            try { fileConfiguration.save(teamInvFileList.get(fileConfiguration)); } catch (Exception e) { e.printStackTrace(); }
        }
    }
}
