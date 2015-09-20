package bestsss.counter;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLongArray;

public class Striped implements Counter{
  private static final int ADDEND = 997;
  private static final int CACHE_LINE=64;//almost all CPUs (save itanium have 64 bytes cache lines; 
  private static final int SHIFT  = Integer.numberOfTrailingZeros(CACHE_LINE / (Long.SIZE/8)) ;//long has 8 bytes, log2(8) = 3, i.e 1<<3 = 8

  final AtomicLongArray cells;
  final int shift;
  final int mask;
  private final AtomicInteger nextIndex;
  Striped(){
    nextIndex = new AtomicInteger();
    int CPUs = Runtime.getRuntime().availableProcessors();    
    if (CPUs>1){
      int length = Integer.highestOneBit(CPUs-1)<<1;//next power2
      length*=2;//0.5 load factor
      this.mask = length -1;
      this.shift = SHIFT;//shift to avoid 

      length +=2;//pad 1 cache line both ends
      length<<=shift;     
      cells = new AtomicLongArray(length);            
    } else{
      shift = 0;
      mask = 0;
      cells = new AtomicLongArray(2);     
    }

  }
  private int initialIndex(){//sort of ordered index
    return arrayIdx(nextIndex.getAndAdd(ADDEND));
  }
  
  private int arrayIdx(int index) {
    return (1 +(index & mask)) << shift ;
  }

  private final ThreadLocal<int[]> localIndex = new ThreadLocal<>();//it's possible not to use ThreadLocal but then the results are way too random
  @Override
  public void add(long delta){
    int[] idx = localIndex.get();
    
    if (idx==null){
      localIndex.set(idx=new int[]{initialIndex()});
    } 
    if (idx.length>0){//guaranteed to be true, but help JIT, not inserting IOOB traps   
      int i = idx[0];
      long value = cells.get(i);
      if (cells.compareAndSet(i, value, value+delta)){//cas success    
        return;
      }
      idx[0]=i = arrayIdx(i+ADDEND);//try another index (997 is a prime); since having 2x CPUs it's likely to stablize at some point
      cells.addAndGet(i, delta);
    }       
  }

  @Override
  public long get() {
    long sum = 0;
    for (int i=0, step = 1<<shift; i<cells.length(); i+=step){
      sum+=cells.get(i);
    }
    return sum;
  }
}
