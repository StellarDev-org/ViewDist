package xuan.cat.fartherviewdistance.code;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitScheduler;
import org.bukkit.scheduler.BukkitTask;

import xuan.cat.fartherviewdistance.api.branch.BranchChunk;
import xuan.cat.fartherviewdistance.api.branch.BranchChunkLight;
import xuan.cat.fartherviewdistance.api.branch.BranchMinecraft;
import xuan.cat.fartherviewdistance.api.branch.BranchNBT;
import xuan.cat.fartherviewdistance.api.branch.BranchPacket;
import xuan.cat.fartherviewdistance.api.branch.packet.PacketEvent;
import xuan.cat.fartherviewdistance.api.branch.packet.PacketMapChunkEvent;
import xuan.cat.fartherviewdistance.api.event.PlayerSendExtendChunkEvent;
import xuan.cat.fartherviewdistance.code.data.ConfigData;
import xuan.cat.fartherviewdistance.code.data.CumulativeReport;
import xuan.cat.fartherviewdistance.code.data.LangFiles;
import xuan.cat.fartherviewdistance.code.data.NetworkTraffic;
import xuan.cat.fartherviewdistance.code.data.PlayerChunkView;
import xuan.cat.fartherviewdistance.code.data.viewmap.ViewMap;
import xuan.cat.fartherviewdistance.code.data.viewmap.ViewShape;

/**
 * 區塊伺服器
 */
@SuppressWarnings("unused")
public final class ChunkServer {

    public static final Random random = new Random();
    public final BranchMinecraft branchMinecraft;
    public final BranchPacket branchPacket;
    public final Map<Player, PlayerChunkView> playersViewMap = new ConcurrentHashMap<>();
    public final CumulativeReport serverCumulativeReport = new CumulativeReport();
    public final Map<World, CumulativeReport> worldsCumulativeReport = new ConcurrentHashMap<>();
    public final Map<Integer, CumulativeReport> threadsCumulativeReport = new ConcurrentHashMap<>();
    public final Set<Thread> threadsSet = ConcurrentHashMap.newKeySet();
    public final LangFiles lang = new LangFiles();
    private final ConfigData configData;
    private final Plugin plugin;
    private final Set<BukkitTask> bukkitTasks = ConcurrentHashMap.newKeySet();
    private final NetworkTraffic serverNetworkTraffic = new NetworkTraffic();
    private final Map<World, NetworkTraffic> worldsNetworkTraffic = new ConcurrentHashMap<>();
    private final AtomicInteger serverGeneratedChunk = new AtomicInteger(0);
    private final Map<World, AtomicInteger> worldsGeneratedChunk = new ConcurrentHashMap<>();
    private final Set<Runnable> waitMoveSyncQueue = ConcurrentHashMap.newKeySet();
    private final ViewShape viewShape;
    public volatile boolean globalPause = false;
    private boolean running = true;
    private ScheduledExecutorService multithreadedService;
    private AtomicBoolean multithreadedCanRun;
    private List<World> lastWorldList = new ArrayList<>();

    public ChunkServer(final ConfigData configData, final Plugin plugin, final ViewShape viewShape, final BranchMinecraft branchMinecraft,
            final BranchPacket branchPacket) {
        this.configData = configData;
        this.plugin = plugin;
        this.branchMinecraft = branchMinecraft;
        this.branchPacket = branchPacket;
        this.viewShape = viewShape;
        final BukkitScheduler scheduler = Bukkit.getScheduler();
        this.bukkitTasks.add(scheduler.runTaskTimer(plugin, this::tickSync, 0, 1));
        this.bukkitTasks.add(scheduler.runTaskTimerAsynchronously(plugin, this::tickAsync, 0, 1));
        this.bukkitTasks.add(scheduler.runTaskTimerAsynchronously(plugin, this::tickReport, 0, 20));
        this.reloadMultithreaded();
    }

    /**
     * The function creates and returns a new PlayerChunkView object, and adds it to
     * a map of player views.
     *
     * @param player The player object represents a player in the game. It likely
     *                   contains information such as the player's name, level,
     *                   inventory, and other relevant data.
     * @return The method is returning an instance of the PlayerChunkView class.
     */
    public PlayerChunkView initView(final Player player) {
        final PlayerChunkView view = new PlayerChunkView(player, this.configData, this.viewShape, this.branchPacket);
        this.playersViewMap.put(player, view);
        return view;
    }

