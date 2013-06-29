package org.jdownloader.controlling;

import java.io.File;
import java.util.ArrayList;
import java.util.Locale;

import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;

import org.appwork.utils.StringUtils;

public class MirrorPackage {

    private String                  id;
    private ArrayList<DownloadLink> list;
    private boolean                 finished = false;

    public boolean isFinished() {
        return finished;
    }

    public long getBytesLoaded() {
        return bytesLoaded;
    }

    private long    bytesLoaded = 0;
    private long    bytesTotal  = -1;
    private boolean online      = false;
    private boolean offline     = true;
    private boolean enabled     = false;
    private String  md5         = null;
    private String  sha1        = null;
    private long    speed;

    public boolean isOffline() {
        return offline;
    }

    public boolean isUnknownOnlineStatus() {
        return !offline && !online;
    }

    public boolean isOnline() {
        return online;
    }

    public MirrorPackage(String mirrorID) {
        id = mirrorID;
        list = new ArrayList<DownloadLink>();
    }

    public String add(DownloadLink link) {
        if (bytesTotal > 0 && link.getDownloadMax() > 0 && link.getDownloadMax() != bytesTotal) {
            // size mismatch
            return id + "/" + link.getDownloadMax();
        }
        if (StringUtils.isNotEmpty(md5) && StringUtils.isNotEmpty(link.getMD5Hash()) && !link.getMD5Hash().toLowerCase(Locale.ENGLISH).equals(md5)) {
            // hash mismatch
            return id + "/" + link.getMD5Hash().toLowerCase(Locale.ENGLISH);
        }

        if (StringUtils.isNotEmpty(sha1) && StringUtils.isNotEmpty(link.getSha1Hash()) && !link.getSha1Hash().toLowerCase(Locale.ENGLISH).equals(sha1)) {
            // hash mismatch
            return id + "/" + link.getSha1Hash().toLowerCase(Locale.ENGLISH);
        }
        finished |= link.getLinkStatus().isFinished() && new File(link.getFileOutput()).exists();
        if (link.isEnabled()) {
            bytesLoaded = Math.max(bytesLoaded, link.getDownloadCurrent());
        }
        if (StringUtils.isEmpty(md5) && StringUtils.isNotEmpty(link.getMD5Hash())) {
            md5 = link.getMD5Hash().toLowerCase(Locale.ENGLISH);
        }

        if (StringUtils.isEmpty(sha1) && StringUtils.isNotEmpty(link.getSha1Hash())) {
            sha1 = link.getSha1Hash().toLowerCase(Locale.ENGLISH);
        }
        if (bytesTotal < 0) bytesTotal = link.getDownloadMax();

        online |= link.getAvailableStatus() == AvailableStatus.TRUE;
        offline &= link.getAvailableStatus() == AvailableStatus.FALSE;
        enabled |= link.isEnabled();
        speed = Math.max(speed, link.getDownloadSpeed());
        list.add(link);
        return null;
    }

    public long getTotalBytes() {
        return bytesTotal;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public long getSpeed() {
        return speed;
    }

}
