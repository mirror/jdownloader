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
import jd.plugins.PluginForHost;
import jd.utils.JDUtilities;

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

            String source = Encoding.htmlDecode(br.toString().replace("<em>", "").replace("</em>", ""));

            String[] links = HTMLParser.getHttpLinks(source, null);

            for (String l : links) {
                try {
                    if (l.toLowerCase().contains(d.toLowerCase()) && new URL(l).getHost().toLowerCase().contains(d.toLowerCase())) {
                        for (HostPluginWrapper pw : JDUtilities.getPluginsForHost()) {

                            if (pw.canHandle(l)) {
TestUtils.log( Encoding.urlDecode(l, true));
                                DownloadLink dl = new DownloadLink((PluginForHost) pw.getNewPluginInstance(), null, pw.getHost(), Encoding.urlDecode(l, true), true);
                                dl.isAvailable();
                                if (dl.isAvailable()) {
                                    System.out.println("Hoster: " + dl.getDownloadURL() + " : " + new File(dl.getFileOutput()).getName() + " : " + dl.getDownloadSize() + " Bytes");
                                }

                            }
                        }

                        for (DecryptPluginWrapper pw : DecryptPluginWrapper.getDecryptWrapper()) {

                            if (pw.canHandle(l)) {
                                PluginForDecrypt plg = (PluginForDecrypt) pw.getNewPluginInstance();

                                CryptedLink[] dd = plg.getDecryptableLinks(l);

                                if (dd != null && dd.length > 0) {
                                    ArrayList<DownloadLink> a = plg.decryptIt(dd[0], new ProgressController("test", 10));
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
