package me.flex.proauction.auction;

import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;

public class AuctionManager {

    private final List<AuctionItem> listings = new CopyOnWriteArrayList<>();
    private final List<AuctionItem> expired = new CopyOnWriteArrayList<>();

    public List<AuctionItem> getListingsSnapshot() {
        return new ArrayList<>(listings);
    }

    public List<AuctionItem> getExpiredSnapshot() {
        return new ArrayList<>(expired);
    }

    public void setAll(List<AuctionItem> newListings, List<AuctionItem> newExpired) {
        listings.clear();
        expired.clear();
        listings.addAll(newListings);
        expired.addAll(newExpired);
    }

    public void addListing(AuctionItem item) {
        listings.add(item);
    }

    public void removeById(UUID id) {
        listings.removeIf(a -> a.id().equals(id));
        expired.removeIf(a -> a.id().equals(id));
    }

    public Optional<AuctionItem> getById(UUID id) {
        for (AuctionItem a : listings) {
            if (a.id().equals(id)) return Optional.of(a);
        }
        return Optional.empty();
    }

    public int countActiveFor(UUID seller) {
        int c = 0;
        for (AuctionItem a : listings) {
            if (a.sellerUuid().equals(seller)) c++;
        }
        return c;
    }

    public AuctionItem createListing(Player seller, ItemStack item, double price, String currencyKey, long durationSeconds) {
        long now = System.currentTimeMillis();
        long expires = now + (durationSeconds * 1000L);

        AuctionItem ai = new AuctionItem(
                UUID.randomUUID(),
                seller.getUniqueId(),
                seller.getName(),
                item.clone(),
                price,
                currencyKey,
                now,
                expires
        );

        addListing(ai);
        return ai;
    }

    public void processExpirations() {
        long now = System.currentTimeMillis();
        for (AuctionItem ai : new ArrayList<>(listings)) {
            if (ai.expiresAtMillis() <= now) {
                listings.remove(ai);
                expired.add(ai);
            }
        }
    }

    public Optional<AuctionItem> findActiveByPrefix(String prefix) {
        String p = prefix.toLowerCase();
        for (AuctionItem ai : listings) {
            String idp = ai.id().toString().split("-")[0].toLowerCase();
            if (idp.equals(p)) return Optional.of(ai);
        }
        return Optional.empty();
    }

    public Optional<AuctionItem> findExpiredByPrefix(UUID seller, String prefix) {
        String p = prefix.toLowerCase();
        for (AuctionItem ai : expired) {
            if (!ai.sellerUuid().equals(seller)) continue;
            String idp = ai.id().toString().split("-")[0].toLowerCase();
            if (idp.equals(p)) return Optional.of(ai);
        }
        return Optional.empty();
    }

    public List<AuctionItem> expiredFor(UUID seller) {
        List<AuctionItem> out = new ArrayList<>();
        for (AuctionItem ai : expired) {
            if (ai.sellerUuid().equals(seller)) out.add(ai);
        }
        return out;
    }

    public boolean cancelListing(UUID seller, String idPrefix) {
        Optional<AuctionItem> opt = findActiveByPrefix(idPrefix);
        if (opt.isEmpty()) return false;

        AuctionItem ai = opt.get();
        if (!ai.sellerUuid().equals(seller)) return false;

        listings.remove(ai);
        expired.add(ai); // cancellation returns item via "expired/claim"
        return true;
    }

    public boolean claimExpired(UUID seller, String idPrefix) {
        Optional<AuctionItem> opt = findExpiredByPrefix(seller, idPrefix);
        if (opt.isEmpty()) return false;

        expired.remove(opt.get());
        return true;
    }
}
