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
    protected boolean allowFilenameFromURL             = false;

    protected Request initialRequest                   = null;

    public void setFilenameFix(boolean b) {
        this.fixWrongContentDispositionHeader = b;
    }

    public void setAllowFilenameFromURL(boolean b) {
        this.allowFilenameFromURL = b;
    }

    /* do not use in old JD 09581 plugins */
    public abstract ManagedThrottledConnectionHandler getManagedConnetionHandler();

    public void setInitialRequest(Request initialRequest) {
        if (initialRequest == null) {
            throw new IllegalArgumentException("initialRequest is null!");
        }
        this.initialRequest = initialRequest;
    };

    public abstract URLConnectionAdapter connect(Browser br) throws Exception;

    public abstract long getTotalLinkBytesLoadedLive();

    public FileBytesMapView getCacheMapView() {
        return null;
    };

    protected static final ArrayList<AtomicBoolean> HASHCHECK_QEUEU = new ArrayList<AtomicBoolean>();

    protected HashResult getHashResult(Downloadable downloadable, File file) throws InterruptedException {
        if (JsonConfig.create(GeneralSettings.class).isHashCheckEnabled() && downloadable.isHashCheckEnabled()) {
            AtomicBoolean hashCheckLock = new AtomicBoolean(false);
            synchronized (HASHCHECK_QEUEU) {
                HASHCHECK_QEUEU.add(hashCheckLock);
                hashCheckLock.set(HASHCHECK_QEUEU.indexOf(hashCheckLock) != 0);
            }
            try {
                if (hashCheckLock.get()) {
                    synchronized (hashCheckLock) {
                        if (hashCheckLock.get()) {
                            final PluginProgress hashProgress = new HashCheckPluginProgress(null, Color.YELLOW.darker().darker(), null);
                            try {
                                downloadable.addPluginProgress(hashProgress);
                                hashCheckLock.wait();
                            } finally {
                                downloadable.removePluginProgress(hashProgress);
                            }
                        }
                    }
                }
                final HashInfo hashInfo = downloadable.getHashInfo();
                final HashResult hashResult = downloadable.getHashResult(hashInfo, file);
                return hashResult;
            } finally {
                synchronized (HASHCHECK_QEUEU) {
                    boolean callNext = HASHCHECK_QEUEU.indexOf(hashCheckLock) == 0;
                    HASHCHECK_QEUEU.remove(hashCheckLock);
                    if (HASHCHECK_QEUEU.size() > 0 && callNext) {
                        hashCheckLock = HASHCHECK_QEUEU.get(0);
                    } else {
                        hashCheckLock = null;
                    }
                }
                if (hashCheckLock != null) {
                    synchronized (hashCheckLock) {
                        hashCheckLock.set(false);
                        hashCheckLock.notifyAll();
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

    /* do not use in old JD 09581 plugins */
    /**
     * returns of the download has been resumed
     *
     * @return
     */
    public abstract boolean isResumedDownload();

}