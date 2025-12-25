package me.flex.proauction.storage;

import me.flex.proauction.ProAuctionPlugin;
import me.flex.proauction.auction.AuctionItem;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.inventory.ItemStack;

import java.io.File;
import java.io.IOException;
import java.util.*;

public class YamlDataStore {

    private final ProAuctionPlugin plugin;
    private final File file;
    private FileConfiguration cfg;

    public YamlDataStore(ProAuctionPlugin plugin) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), "data.yml");
        if (!file.exists()) {
            plugin.saveResource("data.yml", false);
        }
        this.cfg = YamlConfiguration.loadConfiguration(file);
    }

    public void reload() {
        this.cfg = YamlConfiguration.loadConfiguration(file);
    }

    public void save() {
        try {
            cfg.save(file);
        } catch (IOException ignored) {
        }
    }

    public List<AuctionItem> loadListings() {
        List<Map<?, ?>> list = cfg.getMapList("data.listings");
        return loadFrom(list);
    }

    public List<AuctionItem> loadExpired() {
        List<Map<?, ?>> list = cfg.getMapList("data.expired");
        return loadFrom(list);
    }

    private List<AuctionItem> loadFrom(List<Map<?, ?>> list) {
        List<AuctionItem> out = new ArrayList<>();

        for (Map<?, ?> m : list) {
            try {
                UUID id = UUID.fromString(str(m, "id", ""));
                UUID seller = UUID.fromString(str(m, "sellerUuid", ""));
                String sellerName = str(m, "sellerName", "Unknown");

                String itemB64 = str(m, "item", "");
                ItemStack item = ItemStackSerializer.fromBase64(itemB64);
                if (item == null) continue;

                double price = dbl(m, "price", 0.0);
                String currency = str(m, "currency", "vault");

                long created = lng(m, "createdAt", System.currentTimeMillis());
                long expires = lng(m, "expiresAt", created);

                out.add(new AuctionItem(id, seller, sellerName, item, price, currency, created, expires));
            } catch (Exception ignored) {
            }
        }

        return out;
    }

    public void writeAll(List<AuctionItem> listings, List<AuctionItem> expired) {
        cfg.set("data.listings", serialize(listings));
        cfg.set("data.expired", serialize(expired));
        save();
    }

    private List<Map<String, Object>> serialize(List<AuctionItem> items) {
        List<Map<String, Object>> out = new ArrayList<>();
        for (AuctionItem ai : items) {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id", ai.id().toString());
            m.put("sellerUuid", ai.sellerUuid().toString());
            m.put("sellerName", ai.sellerName());
            m.put("item", ItemStackSerializer.toBase64(ai.item()));
            m.put("price", ai.price());
            m.put("currency", ai.currencyKey());
            m.put("createdAt", ai.createdAtMillis());
            m.put("expiresAt", ai.expiresAtMillis());
            out.add(m);
        }
        return out;
    }

    private String str(Map<?, ?> m, String key, String def) {
        Object v = m.get(key);
        if (v == null) return def;
        return String.valueOf(v);
    }

    private long lng(Map<?, ?> m, String key, long def) {
        Object v = m.get(key);
        if (v == null) return def;
        try {
            if (v instanceof Number n) return n.longValue();
            return Long.parseLong(String.valueOf(v));
        } catch (Exception e) {
            return def;
        }
    }

    private double dbl(Map<?, ?> m, String key, double def) {
        Object v = m.get(key);
        if (v == null) return def;
        try {
            if (v instanceof Number n) return n.doubleValue();
            return Double.parseDouble(String.valueOf(v));
        } catch (Exception e) {
            return def;
        }
    }
}
