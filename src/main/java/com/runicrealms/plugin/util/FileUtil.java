package com.runicrealms.plugin.util;

import com.runicrealms.plugin.RunicBank;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

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
            fileConfig.set("max_page_index", 0); // banks have one page by default
            try {
                fileConfig.save(file);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return file;
    }

    public static FileConfiguration getPlayerFileConfig(UUID uuid) {
        File playerFile = FileUtil.getPlayerFile(uuid);
        return YamlConfiguration.loadConfiguration(playerFile);
    }

    public static int getMaxPageIndex(Player pl) {
        FileConfiguration fileConfig = getPlayerFileConfig(pl.getUniqueId());
        return fileConfig.getInt("max_page_index");
    }

    public static void saveFile(FileConfiguration fileConfig, UUID uuid) {
        try {
            fileConfig.save(FileUtil.getPlayerFile(uuid));
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }
}
