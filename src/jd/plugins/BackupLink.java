//    jDownloader - Downloadmanager
//    Copyright (C) 2008  JD-Team jdownloader@freenet.de
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
import java.io.Serializable;

public class BackupLink implements Serializable {

    public static final int LINKTYPE_CONTAINER = 1;
    public static final int LINKTYPE_NORMAL = 0;
    /**
     * 
     */
    private static final long serialVersionUID = 1L;

    /**
     * Containername
     */
    private String container;

    /**
     * Dateiname des Containers
     */
    private String containerFile;

    /**
     * Index dieses DownloadLinks innerhalb der Containerdatei
     */
    private int containerIndex = -1;
    private String containerType;
    private int linkType;
    /**
     * Von hier soll de Download stattfinden
     */
    private String urlDownload;

    public BackupLink(File containerfile, int id, String containerType) {
        containerFile = containerfile.getAbsolutePath();
        containerIndex = id;
        this.containerType = containerType;
        linkType = LINKTYPE_CONTAINER;

    }

    public BackupLink(String urlDownload) {
        linkType = LINKTYPE_NORMAL;
        this.urlDownload = urlDownload;

    }

    public String getContainer() {
        return container;
    }

    public String getContainerFile() {
        return containerFile;
    }

    public int getContainerIndex() {
        return containerIndex;
    }

    public String getContainerType() {
        return containerType;
    }

    public int getLinkType() {
        return linkType;
    }

    public String getUrlDownload() {
        return urlDownload;
    }

    @Override
    public String toString() {
        return " containerType :" + containerType + " linkType :" + linkType + " containerIndex :" + containerIndex + " container :" + container + " urlDownload :" + urlDownload;
    }

}
