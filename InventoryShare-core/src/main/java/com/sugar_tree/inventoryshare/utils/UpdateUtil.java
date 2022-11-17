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
package com.sugar_tree.inventoryshare.utils;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

import static com.sugar_tree.inventoryshare.api.SharedConstants.logger;
import static com.sugar_tree.inventoryshare.api.SharedConstants.plugin;

public class UpdateUtil {
    public static String version;
    public static void checkUpdate() {
        try {
            URL url = new URL("https://github.com/the-sugar-tree/InventoryShare/releases/latest");
            String line;
            HttpURLConnection httpURLConnection = ((HttpURLConnection) url.openConnection());
            httpURLConnection.setRequestMethod("GET");
            BufferedReader br = new BufferedReader(new InputStreamReader(httpURLConnection.getInputStream(), StandardCharsets.UTF_8));
            while ((line = br.readLine()) != null) {
                if (line.contains("<a aria-current=\"page\" href=\"/the-sugar-tree/InventoryShare/releases/tag/")) {
                    version = line.substring(line.lastIndexOf(" ") + 1);
                    break;
                }
            }
            br.close();
        } catch (Exception e) {
            e.printStackTrace();
        }

        if (!plugin.getDescription().getVersion().equals(version)) {
            logger.warning("플러그인 업데이트가 가능합니다! 현재 버전: v" + plugin.getDescription().getVersion() + " 새 버전: " + version);
            logger.warning("https://github.com/the-sugar-tree/InventoryShare/releases/latest");
        }
    }
}
