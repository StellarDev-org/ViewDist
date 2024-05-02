package xuan.cat.fartherviewdistance.code.branch.v120_6;

import java.lang.reflect.Field;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.common.ServerboundKeepAlivePacket;
import net.minecraft.network.protocol.game.ClientboundForgetLevelChunkPacket;
import net.minecraft.network.protocol.game.ClientboundLevelChunkWithLightPacket;
import net.minecraft.network.protocol.game.ClientboundSetChunkCacheRadiusPacket;
import net.minecraft.world.level.ChunkPos;
import xuan.cat.fartherviewdistance.api.branch.packet.PacketKeepAliveEvent;
import xuan.cat.fartherviewdistance.api.branch.packet.PacketMapChunkEvent;
import xuan.cat.fartherviewdistance.api.branch.packet.PacketUnloadChunkEvent;
import xuan.cat.fartherviewdistance.api.branch.packet.PacketViewDistanceEvent;

public final class Branch_120_6_ProxyPlayerConnection {

    public static boolean read(final Player player, final Packet<?> packet) {
        if (packet instanceof ServerboundKeepAlivePacket) {
            final PacketKeepAliveEvent event = new PacketKeepAliveEvent(player, ((ServerboundKeepAlivePacket) packet).getId());
            Bukkit.getPluginManager().callEvent(event);
            return !event.isCancelled();
        } else {
            return true;
        }
    }

    private static Field field_ClientboundForgetLevelChunkPacket_chunkPos;
    private static Field field_ClientboundSetChunkCacheRadiusPacket_distance;
    private static Field field_ClientboundLevelChunkWithLightPacket_chunkX;
    private static Field field_ClientboundLevelChunkWithLightPacket_chunkZ;

    static {
        try {
            Branch_120_6_ProxyPlayerConnection.field_ClientboundForgetLevelChunkPacket_chunkPos = ClientboundForgetLevelChunkPacket.class
                    .getDeclaredField("pos");
            Branch_120_6_ProxyPlayerConnection.field_ClientboundSetChunkCacheRadiusPacket_distance = ClientboundSetChunkCacheRadiusPacket.class
                    .getDeclaredField("radius");
            Branch_120_6_ProxyPlayerConnection.field_ClientboundLevelChunkWithLightPacket_chunkX = ClientboundLevelChunkWithLightPacket.class
                    .getDeclaredField("x");
            Branch_120_6_ProxyPlayerConnection.field_ClientboundLevelChunkWithLightPacket_chunkZ = ClientboundLevelChunkWithLightPacket.class
                    .getDeclaredField("z");
            Branch_120_6_ProxyPlayerConnection.field_ClientboundForgetLevelChunkPacket_chunkPos.setAccessible(true);
            Branch_120_6_ProxyPlayerConnection.field_ClientboundSetChunkCacheRadiusPacket_distance.setAccessible(true);
            Branch_120_6_ProxyPlayerConnection.field_ClientboundLevelChunkWithLightPacket_chunkX.setAccessible(true);
            Branch_120_6_ProxyPlayerConnection.field_ClientboundLevelChunkWithLightPacket_chunkZ.setAccessible(true);
        } catch (final Exception ex) {
            ex.printStackTrace();
        }
    }

    public static boolean write(final Player player, final Packet<?> packet) {
        try {
            if (packet instanceof ClientboundForgetLevelChunkPacket) {
                final PacketUnloadChunkEvent event = new PacketUnloadChunkEvent(player,
                        (ChunkPos) Branch_120_6_ProxyPlayerConnection.field_ClientboundForgetLevelChunkPacket_chunkPos.get(packet));
                Bukkit.getPluginManager().callEvent(event);
                return !event.isCancelled();
            } else if (packet instanceof ClientboundSetChunkCacheRadiusPacket) {
                final PacketViewDistanceEvent event = new PacketViewDistanceEvent(player,
                        Branch_120_6_ProxyPlayerConnection.field_ClientboundSetChunkCacheRadiusPacket_distance.getInt(packet));
                Bukkit.getPluginManager().callEvent(event);
                return !event.isCancelled();
            } else if (packet instanceof ClientboundLevelChunkWithLightPacket) {
                final PacketMapChunkEvent event = new PacketMapChunkEvent(player,
                        Branch_120_6_ProxyPlayerConnection.field_ClientboundLevelChunkWithLightPacket_chunkX.getInt(packet),
                        Branch_120_6_ProxyPlayerConnection.field_ClientboundLevelChunkWithLightPacket_chunkZ.getInt(packet));
                Bukkit.getPluginManager().callEvent(event);
                return !event.isCancelled();
            } else {
                return true;
            }
        } catch (final Exception ex) {
            ex.printStackTrace();
            return true;
        }
    }
}
