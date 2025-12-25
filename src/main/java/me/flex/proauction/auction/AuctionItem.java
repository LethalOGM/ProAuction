package me.flex.proauction.auction;

import org.bukkit.inventory.ItemStack;

import java.util.UUID;

public record AuctionItem(
        UUID id,
        UUID sellerUuid,
        String sellerName,
        ItemStack item,
        double price,
        String currencyKey,
        long createdAtMillis,
        long expiresAtMillis
) {}
