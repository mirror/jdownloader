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

import static org.junit.Assert.fail;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map.Entry;

import jd.HostPluginWrapper;
import jd.controlling.DistributeData;
import jd.controlling.SingleDownloadController;
import jd.gui.UserIO;
import jd.gui.swing.jdgui.userio.UserIOGui;
import jd.http.Browser;
import jd.nutils.encoding.Encoding;
import jd.parser.html.HTMLParser;
import jd.plugins.DownloadLink;
import jd.plugins.LinkStatus;
import jd.plugins.DownloadLink.AvailableStatus;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import tests.utils.TestUtils;

public class SingleHoster {
    private HashMap<String, String> links;

    @Before
    public void setUp() {

        TestUtils.mainInit();
        // TestUtils.initGUI();

        UserIO.setInstance(UserIOGui.getInstance());
        TestUtils.initDecrypter();
        TestUtils.initContainer();
        TestUtils.initHosts();
        TestUtils.finishInit();

        String d = TestUtils.getStringProperty("HOSTERDOMAIN");
        // UserIO.setCountdownTime(2);
        // String d = UserIOGui.getInstance().requestInputDialog(0,
        // "Please enter Hoster Domain. e.g. rapidshare.de", null, def, null,
        // null, null);
        // UserIO.setCountdownTime(20);
        links = TestUtils.getHosterLinks(d);
        // links = new HashMap<String, String>();
        if (!links.containsKey(TestUtils.HOSTER_LINKTYPE_NORMAL + 1)) {

            Browser br = new Browser();

            try {
                br.setDebug(true);
                br.getPage("http://www.google.de/search?as_q=&num=250&as_qdr=m&as_epq=intext%3A" + Encoding.urlEncode(d));

                String source = Encoding.htmlDecode(br.toString().replace("<em>", "").replace("</em>", ""));

                String[] links = HTMLParser.getHttpLinks(source, null);
                int ii = 1;
                main: for (String l : links) {
                    try {
                        if (l.toLowerCase().contains(d.toLowerCase()) && new URL(l).getHost().toLowerCase().contains(d.toLowerCase())) {
                            for (HostPluginWrapper pw : HostPluginWrapper.getHostWrapper()) {

                                if (pw.canHandle(l)) {

                                    DownloadLink dl = new DownloadLink(pw.getNewPluginInstance(), null, pw.getHost(), Encoding.urlDecode(l, true), true);
                                    dl.isAvailable();
                                    if (dl.isAvailable()) {
                                        System.out.println(dl.getDownloadURL());
                                        this.links.put(TestUtils.HOSTER_LINKTYPE_NORMAL + ii++, dl.getDownloadURL());
                                        if (ii > 4) break main;

                                    }

                                }
                            }

                        }
                    } catch (Exception e) {
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            }

        }
        TestUtils.log("Found " + links.size() + " test link(s) for host " + d);
    }

    @Test
    public void findDownloadLink() {
        try {
            for (Entry<String, String> next : links.entrySet()) {
                getDownloadLink(next.getValue());
            }
        } catch (Exception e) {
            fail(e.getMessage());
        }

    }

    @Test
    public void availableCheck() {

        for (Entry<String, String> next : links.entrySet()) {
            try {
                DownloadLink dlink = getDownloadLink(next.getValue());
                TestUtils.log("Testing link: " + next.getValue());
                if (next.getKey().equalsIgnoreCase(TestUtils.HOSTER_LINKTYPE_NORMAL + 1)) {
                    if (dlink.getAvailableStatus() != AvailableStatus.TRUE) {
                        fail(TestUtils.log(next.getKey() + " Downloadlink " + next.getValue() + " is marked as NOT AVAILABLE"));
                    }
                } else if (next.getKey().equalsIgnoreCase(TestUtils.HOSTER_LINKTYPE_NORMAL + 2)) {
                    if (dlink.getAvailableStatus() != AvailableStatus.TRUE) {
                        fail(TestUtils.log(next.getKey() + " Downloadlink " + next.getValue() + " is marked as NOT AVAILABLE"));
                    }
                } else if (next.getKey().equalsIgnoreCase(TestUtils.HOSTER_LINKTYPE_NORMAL + 3)) {
                    if (dlink.getAvailableStatus() != AvailableStatus.TRUE) {
                        fail(TestUtils.log(next.getKey() + " Downloadlink " + next.getValue() + " is marked as NOT AVAILABLE"));
                    }
                } else if (next.getKey().equalsIgnoreCase(TestUtils.HOSTER_LINKTYPE_NORMAL + 4)) {
                    if (dlink.getAvailableStatus() != AvailableStatus.TRUE) {
                        fail(TestUtils.log(next.getKey() + " Downloadlink " + next.getValue() + " is marked as NOT AVAILABLE"));
                    }
                } else if (next.getKey().equalsIgnoreCase(TestUtils.HOSTER_LINKTYPE_NORMAL + 5)) {
                    if (dlink.getAvailableStatus() != AvailableStatus.TRUE) {
                        fail(TestUtils.log(next.getKey() + " Downloadlink " + next.getValue() + " is marked as NOT AVAILABLE"));
                    }
                } else if (next.getKey().equalsIgnoreCase(TestUtils.HOSTER_LINKTYPE_FNF + 1)) {
                    if (dlink.getAvailableStatus() != AvailableStatus.FALSE) {
                        fail(TestUtils.log(next.getKey() + " Downloadlink " + next.getValue() + " is marked as " + dlink.getAvailableStatus()));
                    }
                } else if (next.getKey().equalsIgnoreCase(TestUtils.HOSTER_LINKTYPE_ABUSED + 1)) {
                    if (dlink.getAvailableStatus() != AvailableStatus.FALSE) {
                        fail(TestUtils.log(next.getKey() + " Downloadlink " + next.getValue() + " is marked as " + dlink.getAvailableStatus()));
                    }
                } else if (next.getKey().equalsIgnoreCase(TestUtils.HOSTER_LINKTYPE_ERROR_TEMP + 1)) {
                    if (dlink.getAvailableStatus() != AvailableStatus.TRUE) {
                        fail(TestUtils.log(next.getKey() + " Downloadlink " + next.getValue() + " is marked as " + dlink.getAvailableStatus()));
                    }
                }
            } catch (TestException e) {
                fail(e.getMessage());
            }
        }

    }

    @Test
    public void downloadFree() {
        for (Entry<String, String> next : links.entrySet()) {
            TestUtils.log(next.getValue());
            if (next.getKey().equalsIgnoreCase(TestUtils.HOSTER_LINKTYPE_NORMAL + 1)) {
                download(next.getValue());
            }
            if (next.getKey().equalsIgnoreCase(TestUtils.HOSTER_LINKTYPE_NORMAL + 2)) {
                download(next.getValue());
            }
            if (next.getKey().equalsIgnoreCase(TestUtils.HOSTER_LINKTYPE_NORMAL + 3)) {
                download(next.getValue());
            }
            if (next.getKey().equalsIgnoreCase(TestUtils.HOSTER_LINKTYPE_NORMAL + 4)) {
                download(next.getValue());
            }
            if (next.getKey().equalsIgnoreCase(TestUtils.HOSTER_LINKTYPE_NORMAL + 5)) {
                download(next.getValue());
            }
        }
    }

    private void download(String url) {

        try {
            DownloadLink dlink = getDownloadLink(url);
            dlink.getPlugin().setAGBChecked(true);
            if (dlink.getAvailableStatus() != AvailableStatus.TRUE) {
                fail(TestUtils.log("Downloadlink " + url + " is marked as NOT AVAILABLE"));
            }
            SingleDownloadController download = new SingleDownloadController(dlink, null);

            dlink.getLinkStatus().setActive(true);
            if (new File(dlink.getFileOutput()).delete()) {
                TestUtils.log("Removed local testfile: " + dlink.getFileOutput());
            }

            download.start();
            long waittimeuntilstart = 60 * 3 * 1000;

            long start = System.currentTimeMillis();
            while (true) {

                Thread.sleep(1000);
                if (!dlink.getLinkStatus().hasStatus(LinkStatus.PLUGIN_IN_PROGRESS)) {
                    TestUtils.log("Download did not start correctly. PLugin is not active any more");
                    return;
                }
                if (dlink.getLinkStatus().getStatusString().trim().length() == 0) {
                    fail(TestUtils.log("Download did not start correctly. Statusstring is empty"));
                    download.abortDownload();
                    return;
                }
                System.out.println(dlink.getLinkStatus().getStatusString());

                if (dlink.getDownloadCurrent() > 0 && !dlink.getLinkStatus().getStatusString().startsWith("Connecting")) {
                    TestUtils.log("Download started correct");
                    download.abortDownload();
                    return;
                }
                if (start + waittimeuntilstart < System.currentTimeMillis()) {

                    if (dlink.getDownloadCurrent() <= 0) {
                        fail(TestUtils.log("Download did not start correctly"));
                        download.abortDownload();
                        return;
                    } else {
                        TestUtils.log("Download started correct");
                        download.abortDownload();
                        return;
                    }
                }

            }

        } catch (TestException e) {
            fail(e.getMessage());
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

    }

    private DownloadLink getDownloadLink(String url) throws TestException {

        DistributeData distributeData = new DistributeData(url);
        ArrayList<DownloadLink> links = distributeData.findLinks();
        if (links == null || links.size() == 0) {
            throw new TestException("No plugin found for " + url);

        } else {
            if (!url.toLowerCase().contains(links.get(0).getPlugin().getHost())) {
                throw new TestException("Wrong plugin found for " + url + " (" + links.get(0).getPlugin().getHost() + ")");
            } else {
                return links.get(0);
            }
        }

    }

    @After
    public void tearDown() throws Exception {
        // JDUtilities.getController().exit();
    }
}