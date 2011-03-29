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

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "badjojo.com" }, urls = { "http://(www\\.)?badjojo\\.com/\\d+/.{1}" }, flags = { 0 })
public class BadJoJoComDecrypter extends PluginForDecrypt {

    public BadJoJoComDecrypter(PluginWrapper wrapper) {
        super(wrapper);
    }

    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        String parameter = param.toString();
        br.getPage(parameter);
        String filename = br.getRegex("<title>(.*?)</title>").getMatch(0);
        String decrypted = null;
        String externalID = br.getRegex("name=\"FlashVars\" value=\"id=(\\d+)\\&style=redtube\"").getMatch(0);
        if (externalID == null) externalID = br.getRegex("\"http://embed\\.redtube\\.com/player/\\?id=(\\d+)\\&style=").getMatch(0);
        if (externalID != null) {
            decrypted = "http://www.redtube.com/" + externalID;
            decryptedLinks.add(createDownloadlink(decrypted));
            return decryptedLinks;
        }
        externalID = br.getRegex("freeviewmovies\\.com/flv/skin/ofconfig\\.php\\?id=(\\d+)\"").getMatch(0);
        if (externalID != null) {
            decrypted = "http://www.freeviewmovies.com/porn/" + externalID + "/blabla.html";
            decryptedLinks.add(createDownloadlink(decrypted));
            return decryptedLinks;
        }
        externalID = br.getRegex("id_video=(\\d+)\"").getMatch(0);
        if (externalID != null) {
            decrypted = "http://www.xvideos.com/video" + externalID;
            decryptedLinks.add(createDownloadlink(decrypted));
            return decryptedLinks;
        }
        externalID = br.getRegex("flashvars=\"VideoCode=(.*?)\"").getMatch(0);
        if (externalID != null) {
            br.getPage("http://www.shufuni.com/handlers/FLVStreamingv2.ashx?videoCode=" + externalID);
            String finallink = br.getRegex("CDNUrl=(http://.*?)\\&SeekType=").getMatch(0);
            if (finallink == null) {
                logger.warning("badjojo decrypter broken for link: " + parameter);
                return null;
            }
            DownloadLink dl = createDownloadlink("directhttp://" + finallink);
            if (filename != null) dl.setFinalFileName(filename + ".flv");
            decryptedLinks.add(dl);
            return decryptedLinks;
        }
        decrypted = parameter.replace("badjojo.com", "decryptedbadjojo.com");
        decryptedLinks.add(createDownloadlink(decrypted));
        return decryptedLinks;
    }

}
