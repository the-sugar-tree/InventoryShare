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

import com.sugar_tree.inventoryshare.utils.I18NUtil;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.Plugin;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CommandsTest {

    private static final String DENIED = ChatColor.RED + "I'm sorry, but you do not have permission to perform this command. Please contact the server administrators if you believe that this is in error.";

    @TempDir
    static File dataFolder;

    private final PluginSettings settings = new PluginSettings(null);
    private final List<String> messages = new ArrayList<>();
    private final Command command = new Command("inventoryshare") {
        @Override
        public boolean execute(CommandSender sender, String label, String[] args) {
            return false;
        }
    };

    /** Commands builds its usage message from the language file, so load the one in the jar */
    @BeforeAll
    static void setUpLanguage() {
        Logger logger = Logger.getLogger("CommandsTest");
        logger.setUseParentHandlers(false);
        SharedConstants.logger = logger;
        YamlConfiguration config = new YamlConfiguration();
        config.set("language", "en_us");
        SharedConstants.plugin = (Plugin) Proxy.newProxyInstance(Plugin.class.getClassLoader(), new Class<?>[]{Plugin.class},
                (proxy, method, args) -> {
                    switch (method.getName()) {
                        case "getConfig": return config;
                        // has no languages folder, so the language file in the jar is used
                        case "getDataFolder": return dataFolder;
                        default: throw new UnsupportedOperationException(method.getName());
                    }
                });
        I18NUtil.init();
    }

    @AfterAll
    static void tearDown() {
        SharedConstants.plugin = null;
    }

    @Test
    void commandWithoutPermissionIsDenied() {
        Commands commands = new Commands(settings, null);
        boolean inventory = settings.isInventory();

        assertTrue(commands.onCommand(sender(false), command, "inventoryshare", new String[]{"inventory", String.valueOf(!inventory)}));
        assertEquals(Collections.singletonList(DENIED), messages);
        assertEquals(inventory, settings.isInventory(), "the setting must not change");
    }

    @Test
    void commandWithPermissionRuns() {
        Commands commands = new Commands(settings, null);

        assertTrue(commands.onCommand(sender(true), command, "inventoryshare", new String[]{"check"}));
        assertEquals(1, messages.size());
        assertFalse(messages.contains(DENIED));
    }

    private CommandSender sender(boolean permitted) {
        return (CommandSender) Proxy.newProxyInstance(CommandSender.class.getClassLoader(), new Class<?>[]{CommandSender.class},
                (proxy, method, args) -> {
                    switch (method.getName()) {
                        case "hasPermission":
                            return permitted && "inventoryshare.command".equals(args[0]);
                        case "sendMessage":
                            messages.add((String) args[0]);
                            return null;
                        default: throw new UnsupportedOperationException(method.getName());
                    }
                });
    }
}
