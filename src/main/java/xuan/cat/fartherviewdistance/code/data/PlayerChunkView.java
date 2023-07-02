package xuan.cat.fartherviewdistance.code.data;

import java.util.Map.Entry;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.WorldBorder;
import org.bukkit.entity.Player;
import xuan.cat.fartherviewdistance.api.branch.BranchPacket;
import xuan.cat.fartherviewdistance.api.data.PlayerView;
import xuan.cat.fartherviewdistance.api.event.PlayerCheckViewDistanceEvent;
import xuan.cat.fartherviewdistance.api.event.PlayerInitViewEvent;
import xuan.cat.fartherviewdistance.api.event.PlayerSendUnloadChunkEvent;
import xuan.cat.fartherviewdistance.api.event.PlayerSendViewDistanceEvent;
import xuan.cat.fartherviewdistance.api.event.PlayerViewMarkSendChunkEvent;
import xuan.cat.fartherviewdistance.api.event.PlayerViewMarkWaitChunkEvent;
import xuan.cat.fartherviewdistance.code.ChunkServer;
import xuan.cat.fartherviewdistance.code.data.ConfigData;
import xuan.cat.fartherviewdistance.code.data.CumulativeReport;
import xuan.cat.fartherviewdistance.code.data.NetworkSpeed;
import xuan.cat.fartherviewdistance.code.data.NetworkTraffic;
import xuan.cat.fartherviewdistance.code.data.viewmap.ViewMap;
import xuan.cat.fartherviewdistance.code.data.viewmap.ViewShape;

public final class PlayerChunkView {

  public final PlayerView viewAPI;
  public final NetworkTraffic networkTraffic = new NetworkTraffic();
  public final NetworkSpeed networkSpeed = new NetworkSpeed();
  public final CumulativeReport cumulativeReport = new CumulativeReport();
  private final Player player;
  private final BranchPacket branchPacket;
  private final ViewMap mapView;
  private final ConfigData configData;
  public Integer forciblyMaxDistance = null;
  public Integer forciblySendSecondMaxBytes = null;
  public volatile boolean moveTooFast = false;
  public volatile boolean waitSend = false;
  public volatile long syncKey;
  public boolean permissionsNeed = true;
  private int lastDistance = 0;
  private long delayTime;
  private boolean isUnload = false;
  private World lastWorld;
  private Location oldLocation = null;
  private Long permissionsCheck = null;
  private Integer permissionsHit = null;

  public PlayerChunkView(
    Player player,
    ConfigData configData,
    ViewShape viewShape,
    BranchPacket branchPacket
  ) {
    this.player = player;
    this.configData = configData;
    this.branchPacket = branchPacket;
    this.mapView = configData.viewDistanceMode.createMap(viewShape);
    this.lastWorld = player.getWorld();
    this.syncKey = ChunkServer.random.nextLong();
    this.updateDistance();
    this.delay();
    this.mapView.setCenter(player.getLocation());
    this.viewAPI = new PlayerView(this);
    Bukkit.getPluginManager().callEvent(new PlayerInitViewEvent(this.viewAPI));
  }

  private int serverDistance() {
    return this.configData.serverViewDistance <= -1
      ? Bukkit.getViewDistance() + 1
      : this.configData.serverViewDistance;
  }

  public void updateDistance() {
    this.updateDistance(false);
  }

  /**
   * The function updates the distance for a map view and sends a packet to the player if the distance
   * has changed.
   *
   * @param forcibly The `forcibly` parameter is a boolean flag that indicates whether the distance
   * update should be forced, regardless of whether the distance has changed or not. If `forcibly` is
   * set to `true`, the distance update will always be performed. If `forcibly` is set to `false`,
   */
  private void updateDistance(boolean forcibly) {
    int newDistance = this.max();
    synchronized (this.mapView) {
      this.mapView.serverDistance = this.serverDistance();
      if (newDistance < this.mapView.serverDistance) {
        newDistance = this.mapView.serverDistance;
      }
    }

    if (forcibly || this.lastDistance != newDistance) {
      this.mapView.markOutsideWait(newDistance);
      int gapDistance = this.lastDistance - newDistance;
      this.lastDistance = newDistance;
      this.mapView.extendDistance = newDistance;
      if (gapDistance > 0) {
        this.mapView.completedDistance.addAndGet(-gapDistance);
      }

      PlayerSendViewDistanceEvent event = new PlayerSendViewDistanceEvent(
        this.viewAPI,
        newDistance
      );
      Bukkit.getPluginManager().callEvent(event);
      if (!event.isCancelled()) {
        this.branchPacket.sendViewDistance(this.player, event.getDistance());
      }
    }
  }

