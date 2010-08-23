package jd.plugins;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

import jd.gui.swing.components.table.JDTable;

public class FilePackageInfoCache extends LinkedHashMap<FilePackage, FilePackageInfo> {

    private final static FilePackageInfoCache INSTANCE = new FilePackageInfoCache();

    private static int MAX_ENTRIES = 50;
    private static AtomicLong lastReset = new AtomicLong(0l);
    /**
     * 
     */
    private static final long serialVersionUID = 894074059083531243L;
    private static Thread thread = null;

    private FilePackageInfoCache() {
        if (thread == null) {
            thread = new Thread(new Runnable() {

                public void run() {
                    while (true) {
                        try {
                            Thread.sleep(1000);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                        lastReset.incrementAndGet();
                    }
                }
            });
            thread.setDaemon(true);
            thread.setName("FilePackageInfoCache");
            thread.start();
        }
    }

    public static synchronized final FilePackageInfoCache getInstance() {
        return INSTANCE;
    }

    @Override
    protected boolean removeEldestEntry(Map.Entry<FilePackage, FilePackageInfo> eldest) {
        return size() > MAX_ENTRIES;
    }

    @Override
    public FilePackageInfo get(Object key) {
        FilePackageInfo info = super.get(key);
        if (info == null) {
            info = new FilePackageInfo((FilePackage) key);
            put((FilePackage) key, info);
        }
        info.reset(lastReset.get());
        return info;
    }

    public static void reset() {
        lastReset.incrementAndGet();
    }

    public static void setMaxItems(JDTable table) {
        if (table == null) {
            MAX_ENTRIES = 50;
        } else {
            MAX_ENTRIES = Math.max(20, (int) (table.getVisibleRect().getHeight() / 16.0) + 2);
        }
    }
}
