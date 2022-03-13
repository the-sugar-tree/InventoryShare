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

import com.google.common.collect.ImmutableList;
import net.minecraft.core.NonNullList;
import net.minecraft.world.entity.player.PlayerInventory;
import net.minecraft.world.item.ItemStack;
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
    public static Inventory InventoryClass;
    public static fileManager fileManagerClass;
    public static File invfile;
    public static File advfile;

    public static NonNullList<ItemStack> items = NonNullList.a(36, ItemStack.b);
    public static NonNullList<ItemStack> armor = NonNullList.a(4, ItemStack.b);
    public static NonNullList<ItemStack> extraSlots = NonNullList.a(1, ItemStack.b);
    public static List<NonNullList<ItemStack>> contents = ImmutableList.of(items, armor, extraSlots);
    public static Map<String, Map<String, NonNullList<ItemStack>>> InventoryList = new HashMap<>();

    public static void saveConfigs(Plugin p) {
        fileManagerClass.deleteWasteFiles();
        p.saveConfig();
        try { invconfig.save(invfile); } catch (Exception e) { e.printStackTrace(); }
        try { advconfig.save(advfile); } catch (Exception e) { e.printStackTrace(); }
        for (FileConfiguration fileConfiguration : teamInvFileList.keySet()) {
            try { fileConfiguration.save(teamInvFileList.get(fileConfiguration)); } catch (Exception e) { e.printStackTrace(); }
        }
    }
}
