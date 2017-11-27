package org.jdownloader.controlling.lists;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import jd.controlling.downloadcontroller.DownloadController;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;

import org.appwork.scheduler.DelayedRunnable;
import org.appwork.utils.StringUtils;
import org.jdownloader.settings.staticreferences.CFG_GENERAL;

public class DupeManager {
    private volatile Map<String, Object> map = null;
    private final DelayedRunnable        updateDelayer;
    private final boolean                enabled;

    public DupeManager() {
        map = new HashMap<String, Object>();
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
            final Map<String, Object> lMap = map;
            if (lMap == null) {
                return false;
            }
            final Object item = lMap.get(linkID);
            if (item instanceof DownloadLink) {
                final DownloadLink link = (DownloadLink) item;
                if (StringUtils.equals(link.getLinkID(), linkID)) {
                    final FilePackage p = link.getParentNode();
                    if (p != null && p.getControlledBy() != null) {
                        return true;
                    } else {
                        invalidate();
                    }
                }
                return false;
            }
            final List<DownloadLink> lst = (List<DownloadLink>) item;
            boolean invalidate = false;
            for (final DownloadLink link : lst) {
                if (StringUtils.equals(link.getLinkID(), linkID)) {
                    final FilePackage p = link.getParentNode();
                    if (p != null && p.getControlledBy() != null) {
                        return true;
                    } else {
                        invalidate = true;
                    }
                }
            }
            if (invalidate) {
                invalidate();
            }
        }
        return false;
    }

    private Map<String, Object> refreshMap() {
        if (enabled) {
            final HashMap<String, ArrayList<DownloadLink>> map = new HashMap<String, ArrayList<DownloadLink>>();
            for (final FilePackage fpkg : DownloadController.getInstance().getPackagesCopy()) {
                final boolean readL2 = fpkg.getModifyLock().readLock();
                try {
                    for (final DownloadLink link : fpkg.getChildren()) {
                        final String linkID = link.getLinkID();
                        ArrayList<DownloadLink> lst = map.get(linkID);
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
            final Map<String, Object> cowMap = new HashMap<String, Object>();
            for (final Entry<String, ArrayList<DownloadLink>> entry : map.entrySet()) {
                if (entry.getValue().size() == 1) {
                    cowMap.put(entry.getKey(), entry.getValue().get(0));
                } else {
                    entry.getValue().trimToSize();
                    cowMap.put(entry.getKey(), entry.getValue());
                }
            }
            this.map = cowMap;
            return cowMap;
        } else {
            this.map = null;
            return null;
        }
    }
}
