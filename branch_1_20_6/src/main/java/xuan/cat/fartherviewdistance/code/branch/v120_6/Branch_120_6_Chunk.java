package xuan.cat.fartherviewdistance.code.branch.v120_6;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import org.bukkit.block.data.BlockData;
import org.bukkit.craftbukkit.CraftChunk;
import org.bukkit.craftbukkit.block.CraftBlock;
import org.bukkit.craftbukkit.block.data.CraftBlockData;
import org.bukkit.craftbukkit.util.CraftNamespacedKey;
import org.bukkit.util.Vector;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.LevelChunkSection;
import net.minecraft.world.level.chunk.PalettedContainer;
import net.minecraft.world.level.chunk.status.ChunkStatus;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.material.FluidState;
import xuan.cat.fartherviewdistance.api.branch.BranchChunk;
import xuan.cat.fartherviewdistance.api.branch.BranchChunkLight;
import xuan.cat.fartherviewdistance.api.branch.BranchNBT;

public final class Branch_120_6_Chunk implements BranchChunk {

    private final LevelChunk levelChunk;
    private final ServerLevel worldServer;

    public Branch_120_6_Chunk(final ServerLevel worldServer, final LevelChunk levelChunk) {
        this.levelChunk = levelChunk;
        this.worldServer = worldServer;
    }

    @Override
    public BranchNBT toNBT(final BranchChunkLight light, final List<Runnable> asyncRunnable) {
        return new Branch_120_6_NBT(Branch_120_6_ChunkRegionLoader.saveChunk(this.worldServer, this.levelChunk,
                (Branch_120_6_ChunkLight) light, asyncRunnable));
    }

    LevelChunk getLevelChunk() { return this.levelChunk; }

    @Override
    public org.bukkit.Chunk getChunk() { return new CraftChunk(this.levelChunk); }

    @Override
    public org.bukkit.World getWorld() { return this.worldServer.getWorld(); }

    public BlockState getIBlockData(final int x, final int y, final int z) {
        final int indexY = (y >> 4) - this.levelChunk.getMinSection();
        final LevelChunkSection[] chunkSections = this.levelChunk.getSections();
        if (indexY >= 0 && indexY < chunkSections.length) {
            final LevelChunkSection chunkSection = chunkSections[indexY];
            if (chunkSection != null && !chunkSection.hasOnlyAir())
                return chunkSection.getBlockState(x & 15, y & 15, z & 15);
        }
        return Blocks.AIR.defaultBlockState();
    }

    public void setIBlockData(final int x, final int y, final int z, final BlockState iBlockData) {
        final int indexY = (y >> 4) - this.levelChunk.getMinSection();
        final LevelChunkSection[] chunkSections = this.levelChunk.getSections();

        if (indexY >= 0 && indexY < chunkSections.length) {
            LevelChunkSection chunkSection = chunkSections[indexY];

            if (chunkSection == null) {
                chunkSection = chunkSections[indexY] = new LevelChunkSection(
                        this.worldServer.registryAccess().registryOrThrow(Registries.BIOME), this.levelChunk.getLevel(),
                        new ChunkPos(this.levelChunk.locX, this.levelChunk.locZ), indexY);
            }
            chunkSection.setBlockState(x & 15, y & 15, z & 15, iBlockData, false);
        }
    }

    @Override
    public boolean equalsBlockData(final int x, final int y, final int z, final BlockData blockData) {
        return this.equalsBlockData(x, y, z, ((CraftBlockData) blockData).getState());
    }

    public boolean equalsBlockData(final int x, final int y, final int z, final BlockState other) {
        final BlockState state = this.getIBlockData(x, y, z);
        return state != null && state.equals(other);
    }

    @Override
    public BlockData getBlockData(final int x, final int y, final int z) {
        final BlockState blockData = this.getIBlockData(x, y, z);
        return blockData != null ? CraftBlockData.fromData(blockData) : CraftBlockData.fromData(Blocks.AIR.defaultBlockState());
    }

