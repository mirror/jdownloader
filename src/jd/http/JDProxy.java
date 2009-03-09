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

import jd.parser.Regex;

public class JDProxy extends Proxy {
    private String user = null;

    public String getUser() {
        return user;
    }

    public void setUser(String user) {
        this.user = user;
    }

    public String getPass() {
        return pass;
    }

    public void setPass(String pass) {
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

    public JDProxy(java.net.Proxy.Type type, SocketAddress sa) {
        super(type, sa);

        this.host = new Regex(sa.toString(), "(.*)\\/").getMatch(0);

    }

    public JDProxy(java.net.Proxy.Type type, String host, int port) {
        super(type, new InetSocketAddress(host, port));

        this.port = port;
        this.host = host;

    }

    public JDProxy(String hostAndPort) {
        super(JDProxy.Type.HTTP, new InetSocketAddress(hostAndPort.split("\\:")[0], Integer.parseInt(hostAndPort.split("\\:")[1])));
        port = Integer.parseInt(hostAndPort.split("\\:")[1]);
        this.host = hostAndPort.split("\\:")[0];

    }

    public JDProxy(String host, int port, String user, String pass) {
        super(JDProxy.Type.HTTP, new InetSocketAddress(host, port));
        this.port = port;
        this.user = user;
        this.pass = pass;
        this.host = host;

    }

    private JDProxy() {
        super(JDProxy.Type.HTTP, new InetSocketAddress(80));
        // TODO Auto-generated constructor stub

        this.direct = true;
    }

    public Type type() {
        if (direct) return Type.DIRECT;
        return super.type();
    }

    public String getHost() {
        return host;
    }

    public String toString() {
        return "JDProxy: " + super.toString();
    }
}