    /**
     * The clearView function removes a player's view from the playersViewMap.
     *
     * @param player The "player" parameter is an object of the Player class.
     */
    public void clearView(final Player player) { this.playersViewMap.remove(player); }

    /**
     * The getView function returns the PlayerChunkView associated with a given
     * player.
     *
     * @param player The player object for which we want to retrieve the view.
     * @return The method is returning an object of type PlayerChunkView.
     */
    public PlayerChunkView getView(final Player player) { return (PlayerChunkView) this.playersViewMap.get(player); }

    /**
     * The `reloadMultithreaded` function initializes and schedules multiple threads
     * for running tasks concurrently.
     */
    public synchronized void reloadMultithreaded() {
        if (this.multithreadedCanRun != null) {
            this.multithreadedCanRun.set(false);
        }

        if (this.multithreadedService != null) {
            this.multithreadedService.shutdown();
        }

        this.threadsCumulativeReport.clear();
        this.threadsSet.clear();
        this.playersViewMap.values().forEach(view -> view.waitSend = false);
        final AtomicBoolean canRun = new AtomicBoolean(true);
        this.multithreadedCanRun = canRun;
        this.multithreadedService = Executors.newScheduledThreadPool(this.configData.asyncThreadAmount + 1);
        this.multithreadedService.schedule(() -> {
            final Thread thread = Thread.currentThread();
            thread.setName("FartherViewDistance View thread");
            thread.setPriority(3);
            this.threadsSet.add(thread);
            this.runView(canRun);
        }, 0L, TimeUnit.MILLISECONDS);

        for (int index = 0; index < this.configData.asyncThreadAmount; ++index) {
            final int threadNumber = index;
            final CumulativeReport threadCumulativeReport = new CumulativeReport();
            this.threadsCumulativeReport.put(index, threadCumulativeReport);
            this.multithreadedService.schedule(() -> {
                final Thread thread = Thread.currentThread();
                thread.setName("FartherViewDistance AsyncTick thread #" + threadNumber);
                thread.setPriority(2);
                this.threadsSet.add(thread);
                this.runThread(canRun, threadCumulativeReport);
            }, 0L, TimeUnit.MILLISECONDS);
        }
    }

    /**
     * The function initializes various data structures for a given world in a Java
     * program.
     *
     * @param world The "world" parameter is an instance of the "World" class.
     */
    public void initWorld(final World world) {
        this.worldsNetworkTraffic.put(world, new NetworkTraffic());
        this.worldsCumulativeReport.put(world, new CumulativeReport());
        this.worldsGeneratedChunk.put(world, new AtomicInteger(0));
    }

    /**
     * The function clears specific data associated with a given world in a Java
     * program.
     *
     * @param world The "world" parameter is an object of the World class.
     */
    public void clearWorld(final World world) {
        this.worldsNetworkTraffic.remove(world);
        this.worldsCumulativeReport.remove(world);
        this.worldsGeneratedChunk.remove(world);
    }

    /**
     * The tickSync function shuffles the list of worlds, updates the lastWorldList
     * variable, and removes all runnables from the waitMoveSyncQueue.
     */
    private void tickSync() {
        final List<World> worldList = Bukkit.getWorlds();
        Collections.shuffle(worldList);
        this.lastWorldList = worldList;
        this.waitMoveSyncQueue.removeIf(runnable -> {
            try {
                runnable.run();
            } catch (final Exception exception) {
                exception.printStackTrace();
            }

            return true;
        });
    }

    /**
     * The tickAsync function updates network traffic and generated chunk values for
     * the server, worlds, and player views.
     */
    private void tickAsync() {
        this.serverNetworkTraffic.next();
        this.worldsNetworkTraffic.values().forEach(NetworkTraffic::next);
        this.playersViewMap.values().forEach(view -> {
            view.networkTraffic.next();
            view.networkSpeed.next();
        });
        this.serverGeneratedChunk.set(0);
        this.worldsGeneratedChunk.values().forEach(generatedChunk -> generatedChunk.set(0));
    }

