package bestsss.counter;

import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class LockCounter implements Counter{
  private final Lock lock = new ReentrantLock(false);
  private long value;
  @Override
  public void add(long delta) {
    lock.lock();    
    value+=delta;//saved try/finally for, since the only exception during add is java.lang.VirtualMachineError, which is unrecovarable.  
    lock.unlock();
  }

  @Override
  public long get() {
    lock.lock();
    try{
      return value;
    }finally{
      lock.unlock();
    }
  }

}
