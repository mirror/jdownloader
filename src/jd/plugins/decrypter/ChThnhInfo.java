//    jDownloader - Downloadmanager
//    Copyright (C) 2009  JD-Team support@jdownloader.org
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
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.plugins.PluginForDecrypt;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "chauthanh.info" }, urls = { "http://[\\w\\.]*?chauthanh\\.info/animeDownload/anime/.*?\\.html" }, flags = { 0 })
public class ChThnhInfo extends PluginForDecrypt {

    public ChThnhInfo(PluginWrapper wrapper) {
        super(wrapper);
    }

    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        String parameter = param.toString();
        br.getPage(parameter);
        if (br.containsHTML("The series information was not found on this server")) {
            logger.info("Link offline: " + parameter);
            return decryptedLinks;
        }
        String fpName = br.getRegex("<title>Download anime(.*?)\\- Download Anime").getMatch(0);
        if (fpName == null) fpName = br.getRegex("class=\"bold1\">Download anime(.*?)</span>").getMatch(0);
        String[] links = br.getRegex("<tr><td><a href=\"(/.*?)\"").getColumn(0);
        if (links == null || links.length == 0) links = br.getRegex("\"(/animeDownload/download/\\d+/.*?)\"").getColumn(0);
        if (links == null || links.length == 0) return null;
        for (String finallink : links) {
            finallink = "http://chauthanh.info" + Encoding.htmlDecode(finallink);
            decryptedLinks.add(createDownloadlink(finallink));
        }
        if (fpName != null) {
            FilePackage fp = FilePackage.getInstance();
            fp.setName(fpName.trim());
            fp.addLinks(decryptedLinks);
        }
        return decryptedLinks;
    }

    /* NO OVERRIDE!! */
    public boolean hasCaptcha(CryptedLink link, jd.plugins.Account acc) {
        return false;
    }

}