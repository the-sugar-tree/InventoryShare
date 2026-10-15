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

package com.sugar_tree.inventoryshare.storage;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Copies of inventory files that could not be fully loaded. Backups are never deleted automatically.
 */
final class InventoryBackup {

    private InventoryBackup() {}

    /**
     * Copies {@code source} to {@code <directory>/<name>-<yyyyMMdd-HHmmss>[-dv<dataVersion>].yml}.
     *
     * @param name        e.g. {@code inventory} or {@code team-red}
     * @param dataVersion data version of the newest skipped item, or {@code -1} to leave it out of the name
     * @return the backup file
     */
    static File create(File directory, File source, String name, int dataVersion) throws IOException {
        String timestamp = new SimpleDateFormat("yyyyMMdd-HHmmss").format(new Date());
        File target = new File(directory, name + "-" + timestamp + (dataVersion >= 0 ? "-dv" + dataVersion : "") + ".yml");
        Files.createDirectories(directory.toPath());
        Files.copy(source.toPath(), target.toPath());
        return target;
    }
}
