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

package jd.http;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

public class Cookie {
    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("EEE, dd-MMM-yyyy hh:mm:ss z");
    private Date expires = null;
    private String formatedexpires = null;
    private String path;
    private String host;
    private String value;
    private String key;
    private String domain;

    public Cookie(String host, String key, String value) {
        this.host = host;
        this.key = key;
        this.value = value;
    }

    public Cookie() {
        // TODO Auto-generated constructor stub
    }

    public void setHost(String host) {
        this.host = host;

    }

    public void setPath(String path) {
        this.path = path;

    }

    public void setExpires(String expires) {
        if (expires == null) {
            this.expires = null;
            this.formatedexpires = null;
            return;
        }
        try {
            this.expires = DATE_FORMAT.parse(expires);
        } catch (ParseException e) {
            try {
                this.expires = new SimpleDateFormat("EEE, dd MMM yyyy hh:mm:ss z").parse(expires);
            } catch (ParseException e2) {
                e2.printStackTrace();
                this.expires = null;
                this.formatedexpires = null;
                return;
            }
        }
        this.formatedexpires = DATE_FORMAT.format(this.expires);
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
        if (expires == null || formatedexpires == null) return false;
        String Current = DATE_FORMAT.format(new Date());
        try {
            return DATE_FORMAT.parse(Current).after(DATE_FORMAT.parse(formatedexpires));
        } catch (ParseException e1) {
            // TODO Auto-generated catch block
            e1.printStackTrace();
            return false;
        }
    }

    public Date getExpires() {
        return expires;
    }

    public void setExpires(Date expires) {
        this.expires = expires;
        if (expires != null) {
            formatedexpires = DATE_FORMAT.format(expires);
        } else {
            formatedexpires = null;
        }
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

}
