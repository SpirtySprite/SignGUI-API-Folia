package me.kirug.signgui;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.wrappers.BlockPosition;
import com.comphenix.protocol.wrappers.WrappedBlockData;
import org.bukkit.Material;
import org.bukkit.entity.Player;

import java.util.List;

/**
 * Manages the construction and sending of packets for the Virtual Sign GUI using ProtocolLib.
 */
public class SignPacketManager {

    // 5 blocks below the player ensures chunk loading while keeping it out of sight
    private static final int SIGN_OFFSET = -5;

    /**
     * Opens a virtual sign editor for the player.
     * @param player The player to open the editor for.
     * @param lines Initial lines to display.
     * @param signType The material type of the sign.
     */
    public static void openSignEditor(Player player, List<String> lines, Material signType) {
        if (player == null || !player.isOnline()) {
            return;
        }

        // Calculate position relative to player
        int x = player.getLocation().getBlockX();
        int y = Math.max(player.getWorld().getMinHeight(), player.getLocation().getBlockY() + SIGN_OFFSET);
        int z = player.getLocation().getBlockZ();

        BlockPosition signPosition = new BlockPosition(x, y, z);

        try {
            // 1. Send BlockChange packet to turn the block into a sign (Virtual)
            PacketContainer blockChange = new PacketContainer(PacketType.Play.Server.BLOCK_CHANGE);
            blockChange.getBlockPositionModifier().write(0, signPosition);
            blockChange.getBlockData().write(0, WrappedBlockData.createData(signType));

            ProtocolLibrary.getProtocolManager().sendServerPacket(player, blockChange);

            // 2. Open the Sign Editor
            PacketContainer openSign = new PacketContainer(PacketType.Play.Server.OPEN_SIGN_EDITOR);
            openSign.getBlockPositionModifier().write(0, signPosition);
            openSign.getBooleans().write(0, true); // isFrontText

            ProtocolLibrary.getProtocolManager().sendServerPacket(player, openSign);

        } catch (Exception e) {
            System.err.println("[SignGuiAPI] Failed to open sign editor for " + player.getName());
            e.printStackTrace();
        }
    }

    /**
     * Reverts the virtual sign block to AIR (Cleanup).
     * @param player The player to send the packet to.
     * @param position The position of the sign to remove.
     */
    public static void cleanUpSign(Player player, BlockPosition position) {
        if (player == null || !player.isOnline()) {
            return;
        }

        try {
            PacketContainer blockChange = new PacketContainer(PacketType.Play.Server.BLOCK_CHANGE);
            blockChange.getBlockPositionModifier().write(0, position);
            blockChange.getBlockData().write(0, WrappedBlockData.createData(Material.AIR));

            ProtocolLibrary.getProtocolManager().sendServerPacket(player, blockChange);
        } catch (Exception e) {
            System.err.println("[SignGuiAPI] Failed to cleanup sign for " + player.getName());
            e.printStackTrace();
        }
    }
}
