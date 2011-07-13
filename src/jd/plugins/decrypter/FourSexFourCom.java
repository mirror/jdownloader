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

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "4sex4.com" }, urls = { "http://(www\\.)?4sex4\\.com/\\d+/.*?\\.html" }, flags = { 0 })
public class FourSexFourCom extends PluginForDecrypt {

    public FourSexFourCom(PluginWrapper wrapper) {
        super(wrapper);
    }

    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        String parameter = param.toString();
        br.getPage(parameter);
        String tempID = br.getRegex("name=\"FlashVars\" value=\"options=(http://(www\\.)keezmovies\\.com/.*?)\"").getMatch(0);
        if (tempID != null) {
            br.getPage(tempID);
            String finallink = br.getRegex("<share>(http://.*?)</share>").getMatch(0);
            if (finallink == null) {
                logger.warning("Decrypter broken for link: " + parameter);
                return null;
            }
            DownloadLink dl = createDownloadlink(finallink);
            decryptedLinks.add(dl);
            return decryptedLinks;
        }
        tempID = br.getRegex("name=\"FlashVars\" value=\"options=(http://(www\\.)?extremetube\\.com/embed_player\\.php\\?id=\\d+)\"").getMatch(0);
        if (tempID != null) {
            br.getPage(tempID);
            String finallink = br.getRegex("<click_tag>(http://.*?)</click_tag>").getMatch(0);
            if (finallink == null) {
                logger.warning("Decrypter broken for link: " + parameter);
                return null;
            }
            DownloadLink dl = createDownloadlink(finallink);
            decryptedLinks.add(dl);
            return decryptedLinks;
        }
        String filename = br.getRegex("<title>(.*?) \\- 4sex4\\.com</title>").getMatch(0);
        if (filename == null) filename = br.getRegex("<h1>(.*?)</h1>").getMatch(0);
        if (filename == null) {
            logger.warning("Decrypter broken for link: " + parameter);
            return null;
        }
        filename = filename.trim();
        tempID = br.getRegex("flashvars=\"videoCode=(.*?)\\&WID=\">").getMatch(0);
        if (tempID != null) {
            br.getPage("http://www.shufuni.com/handlers/FLVStreamingv2.ashx?videoCode=" + tempID);
            String finallink = br.getRegex("CDNUrl=(http://.*?)\\&SeekType").getMatch(0);
            DownloadLink dl = createDownloadlink("directhttp://" + finallink);
            dl.setFinalFileName(filename + finallink.substring(finallink.length() - 4, finallink.length()));
            decryptedLinks.add(dl);
            return decryptedLinks;
        }
        tempID = br.getRegex("xvideos\\.com/embedframe/(\\d+)").getMatch(0);
        if (tempID != null) {
            decryptedLinks.add(createDownloadlink("http://www.xvideos.com/video" + tempID));
            return decryptedLinks;
        }
        if (tempID == null) {
            logger.warning("Decrypter broken for link: " + parameter);
            return null;
        }
        return decryptedLinks;
    }

}