  private double square(double num) {
    return num * num;
  }

  public boolean overSpeed() {
    return this.overSpeed(this.player.getLocation());
  }

  /**
   * The function checks if the speed between the current location and the previous location is greater
   * than a specified threshold.
   *
   * @param location The "location" parameter represents the current location of an object or entity in
   * a specific world.
   * @return The method is returning a boolean value.
   */
  public boolean overSpeed(Location location) {
    ConfigData.World configWorld =
      this.configData.getWorld(this.lastWorld.getName());
    if (configWorld.speedingNotSend == -1.0D) {
      return false;
    } else {
      double speed = 0.0D;
      if (
        this.oldLocation != null &&
        this.oldLocation.getWorld() == location.getWorld()
      ) {
        speed =
          Math.sqrt(
            this.square(this.oldLocation.getX() - location.getX()) +
            this.square(this.oldLocation.getZ() - location.getZ())
          );
      }

      this.oldLocation = location;
      return speed > configWorld.speedingNotSend;
    }
  }

  public synchronized boolean move() {
    return this.move(this.player.getLocation());
  }

  public synchronized boolean move(Location location) {
    return this.move(location.getBlockX() >> 4, location.getBlockZ() >> 4);
  }

  /**
   * The function moves the player to a new chunk in the game world and unloads any previously loaded
   * chunks if necessary.
   *
   * @param chunkX The X coordinate of the chunk to move to.
   * @param chunkZ The parameter `chunkZ` represents the Z-coordinate of the chunk.
   * @return The method is returning a boolean value.
   */
  public synchronized boolean move(int chunkX, int chunkZ) {
    if (this.isUnload) {
      return false;
    } else if (this.player.getWorld() != this.lastWorld) {
      this.unload();
      return false;
    } else {
      for (long chunkKey : this.mapView.movePosition(chunkX, chunkZ)) {
        int hitX = ViewMap.getX(chunkKey);
        int hitZ = ViewMap.getZ(chunkKey);
        PlayerSendUnloadChunkEvent event = new PlayerSendUnloadChunkEvent(
          this.viewAPI,
          hitX,
          hitZ
        );
        Bukkit.getPluginManager().callEvent(event);
        if (!event.isCancelled()) {
          this.branchPacket.sendUnloadChunk(this.player, hitX, hitZ);
        }
      }

      return true;
    }
  }

  public void delay() {
    this.delay(
        System.currentTimeMillis() +
        (long) this.configData.getWorld(this.lastWorld.getName())
          .delayBeforeSend
      );
  }

  public void delay(long delayTime) {
    this.delayTime = delayTime;
  }

  /**
   * The function returns the next chunk key if it is within the world border and certain conditions
   * are met, otherwise it returns null.
   *
   * @return The method `next()` returns a `Long` value.
   */
  public Long next() {
    if (this.player.getWorld() != this.lastWorld) {
      this.unload();
      return null;
    } else if (this.isUnload) {
      return null;
    } else if (this.delayTime >= System.currentTimeMillis()) {
      return null;
    } else {
      Long chunkKey = this.mapView.get();
      if (chunkKey == null) {
        return null;
      } else {
        WorldBorder worldBorder = this.lastWorld.getWorldBorder();
        int chunkX = ViewMap.getX(chunkKey);
        int chunkZ = ViewMap.getZ(chunkKey);
        Location borderCenter = worldBorder.getCenter();
        int borderSizeRadius = (int) worldBorder.getSize() / 2;
        int borderMinX = (borderCenter.getBlockX() - borderSizeRadius >> 4) - 1;
        int borderMaxX = (borderCenter.getBlockX() + borderSizeRadius >> 4) + 1;
        int borderMinZ = (borderCenter.getBlockZ() - borderSizeRadius >> 4) - 1;
        int borderMaxZ = (borderCenter.getBlockZ() + borderSizeRadius >> 4) + 1;
        return (
            borderMinX <= chunkX &&
            chunkX <= borderMaxX &&
            borderMinZ <= chunkZ &&
            chunkZ <= borderMaxZ
          )
          ? chunkKey
          : null;
      }
    }
  }

