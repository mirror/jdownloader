package org.jdownloader.api.downloads;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import jd.controlling.downloadcontroller.DownloadController;
import jd.controlling.downloadcontroller.DownloadWatchDog;
import jd.controlling.packagecontroller.AbstractPackageChildrenNodeFilter;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.plugins.FilePackageView;
import jd.plugins.LinkStatus;

import org.appwork.remoteapi.APIQuery;
import org.appwork.remoteapi.QueryResponseMap;

public class DownloadsAPIImpl implements DownloadsAPI {

    public boolean start() {
        DownloadWatchDog.getInstance().startDownloads();
        return true;
    }

    public boolean stop() {
        DownloadWatchDog.getInstance().stopDownloads();
        return true;
    }

    public boolean pause(Boolean value) {
        DownloadWatchDog.getInstance().pauseDownloadWatchDog(value);
        return true;
    }

    @Override
    public List<FilePackageAPIStorable> queryPackages(APIQuery queryParams) {
        DownloadController dlc = DownloadController.getInstance();
        DownloadWatchDog dwd = DownloadWatchDog.getInstance();
        ArrayList<FilePackage> packages = dlc.getPackagesCopy();
        List<FilePackageAPIStorable> ret = new ArrayList<FilePackageAPIStorable>(dlc.size());
        int startWith = queryParams.getStartAt();
        int maxResults = queryParams.getMaxResults();
        if (startWith > dlc.size() - 1) return ret;
        if (startWith < 0) startWith = 0;
        if (maxResults < 0) maxResults = dlc.size();

        for (int i = startWith; i < Math.min(startWith + maxResults, dlc.size()); i++) {
            FilePackage fp = packages.get(i);
            boolean readL = fp.getModifyLock().readLock();
            try {
                FilePackageView fpView = new FilePackageView(fp);
                fpView.update();
                FilePackageAPIStorable fps = new FilePackageAPIStorable(fp);

                QueryResponseMap infomap = new QueryResponseMap();

                if (queryParams._getQueryParam("saveTo", Boolean.class, false)) {
                    infomap.put("saveTo", fp.getDownloadDirectory());
                }
                if (queryParams._getQueryParam("size", Boolean.class, false)) {
                    long size = 0;
                    for (DownloadLink dl : fp.getChildren()) {
                        size = size + dl.getDownloadSize();
                    }
                    infomap.put("size", size);
                }
                if (queryParams._getQueryParam("childCount", Boolean.class, false)) {
                    infomap.put("childCount", fp.getChildren().size());
                }
                if (queryParams._getQueryParam("hosts", Boolean.class, false)) {
                    Set<String> hosts = new HashSet<String>();
                    for (DownloadLink dl : fp.getChildren()) {
                        hosts.add(dl.getHost());
                    }
                    infomap.put("hosts", hosts);
                }
                if (queryParams._getQueryParam("activeTask", Boolean.class, false)) {
                    infomap.put("activeTask", "N/A");
                }
                if (queryParams._getQueryParam("speed", Boolean.class, false)) {
                    infomap.put("speed", dwd.getDownloadSpeedbyFilePackage(fp));
                }
                if (queryParams._getQueryParam("finished", Boolean.class, false)) {
                    infomap.put("finished", fpView.isFinished());
                }
                if (queryParams._getQueryParam("eta", Boolean.class, false)) {
                    infomap.put("eta", fpView.getETA());
                }
                if (queryParams._getQueryParam("done", Boolean.class, false)) {
                    Long done = 0l;
                    for (DownloadLink dl : fp.getChildren()) {
                        done = done + dl.getDownloadCurrent();
                    }
                    infomap.put("done", done);
                }
                if (queryParams._getQueryParam("progress", Boolean.class, false)) {
                    infomap.put("progress", -1);
                }
                if (queryParams._getQueryParam("comment", Boolean.class, false)) {
                    infomap.put("comment", fp.getComment());
                }
                if (queryParams.fieldRequested("enabled")) {
                    boolean enabled = false;
                    for (DownloadLink dl : fp.getChildren()) {
                        if (dl.isEnabled()) {
                            enabled = true;
                            break;
                        }
                    }
                    infomap.put("enabled", enabled);
                }
                if (queryParams._getQueryParam("running", Boolean.class, false)) {
                    infomap.put("running", dwd.getRunningFilePackages().contains(fp));
                }

                fps.setInfoMap(infomap);
                ret.add(fps);
            } finally {
                fp.getModifyLock().readUnlock(readL);
            }
        }
        return ret;
    }

