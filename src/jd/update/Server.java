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

package jd.update;

import java.io.Serializable;
import java.util.ArrayList;

public class Server implements Serializable {

    /**
     * 
     */
    private static final long serialVersionUID = 701200615385983L;
    private int               percent;
    private String            path;
    private long              requestTime      = 0;
    private int               requestCount     = 0;

    public int getPercent() {
        return percent;
    }

    public void setPercent(int percent) {
        this.percent = percent;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public String toString() {
        return this.path + " (" + percent + "%)";
    }

    public Server(int percent, String host) {
        this.percent = percent;
        this.path = host;
    }

    /**
     * selects depending on the percents a ranodm server
     * 
     * @param list
     * @return
     */
    public static Server selectServer(java.util.List<Server> list) {

        int rand = (int) (Math.random() * 100);
        int total = 0;
        Server ret = null;
        for (Server s : list) {
            if (s.getPercent() <= 0) { return selectServerByRequestTime(list); }
            ret = s;
            total += s.getPercent();
            if (rand <= total && rand > total - s.getPercent()) break;
        }

        return ret;

    }

    /**
     * Gibt den server mit der besten requestzeit zurück
     * 
     * @param list
     * @return
     */
    private static Server selectServerByRequestTime(java.util.List<Server> list) {

        Server ret = null;
        for (Server s : list) {
            if (ret == null || s.getRequestTime() < ret.getRequestTime()) ret = s;

        }

        return ret;
    }

    /**
     * summiert die requestzeiten zu einem durchschnitt
     * 
     * @param l
     */
    public void setRequestTime(long l) {
        requestTime = (requestTime * requestCount + l) / (requestCount + 1);
        this.requestCount++;

    }

    public long getRequestTime() {
        return requestTime;
    }
}