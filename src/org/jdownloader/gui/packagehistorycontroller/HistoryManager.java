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

    private ArrayList<T> packageHistory;
    private boolean      changed = false;

    public HistoryManager(ArrayList<T> packageNameHistory, int max) {

        if (packageNameHistory == null) {
            packageNameHistory = new ArrayList<T>();
        }
        this.packageHistory = packageNameHistory;
        Collections.sort(packageHistory);
        for (Iterator<T> it = packageHistory.iterator(); it.hasNext();) {
            T next = it.next();
            if (next == null || StringUtils.isEmpty(next.getName())) {
                it.remove();
                continue;
            }
            if (packageHistory.size() > max && max > 0) {

                it.remove();

            }

        }

        ShutdownController.getInstance().addShutdownEvent(new ShutdownEvent() {

            @Override
            public void onShutdown(ShutdownRequest shutdownRequest) {
                if (changed) {
                    save(packageHistory);
                }
            }
        });

    }

    public List<T> list() {
        return packageHistory;
    }

    public void add(String packageName) {

        if (!StringUtils.isEmpty(packageName)) {
            changed = true;
            boolean found = false;
            for (T pe : packageHistory) {
                if (pe.getName().equalsIgnoreCase(packageName)) {
                    pe.setTime(System.currentTimeMillis());
                    found = true;
                    break;
                }
            }
            if (!found) {
                T newOne = createNew(packageName);
                newOne.setTime(System.currentTimeMillis());
                packageHistory.add(newOne);

            }

            Collections.sort(packageHistory);

        }
    }

    abstract protected T createNew(String name);

    abstract protected void save(ArrayList<T> list);

}