    @SuppressWarnings("rawtypes")
    @Override
    public List<DownloadLinkAPIStorable> queryLinks(APIQuery queryParams) {
        List<DownloadLinkAPIStorable> result = new ArrayList<DownloadLinkAPIStorable>();

        DownloadController dlc = DownloadController.getInstance();
        DownloadWatchDog dwd = DownloadWatchDog.getInstance();

        // retrieve packageUUIDs from queryParams
        List<Long> packageUUIDs = new ArrayList<Long>();
        if (!queryParams._getQueryParam("packageUUIDs", List.class, new ArrayList()).isEmpty()) {
            List uuidsFromQuery = queryParams._getQueryParam("packageUUIDs", List.class, new ArrayList());
            for (Object o : uuidsFromQuery) {
                try {
                    packageUUIDs.add((Long) o);
                } catch (ClassCastException e) {
                    continue;
                }
            }
        }

        List<FilePackage> matched = new ArrayList<FilePackage>();

        // if no specific uuids are specified collect all packages
        if (packageUUIDs.isEmpty()) {
            matched = dlc.getPackagesCopy();
        } else {
            boolean b = dlc.readLock();
            try {
                for (FilePackage pkg : dlc.getPackages()) {
                    if (packageUUIDs.contains(pkg.getUniqueID().getID())) {
                        matched.add(pkg);
                    }
                }
            } finally {
                dlc.readUnlock(b);
            }
        }

        // collect children of the selected packages and convert to storables for response
        List<DownloadLink> links = new ArrayList<DownloadLink>();
        for (FilePackage pkg : matched) {
            boolean b = pkg.getModifyLock().readLock();
            try {
                links.addAll(pkg.getChildren());
            } finally {
                pkg.getModifyLock().readUnlock(b);
            }
        }

        if (links.isEmpty()) return result;

        int startWith = queryParams.getStartAt();
        int maxResults = queryParams.getMaxResults();

        if (startWith > links.size() - 1) return result;
        if (startWith < 0) startWith = 0;
        if (maxResults < 0) maxResults = links.size();

        for (int i = startWith; i < Math.min(startWith + maxResults, links.size()); i++) {

            DownloadLink dl = links.get(i);
            DownloadLinkAPIStorable dls = new DownloadLinkAPIStorable(dl);

            QueryResponseMap infomap = new QueryResponseMap();

            if (queryParams._getQueryParam("host", Boolean.class, false)) {
                infomap.put("host", dl.getHost());
            }
            if (queryParams._getQueryParam("size", Boolean.class, false)) {
                infomap.put("size", dl.getDownloadSize());
            }
            if (queryParams._getQueryParam("done", Boolean.class, false)) {
                infomap.put("done", dl.getDownloadCurrent());
            }
            if (queryParams._getQueryParam("speed", Boolean.class, false)) {
                infomap.put("speed", dl.getDownloadSpeed());
            }
            if (queryParams._getQueryParam("eta", Boolean.class, false)) {
                infomap.put("eta", -1);
            }
            if (queryParams._getQueryParam("finished", Boolean.class, false)) {
                infomap.put("finished", (dl.getLinkStatus() != null && dl.getLinkStatus().getLatestStatus() == LinkStatus.FINISHED));
            }
            if (queryParams._getQueryParam("linkStatus", Boolean.class, false)) {
                infomap.put("linkStatus", new LinkStatusAPIStorable(dl.getLinkStatus()));
            }
            if (queryParams._getQueryParam("running", Boolean.class, false)) {
                infomap.put("running", dwd.getRunningDownloadLinks().contains(dl));
            }
            if (queryParams.fieldRequested("enabled")) infomap.put("enabled", dl.isEnabled());

            infomap.put("packageUUID", dl.getParentNode().getUniqueID().getID());

            dls.setInfoMap(infomap);
            result.add(dls);
        }

        return result;
    }

