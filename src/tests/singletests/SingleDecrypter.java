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
import static org.junit.Assert.fail;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map.Entry;

import jd.DecryptPluginWrapper;
import jd.controlling.ProgressController;
import jd.gui.UserIO;
import jd.gui.swing.jdgui.userio.UserIOGui;
import jd.plugins.CryptedLink;
import jd.plugins.DownloadLink;
import jd.plugins.PluginForDecrypt;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import tests.utils.TestUtils;

public class SingleDecrypter {
    private HashMap<String, String> links;

    /**
     * change this to the host that is tested, test links are taken from
     * http://jdownloader
     * .net:8081/knowledge/wiki/development/intern/testlinks/decrypter/DECRYPT
     */
    private static final String DECRYPT = "relink.us";

    @Before
    public void setUp() {
        TestUtils.mainInit();
        // TestUtils.initGUI();
        UserIO.setInstance(UserIOGui.getInstance());
        TestUtils.initDecrypter();
        TestUtils.initContainer();
        TestUtils.initHosts();
        TestUtils.finishInit();

        links = TestUtils.getDecrypterLinks(DECRYPT);
        TestUtils.log("Found " + links.size() + " test link(s) for decrypter " + DECRYPT);
    }

    @Test
    public void decrypt() {
        for (Entry<String, String> next : links.entrySet()) {
            TestUtils.log("Testing link: " + next.getValue());
            if (next.getKey().equalsIgnoreCase("NORMAL_DECRYPTERLINK_1")) {
                decryptURL(next.getValue());
            } else if (next.getKey().equalsIgnoreCase("NORMAL_DECRYPTERLINK_2")) {
                decryptURL(next.getValue());
            } else if (next.getKey().equalsIgnoreCase("NORMAL_DECRYPTERLINK_3")) {
                decryptURL(next.getValue());
            } else if (next.getKey().equalsIgnoreCase("NORMAL_DECRYPTERLINK_4")) {
                decryptURL(next.getValue());
            } else if (next.getKey().equalsIgnoreCase("NORMAL_DECRYPTERLINK_5")) {
                decryptURL(next.getValue());
            } else if (next.getKey().startsWith("PASSWORD_PROTECTED_1:")) {
                decryptPWURL(next.getValue(), next.getKey().substring("PASSWORD_PROTECTED_1:".length()));
            } else {
                System.out.println("No Test for " + next.getKey());
            }
        }
    }

    private void decryptPWURL(String url, String pw) {
        boolean found = false;
        System.out.println("Enter password: " + pw);
        for (DecryptPluginWrapper pd : DecryptPluginWrapper.getDecryptWrapper()) {
            if (pd.canHandle(url)) {
                found = true;
                PluginForDecrypt plg = pd.getNewPluginInstance();

                CryptedLink[] d = plg.getDecryptableLinks(url);

                try {
                    ProgressController pc;
                    d[0].setProgressController(pc = new ProgressController("test", 10, null));
                    ArrayList<DownloadLink> a = plg.decryptIt(d[0], pc);

                    if (a.size() > 1 || (a.size() == 1 && a.get(0).getBrowserUrl() != null))
                        assertTrue(true);
                    else {
                        TestUtils.log("Error with url: " + url);
                        assertTrue(false);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
                break;
            }
        }
        if (!found) {
            TestUtils.log("Url not found: " + url);
            fail();
        }
    }

    private void decryptURL(String url) {
        boolean found = false;
        for (DecryptPluginWrapper pd : DecryptPluginWrapper.getDecryptWrapper()) {
            if (pd.canHandle(url)) {
                found = true;
                PluginForDecrypt plg = pd.getNewPluginInstance();

                CryptedLink[] d = plg.getDecryptableLinks(url);

                try {
                    ArrayList<DownloadLink> a = plg.decryptIt(d[0], new ProgressController("test", 10, null));

                    if (a.size() > 1 || (a.size() == 1 && a.get(0).getBrowserUrl() != null)) {
                        assertTrue(true);
                    } else {
                        fail(TestUtils.log("Error with url: " + url));
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
                break;
            }
        }
        if (!found) {
            TestUtils.log("Url not found: " + url);
            fail();
        }
    }

    @After
    public void tearDown() throws Exception {
        // JDUtilities.getController().exit();
    }
}