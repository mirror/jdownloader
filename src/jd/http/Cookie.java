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

package jd.http;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import jd.controlling.JDLogger;

public class Cookie {

    private static final String[] dateformats = new String[] { "EEE, dd-MMM-yyyy HH:mm:ss z", "EEE, dd MMM yyyy HH:mm:ss z", "EEE MMM dd HH:mm:ss z yyyy", "EEE, dd-MMM-yyyy HH:mm:ss z", "EEEE, dd-MMM-yy HH:mm:ss z" };

    private String path;
    private String host;
    private String value;
    private String key;
    private String domain;

    private long hostTime = -1;
    private long creationTime = System.currentTimeMillis();
    private long expireTime = -1;

    public Cookie(String host, String key, String value) {
        this.host = host;
        this.key = key;
        this.value = value;
    }

    public Cookie() {
        host = "";
        key = "";
        value = "";
    }

    public void setHost(String host) {
        this.host = host;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public void setExpires(String expires) {
        if (expires == null) {
            this.expireTime = -1;
            // System.out.println("setExpire: Cookie: no expireDate found! " +
            // this.host + " " + this.key);
            return;
        }
        Date expireDate = null;
        for (String format : dateformats) {
            try {
                SimpleDateFormat sdf = new SimpleDateFormat(format, Locale.UK);
                sdf.setLenient(false);
                expireDate = sdf.parse(expires);
                break;
            } catch (Exception e2) {
            }
        }
        if (expireDate != null) {
            this.expireTime = expireDate.getTime();
            return;
        }
        this.expireTime = -1;
        JDLogger.getLogger().severe("Cookie: no Format for " + expires + " found!");
        return;
    }

    public void setValue(String value) {
        this.value = value;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public void setDomain(String domain) {
        this.domain = domain;
    }

    public boolean isExpired() {
        if (this.expireTime == -1) {
            // System.out.println("isexpired: no expireDate found! " + this.host
            // + " " + this.key);
            return false;
        }
        if (this.hostTime == -1) {
            JDLogger.getLogger().severe("Cookie: no HostTime found! ExpireStatus cannot be checked " + this.host + " " + this.key);
            return false;
        } else {
            long check = (System.currentTimeMillis() - this.creationTime) + this.hostTime;
            // System.out.println(this.host + " " + this.key + " " +
            // this.creationTime + " " + this.hostTime + " " + this.expireTime +
            // " " + check);
            if (check > this.expireTime) {
                // System.out.println("Expired: " + this.host + " " + this.key);
                return true;
            } else
                return false;
        }
    }

    public long getHostTime() {
        return this.hostTime;
    }

    public long getExpireDate() {
        return this.expireTime;
    }

    public void setHostTime(String Date) {
        if (Date == null) {
            this.hostTime = -1;
            // System.out.println("Cookie: no HostTime found! " + this.host +
            // " " + this.key);
            return;
        }
        Date ResponseDate = null;
        for (String format : dateformats) {
            try {
                SimpleDateFormat sdf = new SimpleDateFormat(format, Locale.UK);
                sdf.setLenient(false);
                ResponseDate = sdf.parse(Date);
                break;
            } catch (Exception e2) {
            }
        }
        if (ResponseDate != null) {
            this.hostTime = ResponseDate.getTime();
            return;
        }
        this.hostTime = -1;
        JDLogger.getLogger().severe("Cookie: no Format for " + Date + " found!");
        return;
    }

    public String getPath() {
        return path;
    }

    public String getHost() {
        return host;
    }

    public String getValue() {
        return value;
    }

    public String getKey() {
        return key;
    }

    public String getDomain() {
        return domain;
    }

    public String toString() {
        return key + "=" + value + " @" + host;
    }

    public boolean equals(Cookie cookie2) {
        if (cookie2 == this) return true;
        if (!cookie2.getHost().equalsIgnoreCase(this.getHost())) return false;
        if (!cookie2.getKey().equalsIgnoreCase(this.getKey())) return false;
        return true;
    }

}
