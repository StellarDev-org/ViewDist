package xuan.cat.fartherviewdistance.code.data;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import org.bukkit.Material;
import org.bukkit.block.data.BlockData;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;
import xuan.cat.fartherviewdistance.code.data.ConfigData;
import xuan.cat.fartherviewdistance.code.data.viewmap.ViewMapMode;

@SuppressWarnings({ "rawtypes", "unchecked" })
public final class ConfigData {

    private final JavaPlugin plugin;
    public ViewMapMode viewDistanceMode;
    public int serverViewDistance;
    public boolean autoAdaptPlayerNetworkSpeed;
    public double playerNetworkSpeedUseDegree;
    public int asyncThreadAmount;
    public int serverTickMaxGenerateAmount;
    public boolean calculateMissingHeightMap;
    public boolean disableFastProcess;
    public List<Entry<String, Integer>> permissionsNodeList;
    public long permissionsPeriodicMillisecondCheck;
    private FileConfiguration fileConfiguration;
    private int serverSendSecondMaxBytes;
    private ConfigData.World worldDefault;
    private Map<String, ConfigData.World> worlds;

    public ConfigData(final JavaPlugin plugin, final FileConfiguration fileConfiguration) {
        this.plugin = plugin;
        this.fileConfiguration = fileConfiguration;
        this.load();
    }

    public void reload() {
        this.plugin.reloadConfig();
        this.fileConfiguration = this.plugin.getConfig();
        this.load();
    }

    public int getServerSendTickMaxBytes() { return this.serverSendSecondMaxBytes / 20; }

    public ConfigData.World getWorld(final String worldName) {
        return (ConfigData.World) this.worlds.getOrDefault(worldName, this.worldDefault);
    }

