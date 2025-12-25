package me.flex.proauction.economy.providers;

import me.clip.placeholderapi.PlaceholderAPI;
import me.flex.proauction.ProAuctionPlugin;
import me.flex.proauction.economy.Currency;
import me.flex.proauction.economy.EconomyProvider;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.text.DecimalFormat;
import java.util.Optional;

public class RubiesEconomyProvider implements EconomyProvider {

    private final ProAuctionPlugin plugin;
    private final DecimalFormat noDecimal = new DecimalFormat("0");

    public RubiesEconomyProvider(ProAuctionPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean isAvailable() {
        return Bukkit.getPluginManager().getPlugin("PlaceholderAPI") != null;
    }

    private Optional<Currency> rubiesCurrency() {
        return plugin.getCurrencyRegistry().get("rubies");
    }

    @Override
    public double getBalance(Player player) {
        if (!isAvailable()) return 0.0;

        Currency c = rubiesCurrency().orElse(null);
        if (c == null || c.placeholder() == null) return 0.0;

        String raw = PlaceholderAPI.setPlaceholders(player, c.placeholder());
        raw = raw.replace(",", "").trim();

        try {
            return Double.parseDouble(raw);
        } catch (NumberFormatException e) {
            return 0.0;
        }
    }

    @Override
    public boolean has(Player player, double amount) {
        return getBalance(player) >= amount;
    }

    @Override
    public boolean withdraw(Player player, double amount) {
        double bal = getBalance(player);
        if (bal < amount) return false;

        runSet(player, bal - amount);
        return true;
    }

    @Override
    public boolean deposit(Player player, double amount) {
        double bal = getBalance(player);
        runSet(player, bal + amount);
        return true;
    }

    private void runSet(Player player, double newBal) {
        Currency c = rubiesCurrency().orElse(null);
        if (c == null || c.setCommand() == null) return;

        String amt = noDecimal.format(newBal);
        String cmd = c.setCommand()
                .replace("%player%", player.getName())
                .replace("%amount%", amt);

        Bukkit.dispatchCommand(Bukkit.getConsoleSender(), cmd);
    }
}
