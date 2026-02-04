package me.kirug.signgui;

import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class TestCommand implements CommandExecutor {

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("Only players can use this command.");
            return true;
        }

        Player player = (Player) sender;

        // Example 1: Standard input with Dark Oak Sign
        if (args.length > 0 && args[0].equalsIgnoreCase("dark")) {
            SignGUI.builder()
                    .type(Material.DARK_OAK_SIGN)
                    .setLines("Type something", "on this", "Dark Oak", "Sign!")
                    .setHandler((p, lines) -> {
                        p.sendMessage("§aYou wrote on Dark Oak:");
                        for (String line : lines) {
                            p.sendMessage("§7- " + line);
                        }
                    })
                    .build()
                    .open(player);
            return true;
        }

        // Example 2: Single line input (Line 0) with Spruce Sign
        // Usage: /signinput single
        if (args.length > 0 && args[0].equalsIgnoreCase("single")) {
             SignGUI.builder()
                    .type(Material.SPRUCE_SIGN)
                    .setLine(0, "Enter Name Here")
                    .setLine(1, "^^^^^^^^^")
                    .setHandler(0, (p, result) -> {
                        p.sendMessage("§eHello, " + result + "!");
                    })
                    .build()
                    .open(player);
             return true;
        }

        // Default: Oak Sign, full output
        SignGUI.builder()
                .setLines("Default", "Oak", "Sign", "Test")
                .setHandler((p, lines) -> {
                    p.sendMessage("§bStandard Input Received.");
                })
                .build()
                .open(player);

        return true;
    }
}
