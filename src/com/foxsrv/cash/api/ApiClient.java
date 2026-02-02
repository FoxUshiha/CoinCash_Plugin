package com.foxsrv.cash.api;

import org.bukkit.plugin.java.JavaPlugin;

import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Locale;

public class ApiClient {

    private final JavaPlugin plugin;
    private final DecimalFormat decimalFormat;

    public ApiClient(JavaPlugin plugin) {
        this.plugin = plugin;
        // Forçar formatação com ponto decimal
        DecimalFormatSymbols symbols = new DecimalFormatSymbols(Locale.US);
        symbols.setDecimalSeparator('.');
        decimalFormat = new DecimalFormat("#.########", symbols);
        decimalFormat.setMaximumFractionDigits(8);
    }

    private boolean post(String endpoint, String payload) {
        try {
            String baseUrl = plugin.getConfig().getString("API");
            
            if (baseUrl == null || baseUrl.isEmpty()) {
                plugin.getLogger().severe("Configuração 'API' não definida no config.yml!");
                return false;
            }
            
            // Garantir formato correto da URL
            baseUrl = baseUrl.trim();
            if (!baseUrl.endsWith("/")) {
                baseUrl += "/";
            }
            
            // Remover barras duplicadas
            if (endpoint.startsWith("/")) {
                endpoint = endpoint.substring(1);
            }
            
            URL url = new URL(baseUrl + endpoint);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            
            conn.setRequestMethod("POST");
            conn.setConnectTimeout(15000); // 15 segundos
            conn.setReadTimeout(15000);    // 15 segundos
            conn.setDoOutput(true);
            conn.setRequestProperty("Content-Type", "application/json");
            conn.setRequestProperty("Accept", "application/json");
            conn.setRequestProperty("User-Agent", "CoinCash-Plugin/1.0");
            
            // Log para debug
            plugin.getLogger().info("Enviando para API: " + url.toString());
            plugin.getLogger().info("Payload: " + payload);
            
            try (OutputStream os = conn.getOutputStream()) {
                byte[] payloadBytes = payload.getBytes(StandardCharsets.UTF_8);
                os.write(payloadBytes);
                os.flush();
            }
            
            int responseCode = conn.getResponseCode();
            String responseMsg = conn.getResponseMessage();
            
            plugin.getLogger().info("Resposta da API: " + responseCode + " " + responseMsg);
            
            // Ler resposta para debug
            if (responseCode >= 400) {
                try (java.io.InputStream errorStream = conn.getErrorStream()) {
                    if (errorStream != null) {
                        String errorBody = new String(errorStream.readAllBytes(), StandardCharsets.UTF_8);
                        plugin.getLogger().warning("Corpo do erro: " + errorBody);
                    }
                }
            }
            
            // Aceita códigos 2xx como sucesso
            return responseCode >= 200 && responseCode < 300;
            
        } catch (Exception e) {
            plugin.getLogger().severe("Erro na comunicação com API: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    public boolean buy(String userCard, String serverCard, double amount) {
        String formattedAmount = decimalFormat.format(amount);
        String payload = String.format(
            "{\"fromCard\":\"%s\",\"toCard\":\"%s\",\"amount\":%s}",
            userCard, serverCard, formattedAmount
        );
        plugin.getLogger().info("Payload de compra: " + payload);
        return post("api/card/pay", payload);
    }

    public boolean sell(String serverCard, String userCard, double amount) {
        String formattedAmount = decimalFormat.format(amount);
        String payload = String.format(
            "{\"fromCard\":\"%s\",\"toCard\":\"%s\",\"amount\":%s}",
            serverCard, userCard, formattedAmount
        );
        plugin.getLogger().info("Payload de venda: " + payload);
        return post("api/card/pay", payload);
    }
}