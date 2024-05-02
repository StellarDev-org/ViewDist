package xuan.cat.fartherviewdistance.code;

import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.PluginCommand;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;
import xuan.cat.fartherviewdistance.api.branch.BranchMinecraft;
import xuan.cat.fartherviewdistance.api.branch.BranchPacket;
import xuan.cat.fartherviewdistance.code.branch.v120.Branch_120_Minecraft;
import xuan.cat.fartherviewdistance.code.branch.v120.Branch_120_Packet;
import xuan.cat.fartherviewdistance.code.branch.v120_2.Branch_120_2_Minecraft;
import xuan.cat.fartherviewdistance.code.branch.v120_2.Branch_120_2_Packet;
import xuan.cat.fartherviewdistance.code.branch.v120_4.Branch_120_4_Minecraft;
import xuan.cat.fartherviewdistance.code.branch.v120_4.Branch_120_4_Packet;
import xuan.cat.fartherviewdistance.code.branch.v120_6.Branch_120_6_Minecraft;
import xuan.cat.fartherviewdistance.code.branch.v120_6.Branch_120_6_Packet;
import xuan.cat.fartherviewdistance.code.branch.v14.Branch_14_Minecraft;
import xuan.cat.fartherviewdistance.code.branch.v14.Branch_14_Packet;
import xuan.cat.fartherviewdistance.code.branch.v15.Branch_15_Minecraft;
import xuan.cat.fartherviewdistance.code.branch.v15.Branch_15_Packet;
import xuan.cat.fartherviewdistance.code.branch.v16.Branch_16_Minecraft;
import xuan.cat.fartherviewdistance.code.branch.v16.Branch_16_Packet;
import xuan.cat.fartherviewdistance.code.branch.v17.Branch_17_Minecraft;
import xuan.cat.fartherviewdistance.code.branch.v17.Branch_17_Packet;
import xuan.cat.fartherviewdistance.code.branch.v18.Branch_18_Minecraft;
import xuan.cat.fartherviewdistance.code.branch.v18.Branch_18_Packet;
import xuan.cat.fartherviewdistance.code.branch.v19.Branch_19_Minecraft;
import xuan.cat.fartherviewdistance.code.branch.v19.Branch_19_Packet;
import xuan.cat.fartherviewdistance.code.command.Command;
import xuan.cat.fartherviewdistance.code.command.CommandSuggest;
import xuan.cat.fartherviewdistance.code.data.ConfigData;
import xuan.cat.fartherviewdistance.code.data.viewmap.ViewShape;

public final class ChunkIndex extends JavaPlugin {

    // private static ProtocolManager protocolManager;
    private static Plugin plugin;
    private static ChunkServer chunkServer;
    private static ConfigData configData;
    private static BranchPacket branchPacket;
    private static BranchMinecraft branchMinecraft;

