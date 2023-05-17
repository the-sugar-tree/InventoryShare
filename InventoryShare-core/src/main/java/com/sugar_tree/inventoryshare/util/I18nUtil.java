package com.sugar_tree.inventoryshare.util;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.util.MissingResourceException;

import static com.sugar_tree.inventoryshare.api.SharedConstants.PREFIX;
import static com.sugar_tree.inventoryshare.api.SharedConstants.plugin;

public class I18nUtil {

    private final static Bundle defaultBundle;
    private final static Bundle bundle;
    static {
        String defaultLanguage = "ko_kr";
        defaultBundle = Bundle.getBundle(defaultLanguage);
        String language = plugin.getConfig().getString("language");
        if (language == null) {
            bundle = Bundle.getBundle(defaultLanguage);
        } else {
            bundle = Bundle.getBundle(language);
        }
    }

    public static String get(String key) {
        return get(key, false);
    }

    public static String get(String key, String... args) {
        return get(key, false, args);
    }

    public static String get(String key, boolean prefix) {
        String r;
        try {
            r = bundle.get(key);
        } catch (MissingResourceException e) {
            r = defaultBundle.get(key);
        }
        if (prefix) return PREFIX + r;
        else return r;
    }

    public static String get(String key, boolean prefix, String... args) {
        String s;
        try {
            s = bundle.get(key);
        } catch (MissingResourceException e) {
            s = defaultBundle.get(key);
        }
        if (prefix) return PREFIX + String.format(s, (Object[]) args);
        else return String.format(s, (Object[]) args);
    }

    private final static class Bundle {
        final FileConfiguration config;

        static Bundle getBundle(String locale) {
            return new Bundle(YamlConfiguration.loadConfiguration(new File(plugin.getDataFolder(), "/languages/lang_" + locale + ".yml")));
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
}
