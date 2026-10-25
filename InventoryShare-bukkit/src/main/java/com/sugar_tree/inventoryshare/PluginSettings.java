/*
 * This file is part of InventoryShare, licensed under the GPL-3.0 License.
 *
 * InventoryShare the minecraft plugin that enables sharing inventory with other players
 * Copyright (c) 2023 the-sugar-tree and contributors
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */

package com.sugar_tree.inventoryshare;

import lombok.Getter;
import lombok.Setter;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.plugin.Plugin;

/**
 * The settings in {@code config.yml} that the commands can change. This is the only class that reads or writes them.
 */
public final class PluginSettings {

    private final Plugin plugin;

    /**
     * -- GETTER --
     * Whether players share an inventory
     */
    @Setter
    @Getter
    private boolean inventory = true;
    /**
     * -- GETTER --
     * Whether players share advancements
     */
    @Setter
    @Getter
    private boolean advancement = true;
    /**
     * -- GETTER --
     * Whether a player's death location is broadcast
     */
    @Setter
    @Getter
    private boolean announcedeath = false;
    /**
     * -- GETTER --
     * Whether the players of a team share the team's inventory instead of the one shared by all players
     */
    @Setter
    @Getter
    private boolean teaminventory = false;

    public PluginSettings(Plugin plugin) {
        this.plugin = plugin;
    }

    /** Reads the settings from the loaded {@code config.yml} */
    public void load() {
        read(plugin.getConfig());
    }

    /** Reads {@code config.yml} from the disk again, then the settings from it */
    public void reload() {
        plugin.reloadConfig();
        load();
    }

    /** Writes the settings into {@code config.yml} and saves it */
    public void save() {
        write(plugin.getConfig());
        plugin.saveConfig();
    }

    /** A setting missing from {@code config} keeps its current value */
    void read(ConfigurationSection config) {
        if (config.contains("inventory")) inventory = config.getBoolean("inventory");
        if (config.contains("advancement")) advancement = config.getBoolean("advancement");
        if (config.contains("announcedeath")) announcedeath = config.getBoolean("announcedeath");
        if (config.contains("teaminventory")) teaminventory = config.getBoolean("teaminventory");
    }

    void write(ConfigurationSection config) {
        config.set("inventory", inventory);
        config.set("advancement", advancement);
        config.set("announcedeath", announcedeath);
        config.set("teaminventory", teaminventory);
    }

}
