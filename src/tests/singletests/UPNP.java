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

package tests.singletests;

import java.net.InetAddress;

import jd.controlling.reconnect.RouterUtils;
import jd.nrouter.UPNPRouter;

import org.junit.Test;

public class UPNP {

    @Test
    public void ipcheck() {
        InetAddress address = RouterUtils.getAddress(true);

        UPNPRouter upnp = new UPNPRouter(address);
        if (upnp.isUPNPDevice()) {
            System.out.println("UPNP Router at " + address);

            String oldip = upnp.getExternalIPAddress();
            System.out.println(upnp + " external ip " + oldip);

            System.out.println("Refresh IP Request: ");
            System.out.println(upnp.getRefreshIPRequest());
            upnp.refreshIP();

            String ip;
            int i = 0;
            while (true) {
                try {
                    Thread.sleep(500);
                } catch (InterruptedException e) {

                    e.printStackTrace();
                }
                i += 500;
                System.out.println(i + " ms");
                ip = upnp.getExternalIPAddress();
                if (!ip.equals(oldip)) {
                    if (ip.equals("0.0.0.0")) {
                        oldip = ip;
                        System.out.println("OFFLINE");
                    } else {
                        System.out.println(upnp + "NEW external ip " + ip);
                        break;
                    }
                }
            }
        }
    }

}