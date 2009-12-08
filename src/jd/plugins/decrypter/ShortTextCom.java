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
import jd.parser.html.HTMLParser;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterException;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.PluginForDecrypt;
import jd.utils.locale.JDL;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "shorttext.com" }, urls = { "http://[\\w\\.]*?shorttext\\.com/[a-z0-9]+" }, flags = { 2 })
public class ShortTextCom extends PluginForDecrypt {

    public ShortTextCom(PluginWrapper wrapper) {
        super(wrapper);
    }

    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        String parameter = param.toString();
        br.setFollowRedirects(false);
        br.getPage(parameter);
        /* Error handling */
        if (br.containsHTML("the page has expired or no such shortText information available")) throw new DecrypterException(JDL.L("plugins.decrypt.errormsg.unavailable", "Perhaps wrong URL or there are no links to add"));
        String plaintxt = br.getRegex("<span id=\"lblEdit\"></span>(.*?)<span id=\"lblCount\"><br><br>").getMatch(0);
        if (plaintxt == null) plaintxt = br.getRegex("<DIV align=\"justify\">(.*?)</DIV>").getMatch(0);
        if (plaintxt == null) return null;
        String[] links = HTMLParser.getHttpLinks(plaintxt, "");
        if (links.length == 0) throw new DecrypterException(JDL.L("plugins.decrypt.errormsg.unavailable", "Perhaps wrong URL or there are no links to add"));
        for (String dl : links) {
            if (!dl.contains("shorttext.com") && !dl.contains("tweetmeme.com") || !dl.contains("sharethis.com")) {
                decryptedLinks.add(createDownloadlink(dl));
            }
        }

        return decryptedLinks;
    }

}
