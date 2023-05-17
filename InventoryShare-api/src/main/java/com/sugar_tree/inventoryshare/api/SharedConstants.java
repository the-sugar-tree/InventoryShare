/*
 * Copyright (c) 2021 the-sugar-tree
 *
 *  Licensed under the General Public License, Version 3.0 (the "License");
 *  you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * https://opensource.org/licenses/lgpl-3.0.html
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.sugar_tree.inventoryshare.api;

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