  public void unload() {
    if (!this.isUnload) {
      this.delay();
      this.syncKey = ChunkServer.random.nextLong();
      this.isUnload = true;
      this.branchPacket.sendViewDistance(this.player, 0);
      this.branchPacket.sendViewDistance(
          this.player,
          this.mapView.extendDistance
        );
      this.mapView.clear();
    }
  }

  public boolean install() {
    if (this.isUnload) {
      this.delay();
      this.mapView.clear();
      this.updateDistance(true);
      this.lastWorld = this.player.getWorld();
      this.isUnload = false;
      return true;
    } else {
      return false;
    }
  }

  public void send(int x, int z) {
    PlayerViewMarkSendChunkEvent event = new PlayerViewMarkSendChunkEvent(
      this.viewAPI,
      x,
      z
    );
    Bukkit.getPluginManager().callEvent(event);
    if (!event.isCancelled()) {
      this.mapView.markSendPosition(x, z);
    }
  }

  public void remove(int x, int z) {
    PlayerViewMarkWaitChunkEvent event = new PlayerViewMarkWaitChunkEvent(
      this.viewAPI,
      x,
      z
    );
    Bukkit.getPluginManager().callEvent(event);
    if (!event.isCancelled()) {
      this.mapView.markWaitPosition(x, z);
    }
  }

  /**
   * The function calculates and returns the maximum view distance for a player based on various
   * conditions and configurations.
   *
   * @return The method is returning an integer value, which represents the maximum view distance.
   */
  public int max() {
    ConfigData.World configWorld =
      this.configData.getWorld(this.lastWorld.getName());
    int viewDistance = configWorld.maxViewDistance;
    int clientViewDistance = this.player.getClientViewDistance();
    Integer forciblyViewDistance = this.forciblyMaxDistance;
    PlayerCheckViewDistanceEvent event = new PlayerCheckViewDistanceEvent(
      this.viewAPI,
      this.serverDistance(),
      clientViewDistance,
      viewDistance
    );
    Bukkit.getPluginManager().callEvent(event);
    if (event.getForciblyDistance() != null) {
      viewDistance = event.getForciblyDistance();
    } else if (forciblyViewDistance != null) {
      viewDistance = forciblyViewDistance;
    } else if (
      this.permissionsNeed ||
      this.configData.permissionsPeriodicMillisecondCheck != -1L &&
      (
        this.permissionsCheck == null ||
        this.permissionsCheck <=
        System.currentTimeMillis() -
        this.configData.permissionsPeriodicMillisecondCheck
      )
    ) {
      this.permissionsNeed = false;
      this.permissionsCheck = System.currentTimeMillis();
      this.permissionsHit = null;

      for (Entry<String, Integer> permissionsNodeEntry : this.configData.permissionsNodeList) {
        int permissionViewDistance = permissionsNodeEntry.getValue();
        if (
          permissionViewDistance <= configWorld.maxViewDistance &&
          (
            this.permissionsHit == null ||
            permissionViewDistance > this.permissionsHit
          ) &&
          this.player.hasPermission((String) permissionsNodeEntry.getKey())
        ) {
          this.permissionsHit = permissionViewDistance;
        }
      }
    }

    if (this.permissionsHit != null) {
      viewDistance = this.permissionsHit;
    }

    if (viewDistance > clientViewDistance) {
      viewDistance = clientViewDistance;
    }

    if (viewDistance < 1) {
      viewDistance = 1;
    }

    return viewDistance;
  }

  public void clear() {
    this.mapView.clear();
  }

  public void recalculate() {
    this.mapView.markOutsideWait(this.mapView.serverDistance);
  }

  public ViewMap getMap() {
    return this.mapView;
  }

  public World getLastWorld() {
    return this.lastWorld;
  }

  public Player getPlayer() {
    return this.player;
  }

  public long getDelayTime() {
    return this.delayTime;
  }
}
