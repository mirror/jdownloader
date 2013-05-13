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
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.PluginForDecrypt;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "gamebanana.com" }, urls = { "http://(www\\.)?([a-z0-9]+\\.)?gamebanana\\.com/[a-z0-9]+/download/\\d+" }, flags = { 0 })
public class GamaBananaCom extends PluginForDecrypt {

    public GamaBananaCom(PluginWrapper wrapper) {
        super(wrapper);
    }

    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        String parameter = param.toString();
        br.setFollowRedirects(true);
        if (!parameter.contains("/download/")) parameter = parameter.replace("/maps/", "/maps/download/");
        br.getPage(parameter);
        String finallink = br.getRegex("<input type=\"hidden\" id=\"DirectDownloadUrl\" value=\"(http://[^<>\"\\']+)\"").getMatch(0);
        if (finallink == null) {
            finallink = br.getRegex("Problems with the Download?<br/>Try this <a href=\"(http://[^<>\"\\']+)\"").getMatch(0);
            if (finallink == null) {
                finallink = br.getRegex("<td class=\"Download\">[\t\n\r ]+<a href=\"(http://[^<>\"\\']+)\"").getMatch(0);
                if (finallink == null) {
                    finallink = br.getRegex("\"(http://files\\.gamebanana\\.com/[^<>\"\\'/]+/[^<>\"\\']+)\"").getMatch(0);
                }
            }
        }
        if (finallink == null) {
            logger.warning("Decrypter broken for link: " + parameter);
            return null;
        }
        decryptedLinks.add(createDownloadlink("directhttp://" + finallink));
        return decryptedLinks;
    }

    /* NO OVERRIDE!! */
    public boolean hasCaptcha(CryptedLink link, jd.plugins.Account acc) {
        return false;
    }

}