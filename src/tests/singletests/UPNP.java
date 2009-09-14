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

import java.util.Date;

import org.cybergarage.net.HostInterface;
import org.cybergarage.upnp.ControlPoint;
import org.cybergarage.upnp.DeviceList;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class UPNP {

    @Before
    public void setUp() {

    }

    @Test
    public void ipcheck() {
        // InetAddress address = RouterUtils.getAddress(true);

        int nHostAddrs = HostInterface.getNHostAddresses();
        for (int n = 0; n < nHostAddrs; n++) {
            System.out.println(HostInterface.getHostAddress(n));
        }

        final ControlPoint c = new ControlPoint();
        for (int i = 0; i < 1000000; i++) {

      
            c.start();

            try {
                Thread.sleep(2000);
            } catch (InterruptedException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }

            DeviceList list = c.getDeviceList();
            System.out.println("Found");
            for (int n = 0; n < list.size(); n++) {

                System.out.println(new Date());
                System.out.println(list.getDevice(n).getFriendlyName());
                System.out.println(list.getDevice(n).getLocation());
            }

        }

    }

    @After
    public void tearDown() throws Exception {

    }
}