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

package jd.http.download;

public class BrowserException extends Exception {

    private static final long serialVersionUID = -1576784726086641221L;

    public static final int TYPE_RANGE = 1;
    public static final int TYPE_BADREQUEST = 2;
    public static final int TYPE_REDIRECT = 3;
    public static final int TYPE_LOCAL_IO = 4;

    private int type;

    public int getType() {
        return type;
    }

    public void setType(int type) {
        this.type = type;
    }

    public BrowserException(String l) {
        super(l);
    }

    public BrowserException(String l, int type) {
        super(l);
        this.type = type;
    }

}
