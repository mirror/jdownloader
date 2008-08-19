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

public class ContainerStatus {

    public static final int STATUS_FAILED = 1 << 2;
    public static final int STATUS_FINISHED = 1 << 1;
    public static final int TODO = 1 << 0;
    private File container;
    private int status = TODO;

    public ContainerStatus() {

    }

    public ContainerStatus(File lc) {
        container = lc;
    }

    /**
     * Fügt einen LinkStatus.* Status hinzu.Der alte status wird dabei nicht
     * gelöscht.
     * 
     * @param status
     */
    public void addStatus(int status) {
        this.status |= status;

    }

    public File getContainer() {
        return container;
    }

    public int getLatestStatus() {

        return 0;
    }

    /**
     * Gibt zurück ob der zugehörige Link einen bestimmten status hat.
     * 
     * @param status
     * @return
     */
    public boolean hasStatus(int status) {

        return (this.status | status) > 0;
    }

    public boolean isStatus(int status) {
        return this.status == status;
    }

    /** Entfernt eine Statusid */
    public void removeStatus(int status) {
        this.status ^= status;
    }

    public void setErrorMessage(String string) {

    }

    /**
     * Setzt den Linkstatus. Es dürfen nur LInkStatus.*STATUS ids verwendet
     * werden
     * 
     * @param status
     */
    public void setStatus(int status) {
        this.status = status;
    }

    public void setStatusText(String l) {

    }

}
