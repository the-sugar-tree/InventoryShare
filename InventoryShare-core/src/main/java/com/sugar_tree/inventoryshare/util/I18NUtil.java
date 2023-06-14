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
package com.sugar_tree.inventoryshare.util;

import com.sugar_tree.inventoryshare.InventoryShare;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
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
        return get(key, false);
    }

    public static String get(String key, Object... args) {
        return get(key, false, args);
    }

    public static String get(String key, boolean prefix) {
        String s;
        s = bundle.get(key);
        if (prefix) return PREFIX + s;
        else return s;
    }

    public static String get(String key, boolean prefix, Object... args) {
        String s;
        s = bundle.get(key);
        if (prefix) return PREFIX + String.format(s, args);
        else return String.format(s, args);
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
                is = Objects.requireNonNull(InventoryShare.class.getResourceAsStream("/languages/lang_ko_kr.yml"), "Default language file (en_us) does not exist.");
            } else {
                is = Objects.requireNonNull(InventoryShare.class.getResourceAsStream("/languages/lang_en_us.yml"), "Default language file (ko_kr) does not exist.");
            }
            InputStreamReader isr = new InputStreamReader(is, StandardCharsets.UTF_8);
            return new Bundle(YamlConfiguration.loadConfiguration(isr));
        }

        private Bundle(FileConfiguration config) {
            this.config = config;
//            if (!config.getString("FILE_VERSION").equals(plugin.getDescription().getVersion())) {
                // TODO: 2023-05-17 Add informing need update
//            }
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
            FileConfiguration fileConfiguration = YamlConfiguration.loadConfiguration(new File(plugin.getDataFolder(), "\\languages\\lang_" + locale + ".yml"));
            String version = fileConfiguration.getString("FILE_VERSION");
            if (!version.equals(plugin.getDescription().getVersion())) {
                logger.warning("Updating Langauge File... [" + locale + "]");
                Set<String> changed = fileConfiguration.getKeys(true);
                InputStream is = Objects.requireNonNull(InventoryShare.class.getResourceAsStream("/languages/lang_" + locale + ".yml"), "Default language file ("+ locale +") does not exist.");
                InputStreamReader isr = new InputStreamReader(is, StandardCharsets.UTF_8);
                FileConfiguration defaultConfig = YamlConfiguration.loadConfiguration(isr);
                for (String str : defaultConfig.getKeys(true)) {
                    if (!changed.contains(str)) {
                        fileConfiguration.set(str, defaultConfig.get(str));
                    }
                }
            }
        }
    }
}
