package me.kirug.signgui;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.Arrays;

/**
 * Demonstrates how to use SignGuiAPI within a Menu/Inventory context.
 */
public class MenuTestCommand implements CommandExecutor, Listener {

    private final SignGuiPlugin plugin;

    public MenuTestCommand(SignGuiPlugin plugin) {
        this.plugin = plugin;
        Bukkit.getPluginManager().registerEvents(this, plugin);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) return true;
        openMenu((Player) sender, "Change This Item");
        return true;
    }

    private void openMenu(Player player, String currentName) {
        Inventory inv = Bukkit.createInventory(null, 9, "Menu Integration Test");

        ItemStack item = new ItemStack(Material.NAME_TAG);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(currentName);
        meta.setLore(Arrays.asList("Click to rename via Sign GUI"));
        item.setItemMeta(meta);

        inv.setItem(4, item);
        player.openInventory(inv);
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!event.getView().getTitle().equals("Menu Integration Test")) return;
        event.setCancelled(true);

        if (event.getCurrentItem() == null || event.getCurrentItem().getType() == Material.AIR) return;

        Player player = (Player) event.getWhoClicked();
        
        // 1. Close inventory (optional, but clean)
        player.closeInventory();

        // 2. Open Sign GUI
        SignGUI.builder()
                .setLines("", "^ Enter Name ^", "", "")
                .setHandler((p, lines) -> {
                    // Logic to handle input
                    String newName = lines[0];
                    if (newName.isEmpty()) newName = "Default Name";

                    // 3. Re-open menu with updated state
                    // Since we are already on the scheduler thread in the callback, 
                    // we can just call the method directly.
                    openMenu(p, newName);
                    
                    p.sendMessage("Updated item name to: " + newName);
                })
                .build()
                .open(player);
    }
}
