package jd.controlling.linkcrawler;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.concurrent.atomic.AtomicInteger;

public abstract class LinkCrawlerRunnable implements Runnable {

    private final LinkCrawler                              crawler;
    private final int                                      generation;
    static HashMap<Object, ArrayList<LinkCrawlerRunnable>> SEQ_RUNNABLES = new HashMap<Object, ArrayList<LinkCrawlerRunnable>>();
    static HashMap<Object, AtomicInteger>                  SEQ_COUNTER   = new HashMap<Object, AtomicInteger>();

    protected LinkCrawlerRunnable(LinkCrawler crawler, int generation) {
        if (crawler == null) throw new IllegalArgumentException("crawler==null?");
        this.crawler = crawler;
        this.generation = generation;
    }

    public LinkCrawler getLinkCrawler() {
        return crawler;
    }

    public void run() {
        if (sequentialLockingObject() == null || maxConcurrency() == Integer.MAX_VALUE) {
            run_now();
        } else {
            run_delayed();
        }
    }

    protected void run_delayed() {
        Object lock = sequentialLockingObject();
        LinkCrawlerRunnable startRunnable = null;
        synchronized (SEQ_RUNNABLES) {
            ArrayList<LinkCrawlerRunnable> seqs = SEQ_RUNNABLES.get(lock);
            if (seqs == null) {
                /* no queued sequential runnable */
                seqs = new ArrayList<LinkCrawlerRunnable>();
                SEQ_RUNNABLES.put(lock, seqs);
            }
            AtomicInteger counter = SEQ_COUNTER.get(lock);
            if (counter == null) {
                counter = new AtomicInteger(0);
                SEQ_COUNTER.put(lock, counter);
            }
            if (counter.get() < maxConcurrency()) {
                /* we have still some slots available for concurrent running */
                startRunnable = this;
                counter.incrementAndGet();
            }
            seqs.add(this);
        }
        if (startRunnable == null) return;
        try {
            this.run_now();
        } finally {
            synchronized (SEQ_RUNNABLES) {
                ArrayList<LinkCrawlerRunnable> seqs = SEQ_RUNNABLES.get(lock);
                AtomicInteger counter = SEQ_COUNTER.get(lock);
                if (seqs != null) {
                    /* remove current Runnable */
                    counter.decrementAndGet();
                    seqs.remove(this);
                    if (seqs.size() == 0) {
                        /* remove sequential runnable queue */
                        SEQ_RUNNABLES.remove(lock);
                        SEQ_COUNTER.remove(lock);
                    } else {
                        /* process next waiting runnable */
                        LinkCrawlerRunnable next = seqs.remove(0);
                        LinkCrawler.threadPool.execute(next);
                    }
                }
            }
        }
    }

    /**
     * run this Runnable now
     */
    protected void run_now() {
        try {
            if (crawler.getCrawlerGeneration(false) == this.generation) {
                crawling();
            }
        } finally {
            crawler.checkFinishNotify();
        }
    }

    abstract void crawling();

    public long getAverageRuntime() {
        return 0;
    }

    protected Object sequentialLockingObject() {
        return null;
    }

    protected int maxConcurrency() {
        return Integer.MAX_VALUE;
    }
}
