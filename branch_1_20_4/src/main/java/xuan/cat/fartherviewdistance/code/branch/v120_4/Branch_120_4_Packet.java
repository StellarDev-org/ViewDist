package xuan.cat.fartherviewdistance.code.branch.v120_4;

import io.netty.buffer.Unpooled;
import java.util.function.Consumer;
import net.minecraft.network.Connection;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.common.ClientboundKeepAlivePacket;
import net.minecraft.network.protocol.game.ClientboundForgetLevelChunkPacket;
import net.minecraft.network.protocol.game.ClientboundLevelChunkWithLightPacket;
import net.minecraft.network.protocol.game.ClientboundSetChunkCacheRadiusPacket;
import net.minecraft.world.level.ChunkPos;
import org.bukkit.craftbukkit.v1_20_R3.entity.CraftPlayer;
import org.bukkit.entity.Player;
import xuan.cat.fartherviewdistance.api.branch.BranchChunk;
import xuan.cat.fartherviewdistance.api.branch.BranchChunkLight;
import xuan.cat.fartherviewdistance.api.branch.BranchPacket;

public final class Branch_120_4_Packet implements BranchPacket {

  private final Branch_120_4_PacketHandleChunk handleChunk = new Branch_120_4_PacketHandleChunk();
  private final Branch_120_4_PacketHandleLightUpdate handleLightUpdate = new Branch_120_4_PacketHandleLightUpdate();

  public void sendPacket(Player player, Packet<?> packet) {
    try {
      Connection container =
        ((CraftPlayer) player).getHandle().connection.connection;
      container.send(packet);
    } catch (IllegalArgumentException ignored) {}
  }

  public void sendViewDistance(Player player, int viewDistance) {
    sendPacket(player, new ClientboundSetChunkCacheRadiusPacket(viewDistance));
  }

  public void sendUnloadChunk(Player player, int chunkX, int chunkZ) {
    sendPacket(
      player,
      new ClientboundForgetLevelChunkPacket(new ChunkPos(chunkX, chunkZ))
    );
  }

  public Consumer<Player> sendChunkAndLight(
    BranchChunk chunk,
    BranchChunkLight light,
    boolean needTile,
    Consumer<Integer> consumeTraffic
  ) {
    FriendlyByteBuf serializer = new FriendlyByteBuf(
      Unpooled.buffer().writerIndex(0)
    );
    serializer.writeInt(chunk.getX());
    serializer.writeInt(chunk.getZ());
    this.handleChunk.write(
        serializer,
        ((Branch_120_4_Chunk) chunk).getLevelChunk(),
        needTile
      );
    this.handleLightUpdate.write(serializer, (Branch_120_4_ChunkLight) light);
    consumeTraffic.accept(serializer.readableBytes());
    ClientboundLevelChunkWithLightPacket packet = new ClientboundLevelChunkWithLightPacket(
      serializer
    );
    try {
      // 適用於 paper
      packet.setReady(true);
    } catch (NoSuchMethodError noSuchMethodError) {
      // 適用於 spigot (不推薦)
    }
    return player -> sendPacket(player, packet);
  }

  public void sendKeepAlive(Player player, long id) {
    sendPacket(player, new ClientboundKeepAlivePacket(id));
  }
}
