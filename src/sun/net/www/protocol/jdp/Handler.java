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

package sun.net.www.protocol.jdp;

import java.io.IOException;
import java.net.Proxy;
import java.net.URL;
import java.net.URLConnection;

import jd.http.Browser;
import jd.http.HTTPConnection;
import jd.http.JDProxy;

public class Handler extends sun.net.www.protocol.http.Handler {

    public Handler() {
        this(null, -1);
    }

    public Handler(final String proxy, final int port) {
        this.proxy = proxy;
        this.proxyPort = port;
    }

    @Override
    protected URLConnection openConnection(final URL u) throws IOException {
        return openConnection(u, (Proxy) null);
    }

    @Override
    protected URLConnection openConnection(final URL u, final Proxy p) throws IOException {
        String urlCorrect = u.toString();
        if (urlCorrect.startsWith("jdp")) {
            urlCorrect = "http" + urlCorrect.substring(3);
        }
        if (u.getUserInfo() != null) {
            final String[] logins = u.getUserInfo().split(":");
            Browser.getAssignedBrowserInstance(u).setAuth(u.getHost(), logins[0], logins.length > 1 ? logins[1] : "");
        }
        if (p == null) {
            return new HTTPConnection(Browser.reAssignUrlToBrowserInstance(u, new URL(urlCorrect)), (Proxy) null, this);

        } else {
            final JDProxy pr = Browser.getAssignedBrowserInstance(u).getRequest().getProxy();

            if (pr == null) { throw new IOException("Proxy Mapping failed."); }
            return new HTTPConnection(Browser.reAssignUrlToBrowserInstance(u, new URL(urlCorrect)), pr, this);
        }
    }
}
