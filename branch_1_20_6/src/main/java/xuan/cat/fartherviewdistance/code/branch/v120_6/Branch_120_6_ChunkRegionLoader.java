package xuan.cat.fartherviewdistance.code.branch.v120_6;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import com.mojang.serialization.Codec;
import com.mojang.serialization.Dynamic;

import it.unimi.dsi.fastutil.shorts.ShortList;
import net.minecraft.SharedConstants;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.SectionPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.LongArrayTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.nbt.ShortTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerChunkCache;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ThreadedLevelLightEngine;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.DataLayer;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.LevelChunkSection;
import net.minecraft.world.level.chunk.PalettedContainer;
import net.minecraft.world.level.chunk.PalettedContainerRO;
import net.minecraft.world.level.chunk.ProtoChunk;
import net.minecraft.world.level.chunk.UpgradeData;
import net.minecraft.world.level.chunk.status.ChunkStatus;
import net.minecraft.world.level.chunk.status.ChunkType;
import net.minecraft.world.level.chunk.storage.ChunkSerializer;
import net.minecraft.world.level.levelgen.BelowZeroRetrogen;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.blending.BlendingData;
import net.minecraft.world.level.lighting.LevelLightEngine;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.storage.DimensionDataStorage;
import net.minecraft.world.ticks.LevelChunkTicks;
import net.minecraft.world.ticks.ProtoChunkTicks;
import xuan.cat.fartherviewdistance.api.branch.BranchChunk;
import xuan.cat.fartherviewdistance.api.branch.BranchChunkLight;

/**
 * @see ChunkSerializer 參考 XuanCatAPI.CodeExtendChunkLight
 */
@SuppressWarnings({ "unchecked" })
public final class Branch_120_6_ChunkRegionLoader {

    private static final int CURRENT_DATA_VERSION = SharedConstants.getCurrentVersion().getDataVersion().getVersion();
    private static final boolean JUST_CORRUPT_IT = Boolean.getBoolean("Paper.ignoreWorldDataVersion");

    public static BranchChunk.Status loadStatus(final CompoundTag nbt) {
        try {
            // 適用於 paper
            return Branch_120_6_Chunk.ofStatus(ChunkStatus.getStatus(nbt.getString("Status")));
        } catch (final NoSuchMethodError noSuchMethodError) {
            // 適用於 spigot (不推薦)
            return Branch_120_6_Chunk.ofStatus(ChunkStatus.byName(nbt.getString("Status")));
        }
    }

    private static Codec<PalettedContainerRO<Holder<Biome>>> makeBiomeCodec(final Registry<Biome> biomeRegistry) {
        return PalettedContainer.codecRO(biomeRegistry.asHolderIdMap(), biomeRegistry.holderByNameCodec(),
                PalettedContainer.Strategy.SECTION_BIOMES, biomeRegistry.getHolderOrThrow(Biomes.PLAINS));
    }

    private static Method method_ChunkSerializer_makeBiomeCodecRW;

    static {
        try {
            Branch_120_6_ChunkRegionLoader.method_ChunkSerializer_makeBiomeCodecRW = ChunkSerializer.class
                    .getDeclaredMethod("makeBiomeCodecRW", Registry.class);
            Branch_120_6_ChunkRegionLoader.method_ChunkSerializer_makeBiomeCodecRW.setAccessible(true);
        } catch (final NoSuchMethodException ex) {
            ex.printStackTrace();
        }
    }
    private static Field DimensionDataStorage_Provider;

    static {
        try {
            Branch_120_6_ChunkRegionLoader.DimensionDataStorage_Provider = DimensionDataStorage.class.getDeclaredField("registries");
            Branch_120_6_ChunkRegionLoader.DimensionDataStorage_Provider.setAccessible(true);
        } catch (final NoSuchFieldException ex) {
            ex.printStackTrace();
        }
    }

    private static Codec<PalettedContainer<Holder<Biome>>> makeBiomeCodecRW(final Registry<Biome> biomeRegistry) {
        try {
            return (Codec<PalettedContainer<Holder<Biome>>>) Branch_120_6_ChunkRegionLoader.method_ChunkSerializer_makeBiomeCodecRW
                    .invoke(null, biomeRegistry);
        } catch (InvocationTargetException | IllegalAccessException ex) {
            ex.printStackTrace();
            return null;
        }
    }

