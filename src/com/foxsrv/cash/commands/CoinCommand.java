package com.foxsrv.cash.commands;

import com.foxsrv.cash.Cash;
import com.foxsrv.cash.api.ApiClient;
import com.foxsrv.cash.storage.UserStore;
import com.foxsrv.cash.util.ItemUtil;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.*;

public class CoinCommand implements CommandExecutor, TabCompleter {

    private final Cash plugin;
    private final ApiClient api;
    private final UserStore store;
    private final Map<UUID, Long> cooldown = new HashMap<>();
    private final DecimalFormat amountFormat;

    public CoinCommand(Cash plugin, ApiClient api, UserStore store) {
        this.plugin = plugin;
        this.api = api;
        this.store = store;
        
        // Configurar formatação de números para usar ponto decimal
        DecimalFormatSymbols symbols = new DecimalFormatSymbols(Locale.US);
        symbols.setDecimalSeparator('.');
        amountFormat = new DecimalFormat("#.########", symbols);
        amountFormat.setMaximumFractionDigits(8);
    }

    private boolean inCooldown(Player p) {
        long now = System.currentTimeMillis();
        long last = cooldown.getOrDefault(p.getUniqueId(), 0L);
        long cd = plugin.getConfig().getLong("cooldown_ms", 1000);
        
        return (now - last) < cd;
    }

    private void setCooldown(Player p) {
        cooldown.put(p.getUniqueId(), System.currentTimeMillis());
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (!(sender instanceof Player p)) {
            sender.sendMessage("§cApenas jogadores podem usar este comando.");
            return true;
        }

        if (args.length == 0) {
            sendHelp(p);
            return true;
        }

        if (inCooldown(p)) {
            p.sendMessage("§cAguarde antes de usar novamente.");
            return true;
        }

        String subCommand = args[0].toLowerCase();
        
        switch (subCommand) {
            case "reload":
                handleReload(p);
                break;
                
            case "id":
                handleId(p, args);
                break;
                
            case "buy":
                handleBuy(p, args);
                break;
                
            case "sell":
                handleSell(p, args);
                break;
                
            case "help":
                sendDetailedHelp(p);
                break;
                
            case "info":
                handleInfo(p);
                break;
                
            default:
                p.sendMessage("§cComando desconhecido. Use §e/coin help §cpara ajuda.");
                break;
        }
        
        return true;
    }

    private void sendHelp(Player p) {
        p.sendMessage("§6§lCoinCash §7- Comandos disponíveis:");
        p.sendMessage("§e/coin id <card> §7- Salvar seu card para vendas");
        p.sendMessage("§e/coin buy <quantia> <card> §7- Comprar itens usando seu card");
        p.sendMessage("§e/coin sell <quantia> §7- Vender itens para seu card salvo");
        p.sendMessage("§e/coin info §7- Ver informações do sistema");
        p.sendMessage("§e/coin help §7- Ajuda detalhada");
        if (p.hasPermission("coincash.admin")) {
            p.sendMessage("§e/coin reload §7- Recarregar configuração (admin)");
        }
    }

    private void sendDetailedHelp(Player p) {
        p.sendMessage("§6§lCoinCash §7- Ajuda detalhada:");
        p.sendMessage("§e1. Primeiro, salve seu card:");
        p.sendMessage("§7   /coin id SEU_CARD_AQUI");
        p.sendMessage("§e2. Para comprar itens:");
        p.sendMessage("§7   /coin buy 10 SEU_CARD");
        p.sendMessage("§7   Transfere coins do seu card para o servidor");
        p.sendMessage("§e3. Para vender itens:");
        p.sendMessage("§7   /coin sell 5");
        p.sendMessage("§7   Transfere coins do servidor para seu card salvo");
        
        // Mostrar informações atuais
        handleInfo(p);
    }

