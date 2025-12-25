package me.flex.proauction.gui;

import me.flex.proauction.ProAuctionPlugin;
import me.flex.proauction.auction.AuctionItem;
import me.flex.proauction.economy.Currency;
import me.flex.proauction.util.Text;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class GuiListener implements Listener {

    private static final Map<UUID, Integer> PAGE_STATE = new ConcurrentHashMap<>();
    private static final Map<UUID, AuctionItem> PENDING_PURCHASE = new ConcurrentHashMap<>();

    private final ProAuctionPlugin plugin;

    public GuiListener(ProAuctionPlugin plugin) {
        this.plugin = plugin;
    }

    public static void setPage(Player player, int page) {
        PAGE_STATE.put(player.getUniqueId(), page);
    }

    public static int getPage(Player player) {
        return PAGE_STATE.getOrDefault(player.getUniqueId(), 1);
    }

    public static void setPending(Player player, AuctionItem item) {
        PENDING_PURCHASE.put(player.getUniqueId(), item);
    }

    public static Optional<AuctionItem> getPending(Player player) {
        return Optional.ofNullable(PENDING_PURCHASE.get(player.getUniqueId()));
    }

    public static void clearPending(Player player) {
        PENDING_PURCHASE.remove(player.getUniqueId());
    }

    @EventHandler
    public void onClick(InventoryClickEvent e) {
        if (!(e.getWhoClicked() instanceof Player player)) return;

        Inventory top = e.getView().getTopInventory();
        if (top == null) return;

        String viewTitle = e.getView().getTitle();

        // Auction Browser
        if (isTitleMatch(viewTitle, "auction-browser.title")) {
            e.setCancelled(true);

            int slot = e.getRawSlot();
            if (slot < 0 || slot >= top.getSize()) return;

            ConfigurationSection root = plugin.getGuiConfig().getConfigurationSection("auction-browser");
            if (root == null) return;

            ConfigurationSection buttons = root.getConfigurationSection("buttons");
            if (buttons == null) return;

            int prevSlot = getSlot(buttons, "prev");
            int nextSlot = getSlot(buttons, "next");
            int closeSlot = getSlot(buttons, "close");

            int page = getPage(player);

            if (slot == closeSlot) {
                player.closeInventory();
                return;
            }
            if (slot == prevSlot) {
                new AuctionBrowserGUI(plugin).open(player, Math.max(1, page - 1));
                return;
            }
            if (slot == nextSlot) {
                new AuctionBrowserGUI(plugin).open(player, page + 1);
                return;
            }

            ItemStack clicked = e.getCurrentItem();
            if (clicked == null || clicked.getType().isAir()) return;

            ItemMeta meta = clicked.getItemMeta();
            if (meta == null) return;

            String idStr = meta.getPersistentDataContainer().get(plugin.getListingIdKey(), PersistentDataType.STRING);
            if (idStr == null || idStr.isBlank()) return;

            UUID listingId;
            try {
                listingId = UUID.fromString(idStr);
            } catch (Exception ex) {
                player.sendMessage(Text.color(plugin.getMessagesConfig().getString("messages.listing-missing", "&cThat listing no longer exists.")));
                return;
            }

            Optional<AuctionItem> aiOpt = plugin.getAuctionManager().getById(listingId);
            if (aiOpt.isEmpty()) {
                player.sendMessage(Text.color(plugin.getMessagesConfig().getString("messages.listing-missing", "&cThat listing no longer exists.")));
                return;
            }

            // If expired, force it into expired bucket and refresh
            AuctionItem ai = aiOpt.get();
            if (ai.expiresAtMillis() <= System.currentTimeMillis()) {
                plugin.getAuctionManager().processExpirations();
                plugin.saveDataNow();
                player.sendMessage("§cThat listing has expired.");
                new AuctionBrowserGUI(plugin).open(player, page);
                return;
            }

            new ConfirmPurchaseGUI(plugin).open(player, ai);
            return;
        }

        // Confirm Purchase GUI
        if (isTitleMatch(viewTitle, "confirm-purchase.title")) {
            e.setCancelled(true);

            ConfigurationSection root = plugin.getGuiConfig().getConfigurationSection("confirm-purchase");
            if (root == null) return;

            ConfigurationSection buttons = root.getConfigurationSection("buttons");
            if (buttons == null) return;

            int confirmSlot = getSlot(buttons, "confirm");
            int cancelSlot = getSlot(buttons, "cancel");

            int slot = e.getRawSlot();
            if (slot < 0 || slot >= top.getSize()) return;

            if (slot == cancelSlot) {
                clearPending(player);
                new AuctionBrowserGUI(plugin).open(player, 1);
                return;
            }

            if (slot != confirmSlot) return;

            Optional<AuctionItem> pendingOpt = getPending(player);
            if (pendingOpt.isEmpty()) {
                player.sendMessage(Text.color(plugin.getMessagesConfig().getString("messages.listing-missing", "&cThat listing no longer exists.")));
                return;
            }

            AuctionItem pending = pendingOpt.get();

            // Re-validate existence and expiry at confirm time
            Optional<AuctionItem> currentOpt = plugin.getAuctionManager().getById(pending.id());
            if (currentOpt.isEmpty()) {
                player.sendMessage(Text.color(plugin.getMessagesConfig().getString("messages.listing-missing", "&cThat listing no longer exists.")));
                clearPending(player);
                player.closeInventory();
                return;
            }

            AuctionItem current = currentOpt.get();
            if (current.expiresAtMillis() <= System.currentTimeMillis()) {
                plugin.getAuctionManager().processExpirations();
                plugin.saveDataNow();
                player.sendMessage("§cThat listing has expired.");
                clearPending(player);
                player.closeInventory();
                return;
            }

            if (current.sellerUuid().equals(player.getUniqueId())) {
                player.sendMessage(Text.color(plugin.getMessagesConfig().getString("messages.cannot-buy-own", "&cYou cannot buy your own listing.")));
                return;
            }

            if (player.getInventory().firstEmpty() == -1) {
                player.sendMessage(Text.color(plugin.getMessagesConfig().getString("messages.inventory-full", "&cYour inventory is full.")));
                return;
            }

            var currencyOpt = plugin.getCurrencyRegistry().get(current.currencyKey());
            if (currencyOpt.isEmpty()) {
                player.sendMessage("§cCurrency not found: " + current.currencyKey());
                return;
            }
            Currency currency = currencyOpt.get();

            var eco = plugin.getEconomyService();
            if (!eco.isCurrencyUsable(currency)) {
                player.sendMessage(Text.color(plugin.getMessagesConfig().getString("messages.currency-not-usable", "&cThat currency is not usable right now.")));
                return;
            }

            if (!eco.has(player, currency, current.price())) {
                String msg = plugin.getMessagesConfig().getString("messages.not-enough-money", "&cYou do not have enough %currency%.");
                player.sendMessage(Text.color(msg.replace("%currency%", stripColor(currency.displayName()))));
                return;
            }

            if (!eco.withdraw(player, currency, current.price())) {
                player.sendMessage(Text.color(plugin.getMessagesConfig().getString("messages.transaction-failed", "&cTransaction failed.")));
                return;
            }

            Player sellerOnline = Bukkit.getPlayer(current.sellerUuid());
            if (sellerOnline != null) {
                eco.deposit(sellerOnline, currency, current.price());

                String sellerMsg = plugin.getMessagesConfig().getString(
                        "messages.seller-paid",
                        "&7You sold an item for &f%price% %symbol% &7(%currency%)."
                );

                sellerOnline.sendMessage(Text.color(
                        sellerMsg.replace("%price%", formatPrice(current.price()))
                                .replace("%symbol%", currency.symbol())
                                .replace("%currency%", stripColor(currency.displayName()))
                ));
            }

            player.getInventory().addItem(current.item().clone());
            plugin.getAuctionManager().removeById(current.id());
            plugin.saveDataNow();

            String buyerMsg = plugin.getMessagesConfig().getString(
                    "messages.purchase-success",
                    "&aPurchased for &f%price% %symbol% &7(%currency%)."
            );

            player.sendMessage(Text.color(
                    buyerMsg.replace("%price%", formatPrice(current.price()))
                            .replace("%symbol%", currency.symbol())
                            .replace("%currency%", stripColor(currency.displayName()))
            ));

            clearPending(player);
            player.closeInventory();
        }
    }

    @EventHandler
    public void onClose(InventoryCloseEvent e) {
        if (!(e.getPlayer() instanceof Player player)) return;

        String title = e.getView().getTitle();

        if (isTitleMatch(title, "auction-browser.title")) {
            PAGE_STATE.remove(player.getUniqueId());
        }

        if (isTitleMatch(title, "confirm-purchase.title")) {
            PENDING_PURCHASE.remove(player.getUniqueId());
        }
    }

    private boolean isTitleMatch(String openTitle, String configPath) {
        String expected = ChatColor.stripColor(
                ChatColor.translateAlternateColorCodes('&',
                        plugin.getGuiConfig().getString(configPath, ""))
        );
        return ChatColor.stripColor(openTitle).equalsIgnoreCase(expected);
    }

    private int getSlot(ConfigurationSection buttons, String key) {
        ConfigurationSection cs = buttons.getConfigurationSection(key);
        if (cs == null) return -1;
        return cs.getInt("slot", -1);
    }

    private String formatPrice(double d) {
        if (d == (long) d) return String.valueOf((long) d);
        return String.format("%.2f", d);
    }

    private String stripColor(String s) {
        return s == null ? "" : s.replaceAll("§[0-9A-FK-ORa-fk-or]", "");
    }
}