    public static BranchChunk loadChunk(final ServerLevel world, final int chunkX, final int chunkZ, final CompoundTag nbt,
            final boolean integralHeightmap) {
        if (nbt.contains("DataVersion", 99)) {
            final int dataVersion = nbt.getInt("DataVersion");
            if (!Branch_120_6_ChunkRegionLoader.JUST_CORRUPT_IT && dataVersion > Branch_120_6_ChunkRegionLoader.CURRENT_DATA_VERSION) {
                (new RuntimeException("Server attempted to load chunk saved with newer version of minecraft! " + dataVersion + " > "
                        + Branch_120_6_ChunkRegionLoader.CURRENT_DATA_VERSION)).printStackTrace();
                System.exit(1);
            }
        }

        final ChunkPos chunkPos = new ChunkPos(chunkX, chunkZ);
        final UpgradeData upgradeData = nbt.contains("UpgradeData", 10) ? new UpgradeData(nbt.getCompound("UpgradeData"), world)
                : UpgradeData.EMPTY;
        final boolean isLightOn = Objects.requireNonNullElse(ChunkStatus.byName(nbt.getString("Status")), ChunkStatus.EMPTY)
                .isOrAfter(ChunkStatus.LIGHT) && (nbt.get("isLightOn") != null || nbt.getInt("starlight.light_version") == 6);
        final ListTag sectionArrayNBT = nbt.getList("sections", 10);
        final int sectionsCount = world.getSectionsCount();
        final LevelChunkSection[] sections = new LevelChunkSection[sectionsCount];
        final ServerChunkCache chunkSource = world.getChunkSource();
        final LevelLightEngine lightEngine = chunkSource.getLightEngine();
        final Registry<Biome> biomeRegistry = world.registryAccess().registryOrThrow(Registries.BIOME);
        final Codec<PalettedContainer<Holder<Biome>>> paletteCodec = Branch_120_6_ChunkRegionLoader.makeBiomeCodecRW(biomeRegistry);
        for (int sectionIndex = 0; sectionIndex < sectionArrayNBT.size(); ++sectionIndex) {
            final CompoundTag sectionNBT = sectionArrayNBT.getCompound(sectionIndex);
            final byte locationY = sectionNBT.getByte("Y");
            final int sectionY = world.getSectionIndexFromSectionY(locationY);
            if (sectionY >= 0 && sectionY < sections.length) {
                // 方塊轉換器
                PalettedContainer<BlockState> paletteBlock;
                if (sectionNBT.contains("block_states", 10)) {
                    paletteBlock = ChunkSerializer.BLOCK_STATE_CODEC.parse(NbtOps.INSTANCE, sectionNBT.getCompound("block_states"))
                            .promotePartial(sx -> {
                            }).getOrThrow(Branch_120_6_NothingException::new);
                } else {
                    paletteBlock = new PalettedContainer<>(Block.BLOCK_STATE_REGISTRY, Blocks.AIR.defaultBlockState(),
                            PalettedContainer.Strategy.SECTION_STATES,
                            world.chunkPacketBlockController.getPresetBlockStates(world, chunkPos, locationY));
                }

                // 生態轉換器
                PalettedContainer<Holder<Biome>> paletteBiome;
                if (sectionNBT.contains("biomes", 10)) {
                    paletteBiome = paletteCodec.parse(NbtOps.INSTANCE, sectionNBT.getCompound("biomes")).promotePartial(sx -> {
                    }).getOrThrow(Branch_120_6_NothingException::new);
                } else {
                    try {
                        // 適用於 paper
                        paletteBiome = new PalettedContainer<>(biomeRegistry.asHolderIdMap(), biomeRegistry.getHolderOrThrow(Biomes.PLAINS),
                                PalettedContainer.Strategy.SECTION_BIOMES, null);
                    } catch (final NoSuchMethodError noSuchMethodError) {
                        // 適用於 spigot (不推薦)
                        paletteBiome = new PalettedContainer<>(biomeRegistry.asHolderIdMap(), biomeRegistry.getHolderOrThrow(Biomes.PLAINS),
                                PalettedContainer.Strategy.SECTION_BIOMES, null);
                    }
                }

                final LevelChunkSection chunkSection = new LevelChunkSection(paletteBlock, paletteBiome);
                sections[sectionY] = chunkSection;
            }
        }

        final long inhabitedTime = nbt.getLong("InhabitedTime");
        final ChunkType chunkType = ChunkSerializer.getChunkTypeFromTag(nbt);
        BlendingData blendingData;
        if (nbt.contains("blending_data", 10)) {
            blendingData = BlendingData.CODEC.parse(new Dynamic<>(NbtOps.INSTANCE, nbt.getCompound("blending_data")))
                    .resultOrPartial(sx -> {
                    }).orElse(null);
        } else {
            blendingData = null;
        }

        ChunkAccess chunk;
        if (chunkType == ChunkType.LEVELCHUNK) {
            final LevelChunkTicks<Block> ticksBlock = LevelChunkTicks.load(nbt.getList("block_ticks", 10),
                    sx -> BuiltInRegistries.BLOCK.getOptional(ResourceLocation.tryParse(sx)), chunkPos);
            final LevelChunkTicks<Fluid> ticksFluid = LevelChunkTicks.load(nbt.getList("fluid_ticks", 10),
                    sx -> BuiltInRegistries.FLUID.getOptional(ResourceLocation.tryParse(sx)), chunkPos);
            final LevelChunk levelChunk = new LevelChunk(world.getLevel(), chunkPos, upgradeData, ticksBlock, ticksFluid, inhabitedTime,
                    sections, null, blendingData);
            chunk = levelChunk;

            // 實體方塊
            final ListTag blockEntities = nbt.getList("block_entities", 10);
            for (int entityIndex = 0; entityIndex < blockEntities.size(); ++entityIndex) {
                final CompoundTag entityNBT = blockEntities.getCompound(entityIndex);
                final boolean keepPacked = entityNBT.getBoolean("keepPacked");
                if (keepPacked) {
                    chunk.setBlockEntityNbt(entityNBT);
                } else {
                    // TODO: Re-add blockentities, Check 1.20.4
                }
            }
        } else {
            final ProtoChunkTicks<Block> ticksBlock = ProtoChunkTicks.load(nbt.getList("block_ticks", 10),
                    sx -> BuiltInRegistries.BLOCK.getOptional(ResourceLocation.tryParse(sx)), chunkPos);
            final ProtoChunkTicks<Fluid> ticksFluid = ProtoChunkTicks.load(nbt.getList("fluid_ticks", 10),
                    sx -> BuiltInRegistries.FLUID.getOptional(ResourceLocation.tryParse(sx)), chunkPos);
            final ProtoChunk protochunk = new ProtoChunk(chunkPos, upgradeData, sections, ticksBlock, ticksFluid, world, biomeRegistry,
                    blendingData);
            chunk = protochunk;
            protochunk.setInhabitedTime(inhabitedTime);
            if (nbt.contains("below_zero_retrogen", 10)) {
                BelowZeroRetrogen.CODEC.parse(new Dynamic<>(NbtOps.INSTANCE, nbt.getCompound("below_zero_retrogen")))
                        .resultOrPartial(sx -> {
                        }).ifPresent(protochunk::setBelowZeroRetrogen);
            }

            final ChunkStatus chunkStatus = ChunkStatus.byName(nbt.getString("Status"));
            protochunk.setStatus(chunkStatus);
            if (chunkStatus.isOrAfter(ChunkStatus.FEATURES)) {
                protochunk.setLightEngine(lightEngine);
            }
        }
        chunk.setLightCorrect(isLightOn);

        // 高度圖
        final CompoundTag heightmapsNBT = nbt.getCompound("Heightmaps");
        final EnumSet<Heightmap.Types> enumHeightmapType = EnumSet.noneOf(Heightmap.Types.class);
        for (final Heightmap.Types heightmapTypes : chunk.getStatus().heightmapsAfter()) {
            final String serializationKey = heightmapTypes.getSerializationKey();
            if (heightmapsNBT.contains(serializationKey, 12)) {
                chunk.setHeightmap(heightmapTypes, heightmapsNBT.getLongArray(serializationKey));
            } else {
                enumHeightmapType.add(heightmapTypes);
            }
        }
        if (integralHeightmap) {
            Heightmap.primeHeightmaps(chunk, enumHeightmapType);
        }

        final ListTag processListNBT = nbt.getList("PostProcessing", 9);
        for (int indexList = 0; indexList < processListNBT.size(); ++indexList) {
            final ListTag processNBT = processListNBT.getList(indexList);
            for (int index = 0; index < processNBT.size(); ++index) {
                chunk.addPackedPostProcess(processNBT.getShort(index), indexList);
            }
        }

        if (chunkType == ChunkType.LEVELCHUNK) {
            return new Branch_120_6_Chunk(world, (LevelChunk) chunk);
        } else {
            final ProtoChunk protoChunk = (ProtoChunk) chunk;
            return new Branch_120_6_Chunk(world, new LevelChunk(world, protoChunk, v -> {
            }));
        }
    }

