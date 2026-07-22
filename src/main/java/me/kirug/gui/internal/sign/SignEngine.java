package me.kirug.gui.internal.sign;

import me.kirug.gui.internal.Schedulers;
import me.kirug.gui.internal.net.NettyChannels;
import me.kirug.gui.internal.version.ServerVersion;
import me.kirug.signgui.SignGUI;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.Plugin;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

// Injects the per-player reader, opens the virtual editor, and turns the client's reply into a
// callback on the right thread. The fake sign sits a few blocks under the player and is reverted
// the moment input arrives.
public final class SignEngine {

    private static final int BELOW_PLAYER = -4;

    private static final ConcurrentHashMap<UUID, Pending> PENDING = new ConcurrentHashMap<>();

    private static Plugin plugin;
    private static String handlerName;
    private static volatile boolean initialized;

    private SignEngine() {
    }

    private record Pending(SignGUI.SignInputHandler handler, Object blockPos) {
    }

    public static synchronized void init(Plugin owner) {
        if (initialized) {
            return;
        }
        plugin = owner;
        handlerName = "signgui_reader_" + owner.getName();

        owner.getServer().getPluginManager().registerEvents(new Listener() {
            @EventHandler
            public void onJoin(PlayerJoinEvent event) {
                inject(event.getPlayer());
            }

            @EventHandler
            public void onQuit(PlayerQuitEvent event) {
                PENDING.remove(event.getPlayer().getUniqueId());
                NettyChannels.eject(event.getPlayer(), handlerName);
            }
        }, owner);

        for (Player online : owner.getServer().getOnlinePlayers()) {
            inject(online);
        }
        initialized = true;
    }

    public static synchronized void shutdown() {
        if (!initialized) {
            return;
        }
        for (Player online : plugin.getServer().getOnlinePlayers()) {
            NettyChannels.eject(online, handlerName);
        }
        PENDING.clear();
        initialized = false;
    }

    private static void inject(Player player) {
        try {
            NettyChannels.inject(player, handlerName, packet -> onInbound(player, packet));
        } catch (RuntimeException e) {
            plugin.getLogger().warning("SignGUI: could not inject reader for " + player.getName()
                    + " (" + e.getMessage() + ")");
        }
    }

    public static void open(Player player, List<String> lines, Material signType, SignGUI.SignInputHandler handler) {
        if (player == null || !player.isOnline()) {
            return;
        }
        // Close any open container first - the client ignores the sign editor while another
        // screen is open (e.g. when opening a sign from a chest-menu click). A short delay lets
        // that close settle before we open the sign.
        Schedulers.forEntity(plugin, player, player::closeInventory);
        Schedulers.forEntityLater(plugin, player, () -> {
            if (!player.isOnline()) {
                return;
            }
            try {
                Location loc = player.getLocation();
                int minY = ServerVersion.current().minor() >= 17 ? player.getWorld().getMinHeight() : 0;
                int x = loc.getBlockX();
                int y = Math.max(minY, loc.getBlockY() + BELOW_PLAYER);
                int z = loc.getBlockZ();

                Object pos = SignPackets.blockPos(x, y, z);
                Object blockChange = SignPackets.blockChangePacket(pos, signType);
                Object openEditor = SignPackets.openSignPacket(pos);

                // Register before sending so a fast reply can't race us.
                PENDING.put(player.getUniqueId(), new Pending(handler, pos));
                SignPackets.send(player, blockChange, openEditor);
            } catch (RuntimeException e) {
                PENDING.remove(player.getUniqueId());
                plugin.getLogger().warning("SignGUI: failed to open editor for " + player.getName()
                        + " (" + e.getMessage() + ")");
            }
        }, 2L);
    }

    // Runs on a Netty thread. Returns true when we handled (and should drop) the packet.
    private static boolean onInbound(Player player, Object packet) {
        if (!SignPackets.isUpdateSign(packet)) {
            return false;
        }
        Pending pending = PENDING.remove(player.getUniqueId());
        if (pending == null) {
            return false; // a real sign edit, not ours
        }

        String[] lines = SignPackets.readLines(packet);
        Schedulers.forEntity(plugin, player, () -> {
            try {
                Object air = SignPackets.blockChangePacket(pending.blockPos(), Material.AIR);
                SignPackets.send(player, air);
            } catch (RuntimeException ignored) {
                // Player may have moved regions or logged off.
            }
            try {
                pending.handler().onInput(player, lines);
            } catch (Exception e) {
                plugin.getLogger().severe("SignGUI: handler threw for " + player.getName());
                e.printStackTrace();
            }
        });
        return true;
    }
}
