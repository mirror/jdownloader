package jd.controlling.downloadcontroller;

public class SingleDownloadControllerThreadGroup extends ThreadGroup {

    public SingleDownloadController getController() {
        Thread[] threads = new Thread[4];
        while (true) {
            final int found = enumerate(threads, false);
            if (found < threads.length) {
                /* see doc of enumerate */
                break;
            }
            threads = new Thread[found + (found % 2) + 4];
        }
        for (final Thread thread : threads) {
            if (thread != null && thread instanceof SingleDownloadController) {
                return (SingleDownloadController) thread;
            }
        }
        return null;
    }

    public SingleDownloadControllerThreadGroup(String name) {
        super(name);
    }

}
