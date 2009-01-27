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

package jd.http.download;

public class DownloadEvent {

    private int id;

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public DownloadInterface getSource() {
        return source;
    }

    public void setSource(DownloadInterface source) {
        this.source = source;
    }

    private DownloadInterface source;
    private DownloadChunk chunk;

    public DownloadEvent(int eventID, DownloadInterface download) {
        this.id = eventID;
        this.source = download;
    }

    public DownloadEvent(int eventID, DownloadInterface download, DownloadChunk job) {
        this(eventID, download);
        this.chunk = job;
    }

    public DownloadChunk getChunk() {
        return chunk;
    }

    public void setChunk(DownloadChunk chunk) {
        this.chunk = chunk;
    }

    // public static final int UPDATE_CONNECTION_PROPERTY_CONTENTLENGTH = 1 <<
    // 0;
    // public static final int UPDATE_CONNECTION_PROPERTY_FILENAME = 1 << 1;
    public static final int STATUS_CONNECTED = 1 << 0;
    public static final int STATUS_STARTED = 1 << 2;
    public static final int STATUS_DOWNLOAD_FISNISHED = 1 << 1;
    public static final int STATUS_FINISHED = 1 << 3;
    protected static final int PROGRESS_CHUNK_FINISHED = 1 << 4;
    protected static final int PROGRESS_CHUNK_STARTED = 1 << 5;
    public static final int PROGRESS_CHUNK_BUFFERWRITTEN = 1<<6;

}
