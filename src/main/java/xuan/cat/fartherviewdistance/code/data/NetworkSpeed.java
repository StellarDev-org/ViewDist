package xuan.cat.fartherviewdistance.code.data;

import java.util.concurrent.atomic.AtomicInteger;

public final class NetworkSpeed {

  private final AtomicInteger writeTotal = new AtomicInteger(0);
  private final AtomicInteger consumeTotal = new AtomicInteger(0);
  public volatile long speedTimestamp = 0L;
  public volatile int speedConsume = 0;
  public volatile Long speedID = null;
  public volatile long pingTimestamp = 0L;
  public volatile Long pingID = null;
  public volatile int lastPing = 0;
  private volatile int[] writeArray = new int[50];
  private volatile int[] consumeArray = new int[50];

  public void add(int ping, int length) {
    synchronized (this.writeTotal) {
      this.writeTotal.addAndGet(length);
      this.consumeTotal.addAndGet(ping);
      this.writeArray[0] += length;
      this.consumeArray[0] += ping;
    }
  }

  public int avg() {
    synchronized (this.writeTotal) {
      int writeGet = this.writeTotal.get();
      int consumeGet = Math.max(1, this.consumeTotal.get());
      return writeGet == 0 ? 0 : writeGet / consumeGet;
    }
  }

  public void next() {
    synchronized (this.writeTotal) {
      this.writeTotal.addAndGet(-this.writeArray[this.writeArray.length - 1]);
      this.consumeTotal.addAndGet(
          -this.consumeArray[this.consumeArray.length - 1]
        );
      int[] writeArrayClone = new int[this.writeArray.length];
      int[] consumeArrayClone = new int[this.consumeArray.length];
      System.arraycopy(
        this.writeArray,
        0,
        writeArrayClone,
        1,
        this.writeArray.length - 1
      );
      System.arraycopy(
        this.consumeArray,
        0,
        consumeArrayClone,
        1,
        this.consumeArray.length - 1
      );
      this.writeArray = writeArrayClone;
      this.consumeArray = consumeArrayClone;
    }
  }
}
