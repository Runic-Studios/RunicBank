package com.runicrealms.plugin.util;

import com.runicrealms.plugin.RunicBank;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.UUID;

public class FileUtil {

    public static File getDataDirectory() {
        return new File(RunicBank.getInstance().getDataFolder(), "/data/");
    }

    public static File getPlayerFile(UUID uuid) {
        File file = new File(RunicBank.getInstance().getDataFolder(), "/data/" + uuid.toString() + ".yml");
        if (!file.exists()) {
            FileConfiguration fileConfig = YamlConfiguration.loadConfiguration(file);
            fileConfig.set("pages", 1); // banks have one page by default
            try {
                fileConfig.save(file);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return file;
    }
}
