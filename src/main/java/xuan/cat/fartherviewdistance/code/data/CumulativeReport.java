package xuan.cat.fartherviewdistance.code.data;

public final class CumulativeReport {

  private volatile int[] loadFast = new int[300];
  private volatile int[] loadSlow = new int[300];
  private volatile int[] consume = new int[300];

  public void next() {
    try {
      int[] loadFastClone = new int[300];
      int[] loadSlowClone = new int[300];
      int[] consumeClone = new int[300];
      System.arraycopy(
        this.loadFast,
        0,
        loadFastClone,
        1,
        this.loadFast.length - 1
      );
      System.arraycopy(
        this.loadSlow,
        0,
        loadSlowClone,
        1,
        this.loadSlow.length - 1
      );
      System.arraycopy(
        this.consume,
        0,
        consumeClone,
        1,
        this.consume.length - 1
      );
      this.loadFast = loadFastClone;
      this.loadSlow = loadSlowClone;
      this.consume = consumeClone;
    } catch (Exception var4) {
      var4.printStackTrace();
    }
  }

  public void increaseLoadFast() {
    int var10002 = this.loadFast[0]++;
  }

  public void increaseLoadSlow() {
    int var10002 = this.loadSlow[0]++;
  }

  public void addConsume(int value) {
    this.consume[0] += value;
  }

  public int reportLoadFast5s() {
    int total = 0;

    for (int i = 0; i < 5; ++i) {
      total += this.loadFast[i];
    }

    return total;
  }

  public int reportLoadFast1m() {
    int total = 0;

    for (int i = 0; i < 60; ++i) {
      total += this.loadFast[i];
    }

    return total;
  }

  public int reportLoadFast5m() {
    int total = 0;

    for (int i = 0; i < 300; ++i) {
      total += this.loadFast[i];
    }

    return total;
  }

  public int reportLoadSlow5s() {
    int total = 0;

    for (int i = 0; i < 5; ++i) {
      total += this.loadSlow[i];
    }

    return total;
  }

  public int reportLoadSlow1m() {
    int total = 0;

    for (int i = 0; i < 60; ++i) {
      total += this.loadSlow[i];
    }

    return total;
  }

  public int reportLoadSlow5m() {
    int total = 0;

    for (int i = 0; i < 300; ++i) {
      total += this.loadSlow[i];
    }

    return total;
  }

  public long reportConsume5s() {
    long total = 0L;

    for (int i = 0; i < 5; ++i) {
      total += (long) this.consume[i];
    }

    return total;
  }

  public long reportConsume1m() {
    long total = 0L;

    for (int i = 0; i < 60; ++i) {
      total += (long) this.consume[i];
    }

    return total;
  }

  public long reportConsume5m() {
    long total = 0L;

    for (int i = 0; i < 300; ++i) {
      total += (long) this.consume[i];
    }

    return total;
  }
}
