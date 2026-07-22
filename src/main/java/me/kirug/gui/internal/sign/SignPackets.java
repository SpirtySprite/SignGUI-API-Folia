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

    private static volatile boolean resolved;

    private static Class<?> packetClass;
    private static Class<?> updateSignClass;

    private static Constructor<?> blockPosCtor;
    private static Constructor<?> blockChangeCtor;
    private static Constructor<?> openSignCtor;

    private static Method blockDataToState;
    private static Method sendMethod;

    // The update packet stores either a single String[] (most versions) or four String fields.
    private static Field updateSignLinesField;
    private static java.util.List<Field> updateSignStringFields;

    private SignPackets() {
    }

    // Everything is resolved into locals and only committed at the end, so a partial failure
    // leaves the state clean and rethrows the real cause next time instead of masking it.
    private static synchronized void resolve() {
        if (resolved) {
            return;
        }
        Class<?> blockPos = Reflect.nms("core", "BlockPos", "BlockPosition");
        Class<?> blockState = Reflect.nms("world.level.block.state", "BlockState", "IBlockData");
        Class<?> packet = Reflect.nms("network.protocol", "Packet", "Packet");
        Class<?> blockChange = Reflect.nms("network.protocol.game", "ClientboundBlockUpdatePacket", "PacketPlayOutBlockChange");
        Class<?> openSign = Reflect.nms("network.protocol.game", "ClientboundOpenSignEditorPacket", "PacketPlayOutOpenSignEditor");
        Class<?> updateSign = Reflect.nms("network.protocol.game", "ServerboundSignUpdatePacket", "PacketPlayInUpdateSign");

        Constructor<?> posCtor = Reflect.constructor(blockPos, int.class, int.class, int.class);
        // ClientboundBlockUpdatePacket has two 2-arg ctors; pick (BlockPos, BlockState) by type.
        Constructor<?> bcCtor = Reflect.constructor(blockChange, blockPos, blockState);
        Constructor<?> osCtor = ServerVersion.current().twoSidedSigns()
                ? Reflect.constructor(openSign, blockPos, boolean.class)
                : Reflect.constructor(openSign, blockPos);

        packetClass = packet;
        updateSignClass = updateSign;
        blockPosCtor = posCtor;
        blockChangeCtor = bcCtor;
        openSignCtor = osCtor;
        resolved = true;
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
        locateLineFields();

        if (updateSignLinesField != null) {
            Object raw = Reflect.read(updateSignLinesField, updateSignPacket);
            if (raw instanceof String[]) {
                return (String[]) raw;
            }
        }
        if (updateSignStringFields != null) {
            String[] out = {"", "", "", ""};
            for (int i = 0; i < Math.min(4, updateSignStringFields.size()); i++) {
                Object value = Reflect.read(updateSignStringFields.get(i), updateSignPacket);
                out[i] = value != null ? value.toString() : "";
            }
            return out;
        }
        return new String[]{"", "", "", ""};
    }

    private static synchronized void locateLineFields() {
        if (updateSignLinesField != null || updateSignStringFields != null) {
            return;
        }
        Field array = Reflect.fieldOfTypeOrNull(updateSignClass, String[].class);
        if (array != null) {
            updateSignLinesField = array;
            return;
        }
        // Fall back to individual String fields, in declared order.
        java.util.List<Field> strings = new java.util.ArrayList<>();
        for (Field f : updateSignClass.getDeclaredFields()) {
            if (f.getType() == String.class) {
                f.setAccessible(true);
                strings.add(f);
            }
        }
        updateSignStringFields = strings;
    }
}
