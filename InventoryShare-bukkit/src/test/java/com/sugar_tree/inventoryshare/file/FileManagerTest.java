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

package com.sugar_tree.inventoryshare.file;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FileManagerTest {

    private static final Set<String> TEAMS = new HashSet<>(Arrays.asList("red", "blue.team"));

    @TempDir
    Path temp;

    @Test
    void fileOfExistingTeamIsKept() {
        assertFalse(FileManager.isWasteTeamFile("red.yml", TEAMS));
        assertFalse(FileManager.isWasteTeamFile("blue.team.yml", TEAMS));
    }

    @Test
    void fileOfRemovedTeamIsWaste() {
        assertTrue(FileManager.isWasteTeamFile("green.yml", TEAMS));
        assertTrue(FileManager.isWasteTeamFile("red.yml", Collections.emptySet()));
    }

    @Test
    void teamNameIsCaseSensitive() {
        assertTrue(FileManager.isWasteTeamFile("Red.yml", TEAMS));
    }

    @Test
    void nonYamlFilesAreNeverWaste() {
        assertFalse(FileManager.isWasteTeamFile("green.txt", TEAMS));
        assertFalse(FileManager.isWasteTeamFile("red", TEAMS));
    }

    @Test
    void advancementKeyWithoutNamespaceIsMinecraft() {
        assertEquals("minecraft:story/root", FileManager.fullAdvancementKey("story/root"));
    }

    @Test
    void advancementKeyWithNamespaceIsKept() {
        assertEquals("minecraft:story/root", FileManager.fullAdvancementKey("minecraft:story/root"));
        assertEquals("mypack:foo/bar", FileManager.fullAdvancementKey("mypack:foo/bar"));
    }

    @Test
    void copiesSourceWithDataVersionInName() throws IOException {
        File source = write("inventory.yml", "items: []\n");
        File backup = FileManager.backUp(temp.resolve("backups").toFile(), source, "inventory", 4189);

        assertTrue(backup.getName().matches("inventory-\\d{8}-\\d{6}-dv4189\\.yml"), backup.getName());
        assertEquals("items: []\n", new String(Files.readAllBytes(backup.toPath()), StandardCharsets.UTF_8));
        assertTrue(source.exists(), "the source must stay in place");
    }

    @Test
    void leavesDataVersionOutWhenUnknown() throws IOException {
        File source = write("red.yml", "items: []\n");
        File backup = FileManager.backUp(temp.resolve("backups").toFile(), source, "team-red", -1);

        assertTrue(backup.getName().matches("team-red-\\d{8}-\\d{6}\\.yml"), backup.getName());
    }

    @Test
    void missingSourceFails() {
        File missing = temp.resolve("missing.yml").toFile();
        assertThrows(IOException.class,
                () -> FileManager.backUp(temp.resolve("backups").toFile(), missing, "inventory", -1));
    }

    private File write(String name, String content) throws IOException {
        Path path = temp.resolve(name);
        Files.write(path, content.getBytes(StandardCharsets.UTF_8));
        return path.toFile();
    }
}
