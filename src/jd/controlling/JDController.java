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

package jd.controlling;

import java.io.File;
import java.util.ArrayList;
import java.util.logging.Logger;

import jd.controlling.linkcrawler.CrawledLink;
import jd.controlling.linkcrawler.LinkCrawler;
import jd.gui.UserIO;
import jd.http.Browser;
import jd.nutils.io.JDIO;
import jd.plugins.DownloadLink;
import jd.plugins.PluginsC;

import org.jdownloader.container.D;
import org.jdownloader.controlling.filter.LinkFilterController;

/**
 * Im JDController wird das ganze App gesteuert. Events werden deligiert.
 * 
 * @author JD-Team/astaldo
 */
public class JDController {

    public static JDController getInstance() {
        return INSTANCE;
    }

    /**
     * Der Logger
     */
    private static final Logger LOGGER       = JDLogger.getLogger();

    /**
     * Der Download Watchdog verwaltet die Downloads
     */

    private static JDController INSTANCE     = new JDController();

    private static final Object SHUTDOWNLOCK = new Object();

    private String callService(final String service, final String key) throws Exception {
        LOGGER.finer("Call " + service);
        final Browser br = new Browser();
        br.postPage(service, "jd=1&srcType=plain&data=" + key);
        LOGGER.info("Call re: " + br.toString());
        if (!br.getHttpConnection().isOK() || !br.containsHTML("<rc>")) {
            return null;
        } else {
            final String dlcKey = br.getRegex("<rc>(.*?)</rc>").getMatch(0);
            if (dlcKey.trim().length() < 80) return null;
            return dlcKey;
        }
    }

    public String encryptDLC(PluginsC plg, String xml) {
        if (xml == null || plg == null) return null;
        final String[] encrypt = plg.encrypt(xml);
        if (encrypt == null) {
            LOGGER.severe("Container Encryption failed.");
            return null;
        }
        final String key = encrypt[1];
        xml = encrypt[0];
        final String service = "http://service.jdownloader.org/dlcrypt/service.php";
        try {
            final String dlcKey = callService(service, key);
            if (dlcKey == null) return null;
            return xml + dlcKey;
        } catch (final Exception e) {
            JDLogger.exception(e);
        }
        return null;
    }

    public ArrayList<DownloadLink> getContainerLinks(final File file) {
        LinkCrawler lc = new LinkCrawler();
        lc.setFilter(LinkFilterController.getInstance());
        lc.crawl("file://" + file.getAbsolutePath());
        lc.waitForCrawling();
        ArrayList<DownloadLink> ret = new ArrayList<DownloadLink>();
        for (CrawledLink link : lc.getCrawledLinks()) {
            if (link.getDownloadLink() == null) continue;
            ret.add(link.getDownloadLink());
        }
        return ret;
    }

    /**
     * Saves a list of given links in a DLC.
     * 
     * @param file
     *            Path the DLC file
     * @param links
     *            The links ehich should saved
     */
    public void saveDLC(File file, final ArrayList<DownloadLink> links) {
        if (!file.getAbsolutePath().endsWith("dlc")) {
            file = new File(file.getAbsolutePath() + ".dlc");
        }
        String xml = null;
        PluginsC plg = null;

        xml = new D().createContainerString(links);

        if (xml != null) {
            final String cipher = encryptDLC(plg, xml);
            if (cipher != null) {
                JDIO.writeLocalFile(file, cipher);
                return;
            }
        }
        LOGGER.severe("Container creation failed");
        UserIO.getInstance().requestMessageDialog("Container encryption failed");
    }

}