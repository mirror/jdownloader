package jd;

public class Benchmark {

    private static final long DURATION = 5000;

    /**
     * @param args
     */
    public static void main(String[] args) {
        Runnable r = new Runnable(){
      public void run(){
          while (true) {
              long start = System.currentTimeMillis();
              long c;
              long i = 0;
              while (System.currentTimeMillis() - start < DURATION) {
                  c = start / 945756;
                  i++;
                  if (i == Long.MAX_VALUE) {
                      i = 0;
                      System.out.println("overflow");
                  }
              }
              System.out.println(start+" : "+(i/DURATION));
          }
      }
  };
  
  new Thread(r).start();
  new Thread(r).start();
  new Thread(r).start();
//  new Thread(r).start();
    }
}