    public static BranchChunkLight loadLight(final ServerLevel world, final CompoundTag nbt) {
        // 檢查資料版本
        if (nbt.contains("DataVersion", 99)) {
            final int dataVersion = nbt.getInt("DataVersion");
            if (!Branch_120_6_ChunkRegionLoader.JUST_CORRUPT_IT && dataVersion > Branch_120_6_ChunkRegionLoader.CURRENT_DATA_VERSION) {
                (new RuntimeException("Server attempted to load chunk saved with newer version of minecraft! " + dataVersion + " > "
                        + Branch_120_6_ChunkRegionLoader.CURRENT_DATA_VERSION)).printStackTrace();
                System.exit(1);
            }
        }

        final boolean isLightOn = Objects.requireNonNullElse(ChunkStatus.byName(nbt.getString("Status")), ChunkStatus.EMPTY)
                .isOrAfter(ChunkStatus.LIGHT) && (nbt.get("isLightOn") != null || nbt.getInt("starlight.light_version") == 9);
        final boolean hasSkyLight = world.dimensionType().hasSkyLight();
        final ListTag sectionArrayNBT = nbt.getList("sections", 10);
        final Branch_120_6_ChunkLight chunkLight = new Branch_120_6_ChunkLight(world);
        for (int sectionIndex = 0; sectionIndex < sectionArrayNBT.size(); ++sectionIndex) {
            final CompoundTag sectionNBT = sectionArrayNBT.getCompound(sectionIndex);
            final byte locationY = sectionNBT.getByte("Y");
            if (isLightOn) {
                if (sectionNBT.contains("BlockLight", 7)) {
                    chunkLight.setBlockLight(locationY, sectionNBT.getByteArray("BlockLight"));
                }
                if (hasSkyLight) {
                    if (sectionNBT.contains("SkyLight", 7)) {
                        chunkLight.setSkyLight(locationY, sectionNBT.getByteArray("SkyLight"));
                    }
                }
            }
        }

        return chunkLight;
    }

