import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

public class SimpleTest {

    public static void main(String... args) throws IOException {
        long startTime = System.currentTimeMillis();

        File file = new File("InventoryShare-core/src/main/resources/languages/lang_ko_kr.yml");
        if (!file.exists()) {
            throw new FileNotFoundException("File " + file.getAbsolutePath() + " does not exist.");
        }

        FileConfiguration fileConfiguration = YamlConfiguration.loadConfiguration(new InputStreamReader(Files.newInputStream(file.toPath()), StandardCharsets.UTF_8));

        fileConfiguration.getKeys(false).forEach((s) -> System.out.println(s + " : \"" + fileConfiguration.get(s) + "\""));

        System.out.printf("%.4f%n", ((float) (System.currentTimeMillis() - startTime) / 1000));
    }
}