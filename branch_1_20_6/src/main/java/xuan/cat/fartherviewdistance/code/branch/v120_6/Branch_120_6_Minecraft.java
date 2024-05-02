package xuan.cat.fartherviewdistance.code.branch.v120_6;

import java.io.IOException;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

import org.bukkit.World;
import org.bukkit.craftbukkit.CraftChunk;
import org.bukkit.craftbukkit.CraftWorld;
import org.bukkit.craftbukkit.entity.CraftPlayer;
import org.bukkit.entity.Player;

import io.netty.channel.Channel;
import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.ChannelPromise;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.server.level.ChunkHolder;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.EmptyLevelChunk;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.status.ChunkStatus;
import xuan.cat.fartherviewdistance.api.branch.BranchChunk;
import xuan.cat.fartherviewdistance.api.branch.BranchChunkLight;
import xuan.cat.fartherviewdistance.api.branch.BranchMinecraft;
import xuan.cat.fartherviewdistance.api.branch.BranchNBT;

@SuppressWarnings("resource")
public final class Branch_120_6_Minecraft implements BranchMinecraft {

    /**
     * 參考 XuanCatAPI.CodeExtendWorld
     */
    @Override
    public BranchNBT getChunkNBTFromDisk(final World world, final int chunkX, final int chunkZ) throws IOException {
        CompoundTag nbt = null;
        try {
            final CompletableFuture<Optional<CompoundTag>> futureNBT = (((CraftWorld) world).getHandle()).getChunkSource().chunkMap
                    .read(new ChunkPos(chunkX, chunkZ));
            final Optional<CompoundTag> optionalNBT = futureNBT.get();
            nbt = optionalNBT.orElse(null);
        } catch (InterruptedException | ExecutionException ignored) {
        }
        return nbt != null ? new Branch_120_6_NBT(nbt) : null;
    }

    /**
     * 參考 XuanCatAPI.CodeExtendWorld
     */
    @Override
    public BranchChunk getChunkFromMemoryCache(final World world, final int chunkX, final int chunkZ) {
        try {
            // 適用於 paper
            final ServerLevel level = ((CraftWorld) world).getHandle();
            final ChunkHolder playerChunk = level.getChunkSource().chunkMap
                    .getVisibleChunkIfPresent((long) chunkZ << 32 | (long) chunkX & 4294967295L);
            if (playerChunk != null) {
                final ChunkAccess chunk = playerChunk.getAvailableChunkNow();
                if (chunk != null && !(chunk instanceof EmptyLevelChunk) && chunk instanceof LevelChunk) {
                    return new Branch_120_6_Chunk(level, (LevelChunk) chunk);
                }
            }
            return null;
        } catch (final NoSuchMethodError ignored) {
            return null;
        }
    }

    /**
     * 參考 XuanCatAPI.CodeExtendWorld
     */
    @Override
    public BranchChunk fromChunk(final World world, final int chunkX, final int chunkZ, final BranchNBT nbt,
            final boolean integralHeightmap) {
        return Branch_120_6_ChunkRegionLoader.loadChunk(((CraftWorld) world).getHandle(), chunkX, chunkZ,
                ((Branch_120_6_NBT) nbt).getNMSTag(), integralHeightmap);
    }

    /**
     * 參考 XuanCatAPI.CodeExtendWorld
     */
    @Override
    public BranchChunkLight fromLight(final World world, final BranchNBT nbt) {
        return Branch_120_6_ChunkRegionLoader.loadLight(((CraftWorld) world).getHandle(), ((Branch_120_6_NBT) nbt).getNMSTag());
    }

    /**
     * 參考 XuanCatAPI.CodeExtendWorld
     */
    @Override
    public BranchChunkLight fromLight(final World world) { return new Branch_120_6_ChunkLight(((CraftWorld) world).getHandle()); }

    /**
     * 參考 XuanCatAPI.CodeExtendWorld
     */
    @Override
    public BranchChunk.Status fromStatus(final BranchNBT nbt) {
        return Branch_120_6_ChunkRegionLoader.loadStatus(((Branch_120_6_NBT) nbt).getNMSTag());
    }

    /**
     * 參考 XuanCatAPI.CodeExtendWorld
     */
    @Override
    public BranchChunk fromChunk(final World world, final org.bukkit.Chunk chunk) {
        return new Branch_120_6_Chunk(((CraftChunk) chunk).getCraftWorld().getHandle(),
                (LevelChunk) ((CraftChunk) chunk).getHandle(ChunkStatus.FULL));
    }

    @Override
    public void injectPlayer(final Player player) {
        final ServerPlayer entityPlayer = ((CraftPlayer) player).getHandle();
        final ServerGamePacketListenerImpl connection = entityPlayer.connection;
        final Channel channel = connection.connection.channel;
        final ChannelPipeline pipeline = channel.pipeline();
        pipeline.addAfter("packet_handler", "farther_view_distance_write", new ChannelDuplexHandler() {
            @Override
            public void write(final ChannelHandlerContext ctx, final Object msg, final ChannelPromise promise) throws Exception {
                if (msg instanceof Packet) {
                    if (!Branch_120_6_ProxyPlayerConnection.write(player, (Packet<?>) msg))
                        return;
                }
                super.write(ctx, msg, promise);
            }
        });
        pipeline.addAfter("encoder", "farther_view_distance_read", new ChannelInboundHandlerAdapter() {
            @Override
            public void channelRead(final ChannelHandlerContext ctx, final Object msg) throws Exception {
                if (msg instanceof Packet) {
                    if (!Branch_120_6_ProxyPlayerConnection.read(player, (Packet<?>) msg))
                        return;
                }
                super.channelRead(ctx, msg);
            }
        });
    }
}
