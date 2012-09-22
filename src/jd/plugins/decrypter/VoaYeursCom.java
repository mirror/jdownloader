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
import jd.parser.Regex;
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
        String filename = br.getRegex("<title>Porno XXX \\- ([^<>\"]*?)</title>").getMatch(0);
        String externID = br.getRedirectLocation();
        if (externID != null && externID.length() < 40) {
            logger.info("Link offline: " + parameter);
            return decryptedLinks;
        }
        if (externID != null) {
            DownloadLink dl = createDownloadlink(externID);
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
        if (externID == null) externID = br.getRegex("xvideos\\.com/swf/flv_player_site_v\\d+\\.swf\" /><param name=\"allowFullScreen\" value=\"true\" /><param name=\"flashvars\" value=\"id_video=(\\d+)\"").getMatch(0);
        if (externID != null) {
            decryptedLinks.add(createDownloadlink("http://www.xvideos.com/video" + externID));
            return decryptedLinks;
        }
        externID = br.getRegex("\"(http://(www\\.)?tube8\\.com/embed/[^<>\"/]*?/[^<>\"/]*?/\\d+/?)\"").getMatch(0);
        if (externID != null) {
            decryptedLinks.add(createDownloadlink(externID.replace("tube8.com/embed/", "tube8.com/")));
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
        externID = br.getRegex("player\\.empflix\\.com/video/(\\d+)\"").getMatch(0);
        if (externID != null) {
            decryptedLinks.add(createDownloadlink("http://www.empflix.com/videos/" + System.currentTimeMillis() + "-" + externID + ".html"));
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
        externID = br.getRegex("(http://(www\\.)?keezmovies\\.com/embed_player\\.php\\?id=\\d+)\"").getMatch(0);
        if (externID != null) {
            br.getPage(externID);
            externID = br.getRegex("<share>(http://[^<>\"]*?)</share>").getMatch(0);
            if (externID == null) {
                logger.warning("Decrypter broken for link: " + parameter);
                return null;
            }
            final DownloadLink dl = createDownloadlink(externID);
            decryptedLinks.add(dl);
            return decryptedLinks;
        }
        // For all following ids, a filename is needed
        if (filename == null) {
            logger.warning("Decrypter broken for link: " + parameter);
            return null;
        }
        filename = Encoding.htmlDecode(filename.trim());
        externID = br.getRegex("flashvars=\"enablejs=true\\&autostart=false\\&mediaid=(\\d+)\\&").getMatch(0);
        if (externID != null) {
            br.getPage("http://www.deviantclip.com/playlists/" + externID + "/playlist.xml");
            String finallink = br.getRegex("<location>(http[^<>\"]*?)</location>").getMatch(0);
            if (finallink == null) {
                logger.warning("Couldn't decrypt link: " + parameter);
                return null;
            }
            final DownloadLink dl = createDownloadlink("directhttp://" + Encoding.htmlDecode(finallink));
            dl.setFinalFileName(filename + ".flv");
            decryptedLinks.add(dl);
            return decryptedLinks;
        }
        externID = br.getRegex("shufuni\\.com/Flash/.*?flashvars=\"VideoCode=(.*?)\"").getMatch(0);
        if (externID != null) {
            DownloadLink dl = createDownloadlink("http://www.shufuni.com/handlers/FLVStreamingv2.ashx?videoCode=" + externID);
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
        if (externID != null) {
            externID = Encoding.urlEncode(externID.trim());
            br.postPage("http://www.freeporn.com/getcdnurl/", "jsonRequest=%7B%22returnType%22%3A%22json%22%2C%22file%22%3A%22" + externID + "%22%2C%22request%22%3A%22getAllData%22%2C%22width%22%3A%22505%22%2C%22path%22%3A%22" + externID + "%22%2C%22height%22%3A%22400%22%2C%22loaderUrl%22%3A%22http%3A%2F%2Fcdn1%2Eimage%2Efreeporn%2Ecom%2Fswf%2Fplayer%2FAppLauncher%5Fsecure%2Eswf%22%2C%22htmlHostDomain%22%3A%22www%2Evoayeurs%2Ecom%22%7D&cacheBuster=1339506847983");
            externID = new Regex(br.toString().replace("\\", ""), "\"file\": \"(http[^<>\"]*?)\"").getMatch(0);
            if (externID != null) {
                final DownloadLink dl = createDownloadlink("directhttp://" + externID);
                dl.setFinalFileName(Encoding.htmlDecode(filename.trim()) + ".flv");
                decryptedLinks.add(dl);
                return decryptedLinks;
            }
        }
        if (externID == null) {
            logger.warning("Couldn't decrypt link: " + parameter);
            return null;
        }
        return decryptedLinks;
    }

}
