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
import jd.parser.Regex;
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
        externID = br.getRegex("freeporn.com/swf/player/AppLauncher_secure\\.swf\\'><param.*?name=\\'flashvars\\' value=\\'file=([^<>\"]*?)\\&").getMatch(0);
        if (externID != null) {
            externID = Encoding.urlEncode(externID.trim());
            br.postPage("http://www.freeporn.com/getcdnurl/", "jsonRequest=%7B%22returnType%22%3A%22json%22%2C%22file%22%3A%22" + externID + "%22%2C%22request%22%3A%22getAllData%22%2C%22width%22%3A%22505%22%2C%22path%22%3A%22" + externID + "%22%2C%22height%22%3A%22400%22%2C%22loaderUrl%22%3A%22http%3A%2F%2Fcdn1%2Eimage%2Efreeporn%2Ecom%2Fswf%2Fplayer%2FAppLauncher%5Fsecure%2Eswf%22%2C%22htmlHostDomain%22%3A%22www%2Evoayeurs%2Ecom%22%7D&cacheBuster=1339506847983");
            externID = new Regex(br.toString().replace("\\", ""), "image\\.freeporn\\.com/media/videos/tmb/(\\d+)/").getMatch(0);
            if (externID != null) {
                decryptedLinks.add(createDownloadlink("http://www.freeporn.com/video/" + externID + "/"));
                return decryptedLinks;
            }
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
            DownloadLink dl = createDownloadlink("directhttp://" + externID);
            dl.setFinalFileName(Encoding.htmlDecode(filename.trim()) + ".mp4");
            decryptedLinks.add(dl);
            return decryptedLinks;
        }
        decrypted = parameter.replace("badjojo.com", "decryptedbadjojo.com");
        decryptedLinks.add(createDownloadlink(decrypted));
        return decryptedLinks;
    }

}