    private void handleInfo(Player p) {
        // Obter configurações atuais
        double worth = plugin.getConfig().getDouble("Worth", 0.01);
        double sell = plugin.getConfig().getDouble("Sell", 0.008);
        String itemId = plugin.getConfig().getString("item", "minecraft:gold_ingot");
        int amount = plugin.getConfig().getInt("amount", 1);
        boolean modCompatibility = plugin.getConfig().getBoolean("mod_compatibility", true);
        
        String itemDisplayName = ItemUtil.getItemDisplayName(itemId);
        
        p.sendMessage("");
        p.sendMessage("§6§lInformações do Sistema:");
        p.sendMessage("§7Item configurado: §e" + itemDisplayName + " §7(" + itemId + ")");
        p.sendMessage("§7Multiplicador: §e" + amount + " §7itens por coin");
        p.sendMessage("§7Preço de compra: §e" + amountFormat.format(worth) + " §7coins por unidade");
        p.sendMessage("§7Preço de venda: §e" + amountFormat.format(sell) + " §7coins por unidade");
        p.sendMessage("§7Modo mods: §e" + (modCompatibility ? "ATIVADO" : "DESATIVADO"));
        
        // Mostrar card do jogador se tiver
        String userCard = store.getUserId(p.getUniqueId());
        if (userCard != null && !userCard.isEmpty()) {
            p.sendMessage("§7Seu card salvo: §e" + userCard);
        } else {
            p.sendMessage("§7Seu card: §cNÃO SALVO (use /coin id)");
        }
    }

    private void handleReload(Player p) {
        if (!p.hasPermission("coincash.admin")) {
            p.sendMessage("§cSem permissão.");
            return;
        }
        plugin.reloadConfig();
        p.sendMessage("§aConfiguração recarregada com sucesso!");
        handleInfo(p); // Mostrar novas configurações
    }

    private void handleId(Player p, String[] args) {
        if (args.length < 2) {
            p.sendMessage("§eUso: §f/coin id <seu-card>");
            p.sendMessage("§7Exemplo: §f/coin id 0f9dad8bf12c");
            return;
        }
        
        String card = args[1].trim();
        if (card.length() < 6) {
            p.sendMessage("§cCard inválido. Deve ter pelo menos 6 caracteres.");
            return;
        }
        
        store.setUserId(p.getUniqueId(), card);
        p.sendMessage("§aCard §e" + card + " §asalvo com sucesso!");
        p.sendMessage("§7Agora você pode usar §f/coin sell §7para vender itens.");
        setCooldown(p);
    }

    private void handleBuy(Player p, String[] args) {
        if (args.length < 3) {
            p.sendMessage("§eUso: §f/coin buy <quantia> <card>");
            p.sendMessage("§7Exemplo: §f/coin buy 10 0f9dad8bf12c");
            return;
        }

        // Validar quantidade
        double qty;
        try {
            qty = Double.parseDouble(args[1]);
            if (qty <= 0) {
                p.sendMessage("§cQuantia deve ser maior que zero.");
                return;
            }
            if (qty > 10000) {
                p.sendMessage("§cQuantia máxima é 10000.");
                return;
            }
        } catch (NumberFormatException e) {
            p.sendMessage("§cQuantia inválida. Use números.");
            return;
        }

        // Obter configurações
        String itemId = plugin.getConfig().getString("item", "minecraft:gold_ingot");
        int itemMultiplier = plugin.getConfig().getInt("amount", 1);
        String serverCard = plugin.getConfig().getString("Card");
        double worth = plugin.getConfig().getDouble("Worth", 0.01);

        // Validações
        if (serverCard == null || serverCard.isEmpty()) {
            p.sendMessage("§cCard do servidor não configurado. Avise um administrador.");
            plugin.getLogger().warning("Card do servidor não configurado no config.yml!");
            return;
        }

        // Validar item ID (suporta mods)
        if (!ItemUtil.isValidItemId(itemId)) {
            p.sendMessage("§cItem '" + itemId + "' configurado é inválido.");
            plugin.getLogger().warning("Item inválido configurado: " + itemId);
            p.sendMessage("§7Use formato: §eminecraft:gold_ingot §7ou §emodid:item_name");
            return;
        }

        // Calcular custo (truncar para 8 casas decimais)
        double cost = qty * worth;
        cost = Math.floor(cost * 1e8) / 1e8;
        
        String userCard = args[2].trim();
        String itemDisplayName = ItemUtil.getItemDisplayName(itemId);
        
        plugin.getLogger().info(p.getName() + " tentando comprar: " + qty + " unidades de " + itemId + " por " + cost + " coins");
        
        // Confirmar transação
        p.sendMessage("§6§lConfirmar Compra:");
        p.sendMessage("§7Item: §e" + itemDisplayName);
        p.sendMessage("§7Quantidade: §e" + qty + " §7unidades");
        p.sendMessage("§7Total itens: §e" + (int)(qty * itemMultiplier));
        p.sendMessage("§7Custo total: §e" + amountFormat.format(cost) + " §7coins");
        p.sendMessage("");
        p.sendMessage("§eDigite §f/coin buy " + args[1] + " " + args[2] + " §enovamente para confirmar.");
        
        // Verificar se é uma confirmação (mesmos parâmetros duas vezes seguidas)
        if (isConfirmation(p, "buy", args)) {
            // Executar compra via API
            if (api.buy(userCard, serverCard, cost)) {
                int itemsToGive = (int) (qty * itemMultiplier);
                ItemUtil.give(p, itemId, itemsToGive);
                p.sendMessage("§a✓ Compra realizada com sucesso!");
                p.sendMessage("§7Custo: §e" + amountFormat.format(cost) + " coins");
                p.sendMessage("§7Itens recebidos: §e" + itemsToGive + " " + itemDisplayName);
                setCooldown(p);
            } else {
                p.sendMessage("§c✗ Falha na compra. Verifique:");
                p.sendMessage("§7- Seu card está correto?");
                p.sendMessage("§7- Você tem saldo suficiente?");
                p.sendMessage("§7- O servidor está configurado corretamente?");
            }
        }
    }

