//    jDownloader - Downloadmanager
//    Copyright (C) 2008  JD-Team support@jdownloader.org
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

import java.util.ArrayList;

import jd.CPluginWrapper;
import jd.DecryptPluginWrapper;
import jd.HostPluginWrapper;
import jd.controlling.linkcrawler.CrawledLink;
import jd.controlling.linkcrawler.LinkCrawler;
import jd.nutils.encoding.Encoding;
import jd.parser.html.HTMLParser;
import jd.plugins.DownloadLink;

import org.appwork.utils.Regex;

/**
 * Diese Klasse läuft in einem Thread und verteilt den Inhalt der Zwischenablage
 * an (unter Umständen auch mehrere) Plugins Die gefundenen Treffer werden
 * ausgeschnitten.
 * 
 * @author astaldo
 */
public class DistributeData extends Thread {

    /**
     * Die zu verteilenden Daten
     */
    private String                  data;

    private final ArrayList<String> foundPasswords = new ArrayList<String>();

    /**
     * Erstellt einen neuen Thread mit dem Text, der verteilt werden soll. Die
     * übergebenen Daten werden durch einen URLDecoder geschickt.
     * 
     * @param data
     *            Daten, die verteilt werden sollen
     */
    @Deprecated
    public DistributeData(final String data) {
        super("JD-DistributeData");
        this.data = data;
    }

    /* keep for comp. issues in other projects */
    @Deprecated
    public DistributeData(final String data, final boolean hideGrabber) {
        this(data);
    }

    /* keep for comp. issues in other projects */
    @Deprecated
    public DistributeData setFilterNormalHTTP(final boolean b) {
        return this;
    }

    @Deprecated
    public static boolean hasContainerPluginFor(final String tmp) {
        for (CPluginWrapper cDecrypt : CPluginWrapper.getCWrapper()) {
            if (cDecrypt.canHandle(tmp)) return true;
        }
        return false;
    }

    @Deprecated
    public static boolean hasPluginFor(final String tmp, final boolean filterNormalHTTP) {
        if (DecryptPluginWrapper.getDecryptWrapper() != null) {
            String data = tmp;
            data = data.replaceAll("jd://", "http://");
            for (final DecryptPluginWrapper pDecrypt : DecryptPluginWrapper.getDecryptWrapper()) {
                if (pDecrypt.canHandle(data)) return true;
            }
            for (final HostPluginWrapper pHost : HostPluginWrapper.getHostWrapper()) {
                if (pHost.canHandle(data)) return true;
            }
            data = Encoding.urlDecode(data, true);
            for (final DecryptPluginWrapper pDecrypt : DecryptPluginWrapper.getDecryptWrapper()) {
                if (pDecrypt.canHandle(data)) return true;
            }
            for (final HostPluginWrapper pHost : HostPluginWrapper.getHostWrapper()) {
                if (pHost.canHandle(data)) return true;
            }
            if (!filterNormalHTTP) {
                data = data.replaceAll("http://", "httpviajd://");
                data = data.replaceAll("https://", "httpsviajd://");
                for (final DecryptPluginWrapper pDecrypt : DecryptPluginWrapper.getDecryptWrapper()) {
                    if (pDecrypt.canHandle(data)) return true;
                }
                for (final HostPluginWrapper pHost : HostPluginWrapper.getHostWrapper()) {
                    if (pHost.canHandle(data)) return true;
                }
            }
        }
        return false;
    }

    @Deprecated
    public ArrayList<DownloadLink> findLinks() {
        foundPasswords.addAll(HTMLParser.findPasswords(data));
        LinkCrawler lf = new LinkCrawler();
        lf.crawlNormal(data);
        lf.waitForCrawling();
        ArrayList<DownloadLink> ret = new ArrayList<DownloadLink>(lf.getCrawledLinks().size());
        for (final CrawledLink link : lf.getCrawledLinks()) {
            DownloadLink dl = link.getDownloadLink();
            if (dl != null) {
                dl.addSourcePluginPasswordList(foundPasswords);
                ret.add(dl);
            }
        }
        return ret;
    }

    @Override
    public void run() {
        /*
         * check if there are any links (we need at least a domain and
         * protocoll)
         */
        if (data == null || data.length() == 0 || (!new Regex(data, "//.*?\\.").matches() && !new Regex(data, "jdlist://").matches())) return;
        ArrayList<DownloadLink> links = findLinks();
        if (links.size() > 0) LinkGrabberController.getInstance().addLinks(links, false, false);
    }

}