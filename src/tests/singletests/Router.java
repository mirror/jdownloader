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

import jd.utils.JDUtilities;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import tests.utils.TestUtils;

public class Router {

    @Before
    public void setUp() {

    }

    @Test
    public void ipcheck() {
        jd.nrouter.RouterInfo router = jd.nrouter.RouterInfo.getInstance();

        InetAddress address = router.getIPFormNetStat();

        TestUtils.log("Router getIPFormNetStat check: " + address);

        address = router.getIPFromRouteCommand();
        TestUtils.log("Router getIPFromRouteCommand check: " + address);
        address = router.getIpFormHostTable();
        TestUtils.log("Router getIpFormHostTable check: " + address);

    }

    @After
    public void tearDown() throws Exception {

    }
}