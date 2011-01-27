//    jDownloader - Downloadmanager
//    Copyright (C) 2010  JD-Team support@jdownloader.org
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

package jd.plugins.optional.folderwatch.data;

import java.io.File;
import java.io.Serializable;
import java.text.SimpleDateFormat;
import java.util.Date;

public class HistoryEntry implements Serializable {

    private static final long       serialVersionUID = -8824175672927232845L;

    private static SimpleDateFormat DATE_FORMAT      = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

    private String                  filename;

    private String                  absolutePath;

    private String                  md5Hash;

    private String                  importDate;

    private boolean                 isExisting;

    public HistoryEntry(File file, String md5Hash, boolean isExisting) {
        set(file, md5Hash, isExisting);
    }

    public HistoryEntry(File file, String md5Hash) {
        this(file, md5Hash, true);
    }

    public void set(File file, String md5Hash, boolean isExisting) {
        this.setFilename(file.getName());
        this.setAbsolutePath(file.getAbsolutePath());
        this.setMd5Hash(md5Hash);
        this.setImportDate();
        this.setExisting(isExisting);
    }

    public void setFilename(String filename) {
        this.filename = filename;
    }

    public String getFilename() {
        return filename;
    }

    public void setAbsolutePath(String absolutePath) {
        this.absolutePath = absolutePath;
    }

    public String getAbsolutePath() {
        return absolutePath;
    }

    public void setMd5Hash(String md5Hash) {
        this.md5Hash = md5Hash;
    }

    public String getMd5Hash() {
        return md5Hash;
    }

    private void setImportDate() {
        importDate = DATE_FORMAT.format(new Date());
    }

    public void setImportDate(String mssg) {
        if (mssg == null) {
            importDate = "N/A";
        } else {
            importDate = mssg;
        }
    }

    public String getImportDate() {
        return importDate;
    }

    public void setExisting(boolean value) {
        isExisting = value;
    }

    public boolean isExisting() {
        return isExisting;
    }
}
