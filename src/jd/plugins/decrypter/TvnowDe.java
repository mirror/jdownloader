//jDownloader - Downloadmanager
//Copyright (C) 2009  JD-Team support@jdownloader.org
//
//This program is free software: you can redistribute it and/or modify
//it under the terms of the GNU General Public License as published by
//the Free Software Foundation, either version 3 of the License, or
//(at your option) any later version.
//
//This program is distributed in the hope that it will be useful,
//but WITHOUT ANY WARRANTY; without even the implied warranty of
//MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
//GNU General Public License for more details.
//
//You should have received a copy of the GNU General Public License
//along with this program.  If not, see <http://www.gnu.org/licenses/>.
package jd.plugins.decrypter;

import java.util.ArrayList;
import java.util.LinkedHashMap;

import org.appwork.utils.StringUtils;
import org.jdownloader.scripting.JavaScriptEngineFactory;

import jd.PluginWrapper;
import jd.controlling.ProgressController;
import jd.parser.Regex;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.PluginForDecrypt;
import jd.plugins.PluginForHost;
import jd.utils.JDUtilities;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 3, names = { "tvnow.de" }, urls = { "https?://(?:www\\.)?tvnow\\.de/.+" })
public class TvnowDe extends PluginForDecrypt {
    public TvnowDe(PluginWrapper wrapper) {
        super(wrapper);
    }

    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        final ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        final PluginForHost hostPlugin = JDUtilities.getPluginForHost(this.getHost());
        final String parameter = param.toString();
        if (hostPlugin.canHandle(parameter)) {
            decryptedLinks.add(this.createDownloadlink(parameter));
            return decryptedLinks;
        }
        String formatID = null;
        String showname;
        final String stationName;
        /* E.g. tvnow.de/shows/bla | tvnow.de/serien/bla */
        if (parameter.matches("https?://[^/]+/[^/]+/[^/]+")) {
            /* 2018-12-12: New linkformat */
            /* 2018-12-12: TODO: StationName is not given in URL anymore - this might become a problem in the future! */
            stationName = null;
            showname = new Regex(parameter, "/([^/]+)$").getMatch(0);
        } else {
            /* Old format */
            final Regex urlInfo = new Regex(parameter, "tvnow\\.de/([^/]+)/([^/]+)/.+");
            stationName = urlInfo.getMatch(0);
            showname = urlInfo.getMatch(1);
        }
        formatID = new Regex(showname, ".+\\-(\\d+)$").getMatch(0);
        if (showname == null) {
            return null;
        }
        showname = jd.plugins.hoster.TvnowDe.cleanupShowTitle(showname);
        String formatTitle = null;
        LinkedHashMap<String, Object> entries = null;
        if (formatID == null) {
            if (StringUtils.isEmpty(stationName)) {
                logger.warning("Failed to find stationName");
            }
            jd.plugins.hoster.TvnowDe.prepBR(this.br);
            /* First we need to find the ID of whatever the user added */
            br.getPage(jd.plugins.hoster.TvnowDe.API_BASE + "/formats/seo?fields=id,title,hasFreeEpisodes,isGeoblocked&name=" + showname + ".php&station=" + stationName);
            entries = (LinkedHashMap<String, Object>) JavaScriptEngineFactory.jsonToJavaMap(br.toString());
            if (br.getHttpConnection().getResponseCode() == 404) {
                /* Rare case */
                decryptedLinks.add(this.createOfflinelink(parameter));
                return decryptedLinks;
            }
            // final boolean hasFreeEpisodes = ((Boolean)entries.get("hasFreeEpisodes")).booleanValue();
            // final boolean isGeoblocked = ((Boolean)entries.get("isGeoblocked")).booleanValue();
            formatID = Long.toString(JavaScriptEngineFactory.toLong(entries.get("id"), 0));
            formatTitle = (String) entries.get("title");
        }
        formatTitle = showname;
        if (formatID.equals("0") || StringUtils.isEmpty(formatTitle)) {
            /* Should never happen */
            logger.warning("Failed to find itemID");
            decryptedLinks.add(this.createOfflinelink(parameter));
            return decryptedLinks;
        }
        final int maxItemsPerPage = 100;
        int page = 1;
        int numberofItemsGrabbedTmp;
        int numberofItemsGrabbedTotal = 0;
        /* TODO: Add functionality */
        int maxNumberofItemsToGrab = -1;
        boolean done = false;
        do {
            numberofItemsGrabbedTmp = 0;
            br.getPage(jd.plugins.hoster.TvnowDe.API_BASE + "/movies?fields=*&filter=%7B%22FormatId%22:" + formatID + "%7D&maxPerPage=" + maxItemsPerPage + "&order=BroadcastStartDate+desc&page=" + page);
            entries = (LinkedHashMap<String, Object>) JavaScriptEngineFactory.jsonToJavaMap(br.toString());
            final ArrayList<Object> ressourcelist = (ArrayList<Object>) entries.get("items");
            for (final Object videoO : ressourcelist) {
                entries = (LinkedHashMap<String, Object>) videoO;
                final String videoSeoName = (String) entries.get("seoUrl");
                final String thisStationName;
                if (stationName != null) {
                    thisStationName = stationName;
                } else {
                    thisStationName = (String) entries.get("cornerLogo");
                }
                if (StringUtils.isEmpty(thisStationName) || StringUtils.isEmpty(videoSeoName)) {
                    logger.warning("Failed to find thisStationName or videoSeoName");
                    return null;
                }
                final String contentURL = String.format("https://www.tvnow.de/%s/%s/%s", thisStationName, showname, videoSeoName);
                final DownloadLink dl = this.createDownloadlink(contentURL);
                jd.plugins.hoster.TvnowDe.parseInformation(dl, entries, thisStationName, formatTitle);
                /* Very important! */
                dl.setProperty("url_showname", showname);
                dl.setProperty("url_episodetitle", videoSeoName);
                decryptedLinks.add(dl);
                distribute(dl);
                numberofItemsGrabbedTmp++;
                numberofItemsGrabbedTotal++;
                /* Did we crawl the max number of items the user wanted to have? */
                done = maxNumberofItemsToGrab > 0 && numberofItemsGrabbedTotal >= maxNumberofItemsToGrab;
            }
            if (!done) {
                /* Did we reach the last page? */
                done = numberofItemsGrabbedTmp < maxItemsPerPage;
            }
            page++;
        } while (!done);
        // String fpName = br.getRegex("").getMatch(0);
        // if (fpName != null) {
        // final FilePackage fp = FilePackage.getInstance();
        // fp.setName(Encoding.htmlDecode(fpName.trim()));
        // fp.addLinks(decryptedLinks);
        // }
        return decryptedLinks;
    }
}
