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
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.plugins.PluginForDecrypt;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "download.pandaapp.com" }, urls = { "http://(www\\.)?download\\.pandaapp\\.com/[^<>\"/]*?/[^<>\"/]*?\\-id\\d+\\.html" }, flags = { 0 })
public class DownloadPandaAppCom extends PluginForDecrypt {

    public DownloadPandaAppCom(PluginWrapper wrapper) {
        super(wrapper);
    }

    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        String parameter = param.toString();
        br.setFollowRedirects(true);
        br.getPage(parameter);
        br.setFollowRedirects(false);
        if (br.getURL().equals("http://www.pandaapp.com/error/")) {
            logger.info("Link offline: " + parameter);
            return decryptedLinks;
        }
        final String fpName = br.getRegex("<div class=\"title\">[\t\n\r ]+<h1>([^<>\"]*?)</h1>").getMatch(0);
        String[] links = br.getRegex("\"(http://[^<>\"]*?)\" class=\"btn_netdisk\"").getColumn(0);
        if (links != null && links.length != 0) {
            for (String singleLink : links)
                decryptedLinks.add(createDownloadlink(singleLink));
        }
        final String controller = br.getRegex("\\&controller=([^<>\"/]*?)\\&").getMatch(0);
        if (controller != null) {
            br.getHeaders().put("X-Requested-With", "XMLHttpRequest");
            br.getPage("http://download.pandaapp.com/?app=soft&controller=" + controller + "&action=FastDownAjaxRedirect&f_id=" + new Regex(parameter, "id(\\d+)\\.html$").getMatch(0));
            String finallink = br.getRegex("\"url\":\"(http:[^<>\"]*?)\"").getMatch(0);
            if (finallink != null) {
                finallink = Encoding.htmlDecode(finallink.trim().replace("\\", ""));
                br.getPage(finallink);
                finallink = br.getRedirectLocation();
                if (finallink != null) decryptedLinks.add(createDownloadlink("directhttp://" + finallink));
            }
        }
        if (decryptedLinks.size() == 0) {
            logger.warning("Decrypter broken for link: " + parameter);
            return null;
        }
        if (fpName != null) {
            FilePackage fp = FilePackage.getInstance();
            fp.setName(Encoding.htmlDecode(fpName.trim()));
            fp.addLinks(decryptedLinks);
        }
        return decryptedLinks;
    }

    /* NO OVERRIDE!! */
    public boolean hasCaptcha(CryptedLink link, jd.plugins.Account acc) {
        return false;
    }

}