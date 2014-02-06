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
import java.util.List;

import jd.controlling.linkcrawler.CrawledLink;
import jd.controlling.linkcrawler.LinkCrawler;
import jd.parser.html.HTMLParser;
import jd.plugins.DownloadLink;

/**
 * Diese Klasse läuft in einem Thread und verteilt den Inhalt der Zwischenablage an (unter Umständen auch mehrere) Plugins Die gefundenen Treffer werden
 * ausgeschnitten.
 * 
 * @author astaldo
 */
public class DistributeData {

    /**
     * Die zu verteilenden Daten
     */
    private String                  data;

    private final ArrayList<String> foundPasswords = new ArrayList<String>();

    /**
     * Erstellt einen neuen Thread mit dem Text, der verteilt werden soll. Die übergebenen Daten werden durch einen URLDecoder geschickt.
     * 
     * @param data
     *            Daten, die verteilt werden sollen
     */
    @Deprecated
    public DistributeData(final String data) {
        this.data = data;
    }

    /* keep for comp. issues in other projects */
    @Deprecated
    public DistributeData setFilterNormalHTTP(final boolean b) {
        return this;
    }

    @Deprecated
    public static boolean hasPluginFor(final String tmp, final boolean filterNormalHTTP) {
        return true;
    }

    @Deprecated
    public ArrayList<DownloadLink> findLinks() {
        foundPasswords.addAll(HTMLParser.findPasswords(data));
        LinkCrawler lf = new LinkCrawler();
        lf.crawl(data);
        lf.waitForCrawling();
        ArrayList<DownloadLink> ret = new ArrayList<DownloadLink>(lf.getCrawledLinks().size());
        for (final CrawledLink link : lf.getCrawledLinks()) {
            DownloadLink dl = link.getDownloadLink();
            if (dl != null) {
                List<String> oldList = dl.getSourcePluginPasswordList();
                if (oldList != null && oldList.size() > 0) {
                    oldList = new ArrayList<String>(oldList);
                    oldList.addAll(foundPasswords);
                    dl.setSourcePluginPasswordList(foundPasswords);
                } else {
                    if (foundPasswords.size() > 0) dl.setSourcePluginPasswordList(foundPasswords);
                }
                ret.add(dl);
            }
        }
        return ret;
    }

}