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

import jd.http.Browser;

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

        ArrayList<String[]> table = jd.http.IPCheck.IP_CHECK_SERVICES;
        System.out.println("IPCHECKS:" + table.size());
        // for (int i = 0; i < 20; i++) {
        for (String[] entry : table) {
            Browser br = new Browser();
            // br.setDebug(true);
            try {

                br.getPage(entry[0]);
                String lip = br.getRegex(entry[1]).getMatch(0).trim();
                System.out.println(entry[0] + " reports: " + lip);
                if (ip == null) ip = lip;

                if (!ip.equals(lip)) {
                    System.out.println("regex for " + entry[0] + " may be invalid");
                    assertTrue(false);
                }
            } catch (Exception e) {
                // TODO Auto-generated catch block
                // e.printStackTrace();
                System.out.println(entry[0] + " broken");
                assertTrue(false);
            }

        }
        // }

    }
}
