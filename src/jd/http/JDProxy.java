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

    public JDProxy(java.net.Proxy.Type type, SocketAddress sa) {
        super(type, sa);
    }

    public JDProxy(java.net.Proxy.Type type, String host, int port) {
        super(type,new InetSocketAddress(host, port));
    
       
    }

    public JDProxy(String host_port) {
        super(JDProxy.Type.HTTP,new InetSocketAddress(host_port.split("\\:")[0], Integer.parseInt(host_port.split("\\:")[1])));
    }

}
