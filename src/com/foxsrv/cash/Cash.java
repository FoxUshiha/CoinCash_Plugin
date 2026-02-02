package com.foxsrv.cash;

import com.foxsrv.cash.api.ApiClient;
import com.foxsrv.cash.commands.CoinCommand;
import com.foxsrv.cash.storage.UserStore;
import com.foxsrv.cash.util.ItemUtil;
import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.java.JavaPlugin;

public final class Cash extends JavaPlugin {

    private ApiClient apiClient;
    private UserStore userStore;

    @Override
    public void onEnable() {
        // Salvar configs padrão
        saveDefaultConfig();
        saveResource("users.yml", false);
        
        // Inicializar utilitário de itens
        ItemUtil.init(this);
        
        // Inicializar sistemas
        this.apiClient = new ApiClient(this);
        this.userStore = new UserStore(this);
        
        // Registrar comandos
        registerCommands();
        
        getLogger().info("CoinCash ativado com sucesso!");
        getLogger().info("Modo compatibilidade com mods: " + getConfig().getBoolean("mod_compatibility", true));
    }

    @Override
    public void onDisable() {
        getLogger().info("CoinCash desativado.");
    }

    private void registerCommands() {
        PluginCommand coinCmd = getCommand("coin");
        if (coinCmd != null) {
            CoinCommand executor = new CoinCommand(this, apiClient, userStore);
            coinCmd.setExecutor(executor);
            coinCmd.setTabCompleter(executor);
        } else {
            getLogger().severe("Comando /coin não encontrado no plugin.yml!");
        }
    }

    public ApiClient getApiClient() {
        return apiClient;
    }

    public UserStore getUserStore() {
        return userStore;
    }
}