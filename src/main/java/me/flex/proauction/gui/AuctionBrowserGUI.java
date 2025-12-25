package me.flex.proauction.gui;

import me.flex.proauction.ProAuctionPlugin;
import me.flex.proauction.auction.AuctionItem;
import me.flex.proauction.economy.Currency;
import me.flex.proauction.util.ItemUtil;
import me.flex.proauction.util.Text;
import me.flex.proauction.util.TimeUtil;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class AuctionBrowserGUI {

    private final ProAuctionPlugin plugin;

    public AuctionBrowserGUI(ProAuctionPlugin plugin) {
        this.plugin = plugin;
    }

    public void open(Player player, int page) {
        ConfigurationSection root = plugin.getGuiConfig().getConfigurationSection("auction-browser");
        if (root == null) {
            player.sendMessage("§cGUI config missing: auction-browser");
            return;
        }

        String title = Text.color(root.getString("title", "&8ProAuction"));
        int size = root.getInt("size", 54);

        Inventory inv = Bukkit.createInventory(player, size, title);

        ConfigurationSection filler = root.getConfigurationSection("filler");
        if (filler != null && filler.getBoolean("enabled", true)) {
            Material fillerMat = Material.matchMaterial(filler.getString("material", "BLACK_STAINED_GLASS_PANE"));
            if (fillerMat == null) fillerMat = Material.BLACK_STAINED_GLASS_PANE;
            ItemStack fillItem = ItemUtil.simpleItem(fillerMat, filler.getString("name", " "), List.of());
            for (int i = 0; i < size; i++) inv.setItem(i, fillItem);
        }

        placeButtons(inv, root);

        List<Integer> slots = root.getIntegerList("item-slots");
        if (slots.isEmpty()) {
            player.sendMessage("§cGUI config missing: auction-browser.item-slots");
            return;
        }

        List<AuctionItem> listings = plugin.getAuctionManager().getListingsSnapshot();

        int perPage = slots.size();
        int maxPage = Math.max(1, (int) Math.ceil(listings.size() / (double) perPage));
        int safePage = Math.max(1, Math.min(page, maxPage));

        int startIndex = (safePage - 1) * perPage;
        int endIndex = Math.min(startIndex + perPage, listings.size());

        List<String> loreTemplate = plugin.getGuiConfig().getStringList("listing-lore");

        long now = System.currentTimeMillis();

        int slotIndex = 0;
        for (int i = startIndex; i < endIndex; i++) {
            AuctionItem ai = listings.get(i);

            ItemStack displayItem = ai.item().clone();
            ItemMeta meta = displayItem.getItemMeta();
            if (meta != null) {
                List<String> lore = new ArrayList<>();

                Optional<Currency> currencyOpt = plugin.getCurrencyRegistry().get(ai.currencyKey());
                String currencyName = ai.currencyKey();
                String symbol = "";

                if (currencyOpt.isPresent()) {
                    currencyName = stripColor(currencyOpt.get().displayName());
                    symbol = currencyOpt.get().symbol();
                }

                String timeLeft = TimeUtil.formatDurationShort(ai.expiresAtMillis() - now);

                for (String line : loreTemplate) {
                    lore.add(Text.color(line
                            .replace("%seller%", ai.sellerName())
                            .replace("%price%", formatPrice(ai.price()))
                            .replace("%currency%", currencyName)
                            .replace("%symbol%", symbol)
                            .replace("%time_left%", timeLeft)
                    ));
                }

                meta.setLore(lore);

                meta.getPersistentDataContainer().set(
                        plugin.getListingIdKey(),
                        PersistentDataType.STRING,
                        ai.id().toString()
                );

                displayItem.setItemMeta(meta);
            }

            int slot = slots.get(slotIndex++);
            inv.setItem(slot, displayItem);
        }

        GuiListener.setPage(player, safePage);
        player.openInventory(inv);
    }

    private void placeButtons(Inventory inv, ConfigurationSection root) {
        ConfigurationSection buttons = root.getConfigurationSection("buttons");
        if (buttons == null) return;

        placeButton(inv, buttons.getConfigurationSection("prev"));
        placeButton(inv, buttons.getConfigurationSection("next"));
        placeButton(inv, buttons.getConfigurationSection("close"));
    }

    private void placeButton(Inventory inv, ConfigurationSection btn) {
        if (btn == null) return;

        int slot = btn.getInt("slot", -1);
        if (slot < 0 || slot >= inv.getSize()) return;

        Material mat = Material.matchMaterial(btn.getString("material", "STONE"));
        if (mat == null) mat = Material.STONE;

        inv.setItem(slot, ItemUtil.simpleItem(mat, btn.getString("name", " "), btn.getStringList("lore")));
    }

    private String formatPrice(double d) {
        if (d == (long) d) return String.valueOf((long) d);
        return String.format("%.2f", d);
    }

    private String stripColor(String s) {
        return s == null ? "" : s.replaceAll("§[0-9A-FK-ORa-fk-or]", "");
    }
}
