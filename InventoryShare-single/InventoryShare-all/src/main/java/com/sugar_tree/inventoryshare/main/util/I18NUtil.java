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
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */

package com.sugar_tree.inventoryshare.main.util;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.NoSuchFileException;
import java.util.*;

import static com.sugar_tree.inventoryshare.api.SharedConstants.*;

public class I18NUtil {

    private static Bundle bundle;

    public static void reload() {
        init();
    }

    public static void init() {
        Bundle b = Bundle.getDefaultBundle();
        String language = plugin.getConfig().getString("language");
        try {
            b = Bundle.getBundle(language);
        } catch (NoSuchFileException e) {
            logger.severe(language == null ? "Could not load language info from config.yml" : "Could not find lang_" + language + ".yml");
            logger.severe("Attempt to use default language...");
        }
        if (language == null) {
            saveLanguageInfo();
        }
        bundle = b;
    }

    private static void saveLanguageInfo() {
        Locale systemLocale = Locale.getDefault();
        if (systemLocale.equals(Locale.KOREA) || systemLocale.equals(Locale.KOREAN)) {
            plugin.getConfig().set("language", "ko_kr");
        } else {
            plugin.getConfig().set("language", "en_us");
        }
    }

    public static String get(String key) {
        return get(false, key);
    }

    public static String get(String key, Object... args) {
        return get(false, key, args);
    }

    public static String get(boolean prefix, String key) {
        String s;
        s = bundle.get(key);
        if (prefix) return PREFIX + s + "§r";
        else return s + "§r";
    }

    public static String get(boolean prefix, String key, Object... args) {
        String s;
        s = bundle.get(key);
        if (prefix) return PREFIX + String.format(s + "§r", args);
        else return String.format(s + "§r", args);
    }

    private final static class Bundle {
        final FileConfiguration config;

        static Bundle getBundle(String locale) throws NoSuchFileException {
            File file = new File(plugin.getDataFolder(), "/languages/lang_" + locale + ".yml");
            if (!file.exists()) {
                throw new NoSuchFileException("There's no language files with name: " + locale + ".yml");
            }
            return new Bundle(YamlConfiguration.loadConfiguration(file));
        }

        static Bundle getDefaultBundle() {
            Locale systemLocale = Locale.getDefault();
            InputStream is;
            if (systemLocale.equals(Locale.KOREA) || systemLocale.equals(Locale.KOREAN)) {
                is = Objects.requireNonNull(I18NUtil.class.getResourceAsStream("/languages/lang_ko_kr.yml"), "Default language file (ko_kr) does not exist.");
            } else {
                is = Objects.requireNonNull(I18NUtil.class.getResourceAsStream("/languages/lang_en_us.yml"), "Default language file (en_us) does not exist.");
            }
            InputStreamReader isr = new InputStreamReader(is, StandardCharsets.UTF_8);
            return new Bundle(YamlConfiguration.loadConfiguration(isr));
        }

        private Bundle(FileConfiguration config) {
            this.config = config;
        }

        String get(String key) throws MissingResourceException {
            if (key == null) {
                throw new IllegalArgumentException("key value is null");
            }
            String value = config.getString(key);
            if (value == null) {
                throw new MissingResourceException("No such key in language file: " + key, getClass().getName(), key);
            }
            return value;
        }
    }

    public final static class I18NFileManager {
        public static void saveDefaultLanguageFiles() {
            Set<String> fileNames = new HashSet<>(Arrays.asList("ko_kr", "en_us"));
            for (String locale : fileNames) {
                File file = new File(plugin.getDataFolder(), "\\languages\\lang_" + locale + ".yml");
                if (!file.exists()) {
                    plugin.saveResource("languages/lang_" + locale + ".yml", false);
                } else {
                    updateFile(locale);
                }
            }
        }
        private static void updateFile(String locale) {
            File file = new File(plugin.getDataFolder(), "\\languages\\lang_" + locale + ".yml");
            FileConfiguration fileConfiguration = YamlConfiguration.loadConfiguration(file);
            String version = fileConfiguration.getString("FILE_VERSION");
            if (!version.equals(plugin.getDescription().getVersion())) {
                logger.warning("Updating Langauge File... [" + locale + "]");
                InputStream is = I18NUtil.class.getResourceAsStream("/languages/lang_" + locale + ".yml");
                if (is == null) {
                    logger.severe("Default language file ("+ locale +") does not exist.");
                    return;
                }
                InputStreamReader isr = new InputStreamReader(is, StandardCharsets.UTF_8);
                FileConfiguration defaultConfig = YamlConfiguration.loadConfiguration(isr);
                for (String key : defaultConfig.getKeys(true)) {
                    if (fileConfiguration.get(key) == null) {
                        fileConfiguration.set(key, defaultConfig.get(key));
                    }
                }
            }
            try {
                fileConfiguration.save(file);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
