package me.kirug.signgui;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.events.PacketAdapter;
import com.comphenix.protocol.events.PacketEvent;
import com.comphenix.protocol.wrappers.BlockPosition;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

public class SignPacketListener extends PacketAdapter {

    private final Plugin plugin;

    public SignPacketListener(Plugin plugin) {
        super(plugin, PacketType.Play.Client.UPDATE_SIGN);
        this.plugin = plugin;
    }

    @Override
    public void onPacketReceiving(PacketEvent event) {
        if (event.getPacketType() == PacketType.Play.Client.UPDATE_SIGN) {
            Player player = event.getPlayer();
            if (player == null) return;

            SignGUI.SignInputHandler handler = SignGUI.getHandler(player.getUniqueId());

            if (handler != null) {
                // Read lines from the packet
                String[] lines = event.getPacket().getStringArrays().read(0);
                BlockPosition position = event.getPacket().getBlockPositionModifier().read(0);

                // Run on the player's scheduler (Folia support)
                player.getScheduler().run(plugin, (task) -> {
                    // Cleanup: Revert the sign block to air BEFORE the callback
                    SignPacketManager.cleanUpSign(player, position);

                    try {
                        handler.onInput(player, lines);
                    } catch (Exception e) {
                        plugin.getLogger().severe("Error in SignGUI callback for " + player.getName());
                        e.printStackTrace();
                    }
                }, null);

                event.setCancelled(true);
            }
        }
    }
}
