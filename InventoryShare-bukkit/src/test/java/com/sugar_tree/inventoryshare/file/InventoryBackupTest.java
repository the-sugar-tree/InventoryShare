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

package com.sugar_tree.inventoryshare.file;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class InventoryBackupTest {

    @TempDir
    Path temp;

    @Test
    void copiesSourceWithDataVersionInName() throws IOException {
        File source = write("inventory.yml", "items: []\n");
        File backup = InventoryBackup.create(temp.resolve("backups").toFile(), source, "inventory", 4189);

        assertTrue(backup.getName().matches("inventory-\\d{8}-\\d{6}-dv4189\\.yml"), backup.getName());
        assertEquals("items: []\n", new String(Files.readAllBytes(backup.toPath()), StandardCharsets.UTF_8));
        assertTrue(source.exists(), "the source must stay in place");
    }

    @Test
    void leavesDataVersionOutWhenUnknown() throws IOException {
        File source = write("red.yml", "items: []\n");
        File backup = InventoryBackup.create(temp.resolve("backups").toFile(), source, "team-red", -1);

        assertTrue(backup.getName().matches("team-red-\\d{8}-\\d{6}\\.yml"), backup.getName());
    }

    @Test
    void missingSourceFails() {
        File missing = temp.resolve("missing.yml").toFile();
        assertThrows(IOException.class,
                () -> InventoryBackup.create(temp.resolve("backups").toFile(), missing, "inventory", -1));
    }

    private File write(String name, String content) throws IOException {
        Path path = temp.resolve(name);
        Files.write(path, content.getBytes(StandardCharsets.UTF_8));
        return path.toFile();
    }
}
