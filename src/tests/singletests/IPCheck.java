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

import static org.junit.Assert.assertTrue;

import java.util.ArrayList;

import jd.nrouter.IPCheckProvider;

import org.junit.Before;
import org.junit.Test;

public class IPCheck {

    private String ip;

    @Before
    public void setUp() throws Exception {
        // TestUtils.initJD();
    }

    @Test
    public void checkIPTable() {

        ArrayList<IPCheckProvider> table = jd.nrouter.IPCheck.IP_CHECK_SERVICES;
        System.out.println("IPCHECKS:" + table.size());
        for (IPCheckProvider prov : table) {
            try {

                Object lip = prov.getIP();
                String curip = null;
                if (lip instanceof String) {
                    curip = (String) lip;
                    System.out.println(prov.getInfo() + " reported: " + curip);
                    if (ip == null) ip = curip;
                }

                if (curip == null || !ip.equals(curip)) {
                    System.out.println("regex for " + prov.getIP() + " may be invalid");
                    assertTrue(false);
                }
            } catch (Exception e) {
                System.out.println(prov.getIP() + " broken");
                assertTrue(false);
            }
        }

        // for (int i = 0; i < 30; i++) {
        // System.out.println(jd.nrouter.IPCheck.getIPAddress());
        // }

    }
}
