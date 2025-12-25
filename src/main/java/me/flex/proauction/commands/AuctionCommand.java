package me.flex.proauction.commands;

import me.flex.proauction.ProAuctionPlugin;
import me.flex.proauction.auction.AuctionItem;
import me.flex.proauction.economy.Currency;
import me.flex.proauction.gui.AuctionBrowserGUI;
import me.flex.proauction.util.Text;
import me.flex.proauction.util.TimeUtil;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.List;
import java.util.Optional;

public class AuctionCommand implements CommandExecutor {

    private final ProAuctionPlugin plugin;

    public AuctionCommand(ProAuctionPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {

        if (!(sender instanceof Player player)) {
            sender.sendMessage("Only players can use this command.");
            return true;
        }

        plugin.getAuctionManager().processExpirations();

        if (args.length == 0) {
            new AuctionBrowserGUI(plugin).open(player, 1);
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "sell" -> sell(player, args);
            case "cancel" -> cancel(player, args);
            case "expired" -> expired(player, args);
            case "reload" -> reload(player);
            case "ban" -> ban(player);
            case "search" -> player.sendMessage("§aSearch command registered (v2+).");
            default -> player.sendMessage("§cUnknown subcommand.");
        }

        return true;
    }

    private void sell(Player player, String[] args) {
        if (args.length < 2) {
            player.sendMessage(Text.color(plugin.getMessagesConfig().getString("messages.sell-usage", "&cUsage: /ah sell <price> [currency]")));
            return;
        }

        ItemStack hand = player.getInventory().getItemInMainHand();
        if (hand == null || hand.getType().isAir()) {
            player.sendMessage(Text.color(plugin.getMessagesConfig().getString("messages.sell-air", "&cYou must hold an item in your hand.")));
            return;
        }

        double price;
        try {
            price = Double.parseDouble(args[1]);
        } catch (Exception e) {
            player.sendMessage(Text.color(plugin.getMessagesConfig().getString("messages.sell-invalid-price", "&cInvalid price.")));
            return;
        }

        if (price <= 0) {
            player.sendMessage(Text.color(plugin.getMessagesConfig().getString("messages.sell-invalid-price", "&cInvalid price.")));
            return;
        }

        String defaultCurrency = plugin.getSettingsConfig().getString("settings.default-currency", "vault");
        String currencyKey = (args.length >= 3) ? args[2].toLowerCase() : defaultCurrency;

        Optional<Currency> currencyOpt = plugin.getCurrencyRegistry().get(currencyKey);
        if (currencyOpt.isEmpty()) {
            String msg = plugin.getMessagesConfig().getString("messages.sell-invalid-currency", "&cUnknown currency: %currency%");
            player.sendMessage(Text.color(msg.replace("%currency%", currencyKey)));
            return;
        }

        Currency currency = currencyOpt.get();
        if (!plugin.getEconomyService().isCurrencyUsable(currency)) {
            player.sendMessage(Text.color(plugin.getMessagesConfig().getString("messages.sell-currency-not-usable", "&cThat currency is not usable right now.")));
            return;
        }

        if (plugin.getSettingsConfig().getBoolean("settings.bans.enabled", true)) {
            Material mat = hand.getType();
            if (plugin.getBanManager().isBanned(mat)) {
                player.sendMessage(Text.color(plugin.getMessagesConfig().getString("messages.sell-banned-item", "&cThat item is banned from being listed.")));
                return;
            }
        }

        int max = plugin.getSettingsConfig().getInt("settings.listing.max-active-per-player", 10);
        int active = plugin.getAuctionManager().countActiveFor(player.getUniqueId());
        if (active >= max) {
            String msg = plugin.getMessagesConfig().getString("messages.sell-max-reached", "&cYou have reached the maximum active listings (%max%).");
            player.sendMessage(Text.color(msg.replace("%max%", String.valueOf(max))));
            return;
        }

        long durationSeconds = plugin.getSettingsConfig().getLong("settings.listing.duration-seconds", 172800);

        // remove from seller immediately
        player.getInventory().setItemInMainHand(null);

        AuctionItem ai = plugin.getAuctionManager().createListing(player, hand, price, currencyKey, durationSeconds);
        plugin.saveDataNow();

        String symbol = currency.symbol();
        String currencyName = stripColor(currency.displayName());

        String msg = plugin.getMessagesConfig().getString("messages.sell-success",
                "&aListed item for &f%price% %symbol% &7(%currency%). &7ID: &f%id%");

        player.sendMessage(Text.color(
                msg.replace("%price%", formatPrice(price))
                        .replace("%symbol%", symbol)
                        .replace("%currency%", currencyName)
                        .replace("%id%", ai.id().toString().split("-")[0])
        ));
    }

    private void cancel(Player player, String[] args) {
        if (args.length < 2) {
            player.sendMessage("§cUsage: /ah cancel <id>");
            return;
        }

        String idPrefix = args[1];
        boolean ok = plugin.getAuctionManager().cancelListing(player.getUniqueId(), idPrefix);

        if (!ok) {
            player.sendMessage("§cCould not cancel. Check the ID and that you are the seller.");
            return;
        }

        plugin.saveDataNow();
        player.sendMessage("§aCancelled listing. Your item was moved to /ah expired for claiming.");
    }

    private void expired(Player player, String[] args) {
        if (args.length >= 3 && args[1].equalsIgnoreCase("claim")) {
            String idPrefix = args[2];

            var opt = plugin.getAuctionManager().findExpiredByPrefix(player.getUniqueId(), idPrefix);
            if (opt.isEmpty()) {
                player.sendMessage("§cNo expired item found for that ID.");
                return;
            }

            AuctionItem ai = opt.get();

            if (player.getInventory().firstEmpty() == -1) {
                player.sendMessage(Text.color(plugin.getMessagesConfig().getString("messages.inventory-full", "&cYour inventory is full.")));
                return;
            }

            boolean removed = plugin.getAuctionManager().claimExpired(player.getUniqueId(), idPrefix);
            if (!removed) {
                player.sendMessage("§cUnable to claim that item.");
                return;
            }

            player.getInventory().addItem(ai.item().clone());
            plugin.saveDataNow();

            player.sendMessage("§aClaimed expired item: §f" + ai.id().toString().split("-")[0]);
            return;
        }

        List<AuctionItem> mine = plugin.getAuctionManager().expiredFor(player.getUniqueId());
        if (mine.isEmpty()) {
            player.sendMessage("§7You have no expired/cancelled items.");
            return;
        }

        long now = System.currentTimeMillis();

        player.sendMessage("§eYour expired/cancelled items:");
        for (AuctionItem ai : mine) {
            String id = ai.id().toString().split("-")[0];

            String ago = TimeUtil.formatAgoShort(Math.max(0, now - ai.expiresAtMillis()));

            String currencyKey = ai.currencyKey();
            String currencyName = currencyKey;
            String symbol = "";

            Optional<Currency> cOpt = plugin.getCurrencyRegistry().get(currencyKey);
            if (cOpt.isPresent()) {
                currencyName = stripColor(cOpt.get().displayName());
                symbol = cOpt.get().symbol();
            }

            player.sendMessage("§7- §f" + id
                    + " §7| §f" + ai.item().getType().name()
                    + " §7| §f" + formatPrice(ai.price()) + " " + symbol + " §7(" + currencyName + ")"
                    + " §7| §cexpired: §f" + ago
                    + " §7| §cclaim: §f/ah expired claim " + id);
        }
    }

    private void ban(Player player) {
        if (!player.hasPermission("proauction.admin")) {
            player.sendMessage(Text.color(plugin.getMessagesConfig().getString("messages.ban-no-permission", "&cNo permission.")));
            return;
        }

        ItemStack hand = player.getInventory().getItemInMainHand();
        if (hand == null || hand.getType().isAir()) {
            player.sendMessage(Text.color(plugin.getMessagesConfig().getString("messages.ban-usage", "&cHold an item and run /ah ban")));
            return;
        }

        boolean added = plugin.getBanManager().toggle(hand.getType());
        plugin.saveDataNow();

        if (added) {
            player.sendMessage(Text.color(plugin.getMessagesConfig().getString("messages.ban-added", "&aThis item is now banned from being listed.")));
        } else {
            player.sendMessage(Text.color(plugin.getMessagesConfig().getString("messages.ban-removed", "&eThis item is no longer banned.")));
        }
    }

    private void reload(Player player) {
        if (!player.hasPermission("proauction.admin")) {
            player.sendMessage("§cNo permission.");
            return;
        }

        plugin.getCurrencyRegistry().load();
        plugin.reloadGuiConfig();
        plugin.reloadMessagesConfig();
        plugin.reloadSettingsConfig();
        plugin.reloadBansConfig();
        plugin.getBanManager().loadFromConfig();

        player.sendMessage("§aReloaded currencies.yml, gui.yml, messages.yml, settings.yml, bans.yml");
    }

    private String formatPrice(double d) {
        if (d == (long) d) return String.valueOf((long) d);
        return String.format("%.2f", d);
    }

    private String stripColor(String s) {
        return s == null ? "" : s.replaceAll("§[0-9A-FK-ORa-fk-or]", "");
    }
}
