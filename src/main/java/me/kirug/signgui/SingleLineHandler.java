package me.kirug.signgui;

import org.bukkit.entity.Player;

/**
 * Functional interface for handling input from a single line of a sign.
 */
@FunctionalInterface
public interface SingleLineHandler {
    /**
     * Called when the player finishes editing the sign.
     * @param player The player who edited the sign.
     * @param lineContent The content of the specific line requested.
     */
    void onInput(Player player, String lineContent);
}
