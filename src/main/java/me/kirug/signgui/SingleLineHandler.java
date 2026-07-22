package me.kirug.signgui;

import org.bukkit.entity.Player;

@FunctionalInterface
public interface SingleLineHandler {
    void onInput(Player player, String lineContent);
}
