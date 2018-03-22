package org.jdownloader.gui.packagehistorycontroller;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

import jd.controlling.TaskQueue;

import org.appwork.shutdown.ShutdownController;
import org.appwork.shutdown.ShutdownEvent;
import org.appwork.shutdown.ShutdownRequest;
import org.appwork.utils.StringUtils;
import org.appwork.utils.event.queue.QueueAction;

public abstract class HistoryManager<T extends HistoryEntry> {
    protected final ArrayList<T> packageHistory;

    public HistoryManager(final List<T> packageNameHistory) {
        try {
            ShutdownController.getInstance().addShutdownEvent(new ShutdownEvent() {
                @Override
                public void onShutdown(ShutdownRequest shutdownRequest) {
                    save(list());
                }
            });
        } catch (Throwable e) {
            /* prevent ClassNotFound exception on shutdown */
        }
        if (packageNameHistory == null) {
            packageHistory = new ArrayList<T>();
        } else {
            packageHistory = new ArrayList<T>(packageNameHistory);
        }
        Collections.sort(packageHistory);
        int packageHistoryIndex = 0;
        final int max = Math.max(0, getMaxLength());
        for (Iterator<T> it = packageHistory.iterator(); it.hasNext();) {
            final T next = it.next();
            if (next == null || StringUtils.isEmpty(next.getName()) || !isValid(next.getName())) {
                it.remove();
                continue;
            }
            if (packageHistoryIndex++ >= max) {
                it.remove();
            }
        }
    }

    protected abstract int getMaxLength();

    public synchronized List<T> list() {
        return new ArrayList<T>(packageHistory);
    }

    public synchronized void clear() {
        packageHistory.clear();
    }

    protected synchronized boolean contains(String packageName) {
        if (!StringUtils.isEmpty(packageName)) {
            for (final T existing : packageHistory) {
                if (existing.getName().equalsIgnoreCase(packageName)) {
                    return true;
                }
            }
        }
        return false;
    }

    protected synchronized T get(String packageName) {
        if (!StringUtils.isEmpty(packageName)) {
            for (final T existing : packageHistory) {
                if (existing.getName().equalsIgnoreCase(packageName)) {
                    return existing;
                }
            }
        }
        return null;
    }

    private final AtomicLong saveRequest = new AtomicLong(-1);

    protected boolean isValid(String input) {
        return true;
    }

    public synchronized void add(String packageName) {
        if (!StringUtils.isEmpty(packageName) && isValid(packageName)) {
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
            final int max = Math.max(0, getMaxLength());
            while (packageHistory.size() > max) {
                packageHistory.remove(packageHistory.size() - 1);
            }
            final long saveRequest = this.saveRequest.incrementAndGet();
            TaskQueue.getQueue().addAsynch(new QueueAction<Void, RuntimeException>() {
                @Override
                protected Void run() throws RuntimeException {
                    if (saveRequest == HistoryManager.this.saveRequest.get()) {
                        save(list());
                    }
                    return null;
                }
            });
        }
    }

    abstract protected T createNew(String name);

    abstract protected void save(List<T> list);
}
