package me.flex.proauction.economy;

import org.bukkit.entity.Player;

public interface EconomyProvider {
    boolean isAvailable();
    double getBalance(Player player);
    boolean has(Player player, double amount);
    boolean withdraw(Player player, double amount);
    boolean deposit(Player player, double amount);
}
