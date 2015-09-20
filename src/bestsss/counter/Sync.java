package bestsss.counter;

public class Sync implements Counter{
  private long value;
  
  @Override
  public synchronized void add(long delta) {
    value+=delta;
  }

  @Override
  public synchronized long get() {
    return value;
  }
}
