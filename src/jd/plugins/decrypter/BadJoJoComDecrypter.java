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

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "badjojo.com" }, urls = { "http://(www\\.)?badjojo\\.com/\\d+/.{1}" }, flags = { 0 })
public class BadJoJoComDecrypter extends PluginForDecrypt {

    public BadJoJoComDecrypter(PluginWrapper wrapper) {
        super(wrapper);
    }

    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        String parameter = param.toString();
        br.getPage(parameter);
        if ("http://www.badjojo.com/".equals(br.getRedirectLocation())) {
            final DownloadLink dl = createDownloadlink(parameter.replace("badjojo.com", "decryptedbadjojo.com"));
            dl.setAvailable(false);
            decryptedLinks.add(dl);
            return decryptedLinks;
        }
        br.setFollowRedirects(true);
        if (br.getRedirectLocation() != null) br.getPage(br.getRedirectLocation());
        String filename = br.getRegex("<title>(.*?)</title>").getMatch(0);
        String decrypted = null;
        String externID = br.getRegex("name=\"FlashVars\" value=\"id=(\\d+)\\&style=redtube\"").getMatch(0);
        if (externID == null) externID = br.getRegex("\"http://embed\\.redtube\\.com/player/\\?id=(\\d+)\\&style=").getMatch(0);
        if (externID != null) {
            decrypted = "http://www.redtube.com/" + externID;
            decryptedLinks.add(createDownloadlink(decrypted));
            return decryptedLinks;
        }
        externID = br.getRegex("freeviewmovies\\.com/flv/skin/ofconfig\\.php\\?id=(\\d+)\"").getMatch(0);
        if (externID != null) {
            decrypted = "http://www.freeviewmovies.com/porn/" + externID + "/blabla.html";
            decryptedLinks.add(createDownloadlink(decrypted));
            return decryptedLinks;
        }
        externID = br.getRegex("xvideos\\.com/embedframe/(\\d+)\"").getMatch(0);
        if (externID == null) externID = br.getRegex("value=\"http://(www\\.)?xvideos\\.com/sitevideos/.*?value=\"id_video=(\\d+)\"").getMatch(1);
        if (externID == null) externID = br.getRegex("static\\.xvideos\\.com/swf/flv_player_site_v\\d+\\.swf\" /><param name=\"allowFullScreen\" value=\"true\" /><param name=\"flashvars\" value=\"id_video=(\\d+)\"").getMatch(0);
        if (externID != null) {
            decrypted = "http://www.xvideos.com/video" + externID;
            decryptedLinks.add(createDownloadlink(decrypted));
            return decryptedLinks;
        }
        externID = br.getRegex("\"http://(www\\.)?cyberporn\\.com/embed/(\\d+)").getMatch(0);
        if (externID != null) {
            decryptedLinks.add(createDownloadlink("http://www.cyberporn.com/video/" + externID));
            return decryptedLinks;
        }
        externID = br.getRegex("stileproject\\.com/embed/(\\d+)").getMatch(0);
        if (externID != null) {
            decryptedLinks.add(createDownloadlink("http://stileproject.com/video/" + externID));
            return decryptedLinks;
        }
        externID = br.getRegex("pornhub\\.com/embed/(\\d+)").getMatch(0);
        if (externID == null) externID = br.getRegex("pornhub\\.com/view_video\\.php\\?viewkey=(\\d+)").getMatch(0);
        if (externID != null) {
            DownloadLink dl = createDownloadlink("http://www.pornhub.com/view_video.php?viewkey=" + externID);
            decryptedLinks.add(dl);
            return decryptedLinks;
        }
        externID = br.getRegex("embed\\.pornrabbit\\.com/player\\.swf\\?movie_id=(\\d+)\"").getMatch(0);
        if (externID == null) externID = br.getRegex("pornrabbit\\.com/embed/(\\d+)").getMatch(0);
        if (externID != null) {
            decryptedLinks.add(createDownloadlink("http://pornrabbit.com/video/" + externID + "/"));
            return decryptedLinks;
        }
        externID = br.getRegex("spankwire\\.com/EmbedPlayer\\.aspx\\?ArticleId=(\\d+)\"").getMatch(0);
        if (externID != null) {
            decryptedLinks.add(createDownloadlink("http://www.spankwire.com/" + System.currentTimeMillis() + "/video" + externID));
            return decryptedLinks;
        }
        externID = br.getRegex("dl\\.pornhost\\.com%2F0%2F\\d+%2F(\\d+)%2F").getMatch(0);
        if (externID != null) {
            decryptedLinks.add(createDownloadlink("http://pornhost.com/" + externID));
            return decryptedLinks;
        }
        externID = br.getRegex("(https?://www\\.keezmovies\\.com/embed_player\\.php\\?v?id=\\d+)").getMatch(0);
        if (externID != null) {
            decryptedLinks.add(createDownloadlink(externID));
            return decryptedLinks;
        }
        // youporn.com handling 1
        externID = br.getRegex("youporn\\.com/embed/(\\d+)").getMatch(0);
        if (externID != null) {
            final DownloadLink dl = createDownloadlink("http://www.youporn.com/watch/" + externID + "/" + System.currentTimeMillis());
            decryptedLinks.add(dl);
            return decryptedLinks;
        }
        externID = br.getRegex("player\\.tnaflix\\.com/video/(\\d+)\"").getMatch(0);
        if (externID != null) {
            decryptedLinks.add(createDownloadlink("http://www.tnaflix.com/cum-videos/" + System.currentTimeMillis() + "/video" + externID));
            return decryptedLinks;
        }
        externID = br.getRegex("\"(http://(www\\.)?xhamster\\.com/xembed\\.php\\?video=\\d+)\"").getMatch(0);
        if (externID != null) {
            decryptedLinks.add(createDownloadlink(externID));
            return decryptedLinks;
        }
        // filename needed for stuff below
        if (filename == null) {
            logger.warning("Decrypter broken for link: " + parameter);
            return null;
        }
        externID = br.getRegex("shufuni\\.com/Flash/.*?flashvars=\"VideoCode=(.*?)\"").getMatch(0);
        if (externID != null) {
            DownloadLink dl = createDownloadlink("http://www.shufuni.com/handlers/FLVStreamingv2.ashx?videoCode=" + externID);
            dl.setFinalFileName(Encoding.htmlDecode(filename.trim()));
            decryptedLinks.add(dl);
            return decryptedLinks;
        }
        // pornhub handling number 2
        externID = br.getRegex("name=\"FlashVars\" value=\"options=(http://(www\\.)?pornhub\\.com/embed_player(_v\\d+)?\\.php\\?id=\\d+)\"").getMatch(0);
        if (externID != null) {
            br.getPage(externID);
            if (br.containsHTML("<link_url>N/A</link_url>") || br.containsHTML("No htmlCode read")) {
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
        externID = br.getRegex("freeporn.com/swf/player/AppLauncher_secure\\.swf\\'><param.*?name=\\'flashvars\\' value=\\'file=([^<>\"]*?)\\&").getMatch(0);
        if (externID != null) {
            logger.info("Link offline: " + parameter);
            return decryptedLinks;
        }
        externID = br.getRegex("\"(http://(www\\.)?boysfood\\.com/embed/\\d+/?)\"").getMatch(0);
        if (externID != null) {
            br.getPage(externID);
            final String cb = br.getRegex("\\?cb=(\\d+)\\'").getMatch(0);
            if (cb == null) {
                logger.warning("Decrypter broken for link: " + parameter);
                return null;
            }
            final String postData = "jsonRequest=%7B%22returnType%22%3A%22json%22%2C%22file%22%3A%22%22%2C%22htmlHostDomain%22%3Anull%2C%22request%22%3A%22getAllData%22%2C%22playerOnly%22%3A%22true%22%2C%22loaderUrl%22%3A%22http%3A%2F%2Fcdn1%2Estatic%2Eatlasfiles%2Ecom%2Fplayer%2Fmemberplayer%2Eswf%3Fcb%3D" + cb + "%22%2C%22appdataurl%22%3A%22" + Encoding.urlEncode(externID) + "%22%2C%22path%22%3A%22%22%2C%22cb%22%3A%22" + cb + "%22%7D&cacheBuster=" + System.currentTimeMillis();
            br.postPage(externID, postData);
            externID = br.getRegex("\"file\": \"(http://[^<>\"]*?)\"").getMatch(0);
            if (externID == null) {
                logger.warning("Decrypter broken for link: " + parameter);
                return null;
            }
            final DownloadLink dl = createDownloadlink("directhttp://" + externID);
            dl.setFinalFileName(Encoding.htmlDecode(filename.trim()) + ".mp4");
            decryptedLinks.add(dl);
            return decryptedLinks;
        }
        // youporn.com handling 2
        externID = br.getRegex("flashvars=\"file=(http%3A%2F%2Fdownload\\.youporn\\.com[^<>\"]*?)\\&").getMatch(0);
        if (externID != null) {
            br.setCookie("http://youporn.com/", "age_verified", "1");
            br.setCookie("http://youporn.com/", "is_pc", "1");
            br.setCookie("http://youporn.com/", "language", "en");
            br.getPage(Encoding.htmlDecode(externID));
            if (br.getRequest().getHttpConnection().getResponseCode() == 404) {
                logger.warning("FourSexFourCom -> youporn link invalid, please check browser to confirm: " + parameter);
                return null;
            }
            if (br.containsHTML("download\\.youporn\\.com/agecheck")) {
                logger.info("Link broken or offline: " + parameter);
                return decryptedLinks;
            }
            externID = br.getRegex("\"(http://(www\\.)?download\\.youporn.com/download/\\d+/\\?xml=1)\"").getMatch(0);
            if (externID == null) {
                logger.warning("Decrypter broken for link: " + parameter);
                return null;
            }
            br.getPage(externID);
            final String finallink = br.getRegex("<location>(http://.*?)</location>").getMatch(0);
            if (finallink == null) {
                logger.warning("Decrypter broken for link: " + parameter);
                return null;
            }
            final DownloadLink dl = createDownloadlink("directhttp://" + Encoding.htmlDecode(finallink));
            String type = br.getRegex("<meta rel=\"type\">(.*?)</meta>").getMatch(0);
            if (type == null) type = "flv";
            dl.setFinalFileName(filename + "." + type);
            decryptedLinks.add(dl);
            return decryptedLinks;
        }
        externID = br.getRegex("\"(http://(www\\.)?freeviewmovies\\.com/embed/\\d+/)\"").getMatch(0);
        if (externID != null) {
            br.getPage(externID);
            final String cb = br.getRegex("\\?cb=(\\d+)\\'").getMatch(0);
            if (cb == null) {
                logger.warning("Decrypter broken for link: " + parameter);
                return null;
            }
            final String postData = "jsonRequest=%7B%22returnType%22%3A%22json%22%2C%22file%22%3A%22%22%2C%22htmlHostDomain%22%3Anull%2C%22request%22%3A%22getAllData%22%2C%22playerOnly%22%3A%22true%22%2C%22loaderUrl%22%3A%22http%3A%2F%2Fcdn1%2Estatic%2Eatlasfiles%2Ecom%2Fplayer%2Fmemberplayer%2Eswf%3Fcb%3D" + cb + "%22%2C%22appdataurl%22%3A%22" + Encoding.urlEncode(externID) + "%22%2C%22path%22%3A%22%22%2C%22cb%22%3A%22" + cb + "%22%7D&cacheBuster=" + System.currentTimeMillis();
            br.postPage(externID, postData);
            externID = br.getRegex("\"file\": \"(http://[^<>\"]*?)\"").getMatch(0);
            if (externID == null) {
                logger.warning("Decrypter broken for link: " + parameter);
                return null;
            }
            final DownloadLink dl = createDownloadlink("directhttp://" + externID);
            dl.setFinalFileName(Encoding.htmlDecode(filename.trim()) + ".mp4");
            decryptedLinks.add(dl);
            return decryptedLinks;
        }
        externID = br.getRegex("\"(http://(www\\.)?gayjojo\\.com/embed/\\d+/[a-z0-9\\-]+)\"").getMatch(0);
        if (externID != null) {
            logger.info("Link offline because extern link must be offline: " + externID);
            logger.info("Original link: " + parameter);
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
            if (br.containsHTML(">Sorry, this video is no longer available")) {
                logger.info("Link offline: " + parameter);
                return decryptedLinks;
            }
            final String finallink = br.getRegex("<link>(http://.*?)</link>").getMatch(0);
            if (finallink == null) {
                logger.warning("decrypter broken for link: " + parameter);
                return null;
            }
            decryptedLinks.add(createDownloadlink(Encoding.htmlDecode(finallink)));
            return decryptedLinks;
        }
        externID = br.getRegex("MoviesAnd\\.com/embedded/(\\d+)\"").getMatch(0);
        if (externID != null) {
            decryptedLinks.add(createDownloadlink("http://www.moviesand.com/videos/" + externID + "/" + System.currentTimeMillis() + ".html"));
            return decryptedLinks;
        }
        // 2nd handling for tnaflix
        externID = br.getRegex("tnaflix\\.com/embedding_player/player_[^<>\"]+\\.swf.*?config=(embedding_feed\\.php\\?viewkey=[a-z0-9]+)").getMatch(0);
        if (externID != null) {
            br.getPage("http://www.tnaflix.com/embedding_player/" + externID);
            externID = br.getRegex("start_thumb>http://static\\.tnaflix\\.com/thumbs/[a-z0-9\\-_]+/[a-z0-9]+_(\\d+)l\\.jpg<").getMatch(0);
            if (externID != null) {
                decryptedLinks.add(createDownloadlink("http://www.tnaflix.com/cum-videos/" + System.currentTimeMillis() + "/video" + externID));
                return decryptedLinks;
            }
        }
        decrypted = parameter.replace("badjojo.com", "decryptedbadjojo.com");
        decryptedLinks.add(createDownloadlink(decrypted));
        return decryptedLinks;
    }

    /* NO OVERRIDE!! */
    public boolean hasCaptcha(CryptedLink link, jd.plugins.Account acc) {
        return false;
    }

}