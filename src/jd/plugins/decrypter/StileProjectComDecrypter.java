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
import jd.nutils.encoding.Encoding;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.PluginForDecrypt;

//EmbedDecrypter 0.1.3
@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "stileproject.com" }, urls = { "http://(www\\.)?stileproject\\.com/video/\\d+" }, flags = { 0 })
public class StileProjectComDecrypter extends PluginForDecrypt {

    public StileProjectComDecrypter(PluginWrapper wrapper) {
        super(wrapper);
    }

    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        String parameter = param.toString();
        br.setFollowRedirects(true);
        br.getPage(parameter);
        final DownloadLink mainlink = createDownloadlink(parameter.replace("stileproject.com/", "stileprojectdecrypted.com/"));
        String filename = br.getRegex("<title>([^<>\"]*?) \\- StileProject\\.com</title>").getMatch(0);
        String externID = br.getRegex("xvideos\\.com/embedframe/(\\d+)\"").getMatch(0);
        if (externID != null) {
            decryptedLinks.add(createDownloadlink("http://www.xvideos.com/video" + externID));
            return decryptedLinks;
        }
        externID = br.getRegex("madthumbs\\.com%2Fvideos%2Fembed_config%3Fid%3D(\\d+)").getMatch(0);
        if (externID != null) {
            DownloadLink dl = createDownloadlink("http://www.madthumbs.com/videos/amateur/" + new Random().nextInt(100000) + "/" + externID);
            decryptedLinks.add(dl);
            return decryptedLinks;
        }
        externID = br.getRegex("(\"|\\')(http://(www\\.)?tube8\\.com/embed/[^<>\"/]*?/[^<>\"/]*?/\\d+/?)(\"|\\')").getMatch(1);
        if (externID != null) {
            decryptedLinks.add(createDownloadlink(externID.replace("tube8.com/embed/", "tube8.com/")));
            return decryptedLinks;
        }
        externID = br.getRegex("redtube\\.com/player/\"><param name=\"FlashVars\" value=\"id=(\\d+)\\&").getMatch(0);
        if (externID == null) externID = br.getRegex("embed\\.redtube\\.com/player/\\?id=(\\d+)\\&").getMatch(0);
        if (externID != null) {
            final DownloadLink dl = createDownloadlink("http://www.redtube.com/" + externID);
            decryptedLinks.add(dl);
            return decryptedLinks;
        }
        // drtuber.com embed v3
        externID = br.getRegex("(http://(www\\.)?drtuber\\.com/player/config_embed3\\.php\\?vkey=[a-z0-9]+)").getMatch(0);
        if (externID != null) {
            decryptedLinks.add(createDownloadlink(externID));
            return decryptedLinks;
        }
        // drtuber.com embed v4
        externID = br.getRegex("\"(http://(www\\.)?drtuber\\.com/embed/\\d+)\"").getMatch(0);
        if (externID != null) {
            decryptedLinks.add(createDownloadlink(externID));
            return decryptedLinks;
        }
        externID = br.getRegex("\"(http://(www\\.)?xhamster\\.com/xembed\\.php\\?video=\\d+)\"").getMatch(0);
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
        externID = br.getRegex("hardsextube\\.com/embed/(\\d+)/").getMatch(0);
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
        externID = br.getRegex("player\\.tnaflix\\.com/video/(\\d+)\"").getMatch(0);
        if (externID != null) {
            decryptedLinks.add(createDownloadlink("http://www.tnaflix.com/cum-videos/" + System.currentTimeMillis() + "/video" + externID));
            return decryptedLinks;
        }
        externID = br.getRegex("metacafe\\.com/fplayer/(\\d+)/").getMatch(0);
        if (externID != null) {
            decryptedLinks.add(createDownloadlink("http://www.metacafe.com/watch/" + externID + "/" + System.currentTimeMillis()));
            return decryptedLinks;
        }
        externID = br.getRegex("pornhub\\.com/embed/(\\d+)").getMatch(0);
        if (externID == null) externID = br.getRegex("pornhub\\.com/view_video\\.php\\?viewkey=(\\d+)").getMatch(0);
        if (externID != null) {
            DownloadLink dl = createDownloadlink("http://www.pornhub.com/view_video.php?viewkey=" + externID);
            decryptedLinks.add(dl);
            return decryptedLinks;
        }
        // pornhub handling number 2
        externID = br.getRegex("name=\"FlashVars\" value=\"options=(http://(www\\.)?pornhub\\.com/embed_player(_v\\d+)?\\.php\\?id=\\d+)\"").getMatch(0);
        if (externID != null) {
            br.getPage(externID);
            if (br.containsHTML("<link_url>N/A</link_url>") || br.containsHTML("No htmlCode read") || br.containsHTML(">404 Not Found<")) {
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
        // myxvids.com 1
        externID = br.getRegex("\"(http://(www\\.)?myxvids\\.com/embed/\\d+)\"").getMatch(0);
        if (externID != null) {
            decryptedLinks.add(createDownloadlink(externID));
            return decryptedLinks;
        }
        // myxvids.com 2
        externID = br.getRegex("(\\'|\")(http://(www\\.)?myxvids\\.com/embed_code/\\d+/\\d+/myxvids_embed\\.js)(\\'|\")").getMatch(1);
        if (externID != null) {
            br.getPage(externID);
            final String finallink = br.getRegex("\"(http://(www\\.)?myxvids\\.com/embed/\\d+)\"").getMatch(0);
            if (finallink == null) {
                logger.warning("Decrypter broken for link: " + parameter);
                return null;
            }
            decryptedLinks.add(createDownloadlink(finallink));
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
        externID = br.getRegex("<iframe src=\"http://(www\\.)?yobt\\.tv/embed/(\\d+)\\.html\"").getMatch(1);
        if (externID != null) {
            decryptedLinks.add(createDownloadlink("http://www.yobt.tv/content/" + externID + "/" + System.currentTimeMillis() + ".html"));
            return decryptedLinks;
        }
        externID = br.getRegex("book\\-mark\\.net/playerconfig/(\\d+)/").getMatch(0);
        if (externID != null) {
            DownloadLink dl = createDownloadlink("http://www.book-mark.net/videos/" + externID + "/x.html");
            decryptedLinks.add(dl);
            return decryptedLinks;
        }
        externID = br.getRegex("\"(http://(www\\.)?deviantclip\\.com/watch/[^<>\"]*?)\"").getMatch(0);
        if (externID != null) {
            decryptedLinks.add(createDownloadlink(externID));
            return decryptedLinks;
        }
        externID = br.getRegex("webdata\\.vidz\\.com/demo/swf/FlashPlayerV2\\.swf\".*?flashvars=\"id_scene=(\\d+)").getMatch(0);
        if (externID != null) {
            decryptedLinks.add(createDownloadlink("http://www.vidz.com/video/" + System.currentTimeMillis() + "/vidz_porn_videos/?s=" + externID));
            return decryptedLinks;
        }
        // youporn.com handling 1
        externID = br.getRegex("youporn\\.com/embed/(\\d+)").getMatch(0);
        if (externID != null) {
            final DownloadLink dl = createDownloadlink("http://www.youporn.com/watch/" + externID + "/" + System.currentTimeMillis());
            decryptedLinks.add(dl);
            return decryptedLinks;
        }
        externID = br.getRegex("pornative\\.com/embed/player\\.swf\\?movie_id=(\\d+)").getMatch(0);
        if (externID != null) {
            DownloadLink dl = createDownloadlink("http://pornative.com/" + externID + ".html");
            decryptedLinks.add(dl);
            return decryptedLinks;
        }
        externID = br.getRegex("pornyeah\\.com/playerConfig\\.php\\?[a-z0-9]+\\.[a-z0-9\\.]+\\|(\\d+)").getMatch(0);
        if (externID != null) {
            DownloadLink dl = createDownloadlink("http://www.pornyeah.com/videos/" + Integer.toString(new Random().nextInt(1000000)) + "-" + externID + ".html");
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
        externID = br.getRegex("\"(http://(www\\.)?xrabbit\\.com/video/embed/[A-Za-z0-9=]+/?)\"").getMatch(0);
        if (br.containsHTML("\\&file=http://embed\\.kickassratios\\.com/")) {
            logger.info("Link offline: " + parameter);
            return decryptedLinks;
        }
        if (externID != null) {
            decryptedLinks.add(createDownloadlink(externID));
            return decryptedLinks;
        }
        externID = br.getRegex("\"(http://(www\\.)?nuvid\\.com/embed/\\d+)\"").getMatch(0);
        if (externID != null) {
            decryptedLinks.add(createDownloadlink(externID));
            return decryptedLinks;
        }
        externID = br.getRegex("\"(http://(www\\.)?youjizz\\.com/videos/embed/\\d+)\"").getMatch(0);
        if (externID != null) {
            decryptedLinks.add(createDownloadlink(externID));
            return decryptedLinks;
        }
        externID = br.getRegex("\"(http://(www\\.)?vporn\\.com/embed/\\d+)\"").getMatch(0);
        if (externID != null) {
            decryptedLinks.add(createDownloadlink(externID));
            return decryptedLinks;
        }
        externID = br.getRegex("\"(http://(www\\.)?bangyoulater\\.com/embed\\.php\\?id=\\d+)\"").getMatch(0);
        if (externID != null) {
            decryptedLinks.add(createDownloadlink(externID));
            return decryptedLinks;
        }
        externID = br.getRegex("\"(http://(www\\.)?pornhost\\.com/(embed/)?\\d+)\"").getMatch(0);
        if (externID != null) {
            decryptedLinks.add(createDownloadlink(externID));
            return decryptedLinks;
        }
        externID = br.getRegex("\"(http://(www\\.)?spankwire\\.com/EmbedPlayer\\.aspx\\?ArticleId=\\d+)\"").getMatch(0);
        if (externID != null) {
            decryptedLinks.add(createDownloadlink(externID));
            return decryptedLinks;
        }
        // filename needed for all IDs below here
        if (filename == null) {
            decryptedLinks.add(mainlink);
            return decryptedLinks;
        }
        filename = Encoding.htmlDecode(filename.trim());
        externID = br.getRegex("shufuni\\.com/Flash/.*?flashvars=\"VideoCode=(.*?)\"").getMatch(0);
        if (externID != null) {
            DownloadLink dl = createDownloadlink("http://www.shufuni.com/handlers/FLVStreamingv2.ashx?videoCode=" + externID);
            dl.setFinalFileName(Encoding.htmlDecode(filename.trim()));
            decryptedLinks.add(dl);
            return decryptedLinks;
        }
        externID = br.getRegex("src=\"http://videos\\.allelitepass\\.com/txc/([^<>\"/]*?)\\.swf\"").getMatch(0);
        if (externID != null) {
            br.getPage("http://videos.allelitepass.com/txc/player.php?video=" + Encoding.htmlDecode(externID));
            externID = br.getRegex("<file>(http://[^<>\"]*?)</file>").getMatch(0);
            if (externID != null) {
                final DownloadLink dl = createDownloadlink("directhttp://" + externID);
                dl.setFinalFileName(filename + ".flv");
                decryptedLinks.add(dl);
                return decryptedLinks;
            }

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
        externID = br.getRegex("(http://(www\\.)?gasxxx\\.com/media/player/config_embed\\.php\\?vkey=\\d+)\"").getMatch(0);
        if (externID != null) {
            br.getPage(externID);
            externID = br.getRegex("<src>(http://[^<>\"]*?\\.flv)</src>").getMatch(0);
            if (externID == null) {
                logger.warning("Decrypter broken for link: " + parameter);
                return null;
            }
            final DownloadLink dl = createDownloadlink("directhttp://" + externID);
            dl.setFinalFileName(filename + ".flv");
            decryptedLinks.add(dl);
            return decryptedLinks;
        }
        externID = br.getRegex("(https?://www\\.keezmovies\\.com/embed_player\\.php\\?v?id=\\d+)").getMatch(0);
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
        decryptedLinks.add(mainlink);
        return decryptedLinks;
    }

}
