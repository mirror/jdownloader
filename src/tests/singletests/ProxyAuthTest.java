package tests.singletests;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.fail;

import java.net.InetSocketAddress;
import java.net.Proxy;

import jd.http.Browser;
import jd.http.JDProxy;
import jd.utils.JDUtilities;

import org.junit.Before;
import org.junit.Test;

import tests.utils.TestUtils;

public class ProxyAuthTest {

    @Before
    public void setUp() throws Exception {
        TestUtils.initJD();
    }

    @Test
    public void proxyAuthTest() {
        Browser.init();
        TestUtils.log("init Proxy");

        JDProxy pr = new JDProxy(Proxy.Type.HTTP, TestUtils.getStringProperty("proxy_ip"), TestUtils.getIntegerProperty("proxy_port"));
        String user = TestUtils.getStringProperty("proxy_user");
        String pass = TestUtils.getStringProperty("proxy_pass");

        if (user != null && user.trim().length() > 0) {
            pr.setUser(user);
        }
        if (pass != null && pass.trim().length() > 0) {
            pr.setPass(pass);
        }
        Browser.setGlobalProxy(pr);

        Browser br = new Browser();
        br.setConnectTimeout(10000);
        br.setReadTimeout(10000);
        try {

            String ip = JDUtilities.getIPAddress(br);
            InetSocketAddress proxyadress = new InetSocketAddress(pr.getHost(), pr.getPort());
            String proxyip = proxyadress.getAddress().getHostAddress();
         
                assertFalse("Coult not connect to proxy", ip.equals("offline"));
            
            if (!ip.equals(proxyip)) {
                fail("Request did not use the proxy");
            }
        } catch (Exception e) {
            fail("proxy error: " + e.getLocalizedMessage());

        }
        if (br.getHttpConnection().isOK()) {
            TestUtils.log("proxy ok");
        } else {
            TestUtils.log("proxy  FAILED");
            fail("proxy error: " + br.getHttpConnection().toString());
        }
    }
}
