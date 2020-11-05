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
import java.util.Collections;

import jd.PluginWrapper;
import jd.controlling.ProgressController;
import jd.controlling.linkcrawler.CrawledLink;
import jd.controlling.linkcrawler.LinkCrawler;
import jd.http.Browser;
import jd.http.URLConnectionAdapter;
import jd.nutils.encoding.Encoding;
import jd.parser.html.HTMLParser;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;

import org.appwork.utils.Regex;
import org.appwork.utils.StringUtils;
import org.appwork.utils.encoding.Base64;
import org.jdownloader.plugins.components.antiDDoSForDecrypt;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 3, names = { "getcomics.info" }, urls = { "https?://getcomics\\.info/(?!share/|page/)[^/]+/.+" })
public class GetComics extends antiDDoSForDecrypt {
    public GetComics(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        final ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        final String parameter = param.toString();
        // Load page
        br.setFollowRedirects(true);
        final URLConnectionAdapter con = openAntiDDoSRequestConnection(br, br.createGetRequest(parameter));
        try {
            final LinkCrawler crawler = getCrawler();
            if (crawler.getDeepInspector().looksLikeDownloadableContent(con)) {
                final CrawledLink direct = crawler.createDirectHTTPCrawledLink(getCurrentLink(), con);
                decryptedLinks.add(direct.getDownloadLink());
                // return decryptedLinks;
            } else {
                br.followConnection();
            }
        } finally {
            con.disconnect();
        }
        if (br.containsHTML("You have been redirected through this website from a suspicious source")) {
            String base64 = new Regex(parameter, "((aHR0c|ZnRwOi).+)($|\\?)").getMatch(0);
            if (base64 != null) {
                /* base64 http and ftp */
                while (true) {
                    if (base64.length() % 4 != 0) {
                        base64 += "=";
                    } else {
                        break;
                    }
                }
                final byte[] decoded = Base64.decode(base64);
                if (decoded != null) {
                    final String possibleURLs = new String(decoded, "UTF-8");
                    decryptedLinks.add(createDownloadlink(possibleURLs));
                    return decryptedLinks;
                }
            }
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        final String title = br.getRegex("<title>(.+?) &ndash; GetComics").getMatch(0);
        String baseurl1 = br.getHost();
        ArrayList<String> links = new ArrayList<String>();
        final String textBody = br.getRegex("<section class=\"post-contents\">(.*)<strong>(?:Screenshots|Notes)").getMatch(0);
        if (StringUtils.isNotEmpty(textBody)) {
            Collections.addAll(links, HTMLParser.getHttpLinks(textBody, null));
        } else {
            Collections.addAll(links, br.getRegex("<h1[^>]+class\\s*=\\s*\"post-title\"[^>]*>\\s*<a[^>]+href\\s*=\\s*\"([^\"]+)\"[^>]*>").getColumn(0));
            Collections.addAll(links, br.getRegex("<a[^>]+class\\s*=\\s*\"page-numbers[^\"]*\"[^>]+href\\s*=\\s*\"([^\"]+)\"").getColumn(0));
            Collections.addAll(links, br.getRegex("href\\s*=\\s*\"([^\"]+)\"[^>]+class\\s*=\\s*\"pagination-button").getColumn(0));
        }
        if (!links.isEmpty()) {
            for (String link : links) {
                String detectedLink = null;
                if (StringUtils.containsIgnoreCase(link, "run.php-urls")) {
                    // checks for correct referer!
                    final Browser brc = br.cloneBrowser();
                    brc.setFollowRedirects(false);
                    getPage(brc, link);
                    String redirect = brc.getRedirectLocation();
                    if (redirect == null) {
                        sleep(1000, param);
                        getPage(brc, parameter);
                        getPage(brc, link);
                        redirect = brc.getRedirectLocation();
                    }
                    if (redirect != null) {
                        detectedLink = redirect;
                    }
                } else {
                    detectedLink = Encoding.htmlDecode(link);
                }
                if (new Regex(detectedLink, ".*(imgur\\.com|/contact|/sitemap|/how-to-download).*").matches()) {
                    continue;
                }
                decryptedLinks.add(createDownloadlink(detectedLink));
            }
        }
        if (StringUtils.isEmpty(title)) {
            final FilePackage filePackage = FilePackage.getInstance();
            filePackage.setName(Encoding.htmlDecode(title));
            filePackage.addLinks(decryptedLinks);
        }
        //
        return decryptedLinks;
    }
}