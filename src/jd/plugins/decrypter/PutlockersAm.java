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

@DecrypterPlugin(revision = "$Revision: 40114 $", interfaceVersion = 3, names = { "putlockers.am" }, urls = { "https?://(www[0-9]*\\.)?putlockers\\.am/watch/.*" })
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
        String linkBlock = br.getRegex(" <div id=\"cont_player\">(.*)<div class=\"movies-list-wrap mlw-related\">").getMatch(0).toString();
        String[][] links = new Regex(linkBlock, "href=\"([^\"]+)\"").getMatches();
        for (String[] link : links) {
            decryptedLinks.add(createDownloadlink(Encoding.htmlDecode(link[0])));
        }
        String iframe64 = br.getRegex("document\\.write\\(Base64\\.decode\\(\"([^\"]+)\"").getMatch(0);
        if (iframe64 != null) {
            String iframe = Encoding.Base64Decode(iframe64);
            String iframeURL = new Regex(iframe, "src=\"([^\"]+)\"").getMatch(0);
            if (iframeURL != null) {
                decryptedLinks.add(createDownloadlink(Encoding.htmlDecode(iframeURL)));
            }
        } else {
            String mediablock = br.getRegex("<div id=\"media-player\">(.*)</div>.*<div class=\"mvi-content\">").getMatch(0);
            String mediaLinkURL = new Regex(mediablock, "href=\"([^\"]+)\"").getMatch(0);
            if (mediaLinkURL != null) {
                decryptedLinks.add(createDownloadlink(Encoding.htmlDecode(mediaLinkURL)));
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