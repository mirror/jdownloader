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

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "timekiller-erotic.com" }, urls = { "http://(www\\.)?timekiller\\-erotic\\.com/(Video/\\d+/.*?|PornHub/\\d+/[a-z0-9\\-_]+)\\.html" }, flags = { 0 })
public class TimeKillerEroticCom extends PluginForDecrypt {

    public TimeKillerEroticCom(PluginWrapper wrapper) {
        super(wrapper);
    }

    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        br.setFollowRedirects(false);
        String parameter = param.toString();
        br.getPage(parameter);
        if (parameter.matches("http://(www\\.)?timekiller\\-erotic\\.com/PornHub/\\d+/[a-z0-9\\-_]+\\.html")) {
            final String continueLink = br.getRegex("\"(http://(www\\.)?lesbians666\\.com/player/pornhubplayer\\.php\\?video=\\d+)\"").getMatch(0);
            if (continueLink == null) {
                logger.warning("Decrypter broken for link: " + parameter);
                return null;
            }
            br.getPage(continueLink);
            String pornHubLink = br.getRegex("(http://(www\\.)?pornhub\\.com/embed_player\\.php\\?id=\\d+)").getMatch(0);
            if (pornHubLink == null) pornHubLink = br.getRegex("\"(http://(www\\.)?pornhub\\.com/embed/\\d+)\"").getMatch(0);
            if (pornHubLink == null) {
                logger.warning("Decrypter broken for link: " + parameter);
                return null;
            }
            decryptedLinks.add(createDownloadlink(pornHubLink));
        } else {
            String externID = br.getRedirectLocation();
            if (externID != null) {
                final DownloadLink dl = createDownloadlink(externID);
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
            externID = br.getRegex("http://flash\\.serious\\-cash\\.com/flvplayer\\.swf\" width=\"\\d+\" height=\"\\d+\" allowfullscreen=\"true\" flashvars=\"file=(.*?)\\&").getMatch(0);
            if (externID != null) {
                DownloadLink dl = createDownloadlink("directhttp://http://flash.serious-cash.com/" + externID + ".flv");
                decryptedLinks.add(dl);
                dl.setFinalFileName(filename + ".flv");
                return decryptedLinks;
            }
            externID = br.getRegex("\"(http://(www\\.)?xhamster\\.com/xembed\\.php\\?video=\\d+)\"").getMatch(0);
            if (externID != null) {
                decryptedLinks.add(createDownloadlink(externID));
                return decryptedLinks;
            }
            externID = br.getRegex("file=(http://(www\\.)?hostave\\d+\\.net/.*?)\\&screenfile").getMatch(0);
            if (externID != null) {
                DownloadLink dl = createDownloadlink("directhttp://" + externID);
                dl.setFinalFileName(filename + ".flv");
                decryptedLinks.add(dl);
                return decryptedLinks;

            }
            externID = br.getRegex("var urlAddress = \"(http://.*?)\"").getMatch(0);
            if (externID != null) {
                DownloadLink dl = createDownloadlink(externID);
                decryptedLinks.add(dl);
                return decryptedLinks;
            }
            externID = br.getRegex("\\&file=(http://static\\.mofos\\.com/.*?)\\&enablejs").getMatch(0);
            if (externID != null) {
                DownloadLink dl = createDownloadlink("directhttp://" + externID);
                dl.setFinalFileName(filename + ".flv");
                decryptedLinks.add(dl);
                return decryptedLinks;
            }
            externID = br.getRegex("addVariable\\(\\'file\\',\\'(http://.*?)\\'\\)").getMatch(0);
            if (externID == null) externID = br.getRegex("\\'(http://(www\\.)?amateurdumper\\.com/videos/.*?)\\'").getMatch(0);
            if (externID != null) {
                DownloadLink dl = createDownloadlink("directhttp://" + externID);
                dl.setFinalFileName(filename + ".flv");
                decryptedLinks.add(dl);
                return decryptedLinks;
            }
            externID = br.getRegex("\"(http://(www\\.)pornyeah\\.com/videos/.*?)\"").getMatch(0);
            if (externID != null) {
                DownloadLink dl = createDownloadlink(externID);
                decryptedLinks.add(dl);
                return decryptedLinks;
            }
            // pornyeah 2
            externID = br.getRegex("pornyeah\\.com/playerConfig\\.php\\?[a-z0-9]+\\.[a-z0-9\\.]+\\|(\\d+)").getMatch(0);
            if (externID != null) {
                DownloadLink dl = createDownloadlink("http://www.pornyeah.com/videos/" + Integer.toString(new Random().nextInt(1000000)) + "-" + externID + ".html");
                decryptedLinks.add(dl);
                return decryptedLinks;
            }
            externID = br.getRegex("timekiller\\-erotic\\.com/player/pikoplayer\\.php\\?video=(http://.*?)\"").getMatch(0);
            if (externID == null) externID = br.getRegex("(http://(www\\.)?video\\.timekiller\\-erotic\\.com/flv/.*?)\"").getMatch(0);
            if (externID != null) {
                DownloadLink dl = createDownloadlink("directhttp://" + externID);
                dl.setFinalFileName(filename + ".flv");
                decryptedLinks.add(dl);
                return decryptedLinks;
            }
            externID = br.getRegex("\"id_video=(\\d+)\"").getMatch(0);
            if (externID != null) {
                DownloadLink dl = createDownloadlink("http://www.xvideos.com/video" + externID);
                decryptedLinks.add(dl);
                return decryptedLinks;
            }
            externID = br.getRegex("book\\-mark\\.net/playerconfig/(\\d+)/").getMatch(0);
            if (externID != null) {
                DownloadLink dl = createDownloadlink("http://www.book-mark.net/videos/" + externID + "/x.html");
                decryptedLinks.add(dl);
                return decryptedLinks;
            }
            externID = br.getRegex("redtube\\.com/player/\"><param name=\"FlashVars\" value=\"id=(\\d+)\\&").getMatch(0);
            if (externID == null) externID = br.getRegex("embed\\.redtube\\.com/player/\\?id=(\\d+)\\&").getMatch(0);
            if (externID != null) {
                DownloadLink dl = createDownloadlink("http://www.redtube.com/" + externID);
                decryptedLinks.add(dl);
                return decryptedLinks;
            }
            externID = br.getRegex("pornhub\\.com/embed/(\\d+)").getMatch(0);
            if (externID == null) externID = br.getRegex("pornhub\\.com/view_video\\.php\\?viewkey=(\\d+)").getMatch(0);
            if (externID == null) externID = br.getRegex("pornhubplayer\\.php\\?video=(\\d+)\"").getMatch(0);
            if (externID != null) {
                DownloadLink dl = createDownloadlink("http://www.pornhub.com/view_video.php?viewkey=" + externID);
                decryptedLinks.add(dl);
                return decryptedLinks;
            }
            // drtuber.com embed v3
            externID = br.getRegex("(http://(www\\.)?drtuber\\.com/player/config_embed3\\.php\\?vkey=[a-z0-9]+)").getMatch(0);
            if (externID != null) {
                decryptedLinks.add(createDownloadlink(externID));
                return decryptedLinks;
            }
            // pornhub handling number 2
            externID = br.getRegex("name=\"FlashVars\" value=\"options=(http://(www\\.)?pornhub\\.com/embed_player(_v\\d+)?\\.php\\?id=\\d+)\"").getMatch(0);
            if (externID != null) {
                br.getPage(externID);
                if (br.containsHTML("<link_url>N/A</link_url>")) {
                    logger.info("Link offline: " + parameter);
                    return decryptedLinks;
                }
                externID = br.getRegex("<link_url>(http://[^<>\"]*?)</link_url>").getMatch(0);
                if (externID == null) {
                    logger.warning("Decrypter broken for link: " + parameter);
                    return null;
                }
                decryptedLinks.add(createDownloadlink(externID));
                return decryptedLinks;
            }
            if (externID == null) {
                logger.warning("Decrypter broken for link: " + parameter);
                return null;
            }
        }
        return decryptedLinks;
    }

    /* NO OVERRIDE!! */
    public boolean hasCaptcha(CryptedLink link, jd.plugins.Account acc) {
        return false;
    }

}