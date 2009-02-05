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

package jd.plugins;

import java.io.File;

import jd.config.Property;
import jd.http.Encoding;

public class BackupLink extends Property {

    public static final int LINKTYPE_CONTAINER = 1;
    public static final int LINKTYPE_NORMAL = 0;

    private static final long serialVersionUID = 1L;

    // Index dieses DownloadLinks innerhalb der Containerdatei

    private int linkType;

    // Von hier soll der Download stattfinden

    private String urlDownload;

    public BackupLink(File containerfile, int id, String containerType) {

        setProperty("container", containerType);
        setProperty("containerfile", containerfile.getAbsolutePath());
        setProperty("containerindex", id);
        linkType = LINKTYPE_CONTAINER;
    }

    public BackupLink(String urlDownload) {
        linkType = LINKTYPE_NORMAL;
        this.urlDownload = Encoding.Base64Encode(urlDownload);
    }

    public int getLinkType() {
        return linkType;
    }

    public String getUrlDownload() {
        return Encoding.Base64Decode(urlDownload);
    }

}