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

//EmbedDecrypter 0.2.8
@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "superhqporn.com" }, urls = { "http://www\\.superhqporn\\.com/\\?v=[A-Z0-9]+" }, flags = { 0 })
public class SuperhqpornComDecrypter extends PluginForDecrypt {

    public SuperhqpornComDecrypter(PluginWrapper wrapper) {
        super(wrapper);
    }

    @SuppressWarnings("deprecation")
    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        String externID = null;
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        String parameter = param.toString();
        br.setFollowRedirects(false);
        br.getPage(parameter);
        final DownloadLink main = createDownloadlink(parameter.replace("superhqporn.com/", "superhqporndecrypted.com/"));
        try {
            main.setContentUrl(parameter);
        } catch (final Throwable e) {
            /* Not available in 0.9.581 Stable */
            main.setBrowserUrl(parameter);
        }
        if (br.containsHTML(">Video Not Exists<|>Requested video not exist") || br.getHttpConnection().getResponseCode() == 404) {
            main.setAvailable(false);
            main.setProperty("offline", true);
            decryptedLinks.add(main);
            return decryptedLinks;
        }
        externID = br.getRedirectLocation();
        if (externID != null && !externID.contains("superhqporn.com/")) {
            decryptedLinks.add(createDownloadlink(externID));
            return decryptedLinks;
        } else if (externID != null) {
            br.getPage(externID);
            externID = null;
        }
        // xvideos.com 1
        externID = br.getRegex("xvideos\\.com/embedframe/(\\d+)\"").getMatch(0);
        if (externID != null) {
            decryptedLinks.add(createDownloadlink("http://www.xvideos.com/video" + externID));
            return decryptedLinks;
        }
        // xvideos.com 2
        externID = br.getRegex("\"(http://(www\\.)?flashservice\\.xvideos\\.com/embedframe/\\d+)\"").getMatch(0);
        if (externID != null) {
            decryptedLinks.add(createDownloadlink(externID));
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
        if (externID == null) {
            externID = br.getRegex("embed\\.redtube\\.com/player/\\?id=(\\d+)\\&").getMatch(0);
        }
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
        if (externID == null) {
            externID = br.getRegex("pornrabbit\\.com/embed/(\\d+)").getMatch(0);
        }
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
        externID = br.getRegex("\"(http://(www\\.)?pornhub\\.com/embed/[a-z0-9]+)\"").getMatch(0);
        if (externID != null) {
            DownloadLink dl = createDownloadlink(externID);
            decryptedLinks.add(dl);
            return decryptedLinks;
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
            decryptedLinks.add(createDownloadlink("http://www.yobtdecrypted.tv/content/" + externID + "/" + System.currentTimeMillis() + ".html"));
            return decryptedLinks;
        }
        externID = br.getRegex("stileproject\\.com/embed/(\\d+)").getMatch(0);
        if (externID != null) {
            decryptedLinks.add(createDownloadlink("http://stileproject.com/video/" + externID));
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
        externID = br.getRegex("(http://(www\\.)?mofosex\\.com/(embed_player\\.php\\?id=|embed\\?videoid=)\\d+)").getMatch(0);
        if (externID != null) {
            final DownloadLink dl = createDownloadlink(externID);
            decryptedLinks.add(dl);
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
        externID = br.getRegex("\"(http://(www\\.)?spankwire\\.com/EmbedPlayer\\.aspx/?\\?ArticleId=\\d+)").getMatch(0);
        if (externID != null) {
            decryptedLinks.add(createDownloadlink(externID));
            return decryptedLinks;
        }
        externID = br.getRegex("\"(http://(www\\.)?submityourflicks\\.com/embedded/\\d+)\"").getMatch(0);
        if (externID != null) {
            decryptedLinks.add(createDownloadlink(externID));
            return decryptedLinks;
        }
        externID = br.getRegex("(http://(www\\.)?theamateurzone\\.info/media/player/config_embed\\.php\\?vkey=\\d+)\"").getMatch(0);
        if (externID != null) {
            decryptedLinks.add(createDownloadlink(externID));
            return decryptedLinks;
        }
        externID = br.getRegex("\"(http://(www\\.)?embeds\\.sunporno\\.com/embed/[^<>\"]*?)\"").getMatch(0);
        if (externID != null) {
            if (externID.equals("http://embeds.sunporno.com/embed/videos")) {
                final DownloadLink offline = createDownloadlink("directhttp://" + externID);
                offline.setAvailable(false);
                offline.setProperty("offline", true);
                decryptedLinks.add(offline);
                return decryptedLinks;
            }
            decryptedLinks.add(createDownloadlink(externID));
            return decryptedLinks;
        }
        externID = br.getRegex("\"(http://(www\\.)?fux\\.com/embed/\\d+)\"").getMatch(0);
        if (externID != null) {
            decryptedLinks.add(createDownloadlink(externID));
            return decryptedLinks;
        }
        externID = br.getRegex("moviefap\\.com/embedding_player/player.*?value=\"config=(embedding_feed\\.php\\?viewkey=[a-z0-9]+)\"").getMatch(0);
        if (externID != null) {
            decryptedLinks.add(createDownloadlink("http://www.moviefap.com/embedding_player/" + externID));
            return decryptedLinks;
        }
        externID = br.getRegex("\"(http://(www\\.)?embed\\.porntube\\.com/\\d+)\"").getMatch(0);
        if (externID == null) {
            externID = br.getRegex("\"(http://(www\\.)?porntube\\.com/embed/\\d+)\"").getMatch(0);
        }
        if (externID != null) {
            decryptedLinks.add(createDownloadlink(externID));
            return decryptedLinks;
        }
        externID = br.getRegex("\"(http://(www\\.)?xxxhdd\\.com/embed/\\d+)\"").getMatch(0);
        if (externID != null) {
            final DownloadLink dl = createDownloadlink(externID);
            decryptedLinks.add(dl);
            return decryptedLinks;
        }
        externID = br.getRegex("\"(http://(www\\.)?extremetube\\.com/embed/[^<>\"/]*?)\"").getMatch(0);
        if (externID != null) {
            decryptedLinks.add(createDownloadlink(externID));
            return decryptedLinks;
        }
        externID = br.getRegex("\"(http://embeds\\.ah\\-me\\.com/embed/\\d+)\"").getMatch(0);
        if (externID != null) {
            decryptedLinks.add(createDownloadlink(externID));
            return decryptedLinks;
        }
        externID = br.getRegex("\"(http://(www\\.)?proporn\\.com/embed/\\d+)\"").getMatch(0);
        if (externID != null) {
            decryptedLinks.add(createDownloadlink(externID));
            return decryptedLinks;
        }
        /* No extern id found --> Add main-link */
        decryptedLinks.add(main);
        return decryptedLinks;
    }

}
