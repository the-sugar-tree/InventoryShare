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

package com.sugar_tree.inventoryshare.nms;

import org.junit.jupiter.api.Assumptions;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Properties;
import java.util.stream.Stream;

/**
 * Spigot runtime classpaths written by {@code InventoryShare-nms-test/build.gradle}.
 */
final class SpigotJars {

    private static Properties classpaths;

    private SpigotJars() {}

    /**
     * @return every Minecraft version listed in the build script, installed or not
     */
    static Stream<String> versions() {
        return Arrays.stream(System.getProperty("spigot.versions", "").split(","))
                .filter(version -> !version.isEmpty());
    }

    static Stream<String> versionsBelow(String version) {
        return versions().filter(mc -> ServerVersion.parse(mc).isOlderThan(version));
    }

    static Stream<String> versionsFrom(String version) {
        return versions().filter(mc -> ServerVersion.parse(mc).isAtLeast(version));
    }

    /**
     * Opens the Spigot classpath of the version in a loader isolated from the test classpath,
     * or skips the test if the jar is not installed.
     */
    static URLClassLoader open(String mc) throws IOException {
        String classpath = classpaths().getProperty(mc, "");
        Assumptions.assumeFalse(classpath.isEmpty(),
                "Spigot " + mc + " is not installed in mavenLocal (BuildTools --rev " + mc + ")");
        String[] paths = classpath.split(File.pathSeparator);
        URL[] urls = new URL[paths.length];
        for (int i = 0; i < paths.length; i++) {
            urls[i] = new File(paths[i]).toURI().toURL();
        }
        return new URLClassLoader(urls, ClassLoader.getPlatformClassLoader());
    }

    private static synchronized Properties classpaths() throws IOException {
        if (classpaths == null) {
            Properties properties = new Properties();
            try (InputStream in = Files.newInputStream(Paths.get(System.getProperty("spigot.classpaths")))) {
                properties.load(in);
            }
            classpaths = properties;
        }
        return classpaths;
    }
}
