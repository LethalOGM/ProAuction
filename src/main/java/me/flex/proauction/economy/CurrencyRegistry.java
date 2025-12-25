package me.flex.proauction.economy;

import me.flex.proauction.util.Text;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.util.*;

public class CurrencyRegistry {

    private final JavaPlugin plugin;
    private final Map<String, Currency> currencies = new LinkedHashMap<>();

    public CurrencyRegistry(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    public void load() {
        currencies.clear();

        File file = new File(plugin.getDataFolder(), "currencies.yml");
        if (!file.exists()) {
            plugin.saveResource("currencies.yml", false);
        }

        YamlConfiguration cfg = YamlConfiguration.loadConfiguration(file);
        ConfigurationSection root = cfg.getConfigurationSection("currencies");
        if (root == null) return;

        for (String key : root.getKeys(false)) {
            ConfigurationSection cs = root.getConfigurationSection(key);
            if (cs == null) continue;

            String display = Text.color(cs.getString("display-name", key));
            String symbol = cs.getString("symbol", "");
            String typeStr = cs.getString("type", "VAULT").toUpperCase(Locale.ROOT);

            Currency.CurrencyType type;
            try {
                type = Currency.CurrencyType.valueOf(typeStr);
            } catch (Exception e) {
                plugin.getLogger().warning("Invalid currency type for '" + key + "': " + typeStr);
                continue;
            }

            String placeholder = cs.getString("placeholder", null);
            String setCmd = cs.getString("set-command", null);

            currencies.put(key.toLowerCase(Locale.ROOT), new Currency(
                    key.toLowerCase(Locale.ROOT),
                    display,
                    symbol,
                    type,
                    placeholder,
                    setCmd
            ));
        }
    }

    public Optional<Currency> get(String key) {
        if (key == null) return Optional.empty();
        return Optional.ofNullable(currencies.get(key.toLowerCase(Locale.ROOT)));
    }

    public Collection<Currency> all() {
        return Collections.unmodifiableCollection(currencies.values());
    }
}
