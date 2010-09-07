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

import jd.nrouter.RouterUtils;

import org.junit.Test;

import tests.utils.TestUtils;

public class RouterTest {

    @Test
    public void ipcheck() {
        InetAddress address = RouterUtils.getIPFormNetStat();

        TestUtils.log("RouterTest getIPFormNetStat check: " + address);

        address = RouterUtils.getIPFromRouteCommand();
        TestUtils.log("RouterTest getIPFromRouteCommand check: " + address);
        address = RouterUtils.getIpFormHostTable();
        TestUtils.log("RouterTest getIpFormHostTable check: " + address);
    }

}