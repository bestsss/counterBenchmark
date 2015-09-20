package bestsss.counter;

import java.util.concurrent.atomic.AtomicLong;

public class Atomic implements Counter{
  private final AtomicLong value=new AtomicLong();
  
  @Override
  public void add(long delta) {
    value.addAndGet(delta);
  }

  @Override
  public long get() {
    return value.get();
  }
}
