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
import jd.plugins.FilePackage;
import jd.plugins.PluginForDecrypt;
import jd.utils.locale.JDL;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "mp3.uzhgorod.name" }, urls = { "http://[\\w\\.]*?mp3\\.uzhgorod\\.name/(\\d+/\\d+/\\d+/|mp3music/\\d+-).+\\.html" }, flags = { 0 })
public class MpUzhgrdNme extends PluginForDecrypt {

    public MpUzhgrdNme(PluginWrapper wrapper) {
        super(wrapper);
    }

    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        br.setFollowRedirects(false);
        br.setCustomCharset("windows-1251");
        String parameter = param.toString();
        br.getPage(parameter);
        if (br.containsHTML("К сожалению, данная страница для Вас не доступна, возможно был изменен ее адрес или она была удалена. Пожалуйста, воспользуйтесь поиском")) throw new DecrypterException(JDL.L("plugins.decrypt.errormsg.unavailable", "Perhaps wrong URL or the download is not available anymore."));
        String fpName = br.getRegex("<title>(.*?)\\&raquo; Территория Меломана").getMatch(0);
        if (fpName == null) {
            fpName = br.getRegex("<h1>(.*?)</h1>").getMatch(0);
            if (fpName == null) fpName = br.getRegex("title='(.*?)'").getMatch(0);
        }
        String pagepiece = br.getRegex("</strong><br /><\\!--dle_leech_begin--><a href=\"(.*?)</a><\\!--dle_leech_end--></div>").getMatch(0);
        if (pagepiece == null) pagepiece = br.getRegex("<span><strong></strong></span></div>(.*?)</div></div></div>.*?<div class=\"clr\"></div>").getMatch(0);
        String[] links = null;
        if (pagepiece != null) {
            links = HTMLParser.getHttpLinks(pagepiece, "");
        } else {
            links = br.getRegex("<!--dle_leech_begin--><a href=\"(.*?)\"").getColumn(0);
        }
        if (links == null || links.length == 0) return null;
        progress.setRange(links.length);
        for (String dl : links) {
            if (dl.contains("/engine/go.php")) {
                br.getPage(dl);
                String finallink = br.getRedirectLocation();
                if (finallink == null)
                    logger.info("finallink for link " + dl + " was null");
                else
                    decryptedLinks.add(createDownloadlink(finallink));
            } else {
                if (!dl.contains("mp3.uzhgorod.name/")) decryptedLinks.add(createDownloadlink(dl));
            }
            progress.increase(1);
        }
        if (fpName != null) {
            FilePackage fp = FilePackage.getInstance();
            fp.setName(fpName.trim());
            fp.addLinks(decryptedLinks);
        }
        return decryptedLinks;
    }

}
