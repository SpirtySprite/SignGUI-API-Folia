package me.kirug.gui.internal.sign;

import me.kirug.gui.internal.net.NettyChannels;
import me.kirug.gui.internal.reflect.Reflect;
import me.kirug.gui.internal.version.ServerVersion;
import org.bukkit.Material;
import org.bukkit.entity.Player;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;

// Builds and sends the two outbound sign packets and parses the inbound one. The block state
// for the fake sign comes from Bukkit's BlockData#getState() bridge, so we never touch numeric
// block ids or the blockstate registry.
public final class SignPackets {

    private static Class<?> blockPosClass;
    private static Class<?> packetClass;
    private static Class<?> blockChangeClass;
    private static Class<?> openSignClass;
    private static Class<?> updateSignClass;

    private static Constructor<?> blockPosCtor;
    private static Constructor<?> blockChangeCtor;
    private static Constructor<?> openSignCtor;

    private static Method blockDataToState;
    private static Method sendMethod;

    private static Field updateSignLinesField;

    private SignPackets() {
    }

    private static void resolve() {
        if (blockPosClass != null) {
            return;
        }
        blockPosClass = Reflect.nms("core", "BlockPos", "BlockPosition");
        packetClass = Reflect.nms("network", "Packet", "Packet");
        blockChangeClass = Reflect.nms("network.protocol.game", "ClientboundBlockUpdatePacket", "PacketPlayOutBlockChange");
        openSignClass = Reflect.nms("network.protocol.game", "ClientboundOpenSignEditorPacket", "PacketPlayOutOpenSignEditor");
        updateSignClass = Reflect.nms("network.protocol.game", "ServerboundSignUpdatePacket", "PacketPlayInUpdateSign");

        blockPosCtor = Reflect.constructor(blockPosClass, int.class, int.class, int.class);
        blockChangeCtor = Reflect.constructorWithArgCount(blockChangeClass, 2);
        openSignCtor = Reflect.constructorWithArgCount(openSignClass, ServerVersion.current().twoSidedSigns() ? 2 : 1);
    }

    public static Object blockPos(int x, int y, int z) {
        resolve();
        return Reflect.newInstance(blockPosCtor, x, y, z);
    }

    public static Object blockChangePacket(Object blockPos, Material material) {
        resolve();
        Object craftBlockData = material.createBlockData();
        if (blockDataToState == null) {
            try {
                blockDataToState = craftBlockData.getClass().getMethod("getState");
                blockDataToState.setAccessible(true);
            } catch (NoSuchMethodException e) {
                throw new IllegalStateException("CraftBlockData#getState() missing on " + ServerVersion.current(), e);
            }
        }
        Object blockState = Reflect.invoke(blockDataToState, craftBlockData);
        return Reflect.newInstance(blockChangeCtor, blockPos, blockState);
    }

    public static Object openSignPacket(Object blockPos) {
        resolve();
        if (ServerVersion.current().twoSidedSigns()) {
            return Reflect.newInstance(openSignCtor, blockPos, true);
        }
        return Reflect.newInstance(openSignCtor, blockPos);
    }

    public static void send(Player player, Object... packets) {
        resolve();
        Object connection = NettyChannels.connection(player);
        if (sendMethod == null) {
            sendMethod = Reflect.methodTaking(connection.getClass(), packetClass);
        }
        for (Object packet : packets) {
            Reflect.invoke(sendMethod, connection, packet);
        }
    }

    public static boolean isUpdateSign(Object packet) {
        resolve();
        return updateSignClass.isInstance(packet);
    }

    public static String[] readLines(Object updateSignPacket) {
        resolve();
        if (updateSignLinesField == null) {
            updateSignLinesField = Reflect.fieldOfType(updateSignClass, String[].class);
        }
        Object raw = Reflect.read(updateSignLinesField, updateSignPacket);
        if (raw instanceof String[]) {
            return (String[]) raw;
        }
        return new String[]{"", "", "", ""};
    }
}
