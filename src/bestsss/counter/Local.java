package bestsss.counter;

import java.lang.ref.WeakReference;
import java.util.concurrent.CopyOnWriteArrayList;

public class Local implements Counter{
  private final CopyOnWriteArrayList<Cell> cells = new CopyOnWriteArrayList<>();

  private final ThreadLocal<Cell> current = new ThreadLocal<Cell>(){
    @Override
    protected Cell initialValue() {
      Cell result = new Cell();
      cells.add(result);
      return result;
    }       
  };
  
  @Override
  public void add(long delta) {
    current.get().value+=delta;
  }

  @Override
  public long get() {
    long result = 0;
    for (Cell cell : cells){
      result+=cell.value;
      if (cell.get()==null){//clear up dead stuff
        cells.remove(cell);//CoWArrayList, tolerate removes during iteration
      }
    }
    return result;
  }
  
  private static class Cell extends WeakReference<Thread>{
    public Cell() {
      super(Thread.currentThread());      
    }
    long value;//risk of false sharing but it's a nice looking    
  }
}
