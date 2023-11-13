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
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import org.appwork.storage.TypeRef;
import org.appwork.utils.StringUtils;
import org.appwork.utils.parser.UrlQuery;

import jd.PluginWrapper;
import jd.controlling.ProgressController;
import jd.http.Browser;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.parser.html.HTMLParser;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForDecrypt;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 3, names = {}, urls = {})
public class SextbNet extends PluginForDecrypt {
    public SextbNet(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public Browser createNewBrowserInstance() {
        final Browser br = super.createNewBrowserInstance();
        br.setFollowRedirects(true);
        return br;
    }

    public static List<String[]> getPluginDomains() {
        final List<String[]> ret = new ArrayList<String[]>();
        // each entry in List<String[]> will result in one PluginForDecrypt, Plugin.getHost() will return String[0]->main domain
        ret.add(new String[] { "sextb.net" });
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
            ret.add("https?://(?:www\\.)?" + buildHostsPatternPart(domains) + "/([A-Za-z0-9\\-_]+)$");
        }
        return ret.toArray(new String[0]);
    }

    public ArrayList<DownloadLink> decryptIt(final CryptedLink param, ProgressController progress) throws Exception {
        final String slug = new Regex(param.getCryptedUrl(), this.getSupportedLinks()).getMatch(0);
        br.getPage(param.getCryptedUrl());
        if (br.getHttpConnection().getResponseCode() == 404) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        String title = br.getRegex("<title>([^>]+)</title>").getMatch(0);
        if (title == null) {
            /* Fallback */
            title = slug.replace("-", " ");
        }
        title = Encoding.htmlDecode(title).trim();
        final String filmID = br.getRegex("filmId\\s*=\\s*(\\d+);").getMatch(0);
        if (filmID == null) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        final String[] mirrorIDs = br.getRegex("data-id=\"(\\d+)").getColumn(0);
        if (mirrorIDs == null || mirrorIDs.length == 0) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        final HashSet<String> dupes = new HashSet<String>();
        final ArrayList<DownloadLink> ret = new ArrayList<DownloadLink>();
        final FilePackage fp = FilePackage.getInstance();
        fp.setName(title);
        int counter = 0;
        for (final String mirrorID : mirrorIDs) {
            counter++;
            logger.info("Crawling item: " + counter + "/" + mirrorIDs.length + " | " + mirrorID);
            if (!dupes.add(mirrorID)) {
                /* Skip duplicates. */
                continue;
            }
            final Browser brc = br.cloneBrowser();
            final UrlQuery query = new UrlQuery();
            query.add("episode", mirrorID);
            query.add("filmId", filmID);
            brc.postPage("/ajax/player", query);
            final Map<String, Object> entries = restoreFromString(brc.getRequest().getHtmlCode(), TypeRef.MAP);
            final String html = entries.get("player").toString();
            if (StringUtils.containsIgnoreCase(html, "This is a server for VIP Members")) {
                logger.info("Potential premium only mirror: " + mirrorID);
            }
            final String[] urls = HTMLParser.getHttpLinks(html, br.getURL());
            for (final String url : urls) {
                if (this.canHandle(url)) {
                    /* Ignore results that would be processed by this crawler again. */
                    continue;
                }
                final DownloadLink link = createDownloadlink(url);
                link._setFilePackage(fp);
                ret.add(link);
                distribute(link);
            }
            if (this.isAbort()) {
                logger.info("Stopping because: Aborted by user");
                break;
            }
        }
        return ret;
    }
}
