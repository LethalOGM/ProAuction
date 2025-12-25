package me.flex.proauction.gui;

import me.flex.proauction.ProAuctionPlugin;
import me.flex.proauction.auction.AuctionItem;
import me.flex.proauction.util.ItemUtil;
import me.flex.proauction.util.Text;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.List;

public class ConfirmPurchaseGUI {

    private final ProAuctionPlugin plugin;

    public ConfirmPurchaseGUI(ProAuctionPlugin plugin) {
        this.plugin = plugin;
    }

    public void open(Player player, AuctionItem item) {
        ConfigurationSection root = plugin.getGuiConfig().getConfigurationSection("confirm-purchase");
        if (root == null) {
            player.sendMessage("§cGUI config missing: confirm-purchase");
            return;
        }

        String title = Text.color(root.getString("title", "&8Confirm Purchase"));
        int size = root.getInt("size", 27);

        Inventory inv = Bukkit.createInventory(player, size, title);

        ItemStack fill = ItemUtil.simpleItem(Material.BLACK_STAINED_GLASS_PANE, " ", List.of());
        for (int i = 0; i < size; i++) inv.setItem(i, fill);

        int infoSlot = root.getInt("info-slot", 13);
        inv.setItem(infoSlot, item.item().clone());

        ConfigurationSection buttons = root.getConfigurationSection("buttons");
        if (buttons != null) {
            place(inv, buttons.getConfigurationSection("confirm"));
            place(inv, buttons.getConfigurationSection("cancel"));
        }

        GuiListener.setPending(player, item);
        player.openInventory(inv);
    }

    private void place(Inventory inv, ConfigurationSection btn) {
        if (btn == null) return;

        int slot = btn.getInt("slot", -1);
        if (slot < 0 || slot >= inv.getSize()) return;

        Material mat = Material.matchMaterial(btn.getString("material", "STONE"));
        if (mat == null) mat = Material.STONE;

        inv.setItem(slot, ItemUtil.simpleItem(mat, btn.getString("name", " "), btn.getStringList("lore")));
    }
}
