package me.flex.proauction.util;

import org.bukkit.ChatColor;

public final class Text {
    private Text() {}

    public static String color(String s) {
        return ChatColor.translateAlternateColorCodes('&', s);
    }
}
