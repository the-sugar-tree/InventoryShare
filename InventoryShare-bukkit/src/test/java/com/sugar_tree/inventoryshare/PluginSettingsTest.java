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

import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PluginSettingsTest {

    @Test
    void missingSettingsKeepTheirValues() {
        PluginSettings settings = new PluginSettings(null);
        YamlConfiguration config = new YamlConfiguration();
        config.set("teaminventory", true);

        settings.read(config);

        assertTrue(settings.isInventory());
        assertTrue(settings.isAdvancement());
        assertFalse(settings.isAnnouncedeath());
        assertTrue(settings.isTeaminventory());
    }

    @Test
    void writtenSettingsAreReadBack() throws Exception {
        PluginSettings settings = new PluginSettings(null);
        settings.setInventory(false);
        settings.setAdvancement(false);
        settings.setAnnouncedeath(true);
        settings.setTeaminventory(true);
        YamlConfiguration config = new YamlConfiguration();
        settings.write(config);

        YamlConfiguration reloaded = new YamlConfiguration();
        reloaded.loadFromString(config.saveToString());
        PluginSettings read = new PluginSettings(null);
        read.read(reloaded);

        assertFalse(read.isInventory());
        assertFalse(read.isAdvancement());
        assertTrue(read.isAnnouncedeath());
        assertTrue(read.isTeaminventory());
        assertEquals(4, reloaded.getKeys(false).size());
    }
}
