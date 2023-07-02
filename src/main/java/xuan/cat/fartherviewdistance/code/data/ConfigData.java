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

  public ConfigData(JavaPlugin plugin, FileConfiguration fileConfiguration) {
    this.plugin = plugin;
    this.fileConfiguration = fileConfiguration;
    this.load();
  }

  public void reload() {
    this.plugin.reloadConfig();
    this.fileConfiguration = this.plugin.getConfig();
    this.load();
  }

  public int getServerSendTickMaxBytes() {
    return this.serverSendSecondMaxBytes / 20;
  }

  public ConfigData.World getWorld(String worldName) {
    return (ConfigData.World) this.worlds.getOrDefault(
        worldName,
        this.worldDefault
      );
  }

  private void load() {
    String viewDistanceModeString =
      this.fileConfiguration.getString("view-distance-mode", "X31");

    ViewMapMode viewDistanceMode;
    try {
      viewDistanceMode =
        ViewMapMode.valueOf(viewDistanceModeString.toUpperCase(Locale.ROOT));
    } catch (Exception var23) {
      throw new NullPointerException(
        "config.yml>view-distance-mode Non-existent option: " +
        viewDistanceModeString +
        " , allowed options: " +
        Arrays.toString(ViewMapMode.values())
      );
    }

    int serverViewDistance =
      this.fileConfiguration.getInt("server-view-distance", -1);
    boolean autoAdaptPlayerNetworkSpeed =
      this.fileConfiguration.getBoolean(
          "auto-adapt-player-network-speed",
          true
        );
    double playerNetworkSpeedUseDegree =
      this.fileConfiguration.getDouble("player-network-speed-use-degree", 0.6D);
    int asyncThreadAmount =
      this.fileConfiguration.getInt("async-thread-amount", 2);
    int serverSendSecondMaxBytes =
      this.fileConfiguration.getInt("server-send-second-max-bytes", 20971520);
    int serverTickMaxGenerateAmount =
      this.fileConfiguration.getInt("server-tick-max-generate-amount", 2);
    boolean calculateMissingHeightMap =
      this.fileConfiguration.getBoolean("calculate-missing-height-map", false);
    boolean disableFastProcess =
      this.fileConfiguration.getBoolean("disable-fast-process", false);
    ConfigurationSection permissionsConfiguration =
      this.fileConfiguration.getConfigurationSection("permissions");
    if (permissionsConfiguration == null) {
      throw new NullPointerException("config.yml>permissions");
    } else {
      Map<String, Integer> permissionsNodeMap = new HashMap();

      for (String line : permissionsConfiguration.getStringList("node-list")) {
        String[] lineSplit = line.split(";", 2);
        if (lineSplit.length != 2) {
          throw new NullPointerException(
            "config.yml>permissions->node-list Can't find the separator \";\": " +
            line
          );
        }

        permissionsNodeMap.put(lineSplit[1], Integer.parseInt(lineSplit[0]));
      }

      long permissionsPeriodicMillisecondCheck = permissionsConfiguration.getLong(
        "periodic-millisecond-check",
        60000L
      );
      ConfigurationSection worldsConfiguration =
        this.fileConfiguration.getConfigurationSection("worlds");
      Map<String, ConfigData.World> worlds = new HashMap();
      if (worldsConfiguration == null) {
        throw new NullPointerException("config.yml>worlds");
      } else {
        ConfigurationSection worldDefaultConfiguration = worldsConfiguration.getConfigurationSection(
          "default"
        );
        if (worldDefaultConfiguration == null) {
          worldDefaultConfiguration = new YamlConfiguration();
        }

        ConfigData.World worldDefault = new ConfigData.World(
          viewDistanceMode,
          "",
          worldDefaultConfiguration.getBoolean("enable", true),
          worldDefaultConfiguration.getInt("max-view-distance", 31),
          worldDefaultConfiguration.getInt("world-tick-max-generate-amount", 2),
          worldDefaultConfiguration.getBoolean("send-title-data", true),
          worldDefaultConfiguration.getInt(
            "world-send-second-max-bytes",
            10485760
          ),
          worldDefaultConfiguration.getInt(
            "player-send-second-max-bytes",
            2097152
          ),
          worldDefaultConfiguration.getBoolean(
            "read-server-loaded-chunk",
            true
          ),
          worldDefaultConfiguration.getInt("delay-before-send", 5000),
          this.parsePreventXray(
              worldDefaultConfiguration.getConfigurationSection("prevent-xray"),
              "default",
              (Map<BlockData, BlockData[]>) null
            ),
          worldDefaultConfiguration.getDouble("speeding-not-send", 1.2D)
        );

        for (String worldName : worldsConfiguration.getKeys(false)) {
          if (!worldName.equals("default")) {
            ConfigurationSection worldConfiguration = worldsConfiguration.getConfigurationSection(
              worldName
            );
            if (worldConfiguration != null) {
              worlds.put(
                worldName,
                new ConfigData.World(
                  viewDistanceMode,
                  worldName,
                  worldConfiguration.getBoolean("enable", worldDefault.enable),
                  worldConfiguration.getInt(
                    "max-view-distance",
                    worldDefault.maxViewDistance
                  ),
                  worldConfiguration.getInt(
                    "world-tick-max-generate-amount",
                    worldDefault.worldTickMaxGenerateAmount
                  ),
                  worldConfiguration.getBoolean(
                    "send-title-data",
                    worldDefault.sendTitleData
                  ),
                  worldConfiguration.getInt(
                    "world-send-second-max-bytes",
                    worldDefault.worldSendSecondMaxBytes
                  ),
                  worldConfiguration.getInt(
                    "player-send-second-max-bytes",
                    worldDefault.playerSendSecondMaxBytes
                  ),
                  worldConfiguration.getBoolean(
                    "read-server-loaded-chunk",
                    worldDefault.readServerLoadedChunk
                  ),
                  worldConfiguration.getInt(
                    "delay-before-send",
                    worldDefault.delayBeforeSend
                  ),
                  this.parsePreventXray(
                      worldConfiguration.getConfigurationSection(
                        "prevent-xray"
                      ),
                      worldName,
                      worldDefault.preventXray
                    ),
                  worldConfiguration.getDouble(
                    "speeding-not-send",
                    worldDefault.speedingNotSend
                  )
                )
              );
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
        this.permissionsPeriodicMillisecondCheck =
          permissionsPeriodicMillisecondCheck;
        this.worldDefault = worldDefault;
        this.worlds = worlds;
      }
    }
  }

  private Map<BlockData, BlockData[]> parsePreventXray(
    ConfigurationSection preventXrayConfiguration,
    String worldName,
    Map<BlockData, BlockData[]> defaultValue
  ) {
    if (preventXrayConfiguration == null) {
      return defaultValue;
    } else {
      Map<BlockData, BlockData[]> preventXrayConversionMap = new HashMap();
      if (preventXrayConfiguration.getBoolean("enable", true)) {
        ConfigurationSection conversionConfiguration = preventXrayConfiguration.getConfigurationSection(
          "conversion-list"
        );
        if (conversionConfiguration != null) {
          for (String toString : conversionConfiguration.getKeys(false)) {
            Material toMaterial = Material.getMaterial(toString.toUpperCase());
            if (toMaterial == null) {
              this.plugin.getLogger()
                .warning(
                  "worlds->" +
                  worldName +
                  "->prevent-xray->conversion-list Can't find this material: " +
                  toString
                );
            } else {
              List<Material> hitMaterials = new ArrayList();

              for (String hitString : conversionConfiguration.getStringList(
                toString
              )) {
                Material targetMaterial = Material.getMaterial(
                  hitString.toUpperCase()
                );
                if (targetMaterial == null) {
                  this.plugin.getLogger()
                    .warning(
                      "worlds->" +
                      worldName +
                      "->prevent-xray->conversion-list Can't find this material: " +
                      hitString
                    );
                } else {
                  hitMaterials.add(targetMaterial);
                }
              }

              BlockData[] materials = new BlockData[hitMaterials.size()];

              for (int i = 0; i < materials.length; ++i) {
                materials[i] =
                  ((Material) hitMaterials.get(i)).createBlockData();
              }

              preventXrayConversionMap.put(
                toMaterial.createBlockData(),
                materials
              );
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

    public World(
      ViewMapMode viewDistanceMode,
      String worldName,
      boolean enable,
      int maxViewDistance,
      int worldTickMaxGenerateAmount,
      boolean sendTitleData,
      int worldSendSecondMaxBytes,
      int playerSendSecondMaxBytes,
      boolean readServerLoadedChunk,
      int delayBeforeSend,
      Map<BlockData, BlockData[]> preventXray,
      double speedingNotSend
    ) {
      this.worldName = worldName;
      this.enable = enable;
      this.maxViewDistance = maxViewDistance;
      if (maxViewDistance > viewDistanceMode.getExtend()) {
        ConfigData.this.plugin.getLogger()
          .warning(
            "`max-view-distance: " +
            maxViewDistance +
            "` exceeded the maximum distance allowed by `view-distance-mode: " +
            viewDistanceMode.name() +
            "`"
          );
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

    public int getPlayerSendTickMaxBytes() {
      return this.playerSendSecondMaxBytes / 20;
    }

    public int getWorldSendTickMaxBytes() {
      return this.worldSendSecondMaxBytes / 20;
    }
  }
}
