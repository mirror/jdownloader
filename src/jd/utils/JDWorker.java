package jd.utils;

import java.util.LinkedList;

public class JDWorker extends Thread {

    private int paralellWorkerNum;
    private LinkedList<Runnable> queue;

    public JDWorker(int i) {
        this.paralellWorkerNum = i;
        this.queue = new LinkedList<Runnable>();
    }

    /**
     * @param args
     */
    public static void main(String[] args) {
        // TODO Auto-generated method stub
        JDWorker w = new JDWorker(2);
        w.add(new Runnable() {
            public void run() {
                for(int i=0; i<20;i++){
                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException e) {
                        // TODO Auto-generated catch block
                        e.printStackTrace();
                    }
                    System.out.println("i");
                }
             
            }

        });
        w.start();
        w.add(new Runnable() {
            public void run() {
                for(int i=0; i<20;i++){
                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException e) {
                        // TODO Auto-generated catch block
                        e.printStackTrace();
                    }
                    System.out.println("i");
                }
             
            }

        });
        w.add(new Runnable() {
            public void run() {
                for(int i=0; i<20;i++){
                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException e) {
                        // TODO Auto-generated catch block
                        e.printStackTrace();
                    }
                    System.out.println("i");
                }
             
            }

        });
    }

    public void run() {
        
        while(true){
        Runnable  runnable=getNextRunnable();
        
        }
        
    }

    private Runnable getNextRunnable() {
        synchronized (queue) {
            return queue.removeFirst();            
        }
    }

    private int add(Runnable runnable) {
        synchronized (queue) {
            queue.add(runnable);
            return queue.size();
        }

    }

}