    /**
     * The tickReport function updates various cumulative reports in the server,
     * worlds, players, and threads.
     */
    private void tickReport() {
        this.serverCumulativeReport.next();
        this.worldsCumulativeReport.values().forEach(CumulativeReport::next);
        this.playersViewMap.values().forEach(view -> view.cumulativeReport.next());
        this.threadsCumulativeReport.values().forEach(CumulativeReport::next);
    }

    /**
     * The function runs a view for players, updating their distance and checking
     * for overspeed, with a sleep time of 50 milliseconds between iterations.
     *
     * @param canRun The "canRun" parameter is an AtomicBoolean object that is used
     *                   to control the execution of the while loop in the "runView"
     *                   method. If the value of "canRun" is true, the loop will
     *                   continue running. If the value of "canRun" is false, the
     *                   loop
     */
    private void runView(final AtomicBoolean canRun) {
        while (canRun.get()) {
            final long startTime = System.currentTimeMillis();

            try {
                this.playersViewMap.forEach((player, view) -> {
                    if (!view.install()) {
                        view.updateDistance();
                    }

                    view.moveTooFast = view.overSpeed();
                });
            } catch (final Exception exception) {
                exception.printStackTrace();
            }

            final long endTime = System.currentTimeMillis();
            final long needSleep = 50 - (endTime - startTime);
            if (needSleep > 0) {
                try {
                    Thread.sleep(needSleep);
                } catch (final InterruptedException ignored) {
                }
            }
        }
    }

