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

import jd.PluginWrapper;
import jd.controlling.ProgressController;
import jd.controlling.linkcrawler.LinkCrawler;
import jd.nutils.encoding.Encoding;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;

import org.appwork.utils.StringUtils;
import org.jdownloader.plugins.components.antiDDoSForDecrypt;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "classiccinemaonline.com" }, urls = { "https?://(www\\.)?classiccinemaonline\\.com/(?:index.php/)?.+$" })
public class ClassicCinemaOnline extends antiDDoSForDecrypt {
    public ClassicCinemaOnline(PluginWrapper wrapper) {
        super(wrapper);
    }

    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        String parameter = param.toString();
        br.setFollowRedirects(true);
        getPage(parameter);
        String fpName = br.getRegex("<title>\\s*([^<]+)\\s*</title>").getMatch(0);
        ArrayList<String> links = new ArrayList<String>();
        Collections.addAll(links, br.getRegex("<iframe[^>]+src=\"([^\"]+)\"").getMatch(0));
        Collections.addAll(links, br.getRegex("<a[^>]+href\\s*=\\s*\"([^\"]+)\"[^>]+><button[^>]+class\\s*=\\s*\"[^\"]+btn-warning\"[^>]+>Download").getMatch(0));
        for (String link : links) {
            if (StringUtils.isNotEmpty(link)) {
                decryptedLinks.add(createDownloadlink(processPrefixSlashes(link)));
            }
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

    private String processPrefixSlashes(String link) throws IOException {
        if (StringUtils.isNotEmpty(link)) {
            link = this.br.getURL(link).toString();
        }
        return link;
    }
}