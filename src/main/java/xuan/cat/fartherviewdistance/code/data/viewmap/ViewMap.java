package xuan.cat.fartherviewdistance.code.data.viewmap;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import org.bukkit.Location;
import org.bukkit.command.CommandSender;
import xuan.cat.fartherviewdistance.code.data.viewmap.ViewShape;

@SuppressWarnings("unused")
public abstract class ViewMap {

    public final ViewShape viewShape;
    public final AtomicInteger completedDistance = new AtomicInteger(-1);
    public int extendDistance = 1;
    public int serverDistance = 1;
    protected int centerX = 0;
    protected int centerZ = 0;

    protected ViewMap(final ViewShape viewShape) { this.viewShape = viewShape; }

    public static int getX(final long positionKey) { return (int) positionKey; }

    public static int getZ(final long positionKey) { return (int) (positionKey >> 32); }

    public static long getPositionKey(final int x, final int z) { return (long) z << 32 & -4294967296L | (long) x & 4294967295L; }

    public abstract List<Long> movePosition(Location var1);

    public abstract List<Long> movePosition(int var1, int var2);

    public abstract Long get();

    public int getCenterX() { return this.centerX; }

    public void setCenterX(final int centerX) { this.centerX = centerX; }

    public int getCenterZ() { return this.centerZ; }

    public void setCenterZ(final int centerZ) { this.centerZ = centerZ; }

    public final void setCenter(final Location location) { this.setCenter(location.getBlockX() >> 4, location.getBlockZ() >> 4); }

    public final void setCenter(final int positionX, final int positionZ) {
        this.setCenterX(positionX);
        this.setCenterZ(positionZ);
    }

    public abstract boolean inPosition(int var1, int var2);

    public abstract boolean isWaitPosition(long var1);

    public abstract boolean isWaitPosition(int var1, int var2);

    public abstract boolean isSendPosition(long var1);

    public abstract boolean isSendPosition(int var1, int var2);

    public abstract void markWaitPosition(long var1);

    public abstract void markWaitPosition(int var1, int var2);

    public abstract void markSendPosition(long var1);

    public abstract void markSendPosition(int var1, int var2);

    public abstract void markOutsideWait(int var1);

    public abstract void markOutsideSend(int var1);

    public abstract void markInsideWait(int var1);

    public abstract void markInsideSend(int var1);

    public abstract List<Long> getAll();

    public abstract List<Long> getAllNotServer();

    public abstract boolean isWaitSafe(int var1, int var2);

    public abstract boolean isSendSafe(int var1, int var2);

    public abstract boolean markWaitSafe(int var1, int var2);

    public abstract void markSendSafe(int var1, int var2);

    public abstract void clear();

    public abstract void debug(CommandSender var1);
}
