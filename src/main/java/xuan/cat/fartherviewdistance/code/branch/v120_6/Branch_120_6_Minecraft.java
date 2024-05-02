package xuan.cat.fartherviewdistance.code.branch.v120_6;

import java.io.IOException;
import org.bukkit.Chunk;
import org.bukkit.World;
import org.bukkit.entity.Player;
import xuan.cat.fartherviewdistance.api.branch.BranchChunk;
import xuan.cat.fartherviewdistance.api.branch.BranchChunkLight;
import xuan.cat.fartherviewdistance.api.branch.BranchMinecraft;
import xuan.cat.fartherviewdistance.api.branch.BranchNBT;

public final class Branch_120_6_Minecraft implements BranchMinecraft {

    @Override
    public BranchNBT getChunkNBTFromDisk(final World world, final int chunkX, final int chunkZ) throws IOException {
        throw new UnsupportedOperationException();
    }

    @Override
    public BranchChunk getChunkFromMemoryCache(final World world, final int chunkX, final int chunkZ) {
        throw new UnsupportedOperationException();
    }

    @Override
    public BranchChunk fromChunk(final World world, final int chunkX, final int chunkZ, final BranchNBT nbt,
            final boolean integralHeightmap) {
        throw new UnsupportedOperationException();
    }

    @Override
    public BranchChunkLight fromLight(final World world, final BranchNBT nbt) { throw new UnsupportedOperationException(); }

    @Override
    public BranchChunkLight fromLight(final World world) { throw new UnsupportedOperationException(); }

    @Override
    public BranchChunk fromChunk(final World world, final Chunk chunk) { throw new UnsupportedOperationException(); }

    @Override
    public BranchChunk.Status fromStatus(final BranchNBT nbt) { throw new UnsupportedOperationException(); }

    @Override
    public void injectPlayer(final Player player) { throw new UnsupportedOperationException(); }
}
