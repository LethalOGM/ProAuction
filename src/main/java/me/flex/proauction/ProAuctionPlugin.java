package me.flex.proauction;

import me.flex.proauction.auction.AuctionManager;
import me.flex.proauction.auction.BanManager;
import me.flex.proauction.commands.AuctionCommand;
import me.flex.proauction.economy.CurrencyRegistry;
import me.flex.proauction.economy.EconomyService;
import me.flex.proauction.gui.GuiListener;
import me.flex.proauction.storage.YamlDataStore;
import me.flex.proauction.update.UpdateChecker;
import org.bukkit.NamespacedKey;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;

public class ProAuctionPlugin extends JavaPlugin {

    private static ProAuctionPlugin instance;

    private CurrencyRegistry currencyRegistry;
    private EconomyService economyService;
    private AuctionManager auctionManager;
    private BanManager banManager;

    private YamlDataStore dataStore;

    private FileConfiguration guiConfig;
    private FileConfiguration messagesConfig;
    private FileConfiguration settingsConfig;
    private FileConfiguration bansConfig;

    private NamespacedKey listingIdKey;

    @Override
    public void onEnable() {
        instance = this;

        listingIdKey = new NamespacedKey(this, "listing_id");

        currencyRegistry = new CurrencyRegistry(this);
        currencyRegistry.load();

        economyService = new EconomyService(this);
        auctionManager = new AuctionManager();
        banManager = new BanManager(this);

        loadGuiConfig();
        loadMessagesConfig();
        loadSettingsConfig();
        loadBansConfig();
        banManager.loadFromConfig();

        dataStore = new YamlDataStore(this);
        auctionManager.setAll(dataStore.loadListings(), dataStore.loadExpired());

        // Update checker (reads settings.yml -> updates.*)
        UpdateChecker.checkAsync(this);

        // Process expirations periodically + save as a safety net
        getServer().getScheduler().runTaskTimer(this, () -> {
            auctionManager.processExpirations();
            saveDataNow();
        }, 20L * 10, 20L * 30);

        getCommand("ah").setExecutor(new AuctionCommand(this));
        getServer().getPluginManager().registerEvents(new GuiListener(this), this);

        getLogger().info("ProAuction enabled");
    }

    @Override
    public void onDisable() {
        saveDataNow();
        getLogger().info("ProAuction disabled");
    }

    public void saveDataNow() {
        if (dataStore == null || auctionManager == null) return;
        dataStore.writeAll(auctionManager.getListingsSnapshot(), auctionManager.getExpiredSnapshot());
    }

    private void loadGuiConfig() {
        File file = new File(getDataFolder(), "gui.yml");
        if (!file.exists()) saveResource("gui.yml", false);
        guiConfig = YamlConfiguration.loadConfiguration(file);
    }

    private void loadMessagesConfig() {
        File file = new File(getDataFolder(), "messages.yml");
        if (!file.exists()) saveResource("messages.yml", false);
        messagesConfig = YamlConfiguration.loadConfiguration(file);
    }

    private void loadSettingsConfig() {
        File file = new File(getDataFolder(), "settings.yml");
        if (!file.exists()) saveResource("settings.yml", false);
        settingsConfig = YamlConfiguration.loadConfiguration(file);
    }

    private void loadBansConfig() {
        File file = new File(getDataFolder(), "bans.yml");
        if (!file.exists()) saveResource("bans.yml", false);
        bansConfig = YamlConfiguration.loadConfiguration(file);
    }

    public void reloadGuiConfig() { loadGuiConfig(); }
    public void reloadMessagesConfig() { loadMessagesConfig(); }
    public void reloadSettingsConfig() { loadSettingsConfig(); }
    public void reloadBansConfig() { loadBansConfig(); }

    public static ProAuctionPlugin getInstance() { return instance; }

    public CurrencyRegistry getCurrencyRegistry() { return currencyRegistry; }
    public EconomyService getEconomyService() { return economyService; }
    public AuctionManager getAuctionManager() { return auctionManager; }
    public BanManager getBanManager() { return banManager; }

    public FileConfiguration getGuiConfig() { return guiConfig; }
    public FileConfiguration getMessagesConfig() { return messagesConfig; }
    public FileConfiguration getSettingsConfig() { return settingsConfig; }
    public FileConfiguration getBansConfig() { return bansConfig; }

    public NamespacedKey getListingIdKey() { return listingIdKey; }
}