    private void load() {
        final String viewDistanceModeString = this.fileConfiguration.getString("view-distance-mode", "X31");

        ViewMapMode viewDistanceMode;
        try {
            viewDistanceMode = ViewMapMode.valueOf(viewDistanceModeString.toUpperCase(Locale.ROOT));
        } catch (final Exception var23) {
            throw new NullPointerException("config.yml>view-distance-mode Non-existent option: " + viewDistanceModeString
                    + " , allowed options: " + Arrays.toString(ViewMapMode.values()));
        }

        final int serverViewDistance = this.fileConfiguration.getInt("server-view-distance", -1);
        final boolean autoAdaptPlayerNetworkSpeed = this.fileConfiguration.getBoolean("auto-adapt-player-network-speed", true);
        final double playerNetworkSpeedUseDegree = this.fileConfiguration.getDouble("player-network-speed-use-degree", 0.6D);
        final int asyncThreadAmount = this.fileConfiguration.getInt("async-thread-amount", 2);
        final int serverSendSecondMaxBytes = this.fileConfiguration.getInt("server-send-second-max-bytes", 20971520);
        final int serverTickMaxGenerateAmount = this.fileConfiguration.getInt("server-tick-max-generate-amount", 2);
        final boolean calculateMissingHeightMap = this.fileConfiguration.getBoolean("calculate-missing-height-map", false);
        final boolean disableFastProcess = this.fileConfiguration.getBoolean("disable-fast-process", false);
        final ConfigurationSection permissionsConfiguration = this.fileConfiguration.getConfigurationSection("permissions");
        if (permissionsConfiguration == null) {
            throw new NullPointerException("config.yml>permissions");
        } else {
            final Map<String, Integer> permissionsNodeMap = new HashMap();

            for (final String line : permissionsConfiguration.getStringList("node-list")) {
                final String[] lineSplit = line.split(";", 2);
                if (lineSplit.length != 2) {
                    throw new NullPointerException("config.yml>permissions->node-list Can't find the separator \";\": " + line);
                }

                permissionsNodeMap.put(lineSplit[1], Integer.parseInt(lineSplit[0]));
            }

            final long permissionsPeriodicMillisecondCheck = permissionsConfiguration.getLong("periodic-millisecond-check", 60000L);
            final ConfigurationSection worldsConfiguration = this.fileConfiguration.getConfigurationSection("worlds");
            final Map<String, ConfigData.World> worlds = new HashMap();
            if (worldsConfiguration == null) {
                throw new NullPointerException("config.yml>worlds");
            } else {
                ConfigurationSection worldDefaultConfiguration = worldsConfiguration.getConfigurationSection("default");
                if (worldDefaultConfiguration == null) {
                    worldDefaultConfiguration = new YamlConfiguration();
                }

                final ConfigData.World worldDefault = new ConfigData.World(viewDistanceMode, "",
                        worldDefaultConfiguration.getBoolean("enable", true), worldDefaultConfiguration.getInt("max-view-distance", 31),
                        worldDefaultConfiguration.getInt("world-tick-max-generate-amount", 2),
                        worldDefaultConfiguration.getBoolean("send-title-data", true),
                        worldDefaultConfiguration.getInt("world-send-second-max-bytes", 10485760),
                        worldDefaultConfiguration.getInt("player-send-second-max-bytes", 2097152),
                        worldDefaultConfiguration.getBoolean("read-server-loaded-chunk", true),
                        worldDefaultConfiguration.getInt("delay-before-send", 5000),
                        this.parsePreventXray(worldDefaultConfiguration.getConfigurationSection("prevent-xray"), "default",
                                (Map<BlockData, BlockData[]>) null),
                        worldDefaultConfiguration.getDouble("speeding-not-send", 1.2D));

                for (final String worldName : worldsConfiguration.getKeys(false)) {
                    if (!worldName.equals("default")) {
                        final ConfigurationSection worldConfiguration = worldsConfiguration.getConfigurationSection(worldName);
                        if (worldConfiguration != null) {
                            worlds.put(worldName, new ConfigData.World(viewDistanceMode, worldName,
                                    worldConfiguration.getBoolean("enable", worldDefault.enable),
                                    worldConfiguration.getInt("max-view-distance", worldDefault.maxViewDistance),
                                    worldConfiguration.getInt("world-tick-max-generate-amount", worldDefault.worldTickMaxGenerateAmount),
                                    worldConfiguration.getBoolean("send-title-data", worldDefault.sendTitleData),
                                    worldConfiguration.getInt("world-send-second-max-bytes", worldDefault.worldSendSecondMaxBytes),
                                    worldConfiguration.getInt("player-send-second-max-bytes", worldDefault.playerSendSecondMaxBytes),
                                    worldConfiguration.getBoolean("read-server-loaded-chunk", worldDefault.readServerLoadedChunk),
                                    worldConfiguration.getInt("delay-before-send", worldDefault.delayBeforeSend),
                                    this.parsePreventXray(worldConfiguration.getConfigurationSection("prevent-xray"), worldName,
                                            worldDefault.preventXray),
                                    worldConfiguration.getDouble("speeding-not-send", worldDefault.speedingNotSend)));
                        }
                    }
                }

                this.viewDistanceMode = viewDistanceMode;
                this.serverViewDistance = serverViewDistance;
                this.autoAdaptPlayerNetworkSpeed = autoAdaptPlayerNetworkSpeed;
                this.playerNetworkSpeedUseDegree = playerNetworkSpeedUseDegree;
                this.asyncThreadAmount = asyncThreadAmount;
                this.serverSendSecondMaxBytes = serverSendSecondMaxBytes;
                this.serverTickMaxGenerateAmount = serverTickMaxGenerateAmount;
                this.calculateMissingHeightMap = calculateMissingHeightMap;
                this.disableFastProcess = disableFastProcess;
                this.permissionsNodeList = new ArrayList(permissionsNodeMap.entrySet());
                this.permissionsPeriodicMillisecondCheck = permissionsPeriodicMillisecondCheck;
                this.worldDefault = worldDefault;
                this.worlds = worlds;
            }
        }
    }

