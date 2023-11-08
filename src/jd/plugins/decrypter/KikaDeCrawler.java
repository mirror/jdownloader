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
import java.util.List;
import java.util.Map;

import org.appwork.storage.TypeRef;

import jd.PluginWrapper;
import jd.controlling.ProgressController;
import jd.http.Browser;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForDecrypt;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 3, names = {}, urls = {})
public class KikaDeCrawler extends PluginForDecrypt {
    public KikaDeCrawler(PluginWrapper wrapper) {
        super(wrapper);
    }

    public Browser createNewBrowserInstance() {
        final Browser br = super.createNewBrowserInstance();
        br.setFollowRedirects(true);
        return br;
    }

    public static List<String[]> getPluginDomains() {
        final List<String[]> ret = new ArrayList<String[]>();
        // each entry in List<String[]> will result in one PluginForDecrypt, Plugin.getHost() will return String[0]->main domain
        ret.add(new String[] { "kika.de" });
        return ret;
    }

    public static String[] getAnnotationNames() {
        return buildAnnotationNames(getPluginDomains());
    }

    @Override
    public String[] siteSupportedNames() {
        return buildSupportedNames(getPluginDomains());
    }

    public static String[] getAnnotationUrls() {
        return buildAnnotationUrls(getPluginDomains());
    }

    public static String[] buildAnnotationUrls(final List<String[]> pluginDomains) {
        final List<String> ret = new ArrayList<String>();
        for (final String[] domains : pluginDomains) {
            ret.add("https?://(?:www\\.)?" + buildHostsPatternPart(domains) + "/.+");
        }
        return ret.toArray(new String[0]);
    }

    public ArrayList<DownloadLink> decryptIt(final CryptedLink param, ProgressController progress) throws Exception {
        /* Look for link to ardmediathek to the same content. */
        final String urlSlug = new Regex(param.getCryptedUrl(), "/([a-z0-9\\-]+)$").getMatch(0);
        if (urlSlug == null) {
            /* Invalid url */
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        br.getPage("https://www.kika.de/_next-api/proxy/v1/videos/" + urlSlug);
        if (br.getHttpConnection().getResponseCode() == 404) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        final Map<String, Object> entries = restoreFromString(br.getRequest().getHtmlCode(), TypeRef.MAP);
        final String externalId = entries.get("externalId").toString();
        if (externalId.matches("ard-.+")) {
            /* This is what we want -> The easy way */
            final ArrayList<DownloadLink> ret = new ArrayList<DownloadLink>();
            ret.add(this.createDownloadlink("https://www.ardmediathek.de/video/dummy-series/dummy-title-url/ard/" + externalId.replace("ard-", "")));
            return ret;
        } else {
            logger.info("Failed to find mirror in ardmediathek -> Try zdfmediathek");
        }
        br.getPage(param.getCryptedUrl());
        if (br.getHttpConnection().getResponseCode() == 404) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        String title = br.getRegex("<title>([^<]+)</title>").getMatch(0);
        if (title == null) {
            title = br.getRegex("\"VideoObject\",\"name\":\"([^\"]+)\"").getMatch(0);
        }
        if (title == null) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        title = title.trim();
        title = title.replaceAll("(?i)\\s*\\(Hörfassung\\)\\s*", "");
        title = title.replaceAll("(?i)\\s*\\| KiKA", "");
        title = title.replaceAll("^(?i)\\s*Filme:\\s*", "");
        title = Encoding.htmlDecode(title).trim();
        logger.info("Searching this title in ZDFMediathek: " + title);
        final ZDFMediathekDecrypter crawler = (ZDFMediathekDecrypter) this.getNewPluginForDecryptInstance("zdf.de");
        final ArrayList<DownloadLink> zdfSearchResults = crawler.crawlZDFMediathekSearchResultsVOD("ZDFtivi", title, 3);
        if (zdfSearchResults.isEmpty()) {
            logger.info("Unable to find mirror item in ZDFMediathek");
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        return zdfSearchResults;
    }
}
