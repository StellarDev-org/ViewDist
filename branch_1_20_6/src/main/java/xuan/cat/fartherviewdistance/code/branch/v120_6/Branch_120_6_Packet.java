package xuan.cat.fartherviewdistance.code.branch.v120_6;

import java.util.function.Consumer;

import org.bukkit.craftbukkit.entity.CraftPlayer;
import org.bukkit.entity.Player;

import io.netty.buffer.Unpooled;
import net.minecraft.network.Connection;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.common.ClientboundKeepAlivePacket;
import net.minecraft.network.protocol.game.ClientboundForgetLevelChunkPacket;
import net.minecraft.network.protocol.game.ClientboundLevelChunkWithLightPacket;
import net.minecraft.network.protocol.game.ClientboundSetChunkCacheRadiusPacket;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.lighting.LevelLightEngine;
import xuan.cat.fartherviewdistance.api.branch.BranchChunk;
import xuan.cat.fartherviewdistance.api.branch.BranchChunkLight;
import xuan.cat.fartherviewdistance.api.branch.BranchPacket;

public final class Branch_120_6_Packet implements BranchPacket {

    private final Branch_120_6_PacketHandleChunk handleChunk = new Branch_120_6_PacketHandleChunk();
    private final Branch_120_6_PacketHandleLightUpdate handleLightUpdate = new Branch_120_6_PacketHandleLightUpdate();

    public void sendPacket(final Player player, final Packet<?> packet) {
        try {
            final Connection container = ((CraftPlayer) player).getHandle().connection.connection;
            container.send(packet);
        } catch (final IllegalArgumentException ignored) {
        }
    }

    @Override
    public void sendViewDistance(final Player player, final int viewDistance) {
        this.sendPacket(player, new ClientboundSetChunkCacheRadiusPacket(viewDistance));
    }

    @Override
    public void sendUnloadChunk(final Player player, final int chunkX, final int chunkZ) {
        this.sendPacket(player, new ClientboundForgetLevelChunkPacket(new ChunkPos(chunkX, chunkZ)));
    }

    @Override
    public Consumer<Player> sendChunkAndLight(final BranchChunk chunk, final BranchChunkLight light, final boolean needTile,
            final Consumer<Integer> consumeTraffic) {
        final FriendlyByteBuf serializer = new FriendlyByteBuf(Unpooled.buffer().writerIndex(0));
        final LevelLightEngine levelLight = ((Branch_120_6_Chunk) chunk).getLevelChunk().level.getLightEngine();
        serializer.writeInt(chunk.getX());
        serializer.writeInt(chunk.getZ());
        this.handleChunk.write(serializer, ((Branch_120_6_Chunk) chunk).getLevelChunk(), needTile);
        this.handleLightUpdate.write(serializer, (Branch_120_6_ChunkLight) light);
        consumeTraffic.accept(serializer.readableBytes());
        final ClientboundLevelChunkWithLightPacket packet = new ClientboundLevelChunkWithLightPacket(
                ((Branch_120_6_Chunk) chunk).getLevelChunk(), levelLight, null, null, false);
        try {
            // 適用於 paper
            packet.setReady(true);
        } catch (final NoSuchMethodError noSuchMethodError) {
            // 適用於 spigot (不推薦)
        }
        return player -> this.sendPacket(player, packet);
    }

    @Override
    public void sendKeepAlive(final Player player, final long id) { this.sendPacket(player, new ClientboundKeepAlivePacket(id)); }
}
