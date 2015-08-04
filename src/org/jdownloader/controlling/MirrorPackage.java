package org.jdownloader.controlling;

import java.io.File;
import java.util.ArrayList;
import java.util.Locale;

import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;

import org.appwork.utils.StringUtils;
import org.jdownloader.plugins.FinalLinkState;

public class MirrorPackage {

    private final String                  id;
    private final ArrayList<DownloadLink> list     = new ArrayList<DownloadLink>();
    private boolean                       finished = false;

    public boolean isFinished() {
        return finished;
    }

    public long getBytesLoaded() {
        return bytesLoaded;
    }

    private long                     bytesLoaded = 0;
    private long                     bytesTotal  = -1;
    private boolean                  online      = false;
    private boolean                  offline     = true;
    private boolean                  enabled     = false;
    private String                   md5         = null;
    private String                   sha1        = null;
    private String                   sha256      = null;
    private long                     speed;
    private final MirrorPackageSetup setup;

    public boolean isOffline() {
        return offline;
    }

    public boolean isUnknownOnlineStatus() {
        return !offline && !online;
    }

    public boolean isOnline() {
        return online;
    }

    public MirrorPackage(String mirrorID, MirrorPackageSetup setup) {
        id = mirrorID;
        this.setup = setup;
    }

    public String add(DownloadLink link) {
        final DownloadLinkView view = link.getView();
        final long linkBytesTotal = view.getBytesTotal();
        if (bytesTotal > 0 && linkBytesTotal > 0 && linkBytesTotal != bytesTotal) {
            // size mismatch
            return id + "/" + linkBytesTotal;
        }
        final String linkMD5 = link.getMD5Hash();
        if (StringUtils.isNotEmpty(md5) && StringUtils.isNotEmpty(linkMD5) && !StringUtils.equalsIgnoreCase(md5, linkMD5)) {
            // hash mismatch
            return id + "/" + linkMD5.toLowerCase(Locale.ENGLISH);
        }
        final String linkSHA1 = link.getSha1Hash();
        if (StringUtils.isNotEmpty(sha1) && StringUtils.isNotEmpty(linkSHA1) && !StringUtils.equalsIgnoreCase(sha1, linkSHA1)) {
            // hash mismatch
            return id + "/" + linkSHA1.toLowerCase(Locale.ENGLISH);
        }
        final String linkSHA256 = link.getSha256Hash();
        if (StringUtils.isNotEmpty(sha256) && StringUtils.isNotEmpty(linkSHA256) && !StringUtils.equalsIgnoreCase(sha256, linkSHA256)) {
            // hash mismatch
            return id + "/" + linkSHA256.toLowerCase(Locale.ENGLISH);
        }
        if (StringUtils.isEmpty(md5)) {
            md5 = linkMD5;
        }
        if (StringUtils.isEmpty(sha1)) {
            sha1 = linkSHA1;
        }
        if (StringUtils.isEmpty(sha256)) {
            sha256 = linkSHA256;
        }

        final boolean isFinished = FinalLinkState.CheckFinished(link.getFinalLinkState());
        if (isFinished) {
            finished = true;
        }
        if (setup.isLocalFileUsageEnabled()) {
            final String fileOutput = link.getFileOutput();
            if (StringUtils.isNotEmpty(fileOutput)) {
                final File completeFile = new File(fileOutput);
                if (completeFile.exists()) {
                    bytesLoaded = Math.max(bytesLoaded, completeFile.length());
                } else {
                    final File partFile = new File(fileOutput + ".part");
                    if (partFile.exists()) {
                        bytesLoaded = Math.max(bytesLoaded, partFile.length());
                    }
                }
            }
        } else {
            bytesLoaded = Math.max(bytesLoaded, view.getBytesLoaded());
        }
        bytesTotal = Math.max(bytesTotal, view.getBytesTotal());

        if (link.getAvailableStatus() == AvailableStatus.TRUE) {
            online = true;
        } else {
            offline &= link.getAvailableStatus() == AvailableStatus.FALSE;
        }
        if (link.isEnabled()) {
            enabled = true;
            if (!isFinished && link.getDownloadLinkController() != null) {
                speed = Math.max(speed, view.getSpeedBps());
            }
        }
        list.add(link);
        return null;
    }

    private String getFileOutput(DownloadLink link) {
        return link.getFileOutput();
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
