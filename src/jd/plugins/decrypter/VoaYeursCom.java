//jDownloader - Downloadmanager
//Copyright (C) 2013  JD-Team support@jdownloader.org
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
import jd.nutils.encoding.Encoding;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.PluginForDecrypt;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "voayeurs.com" }, urls = { "http://(www\\.)?voayeurs\\.com/(video_\\d+/.*?|.*?\\d+)\\.html" }, flags = { 0 })
public class VoaYeursCom extends PluginForDecrypt {

    public VoaYeursCom(PluginWrapper wrapper) {
        super(wrapper);
    }

    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        br.setFollowRedirects(false);
        String parameter = param.toString();
        br.getPage(parameter);
        String filename = br.getRegex("<title>([^<>\"]*?)\\- Porno HD \\- VOAYEURS\\.COM</title>").getMatch(0);
        if (filename == null) filename = br.getRegex("<h1>([^<>\"]*?)</h1>").getMatch(0);
        String externID = br.getRedirectLocation();
        if (externID != null && externID.length() < 40 || br.containsHTML(">El video ha sido eliminado<")) {
            logger.info("Link offline: " + parameter);
            return decryptedLinks;
        }
        if (externID != null) {
            DownloadLink dl = createDownloadlink(externID);
            decryptedLinks.add(dl);
            return decryptedLinks;
        }
        externID = br.getRegex("src='https?://alotporn\\.com/show\\.php\\?id=(\\d+)[^']+").getMatch(0);
        if (externID != null) {
            DownloadLink dl = createDownloadlink("http://alotporn.com/" + externID);
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
        externID = br.getRegex("madthumbs\\.com%2Fvideos%2Fembed_config%3Fid%3D(\\d+)").getMatch(0);
        if (externID != null) {
            DownloadLink dl = createDownloadlink("http://www.madthumbs.com/videos/amateur/" + new Random().nextInt(100000) + "/" + externID);
            decryptedLinks.add(dl);
            return decryptedLinks;
        }
        externID = br.getRegex("xvideos\\.com/embedframe/(\\d+)\"").getMatch(0);
        if (externID == null) externID = br.getRegex("static\\.xvideos\\.com/swf/.*?flashvars=\"id_video=(\\d+)\"").getMatch(0);
        if (externID == null) externID = br.getRegex("xvideos\\.com/swf/flv_player_site_v\\d+\\.swf\" /><param name=\"allowFullScreen\" value=\"true\" /><param name=\"flashvars\" value=\"id_video=(\\d+)\"").getMatch(0);
        if (externID != null) {
            decryptedLinks.add(createDownloadlink("http://www.xvideos.com/video" + externID));
            return decryptedLinks;
        }
        externID = br.getRegex("\"(http://(www\\.)?tube8\\.com/embed/[^<>\"]*?)\"").getMatch(0);
        if (externID != null) {
            decryptedLinks.add(createDownloadlink(externID));
            return decryptedLinks;
        }
        externID = br.getRegex("(http://(www\\.)?drtuber\\.com/player/config_embed3\\.php\\?vkey=[a-z0-9]+)").getMatch(0);
        if (externID != null) {
            decryptedLinks.add(createDownloadlink(externID));
            return decryptedLinks;
        }
        // drtuber handling 2
        externID = br.getRegex("\"(http://(www\\.)?drtuber\\.com/embed/\\d+)\"").getMatch(0);
        if (externID != null) {
            decryptedLinks.add(createDownloadlink(externID));
            return decryptedLinks;
        }
        externID = br.getRegex("emb\\.slutload\\.com/([A-Za-z0-9]+)\"").getMatch(0);
        if (externID != null) {
            decryptedLinks.add(createDownloadlink("http://slutload.com/watch/" + externID));
            return decryptedLinks;
        }
        externID = br.getRegex("pornerbros\\.com/content/(\\d+)\\.xml").getMatch(0);
        if (externID != null) {
            decryptedLinks.add(createDownloadlink("http://www.pornerbros.com/" + externID + "/" + System.currentTimeMillis() + ".html"));
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

        externID = br.getRegex("hardsextube\\.com/embed/(\\d+)/\"").getMatch(0);
        if (externID != null) {
            decryptedLinks.add(createDownloadlink("http://www.hardsextube.com/video/" + externID + "/"));
            return decryptedLinks;
        }
        externID = br.getRegex("embed\\.pornrabbit\\.com/player\\.swf\\?movie_id=(\\d+)\"").getMatch(0);
        if (externID == null) externID = br.getRegex("pornrabbit\\.com/embed/(\\d+)").getMatch(0);
        if (externID != null) {
            decryptedLinks.add(createDownloadlink("http://pornrabbit.com/video/" + externID + "/"));
            return decryptedLinks;
        }
        externID = br.getRegex("xhamster\\.com/xembed\\.php\\?video=(\\d+)\"").getMatch(0);
        if (externID != null) {
            decryptedLinks.add(createDownloadlink("http://xhamster.com/movies/" + externID + "/" + System.currentTimeMillis() + ".html"));
            return decryptedLinks;
        }
        externID = br.getRegex("pornhub\\.com/embed/(\\d+)").getMatch(0);
        if (externID == null) externID = br.getRegex("pornhub\\.com/view_video\\.php\\?viewkey=(\\d+)").getMatch(0);
        if (externID != null) {
            DownloadLink dl = createDownloadlink("http://www.pornhub.com/view_video.php?viewkey=" + externID);
            decryptedLinks.add(dl);
            return decryptedLinks;
        }
        // youporn.com handling 1
        externID = br.getRegex("youporn\\.com/embed/(\\d+)").getMatch(0);
        if (externID != null) {
            final DownloadLink dl = createDownloadlink("http://www.youporn.com/watch/" + externID + "/" + System.currentTimeMillis());
            decryptedLinks.add(dl);
            return decryptedLinks;
        }
        externID = br.getRegex("(http://(www\\.)?mofosex\\.com/embed_player\\.php\\?id=\\d+)\"").getMatch(0);
        if (externID != null) {
            br.getPage(externID);
            if (br.containsHTML("No htmlCode read")) {
                logger.info("Link offline: " + parameter);
                return decryptedLinks;
            }
            externID = br.getRegex("<click_tag>(http://(www\\.)?mofosex\\.com/videos/\\d+/[^<>\"]*?)</click_tag>").getMatch(0);
            if (externID == null) {
                logger.warning("Decrypter broken for link: " + parameter);
                return null;
            }
            final DownloadLink dl = createDownloadlink(externID);
            decryptedLinks.add(dl);
            return decryptedLinks;
        }
        externID = br.getRegex("\"(http://(www\\.)?fux\\.com/embed/\\d+)\"").getMatch(0);
        if (externID != null) {
            decryptedLinks.add(createDownloadlink(externID));
            return decryptedLinks;
        }
        externID = br.getRegex("\"(http://(www\\.)?vporn\\.com/embed/\\d+)\"").getMatch(0);
        if (externID != null) {
            decryptedLinks.add(createDownloadlink(externID));
            return decryptedLinks;
        }
        externID = br.getRegex("moviefap\\.com/embedding_player/player.*?value=\"config=(embedding_feed\\.php\\?viewkey=[a-z0-9]+)\"").getMatch(0);
        if (externID != null) {
            decryptedLinks.add(createDownloadlink("http://www.moviefap.com/embedding_player/" + externID));
            return decryptedLinks;
        }
        externID = br.getRegex("%26link%3D(videos[^<>\"]*?)%26splash%3D").getMatch(0);
        if (externID != null) {
            decryptedLinks.add(createDownloadlink("http://www.madthumbs.com/" + Encoding.htmlDecode(externID)));
            return decryptedLinks;
        }
        externID = br.getRegex("\"(http://(www\\.)?embed\\.porntube\\.com/\\d+)\"").getMatch(0);
        if (externID != null) {
            decryptedLinks.add(createDownloadlink(externID));
            return decryptedLinks;
        }
        // For all following ids, a filename is needed
        if (filename == null) {
            logger.warning("Decrypter broken for link: " + parameter);
            return null;
        }
        filename = Encoding.htmlDecode(filename.trim());
        externID = br.getRegex("foxytube\\.com/embedded/(\\d+)\"").getMatch(0);
        if (externID != null) {
            br.getPage("http://www.foxytube.com/embconfig/" + externID + "/");
            externID = br.getRegex("<file>(http://[^<>\"]*?)</file>").getMatch(0);
            if (externID == null) {
                logger.warning("Decrypter broken for link: " + parameter);
                return null;
            }
            final DownloadLink dl = createDownloadlink("directhttp://" + externID);
            dl.setFinalFileName(filename + ".flv");
            decryptedLinks.add(dl);
            return decryptedLinks;
        }
        externID = br.getRegex("name=\"FlashVars\" value=\"options=(http://(www\\.)?extremetube\\.com/embed_player\\.php\\?id=\\d+)\"").getMatch(0);
        if (externID != null) {
            br.getPage(externID);
            externID = br.getRegex("<flv_url>(http://[^<>\"]*?)</flv_url>").getMatch(0);
            if (externID == null) {
                logger.warning("Decrypter broken for link: " + parameter);
                return null;
            }
            final DownloadLink dl = createDownloadlink("directhttp://" + externID);
            dl.setFinalFileName(filename + ".flv");
            decryptedLinks.add(dl);
            return decryptedLinks;
        }
        externID = br.getRegex("flashvars=\"enablejs=true\\&autostart=(false|true)\\&mediaid=(\\d+)\\&").getMatch(1);
        if (externID != null) {
            br.getPage("http://www.deviantclip.com/playlists/" + externID + "/playlist.xml");
            String finallinkempflix = br.getRegex("<location>(http[^<>\"]*?)</location>").getMatch(0);
            if (finallinkempflix == null) {
                logger.warning("Couldn't decrypt link: " + parameter);
                return null;
            }
            final DownloadLink dl = createDownloadlink("directhttp://" + Encoding.htmlDecode(finallinkempflix));
            dl.setFinalFileName(filename + ".flv");
            decryptedLinks.add(dl);
            return decryptedLinks;
        }
        externID = br.getRegex("shufuni\\.com/Flash/.*?flashvars=\"VideoCode=(.*?)\"").getMatch(0);
        if (externID != null) {
            final DownloadLink dl = createDownloadlink("http://www.shufuni.com/handlers/FLVStreamingv2.ashx?videoCode=" + externID);
            dl.setFinalFileName(Encoding.htmlDecode(filename.trim()));
            decryptedLinks.add(dl);
            return decryptedLinks;
        }
        externID = br.getRegex("player\\.tnaflix\\.com/video/(\\d+)\"").getMatch(0);
        if (externID != null) {
            final DownloadLink dl = createDownloadlink("http://www.tnaflix.com/teen-porn/" + System.currentTimeMillis() + "/video" + externID);
            dl.setFinalFileName(Encoding.htmlDecode(filename.trim()) + ".flv");
            decryptedLinks.add(dl);
            return decryptedLinks;
        }
        externID = br.getRegex("tnaflix\\.com/embedding_player/player_[^<>\"]+\\.swf\".*?value=\"config=(embedding_feed\\.php\\?viewkey=[a-z0-9]+)\"").getMatch(0);
        if (externID != null) {
            br.getPage("http://www.tnaflix.com/embedding_player/" + externID);
            externID = br.getRegex("start_thumb>http://static\\.tnaflix\\.com/thumbs/[a-z0-9\\-_]+/[a-z0-9]+_(\\d+)l\\.jpg<").getMatch(0);
            if (externID != null) {
                decryptedLinks.add(createDownloadlink("http://www.tnaflix.com/cum-videos/" + System.currentTimeMillis() + "/video" + externID));
                return decryptedLinks;
            }
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
        externID = br.getRegex("freeporn\\.com/swf/player/AppLauncher_secure\\.swf(\"|\\')>.*?<param name=(\"|\\')flashvars(\"|\\') value=(\"|\\')file=([^<>\"]*?)\\&").getMatch(4);
        if (externID == null) externID = br.getRegex("\"(http://(www\\.)?freeporn\\.com/embed/\\d+/?)\"").getMatch(0);
        if (externID != null) {
            logger.info("Link offline: " + parameter);
            return decryptedLinks;
        }
        // youporn.com handling 2 (was never needed yet)
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
            final String finallinkyouporn = br.getRegex("<location>(http://.*?)</location>").getMatch(0);
            if (finallinkyouporn == null) {
                logger.warning("Decrypter broken for link: " + parameter);
                return null;
            }
            final DownloadLink dl = createDownloadlink("directhttp://" + Encoding.htmlDecode(finallinkyouporn));
            String type = br.getRegex("<meta rel=\"type\">(.*?)</meta>").getMatch(0);
            if (type == null) type = "flv";
            dl.setFinalFileName(filename + "." + type);
            decryptedLinks.add(dl);
            return decryptedLinks;
        }
        externID = br.getRegex("(http://(www\\.)?keezmovies\\.com/embed_player\\.php\\?v?id=\\d+)\"").getMatch(0);
        if (externID != null) {
            br.getPage(externID);
            externID = br.getRegex("<share>(http://[^<>\"]*?)</share>").getMatch(0);
            if (externID != null) {
                final DownloadLink dl = createDownloadlink(externID);
                decryptedLinks.add(dl);
                return decryptedLinks;
            } else {
                externID = br.getRegex("<flv_url>(http://[^<>\"]*?)</flv_url>").getMatch(0);
                if (externID == null) {
                    logger.warning("Decrypter broken for link: " + parameter);
                    return null;
                }
                final DownloadLink dl = createDownloadlink("directhttp://" + Encoding.htmlDecode(externID));
                dl.setFinalFileName(filename + ".flv");
                decryptedLinks.add(dl);
                return decryptedLinks;
            }
        }
        /* Find random directlinks */
        externID = br.getRegex("\"(https?://[^<>\"]*?\\.(mp4|flv))\"").getMatch(0);
        if (externID != null) {
            final DownloadLink dl = createDownloadlink("directhttp://" + Encoding.htmlDecode(externID));
            dl.setFinalFileName(filename + externID.substring(externID.lastIndexOf(".")));
            decryptedLinks.add(dl);
            return decryptedLinks;
        }
        if (externID == null) {
            logger.warning("Couldn't decrypt link: " + parameter);
            return null;
        }
        return decryptedLinks;
    }

    /* NO OVERRIDE!! */
    public boolean hasCaptcha(CryptedLink link, jd.plugins.Account acc) {
        return false;
    }

}