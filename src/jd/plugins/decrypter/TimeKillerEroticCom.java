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
import java.util.Random;

import jd.PluginWrapper;
import jd.controlling.ProgressController;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.PluginForDecrypt;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "timekiller-erotic.com" }, urls = { "http://(www\\.)?timekiller\\-erotic\\.com/Video/\\d+/.*?\\.html" }, flags = { 0 })
public class TimeKillerEroticCom extends PluginForDecrypt {

    public TimeKillerEroticCom(PluginWrapper wrapper) {
        super(wrapper);
    }

    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        br.setFollowRedirects(false);
        String parameter = param.toString();
        br.getPage(parameter);
        String tempID = br.getRedirectLocation();
        if (tempID != null) {
            DownloadLink dl = createDownloadlink(tempID);
            decryptedLinks.add(dl);
            return decryptedLinks;
        }
        String filename = br.getRegex("<h1 class=\"st1\"><strong class=\"fl vth\">(.*?)</strong>").getMatch(0);
        if (filename == null) {
            filename = br.getRegex("<meta name=\"title\" content=\"(.*?)\" />").getMatch(0);
            if (filename == null) {
                filename = br.getRegex(":: Viewing Media \\- (.*?)</title>").getMatch(0);
            }
        }
        if (filename == null) {
            logger.warning("Decrypter broken for link: " + parameter);
            return null;
        }
        filename = filename.trim();
        tempID = br.getRegex("http://flash\\.serious\\-cash\\.com/flvplayer\\.swf\" width=\"\\d+\" height=\"\\d+\" allowfullscreen=\"true\" flashvars=\"file=(.*?)\\&").getMatch(0);
        if (tempID != null) {
            DownloadLink dl = createDownloadlink("directhttp://http://flash.serious-cash.com/" + tempID + ".flv");
            decryptedLinks.add(dl);
            dl.setFinalFileName(filename + ".flv");
            return decryptedLinks;
        }
        tempID = br.getRegex("file=(http://(www\\.)?hostave\\d+\\.net/.*?)\\&screenfile").getMatch(0);
        if (tempID != null) {
            DownloadLink dl = createDownloadlink("directhttp://" + tempID);
            dl.setFinalFileName(filename + ".flv");
            decryptedLinks.add(dl);
            return decryptedLinks;

        }
        tempID = br.getRegex("var urlAddress = \"(http://.*?)\"").getMatch(0);
        if (tempID != null) {
            DownloadLink dl = createDownloadlink(tempID);
            decryptedLinks.add(dl);
            return decryptedLinks;
        }
        tempID = br.getRegex("\\&file=(http://static\\.mofos\\.com/.*?)\\&enablejs").getMatch(0);
        if (tempID != null) {
            DownloadLink dl = createDownloadlink("directhttp://" + tempID);
            dl.setFinalFileName(filename + ".flv");
            decryptedLinks.add(dl);
            return decryptedLinks;
        }
        tempID = br.getRegex("addVariable\\(\\'file\\',\\'(http://.*?)\\'\\)").getMatch(0);
        if (tempID == null) tempID = br.getRegex("\\'(http://(www\\.)?amateurdumper\\.com/videos/.*?)\\'").getMatch(0);
        if (tempID != null) {
            DownloadLink dl = createDownloadlink("directhttp://" + tempID);
            dl.setFinalFileName(filename + ".flv");
            decryptedLinks.add(dl);
            return decryptedLinks;
        }
        tempID = br.getRegex("\"(http://(www\\.)pornyeah\\.com/videos/.*?)\"").getMatch(0);
        if (tempID != null) {
            DownloadLink dl = createDownloadlink(tempID);
            decryptedLinks.add(dl);
            return decryptedLinks;
        }
        tempID = br.getRegex("pornyeah\\.com/playerConfig\\.php\\?[a-z0-9]+\\.[a-z0-9]+\\|(\\d+)").getMatch(0);
        if (tempID != null) {
            DownloadLink dl = createDownloadlink("http://www.pornyeah.com/videos/" + Integer.toString(new Random().nextInt(1000000)) + "-" + tempID + ".html");
            decryptedLinks.add(dl);
            return decryptedLinks;
        }
        tempID = br.getRegex("timekiller\\-erotic\\.com/player/pikoplayer\\.php\\?video=(http://.*?)\"").getMatch(0);
        if (tempID == null) tempID = br.getRegex("(http://(www\\.)?video\\.timekiller\\-erotic\\.com/flv/.*?)\"").getMatch(0);
        if (tempID != null) {
            DownloadLink dl = createDownloadlink("directhttp://" + tempID);
            dl.setFinalFileName(filename + ".flv");
            decryptedLinks.add(dl);
            return decryptedLinks;
        }
        tempID = br.getRegex("\"id_video=(\\d+)\"").getMatch(0);
        if (tempID != null) {
            DownloadLink dl = createDownloadlink("http://www.xvideos.com/video" + tempID);
            decryptedLinks.add(dl);
            return decryptedLinks;
        }
        tempID = br.getRegex("book\\-mark\\.net/playerconfig/(\\d+)/").getMatch(0);
        if (tempID != null) {
            DownloadLink dl = createDownloadlink("http://www.book-mark.net/videos/" + tempID + "/x.html");
            decryptedLinks.add(dl);
            return decryptedLinks;
        }
        if (tempID == null) {
            logger.warning("Decrypter broken for link: " + parameter);
            return null;
        }
        return decryptedLinks;
    }

}
