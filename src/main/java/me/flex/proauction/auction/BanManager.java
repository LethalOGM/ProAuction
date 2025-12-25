package me.flex.proauction.auction;

import me.flex.proauction.ProAuctionPlugin;
import org.bukkit.Material;
import org.bukkit.configuration.file.FileConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class BanManager {

    private final ProAuctionPlugin plugin;
    private final Set<Material> banned = new HashSet<>();

    public BanManager(ProAuctionPlugin plugin) {
        this.plugin = plugin;
    }

    public void loadFromConfig() {
        banned.clear();

        FileConfiguration cfg = plugin.getBansConfig();
        List<String> list = cfg.getStringList("banned.materials");
        for (String s : list) {
            Material m = Material.matchMaterial(s);
            if (m != null) banned.add(m);
        }
    }

    public boolean toggle(Material mat) {
        if (banned.contains(mat)) {
            banned.remove(mat);
            save();
            return false;
        } else {
            banned.add(mat);
            save();
            return true;
        }
    }

    public boolean isBanned(Material mat) {
        return banned.contains(mat);
    }

    private void save() {
        FileConfiguration cfg = plugin.getBansConfig();
        cfg.set("banned.materials", banned.stream().map(Material::name).sorted().toList());

        File file = new File(plugin.getDataFolder(), "bans.yml");
        try {
            cfg.save(file);
        } catch (IOException ignored) {
        }
    }
}