    @Override
    public void onEnable() {
        // protocolManager = ProtocolLibrary.getProtocolManager();

        ((ChunkIndex) (ChunkIndex.plugin = (Plugin) this)).saveDefaultConfig();
        ChunkIndex.configData = new ConfigData(this, this.getConfig());

        // 檢測版本
        final String bukkitVersionOld = Bukkit.getBukkitVersion();
        final String bukkitVersion = Bukkit.getServer().getClass().getPackage().getName().replace("org.bukkit.craftbukkit", "").replace(".",
                "");
        if (bukkitVersionOld.matches("^1\\.14[^0-9].*$")) {
            // 1.14
            ChunkIndex.branchPacket = new Branch_14_Packet();
            ChunkIndex.branchMinecraft = new Branch_14_Minecraft();
            ChunkIndex.chunkServer = new ChunkServer(ChunkIndex.configData, (Plugin) this, ViewShape.SQUARE, ChunkIndex.branchMinecraft,
                    ChunkIndex.branchPacket);
        } else if (bukkitVersionOld.matches("^1\\.15\\D.*$")) {
            // 1.15
            ChunkIndex.branchPacket = new Branch_15_Packet();
            ChunkIndex.branchMinecraft = new Branch_15_Minecraft();
            ChunkIndex.chunkServer = new ChunkServer(ChunkIndex.configData, (Plugin) this, ViewShape.SQUARE, ChunkIndex.branchMinecraft,
                    ChunkIndex.branchPacket);
        } else if (bukkitVersionOld.matches("^1\\.16\\D.*$")) {
            // 1.16
            ChunkIndex.branchPacket = new Branch_16_Packet();
            ChunkIndex.branchMinecraft = new Branch_16_Minecraft();
            ChunkIndex.chunkServer = new ChunkServer(ChunkIndex.configData, (Plugin) this, ViewShape.SQUARE, ChunkIndex.branchMinecraft,
                    ChunkIndex.branchPacket);
        } else if (bukkitVersionOld.matches("^1\\.17\\D.*$")) {
            // 1.17
            ChunkIndex.branchPacket = new Branch_17_Packet();
            ChunkIndex.branchMinecraft = new Branch_17_Minecraft();
            ChunkIndex.chunkServer = new ChunkServer(ChunkIndex.configData, (Plugin) this, ViewShape.SQUARE, ChunkIndex.branchMinecraft,
                    ChunkIndex.branchPacket);
        } else if (bukkitVersion.equals("v1_18_R1")) {
            // 1.18
            ChunkIndex.branchPacket = new Branch_18_Packet();
            ChunkIndex.branchMinecraft = new Branch_18_Minecraft();
            ChunkIndex.chunkServer = new ChunkServer(ChunkIndex.configData, (Plugin) this, ViewShape.ROUND, ChunkIndex.branchMinecraft,
                    ChunkIndex.branchPacket);
        } else if (bukkitVersionOld.matches("^1\\.19\\D.*$")) {
            // 1.19
            ChunkIndex.branchPacket = new Branch_19_Packet();
            ChunkIndex.branchMinecraft = new Branch_19_Minecraft();
            ChunkIndex.chunkServer = new ChunkServer(ChunkIndex.configData, (Plugin) this, ViewShape.ROUND, ChunkIndex.branchMinecraft,
                    ChunkIndex.branchPacket);
        } else if (bukkitVersion.equals("v1_20_R1")) {
            // 1.20.1
            ChunkIndex.branchPacket = new Branch_120_Packet();
            ChunkIndex.branchMinecraft = new Branch_120_Minecraft();
            ChunkIndex.chunkServer = new ChunkServer(ChunkIndex.configData, (Plugin) this, ViewShape.ROUND, ChunkIndex.branchMinecraft,
                    ChunkIndex.branchPacket);
        } else if (bukkitVersion.equals("v1_20_R2")) {
            // 1.20.2
            ChunkIndex.branchPacket = new Branch_120_2_Packet();
            ChunkIndex.branchMinecraft = new Branch_120_2_Minecraft();
            ChunkIndex.chunkServer = new ChunkServer(ChunkIndex.configData, (Plugin) this, ViewShape.ROUND, ChunkIndex.branchMinecraft,
                    ChunkIndex.branchPacket);
        } else if (bukkitVersion.equals("v1_20_R3") || bukkitVersion.equals("v1_20_R4")) {
            // 1.20.3 || // 1.20.4
            ChunkIndex.branchPacket = new Branch_120_4_Packet();
            ChunkIndex.branchMinecraft = new Branch_120_4_Minecraft();
            ChunkIndex.chunkServer = new ChunkServer(ChunkIndex.configData, (Plugin) this, ViewShape.ROUND, ChunkIndex.branchMinecraft,
                    ChunkIndex.branchPacket);
        } else if (bukkitVersionOld.contains("1.20.6-R0.1")) {
            // 1.20.5
            ChunkIndex.branchPacket = new Branch_120_6_Packet();
            ChunkIndex.branchMinecraft = new Branch_120_6_Minecraft();
            ChunkIndex.chunkServer = new ChunkServer(ChunkIndex.configData, (Plugin) this, ViewShape.ROUND, ChunkIndex.branchMinecraft,
                    ChunkIndex.branchPacket);
        } else {
            this.getServer().getPluginManager().disablePlugin((Plugin) this);
            throw new IllegalArgumentException("Unsupported MC version: " + bukkitVersion + " | " + " Bukkit Version:" + bukkitVersionOld);
        }

        // 初始化一些資料
        for (final Player player : Bukkit.getOnlinePlayers()) {
            ChunkIndex.chunkServer.initView(player);
        }
        for (final World world : Bukkit.getWorlds()) {
            ChunkIndex.chunkServer.initWorld(world);
        }

        Bukkit.getPluginManager()
                .registerEvents(new ChunkEvent(ChunkIndex.chunkServer, ChunkIndex.branchPacket, ChunkIndex.branchMinecraft), (Plugin) this);
        // protocolManager.addPacketListener(new ChunkPacketEvent(plugin, chunkServer));

        if (Bukkit.getPluginManager().getPlugin("PlaceholderAPI") != null) {
            ChunkPlaceholder.registerPlaceholder();
        }

        // 指令
        final PluginCommand command = this.getCommand("viewdistance");
        if (command != null) {
            command.setExecutor((CommandExecutor) new Command(ChunkIndex.chunkServer, ChunkIndex.configData));
            command.setTabCompleter((TabCompleter) new CommandSuggest(ChunkIndex.chunkServer, ChunkIndex.configData));
        }
    }

    @Override
    public void onDisable() {
        // ChunkPlaceholder.unregisterPlaceholder();
        if (ChunkIndex.chunkServer != null)
            ChunkIndex.chunkServer.close();
    }

    public ChunkIndex() { super(); }

    public static ChunkServer getChunkServer() { return ChunkIndex.chunkServer; }

    public static ConfigData getConfigData() { return ChunkIndex.configData; }

    public static Plugin getPlugin() { return ChunkIndex.plugin; }
}
