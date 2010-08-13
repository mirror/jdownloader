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

package jd.plugins.optional.folderwatch.history;

import java.text.SimpleDateFormat;
import java.util.Date;

public class FolderWatchHistoryEntry {

    private static SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

    private String basename;

    private String absolutePath;

    private String md5Hash;

    private String importDate;

    private boolean isPhysical;

    public FolderWatchHistoryEntry(String basename, String absolutePath, String md5Hash, boolean isPhysical) {
        set(basename, absolutePath, md5Hash, isPhysical);
    }

    public FolderWatchHistoryEntry(String basename, String absolutePath, String md5Hash) {
        set(basename, absolutePath, md5Hash, true);
    }

    public void set(String basename, String absolutePath, String md5Hash, boolean isPhysical) {
        this.setBasename(basename);
        this.setAbsolutePath(absolutePath);
        this.setMd5Hash(md5Hash);
        this.setImportDate();
        this.setPhysical(isPhysical);
    }

    public void setBasename(String basename) {
        this.basename = basename;
    }

    public String getBasename() {
        return basename;
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
        Date date = new Date();

        this.importDate = DATE_FORMAT.format(date);
    }

    public String getImportDate() {
        return importDate;
    }

    public void setPhysical(boolean isPhysical) {
        this.isPhysical = isPhysical;
    }

    public boolean isPhysical() {
        return isPhysical;
    }
}
