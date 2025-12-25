package me.flex.proauction.economy;

import me.flex.proauction.ProAuctionPlugin;
import me.flex.proauction.economy.providers.RubiesEconomyProvider;
import me.flex.proauction.economy.providers.VaultEconomyProvider;

import java.util.EnumMap;
import java.util.Map;

public class EconomyService {

    private final Map<Currency.CurrencyType, EconomyProvider> providers = new EnumMap<>(Currency.CurrencyType.class);

    public EconomyService(ProAuctionPlugin plugin) {
        providers.put(Currency.CurrencyType.VAULT, new VaultEconomyProvider(plugin));
        providers.put(Currency.CurrencyType.RUBIES, new RubiesEconomyProvider(plugin));
    }

    public EconomyProvider providerFor(Currency currency) {
        return providers.get(currency.type());
    }

    public boolean isCurrencyUsable(Currency currency) {
        EconomyProvider p = providerFor(currency);
        return p != null && p.isAvailable();
    }

    public double balance(org.bukkit.entity.Player player, Currency currency) {
        return providerFor(currency).getBalance(player);
    }

    public boolean has(org.bukkit.entity.Player player, Currency currency, double amount) {
        return providerFor(currency).has(player, amount);
    }

    public boolean withdraw(org.bukkit.entity.Player player, Currency currency, double amount) {
        return providerFor(currency).withdraw(player, amount);
    }

    public boolean deposit(org.bukkit.entity.Player player, Currency currency, double amount) {
        return providerFor(currency).deposit(player, amount);
    }
}
