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
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */

import lombok.extern.slf4j.Slf4j;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Locale;

@Slf4j
@SuppressWarnings("ALL")
public class SimpleTest {

    public static void main(String... args) throws IOException {
        long startTime = System.currentTimeMillis();

        /*
        Properties properties = System.getProperties();
        properties.setProperty("user.language", "ko");
        properties.setProperty("user.country", "kr");
        System.setProperties(properties);
        System.getProperties().forEach((o1, o2) -> {
            System.out.println(o1+" : "+o2);
        });
        */

//        checkLanguageConfig();
        log.info("test");
        String s = null;
        System.out.println(s.equals("sads"));
        log.info("{}s elapsed", ((float) (System.currentTimeMillis() - startTime) / 1000));
    }

    private static void checkLanguageConfig() throws IOException {
        File file = new File("InventoryShare-core/src/main/resources/languages/lang_" + Locale.getDefault().toString().toLowerCase() + ".yml");
        if (!file.exists()) {
            throw new FileNotFoundException("File " + file.getAbsolutePath() + " does not exist.");
        }
        FileConfiguration fileConfiguration = YamlConfiguration.loadConfiguration(new InputStreamReader(Files.newInputStream(file.toPath()), StandardCharsets.UTF_8));
        fileConfiguration.getKeys(false).forEach((s) -> System.out.println(s + " : \"" + fileConfiguration.get(s) + "\""));
    }

    private static class Task extends Thread {
        @Override
        public void run() {
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            log.info(Thread.currentThread().getName() + ": Hello");
        }

        public Task start1() {
            super.start();
            return this;
        }
    }
}