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

import com.sugar_tree.inventoryshare.v1_17_R1.FileManager_1_17_R1;
import com.sugar_tree.inventoryshare.v1_17_R1.Inventory_1_17_R1;
import com.sugar_tree.inventoryshare.v1_18_R1.FileManager_1_18_R1;
import com.sugar_tree.inventoryshare.v1_18_R1.Inventory_1_18_R1;
import com.sugar_tree.inventoryshare.v1_18_R2.FileManager_1_18_R2;
import com.sugar_tree.inventoryshare.v1_18_R2.Inventory_1_18_R2;
import com.sugar_tree.inventoryshare.v1_19_R1.FileManager_1_19_R1;
import com.sugar_tree.inventoryshare.v1_19_R1.Inventory_1_19_R1;
import com.sugar_tree.inventoryshare.v1_19_R1_P0.FileManager_1_19_R1_P0;
import com.sugar_tree.inventoryshare.v1_19_R1_P0.Inventory_1_19_R1_P0;
import com.sugar_tree.inventoryshare.v1_19_R1_P1.FileManager_1_19_R1_P1;
import com.sugar_tree.inventoryshare.v1_19_R1_P1.Inventory_1_19_R1_P1;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.util.UUID;

import static com.sugar_tree.inventoryshare.Advancement.AdvancementPatch;
import static com.sugar_tree.inventoryshare.ProtocolLib.protocolLib;
import static com.sugar_tree.inventoryshare.api.Variables.*;

public final class InventoryShare extends JavaPlugin {
    private String minorVersion;
    private String patchVersion;
    private boolean isSupportedVersion = true;
    private boolean isSupportedBukkit = false;
    @SuppressWarnings("FieldCanBeLocal")
    private boolean isProtocolLib = false;

    private Listeners listener;

    @SuppressWarnings("ConstantConditions")
    @Override
    public void onEnable() {
        plugin = this;
        logger = getLogger();
        isSupportedVersion = checkVersion();
        isSupportedBukkit = checkBukkit();
        isProtocolLib = checkProtocolLib();
        if (!isSupportedVersion) {
            logger.severe("이 플러그인은 이 버전을 지원하지 않습니다: " + minorVersion);
            this.setEnabled(false);
            return;
        }
        if (!isSupportedBukkit) {
            this.getLogger().severe("이 플러그인은 Spigot, Paper 버킷만 지원합니다: " + Bukkit.getVersion());
            this.setEnabled(false);
            return;
        }
        if (isProtocolLib) {
            protocolLib(this);
        } else {
            getLogger().warning("이 플러그인의 모든 기능을 사용하시려면 ProtocolLib 플러그인이 필요합니다.");
        }
        switch (minorVersion) {
            case "v1_19_R1" -> {
                switch (patchVersion) {
                    case "1.19-R0.1-SNAPSHOT" -> {
                        InventoryClass = new Inventory_1_19_R1_P0(this);
                        FileManagerClass = new FileManager_1_19_R1_P0(this);
                    }
                    case "1.19.1-R0.1-SNAPSHOT" -> {
                        InventoryClass = new Inventory_1_19_R1_P1(this);
                        FileManagerClass = new FileManager_1_19_R1_P1(this);
                    }
                    default -> {
                        InventoryClass = new Inventory_1_19_R1(this);
                        FileManagerClass = new FileManager_1_19_R1(this);
                    }
                }
            }
            case "v1_18_R2" -> {
                InventoryClass = new Inventory_1_18_R2(this);
                FileManagerClass = new FileManager_1_18_R2(this);
            }
            case "v1_18_R1" -> {
                InventoryClass = new Inventory_1_18_R1(this);
                FileManagerClass = new FileManager_1_18_R1(this);
            }
            case "v1_17_R1" -> {
                InventoryClass = new Inventory_1_17_R1(this);
                FileManagerClass = new FileManager_1_17_R1(this);
            }
        }
        invfile = new File(getDataFolder(), "inventory.yml");
        advfile = new File(getDataFolder(), "advancements.yml");
        invconfig = YamlConfiguration.loadConfiguration(invfile);
        advconfig = YamlConfiguration.loadConfiguration(advfile);
        saveDefaultConfigs();
        getCommand("inventoryshare").setExecutor(new Commands());
        getCommand("inventoryshare").setTabCompleter(new Commands());
        listener = new Listeners();
        Bukkit.getPluginManager().registerEvents(listener, this);
        FileManagerClass.load();
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (inventory) InventoryClass.invApply(player);
            getServer().getScheduler().runTaskLater(this, () -> AdvancementPatch(player), 1);
        }
        getServer().getConsoleSender().sendMessage(PREFIX + ChatColor.YELLOW + "\"인벤토리 공유 플러그인\" by. " + ChatColor.GREEN + "sugar_tree");
    }

    @Override
    public void onDisable() {
        if (!isSupportedVersion) return;
        if (!isSupportedBukkit) return;
        for (UUID puuid : InventoryClass.getRegisteredPlayers()) {
            if (getServer().getOfflinePlayer(puuid).isOnline()) {
                Player p = (Player) getServer().getOfflinePlayer(puuid);
                InventoryClass.invDisApply(p);
            }
        }
        Bukkit.getScheduler().cancelTask(listener.getTaskId());
        FileManagerClass.save();
    }

    private boolean checkVersion() {
        minorVersion = "N/A";
        patchVersion = "N/A";
        try {
            minorVersion = Bukkit.getServer().getClass().getPackage().getName().split("\\.")[3];
        } catch (ArrayIndexOutOfBoundsException e) {
            return false;
        }
        patchVersion = Bukkit.getBukkitVersion();
        return minorVersion.equals("v1_19_R1") || minorVersion.equals("v1_18_R2") || minorVersion.equals("v1_18_R1") || minorVersion.equals("v1_17_R1");
    }
    private boolean checkBukkit() {
        return Bukkit.getVersion().contains("Paper") || Bukkit.getVersion().contains("Spigot");
    }
    private boolean checkProtocolLib() {
        return getServer().getPluginManager().getPlugin("ProtocolLib") != null;
    }

    @SuppressWarnings("ResultOfMethodCallIgnored")
    private void saveDefaultConfigs() {
        saveDefaultConfig();
        if (!(invfile.exists())) {
            saveResource("inventory.yml", false);
        }
        if (!(advfile.exists())) {
            saveResource("advancements.yml", false);
        }
        if (!(new File(getDataFolder(), "\\teams")).exists()) {
            (new File(getDataFolder(), "\\teams")).mkdir();
        }
    }
}
