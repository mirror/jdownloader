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

import java.net.InetSocketAddress;
import java.net.Proxy;
import java.net.SocketAddress;

import jd.controlling.JDLogger;
import jd.parser.Regex;

public class JDProxy extends Proxy {
    private String user = null;

    public String getUser() {
        return user;
    }

    public void setUser(final String user) {
        this.user = user;
    }

    public String getPass() {
        return pass;
    }

    public void setPass(final String pass) {
        this.pass = pass;
    }

    private String pass = null;
    private int port = 80;
    private String host;
    private boolean direct = false;
    public static JDProxy NO_PROXY = new JDProxy();

    public int getPort() {
        return port;
    }

    public JDProxy(final java.net.Proxy.Type type, final SocketAddress sa) {
        super(type, sa);
        this.host = new Regex(sa.toString(), "(.*)\\/").getMatch(0);
    }

    public JDProxy(final java.net.Proxy.Type type, final String host, final int port) {
        super(type, new InetSocketAddress(getInfo(host, "" + port)[0], port));
        this.port = port;
        this.host = getInfo(host, "" + port)[0];
    }

    private static String[] getInfo(final String host, final String port) {
        final String[] info = new String[2];
        if (host == null) return info;
        final String tmphost = host.replaceFirst("http://", "").replaceFirst("https://", "");
        String tmpport = new Regex(host, ".*?:(\\d+)").getMatch(0);
        if (tmpport != null) {
            info[1] = "" + tmpport;
        } else {
            if (port != null) {
                tmpport = new Regex(port, "(\\d+)").getMatch(0);
            }
            if (tmpport != null) {
                info[1] = "" + tmpport;
            } else {
                JDLogger.getLogger().severe("No proxyport defined, using default 8080");
                info[1] = "8080";
            }
        }
        info[0] = new Regex(tmphost, "(.*?)(:|/|$)").getMatch(0);
        return info;
    }

    public JDProxy(final String hostAndPort) {
        super(JDProxy.Type.HTTP, new InetSocketAddress(getInfo(hostAndPort, null)[0], Integer.parseInt(getInfo(hostAndPort, null)[1])));
        port = Integer.parseInt(getInfo(hostAndPort, null)[1]);
        host = getInfo(hostAndPort, null)[0];
    }

    public JDProxy(final String host, final int port, final String user, final String pass) {
        super(JDProxy.Type.HTTP, new InetSocketAddress(getInfo(host, "" + port)[0], port));
        this.port = port;
        this.user = user;
        this.pass = pass;
        this.host = getInfo(host, "" + port)[0];
    }

    private JDProxy() {
        super(JDProxy.Type.HTTP, new InetSocketAddress(80));
        this.direct = true;
    }

    public Type type() {
        return (direct) ? Type.DIRECT : super.type();
    }

    public String getHost() {
        return host;
    }

    public String toString() {
        return "JDProxy: " + super.toString();
    }
}
