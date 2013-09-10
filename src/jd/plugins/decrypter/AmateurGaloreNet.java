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
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.PluginForDecrypt;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "amateurgalore.net" }, urls = { "http://(www\\.)?amateurgalore\\.net/(index/video/[a-z0-9_\\-]+|[a-z]+/\\d+/[A-Za-z0-9\\-]+\\.html)" }, flags = { 0 })
public class AmateurGaloreNet extends PluginForDecrypt {

    public AmateurGaloreNet(PluginWrapper wrapper) {
        super(wrapper);
    }

    private static final String NEWLINK = "http://(www\\.)?amateurgalore\\.net/[a-z]+/\\d+/[A-Za-z0-9\\-]+\\.html";

    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        String parameter = param.toString();
        br.setFollowRedirects(false);
        br.getPage(parameter);
        String filename = null;
        String externID = null;
        if (parameter.matches(NEWLINK)) {
            externID = br.getRedirectLocation();
            if ("http://www.amateurgalore.net/".equals(externID)) {
                logger.info("Link offline: " + parameter);
                return decryptedLinks;
            }
            if (externID != null && !externID.contains("amateurgalore.net/")) {
                decryptedLinks.add(createDownloadlink(externID));
                return decryptedLinks;
            }
        }
        filename = br.getRegex("<meta name=\"DC\\.title\" content=\"([^<>\"]*?) \\- Amateur Porn \\- AmateurGalore \\- Free Amateur Porn\"").getMatch(0);
        if (filename != null) filename = Encoding.htmlDecode(filename.trim());
        externID = br.getRegex("\"http://videobam\\.com/widget/(.*?)/custom").getMatch(0);
        if (externID != null) {
            DownloadLink dl = createDownloadlink("http://videobam.com/videos/download/" + externID);
            decryptedLinks.add(dl);
            return decryptedLinks;
        }
        externID = br.getRegex("name=\"FlashVars\" value=\"options=(http://(www\\.)keezmovies\\.com/.*?)\"").getMatch(0);
        if (externID != null) {
            br.getPage(externID);
            String finallink = br.getRegex("<flv_url>(http://.*?)</flv_url>").getMatch(0);
            if (finallink == null) {
                logger.warning("Decrypter broken for link: " + parameter);
                return null;
            }
            if (filename == null) {
                logger.warning("Decrypter broken for link:" + parameter);
                return null;
            }
            DownloadLink dl = createDownloadlink("directhttp://" + finallink);
            dl.setFinalFileName(filename + ".flv");
            decryptedLinks.add(dl);
            return decryptedLinks;
        }
        externID = br.getRegex("movie_id=(\\d+)").getMatch(0);
        if (externID != null) {
            DownloadLink dl = createDownloadlink("http://www.pornrabbit.com/" + externID + "/bla.html");
            decryptedLinks.add(dl);
            return decryptedLinks;
        }
        externID = br.getRegex("id_video=(\\d+)\"").getMatch(0);
        if (externID == null) externID = br.getRegex("xvideos\\.com/embedframe/(\\d+)\"").getMatch(0);
        if (externID != null) {
            decryptedLinks.add(createDownloadlink("http://www.xvideos.com/video" + externID));
            return decryptedLinks;
        }
        externID = br.getRegex("\"(http://(www\\.)?flashservice\\.xvideos\\.com/embedframe/\\d+)\"").getMatch(0);
        if (externID != null) {
            decryptedLinks.add(createDownloadlink(externID));
            return decryptedLinks;
        }
        externID = br.getRegex("\"(http://(www\\.)?tube8\\.com/embed/[^<>\"/]*?/[^<>\"/]*?/\\d+/?)\"").getMatch(0);
        if (externID != null) {
            decryptedLinks.add(createDownloadlink(externID.replace("tube8.com/embed", "tube8.com/")));
            return decryptedLinks;
        }
        externID = br.getRegex("redtube\\.com/player/\"><param name=\"FlashVars\" value=\"id=(\\d+)\\&").getMatch(0);
        if (externID == null) externID = br.getRegex("embed\\.redtube\\.com/player/\\?id=(\\d+)\\&").getMatch(0);
        if (externID != null) {
            DownloadLink dl = createDownloadlink("http://www.redtube.com/" + externID);
            decryptedLinks.add(dl);
            return decryptedLinks;
        }
        // empflix.com 1
        externID = br.getRegex("player\\.empflix\\.com/video/(\\d+)\"").getMatch(0);
        if (externID != null) {
            decryptedLinks.add(createDownloadlink("http://www.empflix.com/videos/" + System.currentTimeMillis() + "-" + externID + ".html"));
            return decryptedLinks;
        }
        // empflix.com 2
        externID = br.getRegex("empflix\\.com/embedding_player/player[^<>\"/]*?\\.swf\".*?value=\"config=embedding_feed\\.php\\?viewkey=([^<>\"]*?)\"").getMatch(0);
        if (externID != null) {
            // Find original empflix link and add it to the list
            br.getPage("http://www.empflix.com/embedding_player/embedding_feed.php?viewkey=" + externID);
            String finallink = br.getRegex("<link>(http://.*?)</link>").getMatch(0);
            if (finallink == null) {
                logger.warning("decrypter broken for link: " + parameter);
                return null;
            }
            decryptedLinks.add(createDownloadlink(Encoding.htmlDecode(finallink)));
            return decryptedLinks;
        }
        // Can't be played/downloaded
        externID = br.getRegex("config=(http://(www\\.)?freudbox\\.com/video/flv/[A-Za-z0-9\\-_]+/config/)\"").getMatch(0);
        if (externID != null) {
            logger.info("Link broken: " + parameter);
            return decryptedLinks;
        }
        if (br.containsHTML("(name=\"movie\" value=\"http://(www\\.)?megaporn\\.com/|name=\"movie\" value=\"http://video\\.megarotic\\.com/|<h3><center><a href=\"http://seemygf\\.com/vod/)")) {
            logger.info("Link offline: " + parameter);
            return decryptedLinks;
        }
        // Nothing there
        if (!br.containsHTML("id=\"video_extended\"")) {
            logger.info("Link broken: " + parameter);
            return decryptedLinks;
        }
        if (externID == null) {
            logger.warning("Decrypter broken for link: " + parameter);
            return null;
        }
        return decryptedLinks;
    }

    /* NO OVERRIDE!! */
    public boolean hasCaptcha(CryptedLink link, jd.plugins.Account acc) {
        return false;
    }

}