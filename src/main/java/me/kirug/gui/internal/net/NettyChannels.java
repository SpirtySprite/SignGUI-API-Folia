package me.kirug.gui.internal.net;

import io.netty.channel.Channel;
import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelHandlerContext;
import me.kirug.gui.internal.reflect.Reflect;
import org.bukkit.entity.Player;

import java.lang.reflect.Field;
import java.util.function.Predicate;

// Grabs a player's Netty channel and slots an interceptor into its pipeline. Reading the
// client's UpdateSign packet is the one thing plain Bukkit can't do. We walk
// EntityPlayer -> connection -> network manager -> channel purely by field type, so it holds
// from 1.16.5 to 1.21.x.
public final class NettyChannels {

    private static final String VANILLA_HANDLER = "packet_handler";

    private static Class<?> playerConnectionClass;
    private static Class<?> networkManagerClass;
    private static Field connectionField;
    private static Field networkManagerField;
    private static Field channelField;

    private NettyChannels() {
    }

    private static void resolve() {
        if (channelField != null) {
            return;
        }
        playerConnectionClass = Reflect.nms("server.network", "ServerGamePacketListenerImpl", "PlayerConnection");
        networkManagerClass = Reflect.nms("network", "Connection", "NetworkManager");
    }

    public static Object connection(Player player) {
        resolve();
        Object entityPlayer = Reflect.handle(player);
        if (connectionField == null) {
            connectionField = Reflect.fieldAssignableFrom(entityPlayer.getClass(), playerConnectionClass);
        }
        return Reflect.read(connectionField, entityPlayer);
    }

    public static Channel channel(Player player) {
        resolve();
        Object entityPlayer = Reflect.handle(player);

        if (connectionField == null) {
            connectionField = Reflect.fieldAssignableFrom(entityPlayer.getClass(), playerConnectionClass);
        }
        Object connection = Reflect.read(connectionField, entityPlayer);

        if (networkManagerField == null) {
            networkManagerField = Reflect.fieldAssignableFrom(connection.getClass(), networkManagerClass);
        }
        Object networkManager = Reflect.read(networkManagerField, connection);

        if (channelField == null) {
            channelField = Reflect.fieldOfType(networkManager.getClass(), Channel.class);
        }
        return (Channel) Reflect.read(channelField, networkManager);
    }

    // consumer returns true when it handled the packet and it should be dropped.
    public static void inject(Player player, String handlerName, Predicate<Object> consumer) {
        Channel channel = channel(player);
        if (channel == null) {
            return;
        }
        channel.eventLoop().execute(() -> {
            try {
                if (channel.pipeline().get(handlerName) != null) {
                    channel.pipeline().remove(handlerName);
                }
                ChannelDuplexHandler handler = new ChannelDuplexHandler() {
                    @Override
                    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
                        boolean consumed;
                        try {
                            consumed = consumer.test(msg);
                        } catch (Throwable t) {
                            consumed = false; // never break the connection over our reflection
                        }
                        if (!consumed) {
                            super.channelRead(ctx, msg);
                        }
                    }
                };
                if (channel.pipeline().get(VANILLA_HANDLER) != null) {
                    channel.pipeline().addBefore(VANILLA_HANDLER, handlerName, handler);
                } else {
                    channel.pipeline().addLast(handlerName, handler);
                }
            } catch (RuntimeException ignored) {
                // Pipeline is probably closing; nothing to do.
            }
        });
    }

    public static void eject(Player player, String handlerName) {
        Channel channel;
        try {
            channel = channel(player);
        } catch (RuntimeException e) {
            return;
        }
        if (channel == null) {
            return;
        }
        channel.eventLoop().execute(() -> {
            try {
                if (channel.pipeline().get(handlerName) != null) {
                    channel.pipeline().remove(handlerName);
                }
            } catch (RuntimeException ignored) {
                // Channel already gone.
            }
        });
    }
}
