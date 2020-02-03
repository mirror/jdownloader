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

import java.io.IOException;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Collections;

import org.appwork.utils.StringUtils;
import org.jdownloader.plugins.components.antiDDoSForDecrypt;

import jd.PluginWrapper;
import jd.controlling.ProgressController;
import jd.controlling.linkcrawler.LinkCrawler;
import jd.http.Browser;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "hentainexus.com" }, urls = { "https?://(?:www\\.)?hentainexus\\.com/(?:view/(\\d+)|(page/[^/]+)\\?q=.+)" })
public class HentainexusCom extends antiDDoSForDecrypt {
    public HentainexusCom(PluginWrapper wrapper) {
        super(wrapper);
    }

    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        String parameter = param.toString();
        br.setFollowRedirects(true);
        getPage(parameter);
        final String id = new Regex(parameter, this.getSupportedLinks()).getMatch(0);
        String author = br.getRegex("<a href=\"/\\?q=artist:[^\"]+\" class=\"has-text-primary\">([^<>]+)</a>").getMatch(0);
        String magazine = br.getRegex("<a href=\"/\\?q=magazine:[^\"]+\" class=\"has-text-primary\">([^<>]+)</a>").getMatch(0);
        String title = br.getRegex("<title>\\s*(?:\\[[^\\]]+\\]\\s*)?([^<]+)\\s+::\\s+HentaiNexus").getMatch(0);
        final String fpName;
        if (author != null && magazine != null && title != null) {
            author = Encoding.htmlDecode(author).trim();
            magazine = Encoding.htmlDecode(magazine).trim();
            title = Encoding.htmlDecode(title).trim();
            fpName = String.format(" [%s] %s (%s)", author, title, magazine);
        } else if (title != null) {
            title = Encoding.htmlDecode(title).trim();
            fpName = title;
        } else {
            /* Final fallback */
            fpName = id;
        }
        ArrayList<String> links = new ArrayList<String>();
        Collections.addAll(links, br.getRegex("<a[^>]+href\\s*=\\s*\"([^\"]*/read/[^\"]+)\"[^>]*>\\s*<div[^>]+class\\s*=\\s*\"card\"[^>]*>").getColumn(0));
        if (parameter.contains("?q=")) {
            Collections.addAll(links, br.getRegex("<a[^>]+href\\s*=\\s*\"([^\"]*/view/[^\"]+)\"[^>]*>\\s*<div[^>]+class\\s*=\\s*\"card\"[^>]*>").getColumn(0));
            Collections.addAll(links, br.getRegex("<a[^>]+class\\s*=\\s*\"[^\"]*pagination-(?:next|previous)?[^\"]*\"[^>]+href\\s*=\\s*\"([^\"]+)\"[^>]*>").getColumn(0));
        }
        final DecimalFormat df = new DecimalFormat("000");
        int page = 0;
        for (String link : links) {
            page++;
            link = processPrefixSlashes(br, Encoding.htmlDecode(link));
            final DownloadLink dl = createDownloadlink(link);
            dl.setFinalFileName(fpName + "_" + df.format(page) + ".png");
            dl.setAvailable(true);
            decryptedLinks.add(dl);
        }
        /* 2020-02-03: Not required anymore --> We got a host plugin now */
        // String image = br.getRegex("<img[^>]+src\\s*=\\s*\"([^\"]+)\"[^>]+id\\s*=\\s*\"currImage\"[^>]*>").getMatch(0);
        // if (StringUtils.isNotEmpty(image)) {
        // String page = new Regex(parameter, "/read/[^/]+/([^/]+)").getMatch(0);
        // image = processPrefixSlashes(br, Encoding.htmlDecode(image));
        // DownloadLink dl = createDownloadlink(image);
        // if (StringUtils.isNotEmpty(fpName) && StringUtils.isNotEmpty(page)) {
        // dl.setFinalFileName(fpName + "_" + page + getFileNameExtensionFromString(image, ".png"));
        // }
        // decryptedLinks.add(dl);
        // }
        if (StringUtils.isNotEmpty(fpName)) {
            final FilePackage fp = FilePackage.getInstance();
            fp.setName(Encoding.htmlDecode(fpName.trim()));
            fp.setProperty(LinkCrawler.PACKAGE_ALLOW_MERGE, true);
            fp.setProperty(LinkCrawler.PACKAGE_ALLOW_INHERITANCE, true);
            fp.addLinks(decryptedLinks);
        }
        return decryptedLinks;
    }

    private String processPrefixSlashes(Browser br, String link) throws IOException {
        link = link.trim().replaceAll("^//", "https://");
        if (link.startsWith("/")) {
            link = br.getURL(link).toString();
        }
        return link;
    }
}