package xuan.cat.fartherviewdistance.code.branch.v120_6;

import java.util.function.Consumer;
import org.bukkit.entity.Player;
import xuan.cat.fartherviewdistance.api.branch.BranchChunk;
import xuan.cat.fartherviewdistance.api.branch.BranchChunkLight;
import xuan.cat.fartherviewdistance.api.branch.BranchPacket;

public final class Branch_120_6_Packet implements BranchPacket {

    @Override
    public void sendViewDistance(final Player player, final int viewDistance) { throw new UnsupportedOperationException(); }

    @Override
    public void sendUnloadChunk(final Player player, final int chunkX, final int chunkZ) { throw new UnsupportedOperationException(); }

    @Override
    public Consumer<Player> sendChunkAndLight(final BranchChunk chunk, final BranchChunkLight light, final boolean needTile,
            final Consumer<Integer> consumeTraffic) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void sendKeepAlive(final Player player, final long id) { throw new UnsupportedOperationException(); }
}
