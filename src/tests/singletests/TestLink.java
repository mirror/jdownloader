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

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;

import jd.DecryptPluginWrapper;
import jd.HostPluginWrapper;
import jd.controlling.ProgressController;
import jd.gui.swing.jdgui.userio.UserIOGui;
import jd.http.Browser;
import jd.nutils.encoding.Encoding;
import jd.parser.html.HTMLParser;
import jd.plugins.CryptedLink;
import jd.plugins.DownloadLink;
import jd.plugins.PluginForDecrypt;

import org.junit.Before;
import org.junit.Test;

import tests.utils.TestUtils;

public class TestLink {

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
    public void getTestLinks() {

        String d = UserIOGui.getInstance().requestInputDialog(0, "Please enter Domain. e.g. rapidshare.de", null, null, null, null, null);

        Browser br = new Browser();

        try {
            br.setDebug(true);
            br.getPage("http://www.google.de/search?as_q=&num=250&as_qdr=m&as_epq=intext%3A" + Encoding.urlEncode(d));

            String source = br.toString().replace("<em>", "").replace("</em>", "");

            try {
                source = Encoding.htmlDecode(source);
            } catch (Exception e) {

            }

            String[] links = HTMLParser.getHttpLinks(source, null);

            for (String l : links) {
                try {
                    if (l.toLowerCase().contains(d.toLowerCase()) && new URL(l).getHost().toLowerCase().contains(d.toLowerCase())) {
                        for (HostPluginWrapper pw : HostPluginWrapper.getHostWrapper()) {

                            if (pw.canHandle(l)) {
                                // TestUtils.log(Encoding.urlDecode(l, true));
                                DownloadLink dl = new DownloadLink(pw.getNewPluginInstance(), null, pw.getHost(), Encoding.urlDecode(l, true), true);
                                dl.isAvailable();
                                if (dl.isAvailable()) {
                                    System.out.println("Hoster: " + dl.getDownloadURL() + " : " + new File(dl.getFileOutput()).getName() + " : " + dl.getDownloadSize() + " Bytes");
                                }

                            }
                        }

                        for (DecryptPluginWrapper pw : DecryptPluginWrapper.getDecryptWrapper()) {

                            if (pw.canHandle(l)) {
                                PluginForDecrypt plg = pw.getNewPluginInstance();

                                CryptedLink[] dd = plg.getDecryptableLinks(l);

                                if (dd != null && dd.length > 0) {
                                    ArrayList<DownloadLink> a = plg.decryptIt(dd[0], new ProgressController("test", 10, null));
                                    if (a != null && a.size() > 0) {
                                        System.out.println("Crypter: " + dd[0] + " : Files:" + a.size());
                                    }
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
}
