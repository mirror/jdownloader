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
import jd.http.Browser.BrowserException;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.PluginForDecrypt;

//EmbedDecrypter 0.1
@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "mycrazyvids.com" }, urls = { "http://(www\\.)?mycrazyvids\\.com/([a-z0-9\\-_]+\\-\\d+\\.html|\\?go=click\\&c=\\d+\\&n=\\d+\\&e=\\d+\\&g=\\d+\\&r=\\d+\\&u=http[^<>\"/]+)" }, flags = { 0 })
public class MyCrazyVidsCom extends PluginForDecrypt {

    public MyCrazyVidsCom(PluginWrapper wrapper) {
        super(wrapper);
    }

    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        String parameter = param.toString();
        if (parameter.matches("http://(www\\.)?mycrazyvids\\.com/\\?go=click\\&c=\\d+\\&n=\\d+\\&e=\\d+\\&g=\\d+\\&r=\\d+\\&u=http[^<>\"/]+")) {
            String externLink = new Regex(parameter, "\\&u=(http[^<>\"/]+)").getMatch(0);
            externLink = Encoding.deepHtmlDecode(externLink);
            decryptedLinks.add(createDownloadlink(externLink));
        } else {
            try {
                br.getPage(parameter);
            } catch (final BrowserException e) {
                logger.info("Cannot decrypt link, either offline or server error: " + parameter);
                return decryptedLinks;
            }
            if (br.containsHTML(">404 Not Found<")) {
                logger.info("Cannot decrypt link, either offline or server error: " + parameter);
                return decryptedLinks;
            }
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
            externID = br.getRegex("(http://drtuber\\.com/player/config_embed3\\.php\\?vkey=[a-z0-9]+)").getMatch(0);
            if (externID != null) {
                decryptedLinks.add(createDownloadlink(externID));
                return decryptedLinks;
            }
            externID = br.getRegex("\"(http://(www\\.)?deviantclip\\.com/watch/[^<>\"]*?)\"").getMatch(0);
            if (externID != null) {
                decryptedLinks.add(createDownloadlink(externID));
                return decryptedLinks;
            }
            externID = br.getRegex("(http://(www\\.)?keezmovies\\.com/embed_player\\.php\\?v?id=\\d+)\"").getMatch(0);
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
            // filename needed for all IDs below
            String filename = br.getRegex("<h1 class=\"name\">([^<>\"]*?)</h1>").getMatch(0);
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
            externID = br.getRegex("<embed src=\\'http://(www\\.)?hardsextube\\.com/embed/(\\d+)/\\'").getMatch(0);
            if (externID != null) {
                decryptedLinks.add(createDownloadlink("http://www.hardsextube.com/video/" + externID + "/"));
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