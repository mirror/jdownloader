//    jDownloader - Downloadmanager
//    Copyright (C) 2008  JD-Team support@jdownloader.org
//
//    This program is free software: you can redistribute it and/or modify
//    it under the terms of the GNU General Public License as published by
//    the Free Software Foundation, either version 3 of the License, or
//    (at your option) any later version.
//
//    This program is distributed in the hope that it will be useful,
//    but WITHOUT ANY WARRANTY; without even the implied warranty of
//    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
//    GNU General Public License for more details.
//
//    You should have received a copy of the GNU General Public License
//    along with this program.  If not, see <http://www.gnu.org/licenses/>.

package jd.plugins.decrypter;

import java.util.ArrayList;

import jd.PluginWrapper;
import jd.controlling.ProgressController;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.PluginForDecrypt;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "relinka.net" }, urls = { "http://(www\\.)?relinka\\.net/folder/[a-z0-9]{8}\\-[a-z0-9]{4}" }, flags = { 0 })
public class RlnkNt extends PluginForDecrypt {

    public RlnkNt(PluginWrapper wrapper) {
        super(wrapper);
    }

    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        String parameter = param.toString();
        br.getPage(parameter);
        if (br.containsHTML("class=\"error\"><b>")) {
            logger.info("Link offline: " + parameter);
            return decryptedLinks;
        }
        final String links[] = br.getRegex("<a href=\"http://relinka.net\\/out\\/[a-z0-9]{8}-[a-z0-9]{4}\\/(.*?)\" class=").getColumn(0);
        final String folderId = new Regex(parameter, "http://relinka.net\\/folder\\/([a-z0-9]{8}-[a-z0-9]{4})").getMatch(0);
        if (links == null || links.length == 0) {
            if (!br.containsHTML("class=\"file")) {
                logger.info("Link offline: " + parameter);
                return decryptedLinks;
            }
            logger.warning("Decrypter broken for link: " + parameter);
            return null;
        }
        for (String element : links) {
            String encodedLink = Encoding.htmlDecode(new Regex(br.getPage("http://relinka.net/out/" + folderId + "/" + element), "<iframe src=\"(.*)\" marginhe").getMatch(0));
            decryptedLinks.add(createDownloadlink(encodedLink));
        }
        return decryptedLinks;
    }

    /* NO OVERRIDE!! */
    public boolean hasCaptcha(CryptedLink link, jd.plugins.Account acc) {
        return false;
    }

}