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

import java.io.IOException;
import java.net.URL;

import jd.http.Browser;
import jd.nutils.Threader;
import jd.nutils.jobber.JDRunnable;

import org.junit.Before;
import org.junit.Test;

import tests.utils.TestUtils;

public class ConnectionControl {

    @Before
    public void setUp() throws Exception {
        TestUtils.initJD();
    }

    /**
     * Testes the request time limitation globaly. different browserinstances in
     * different threads call the same host. there should be a requestgap fo
     * 1000 ms between each nrequest. Fails if test passes too fast
     * 
     * @throws Exception
     */
    @Test
    public void testRequestIntervalLimitGlobal() throws Exception {

        int interval = 1000;
        int requests = 30;
        int threads = 3;
        URL url = new URL("http://jdownloader.org/home/index");
        TestUtils.log("Start test");
        TestUtils.log("Interval " + interval);
        TestUtils.log("Requests " + requests + " in " + threads + " threads");
        TestUtils.log("url " + url);

        Browser.setRequestIntervalLimitGlobal(url.getHost(), interval);
        long start = System.currentTimeMillis();
        TestUtils.log("StartTime" + start);
        TestUtils.log("Test should take at least " + (requests * interval) + "msec");
        Threader th = new Threader();
        for (int i = 0; i < threads; i++) {
            th.add(createThread("http://jdownloader.org/home/index", requests / threads, new Browser()));

        }
        th.startAndWait();
        TestUtils.log("Endtime: " + System.currentTimeMillis());
        long dif = System.currentTimeMillis() - start;
        TestUtils.log("Test took " + dif + " ms");
        assertTrue(((long) requests * interval) < dif + interval);
        // if () { throw new UnitTestException("time error"); }

    }

    /**
     * Browser request time control test. a single browser calls out of
     * different threads the same host. between ech request should be a timegap
     * of 1000s Fails if test passes too fast
     * 
     * @throws Exception
     */
    @Test
    public void testRequestIntervalLimitExclusive() throws Exception {
        Browser br = new Browser();
        int interval = 1000;
        int requests = 30;
        int threads = 3;
        URL url = new URL("http://jdownloader.org/home/index");
        TestUtils.log("Start test");
        TestUtils.log("Interval " + interval);
        TestUtils.log("Requests " + requests + " in " + threads + " threads");
        TestUtils.log("url " + url);

        br.setRequestIntervalLimit(url.getHost(), interval);

        long start = System.currentTimeMillis();
        TestUtils.log("StartTime" + start);
        TestUtils.log("Test should take at least " + (requests * interval) + "msec");
        Threader th = new Threader();
        for (int i = 0; i < threads; i++) {

            th.add(createThread("http://jdownloader.org/home/index", requests / threads, br));

        }
        th.startAndWait();
        TestUtils.log("Endtime: " + System.currentTimeMillis());
        long dif = System.currentTimeMillis() - start;
        TestUtils.log("Test took " + dif + " ms");
        assertTrue(((long) requests * interval) < dif + interval);

    }

    private JDRunnable createThread(final String string, final int i, final Browser br) {
        return new JDRunnable() {

            public void go() throws Exception {
                for (int ii = 0; ii < i; ii++) {
                    try {
                        br.getPage(string);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }

        };
    }

}