    /**
     * The `runThread` function runs a continuous loop that performs various
     * operations on a list of `PlayerChunkView` objects, based on certain
     * conditions and configurations.
     *
     * @param canRun                 A boolean flag that indicates whether the
     *                                   thread should continue running or not. If
     *                                   the value is true, the thread will continue
     *                                   running. If the value is false, the thread
     *                                   will stop running and exit the loop.
     * @param threadCumulativeReport The `threadCumulativeReport` parameter is an
     *                                   instance of the `CumulativeReport` class.
     *                                   It is used to keep track of the cumulative
     *                                   report for the current thread. The
     *                                   `CumulativeReport` class likely contains
     *                                   methods to increment various counters or
     *                                   statistics related to the thread's
     *                                   execution.
     */
    @SuppressWarnings({ "rawtypes", "unchecked" })
    private void runThread(final AtomicBoolean canRun, final CumulativeReport threadCumulativeReport) {
        while (canRun.get()) {
            final long startTime = System.currentTimeMillis();
            final long effectiveTime = startTime + 50;
            if (!this.globalPause) {
                try {
                    final List<World> worldList = this.lastWorldList;
                    final List<PlayerChunkView> viewList = Arrays
                            .asList((PlayerChunkView[]) this.playersViewMap.values().toArray(new PlayerChunkView[0]));
                    Collections.shuffle(viewList);

                    for (final PlayerChunkView view : viewList) {
                        view.move();
                    }

                    final Map<World, List<PlayerChunkView>> worldsViews = new HashMap<>();

                    for (final PlayerChunkView view : viewList) {
                        ((List) worldsViews.computeIfAbsent(view.getLastWorld(), key -> new ArrayList<>())).add(view);
                    }

                    handleServer: {
                        for (final World world : worldList) {
                            // 世界配置
                            final ConfigData.World configWorld = this.configData.getWorld(world.getName());
                            if (!configWorld.enable)
                                continue;
                            // 世界報告
                            final CumulativeReport worldCumulativeReport = this.worldsCumulativeReport.get(world);
                            if (worldCumulativeReport == null)
                                continue;
                            // 世界網路流量
                            final NetworkTraffic worldNetworkTraffic = this.worldsNetworkTraffic.get(world);
                            if (worldNetworkTraffic == null)
                                continue;
                            if (this.serverNetworkTraffic.exceed(this.configData.getServerSendTickMaxBytes()))
                                break handleServer;
                            if (worldNetworkTraffic.exceed(configWorld.getWorldSendTickMaxBytes()))
                                continue;

                            /// 世界已生成的區塊數量
                            final AtomicInteger worldGeneratedChunk = this.worldsGeneratedChunk.getOrDefault(world,
                                    new AtomicInteger(Integer.MAX_VALUE));

                            handleWorld: {
                                // 所有玩家都網路流量都已滿載
                                boolean playersFull = false;
                                while (!playersFull && effectiveTime >= System.currentTimeMillis()) {
                                    playersFull = true;
                                    for (final PlayerChunkView view : worldsViews.getOrDefault(world, new ArrayList<>(0))) {
                                        if (this.serverNetworkTraffic.exceed(this.configData.getServerSendTickMaxBytes()))
                                            break handleServer;
                                        if (worldNetworkTraffic.exceed(configWorld.getWorldSendTickMaxBytes()))
                                            break handleWorld;
                                        synchronized (view.networkTraffic) {
                                            final Integer forciblySendSecondMaxBytes = view.forciblySendSecondMaxBytes;
                                            if (view.networkTraffic.exceed(forciblySendSecondMaxBytes != null
                                                    ? (int) (forciblySendSecondMaxBytes * this.configData.playerNetworkSpeedUseDegree) / 20
                                                    : configWorld.getPlayerSendTickMaxBytes()))
                                                continue;
                                            if (this.configData.autoAdaptPlayerNetworkSpeed
                                                    && view.networkTraffic.exceed(Math.max(1, view.networkSpeed.avg() * 50)))
                                                continue;
                                        }
                                        if (view.waitSend) {
                                            playersFull = false;
                                            continue;
                                        }
                                        if (view.moveTooFast)
                                            continue;
                                        view.waitSend = true;
                                        final long syncKey = view.syncKey;
                                        final Long chunkKey = view.next();
                                        if (chunkKey == null) {
                                            view.waitSend = false;
                                            continue;
                                        }
                                        playersFull = false;
                                        final int chunkX = ViewMap.getX(chunkKey);
                                        final int chunkZ = ViewMap.getZ(chunkKey);

                                        handlePlayer: {
                                            if (!this.configData.disableFastProcess) {
                                                // 讀取最新
                                                try {
                                                    if (configWorld.readServerLoadedChunk) {
                                                        final BranchChunk chunk = this.branchMinecraft.getChunkFromMemoryCache(world,
                                                                chunkX, chunkZ);
                                                        if (chunk != null) {
                                                            // 讀取快取
                                                            this.serverCumulativeReport.increaseLoadFast();
                                                            worldCumulativeReport.increaseLoadFast();
                                                            view.cumulativeReport.increaseLoadFast();
                                                            threadCumulativeReport.increaseLoadFast();
                                                            final List<Runnable> asyncRunnable = new ArrayList<>();
                                                            final BranchChunkLight chunkLight = this.branchMinecraft.fromLight(world);
                                                            final BranchNBT chunkNBT = chunk.toNBT(chunkLight, asyncRunnable);
                                                            asyncRunnable.forEach(Runnable::run);
                                                            this.sendChunk(world, configWorld, worldNetworkTraffic, view, chunkX, chunkZ,
                                                                    chunkNBT, chunkLight, syncKey, worldCumulativeReport,
                                                                    threadCumulativeReport);
                                                            break handlePlayer;
                                                        }
                                                    }
                                                } catch (NullPointerException | NoClassDefFoundError | NoSuchMethodError
                                                        | NoSuchFieldError exception) {
                                                    exception.printStackTrace();
                                                } catch (final Exception ignored) {
                                                }

                                                // 讀取最快
                                                try {
                                                    final BranchNBT chunkNBT = this.branchMinecraft.getChunkNBTFromDisk(world, chunkX,
                                                            chunkZ);
                                                    if (chunkNBT != null
                                                            && this.branchMinecraft.fromStatus(chunkNBT).isAbove(BranchChunk.Status.FULL)) {
                                                        // 讀取區域文件
                                                        this.serverCumulativeReport.increaseLoadFast();
                                                        worldCumulativeReport.increaseLoadFast();
                                                        view.cumulativeReport.increaseLoadFast();
                                                        threadCumulativeReport.increaseLoadFast();
                                                        this.sendChunk(world, configWorld, worldNetworkTraffic, view, chunkX, chunkZ,
                                                                chunkNBT, this.branchMinecraft.fromLight(world, chunkNBT), syncKey,
                                                                worldCumulativeReport, threadCumulativeReport);
                                                        break handlePlayer;
                                                    }
                                                } catch (NullPointerException | NoClassDefFoundError | NoSuchMethodError
                                                        | NoSuchFieldError exception) {
                                                    exception.printStackTrace();
                                                } catch (final Exception ignored) {
                                                }
                                            }

                                            final boolean canGenerated = this.serverGeneratedChunk
                                                    .get() < this.configData.serverTickMaxGenerateAmount
                                                    && worldGeneratedChunk.get() < configWorld.worldTickMaxGenerateAmount;
                                            if (canGenerated) {
                                                this.serverGeneratedChunk.incrementAndGet();
                                                worldGeneratedChunk.incrementAndGet();
                                            }

                                            // 生成
                                            try {
                                                // paper
                                                final Chunk chunk = world.getChunkAtAsync(chunkX, chunkZ, canGenerated, true).get();
                                                if (chunk != null) {
                                                    this.serverCumulativeReport.increaseLoadSlow();
                                                    worldCumulativeReport.increaseLoadSlow();
                                                    view.cumulativeReport.increaseLoadSlow();
                                                    threadCumulativeReport.increaseLoadSlow();
                                                    try {
                                                        final List<Runnable> asyncRunnable = new ArrayList<>();
                                                        final BranchChunkLight chunkLight = this.branchMinecraft.fromLight(world);
                                                        final BranchNBT chunkNBT = this.branchMinecraft.fromChunk(world, chunk)
                                                                .toNBT(chunkLight, asyncRunnable);
                                                        asyncRunnable.forEach(Runnable::run);
                                                        this.sendChunk(world, configWorld, worldNetworkTraffic, view, chunkX, chunkZ,
                                                                chunkNBT, chunkLight, syncKey, worldCumulativeReport,
                                                                threadCumulativeReport);
                                                        break handlePlayer;
                                                    } catch (NullPointerException | NoClassDefFoundError | NoSuchMethodError
                                                            | NoSuchFieldError exception) {
                                                        exception.printStackTrace();
                                                    } catch (final Exception ignored) {
                                                    }
                                                } else if (this.configData.serverTickMaxGenerateAmount > 0
                                                        && configWorld.worldTickMaxGenerateAmount > 0) {
                                                    view.remove(chunkX, chunkZ);
                                                    break handlePlayer;
                                                }
                                            } catch (final ExecutionException ignored) {
                                                view.remove(chunkX, chunkZ);
                                                break handlePlayer;
                                            } catch (final NoSuchMethodError methodError) {
                                                // spigot (不推薦)
                                                if (canGenerated) {
                                                    this.serverCumulativeReport.increaseLoadSlow();
                                                    worldCumulativeReport.increaseLoadSlow();
                                                    view.cumulativeReport.increaseLoadSlow();
                                                    threadCumulativeReport.increaseLoadSlow();
                                                    try {
                                                        final List<Runnable> asyncRunnable = new ArrayList<>();
                                                        final BranchChunkLight chunkLight = this.branchMinecraft.fromLight(world);
                                                        final CompletableFuture<BranchNBT> syncNBT = new CompletableFuture<>();
                                                        this.waitMoveSyncQueue.add(() -> syncNBT.complete(
                                                                this.branchMinecraft.fromChunk(world, world.getChunkAt(chunkX, chunkZ))
                                                                        .toNBT(chunkLight, asyncRunnable)));
                                                        final BranchNBT chunkNBT = syncNBT.get();
                                                        asyncRunnable.forEach(Runnable::run);
                                                        this.sendChunk(world, configWorld, worldNetworkTraffic, view, chunkX, chunkZ,
                                                                chunkNBT, chunkLight, syncKey, worldCumulativeReport,
                                                                threadCumulativeReport);
                                                        break handlePlayer;
                                                    } catch (NullPointerException | NoClassDefFoundError | NoSuchMethodError
                                                            | NoSuchFieldError exception) {
                                                        exception.printStackTrace();
                                                    } catch (final Exception ignored) {
                                                    }
                                                }
                                            } catch (final InterruptedException ignored) {
                                            } catch (final Exception ex) {
                                                ex.printStackTrace();
                                            }
                                        }

                                        view.waitSend = false;
                                    }

                                    try {
                                        Thread.sleep(0L);
                                    } catch (final InterruptedException ignored) {
                                    }
                                }
                            }
                        }
                    }
                } catch (final Exception exception) {
                    exception.printStackTrace();
                }
            }

            final long endTime = System.currentTimeMillis();
            final long needSleep = 50 - (endTime - startTime);
            if (needSleep > 0) {
                try {
                    Thread.sleep(needSleep);
                } catch (final InterruptedException ignored) {
                }
            }
        }
    }