    private void handleSell(Player p, String[] args) {
        if (args.length < 2) {
            p.sendMessage("§eUso: §f/coin sell <quantia>");
            p.sendMessage("§7Exemplo: §f/coin sell 5");
            return;
        }

        // Verificar se o jogador tem card salvo
        String userCard = store.getUserId(p.getUniqueId());
        if (userCard == null || userCard.isEmpty()) {
            p.sendMessage("§cVocê precisa salvar seu card primeiro!");
            p.sendMessage("§7Use: §f/coin id <seu-card>");
            return;
        }

        // Validar quantidade
        double qty;
        try {
            qty = Double.parseDouble(args[1]);
            if (qty <= 0) {
                p.sendMessage("§cQuantia deve ser maior que zero.");
                return;
            }
            if (qty > 10000) {
                p.sendMessage("§cQuantia máxima é 10000.");
                return;
            }
        } catch (NumberFormatException e) {
            p.sendMessage("§cQuantia inválida. Use números.");
            return;
        }

        // Obter configurações
        String itemId = plugin.getConfig().getString("item", "minecraft:gold_ingot");
        int itemMultiplier = plugin.getConfig().getInt("amount", 1);
        String serverCard = plugin.getConfig().getString("Card");
        double sellValue = plugin.getConfig().getDouble("Sell", 0.008);

        // Validações
        if (serverCard == null || serverCard.isEmpty()) {
            p.sendMessage("§cCard do servidor não configurado. Avise um administrador.");
            plugin.getLogger().warning("Card do servidor não configurado no config.yml!");
            return;
        }

        // Validar item ID (suporta mods)
        if (!ItemUtil.isValidItemId(itemId)) {
            p.sendMessage("§cItem '" + itemId + "' configurado é inválido.");
            plugin.getLogger().warning("Item inválido configurado: " + itemId);
            return;
        }

        // Calcular quantidade de itens necessários
        int itemsNeeded = (int) (qty * itemMultiplier);
        String itemDisplayName = ItemUtil.getItemDisplayName(itemId);
        
        // Verificar se o jogador tem itens suficientes
        if (!ItemUtil.has(p, itemId, itemsNeeded)) {
            p.sendMessage("§cVocê não tem itens suficientes!");
            p.sendMessage("§7Necessário: §e" + itemsNeeded + " " + itemDisplayName);
            return;
        }

        // Calcular recompensa (truncar para 8 casas decimais)
        double reward = qty * sellValue;
        reward = Math.floor(reward * 1e8) / 1e8;
        
        // Confirmar transação
        p.sendMessage("§6§lConfirmar Venda:");
        p.sendMessage("§7Item: §e" + itemDisplayName);
        p.sendMessage("§7Quantidade: §e" + qty + " §7unidades");
        p.sendMessage("§7Total itens: §e" + itemsNeeded);
        p.sendMessage("§7Ganho total: §e" + amountFormat.format(reward) + " §7coins");
        p.sendMessage("");
        p.sendMessage("§eDigite §f/coin sell " + args[1] + " §enovamente para confirmar.");
        
        // Verificar se é uma confirmação
        if (isConfirmation(p, "sell", args)) {
            plugin.getLogger().info(p.getName() + " tentando vender: " + itemsNeeded + " " + itemId + " por " + reward + " coins");
            
            // Executar venda via API
            if (api.sell(serverCard, userCard, reward)) {
                ItemUtil.remove(p, itemId, itemsNeeded);
                p.sendMessage("§a✓ Venda realizada com sucesso!");
                p.sendMessage("§7Itens vendidos: §e" + itemsNeeded + " " + itemDisplayName);
                p.sendMessage("§7Coins recebidos: §e" + amountFormat.format(reward));
                setCooldown(p);
            } else {
                p.sendMessage("§c✗ Falha na venda. Verifique:");
                p.sendMessage("§7- O servidor tem saldo suficiente?");
                p.sendMessage("§7- Sua conexão com a API está funcionando?");
            }
        }
    }

