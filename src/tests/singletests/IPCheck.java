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

import java.util.ArrayList;

import jd.controlling.reconnect.IPCheckProvider;

import org.junit.Assert;
import org.junit.Test;

public class IPCheck {

    @Test
    public void checkIPTable() {
        ArrayList<IPCheckProvider> table = jd.controlling.reconnect.IPCheck.IP_CHECK_SERVICES;
        System.out.println("Number of available IPCheckProvider: " + table.size());

        Assert.assertFalse(table.isEmpty());

        String ip = null;
        for (IPCheckProvider prov : table) {

            String curip = prov.getIP().toString();
            System.out.println(prov.getInfo() + ": Reported " + curip);
            if (ip == null) ip = curip;

            if (!ip.equals(curip)) {
                Assert.fail(prov.getInfo() + ": Regex for " + prov.getIP() + " may be invalid!");
            }

        }
    }

}
