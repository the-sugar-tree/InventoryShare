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

import lombok.extern.slf4j.Slf4j;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

@Slf4j
@SuppressWarnings("ALL")
public class SimpleTest {

    public static void main(String... args) throws IOException {
        long startTime = System.currentTimeMillis();
//        GradleUpdater.work("8.3");

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

        Map<UUID, String> map = new HashMap<>();
        final UUID u1 = UUID.randomUUID();
        final UUID u2 = UUID.randomUUID();
        final UUID u3 = UUID.randomUUID();
        final UUID u4 = UUID.randomUUID();
        map.put(u1, "a");
        map.put(u2, "b");
        map.put(u3, "c");
        map.put(u4, null);
        log.info(Boolean.toString(map.containsKey(u4)));
        log.info(Boolean.toString(map.get(u4) == null));
        log.info("test");
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


    private static class GradleUpdater {
        private static void work(String gradleVersion) throws IOException {
//            String gradleVersion = "8.3";
            String fileName = "gradle-wrapper.properties";
            String exactline = "distributionUrl=https\\\\://services.gradle.org/distributions/gradle-" + gradleVersion + "-bin.zip";
            String regex = "distributionUrl=https\\\\:\\/\\/services\\.gradle\\.org\\/distributions\\/gradle-(([0-9]\\.)?[0-9]\\.[0-9])-bin\\.zip";
            for (File dirs1 : new File("C:\\Users\\sugar_tree\\IdeaProjects").listFiles(File::isDirectory)) {
                for (File dirs2 : dirs1.listFiles(File::isDirectory)) {
                    for (File dirs3 : dirs2.listFiles((dir, name) -> {
                        return name.equals("gradle");
                    })) {
                        for (File dirs4 : dirs3.listFiles((dir, name) -> {
                            return name.equals("wrapper");
                        })) {
                            for (File file : dirs4.listFiles((dir, name) -> {
                                return name.equals(fileName);
                            })) {
                                log.info("found: " + file.getAbsolutePath());

                                BufferedReader reader = new BufferedReader(new FileReader(file));
                                StringBuilder sb = new StringBuilder();
                                String line;
                                while ((line = reader.readLine()) != null) {
                                    sb.append(line).append("\n");
                                }
                                String str = sb.toString();
                                if (!str.contains(exactline.replaceAll("\\\\\\\\", "\\\\"))) {
                                    log.info("deleted and replaced");
                                    File dotGradle = new File(dirs2, ".gradle");
                                    if (dotGradle.exists()) {
                                        delete(dotGradle);
                                    }
                                    Files.writeString(file.toPath(), str.replaceAll(regex, exactline));
                                }
                            }
                        }
                    }
                }
            }
        }

        private static void delete(File file) throws IOException {
            if (file.exists()) {
                if (file.listFiles() == null) {
                    Files.delete(file.toPath());
                } else {
                    for (File child : file.listFiles()) {
                        delete(child);
                    }
                }
                Files.delete(file.toPath());
            }
        }
    }
}