    /**
     * The function sends a chunk of data to a player in a Minecraft world, taking
     * into account various configurations and network traffic.
     *
     * @param world                  The `world` parameter is an instance of the
     *                                   `World` class, which represents the
     *                                   Minecraft world in which the chunk is
     *                                   located.
     * @param configWorld            The `configWorld` parameter is an object of
     *                                   type `ConfigData.World` which contains
     *                                   configuration data specific to the world in
     *                                   which the chunk is being sent.
     * @param worldNetworkTraffic    The `worldNetworkTraffic` parameter is an
     *                                   object that represents the network traffic
     *                                   for the entire world. It is used to track
     *                                   the amount of network traffic used by the
     *                                   chunk being sent.
     * @param view                   The "view" parameter represents the player's
     *                                   chunk view, which contains information
     *                                   about the chunks that the player can see.
     * @param chunkX                 The `chunkX` parameter represents the X
     *                                   coordinate of the chunk that needs to be
     *                                   sent.
     * @param chunkZ                 The parameter `chunkZ` represents the
     *                                   Z-coordinate of the chunk that needs to be
     *                                   sent.
     * @param chunkNBT               The chunkNBT parameter is an object that
     *                                   represents the NBT (Named Binary Tag) data
     *                                   of the chunk. NBT is a data format used by
     *                                   Minecraft to store structured data. In this
     *                                   case, the chunkNBT object contains the NBT
     *                                   data for the chunk being sent.
     * @param chunkLight             The parameter `chunkLight` is of type
     *                                   `BranchChunkLight` and represents the light
     *                                   data for the chunk being sent.
     * @param syncKey                A unique identifier used to synchronize the
     *                                   sending of chunks between the server and
     *                                   the player.
     * @param worldCumulativeReport  The `worldCumulativeReport` parameter is an
     *                                   object of type `CumulativeReport` that
     *                                   represents the cumulative report for the
     *                                   world. It is used to track the cumulative
     *                                   network traffic consumption for the world.
     * @param threadCumulativeReport The parameter `threadCumulativeReport` is of
     *                                   type `CumulativeReport`. It is used to
     *                                   track the cumulative network traffic
     *                                   consumption for a specific thread.
     */
    private void sendChunk(final World world, final ConfigData.World configWorld, final NetworkTraffic worldNetworkTraffic,
            final PlayerChunkView view, final int chunkX, final int chunkZ, final BranchNBT chunkNBT, final BranchChunkLight chunkLight,
            final long syncKey, final CumulativeReport worldCumulativeReport, final CumulativeReport threadCumulativeReport) {
        final BranchChunk chunk = this.branchMinecraft.fromChunk(world, chunkX, chunkZ, chunkNBT,
                this.configData.calculateMissingHeightMap);
        final PlayerSendExtendChunkEvent event = new PlayerSendExtendChunkEvent(view.viewAPI, chunk, world);
        Bukkit.getPluginManager().callEvent(event);
        if (event.isCancelled())
            return;
        if (configWorld.preventXray != null && configWorld.preventXray.size() > 0) {
            for (final Map.Entry<BlockData, BlockData[]> conversionMap : configWorld.preventXray.entrySet())
                chunk.replaceAllMaterial(conversionMap.getValue(), conversionMap.getKey());
        }

        final AtomicInteger consumeTraffic = new AtomicInteger(0);
        final Consumer<Player> chunkAndLightPacket = this.branchPacket.sendChunkAndLight(chunk, chunkLight, configWorld.sendTitleData,
                consumeTraffic::addAndGet);
        synchronized (view.networkSpeed) {
            final Location nowLoc = view.getPlayer().getLocation();
            final int nowChunkX = nowLoc.getBlockX() >> 4;
            final int nowChunkZ = nowLoc.getBlockZ() >> 4;
            final ViewMap viewMap = view.getMap();
            if (world != nowLoc.getWorld()) {
                view.getMap().markWaitPosition(chunkX, chunkZ);
                return;
            }
            if (view.getMap().isWaitPosition(chunkX, chunkZ))
                return;
            if (this.viewShape.isInsideEdge(nowChunkX, nowChunkZ, chunkX, chunkZ, viewMap.serverDistance))
                return;
            if (view.syncKey != syncKey)
                return;
            if (!this.running)
                return;

            final boolean needMeasure = this.configData.autoAdaptPlayerNetworkSpeed
                    && (view.networkSpeed.speedID == null && view.networkSpeed.speedTimestamp + 1000 <= System.currentTimeMillis()
                            || view.networkSpeed.speedTimestamp + 30000 <= System.currentTimeMillis());
            // ping
            if (needMeasure) {
                if (view.networkSpeed.speedID != null) {
                    view.networkSpeed.add(30000, 0);
                }

                final long pingID = ChunkServer.random.nextLong();
                view.networkSpeed.pingID = pingID;
                view.networkSpeed.pingTimestamp = System.currentTimeMillis();
                this.branchPacket.sendKeepAlive(view.getPlayer(), pingID);
            }

            chunkAndLightPacket.accept(view.getPlayer());
            this.serverNetworkTraffic.use(consumeTraffic.get());
            worldNetworkTraffic.use(consumeTraffic.get());
            view.networkTraffic.use(consumeTraffic.get());
            this.serverCumulativeReport.addConsume(consumeTraffic.get());
            worldCumulativeReport.addConsume(consumeTraffic.get());
            view.cumulativeReport.addConsume(consumeTraffic.get());
            threadCumulativeReport.addConsume(consumeTraffic.get());

            if (needMeasure) {
                final long speedID = ChunkServer.random.nextLong();
                view.networkSpeed.speedID = speedID;
                view.networkSpeed.speedConsume = consumeTraffic.get();
                view.networkSpeed.speedTimestamp = System.currentTimeMillis();
                this.branchPacket.sendKeepAlive(view.getPlayer(), speedID);
            }
        }
    }