    @Override
    public void setBlockData(final int x, final int y, final int z, final BlockData blockData) {
        final BlockState iBlockData = ((CraftBlockData) blockData).getState();
        if (iBlockData != null)
            this.setIBlockData(x, y, z, iBlockData);
    }

    @Override
    public Map<Vector, BlockData> getBlockDataMap() {
        final Map<Vector, BlockData> vectorBlockDataMap = new HashMap<>();
        final int maxHeight = this.worldServer.getMaxBuildHeight();
        final int minHeight = this.worldServer.getMinBuildHeight();
        for (int x = 0; x < 16; x++) {
            for (int y = minHeight; y < maxHeight; y++) {
                for (int z = 0; z < 16; z++) {
                    final BlockData blockData = this.getBlockData(x, y, z);
                    final org.bukkit.Material material = blockData.getMaterial();
                    if (material != org.bukkit.Material.AIR && material != org.bukkit.Material.VOID_AIR
                            && material != org.bukkit.Material.CAVE_AIR) {
                        vectorBlockDataMap.put(new Vector(x, y, z), blockData);
                    }
                }
            }
        }

        return vectorBlockDataMap;
    }

    @Override
    public int getX() { return this.levelChunk.getPos().x; }

    @Override
    public int getZ() { return this.levelChunk.getPos().z; }

    private static Field field_LevelChunkSection_nonEmptyBlockCount;

    static {
        try {
            Branch_120_6_Chunk.field_LevelChunkSection_nonEmptyBlockCount = LevelChunkSection.class.getDeclaredField("nonEmptyBlockCount"); // TODO
                                                                                                                                            // 映射
            // nonEmptyBlockCount
            Branch_120_6_Chunk.field_LevelChunkSection_nonEmptyBlockCount.setAccessible(true);
        } catch (final NoSuchFieldException exception) {
            exception.printStackTrace();
        }
    }

    @Override
    public void replaceAllMaterial(final BlockData[] target, final BlockData to) {
        final Map<Block, BlockState> targetMap = new HashMap<>();
        for (final BlockData targetData : target) {
            final BlockState targetState = ((CraftBlockData) targetData).getState();
            targetMap.put(targetState.getBlock(), targetState);
        }
        final BlockState toI = ((CraftBlockData) to).getState();
        for (final LevelChunkSection section : this.levelChunk.getSections()) {
            if (section != null) {
                final AtomicInteger counts = new AtomicInteger();
                final PalettedContainer<BlockState> blocks = section.getStates();
                final List<Integer> conversionLocationList = new ArrayList<>();
                final PalettedContainer.CountConsumer<BlockState> forEachLocation = (state, location) -> {
                    if (state == null)
                        return;
                    final BlockState targetState = targetMap.get(state.getBlock());
                    if (targetState != null) {
                        conversionLocationList.add(location);
                        state = toI;
                    }
                    if (!state.isAir())
                        counts.incrementAndGet();
                    final FluidState fluid = state.getFluidState();
                    if (!fluid.isEmpty())
                        counts.incrementAndGet();
                };
                try {
                    // 適用於 paper
                    blocks.forEachLocation(forEachLocation);
                } catch (final NoSuchMethodError noSuchMethodError) {
                    // 適用於 spigot (不推薦)
                    blocks.count(forEachLocation);
                }
                conversionLocationList.forEach(location -> {
                    blocks.getAndSetUnchecked(location & 15, location >> 8 & 15, location >> 4 & 15, toI);
                });
                try {
                    Branch_120_6_Chunk.field_LevelChunkSection_nonEmptyBlockCount.set(section, counts.shortValue());
                } catch (final IllegalAccessException exception) {
                    exception.printStackTrace();
                }
            }
        }
    }

    @Override
    public org.bukkit.Material getMaterial(final int x, final int y, final int z) { return this.getBlockData(x, y, z).getMaterial(); }

