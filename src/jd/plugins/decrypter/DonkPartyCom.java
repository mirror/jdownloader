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
import jd.plugins.DecrypterException;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.PluginForDecrypt;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "donkparty.com" }, urls = { "http://(www\\.)?donkparty\\.com/\\d+/.{1}" })
public class DonkPartyCom extends PluginForDecrypt {

    public DonkPartyCom(PluginWrapper wrapper) {
        super(wrapper);
    }

    /* DEV NOTES */
    /* Porn_plugin */

    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        br.setFollowRedirects(false);
        String parameter = param.toString() + "/";
        br.getPage(parameter);
        String tempID = br.getRedirectLocation();
        if (tempID != null) {
            final DownloadLink dl = createDownloadlink(tempID);
            decryptedLinks.add(dl);
            return decryptedLinks;
        }
        if (br.containsHTML("Media not found\\!<") || br.containsHTML("<title> free sex video \\- DonkParty</title>") || br.getHttpConnection().getResponseCode() == 404) {
            final DownloadLink offline = createDownloadlink("directhttp://" + parameter);
            offline.setAvailable(false);
            offline.setProperty("offline", true);
            decryptedLinks.add(offline);
            return decryptedLinks;
        }
        String filename = br.getRegex("<span style=\"font\\-weight: bold; font\\-size: 18px;\">(.*?)</span><br").getMatch(0);
        if (filename == null) {
            filename = br.getRegex("<title>(.*?) (free sex video)? ?\\- Donk\\s*Party</title>").getMatch(0);
        }
        if (filename == null) {
            throw new DecrypterException("Decrypter broken for link: " + parameter);
        }
        filename = filename.trim();
        String sources = br.getRegex("sources\":\\[\\{\"src\":\"(.*?)\"").getMatch(0);
        if (sources != null) {
            String finallink = sources;
            DownloadLink dl = createDownloadlink("directhttp://" + finallink);
            dl.setFinalFileName(filename + ".mp4");
            decryptedLinks.add(dl);
            return decryptedLinks;
        }
        tempID = br.getRegex("settings=(http://secret\\.shooshtime\\.com/playerConfig\\.php?.*?)\"").getMatch(0);
        if (tempID != null) {
            br.getPage(tempID);
            String finallink = br.getRegex("defaultVideo:(http://.*?);").getMatch(0);
            if (finallink == null) {
                throw new DecrypterException("Decrypter broken for link: " + parameter);
            }
            DownloadLink dl = createDownloadlink("directhttp://" + finallink);
            dl.setFinalFileName(filename + ".flv");
            decryptedLinks.add(dl);
            return decryptedLinks;
        }
        tempID = br.getRegex("xhamster\\.com/xembed\\.php\\?video=(\\d+)\"").getMatch(0);
        if (tempID != null) {
            decryptedLinks.add(createDownloadlink("http://xhamster.com/movies/" + tempID + "/" + System.currentTimeMillis() + ".html"));
            return decryptedLinks;
        }
        tempID = br.getRegex("\"(http://(www\\.)?myxvids\\.com/embed/\\d+)\"").getMatch(0);
        if (tempID != null) {
            decryptedLinks.add(createDownloadlink(tempID));
            return decryptedLinks;
        }
        if (br.containsHTML("megaporn.com/")) {
            logger.info("Link offline: " + parameter);
            return decryptedLinks;
        }
        String iframe = br.getRegex("<iframe src=\"([^<>\"]+)\"[^<>]*allowfullscreen[^<>]*").getMatch(0);
        if (iframe != null) {
            decryptedLinks.add(createDownloadlink(iframe));
            return decryptedLinks;
        }
        if (tempID == null) {
            throw new DecrypterException("Decrypter broken for link: " + parameter);
        }
        return decryptedLinks;
    }

    /* NO OVERRIDE!! */
    public boolean hasCaptcha(CryptedLink link, jd.plugins.Account acc) {
        return false;
    }

}