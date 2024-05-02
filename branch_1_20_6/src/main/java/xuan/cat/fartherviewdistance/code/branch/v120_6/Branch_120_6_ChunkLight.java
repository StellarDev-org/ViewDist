package xuan.cat.fartherviewdistance.code.branch.v120_6;

import java.util.Arrays;

import org.bukkit.World;
import org.bukkit.craftbukkit.CraftWorld;

import net.minecraft.server.level.ServerLevel;
import xuan.cat.fartherviewdistance.api.branch.BranchChunkLight;

public final class Branch_120_6_ChunkLight implements BranchChunkLight {

    public static final byte[] EMPTY = new byte[0];

    private final ServerLevel worldServer;
    private final byte[][] blockLights;
    private final byte[][] skyLights;

    public Branch_120_6_ChunkLight(final World world) { this(((CraftWorld) world).getHandle()); }

    public Branch_120_6_ChunkLight(final ServerLevel worldServer) {
        this(worldServer, new byte[worldServer.getSectionsCount() + 2][], new byte[worldServer.getSectionsCount() + 2][]);
    }

    public Branch_120_6_ChunkLight(final ServerLevel worldServer, final byte[][] blockLights, final byte[][] skyLights) {
        this.worldServer = worldServer;
        this.blockLights = blockLights;
        this.skyLights = skyLights;
        Arrays.fill(blockLights, Branch_120_6_ChunkLight.EMPTY);
        Arrays.fill(skyLights, Branch_120_6_ChunkLight.EMPTY);
    }

    public ServerLevel getWorldServer() { return this.worldServer; }

    public int getArrayLength() { return this.blockLights.length; }

    public static int indexFromSectionY(final ServerLevel worldServer, final int sectionY) {
        return sectionY - worldServer.getMinSection() + 1;
    }

    public void setBlockLight(final int sectionY, final byte[] blockLight) {
        this.blockLights[Branch_120_6_ChunkLight.indexFromSectionY(this.worldServer, sectionY)] = blockLight;
    }

    public void setSkyLight(final int sectionY, final byte[] skyLight) {
        this.skyLights[Branch_120_6_ChunkLight.indexFromSectionY(this.worldServer, sectionY)] = skyLight;
    }

    public byte[] getBlockLight(final int sectionY) {
        return this.blockLights[Branch_120_6_ChunkLight.indexFromSectionY(this.worldServer, sectionY)];
    }

    public byte[] getSkyLight(final int sectionY) {
        return this.skyLights[Branch_120_6_ChunkLight.indexFromSectionY(this.worldServer, sectionY)];
    }

    public byte[][] getBlockLights() { return this.blockLights; }

    public byte[][] getSkyLights() { return this.skyLights; }
}
