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
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.regex.Pattern;

import org.appwork.utils.Regex;
import org.appwork.utils.StringUtils;
import org.jdownloader.plugins.components.antiDDoSForDecrypt;

import jd.PluginWrapper;
import jd.controlling.ProgressController;
import jd.controlling.linkcrawler.LinkCrawler;
import jd.http.Browser;
import jd.nutils.encoding.Encoding;
import jd.parser.html.HTMLParser;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "123enjoy.net" }, urls = { "https?://(www\\.)?123enjoy\\.net/watch/.+\\.html?" })
public class OneTwoThreeEnjoy extends antiDDoSForDecrypt {
    public OneTwoThreeEnjoy(PluginWrapper wrapper) {
        super(wrapper);
    }

    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        String parameter = param.toString();
        br.setFollowRedirects(true);
        getPage(parameter);
        String fpName = br.getRegex("<title>(?:Watch\\s+)([^<]+)\\s+Online\\s+\\|\\s+Watch").getMatch(0);
        String itemID = new Regex(parameter, "/watch/\\w+-([^/.]+)").getMatch(0);
        ArrayList<String> links = new ArrayList<String>();
        String[] sources = br.getRegex("<div[^>]+id\\s*=\\s*\"media-player\"[^>]*>\\s*<div>\\s*<span>\\s*</span>\\s*</div>\\s*<script[^>]+type\\s*=\\s*\"text/javascript\"[^>]*>\\s*document\\.write\\s*\\(\\s*Base64\\.decode\\s*\\(\\s*\"([^\"]+)").getColumn(0);
        if (sources != null && sources.length > 0) {
            for (String source : sources) {
                source = Encoding.Base64Decode(source);
                Collections.addAll(links, HTMLParser.getHttpLinks(source, null));
            }
        }
        if (StringUtils.isNotEmpty(itemID)) {
            Collections.addAll(links, br.getRegex("<a[^>]+href\\s*=\\s*\"([^\"]+/watch/[^\"]+" + Pattern.quote(itemID) + "[^\"]*)\"").getColumn(0));
        }
        if (links != null && links.size() > 0) {
            links = new ArrayList<String>(new LinkedHashSet<String>(links));
            for (String link : links) {
                link = Encoding.htmlDecode(link);
                decryptedLinks.add(createDownloadlink(processPrefixSlashes(br, link)));
            }
        }
        if (fpName != null) {
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
        if (link.startsWith("/") || !link.startsWith("http")) {
            link = br.getURL(link).toString();
        }
        return link;
    }
}