//    jDownloader - Downloadmanager
//    Copyright (C) 2008  JD-Team support@jdownloader.org
//
//    This program is free software: you can redistribute it and/or modify
//    it under the terms of the GNU General Public License as published by
//    the Free Software Foundation, either version 3 of the License, or
//    (at your option) any later version.
//
//    This program is distributed in the hope that it will be useful,
//    but WITHOUT ANY WARRANTY; without even the implied warranty of
//    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
//    GNU General Public License for more details.
//
//    You should have received a copy of the GNU General Public License
//    along with this program.  If not, see <http://www.gnu.org/licenses/>.
package jd.plugins.download;

import java.awt.Color;
import java.io.File;
import java.util.ArrayList;
import java.util.concurrent.atomic.AtomicBoolean;

import jd.controlling.downloadcontroller.ManagedThrottledConnectionHandler;
import jd.http.Browser;
import jd.http.Request;
import jd.http.URLConnectionAdapter;
import jd.plugins.PluginProgress;
import jd.plugins.download.raf.FileBytesMap.FileBytesMapView;
import jd.plugins.download.raf.HTTPDownloader;

import org.appwork.storage.config.JsonConfig;
import org.jdownloader.plugins.HashCheckPluginProgress;
import org.jdownloader.settings.GeneralSettings;

abstract public class DownloadInterface {
    @Deprecated
    public class Chunk {
        @Deprecated
        public Chunk(long startByte, long endByte, URLConnectionAdapter connection, DownloadInterface dl) {
        }

        @Deprecated
        public long getStartByte() {
            return -1;
        }

        @Deprecated
        public long getEndByte() {
            return -1;
        }
    }

    protected boolean fixWrongContentDispositionHeader = false;
    protected boolean allowFilenameFromURL             = true;
    protected Request initialRequest                   = null;

    /** Htmldecode filename from header? */
    @Deprecated
    public void setFilenameFix(boolean b) {
        this.fixWrongContentDispositionHeader = b;
    }

    public void setAllowFilenameFromURL(boolean b) {
        this.allowFilenameFromURL = b;
    }

    public static final boolean isNewHTTPCore() {
        return HTTPDownloader.class.isAssignableFrom(RAFDownload.class);
    }

    public abstract ManagedThrottledConnectionHandler getManagedConnetionHandler();

    public void setInitialRequest(Request initialRequest) {
        if (initialRequest == null) {
            throw new IllegalArgumentException("initialRequest is null!");
        } else {
            this.initialRequest = initialRequest;
        }
    };

    public abstract URLConnectionAdapter connect(Browser br) throws Exception;

    public abstract long getTotalLinkBytesLoadedLive();

    public FileBytesMapView getCacheMapView() {
        return null;
    };

    protected static final ArrayList<AtomicBoolean> HASHCHECK_QEUEU            = new ArrayList<AtomicBoolean>();
    protected static final int                      MAX_CONCURRENT_HASH_CHECKS = JsonConfig.create(GeneralSettings.class).getMaxConcurrentHashChecks();

    protected HashResult getHashResult(Downloadable downloadable, File file) throws InterruptedException {
        if (downloadable.isHashCheckEnabled() && JsonConfig.create(GeneralSettings.class).isHashCheckEnabled()) {
            final int maxConcurrent = MAX_CONCURRENT_HASH_CHECKS;
            final int nextWaitIndex = maxConcurrent - 1;
            AtomicBoolean waitLock = new AtomicBoolean(false);
            synchronized (HASHCHECK_QEUEU) {
                HASHCHECK_QEUEU.add(waitLock);
                waitLock.set(HASHCHECK_QEUEU.indexOf(waitLock) > nextWaitIndex);
            }
            try {
                final PluginProgress hashWait = new HashCheckPluginProgress(null, Color.YELLOW.darker().darker(), null);
                try {
                    downloadable.addPluginProgress(hashWait);
                    if (waitLock.get()) {
                        synchronized (waitLock) {
                            if (waitLock.get()) {
                                waitLock.wait();
                            }
                        }
                    }
                    final HashInfo hashInfo = downloadable.getHashInfo();
                    downloadable.removePluginProgress(hashWait);
                    final HashResult hashResult = downloadable.getHashResult(hashInfo, file);
                    if (hashResult != null) {
                        downloadable.getLogger().info(hashResult.toString());
                        if (hashResult.getFinalLinkState().isFinished()) {
                            downloadable.setHashInfo(hashResult.getHashInfo());
                        }
                    }
                    return hashResult;
                } finally {
                    downloadable.removePluginProgress(hashWait);
                }
            } finally {
                synchronized (HASHCHECK_QEUEU) {
                    final boolean callNext = HASHCHECK_QEUEU.indexOf(waitLock) <= nextWaitIndex;
                    HASHCHECK_QEUEU.remove(waitLock);
                    if (HASHCHECK_QEUEU.size() > nextWaitIndex && callNext) {
                        waitLock = HASHCHECK_QEUEU.get(nextWaitIndex);
                    } else {
                        waitLock = null;
                    }
                }
                if (waitLock != null) {
                    synchronized (waitLock) {
                        waitLock.set(false);
                        waitLock.notifyAll();
                    }
                }
            }
        }
        return null;
    }

    public abstract boolean startDownload() throws Exception;

    public abstract URLConnectionAdapter getConnection();

    public abstract void stopDownload();

    public abstract boolean externalDownloadStop();

    public abstract long getStartTimeStamp();

    public abstract void close();

    public abstract Downloadable getDownloadable();

    /**
     * returns of the download has been resumed
     *
     * @return
     */
    public abstract boolean isResumedDownload();
}