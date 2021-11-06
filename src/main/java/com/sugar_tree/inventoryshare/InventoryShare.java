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

package com.sugar_tree.inventoryshare;

import net.kyori.adventure.text.Component;
import net.minecraft.world.entity.player.PlayerInventory;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.NamespacedKey;
import org.bukkit.advancement.Advancement;
import org.bukkit.advancement.AdvancementProgress;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.*;
import org.bukkit.event.world.WorldSaveEvent;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.util.*;

import static com.sugar_tree.inventoryshare.Advancement.AdvancementPatch;
import static com.sugar_tree.inventoryshare.Inventory.*;
public final class InventoryShare extends JavaPlugin implements Listener {

    public static boolean inventory = true;
    public static boolean advancement = true;
    public static boolean AnnounceDeath = false;

    public static final String PREFIX = ChatColor.LIGHT_PURPLE + "[" + ChatColor.AQUA + "InventoryShare" + ChatColor.LIGHT_PURPLE + "] " + ChatColor.RESET;

    private static File invfile;
    private static File advfile;
    public static FileConfiguration invconfig;
    public static FileConfiguration advconfig;

    public static List<NamespacedKey> advlist = new ArrayList<>();

    public static Map<UUID, PlayerInventory> invList = new HashMap<>();

    @SuppressWarnings("ConstantConditions")
    @Override
    public void onEnable() {
        invfile = new File(getDataFolder(), "inventory.yml");
        advfile = new File(getDataFolder(), "advancements.yml");
        invconfig = YamlConfiguration.loadConfiguration(invfile);
        advconfig = YamlConfiguration.loadConfiguration(advfile);
        saveDefaultConfigs();
        getCommand("inventoryshare").setExecutor(new Commands());
        getCommand("inventoryshare").setTabCompleter(new Commands());
        Bukkit.getPluginManager().registerEvents(new Listeners(), this);
        load();
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (inventory) invApply(player);
            getServer().getScheduler().runTaskLater(this, () -> AdvancementPatch(player), 1);
        }
        getServer().getConsoleSender().sendMessage(PREFIX + ChatColor.YELLOW + "\"인벤토리 공유 플러그인\" by. " + ChatColor.GREEN + "sugar_tree");
    }

    @Override
    public void onDisable() {
        for (UUID puuid : invList.keySet()) {
            Player p = (Player) getServer().getOfflinePlayer(puuid);
            invDisApply(p);
        }
        save();
    }


    private void saveDefaultConfigs() {
        saveDefaultConfig();
        if (!(invfile.exists())) {
            saveResource("inventory.yml", false);
        }
        if (!(advfile.exists())) {
            saveResource("advancements.yml", false);
        }
    }

    public static void saveConfigs() {
        plugin.saveConfig();
        try {
            invconfig.save(invfile);
            advconfig.save(advfile);
        }
        catch (Exception e) {
            e.printStackTrace();
        }
    }
}