    @Override
    public void setMaterial(final int x, final int y, final int z, final org.bukkit.Material material) {
        this.setBlockData(x, y, z, material.createBlockData());
    }

    @Override
    @Deprecated
    public org.bukkit.block.Biome getBiome(final int x, final int z) { return this.getBiome(x, 0, z); }

    @Override
    public org.bukkit.block.Biome getBiome(final int x, final int y, final int z) {
        final BlockPos pos = new BlockPos(x, y, z);
        final CraftBlock biomeChunk = new CraftBlock(this.levelChunk.level.getLevel(), pos);
        return biomeChunk.getBiome();
    }

    @Override
    @Deprecated
    public void setBiome(final int x, final int z, final org.bukkit.block.Biome biome) { this.setBiome(x, 0, z, biome); }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    private static final Map<org.bukkit.block.Biome, ResourceKey<net.minecraft.world.level.biome.Biome>> BIOME_KEY_CACHE = Collections
            .synchronizedMap(new EnumMap(org.bukkit.block.Biome.class));

    @SuppressWarnings({ "unchecked", "rawtypes" })
    public static Holder<net.minecraft.world.level.biome.Biome> biomeToBiomeBase(
            final Registry<net.minecraft.world.level.biome.Biome> registry, final org.bukkit.block.Biome bio) {
        return bio != null && bio != org.bukkit.block.Biome.CUSTOM
                ? registry.getHolderOrThrow((ResourceKey) Branch_120_6_Chunk.BIOME_KEY_CACHE.computeIfAbsent(bio,
                        b -> ResourceKey.create(Registries.BIOME, CraftNamespacedKey.toMinecraft(b.getKey()))))
                : null;
    }

    @Override
    public void setBiome(final int x, final int y, final int z, final org.bukkit.block.Biome biome) {
        this.levelChunk.setBiome(x, y, z, Branch_120_6_Chunk.biomeToBiomeBase(this.levelChunk.biomeRegistry, biome));
    }

    @Override
    public boolean hasFluid(final int x, final int y, final int z) { return !this.getIBlockData(x, y, z).getFluidState().isEmpty(); }

    @Override
    public boolean isAir(final int x, final int y, final int z) { return this.getIBlockData(x, y, z).isAir(); }

    @Override
    public int getHighestY(final int x, final int z) { return this.levelChunk.getHeight(Heightmap.Types.MOTION_BLOCKING, x, z); }

    public static BranchChunk.Status ofStatus(final ChunkStatus chunkStatus) {
        if (chunkStatus == ChunkStatus.EMPTY) {
            return BranchChunk.Status.EMPTY;
        } else if (chunkStatus == ChunkStatus.STRUCTURE_STARTS) {
            return BranchChunk.Status.STRUCTURE_STARTS;
        } else if (chunkStatus == ChunkStatus.STRUCTURE_REFERENCES) {
            return BranchChunk.Status.STRUCTURE_REFERENCES;
        } else if (chunkStatus == ChunkStatus.BIOMES) {
            return BranchChunk.Status.BIOMES;
        } else if (chunkStatus == ChunkStatus.NOISE) {
            return BranchChunk.Status.NOISE;
        } else if (chunkStatus == ChunkStatus.SURFACE) {
            return BranchChunk.Status.SURFACE;
        } else if (chunkStatus == ChunkStatus.CARVERS) {
            return BranchChunk.Status.CARVERS;
        } else if (chunkStatus == ChunkStatus.FEATURES) {
            return BranchChunk.Status.FEATURES;
        } else if (chunkStatus == ChunkStatus.LIGHT) {
            return BranchChunk.Status.LIGHT;
        } else if (chunkStatus == ChunkStatus.SPAWN) {
            return BranchChunk.Status.SPAWN;
        } else {
            return chunkStatus == ChunkStatus.FULL ? BranchChunk.Status.FULL : BranchChunk.Status.EMPTY;
        }
    }

    @Override
    public Status getStatus() { return Branch_120_6_Chunk.ofStatus(this.levelChunk.getStatus()); }
}
