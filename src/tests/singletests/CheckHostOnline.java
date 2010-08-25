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

import java.io.IOException;
import java.util.ArrayList;

import jd.HostPluginWrapper;
import jd.http.Browser;

import org.junit.Before;
import org.junit.Test;

import tests.utils.TestUtils;

public class CheckHostOnline {

    @Before
    public void setUp() throws Exception {
        TestUtils.mainInit();
        TestUtils.initGUI();
        TestUtils.initDecrypter();
        TestUtils.initContainer();
        TestUtils.initHosts();
        TestUtils.finishInit();
    }

    @Test
    public void check() throws IOException {
        Browser br = new Browser();

        int ok = 0;
        ArrayList<HostPluginWrapper> failed = new ArrayList<HostPluginWrapper>();

        for (HostPluginWrapper pw : HostPluginWrapper.getHostWrapper()) {
            try {
                br.getPage(pw.getPlugin().getAGBLink());
                ok++;
            } catch (Exception e) {
                failed.add(pw);
            }
        }

        System.out.println("OK: " + ok);
        System.out.println("Failed: " + failed.size());
        for (HostPluginWrapper pw : failed) {
            System.out.println("\t" + pw.getHost() + "\t\t" + pw.getPlugin().getAGBLink());
        }
    }
}