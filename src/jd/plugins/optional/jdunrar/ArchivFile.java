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

package jd.plugins.optional.jdunrar;

import java.io.File;
import java.util.ArrayList;

public class ArchivFile {

    private String filepath;

    private int percent;
    private File path;

    public File getPath() {
        return path;
    }

    public void setPath(final File file) {
        this.path = file;
    }

    private long size = 0;

    private boolean isProtected = false;

    private final ArrayList<String> volumes;

    public ArchivFile(final String name) {
        this.filepath = name;
        volumes = new ArrayList<String>();
    }

    public void addVolume(final String vol) {
        if (vol != null && volumes.indexOf(vol) < 0) {
            this.volumes.add(vol);
        }
    }

    public String getFilepath() {
        return filepath;
    }

    public int getPercent() {
        return percent;
    }

    public long getSize() {
        return size;
    }

    public ArrayList<String> getVolumes() {
        return volumes;
    }

    public boolean isProtected() {
        return isProtected;
    }

    public void setFilepath(final String filepath) {
        this.filepath = filepath;
    }

    public void setPercent(final int percent) {
        this.percent = percent;
    }

    public void setProtected(final boolean b) {
        this.isProtected = b;
    }

    public void setSize(final long size) {
        this.size = size;
    }

    public String toString() {
        return this.filepath;
    }

    public File getFile() {
        return new File(path, this.filepath);
    }

}