    @Override
    public int speed() {
        DownloadWatchDog dwd = DownloadWatchDog.getInstance();
        return dwd.getDownloadSpeedManager().getSpeed();
    }

    @Override
    public boolean removeLinks(final List<Long> linkIds) {
        if (linkIds == null) return true;

        DownloadController dlc = DownloadController.getInstance();

        List<DownloadLink> rmv = dlc.getChildrenByFilter(new AbstractPackageChildrenNodeFilter<DownloadLink>() {
            @Override
            public int returnMaxResults() {
                return 0;
            }

            @Override
            public boolean acceptNode(DownloadLink node) {
                if (linkIds.contains(node.getUniqueID().getID())) return true;
                return false;
            }
        });

        dlc.writeLock();
        dlc.removeChildren(rmv);
        dlc.writeUnlock();

        return true;
    }

    @Override
    public boolean forceDownload(final List<Long> linkIds) {
        if (linkIds == null) return true;

        DownloadController dlc = DownloadController.getInstance();

        List<DownloadLink> sdl = dlc.getChildrenByFilter(new AbstractPackageChildrenNodeFilter<DownloadLink>() {
            @Override
            public int returnMaxResults() {
                return 0;
            }

            @Override
            public boolean acceptNode(DownloadLink node) {
                if (linkIds.contains(node.getUniqueID().getID())) return true;
                return false;
            }
        });

        DownloadWatchDog dwd = DownloadWatchDog.getInstance();
        dwd.forceDownload(sdl);

        return true;
    }

    @Override
    public boolean enableLinks(final List<Long> linkIds) {
        if (linkIds == null) return true;

        DownloadController dlc = DownloadController.getInstance();

        List<DownloadLink> sdl = dlc.getChildrenByFilter(new AbstractPackageChildrenNodeFilter<DownloadLink>() {
            @Override
            public int returnMaxResults() {
                return 0;
            }

            @Override
            public boolean acceptNode(DownloadLink node) {
                if (linkIds.contains(node.getUniqueID().getID())) return true;
                return false;
            }
        });

        for (DownloadLink dl : sdl) {
            dl.setEnabled(true);
        }

        return true;
    }

    @Override
    public boolean disableLinks(final List<Long> linkIds) {
        if (linkIds == null) return true;

        DownloadController dlc = DownloadController.getInstance();

        List<DownloadLink> sdl = dlc.getChildrenByFilter(new AbstractPackageChildrenNodeFilter<DownloadLink>() {
            @Override
            public int returnMaxResults() {
                return 0;
            }

            @Override
            public boolean acceptNode(DownloadLink node) {
                if (linkIds.contains(node.getUniqueID().getID())) return true;
                return false;
            }
        });

        for (DownloadLink dl : sdl) {
            dl.setEnabled(false);
        }

        return true;
    }

    @Override
    @SuppressWarnings("unchecked")
    public boolean movePackages(APIQuery query) {
        List<Long> packageUUIDs = query._getQueryParam("packageUUIDs", List.class, new ArrayList<Long>());
        Long afterDestPackageUUID = query._getQueryParam("afterDestPackageUUID", Long.class, null);

        DownloadController dlc = DownloadController.getInstance();

        List<FilePackage> selectedPackages = new ArrayList<FilePackage>();
        FilePackage afterDestPackage = null;

        boolean b = dlc.readLock();
        try {
            for (FilePackage fp : dlc.getPackages()) {
                if (packageUUIDs.contains(fp.getUniqueID().getID())) {
                    selectedPackages.add(fp);
                }
                if (afterDestPackageUUID != null && afterDestPackageUUID.equals(fp.getUniqueID().getID())) {
                    afterDestPackage = fp;
                }
            }
        } finally {
            dlc.readUnlock(b);
        }

        dlc.move(selectedPackages, afterDestPackage);
        return true;
    }

}
