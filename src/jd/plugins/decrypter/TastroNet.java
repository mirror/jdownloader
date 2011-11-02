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

import jd.PluginWrapper;
import jd.controlling.ProgressController;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.parser.html.HTMLParser;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.plugins.PluginForDecrypt;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "tastro.net" }, urls = { "http://(www\\.)?tastro\\.net/index\\.php\\?view_article=\\d+" }, flags = { 0 })
public class TastroNet extends PluginForDecrypt {

    public TastroNet(PluginWrapper wrapper) {
        super(wrapper);
    }

    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        String parameter = param.toString();
        br.getPage(parameter);
        if (br.containsHTML("Article was not found")) return decryptedLinks;
        final String veryEffectiveCaptcha = br.getRegex("Unlock Code: <b>(\\d+)</b>").getMatch(0);
        if (veryEffectiveCaptcha != null) br.postPage(parameter, "article_id=" + new Regex(parameter, "tastro\\.net/index\\.php\\?view_article=(\\d+)").getMatch(0) + "&vote=like&unlock_captcha=" + veryEffectiveCaptcha);
        String fpName = br.getRegex("<div class=\"big2\">(.*?)<a href=\"").getMatch(0);
        final String pagepiece = br.getRegex("div class=\"big2\">(.*?)<input type=\"hidden\" name=\"article_id\"").getMatch(0);
        if (pagepiece == null) {
            logger.warning("Decrypter broken for link: " + parameter);
            return null;
        }
        String[] links = HTMLParser.getHttpLinks(pagepiece, "");
        if (links == null || links.length == 0) {
            logger.warning("Decrypter broken for link: " + parameter);
            return null;
        }
        for (String singleLink : links)
            if (!singleLink.matches(".*?tastro\\.net/index\\.php\\?view_article=\\d+")) decryptedLinks.add(createDownloadlink(singleLink));
        if (fpName != null) {
            FilePackage fp = FilePackage.getInstance();
            fp.setName(Encoding.htmlDecode(fpName.trim()));
            fp.addLinks(decryptedLinks);
        }
        return decryptedLinks;
    }

}