    private Map<BlockData, BlockData[]> parsePreventXray(final ConfigurationSection preventXrayConfiguration, final String worldName,
            final Map<BlockData, BlockData[]> defaultValue) {
        if (preventXrayConfiguration == null) {
            return defaultValue;
        } else {
            final Map<BlockData, BlockData[]> preventXrayConversionMap = new HashMap();
            if (preventXrayConfiguration.getBoolean("enable", true)) {
                final ConfigurationSection conversionConfiguration = preventXrayConfiguration.getConfigurationSection("conversion-list");
                if (conversionConfiguration != null) {
                    for (final String toString : conversionConfiguration.getKeys(false)) {
                        final Material toMaterial = Material.getMaterial(toString.toUpperCase());
                        if (toMaterial == null) {
                            this.plugin.getLogger().warning(
                                    "worlds->" + worldName + "->prevent-xray->conversion-list Can't find this material: " + toString);
                        } else {
                            final List<Material> hitMaterials = new ArrayList();

                            for (final String hitString : conversionConfiguration.getStringList(toString)) {
                                final Material targetMaterial = Material.getMaterial(hitString.toUpperCase());
                                if (targetMaterial == null) {
                                    this.plugin.getLogger().warning("worlds->" + worldName
                                            + "->prevent-xray->conversion-list Can't find this material: " + hitString);
                                } else {
                                    hitMaterials.add(targetMaterial);
                                }
                            }

                            final BlockData[] materials = new BlockData[hitMaterials.size()];

                            for (int i = 0; i < materials.length; ++i) {
                                materials[i] = ((Material) hitMaterials.get(i)).createBlockData();
                            }

                            preventXrayConversionMap.put(toMaterial.createBlockData(), materials);
                        }
                    }
                }
            }

            return preventXrayConversionMap;
        }
    }

    public class World {

        public final String worldName;
        public final boolean enable;
        public final int maxViewDistance;
        public final int worldTickMaxGenerateAmount;
        public final boolean sendTitleData;
        public final boolean readServerLoadedChunk;
        public final int delayBeforeSend;
        public final Map<BlockData, BlockData[]> preventXray;
        public final double speedingNotSend;
        private final int worldSendSecondMaxBytes;
        private final int playerSendSecondMaxBytes;

        public World(final ViewMapMode viewDistanceMode, final String worldName, final boolean enable, final int maxViewDistance,
                final int worldTickMaxGenerateAmount, final boolean sendTitleData, final int worldSendSecondMaxBytes,
                final int playerSendSecondMaxBytes, final boolean readServerLoadedChunk, final int delayBeforeSend,
                final Map<BlockData, BlockData[]> preventXray, final double speedingNotSend) {
            this.worldName = worldName;
            this.enable = enable;
            this.maxViewDistance = maxViewDistance;
            if (maxViewDistance > viewDistanceMode.getExtend()) {
                ConfigData.this.plugin.getLogger().warning("`max-view-distance: " + maxViewDistance
                        + "` exceeded the maximum distance allowed by `view-distance-mode: " + viewDistanceMode.name() + "`");
            }

            this.worldTickMaxGenerateAmount = worldTickMaxGenerateAmount;
            this.sendTitleData = sendTitleData;
            this.worldSendSecondMaxBytes = worldSendSecondMaxBytes;
            this.playerSendSecondMaxBytes = playerSendSecondMaxBytes;
            this.readServerLoadedChunk = readServerLoadedChunk;
            this.delayBeforeSend = delayBeforeSend;
            this.preventXray = preventXray;
            this.speedingNotSend = speedingNotSend;
        }

        public int getPlayerSendTickMaxBytes() { return this.playerSendSecondMaxBytes / 20; }

        public int getWorldSendTickMaxBytes() { return this.worldSendSecondMaxBytes / 20; }
    }
}
