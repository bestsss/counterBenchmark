package bestsss.counter;

import java.math.BigDecimal;
import java.math.MathContext;
import java.util.*;
import java.util.concurrent.*;
import static  java.lang.System.nanoTime;

public class TestHarness {
  
  static class AddRunner implements Runnable{
    final Counter counter;
    final CountDownLatch latch;
    final int loops;
    final long delta;
    
    public AddRunner(CountDownLatch latch, Counter counter, int loops, long delta) {
      super();
      this.latch = latch;
      this.counter = counter;
      this.loops = loops;
      this.delta = delta;       
    }

    @Override
    public void run() {
      await();
      
      final long delta = this.delta;
      for (int i=0, max = loops; i<max; i++){ 
        counter.add(delta);
      }
    }

    private void await() {
      latch.countDown();
      try{
        latch.await();
      }catch (InterruptedException _ex) {
        Thread.currentThread().interrupt();
        throw new IllegalStateException(_ex);
      }
    }    
  }
  
  public static void main(String[] args) throws Throwable {
    final int threadCount = Integer.getInteger("threads", Runtime.getRuntime().availableProcessors());    
    final int loops = Integer.getInteger("loops", (int) 2e9);
    final Class<? extends Counter> type = resolveClass(System.getProperty("counter", "Striped"));
    warmup(type.newInstance());
    System.out.printf("Name\tnanos/ops\tTime\tResult%n");
    
    for (int i=0; i<10; i++){
      testOnce(type, threadCount, loops);
      System.gc();
    }
  }

  private static void testOnce(final Class<? extends Counter> type, final int threadCount, final int loops) throws InstantiationException, IllegalAccessException, InterruptedException {
    final CountDownLatch latch = new CountDownLatch(threadCount);
    final Counter counter = type.newInstance();
    
    Collection<Callable<Object>> tasks = new ArrayList<>();
    for (int i=0; i<threadCount; i++){      
      tasks.add(Executors.callable(new AddRunner(latch, counter, loops/threadCount, 1)));
    }
    ExecutorService svc= Executors.newFixedThreadPool(threadCount);
    if (svc instanceof ThreadPoolExecutor)
      ((ThreadPoolExecutor) svc).prestartAllCoreThreads();
    
    long time= -nanoTime();
    svc.invokeAll(tasks);
    time += nanoTime();
    svc.shutdown();
    System.out.printf("'%s'\t%.2f\t%.2fs\t%d%n", 
        counter.getClass().getSimpleName(),
        BigDecimal.valueOf(time).divide(BigDecimal.valueOf(counter.get()), new MathContext(8)),
        BigDecimal.valueOf(time, 9), counter.get() );    
  }

  private static void warmup(Counter counter) {
    final int loops = (int)1.5e5;
    long time=-nanoTime();
    new AddRunner(new CountDownLatch(1), counter, loops, 1).run();
    time += nanoTime();
    long result = counter.get();
    if (result!=loops){
      throw new AssertionError(String.format("%s fails, expected: %d, actual: %d", counter.getClass().getName(), loops, result));
    }
    System.out.printf("Warmup done: %d us%n", TimeUnit.NANOSECONDS.toMicros(time) );
  }


  private static Class<? extends Counter> resolveClass(String className) {
    Class<?> clazz;
    try{
      clazz = Class.forName(className);
    }catch (ClassNotFoundException _ex) {
        String pack = TestHarness.class.getName();
        pack = pack.substring(0, pack.lastIndexOf('.')+1);
        try{
          clazz = (Class<?>)  Class.forName(pack+className);
        }catch (ClassNotFoundException cnf2) {
          throw new IllegalArgumentException("className", _ex);
        }      
    }
    return clazz.asSubclass(Counter.class);
  }
  
}
