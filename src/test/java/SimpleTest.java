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
public class SimpleTest {

    public static void main(String... args) throws IOException, InterruptedException {
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

        new Task().start1().join();
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