    /**
     * The function handles a packet event for a player and sends a specific chunk
     * of a map to the player if it exists in their view.
     *
     * @param player The "player" parameter is of type Player and represents the
     *                   player who triggered the packet event.
     * @param event  The "event" parameter in the "packetEvent" method is of type
     *                   PacketEvent. It represents the event that is triggered when
     *                   a packet is received or sent by the player.
     */
    public void packetEvent(final Player player, final PacketEvent event) {
        final PlayerChunkView view = this.getView(player);
        if (view != null && event instanceof final PacketMapChunkEvent chunkEvent) {
            view.send(chunkEvent.getChunkX(), chunkEvent.getChunkZ());
        }
    }

    /**
     * The function "respawnView" delays the player's view and adds a runnable task
     * to the waitMoveSyncQueue to send the view distance to the player.
     *
     * @param player The "player" parameter is an object of the Player class,
     *                   representing a player in the game.
     */
    public void respawnView(final Player player) {
        final PlayerChunkView view = this.getView(player);
        if (view != null) {
            view.delay();
            this.waitMoveSyncQueue.add(() -> this.branchPacket.sendViewDistance(player, view.getMap().extendDistance));
        }
    }

    /**
     * The function unloads a player's view if they move too far away from their
     * current location.
     *
     * @param player The "player" parameter represents the player for whom the view
     *                   is being unloaded.
     * @param from   The "from" parameter represents the previous location of the
     *                   player.
     * @param move   The "move" parameter represents the new location that the
     *                   player is moving to.
     */
    public void unloadView(final Player player, final Location from, final Location move) {
        final PlayerChunkView view = this.getView(player);
        if (view == null)
            return;
        final int blockDistance = view.getMap().extendDistance << 4;
        if (from.getWorld() != move.getWorld())
            view.unload();
        else if (Math.abs(from.getX() - move.getX()) >= blockDistance || Math.abs(from.getZ() - move.getZ()) >= blockDistance)
            view.unload();
    }

    /**
     * The close() function sets the running flag to false, cancels all Bukkit
     * tasks, and shuts down the multithreaded service.
     */
    void close() {
        this.running = false;

        for (final BukkitTask task : this.bukkitTasks) {
            task.cancel();
        }

        this.multithreadedService.shutdown();
    }
}
