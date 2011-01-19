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

import java.net.InetSocketAddress;

import jd.controlling.reconnect.ipcheck.IPController;
import jd.http.Browser;
import jd.http.HTTPProxy;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import tests.utils.TestUtils;

public class ProxyAuthTest {

    @Test
    public void proxyAuthTest() {

        TestUtils.log("init Proxy");

        final HTTPProxy pr = new HTTPProxy(HTTPProxy.TYPE.HTTP, TestUtils.getStringProperty("proxy_ip"), TestUtils.getIntegerProperty("proxy_port"));
        final String user = TestUtils.getStringProperty("proxy_user");
        final String pass = TestUtils.getStringProperty("proxy_pass");

        if (user != null && user.trim().length() > 0) {
            pr.setUser(user);
        }
        if (pass != null && pass.trim().length() > 0) {
            pr.setPass(pass);
        }
        // Browser.setGlobalProxy(pr);

        try {
            final Browser br = new Browser();
            br.setProxy(pr);
            br.getPage("http://freakshare.net/?language=US");
            final String ip = IPController.getInstance().fetchIP().toString();
            final InetSocketAddress proxyadress = new InetSocketAddress(pr.getHost(), pr.getPort());
            final String proxyip = proxyadress.getAddress().getHostAddress();

            Assert.assertFalse("Coult not connect to proxy", ip.equals("offline"));

            if (ip == null || !ip.equals(proxyip)) {
                Assert.fail("Request did not use the proxy");
            }
        } catch (final Exception e) {
            Assert.fail("proxy error: " + e.getLocalizedMessage());

        }
    }

    @Before
    public void setUp() throws Exception {
        TestUtils.initJD();
    }

}
