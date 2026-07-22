package me.kirug.anvilgui;

import me.kirug.gui.internal.Schedulers;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.event.inventory.PrepareAnvilEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.AnvilInventory;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.Plugin;

import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

// Anvil text input for Paper/Folia 1.16.5-1.21.x. No NMS: opens a real anvil, reads whatever the
// player types in the rename box, hands it back when they click the output slot.
public class AnvilGUI {

    @FunctionalInterface
    public interface AnvilInputHandler {
        void onInput(Player player, String input);
    }

    private static boolean initialized = false;
    private static Plugin pluginInstance;
    private static final ConcurrentHashMap<UUID, Session> SESSIONS = new ConcurrentHashMap<>();

    private final String title;
    private final String initialText;
    private final Material leftItem;
    private final boolean allowEmpty;
    private final AnvilInputHandler handler;

    private AnvilGUI(String title, String initialText, Material leftItem, boolean allowEmpty, AnvilInputHandler handler) {
        this.title = title;
        this.initialText = initialText;
        this.leftItem = leftItem;
        this.allowEmpty = allowEmpty;
        this.handler = handler;
    }

    // Marker holder so we can recognise our own anvils in the listeners.
    private static final class AnvilHolder implements InventoryHolder {
        private Inventory inventory;

        @Override
        public Inventory getInventory() {
            return inventory;
        }
    }

    private record Session(AnvilInputHandler handler, Material leftItem, boolean allowEmpty) {
    }

    public static synchronized void init(Plugin plugin) {
        if (initialized) {
            return;
        }
        pluginInstance = plugin;
        plugin.getServer().getPluginManager().registerEvents(new AnvilListener(), plugin);
        initialized = true;
    }

    public static Builder builder() {
        if (!initialized) {
            throw new IllegalStateException("AnvilGUI is not initialized! Call AnvilGUI.init(plugin) first.");
        }
        return new Builder();
    }

    public void open(Player player) {
        if (player == null || !player.isOnline()) {
            return;
        }
        Schedulers.forEntity(pluginInstance, player, () -> {
            if (!player.isOnline()) {
                return;
            }
            AnvilHolder holder = new AnvilHolder();
            Inventory inventory = createAnvil(holder, title);
            if (inventory == null) {
                player.sendMessage("Anvil GUI is unavailable on this server version.");
                return;
            }
            holder.inventory = inventory;

            ItemStack left = new ItemStack(leftItem);
            ItemMeta meta = left.getItemMeta();
            if (meta != null && initialText != null) {
                meta.setDisplayName(initialText);
                left.setItemMeta(meta);
            }
            inventory.setItem(0, left);

            SESSIONS.put(player.getUniqueId(), new Session(handler, leftItem, allowEmpty));
            player.openInventory(inventory);
        });
    }

    private static Inventory createAnvil(AnvilHolder holder, String title) {
        try {
            if (title != null && !title.isEmpty()) {
                return Bukkit.createInventory(holder, InventoryType.ANVIL, title);
            }
            return Bukkit.createInventory(holder, InventoryType.ANVIL);
        } catch (RuntimeException titled) {
            try {
                return Bukkit.createInventory(holder, InventoryType.ANVIL);
            } catch (RuntimeException e) {
                if (pluginInstance != null) {
                    pluginInstance.getLogger().warning("AnvilGUI: could not create anvil inventory (" + e.getMessage() + ")");
                }
                return null;
            }
        }
    }

    private static boolean isOurAnvil(Inventory inventory) {
        return inventory != null && inventory.getHolder() instanceof AnvilHolder;
    }

    private static final class AnvilListener implements Listener {

        // Keep a result item in the output slot so the player can always click to confirm.
        @EventHandler
        public void onPrepare(PrepareAnvilEvent event) {
            if (!isOurAnvil(event.getInventory())) {
                return;
            }
            AnvilInventory anvil = event.getInventory();
            String text = anvil.getRenameText();

            ItemStack left = anvil.getItem(0);
            Material material = left != null ? left.getType() : Material.PAPER;
            ItemStack result = new ItemStack(material);
            ItemMeta meta = result.getItemMeta();
            if (meta != null) {
                meta.setDisplayName(text != null ? text : "");
                result.setItemMeta(meta);
            }
            event.setResult(result);
            try {
                anvil.setRepairCost(0);
            } catch (Throwable ignored) {
                // Older APIs may not expose setRepairCost.
            }
        }

        @EventHandler
        public void onClick(InventoryClickEvent event) {
            Inventory top = event.getView().getTopInventory();
            if (!isOurAnvil(top)) {
                return;
            }
            event.setCancelled(true); // read-only except the confirm click

            if (!(event.getWhoClicked() instanceof Player player)) {
                return;
            }
            if (event.getRawSlot() != 2) {
                return;
            }
            Session session = SESSIONS.get(player.getUniqueId());
            if (session == null) {
                return;
            }
            AnvilInventory anvil = (AnvilInventory) top;
            String input = anvil.getRenameText();
            if (input == null) {
                input = "";
            }
            if (!session.allowEmpty() && input.trim().isEmpty()) {
                return;
            }

            String finalInput = input;
            SESSIONS.remove(player.getUniqueId());
            player.closeInventory();
            try {
                session.handler().onInput(player, finalInput);
            } catch (Exception e) {
                if (pluginInstance != null) {
                    pluginInstance.getLogger().severe("AnvilGUI: handler threw for " + player.getName());
                }
                e.printStackTrace();
            }
        }

        @EventHandler
        public void onClose(InventoryCloseEvent event) {
            if (isOurAnvil(event.getInventory())) {
                SESSIONS.remove(event.getPlayer().getUniqueId());
            }
        }

        @EventHandler
        public void onQuit(PlayerQuitEvent event) {
            SESSIONS.remove(event.getPlayer().getUniqueId());
        }
    }

    public static class Builder {
        private String title = "";
        private String initialText = "";
        private Material leftItem = Material.PAPER;
        private boolean allowEmpty = true;
        private AnvilInputHandler handler;

        public Builder title(String title) {
            this.title = title != null ? title : "";
            return this;
        }

        public Builder text(String text) {
            this.initialText = text != null ? text : "";
            return this;
        }

        public Builder item(Material material) {
            if (material != null && !material.isAir()) {
                this.leftItem = material;
            }
            return this;
        }

        public Builder disallowEmpty() {
            this.allowEmpty = false;
            return this;
        }

        public Builder setHandler(AnvilInputHandler handler) {
            this.handler = handler;
            return this;
        }

        public AnvilGUI build() {
            if (handler == null) {
                throw new IllegalStateException("Handler must be set before building AnvilGUI");
            }
            return new AnvilGUI(title, initialText, leftItem, allowEmpty, handler);
        }
    }
}
