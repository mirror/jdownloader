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
import java.lang.reflect.Field;

import jd.controlling.JDLogger;

import org.appwork.utils.formatter.StringFormatter;

public class ContainerStatus {

    public static final int STATUS_FAILED = 1 << 2;
    public static final int STATUS_FINISHED = 1 << 1;
    public static final int TODO = 1 << 0;
    private File container;
    private int status = TODO;
    private String statusText;
    private int latestStatus;

    public ContainerStatus() {

    }

    public ContainerStatus(File lc) {
        container = lc;
    }

    /**
     * Fügt einen LinkStatus.* Status hinzu.Der alte Status wird dabei nicht
     * gelöscht.
     * 
     * @param status
     */
    public void addStatus(int status) {
        latestStatus = status;
        this.status |= status;
    }

    public File getContainer() {
        return container;
    }

    /**
     * Gibt zurück ob der zugehörige Link einen bestimmten Status hat.
     * 
     * @param status
     * @return
     */
    public boolean hasStatus(int status) {

        return (this.status & status) > 0;
    }

    public boolean isStatus(int status) {
        return this.status == status;
    }

    // Entfernt eine Statusid
    public void removeStatus(int status) {
        this.status ^= status;
    }

    /**
     * Setzt den Linkstatus. Es dürfen nur LinkStatus.*STATUS ids verwendet
     * werden
     * 
     * @param status
     */
    public void setStatus(int status) {
        this.status = status;
    }

    public void setStatusText(String l) {
        this.statusText = l;
    }

    public String getStatusText() {
        return statusText;
    }

    //@Override
    public String toString() {
        Class<? extends ContainerStatus> cl = this.getClass();
        Field[] fields = cl.getDeclaredFields();
        StringBuilder sb = new StringBuilder();
        sb.append(StringFormatter.fillString(Integer.toBinaryString(status), "0", "", 32) + " <Statuscode\r\n");
        String latest = "";
        for (Field field : fields) {
            if (field.getModifiers() == 25) {
                int value;
                try {
                    value = field.getInt(this);
                    if (hasStatus(value)) {
                        if (value == latestStatus) {
                            latest = "latest:" + field.getName() + "\r\n";
                            sb.append(StringFormatter.fillString(Integer.toBinaryString(value), "0", "", 32) + " |" + field.getName() + "\r\n");

                        } else {

                            sb.append(StringFormatter.fillString(Integer.toBinaryString(value), "0", "", 32) + " |" + field.getName() + "\r\n");
                        }
                    }
                } catch (IllegalArgumentException e) {
                    JDLogger.exception(e);
                } catch (IllegalAccessException e) {
                    JDLogger.exception(e);
                }
            }
        }

        String ret = latest + sb;

        if (statusText != null) {
            ret += "StatusText: " + statusText + "\r\n";
        }
        return ret;
    }
}
