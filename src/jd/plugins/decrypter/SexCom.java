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

import java.io.IOException;
import java.util.ArrayList;
import java.util.Random;

import jd.PluginWrapper;
import jd.controlling.ProgressController;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterException;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.PluginForDecrypt;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "sex.com" }, urls = { "http://(www\\.)?sex\\.com/(pin/\\d+/|picture/\\d+|video/\\d+)" }, flags = { 0 })
public class SexCom extends PluginForDecrypt {

    public SexCom(PluginWrapper wrapper) {
        super(wrapper);
    }

    private static final String TYPE_VIDEO     = "http://(www\\.)?sex\\.com/video/\\d+";

    ArrayList<DownloadLink>     decryptedLinks = new ArrayList<DownloadLink>();
    private String              PARAMETER      = null;

    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        PARAMETER = param.toString().replace("/pin/", "/picture/");
        br.setFollowRedirects(true);
        String externID;
        String filename;
        br.getPage(PARAMETER);
        if (br.getHttpConnection().getResponseCode() == 404) {
            final DownloadLink offline = createDownloadlink("directhttp://" + PARAMETER);
            offline.setAvailable(false);
            offline.setProperty("offline", true);
            decryptedLinks.add(offline);
            return decryptedLinks;
        }
        if (PARAMETER.matches(TYPE_VIDEO)) {
            this.findLink();
        } else {
            filename = br.getRegex("<title>([^<>\"]*?) \\| Sex Videos and Pictures \\| Sex\\.com</title>").getMatch(0);
            if (filename == null || filename.length() <= 2) {
                filename = br.getRegex("addthis:title=\"([^<>\"]*?)\"").getMatch(0);
            }
            if (filename == null || filename.length() <= 2) {
                filename = br.getRegex("property=\"og:title\" content=\"([^<>]*?)\\-  Pin #\\d+ \\| Sex\\.com\"").getMatch(0);
            }
            if (filename == null || filename.length() <= 2) {
                filename = br.getRegex("<div class=\"pin\\-header navbar navbar\\-static\\-top\">[\t\n\r ]+<div class=\"navbar\\-inner\">[\t\n\r ]+<h1>([^<>]*?)</h1>").getMatch(0);
            }
            if (filename == null || filename.length() <= 2) {
                filename = new Regex(PARAMETER, "(\\d+)/?$").getMatch(0);
            }
            filename = Encoding.htmlDecode(filename.trim());
            filename = filename.replace("#", "");
            externID = br.getRegex("<div class=\"from\">From <a rel=\"nofollow\" href=\"(http://[^<>\"]*?)\"").getMatch(0);
            if (externID != null) {
                decryptedLinks.add(createDownloadlink(externID));
                return decryptedLinks;
            }
            externID = br.getRegex("<link rel=\"image_src\" href=\"(http[^<>\"]*?)\"").getMatch(0);
            // For .gif images
            if (externID == null) {
                externID = br.getRegex("<div class=\"image_frame\">[\t\n\r ]+<img alt=\"\" title=\"\" src=\"(http://[^<>\"]*?)\"").getMatch(0);
            }
            if (externID != null) {
                DownloadLink dl = createDownloadlink("directhttp://" + externID);
                dl.setFinalFileName(filename + externID.substring(externID.lastIndexOf(".")));
                decryptedLinks.add(dl);
                return decryptedLinks;
            }
            if (externID == null) {
                logger.warning("Decrypter broken for link: " + PARAMETER);
                throw new DecrypterException("Decrypter broken for link: " + PARAMETER);
            }
        }
        return decryptedLinks;
    }

    private void findLink() throws DecrypterException, IOException {
        String filename = br.getRegex("itemprop=\"name\">([^<>\"]*?)</span>").getMatch(0);
        // xvideos.com 1
        String externID = br.getRegex("xvideos\\.com/embedframe/(\\d+)\"").getMatch(0);
        if (externID != null) {
            decryptedLinks.add(createDownloadlink("http://www.xvideos.com/video" + externID));
            return;
        }
        // xvideos.com 2
        externID = br.getRegex("\"(http://(www\\.)?flashservice\\.xvideos\\.com/embedframe/\\d+)\"").getMatch(0);
        if (externID != null) {
            decryptedLinks.add(createDownloadlink(externID));
            return;
        }
        externID = br.getRegex("madthumbs\\.com%2Fvideos%2Fembed_config%3Fid%3D(\\d+)").getMatch(0);
        if (externID != null) {
            DownloadLink dl = createDownloadlink("http://www.madthumbs.com/videos/amateur/" + new Random().nextInt(100000) + "/" + externID);
            decryptedLinks.add(dl);
            return;
        }
        externID = br.getRegex("(\"|\\')(http://(www\\.)?tube8\\.com/embed/[^<>\"/]*?/[^<>\"/]*?/\\d+/?)(\"|\\')").getMatch(1);
        if (externID != null) {
            decryptedLinks.add(createDownloadlink(externID.replace("tube8.com/embed/", "tube8.com/")));
            return;
        }
        externID = br.getRegex("redtube\\.com/player/\"><param name=\"FlashVars\" value=\"id=(\\d+)\\&").getMatch(0);
        if (externID == null) {
            externID = br.getRegex("embed\\.redtube\\.com/player/\\?id=(\\d+)\\&").getMatch(0);
        }
        if (externID != null) {
            final DownloadLink dl = createDownloadlink("http://www.redtube.com/" + externID);
            decryptedLinks.add(dl);
            return;
        }
        // drtuber.com embed v3
        externID = br.getRegex("(http://(www\\.)?drtuber\\.com/player/config_embed3\\.php\\?vkey=[a-z0-9]+)").getMatch(0);
        if (externID != null) {
            decryptedLinks.add(createDownloadlink(externID));
            return;
        }
        // drtuber.com embed v4
        externID = br.getRegex("\"(http://(www\\.)?drtuber\\.com/embed/\\d+)\"").getMatch(0);
        if (externID != null) {
            decryptedLinks.add(createDownloadlink(externID));
            return;
        }
        externID = br.getRegex("\"(http://(www\\.)?xhamster\\.com/xembed\\.php\\?video=\\d+)\"").getMatch(0);
        if (externID != null) {
            decryptedLinks.add(createDownloadlink(externID));
            return;
        }
        externID = br.getRegex("emb\\.slutload\\.com/([A-Za-z0-9]+)\"").getMatch(0);
        if (externID != null) {
            decryptedLinks.add(createDownloadlink("http://slutload.com/watch/" + externID));
            return;
        }
        externID = br.getRegex("pornerbros\\.com/content/(\\d+)\\.xml").getMatch(0);
        if (externID != null) {
            decryptedLinks.add(createDownloadlink("http://www.pornerbros.com/" + externID + "/" + System.currentTimeMillis() + ".html"));
            return;
        }
        externID = br.getRegex("hardsextube\\.com/embed/(\\d+)/").getMatch(0);
        if (externID != null) {
            decryptedLinks.add(createDownloadlink("http://www.hardsextube.com/video/" + externID + "/"));
            return;
        }
        externID = br.getRegex("embed\\.pornrabbit\\.com/player\\.swf\\?movie_id=(\\d+)\"").getMatch(0);
        if (externID == null) {
            externID = br.getRegex("pornrabbit\\.com/embed/(\\d+)").getMatch(0);
        }
        if (externID != null) {
            decryptedLinks.add(createDownloadlink("http://pornrabbit.com/video/" + externID + "/"));
            return;
        }
        externID = br.getRegex("player\\.tnaflix\\.com/video/(\\d+)\"").getMatch(0);
        if (externID != null) {
            decryptedLinks.add(createDownloadlink("http://www.tnaflix.com/cum-videos/" + System.currentTimeMillis() + "/video" + externID));
            return;
        }
        externID = br.getRegex("metacafe\\.com/fplayer/(\\d+)/").getMatch(0);
        if (externID != null) {
            decryptedLinks.add(createDownloadlink("http://www.metacafe.com/watch/" + externID + "/" + System.currentTimeMillis()));
            return;
        }
        externID = br.getRegex("\"(http://(www\\.)?pornhub\\.com/embed/[a-z0-9]+)\"").getMatch(0);
        if (externID != null) {
            DownloadLink dl = createDownloadlink(externID);
            decryptedLinks.add(dl);
            return;
        }
        // pornhub handling number 2
        externID = br.getRegex("name=\"FlashVars\" value=\"options=(http://(www\\.)?pornhub\\.com/embed_player(_v\\d+)?\\.php\\?id=\\d+)\"").getMatch(0);
        if (externID != null) {
            br.getPage(externID);
            if (br.containsHTML("<link_url>N/A</link_url>") || br.containsHTML("No htmlCode read") || br.containsHTML(">404 Not Found<")) {
                final DownloadLink offline = createDownloadlink("http://www.pornhub.com/view_video.php?viewkey=7684385859" + new Random().nextInt(10000000));
                offline.setName(externID);
                offline.setAvailable(false);
                decryptedLinks.add(offline);
                return;
            }
            externID = br.getRegex("<link_url>(http://[^<>\"]*?)</link_url>").getMatch(0);
            if (externID == null) {
                logger.warning("Decrypter broken for link: " + PARAMETER);
                throw new DecrypterException("Decrypter broken for link: " + PARAMETER);
            }
            decryptedLinks.add(createDownloadlink(externID));
            return;
        }
        // myxvids.com 1
        externID = br.getRegex("\"(http://(www\\.)?myxvids\\.com/embed/\\d+)\"").getMatch(0);
        if (externID != null) {
            decryptedLinks.add(createDownloadlink(externID));
            return;
        }
        // myxvids.com 2
        externID = br.getRegex("(\\'|\")(http://(www\\.)?myxvids\\.com/embed_code/\\d+/\\d+/myxvids_embed\\.js)(\\'|\")").getMatch(1);
        if (externID != null) {
            br.getPage(externID);
            final String finallink = br.getRegex("\"(http://(www\\.)?myxvids\\.com/embed/\\d+)\"").getMatch(0);
            if (finallink == null) {
                logger.warning("Decrypter broken for link: " + PARAMETER);
                throw new DecrypterException("Decrypter broken for link: " + PARAMETER);
            }
            decryptedLinks.add(createDownloadlink(finallink));
            return;
        }
        // empflix.com 1
        externID = br.getRegex("player\\.empflix\\.com/video/(\\d+)\"").getMatch(0);
        if (externID != null) {
            decryptedLinks.add(createDownloadlink("http://www.empflix.com/videos/" + System.currentTimeMillis() + "-" + externID + ".html"));
            return;
        }
        // empflix.com 2
        externID = br.getRegex("empflix\\.com/embedding_player/player[^<>\"/]*?\\.swf\".*?value=\"config=embedding_feed\\.php\\?viewkey=([^<>\"]*?)\"").getMatch(0);
        if (externID != null) {
            // Find original empflix link and add it to the list
            br.getPage("http://www.empflix.com/embedding_player/embedding_feed.php?viewkey=" + externID);
            if (br.containsHTML(">Sorry, this video is no longer available")) {
                logger.info("Link offline: " + PARAMETER);
                return;
            }
            final String finallink = br.getRegex("<link>(http://.*?)</link>").getMatch(0);
            if (finallink == null) {
                logger.warning("decrypter broken for link: " + PARAMETER);
                throw new DecrypterException("Decrypter broken for link: " + PARAMETER);
            }
            decryptedLinks.add(createDownloadlink(Encoding.htmlDecode(finallink)));
            return;
        }
        externID = br.getRegex("<iframe src=\"http://(www\\.)?yobt\\.tv/embed/(\\d+)\\.html\"").getMatch(1);
        if (externID != null) {
            decryptedLinks.add(createDownloadlink("http://www.yobtdecrypted.tv/content/" + externID + "/" + System.currentTimeMillis() + ".html"));
            return;
        }
        externID = br.getRegex("stileproject\\.com/embed/(\\d+)").getMatch(0);
        if (externID != null) {
            decryptedLinks.add(createDownloadlink("http://stileproject.com/video/" + externID));
            return;
        }
        externID = br.getRegex("\"(http://(www\\.)?deviantclip\\.com/watch/[^<>\"]*?)\"").getMatch(0);
        if (externID != null) {
            decryptedLinks.add(createDownloadlink(externID));
            return;
        }
        externID = br.getRegex("webdata\\.vidz\\.com/demo/swf/FlashPlayerV2\\.swf\".*?flashvars=\"id_scene=(\\d+)").getMatch(0);
        if (externID != null) {
            decryptedLinks.add(createDownloadlink("http://www.vidz.com/video/" + System.currentTimeMillis() + "/vidz_porn_videos/?s=" + externID));
            return;
        }
        // youporn.com handling 1
        externID = br.getRegex("youporn\\.com/embed/(\\d+)").getMatch(0);
        if (externID != null) {
            final DownloadLink dl = createDownloadlink("http://www.youporn.com/watch/" + externID + "/" + System.currentTimeMillis());
            decryptedLinks.add(dl);
            return;
        }
        externID = br.getRegex("pornative\\.com/embed/player\\.swf\\?movie_id=(\\d+)").getMatch(0);
        if (externID != null) {
            DownloadLink dl = createDownloadlink("http://pornative.com/" + externID + ".html");
            decryptedLinks.add(dl);
            return;
        }
        externID = br.getRegex("pornyeah\\.com/playerConfig\\.php\\?[a-z0-9]+\\.[a-z0-9\\.]+\\|(\\d+)").getMatch(0);
        if (externID != null) {
            DownloadLink dl = createDownloadlink("http://www.pornyeah.com/videos/" + Integer.toString(new Random().nextInt(1000000)) + "-" + externID + ".html");
            decryptedLinks.add(dl);
            return;
        }
        externID = br.getRegex("(http://(www\\.)?mofosex\\.com/(embed_player\\.php\\?id=|embed\\?videoid=)\\d+)").getMatch(0);
        if (externID != null) {
            final DownloadLink dl = createDownloadlink(externID);
            decryptedLinks.add(dl);
            return;
        }
        externID = br.getRegex("\"(http://(www\\.)?xrabbit\\.com/video/embed/[A-Za-z0-9=]+/?)\"").getMatch(0);
        if (br.containsHTML("\\&file=http://embed\\.kickassratios\\.com/")) {
            logger.info("Link offline: " + PARAMETER);
            return;
        }
        if (externID != null) {
            decryptedLinks.add(createDownloadlink(externID));
            return;
        }
        externID = br.getRegex("\"(http://(www\\.)?nuvid\\.com/embed/\\d+)\"").getMatch(0);
        if (externID != null) {
            decryptedLinks.add(createDownloadlink(externID));
            return;
        }
        externID = br.getRegex("\"(http://(www\\.)?youjizz\\.com/videos/embed/\\d+)\"").getMatch(0);
        if (externID != null) {
            decryptedLinks.add(createDownloadlink(externID));
            return;
        }
        externID = br.getRegex("\"(http://(www\\.)?vporn\\.com/embed/\\d+)\"").getMatch(0);
        if (externID != null) {
            decryptedLinks.add(createDownloadlink(externID));
            return;
        }
        externID = br.getRegex("\"(http://(www\\.)?bangyoulater\\.com/embed\\.php\\?id=\\d+)\"").getMatch(0);
        if (externID != null) {
            decryptedLinks.add(createDownloadlink(externID));
            return;
        }
        externID = br.getRegex("\"(http://(www\\.)?pornhost\\.com/(embed/)?\\d+)\"").getMatch(0);
        if (externID != null) {
            decryptedLinks.add(createDownloadlink(externID));
            return;
        }
        externID = br.getRegex("\"(http://(www\\.)?spankwire\\.com/EmbedPlayer\\.aspx/?\\?ArticleId=\\d+)").getMatch(0);
        if (externID != null) {
            decryptedLinks.add(createDownloadlink(externID));
            return;
        }
        externID = br.getRegex("\"(http://(www\\.)?submityourflicks\\.com/embedded/\\d+)\"").getMatch(0);
        if (externID != null) {
            decryptedLinks.add(createDownloadlink(externID));
            return;
        }
        externID = br.getRegex("(http://(www\\.)?theamateurzone\\.info/media/player/config_embed\\.php\\?vkey=\\d+)\"").getMatch(0);
        if (externID != null) {
            decryptedLinks.add(createDownloadlink(externID));
            return;
        }
        externID = br.getRegex("\"(http://(www\\.)?embeds\\.sunporno\\.com/embed/[^<>\"]*?)\"").getMatch(0);
        if (externID != null) {
            if (externID.equals("http://embeds.sunporno.com/embed/videos")) {
                final DownloadLink offline = createDownloadlink("directhttp://" + externID);
                offline.setAvailable(false);
                offline.setProperty("offline", true);
                decryptedLinks.add(offline);
                return;
            }
            decryptedLinks.add(createDownloadlink(externID));
            return;
        }
        externID = br.getRegex("\"(http://(www\\.)?fux\\.com/embed/\\d+)\"").getMatch(0);
        if (externID != null) {
            decryptedLinks.add(createDownloadlink(externID));
            return;
        }
        externID = br.getRegex("moviefap\\.com/embedding_player/player.*?value=\"config=(embedding_feed\\.php\\?viewkey=[a-z0-9]+)\"").getMatch(0);
        if (externID != null) {
            decryptedLinks.add(createDownloadlink("http://www.moviefap.com/embedding_player/" + externID));
            return;
        }
        externID = br.getRegex("\"(http://(www\\.)?embed\\.porntube\\.com/\\d+)\"").getMatch(0);
        if (externID == null) {
            externID = br.getRegex("\"(http://(www\\.)?porntube\\.com/embed/\\d+)\"").getMatch(0);
        }
        if (externID != null) {
            decryptedLinks.add(createDownloadlink(externID));
            return;
        }
        externID = br.getRegex("\"(http://(www\\.)?xxxhdd\\.com/embed/\\d+)\"").getMatch(0);
        if (externID != null) {
            final DownloadLink dl = createDownloadlink(externID);
            decryptedLinks.add(dl);
            return;
        }
        externID = br.getRegex("\"(http://(www\\.)?extremetube\\.com/embed/[^<>\"/]*?)\"").getMatch(0);
        if (externID != null) {
            decryptedLinks.add(createDownloadlink(externID));
            return;
        }
        externID = br.getRegex("\"(http://embeds\\.ah\\-me\\.com/embed/\\d+)\"").getMatch(0);
        if (externID != null) {
            decryptedLinks.add(createDownloadlink(externID));
            return;
        }
        externID = br.getRegex("\"(http://(www\\.)?proporn\\.com/embed/\\d+)\"").getMatch(0);
        if (externID != null) {
            decryptedLinks.add(createDownloadlink(externID));
            return;
        }
        // filename needed for all IDs below
        if (filename == null) {
            logger.warning("Decrypter broken for link: " + PARAMETER);
            throw new DecrypterException("Decrypter broken for link: " + PARAMETER);
        }
        filename = Encoding.htmlDecode(filename.trim());
        externID = br.getRegex("shufuni\\.com/Flash/.*?flashvars=\"VideoCode=(.*?)\"").getMatch(0);
        if (externID != null) {
            DownloadLink dl = createDownloadlink("http://www.shufuni.com/handlers/FLVStreamingv2.ashx?videoCode=" + externID);
            dl.setFinalFileName(Encoding.htmlDecode(filename.trim()));
            decryptedLinks.add(dl);
            return;
        }
        externID = br.getRegex("src=\"http://videos\\.allelitepass\\.com/txc/([^<>\"/]*?)\\.swf\"").getMatch(0);
        if (externID != null) {
            br.getPage("http://videos.allelitepass.com/txc/player.php?video=" + Encoding.htmlDecode(externID));
            externID = br.getRegex("<file>(http://[^<>\"]*?)</file>").getMatch(0);
            if (externID != null) {
                final DownloadLink dl = createDownloadlink("directhttp://" + externID);
                dl.setFinalFileName(filename + ".flv");
                decryptedLinks.add(dl);
                return;
            }

        }
        // 2nd handling for tnaflix
        externID = br.getRegex("tnaflix\\.com/embedding_player/player_[^<>\"]+\\.swf.*?config=(embedding_feed\\.php\\?viewkey=[a-z0-9]+)").getMatch(0);
        if (externID != null) {
            br.getPage("http://www.tnaflix.com/embedding_player/" + externID);
            externID = br.getRegex("start_thumb>http://static\\.tnaflix\\.com/thumbs/[a-z0-9\\-_]+/[a-z0-9]+_(\\d+)l\\.jpg<").getMatch(0);
            if (externID != null) {
                decryptedLinks.add(createDownloadlink("http://www.tnaflix.com/cum-videos/" + System.currentTimeMillis() + "/video" + externID));
                return;
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
                logger.warning("FourSexFourCom -> youporn link invalid, please check browser to confirm: " + PARAMETER);
                throw new DecrypterException("Decrypter broken for link: " + PARAMETER);
            }
            if (br.containsHTML("download\\.youporn\\.com/agecheck")) {
                logger.info("Link broken or offline: " + PARAMETER);
                return;
            }
            externID = br.getRegex("\"(http://(www\\.)?download\\.youporn.com/download/\\d+/\\?xml=1)\"").getMatch(0);
            if (externID == null) {
                logger.warning("Decrypter broken for link: " + PARAMETER);
                throw new DecrypterException("Decrypter broken for link: " + PARAMETER);
            }
            br.getPage(externID);
            final String finallink = br.getRegex("<location>(http://.*?)</location>").getMatch(0);
            if (finallink == null) {
                logger.warning("Decrypter broken for link: " + PARAMETER);
                throw new DecrypterException("Decrypter broken for link: " + PARAMETER);
            }
            final DownloadLink dl = createDownloadlink("directhttp://" + Encoding.htmlDecode(finallink));
            String type = br.getRegex("<meta rel=\"type\">(.*?)</meta>").getMatch(0);
            if (type == null) {
                type = "flv";
            }
            dl.setFinalFileName(filename + "." + type);
            decryptedLinks.add(dl);
            return;
        }
        externID = br.getRegex("\"(http://(www\\.)?freeviewmovies\\.com/embed/\\d+/)\"").getMatch(0);
        if (externID != null) {
            br.getPage(externID);
            final String cb = br.getRegex("\\?cb=(\\d+)\\'").getMatch(0);
            if (cb == null) {
                logger.warning("Decrypter broken for link: " + PARAMETER);
                throw new DecrypterException("Decrypter broken for link: " + PARAMETER);
            }
            final String postData = "jsonRequest=%7B%22returnType%22%3A%22json%22%2C%22file%22%3A%22%22%2C%22htmlHostDomain%22%3Anull%2C%22request%22%3A%22getAllData%22%2C%22playerOnly%22%3A%22true%22%2C%22loaderUrl%22%3A%22http%3A%2F%2Fcdn1%2Estatic%2Eatlasfiles%2Ecom%2Fplayer%2Fmemberplayer%2Eswf%3Fcb%3D" + cb + "%22%2C%22appdataurl%22%3A%22" + Encoding.urlEncode(externID) + "%22%2C%22path%22%3A%22%22%2C%22cb%22%3A%22" + cb + "%22%7D&cacheBuster=" + System.currentTimeMillis();
            br.postPage(externID, postData);
            externID = br.getRegex("\"file\": \"(http://[^<>\"]*?)\"").getMatch(0);
            if (externID == null) {
                logger.warning("Decrypter broken for link: " + PARAMETER);
                throw new DecrypterException("Decrypter broken for link: " + PARAMETER);
            }
            final DownloadLink dl = createDownloadlink("directhttp://" + externID);
            dl.setFinalFileName(Encoding.htmlDecode(filename.trim()) + ".mp4");
            decryptedLinks.add(dl);
            return;
        }
        externID = br.getRegex("(http://(www\\.)?gasxxx\\.com/media/player/config_embed\\.php\\?vkey=\\d+)\"").getMatch(0);
        if (externID != null) {
            br.getPage(externID);
            externID = br.getRegex("<src>(http://[^<>\"]*?\\.flv)</src>").getMatch(0);
            if (externID == null) {
                logger.warning("Decrypter broken for link: " + PARAMETER);
                throw new DecrypterException("Decrypter broken for link: " + PARAMETER);
            }
            final DownloadLink dl = createDownloadlink("directhttp://" + externID);
            dl.setFinalFileName(filename + ".flv");
            decryptedLinks.add(dl);
            return;
        }
        externID = br.getRegex("(https?://www\\.keezmovies\\.com/embed_player\\.php\\?v?id=\\d+)").getMatch(0);
        if (externID != null) {
            br.getPage(externID);
            externID = br.getRegex("<share>(http://[^<>\"]*?)</share>").getMatch(0);
            if (externID != null) {
                final DownloadLink dl = createDownloadlink(externID);
                decryptedLinks.add(dl);
                return;
            } else {
                externID = br.getRegex("<flv_url>(http://[^<>\"]*?)</flv_url>").getMatch(0);
                if (externID == null) {
                    logger.warning("Decrypter broken for link: " + PARAMETER);
                    throw new DecrypterException("Decrypter broken for link: " + PARAMETER);
                }
                final DownloadLink dl = createDownloadlink("directhttp://" + Encoding.htmlDecode(externID));
                dl.setFinalFileName(filename + ".flv");
                decryptedLinks.add(dl);
                return;
            }
        }
        externID = br.getRegex("foxytube\\.com/embedded/(\\d+)\"").getMatch(0);
        if (externID != null) {
            br.getPage("http://www.foxytube.com/embconfig/" + externID + "/");
            externID = br.getRegex("<file>(http://[^<>\"]*?)</file>").getMatch(0);
            if (externID == null) {
                logger.warning("Decrypter broken for link: " + PARAMETER);
                throw new DecrypterException("Decrypter broken for link: " + PARAMETER);
            }
            final DownloadLink dl = createDownloadlink("directhttp://" + externID);
            dl.setFinalFileName(filename + ".flv");
            decryptedLinks.add(dl);
            return;
        }
        externID = br.getRegex("(http://(www\\.)?5ilthy\\.com/playerConfig\\.php\\?[a-z0-9]+\\.(flv|mp4))").getMatch(0);
        if (externID != null) {
            final DownloadLink dl = createDownloadlink(externID);
            dl.setProperty("5ilthydirectfilename", filename);
            decryptedLinks.add(dl);
            return;

        }
        final String continuelink = br.getRegex("\"(/video/embed\\?id=\\d+\\&pinId=\\d+[^<>\"]*?)\"").getMatch(0);
        if (continuelink == null) {
            logger.warning("Decrypter broken for link: " + PARAMETER);
            throw new DecrypterException("Decrypter broken for link: " + PARAMETER);
        }
        br.getPage("http://www.sex.com" + continuelink);
        externID = br.getRegex("file: \"(http://[^<>\"]*?)\"").getMatch(0);
        if (externID == null) {
            logger.warning("Decrypter broken for link: " + PARAMETER);
            throw new DecrypterException("Decrypter broken for link: " + PARAMETER);
        }
        final DownloadLink fina = createDownloadlink("directhttp://" + externID);
        fina.setFinalFileName(filename + ".mp4");
        decryptedLinks.add(fina);
        return;
    }

    /* NO OVERRIDE!! */
    public boolean hasCaptcha(CryptedLink link, jd.plugins.Account acc) {
        return false;
    }

}