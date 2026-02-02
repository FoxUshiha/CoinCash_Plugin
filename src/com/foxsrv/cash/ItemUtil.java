package com.foxsrv.cash.util;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;

import com.foxsrv.cash.Cash;

import java.util.regex.Pattern;

public class ItemUtil {

    private static final Pattern ITEM_PATTERN = Pattern.compile("^[a-z0-9_.-]+:[a-z0-9/._-]+$", Pattern.CASE_INSENSITIVE);
    private static Cash plugin;

    public static void init(Cash pluginInstance) {
        plugin = pluginInstance;
    }

    public static boolean isValidItemId(String itemId) {
        if (itemId == null || itemId.isEmpty()) return false;
        
        // Se for um Material válido
        try {
            Material.valueOf(itemId.toUpperCase());
            return true;
        } catch (IllegalArgumentException e) {
            // Não é um Material vanilla, verificar se é um item de mod
            return ITEM_PATTERN.matcher(itemId).matches();
        }
    }

    public static ItemStack createItem(String itemId, int amount) {
        if (itemId == null || itemId.isEmpty()) {
            return null;
        }

        // Tentar como Material vanilla primeiro
        try {
            Material material = Material.valueOf(itemId.toUpperCase());
            return new ItemStack(material, amount);
        } catch (IllegalArgumentException e) {
            // Não é Material vanilla, tratar como item de mod
            return createModItem(itemId, amount);
        }
    }

    private static ItemStack createModItem(String itemId, int amount) {
        try {
            // Criar um item placeholder com metadata
            ItemStack item = new ItemStack(Material.PAPER, amount);
            ItemMeta meta = item.getItemMeta();
            
            if (meta != null) {
                // Definir nome customizado
                String displayName = getDisplayName(itemId);
                meta.setDisplayName("§r" + displayName);
                
                // Armazenar o ID real do item no metadata
                if (plugin != null) {
                    NamespacedKey key = new NamespacedKey(plugin, "real_item_id");
                    meta.getPersistentDataContainer().set(key, PersistentDataType.STRING, itemId);
                }
                
                item.setItemMeta(meta);
            }
            
            return item;
        } catch (Exception e) {
            plugin.getLogger().warning("Erro ao criar item de mod: " + itemId + " - " + e.getMessage());
            return new ItemStack(Material.BARRIER, amount);
        }
    }

    private static String getDisplayName(String itemId) {
        // Converter item ID para nome mais legível
        String[] parts = itemId.split(":");
        if (parts.length == 2) {
            String modId = parts[0];
            String itemName = parts[1];
            
            // Converter underscore para espaço e capitalizar
            itemName = itemName.replace('_', ' ');
            itemName = capitalizeWords(itemName);
            
            // Formatar cor baseada no mod (cores diferentes para mods diferentes)
            String color = getColorForMod(modId);
            
            return color + itemName + " §7(" + modId + ")";
        }
        return "§cItem Desconhecido";
    }

    private static String capitalizeWords(String str) {
        StringBuilder result = new StringBuilder();
        String[] words = str.split(" ");
        for (String word : words) {
            if (word.length() > 0) {
                result.append(Character.toUpperCase(word.charAt(0)))
                      .append(word.substring(1).toLowerCase())
                      .append(" ");
            }
        }
        return result.toString().trim();
    }

    private static String getColorForMod(String modId) {
        // Cores diferentes para mods populares
        switch (modId.toLowerCase()) {
            case "minecraft": return "§f";
            case "thermal": return "§c";
            case "immersiveengineering": return "§6";
            case "mekanism": return "§b";
            case "create": return "§e";
            case "tconstruct": return "§9";
            case "botania": return "§d";
            case "draconicevolution": return "§5";
            default: return "§a"; // Verde para outros mods
        }
    }

    public static boolean has(Player p, String itemId, int amount) {
        ItemStack targetItem = createItem(itemId, 1);
        if (targetItem == null) return false;
        
        int count = 0;
        for (ItemStack item : p.getInventory().getContents()) {
            if (item != null && isSameItem(item, itemId, targetItem)) {
                count += item.getAmount();
                if (count >= amount) {
                    return true;
                }
            }
        }
        return false;
    }

    public static void remove(Player p, String itemId, int amount) {
        ItemStack targetItem = createItem(itemId, 1);
        if (targetItem == null) return;
        
        int remaining = amount;
        
        for (int i = 0; i < p.getInventory().getSize(); i++) {
            ItemStack item = p.getInventory().getItem(i);
            if (item != null && isSameItem(item, itemId, targetItem)) {
                int itemAmount = item.getAmount();
                
                if (itemAmount > remaining) {
                    item.setAmount(itemAmount - remaining);
                    remaining = 0;
                    break;
                } else {
                    remaining -= itemAmount;
                    p.getInventory().setItem(i, null);
                }
            }
        }
    }

    public static void give(Player p, String itemId, int amount) {
        ItemStack item = createItem(itemId, amount);
        if (item == null) {
            p.sendMessage("§cErro: Item '" + itemId + "' é inválido.");
            return;
        }

        int maxStack = item.getMaxStackSize();
        int remaining = amount;
        
        while (remaining > 0) {
            int giveAmount = Math.min(remaining, maxStack);
            ItemStack stack = createItem(itemId, giveAmount);
            
            if (p.getInventory().addItem(stack).isEmpty()) {
                remaining -= giveAmount;
            } else {
                // Inventário cheio, dropar no chão
                p.getWorld().dropItem(p.getLocation(), stack);
                remaining -= giveAmount;
            }
        }
        
        if (remaining > 0) {
            p.sendMessage("§cSeu inventário está cheio! Alguns itens foram dropados no chão.");
        }
    }

    private static boolean isSameItem(ItemStack item, String targetItemId, ItemStack targetItem) {
        // Comparar primeiro pelo Material vanilla
        if (item.getType() == targetItem.getType()) {
            // Se ambos são placeholders de mods, verificar o ID no metadata
            if (plugin != null && item.hasItemMeta()) {
                ItemMeta meta = item.getItemMeta();
                NamespacedKey key = new NamespacedKey(plugin, "real_item_id");
                if (meta.getPersistentDataContainer().has(key, PersistentDataType.STRING)) {
                    String realId = meta.getPersistentDataContainer().get(key, PersistentDataType.STRING);
                    return realId != null && realId.equalsIgnoreCase(targetItemId);
                }
            }
            // Para itens vanilla, verificar se é o Material correto
            return item.getType().name().equalsIgnoreCase(targetItemId) || 
                   ("minecraft:" + item.getType().name().toLowerCase()).equalsIgnoreCase(targetItemId);
        }
        return false;
    }

    public static String getItemDisplayName(String itemId) {
        try {
            // Tentar Material vanilla primeiro
            Material material = Material.valueOf(itemId.toUpperCase());
            return material.name().toLowerCase().replace('_', ' ');
        } catch (IllegalArgumentException e) {
            // É um item de mod
            return getDisplayName(itemId);
        }
    }
}