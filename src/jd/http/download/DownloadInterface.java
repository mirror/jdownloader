//    jDownloader - Downloadmanager
//    Copyright (C) 2009  JD-Team support@jdownloader.org
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

package jd.http.download;

import java.io.File;
import java.util.ArrayList;

abstract public class DownloadInterface {
    private int flags = 0;
    private final ArrayList<DownloadListener> downloadListener;
    private final File outputFile;

    public DownloadInterface(final int flags, final File file) {
        this.flags = flags;
        this.outputFile = file;
        downloadListener = new ArrayList<DownloadListener>();
    }

    protected void fireEvent(final DownloadEvent downloadEvent) {
        for (DownloadListener dl : downloadListener) {
            dl.onStatus(downloadEvent);
        }
    }

    protected void fireEvent(final int eventID) {
        fireEvent(new DownloadEvent(eventID, this));
    }

    abstract public void setBandwidthLimit(long bytesPerSecond);

    abstract public long getFileSize();

    abstract public long getBandwidthLimit();

    public void addDownloadListener(final DownloadListener downloadListener) {
        this.removeDownloadListener(downloadListener);
        this.downloadListener.add(downloadListener);
    }

    public File getOutputFile() {
        return outputFile;
    }

    abstract public long getSpeed();

    public void removeDownloadListener(final DownloadListener downloadListener) {
        this.downloadListener.remove(downloadListener);
    }

    public void addStatus(final int status) {
        this.flags |= status;
    }

    public boolean hasStatus(final int status) {
        return (this.flags & status) > 0;
    }

    public void removeStatus(final int status) {
        int mask = 0xffffffff;
        mask &= ~status;
        this.flags &= mask;
    }
}
