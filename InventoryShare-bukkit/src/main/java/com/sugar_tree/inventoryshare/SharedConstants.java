/*
 * This file is part of InventoryShare, licensed under the GPL-3.0 License.
 *
 * InventoryShare the minecraft plugin that enables sharing inventory with ohter players
 * Copyright (c) 2023 the-sugar-tree
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */

package com.sugar_tree.inventoryshare;

import com.sugar_tree.inventoryshare.interfaces.IFileManager;
import com.sugar_tree.inventoryshare.interfaces.IInventoryManager;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.NamespacedKey;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.Plugin;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

@SuppressWarnings("deprecation")
public class SharedConstants {

    public static Logger logger;

    public static boolean inventory = true;
    public static boolean advancement = true;
    public static boolean announcedeath = false;
    public static boolean teaminventory = false;
    public static final String PREFIX = ChatColor.LIGHT_PURPLE + "[" + ChatColor.AQUA + "InventoryShare" + ChatColor.LIGHT_PURPLE + "] " + ChatColor.RESET;
    public static FileConfiguration invconfig;
    public static FileConfiguration advconfig;
    public static Map<FileConfiguration, File> teamInvFileList = new HashMap<>();
    public static List<NamespacedKey> advlist = new ArrayList<>();
    public static IInventoryManager InventoryManager;
    public static IFileManager FileManager;
    public static File invfile;
    public static File advfile;

    public static String I18N_TEAM_SAVED;
    public static String I18N_TEAM_LOADED;

    public static final int WORLD_VERSION;

    static {
        int t;
        try {
            t = Bukkit.getUnsafe().getDataVersion();
        } catch (NoSuchMethodError e) { t = -1; }
        WORLD_VERSION = t;
    }

    public static Plugin plugin;

    public static void saveConfigs() {
        FileManager.deleteWasteFiles();
        plugin.saveConfig();
        try { invconfig.save(invfile); } catch (Exception e) { e.printStackTrace(); }
        try { advconfig.save(advfile); } catch (Exception e) { e.printStackTrace(); }
        for (FileConfiguration fileConfiguration : teamInvFileList.keySet()) {
            try { fileConfiguration.save(teamInvFileList.get(fileConfiguration)); } catch (Exception e) { e.printStackTrace(); }
        }
    }
}
