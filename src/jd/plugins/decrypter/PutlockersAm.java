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

import org.appwork.utils.Regex;

import jd.PluginWrapper;
import jd.controlling.ProgressController;
import jd.nutils.encoding.Encoding;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.plugins.PluginForDecrypt;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 3, names = { "putlockers.am" }, urls = { "https?://(www\\d*\\.)?(putlockers\\.am|putlockerhd\\.io)/(watch/|film/|streaming\\.php\\?|load\\.php\\?).+" })
public class PutlockersAm extends PluginForDecrypt {
    public PutlockersAm(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        String parameter = param.toString();
        br.setFollowRedirects(true);
        br.getPage(parameter);
        String fpName = br.getRegex("<meta name=\"description\" content=\"(?:Watch )([^\"]+) Online \\|").getMatch(0);
        String linkBlock = br.getRegex("(<div id=\"list-eps\"[^>]*>[^$]+)<div class=\"(?:mvi-content|movies-list-wrap mlw-related)\"").getMatch(0);
        if (linkBlock != null) {
            String[] links = new Regex(linkBlock, "(?:href|player-data)=\"([^\"]+)\"").getColumn(0);
            System.out.println(String.join("~~~", links));
            for (String link : links) {
                if (!link.contains("addthis.com") && !link.contains("disqus.com")) {
                    if (link.startsWith("//")) {
                        link = link.replace("//", "");
                    }
                    decryptedLinks.add(createDownloadlink(Encoding.htmlDecode(link)));
                }
            }
        }
        String iframe64 = br.getRegex("document\\.write\\(Base64\\.decode\\(\"([^\"]+)\"").getMatch(0);
        if (iframe64 != null) {
            String iframe = Encoding.Base64Decode(iframe64);
            String iframeURL = new Regex(iframe, "src=\"([^\"]+)\"").getMatch(0);
            if (iframeURL != null) {
                decryptedLinks.add(createDownloadlink(Encoding.htmlDecode(iframeURL)));
            }
        }
        String mediablock = br.getRegex("<div id=\"media-player\"[^>]+>\\s*([^$]*)\\s*<div id=\"(?:bar-player|list-eps)\"").getMatch(0);
        if (mediablock != null) {
            String[] mediaLinkURLs = new Regex(linkBlock, "(?:href|src|player-data)=\"([^\"]+)\"").getColumn(0);
            if (mediaLinkURLs != null) {
                decryptedLinks.add(createDownloadlink(Encoding.htmlDecode(mediaLinkURLs[0])));
            }
        }
        if (fpName != null) {
            final FilePackage fp = FilePackage.getInstance();
            fp.setName(Encoding.htmlDecode(fpName.trim()));
            fp.setProperty("ALLOW_MERGE", true);
            fp.addLinks(decryptedLinks);
        }
        return decryptedLinks;
    }
}