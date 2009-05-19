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
import java.io.Serializable;

public class DownloadProgress implements Serializable {

    /**
     * 
     */
    private static final long serialVersionUID = 7415576592826692024L;
    private File file;

    public File getFile() {
        return file;
    }

    public void setFile(File file) {
        this.file = file;
    }

    public ChunkProgress[] getChunks() {
        return chunks;
    }

    public String toString() {
        String ret = "" + file + "\r\n";
        if (chunks == null) return ret;
        for (int i = 0; i < chunks.length; i++) {
            ret += i + ": " + chunks[i] + "\r\n";
        }
        return ret;

    }

    private ChunkProgress[] chunks;
    private int i = 0;
    public int totalLoaded;

    public DownloadProgress(File outputFile) {
        this.file = outputFile;
    }

    public void reset(int num) {
        i = 0;
        this.chunks = new ChunkProgress[num];

    }

    public void add(ChunkProgress cp) {
        chunks[i++] = cp;
    }
}
