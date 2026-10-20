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

package com.sugar_tree.inventoryshare.utils;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class UpdateUtilTest {

    @ParameterizedTest
    @CsvSource({
            "v3.1.1, 4.0-beta, false",
            "v4.0, 4.0-beta, true",
            "v4.0, 4.0, false",
            "v4.0.1, 4.0, true",
            "v4.1, 4.0.9, true",
            "v4.0-rc1, 4.0, false",
            "v4.0, 4.0.0, false",
            "v10.0, 9.9, true",
            "latest, 4.0, false",
    })
    void comparesVersions(String latest, String current, boolean newer) {
        assertEquals(newer, UpdateUtil.isNewer(latest, current));
    }

    @Test
    void readsTagNameFromTheResponse() {
        String json = "{\"url\":\"https://api.github.com/x\",\"tag_name\": \"v3.1.1\",\"name\":\"3.1.1\"}";
        assertEquals("v3.1.1", UpdateUtil.parseTagName(json));
    }

    @Test
    void responseWithoutTagName() {
        assertNull(UpdateUtil.parseTagName("{\"message\":\"Not Found\"}"));
    }
}
