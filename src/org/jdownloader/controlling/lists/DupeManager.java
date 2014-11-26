package org.jdownloader.controlling.lists;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import jd.controlling.downloadcontroller.DownloadController;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;

import org.appwork.scheduler.DelayedRunnable;
import org.appwork.utils.StringUtils;
import org.jdownloader.settings.staticreferences.CFG_GENERAL;

public class DupeManager {
    private volatile HashMap<String, List<DownloadLink>> map;
    private DelayedRunnable                              updateDelayer;

    public DupeManager() {
        map = new HashMap<String, List<DownloadLink>>();
        updateDelayer = new DelayedRunnable(DownloadController.TIMINGQUEUE, 500, 1000) {
            @Override
            public String getID() {
                return DupeManager.class.getName();
            }

            @Override
            public void delayedrun() {
                if (CFG_GENERAL.CFG.isDupeManagerEnabled()) {
                    refreshMap();
                } else {
                    map = null;
                }
            }

        };
    }

    public void invalidate() {
        updateDelayer.resetAndStart();

    }

    public boolean hasID(String linkID) {

        HashMap<String, List<DownloadLink>> lMap = map;
        if (lMap == null) {
            return false;
        }
        List<DownloadLink> lst = lMap.get(linkID);
        if (lst == null || lst.size() == 0) {
            return false;
        }

        List<DownloadLink> toRemove = null;
        try {
            for (DownloadLink link : lst) {

                if (StringUtils.equals(link.getLinkID(), linkID)) {
                    FilePackage p = link.getParentNode();
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
                if (lst.size() == 0) {
                    lMap.remove(linkID);
                }
            }
        }
        return false;
    }

    private HashMap<String, List<DownloadLink>> refreshMap() {
        HashMap<String, List<DownloadLink>> map = new HashMap<String, List<DownloadLink>>();

        for (FilePackage fpkg : DownloadController.getInstance().getPackagesCopy()) {
            boolean readL2 = fpkg.getModifyLock().readLock();
            try {

                for (DownloadLink link : fpkg.getChildren()) {
                    List<DownloadLink> lst = map.get(link.getLinkID());
                    if (lst == null) {
                        lst = new ArrayList<DownloadLink>();
                        map.put(link.getLinkID(), lst);
                    }
                    lst.add(link);
                }
            } finally {
                fpkg.getModifyLock().readUnlock(readL2);

            }

        }

        this.map = map;
        return map;
    }

}