    // Sistema de confirmação para transações
    private final Map<UUID, ConfirmationData> confirmations = new HashMap<>();
    
    private static class ConfirmationData {
        final String command;
        final String[] args;
        final long timestamp;
        
        ConfirmationData(String command, String[] args) {
            this.command = command;
            this.args = args;
            this.timestamp = System.currentTimeMillis();
        }
        
        boolean matches(String command, String[] args) {
            if (!this.command.equals(command)) return false;
            if (this.args.length != args.length) return false;
            for (int i = 0; i < args.length; i++) {
                if (!this.args[i].equals(args[i])) return false;
            }
            return true;
        }
        
        boolean isExpired() {
            return System.currentTimeMillis() - timestamp > 30000; // 30 segundos
        }
    }
    
    private boolean isConfirmation(Player p, String command, String[] args) {
        UUID uuid = p.getUniqueId();
        ConfirmationData last = confirmations.get(uuid);
        
        // Limpar confirmações expiradas
        if (last != null && last.isExpired()) {
            confirmations.remove(uuid);
            last = null;
        }
        
        if (last != null && last.matches(command, args)) {
            // É uma confirmação, limpar e retornar true
            confirmations.remove(uuid);
            return true;
        } else {
            // Primeira vez, armazenar e retornar false
            confirmations.put(uuid, new ConfirmationData(command, args));
            return false;
        }
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> completions = new ArrayList<>();
        
        if (args.length == 1) {
            // Comandos básicos para todos
            List<String> commands = Arrays.asList("buy", "sell", "id", "help", "info");
            for (String cmd : commands) {
                if (cmd.startsWith(args[0].toLowerCase())) {
                    completions.add(cmd);
                }
            }
            
            // Comando admin
            if (sender.hasPermission("coincash.admin")) {
                if ("reload".startsWith(args[0].toLowerCase())) {
                    completions.add("reload");
                }
            }
        } 
        else if (args.length == 2) {
            switch (args[0].toLowerCase()) {
                case "buy":
                case "sell":
                    // Sugestões de quantidades comuns
                    String[] suggestions = {"1", "5", "10", "16", "32", "64", "128", "256", "512", "1024"};
                    for (String sug : suggestions) {
                        if (sug.startsWith(args[1])) {
                            completions.add(sug);
                        }
                    }
                    break;
                    
                case "id":
                    // Se o jogador já tem um card salvo, sugerir ele
                    if (sender instanceof Player) {
                        Player p = (Player) sender;
                        String savedCard = store.getUserId(p.getUniqueId());
                        if (savedCard != null && !savedCard.isEmpty()) {
                            if (savedCard.startsWith(args[1]) || args[1].isEmpty()) {
                                completions.add(savedCard);
                            }
                        }
                    }
                    completions.add("<seu-card>");
                    break;
            }
        }
        else if (args.length == 3 && args[0].equalsIgnoreCase("buy")) {
            // Para o comando buy, sugerir o card do jogador se tiver salvo
            if (sender instanceof Player) {
                Player p = (Player) sender;
                String savedCard = store.getUserId(p.getUniqueId());
                if (savedCard != null && !savedCard.isEmpty()) {
                    if (savedCard.startsWith(args[2]) || args[2].isEmpty()) {
                        completions.add(savedCard);
                    }
                }
            }
            completions.add("<card>");
        }
        
        // Ordenar e retornar
        Collections.sort(completions);
        return completions;
    }
    
    // Método auxiliar para truncar números
    private double truncate(double value) {
        return Math.floor(value * 1e8) / 1e8;
    }
}