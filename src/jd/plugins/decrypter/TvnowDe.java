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

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 3, names = { "tvnow.de" }, urls = { "https?://(?:www\\.)?tvnow\\.de/[^/]+/[^/]+/list/" })
public class TvnowDe extends PluginForDecrypt {
    public TvnowDe(PluginWrapper wrapper) {
        super(wrapper);
    }

    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        final ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        final String parameter = param.toString();
        final Regex urlInfo = new Regex(parameter, "tvnow\\.de/([^/]+)/([^/]+)/list");
        final String stationName = urlInfo.getMatch(0);
        final String itemName = urlInfo.getMatch(1);
        jd.plugins.hoster.TvnowDe.prepBR(this.br);
        /* First we need to find the ID of whatever the user added */
        br.getPage(jd.plugins.hoster.TvnowDe.API_BASE + "/formats/seo?fields=id,title,hasFreeEpisodes,isGeoblocked&name=" + itemName + ".php&station=" + stationName);
        if (br.getHttpConnection().getResponseCode() == 404) {
            /* Rare case */
            decryptedLinks.add(this.createOfflinelink(parameter));
            return decryptedLinks;
        }
        LinkedHashMap<String, Object> entries = (LinkedHashMap<String, Object>) JavaScriptEngineFactory.jsonToJavaMap(br.toString());
        // final boolean hasFreeEpisodes = ((Boolean)entries.get("hasFreeEpisodes")).booleanValue();
        // final boolean isGeoblocked = ((Boolean)entries.get("isGeoblocked")).booleanValue();
        final String formatID = Long.toString(JavaScriptEngineFactory.toLong(entries.get("id"), 0));
        final String formatTitle = (String) entries.get("title");
        if (formatID.equals("0") || StringUtils.isEmpty(formatTitle)) {
            /* Should never happen */
            logger.warning("Failed to find itemID");
            decryptedLinks.add(this.createOfflinelink(parameter));
            return decryptedLinks;
        }
        final int maxItemsPerPage = 100;
        int page = 1;
        int numberofItemsGrabbedTmp;
        do {
            numberofItemsGrabbedTmp = 0;
            br.getPage(jd.plugins.hoster.TvnowDe.API_BASE + "/movies?fields=*&filter=%7B%22FormatId%22:" + formatID + "%7D&maxPerPage=" + maxItemsPerPage + "&order=BroadcastStartDate+desc&page=" + page);
            entries = (LinkedHashMap<String, Object>) JavaScriptEngineFactory.jsonToJavaMap(br.toString());
            final ArrayList<Object> ressourcelist = (ArrayList<Object>) entries.get("items");
            for (final Object videoO : ressourcelist) {
                entries = (LinkedHashMap<String, Object>) videoO;
                final String videoSeoName = (String) entries.get("seoUrl");
                if (StringUtils.isEmpty(videoSeoName)) {
                    return null;
                }
                final String contentURL = String.format("https://www.tvnow.de/%s/%s/%s", stationName, itemName, videoSeoName);
                final DownloadLink dl = this.createDownloadlink(contentURL);
                jd.plugins.hoster.TvnowDe.parseInformation(dl, entries, stationName, formatTitle);
                /** TODO */
                dl.setAvailable(true);
                decryptedLinks.add(dl);
                distribute(dl);
                numberofItemsGrabbedTmp++;
            }
            page++;
        } while (numberofItemsGrabbedTmp >= maxItemsPerPage);
        // String fpName = br.getRegex("").getMatch(0);
        // if (fpName != null) {
        // final FilePackage fp = FilePackage.getInstance();
        // fp.setName(Encoding.htmlDecode(fpName.trim()));
        // fp.addLinks(decryptedLinks);
        // }
        return decryptedLinks;
    }
}