    public static CompoundTag saveChunk(final ServerLevel world, final ChunkAccess chunk, final Branch_120_6_ChunkLight light,
            final List<Runnable> asyncRunnable) {
        final int minSection = world.getMinSection() - 1; // WorldUtil.getMinLightSection();
        final ChunkPos chunkPos = chunk.getPos();
        final CompoundTag nbt = NbtUtils.addCurrentDataVersion(new CompoundTag());
        nbt.putInt("xPos", chunkPos.x);
        nbt.putInt("yPos", chunk.getMinSection());
        nbt.putInt("zPos", chunkPos.z);
        nbt.putLong("LastUpdate", world.getGameTime());
        nbt.putLong("InhabitedTime", chunk.getInhabitedTime());
        nbt.putString("Status", chunk.getStatus().toString());
        final BlendingData blendingData = chunk.getBlendingData();
        if (blendingData != null) {
            BlendingData.CODEC.encodeStart(NbtOps.INSTANCE, blendingData).resultOrPartial(sx -> {
            }).ifPresent(nbtData -> nbt.put("blending_data", nbtData));
        }

        final BelowZeroRetrogen belowZeroRetrogen = chunk.getBelowZeroRetrogen();
        if (belowZeroRetrogen != null) {
            BelowZeroRetrogen.CODEC.encodeStart(NbtOps.INSTANCE, belowZeroRetrogen).resultOrPartial(sx -> {
            }).ifPresent(nbtData -> nbt.put("below_zero_retrogen", nbtData));
        }

        final LevelChunkSection[] chunkSections = chunk.getSections();
        final ListTag sectionArrayNBT = new ListTag();
        final ThreadedLevelLightEngine lightEngine = world.getChunkSource().getLightEngine();

        // 生態解析器
        final Registry<Biome> biomeRegistry = world.registryAccess().registryOrThrow(Registries.BIOME);
        final Codec<PalettedContainerRO<Holder<Biome>>> paletteCodec = Branch_120_6_ChunkRegionLoader.makeBiomeCodec(biomeRegistry);
        boolean lightCorrect = false;

        for (int locationY = lightEngine.getMinLightSection(); locationY < lightEngine.getMaxLightSection(); ++locationY) {
            final int sectionY = chunk.getSectionIndexFromSectionY(locationY);
            final boolean inSections = sectionY >= 0 && sectionY < chunkSections.length;
            final ThreadedLevelLightEngine lightEngineThreaded = world.getChunkSource().getLightEngine();
            DataLayer blockNibble;
            DataLayer skyNibble;
            try {
                // 適用於 paper
                blockNibble = chunk.getBlockNibbles()[locationY - minSection].toVanillaNibble();
                skyNibble = chunk.getSkyNibbles()[locationY - minSection].toVanillaNibble();
            } catch (final NoSuchMethodError noSuchMethodError) {
                // 適用於 spigot (不推薦)
                blockNibble = lightEngineThreaded.getLayerListener(LightLayer.BLOCK).getDataLayerData(SectionPos.of(chunkPos, locationY));
                skyNibble = lightEngineThreaded.getLayerListener(LightLayer.SKY).getDataLayerData(SectionPos.of(chunkPos, locationY));
            }

            if (inSections || blockNibble != null || skyNibble != null) {
                final CompoundTag sectionNBT = new CompoundTag();
                if (inSections) {
                    final LevelChunkSection chunkSection = chunkSections[sectionY];
                    asyncRunnable.add(() -> {
                        sectionNBT.put("block_states", ChunkSerializer.BLOCK_STATE_CODEC
                                .encodeStart(NbtOps.INSTANCE, chunkSection.getStates()).getOrThrow(Branch_120_6_NothingException::new));
                        sectionNBT.put("biomes", paletteCodec.encodeStart(NbtOps.INSTANCE, chunkSection.getBiomes())
                                .getOrThrow(Branch_120_6_NothingException::new));
                    });
                }

                if (blockNibble != null) {
                    if (!blockNibble.isEmpty()) {
                        if (light != null) {
                            light.setBlockLight(locationY, blockNibble.getData());
                        } else {
                            sectionNBT.putByteArray("BlockLight", blockNibble.getData());
                            lightCorrect = true;
                        }
                    }
                }

                if (skyNibble != null) {
                    if (!skyNibble.isEmpty()) {
                        if (light != null) {
                            light.setSkyLight(locationY, skyNibble.getData());
                        } else {
                            sectionNBT.putByteArray("SkyLight", skyNibble.getData());
                            lightCorrect = true;
                        }
                    }
                }

                // 增加 inSections 確保 asyncRunnable 不會出資料錯誤
                if (!sectionNBT.isEmpty() || inSections) {
                    sectionNBT.putByte("Y", (byte) locationY);
                    sectionArrayNBT.add(sectionNBT);
                }
            }
        }
        nbt.put("sections", sectionArrayNBT);

        if (lightCorrect) {
            nbt.putInt("starlight.light_version", 6);
            nbt.putBoolean("isLightOn", true);
        }

        // 實體方塊
        final ListTag blockEntitiesNBT = new ListTag();
        for (final BlockPos blockPos : chunk.getBlockEntitiesPos()) {
            final CompoundTag blockEntity = chunk.getBlockEntityNbtForSaving(blockPos, null);
            if (blockEntity != null) {
                blockEntitiesNBT.add(blockEntity);
            }
        }
        nbt.put("block_entities", blockEntitiesNBT);

        if (chunk.getStatus().getChunkType() == ChunkType.PROTOCHUNK) {
        }

        final ChunkAccess.TicksToSave tickSchedulers = chunk.getTicksForSerialization();
        final long gameTime = world.getLevelData().getGameTime();
        nbt.put("block_ticks", tickSchedulers.blocks().save(gameTime, block -> BuiltInRegistries.BLOCK.getKey(block).toString()));
        nbt.put("fluid_ticks", tickSchedulers.fluids().save(gameTime, fluid -> BuiltInRegistries.FLUID.getKey(fluid).toString()));

        final ShortList[] packOffsetList = chunk.getPostProcessing();
        final ListTag packOffsetsNBT = new ListTag();
        for (final ShortList shortlist : packOffsetList) {
            final ListTag packsNBT = new ListTag();
            if (shortlist != null) {
                for (final Short shortData : shortlist) {
                    packsNBT.add(ShortTag.valueOf(shortData));
                }
            }
            packOffsetsNBT.add(packsNBT);
        }
        nbt.put("PostProcessing", packOffsetsNBT);

        // 高度圖
        final CompoundTag heightmapsNBT = new CompoundTag();
        for (final Map.Entry<Heightmap.Types, Heightmap> entry : chunk.getHeightmaps()) {
            if (chunk.getStatus().heightmapsAfter().contains(entry.getKey())) {
                heightmapsNBT.put(entry.getKey().getSerializationKey(), new LongArrayTag(entry.getValue().getRawData()));
            }
        }
        nbt.put("Heightmaps", heightmapsNBT);

        return nbt;
    }
}
