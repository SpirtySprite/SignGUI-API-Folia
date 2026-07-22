package me.kirug.signgui;

import me.kirug.gui.internal.sign.SignEngine;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

// Sign input GUI for Paper/Folia 1.16.5-1.21.x. No ProtocolLib: the editor is sent with NMS
// packets built by reflection, and the reply is read through a Netty handler on the connection.
public class SignGUI {

    private static boolean initialized = false;
    private static Plugin pluginInstance;

    @FunctionalInterface
    public interface SignInputHandler {
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

    public static synchronized void init(Plugin plugin) {
        if (initialized) {
            return;
        }
        pluginInstance = plugin;
        SignEngine.init(plugin);
        initialized = true;
        plugin.getLogger().info("SignGuiAPI initialized (dependency-free).");
    }

    public static synchronized void shutdown() {
        if (!initialized) {
            return;
        }
        SignEngine.shutdown();
        initialized = false;
    }

    public static Builder builder() {
        if (!initialized) {
            throw new IllegalStateException("SignGuiAPI is not initialized! Call SignGUI.init(plugin) first.");
        }
        return new Builder();
    }

    public void open(Player player) {
        if (player == null || !player.isOnline()) {
            return;
        }
        SignEngine.open(player, lines, signType, handler);
    }

    public static class Builder {
        private final List<String> lines = new ArrayList<>(Arrays.asList("", "", "", ""));
        private SignInputHandler handler;
        private Material signType = Material.OAK_SIGN;

        public Builder setLine(int index, String text) {
            if (index >= 0 && index < 4) {
                lines.set(index, text != null ? text : "");
            }
            return this;
        }

        public Builder setLines(String... lines) {
            for (int i = 0; i < Math.min(this.lines.size(), lines.length); i++) {
                this.lines.set(i, lines[i] != null ? lines[i] : "");
            }
            return this;
        }

        public Builder type(Material type) {
            if (type != null && type.name().endsWith("_SIGN") && !type.name().contains("HANGING")) {
                this.signType = type;
            } else {
                if (pluginInstance != null) {
                    pluginInstance.getLogger().warning("Invalid sign material: " + type
                            + ". Use a standard standing or wall sign.");
                }
                this.signType = type != null ? type : Material.OAK_SIGN;
            }
            return this;
        }

        public Builder setHandler(SignInputHandler handler) {
            this.handler = handler;
            return this;
        }

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

        public SignGUI build() {
            if (handler == null) {
                throw new IllegalStateException("Handler must be set before building SignGUI");
            }
            return new SignGUI(lines, handler, signType);
        }
    }
}
