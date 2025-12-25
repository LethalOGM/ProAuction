package me.flex.proauction.economy;

public record Currency(
        String key,
        String displayName,
        String symbol,
        CurrencyType type,
        String placeholder,
        String setCommand
) {
    public enum CurrencyType {
        VAULT,
        RUBIES
    }
}
