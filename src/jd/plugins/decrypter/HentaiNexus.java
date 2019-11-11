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

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "hentainexus.com" }, urls = { "https?://(www\\.)?hentainexus\\.com/((?:view|read)/.+|(page/[^/]+)\\?q=.+)" })
public class HentaiNexus extends antiDDoSForDecrypt {
    public HentaiNexus(PluginWrapper wrapper) {
        super(wrapper);
    }

    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        String parameter = param.toString();
        br.setFollowRedirects(true);
        getPage(parameter);
        String fpName = br.getRegex("<title>\\s*(?:\\[[^\\]]+\\]\\s*)?([^<]+)\\s+::\\s+HentaiNexus").getMatch(0);
        ArrayList<String> links = new ArrayList<String>();
        Collections.addAll(links, br.getRegex("<a[^>]+href\\s*=\\s*\"([^\"]*/read/[^\"]+)\"[^>]*>\\s*<div[^>]+class\\s*=\\s*\"card\"[^>]*>").getColumn(0));
        if (parameter.contains("?q=")) {
            Collections.addAll(links, br.getRegex("<a[^>]+href\\s*=\\s*\"([^\"]*/view/[^\"]+)\"[^>]*>\\s*<div[^>]+class\\s*=\\s*\"card\"[^>]*>").getColumn(0));
            Collections.addAll(links, br.getRegex("<a[^>]+class\\s*=\\s*\"[^\"]*pagination-(?:next|previous)?[^\"]*\"[^>]+href\\s*=\\s*\"([^\"]+)\"[^>]*>").getColumn(0));
        }
        for (String link : links) {
            link = processPrefixSlashes(br, Encoding.htmlDecode(link));
            decryptedLinks.add(createDownloadlink(link));
        }
        String image = br.getRegex("<img[^>]+src\\s*=\\s*\"([^\"]+)\"[^>]+id\\s*=\\s*\"currImage\"[^>]*>").getMatch(0);
        if (StringUtils.isNotEmpty(image)) {
            String page = new Regex(parameter, "/read/[^/]+/([^/]+)").getMatch(0);
            image = processPrefixSlashes(br, Encoding.htmlDecode(image));
            DownloadLink dl = createDownloadlink(image);
            if (StringUtils.isNotEmpty(fpName) && StringUtils.isNotEmpty(page)) {
                dl.setFinalFileName(fpName + "_" + page + getFileNameExtensionFromString(image, ".png"));
            }
            decryptedLinks.add(dl);
        }
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