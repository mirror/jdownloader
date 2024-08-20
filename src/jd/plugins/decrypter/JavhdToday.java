//jDownloader - Downloadmanager
//Copyright (C) 2015  JD-Team support@jdownloader.org
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

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.jdownloader.plugins.controller.LazyPlugin;

import jd.PluginWrapper;
import jd.controlling.ProgressController;
import jd.http.Browser;
import jd.parser.Regex;
import jd.parser.html.HTMLParser;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForDecrypt;
import jd.plugins.components.PluginJSonUtils;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = {}, urls = {})
public class JavhdToday extends PluginForDecrypt {
    public JavhdToday(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public LazyPlugin.FEATURE[] getFeatures() {
        return new LazyPlugin.FEATURE[] { LazyPlugin.FEATURE.XXX };
    }

    public static List<String[]> getPluginDomains() {
        final List<String[]> ret = new ArrayList<String[]>();
        // each entry in List<String[]> will result in one PluginForDecrypt, Plugin.getHost() will return String[0]->main domain
        ret.add(new String[] { "javhd.today" });
        ret.add(new String[] { "javrave.club", "javr.club" });
        ret.add(new String[] { "javnew.net" });
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

    @SuppressWarnings("deprecation")
    public ArrayList<DownloadLink> decryptIt(final CryptedLink param, ProgressController progress) throws Exception {
        final ArrayList<DownloadLink> ret = new ArrayList<DownloadLink>();
        final String contenturl = param.getCryptedUrl();
        br.setFollowRedirects(true);
        br.getPage(contenturl);
        if (br.getHttpConnection().getResponseCode() == 404 || br.containsHTML("(?i)404 Not Found\\s*<|Page not found")) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        final String urlSlug = new Regex(br.getURL(), "/([\\w-]+)/?$").getMatch(0);
        final String fptitle;
        if (urlSlug != null) {
            fptitle = urlSlug.replace("-", " ").trim();
        } else {
            /* Fallback */
            fptitle = br._getURL().getPath();
        }
        final FilePackage fp = FilePackage.getInstance();
        fp.setName(fptitle);
        final ArrayList<DownloadLink> mainResults = findLinks(br);
        for (final DownloadLink result : mainResults) {
            result._setFilePackage(fp);
            distribute(result);
            ret.add(result);
        }
        final ArrayList<String> mirrorsNoDupes = new ArrayList<String>();
        final String[] mirrorurls = br.getRegex("(?i)(/[\\w\\-]+/\\?link=\\d+)").getColumn(0);
        /* Remove duplicates */
        for (final String mirrorurl : mirrorurls) {
            if (!mirrorsNoDupes.contains(mirrorurl)) {
                mirrorsNoDupes.add(mirrorurl);
            }
        }
        int index = 0;
        for (final String mirrorurl : mirrorsNoDupes) {
            logger.info("Crawling mirror " + index + "/" + mirrorsNoDupes.size());
            br.getPage(mirrorurl);
            final ArrayList<DownloadLink> thisResults = findLinks(br);
            if (thisResults.isEmpty()) {
                logger.warning("Failed to find any results in link: " + br.getURL());
                continue;
            }
            for (final DownloadLink result : thisResults) {
                result._setFilePackage(fp);
                distribute(result);
                ret.add(result);
            }
            if (this.isAbort()) {
                logger.info("Stopping because: Aborted by user");
                break;
            }
        }
        return ret;
    }

    private ArrayList<DownloadLink> findLinks(final Browser br) throws IOException {
        final ArrayList<DownloadLink> ret = new ArrayList<DownloadLink>();
        /** 2021-02-17: Check for extra iframe containing fembed source URL javhd.today ticket 89512 */
        final String iframeRedirect = br.getRegex("<iframe id=\"main-player\" src=\"(https?://player\\.[^/]+/\\d+/?)\"").getMatch(0);
        if (iframeRedirect != null) {
            br.getPage(iframeRedirect);
        }
        /* 2021-02-17: javhd.today ticket 89512 */
        final String[] allExternalSources = br.getRegex("playEmbed\\('(https?://[^<>\"\\']+)'\\)").getColumn(0);
        if (allExternalSources.length > 0) {
            for (final String externalSource : allExternalSources) {
                ret.add(this.createDownloadlink(externalSource));
            }
            return ret;
        }
        String fembed = br.getRegex("<iframe[^<>]*?src=\"([^<>]*?/v/.*?)\"").getMatch(0);
        if (fembed == null) {
            fembed = br.getRegex("allowfullscreen=[^<>]+?(http[^<>]+?)>").getMatch(0); // javr.club
        }
        if (fembed == null) {
            /* Fallback - crawl all URLs inside all iframes where they usually got their players. */
            final String[] iframes = br.getRegex("<iframe(.*?)</iframe>").getColumn(0);
            for (final String iframe : iframes) {
                final String[] urls = HTMLParser.getHttpLinks(iframe, br.getURL());
                for (final String url : urls) {
                    ret.add(this.createDownloadlink(url));
                }
            }
            return ret;
        }
        fembed = PluginJSonUtils.unescape(fembed);
        ret.add(this.createDownloadlink(fembed));
        return ret;
    }
}