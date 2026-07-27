package me.kirug.signgui;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.wrappers.BlockPosition;
import com.comphenix.protocol.wrappers.WrappedBlockData;
import org.bukkit.Material;
import org.bukkit.entity.Player;

import java.util.List;


public class SignPacketManager {

    private static final int SIGN_OFFSET = -5;


    private static volatile Boolean openSignPacketTakesFrontTextArg = null;


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
            PacketContainer openSign = buildOpenSignPacket(signPosition);

            ProtocolLibrary.getProtocolManager().sendServerPacket(player, openSign);

        } catch (Exception e) {
            System.err.println("[SignGuiAPI] Failed to open sign editor for " + player.getName());
            e.printStackTrace();
        }
    }

    private static PacketContainer buildOpenSignPacket(BlockPosition signPosition) throws Exception {
        Boolean takesFrontText = openSignPacketTakesFrontTextArg;

        if (takesFrontText == null || takesFrontText) {
            try {
                PacketContainer packet = ProtocolLibrary.getProtocolManager()
                        .createPacketConstructor(PacketType.Play.Server.OPEN_SIGN_EDITOR, signPosition, true)
                        .createPacket(signPosition, true);
                openSignPacketTakesFrontTextArg = true;
                return packet;
            } catch (Exception e) {
                if (takesFrontText != null) {
                    throw e; // already confirmed this server supports it; a real failure
                }
                // Fall through to try the older single-arg constructor below.
            }
        }

        PacketContainer packet = new PacketContainer(PacketType.Play.Server.OPEN_SIGN_EDITOR);
        packet.getBlockPositionModifier().write(0, signPosition);
        openSignPacketTakesFrontTextArg = false;
        return packet;
    }

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
