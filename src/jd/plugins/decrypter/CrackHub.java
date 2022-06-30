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

import org.jdownloader.plugins.components.antiDDoSForDecrypt;

import jd.PluginWrapper;
import jd.controlling.ProgressController;
import jd.controlling.linkcrawler.LinkCrawler;
import jd.nutils.encoding.Encoding;
import jd.parser.html.HTMLParser;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 3, names = { "crackhub.site" }, urls = { "https?://(?:www\\.)?crackhub\\.site/[a-z0-9\\-]+/?" })
public class CrackHub extends antiDDoSForDecrypt {
    public CrackHub(PluginWrapper wrapper) {
        super(wrapper);
    }

    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        String parameter = param.toString();
        br.setFollowRedirects(true);
        getPage(parameter);
        String fpName = br.getRegex("<title>\\s*([^<]+)\\s+&#8211\\;\\s+crackhub213").getMatch(0);
        String linkBlock = br.getRegex("<h2[^\\>]+>Downloads</h2>([^§]+)<footer[^>]+class\\s*=\\s*\"entry-meta").getMatch(0);
        ArrayList<String> links = new ArrayList<String>();
        Collections.addAll(links, HTMLParser.getHttpLinks(linkBlock, ""));
        for (String link : links) {
            decryptedLinks.add(createDownloadlink(Encoding.htmlDecode(link)));
        }
        if (fpName != null) {
            final FilePackage fp = FilePackage.getInstance();
            fp.setName(Encoding.htmlDecode(fpName.trim()));
            fp.setProperty(LinkCrawler.PACKAGE_ALLOW_MERGE, true);
            fp.setAllowInheritance(true);
            fp.addLinks(decryptedLinks);
        }
        return decryptedLinks;
    }
}
