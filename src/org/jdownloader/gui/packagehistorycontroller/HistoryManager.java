package org.jdownloader.gui.packagehistorycontroller;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import org.appwork.shutdown.ShutdownController;
import org.appwork.shutdown.ShutdownEvent;
import org.appwork.shutdown.ShutdownRequest;
import org.appwork.utils.StringUtils;

public abstract class HistoryManager<T extends HistoryEntry> {

    private final ArrayList<T> packageHistory;

    public HistoryManager(final List<T> packageNameHistory, final int max) {
        try {
            ShutdownController.getInstance().addShutdownEvent(new ShutdownEvent() {

                @Override
                public void onShutdown(ShutdownRequest shutdownRequest) {
                    save(list());
                }
            });
        } catch (IllegalStateException e) {
            /* prevent ClassNotFound exception on shutdown */
        }
        if (packageNameHistory == null) {
            packageHistory = new ArrayList<T>();
        } else {
            packageHistory = new ArrayList<T>(packageNameHistory);
        }
        Collections.sort(packageHistory);
        int packageHistoryIndex = 0;
        for (Iterator<T> it = packageHistory.iterator(); it.hasNext();) {
            final T next = it.next();
            if (next == null || StringUtils.isEmpty(next.getName())) {
                it.remove();
                continue;
            }
            if (packageHistoryIndex++ >= max) {
                it.remove();
            }
        }

    }

    public synchronized List<T> list() {
        return new ArrayList<T>(packageHistory);
    }

    public synchronized void clear() {
        packageHistory.clear();
    }

    public synchronized void add(String packageName) {
        if (!StringUtils.isEmpty(packageName)) {
            boolean found = false;
            for (final T existing : packageHistory) {
                if (existing.getName().equalsIgnoreCase(packageName)) {
                    existing.setTime(System.currentTimeMillis());
                    found = true;
                    break;
                }
            }
            if (!found) {
                final T newOne = createNew(packageName);
                newOne.setTime(System.currentTimeMillis());
                packageHistory.add(0, newOne);
            } else {
                Collections.sort(packageHistory);
            }
        }
    }

    abstract protected T createNew(String name);

    abstract protected void save(List<T> list);

}
