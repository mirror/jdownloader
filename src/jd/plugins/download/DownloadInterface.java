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

import jd.controlling.downloadcontroller.ManagedThrottledConnectionHandler;
import jd.http.Browser;
import jd.http.URLConnectionAdapter;

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

    public void setFilenameFix(boolean b) {
        this.fixWrongContentDispositionHeader = b;
    }

    public void setAllowFilenameFromURL(boolean b) {
        this.allowFilenameFromURL = b;
    }

    /* do not use in old JD 09581 plugins */
    @Deprecated
    public abstract ManagedThrottledConnectionHandler getManagedConnetionHandler();

    public abstract URLConnectionAdapter connect(Browser br) throws Exception;

    public abstract long getTotalLinkBytesLoadedLive();

    public abstract boolean startDownload() throws Exception;

    public abstract URLConnectionAdapter getConnection();

    public abstract void stopDownload();

    public abstract boolean externalDownloadStop();

    public abstract long getStartTimeStamp();

    public abstract void close();

}