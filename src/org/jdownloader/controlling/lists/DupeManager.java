package org.jdownloader.controlling.lists;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.CopyOnWriteArrayList;

import jd.controlling.downloadcontroller.DownloadController;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;

import org.appwork.scheduler.DelayedRunnable;
import org.appwork.utils.StringUtils;
import org.jdownloader.settings.staticreferences.CFG_GENERAL;

public class DupeManager {
    private volatile HashMap<String, List<DownloadLink>> map = null;
    private final DelayedRunnable                        updateDelayer;
    private final boolean                                enabled;

    public DupeManager() {
        map = new HashMap<String, List<DownloadLink>>();
        enabled = CFG_GENERAL.CFG.isDupeManagerEnabled();
        if (enabled) {
            updateDelayer = new DelayedRunnable(DownloadController.TIMINGQUEUE, 500, 1000) {
                @Override
                public String getID() {
                    return DupeManager.class.getName();
                }

                @Override
                public void delayedrun() {
                    refreshMap();
                }
            };
        } else {
            updateDelayer = null;
        }
    }

    public void invalidate() {
        if (enabled) {
            updateDelayer.resetAndStart();
        }
    }

    public boolean hasID(String linkID) {
        if (enabled) {
            final HashMap<String, List<DownloadLink>> lMap = map;
            if (lMap == null) {
                return false;
            }
            final List<DownloadLink> lst = lMap.get(linkID);
            if (lst == null || lst.size() == 0) {
                return false;
            }
            List<DownloadLink> toRemove = null;
            try {
                for (final DownloadLink link : lst) {
                    if (StringUtils.equals(link.getLinkID(), linkID)) {
                        final FilePackage p = link.getParentNode();
                        if (p != null && p.getControlledBy() != null) {
                            return true;
                        }
                    }
                    if (toRemove == null) {
                        toRemove = new ArrayList<DownloadLink>();
                    }
                    toRemove.add(link);
                }
            } finally {
                if (toRemove != null) {
                    lst.removeAll(toRemove);
                }
            }
        }
        return false;
    }

    private Map<String, List<DownloadLink>> refreshMap() {
        if (enabled) {
            final HashMap<String, List<DownloadLink>> map = new HashMap<String, List<DownloadLink>>();
            for (final FilePackage fpkg : DownloadController.getInstance().getPackagesCopy()) {
                final boolean readL2 = fpkg.getModifyLock().readLock();
                try {
                    for (final DownloadLink link : fpkg.getChildren()) {
                        final String linkID = link.getLinkID();
                        List<DownloadLink> lst = map.get(linkID);
                        if (lst == null) {
                            lst = new ArrayList<DownloadLink>();
                            map.put(linkID, lst);
                        }
                        lst.add(link);
                    }
                } finally {
                    fpkg.getModifyLock().readUnlock(readL2);
                }
            }
            final HashMap<String, List<DownloadLink>> cowMap = new HashMap<String, List<DownloadLink>>();
            for (Entry<String, List<DownloadLink>> entry : map.entrySet()) {
                cowMap.put(entry.getKey(), new CopyOnWriteArrayList<DownloadLink>(entry.getValue()));
            }
            this.map = cowMap;
            return cowMap;
        } else {
            this.map = null;
            return null;
        }
    }
}
