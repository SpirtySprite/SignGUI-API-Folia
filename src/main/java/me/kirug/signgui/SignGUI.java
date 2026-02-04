package me.kirug.signgui;


import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.event.Listener;
import org.bukkit.event.EventHandler;
import org.bukkit.event.player.PlayerQuitEvent;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * A professional Sign GUI API for Folia and Paper servers.
 * <p>
 * Usage:
 * <pre>
 * SignGUI.builder()
 *     .type(Material.DARK_OAK_SIGN)
 *     .setLines("Hello", "World")
 *     .setHandler((player, lines) -> { ... })
 *     .build()
 *     .open(player);
 * </pre>
 */
public class SignGUI {

    private static final ConcurrentHashMap<UUID, SignInputHandler> openSignEditors = new ConcurrentHashMap<>();
    private static boolean initialized = false;
    private static Plugin pluginInstance;

    /**
     * Callback handler for when a player inputs text into the sign.
     */
    public interface SignInputHandler {
        /**
         * Called when the player finishes editing the sign.
         * @param player The player who edited the sign.
         * @param lines The lines of text entered by the player.
         */
        void onInput(Player player, String[] lines);
    }

    private final List<String> lines;
    private final SignInputHandler handler;
    private final Material signType;

    private SignGUI(List<String> lines, SignInputHandler handler, Material signType) {
        this.lines = lines;
        this.handler = handler;
        this.signType = signType;
    }

    /**
     * Initializes the SignGUI API.
     * <p>
     * This MUST be called once by your plugin in onEnable.
     * 
     * @param plugin Your plugin instance.
     */
    public static synchronized void init(Plugin plugin) {
        if (initialized) {
            return;
        }
        pluginInstance = plugin;
        
        // Register the ProtocolLib listener
        com.comphenix.protocol.ProtocolLibrary.getProtocolManager().addPacketListener(new SignPacketListener(plugin));

        // Register Quit Listener to prevent leaks
        plugin.getServer().getPluginManager().registerEvents(new Listener() {
            @EventHandler
            public void onQuit(PlayerQuitEvent event) {
                openSignEditors.remove(event.getPlayer().getUniqueId());
            }
        }, plugin);
        
        initialized = true;
        plugin.getLogger().info("SignGuiAPI initialized.");
    }
    
    /**
     * Creates a new SignGUI Builder.
     * @return A new Builder instance.
     */
    public static Builder builder() {
        if (!initialized) {
            throw new IllegalStateException("SignGuiAPI is not initialized! Call SignGUI.init(plugin) first.");
        }
        return new Builder();
    }

    /**
     * Opens the virtual sign editor for the player.
     * @param player The player to open the sign for.
     */
    public void open(Player player) {
        if (player == null || !player.isOnline()) return;
        
        // Register the handler for this player
        openSignEditors.put(player.getUniqueId(), handler);

        // Open the editor
        SignPacketManager.openSignEditor(player, lines, signType);
    }

    /**
     * Internal method to retrieve and remove the handler for a player.
     * @param playerId The UUID of the player.
     * @return The handler, or null if none found.
     */
    protected static SignInputHandler getHandler(UUID playerId) {
        return openSignEditors.remove(playerId);
    }

    public static class Builder {
        private final List<String> lines = new ArrayList<>(Arrays.asList("", "", "", ""));
        private SignInputHandler handler;
        private Material signType = Material.OAK_SIGN;

        /**
         * Sets a specific line of text on the sign.
         * @param index Line index (0-3).
         * @param text Text to display.
         * @return The builder instance.
         */
        public Builder setLine(int index, String text) {
            if (index >= 0 && index < 4) {
                lines.set(index, text != null ? text : "");
            }
            return this;
        }

        /**
         * Sets all lines of text on the sign.
         * @param lines The lines to display.
         * @return The builder instance.
         */
        public Builder setLines(String... lines) {
            for (int i = 0; i < Math.min(this.lines.size(), lines.length); i++) {
                this.lines.set(i, lines[i] != null ? lines[i] : "");
            }
            return this;
        }

        /**
         * Sets the material type of the sign (e.g. OAK_SIGN, SPRUCE_SIGN).
         * @param type The material to use.
         * @return The builder instance.
         */
        public Builder type(Material type) {
            if (type != null && type.name().endsWith("_SIGN") && !type.name().contains("HANGING")) {
                this.signType = type;
            } else {
                 // Warn or fallback? For now, we allow it but it might look like oak if invalid in packet
                 if (pluginInstance != null) pluginInstance.getLogger().warning("Invalid Sign Material: " + type + ". It should be a standard wall/standing sign.");
                 this.signType = type != null ? type : Material.OAK_SIGN;
            }
            return this;
        }

        /**
         * Sets the handler to call when input is received.
         * @param handler The input handler.
         * @return The builder instance.
         */
        public Builder setHandler(SignInputHandler handler) {
            this.handler = handler;
            return this;
        }

        /**
         * Convenience method to set a handler that only cares about one specific line of input.
         * @param line The line index (0-3) to retrieve.
         * @param handler The single line handler.
         * @return The builder instance.
         */
        public Builder setHandler(int line, SingleLineHandler handler) {
            this.handler = (player, lines) -> {
                if (line >= 0 && line < lines.length) {
                    handler.onInput(player, lines[line]);
                } else {
                    handler.onInput(player, "");
                }
            };
            return this;
        }

        /**
         * Builds the SignGUI instance.
         * @return The ready-to-use SignGUI.
         * @throws IllegalStateException if no handler is set.
         */
        public SignGUI build() {
            if (handler == null) {
                throw new IllegalStateException("Handler must be set before building SignGUI");
            }
            return new SignGUI(lines, handler, signType);
        }
    }
}
