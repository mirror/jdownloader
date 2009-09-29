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

package jd.nrouter;

import java.net.InetAddress;

import jd.controlling.reconnect.Reconnecter;

public class Router {
    private InetAddress address;

    public InetAddress getAddress() {
        return address;
    }

    /**
     * 
     * @param address
     *            the router's IP
     */
    public Router(InetAddress address) {
        this.address = address;
    }

    /**
     * Return the external IP Address
     * 
     * @return
     */
    public String getExternalIPAddress() {
        return IPCheck.getIPAddress();

    }

    /**
     * Performs an reconnect delegating the request to Reconnecter class
     */
    public boolean refreshIP() {
        return Reconnecter.doReconnect();

    }

}
