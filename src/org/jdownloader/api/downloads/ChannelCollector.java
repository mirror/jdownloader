package org.jdownloader.api.downloads;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map.Entry;

import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;

import org.appwork.remoteapi.events.Subscriber;
import org.appwork.storage.JSonStorage;
import org.appwork.utils.Hash;
import org.appwork.utils.StringUtils;
import org.jdownloader.api.RemoteAPIController;
import org.jdownloader.api.downloads.v2.DownloadLinkAPIStorableV2;
import org.jdownloader.api.downloads.v2.DownloadsAPIV2Impl;
import org.jdownloader.api.downloads.v2.FilePackageAPIStorableV2;
import org.jdownloader.api.downloads.v2.LinkQueryStorable;
import org.jdownloader.api.downloads.v2.PackageQueryStorable;

public class ChannelCollector {

    private long                 interval = 1000;
    private long                 lastPush;
    private Subscriber           subscriber;
    private HashSet<String>      activeSubscriptions;

    private DownloadsAPIV2Impl   downloadsAPI;
    private LinkQueryStorable    linkQuery;
    private PackageQueryStorable pkgQuery;

    public ChannelCollector(Subscriber s) {
        this.subscriber = s;
        downloadsAPI = RemoteAPIController.getInstance().getDownloadsAPIV2();
        linkQuery = new LinkQueryStorable();
        linkQuery.setBytesLoaded(true);
        linkQuery.setBytesTotal(true);
        linkQuery.setEta(true);
        linkQuery.setSpeed(true);
        linkQuery.setStatus(true);

        pkgQuery = new PackageQueryStorable();
        pkgQuery.setBytesLoaded(true);
        pkgQuery.setBytesTotal(true);
        pkgQuery.setEta(true);
        pkgQuery.setSpeed(true);
        pkgQuery.setStatus(true);
    }

    public void setLastPush(long lastPush) {
        this.lastPush = lastPush;
    }

    public long getInterval() {
        return interval;
    }

    public void setInterval(long interval) {
        this.interval = interval;
    }

    public void updateSubscriptions() {
        HashSet<String> eventIds = new HashSet<String>();

        for (String id : DownloadControllerEventPublisher.INTERVAL_EVENT_ID_LIST) {

            if (subscriber.isSubscribed("downloads." + id)) {
                eventIds.add(id);

            }
        }

        this.activeSubscriptions = eventIds;
    }

    public boolean hasIntervalSubscriptions() {

        return activeSubscriptions != null && activeSubscriptions.size() > 0;
    }

    public long getLastPush() {
        return lastPush;
    }

    private HashMap<Long, DownloadLinkAPIStorableV2> compareMap = new HashMap<Long, DownloadLinkAPIStorableV2>();

    public HashMap<String, Object> getDiff(DownloadLink dl) {
        synchronized (this) {

            DownloadLinkAPIStorableV2 newData = DownloadsAPIV2Impl.toStorable(linkQuery, dl, this);
            DownloadLinkAPIStorableV2 oldData = compareMap.get(dl.getUniqueID().getID());
            compareMap.put(dl.getUniqueID().getID(), newData);

            HashMap<String, Object> dif = new HashMap<String, Object>();
            if (oldData == null || newData.getBytesLoaded() != oldData.getBytesLoaded()) {
                dif.put("bytesLoaded", newData.getBytesLoaded());
            }

            if (oldData == null || newData.getBytesTotal() != oldData.getBytesTotal()) {
                dif.put("bytesTotal", newData.getBytesTotal());
            }
            if (oldData == null || newData.getEta() != oldData.getEta()) {
                dif.put("eta", newData.getEta());
            }
            if (oldData == null || newData.getSpeed() != oldData.getSpeed()) {
                dif.put("speed", newData.getSpeed());
            }
            if (oldData == null || !StringUtils.equals(newData.getStatus(), oldData.getStatus())) {
                dif.put("status", newData.getStatus());
            }
            if (oldData == null || !StringUtils.equals(newData.getStatusIconKey(), oldData.getStatusIconKey())) {
                dif.put("statusIconKey", newData.getStatusIconKey());
            }

            return dif;
        }
    }

    public Subscriber getSubscriber() {
        return subscriber;
    }

    public void updateBase(DownloadLink dl) {
        compareMap.put(dl.getUniqueID().getID(), DownloadsAPIV2Impl.toStorable(linkQuery, dl, this));

    }

    public void cleanUp(HashSet<DownloadLink> linksToProcess) {
        HashSet<Long> idMap = new HashSet<Long>();
        HashSet<Long> idParentMap = new HashSet<Long>();
        for (DownloadLink dl : linksToProcess) {
            idMap.add(dl.getUniqueID().getID());
            FilePackage p = dl.getParentNode();
            if (p != null) {
                idParentMap.add(p.getUniqueID().getID());
            }
        }
        ArrayList<Long> toRemove = new ArrayList<Long>();
        for (Entry<Long, DownloadLinkAPIStorableV2> es : compareMap.entrySet()) {
            Long key;
            if ((key = es.getKey()) == null || !idMap.contains(key)) {
                toRemove.add(key);
            }
        }
        for (Long r : toRemove) {
            compareMap.remove(r);
        }

        toRemove.clear();
        for (Entry<Long, FilePackageAPIStorableV2> es : comparePackageMap.entrySet()) {
            Long key;
            if ((key = es.getKey()) == null || !idParentMap.contains(key)) {
                toRemove.add(key);
            }
        }
        for (Long r : toRemove) {
            comparePackageMap.remove(r);
        }
    }

    private HashMap<String, String> dupeCache = new HashMap<String, String>();

    public boolean updateDupeCache(String string, HashMap<String, Object> copy) {
        String id = Hash.getMD5(JSonStorage.serializeToJson(copy));
        if (StringUtils.equals(id, dupeCache.put(string, id))) {
            return false;
        }
        return true;
    }

    private HashMap<Long, FilePackageAPIStorableV2> comparePackageMap = new HashMap<Long, FilePackageAPIStorableV2>();

    public HashMap<String, Object> getDiff(FilePackage p) {
        synchronized (this) {

            FilePackageAPIStorableV2 newData = DownloadsAPIV2Impl.toStorable(pkgQuery, p, this);
            FilePackageAPIStorableV2 oldData = comparePackageMap.get(p.getUniqueID().getID());
            comparePackageMap.put(p.getUniqueID().getID(), newData);

            HashMap<String, Object> dif = new HashMap<String, Object>();
            if (oldData == null || newData.getBytesLoaded() != oldData.getBytesLoaded()) {
                dif.put("bytesLoaded", newData.getBytesLoaded());
            }

            if (oldData == null || newData.getBytesTotal() != oldData.getBytesTotal()) {
                dif.put("bytesTotal", newData.getBytesTotal());
            }
            if (oldData == null || newData.getEta() != oldData.getEta()) {
                dif.put("eta", newData.getEta());
            }
            if (oldData == null || newData.getSpeed() != oldData.getSpeed()) {
                dif.put("speed", newData.getSpeed());
            }
            if (oldData == null || !StringUtils.equals(newData.getStatus(), oldData.getStatus())) {
                dif.put("status", newData.getStatus());
            }
            if (oldData == null || !StringUtils.equals(newData.getStatusIconKey(), oldData.getStatusIconKey())) {
                dif.put("statusIconKey", newData.getStatusIconKey());
            }

            return dif;
        }
    }

}
