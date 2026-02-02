package com.foxsrv.cash.storage;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.util.UUID;

public class UserStore {

    private final JavaPlugin plugin;
    private File file;
    private FileConfiguration cfg;

    public UserStore(JavaPlugin plugin) {
        this.plugin = plugin;
        load();
    }

    private void load() {
        file = new File(plugin.getDataFolder(), "users.yml");
        if (!file.exists()) {
            plugin.saveResource("users.yml", false);
        }
        cfg = YamlConfiguration.loadConfiguration(file);
    }

    // Método público para recarregar
    public void reload() {
        load();
    }

    public void setUserId(UUID uuid, String userId) {
        cfg.set("users." + uuid.toString(), userId);
        save();
    }

    public String getUserId(UUID uuid) {
        return cfg.getString("users." + uuid.toString());
    }

    public boolean hasUser(UUID uuid) {
        return cfg.contains("users." + uuid.toString());
    }

    private void save() {
        try {
            cfg.save(file);
        } catch (IOException e) {
            plugin.getLogger().severe("Erro ao salvar users.yml: " + e.getMessage());
        }
    }
}