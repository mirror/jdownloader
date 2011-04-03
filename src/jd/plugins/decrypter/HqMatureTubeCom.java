//jDownloader - Downloadmanager
//Copyright (C) 2011  JD-Team support@jdownloader.org
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
import jd.plugins.DecrypterException;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.PluginForDecrypt;
import jd.utils.locale.JDL;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "hqmaturetube.com" }, urls = { "http://(www\\.)?hqmaturetube\\.com/cms/watch/\\d+\\.php" }, flags = { 0 })
public class HqMatureTubeCom extends PluginForDecrypt {

    public HqMatureTubeCom(PluginWrapper wrapper) {
        super(wrapper);
    }

    // This is a site which shows embedded videos of other sites so we may have
    // to add regexes/handlings here
    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        String parameter = param.toString();
        br.setFollowRedirects(false);
        br.getPage(parameter);
        if (br.containsHTML("(<TITLE>404 Not Found</TITLE>|<H1>Not Found</H1>|was not found on this server\\.<P>)")) throw new DecrypterException(JDL.L("plugins.decrypt.errormsg.unavailable", "Perhaps wrong URL or the download is not available anymore."));
        String filename = br.getRegex("<title>(.*?) \\| HQ Mature Tube \\| Free streaming porn videos</title>").getMatch(0);
        if (filename == null) filename = br.getRegex("<h2 style=\"text-transform:uppercase;\">(.*?)</h2>").getMatch(0);
        if (filename == null) {
            logger.warning("hqmaturetube decrypter broken(filename regex) for link: " + parameter);
            return null;
        }
        filename = filename.trim();
        String externID = br.getRegex("value=\"config=embedding_feed\\.php\\?viewkey=(.*?)\"").getMatch(0);
        if (externID != null) {
            // Find original empflix link and add it to the list
            br.getPage("http://www.empflix.com/embedding_player/embedding_feed.php?viewkey=" + externID);
            String finallink = br.getRegex("<link>(http://.*?)</link>").getMatch(0);
            if (finallink == null) {
                logger.warning("hqmaturetube decrypter broken for link: " + parameter);
                return null;
            }
            decryptedLinks.add(createDownloadlink(Encoding.htmlDecode(finallink)));
            return decryptedLinks;
        }
        externID = br.getRegex("flashvars=\"file=(http://stream\\.mywifesmom\\.com/flv/\\d+\\.flv)\\&").getMatch(0);
        if (externID != null) {
            DownloadLink dl = createDownloadlink("directhttp://" + Encoding.htmlDecode(externID));
            dl.setFinalFileName(filename + ".flv");
            decryptedLinks.add(dl);
            return decryptedLinks;
        }
        externID = br.getRegex("value=\"id_video=(\\d+)\"").getMatch(0);
        if (externID != null) {
            String finallink = "http://www.xvideos.com/video" + externID + "/";
            decryptedLinks.add(createDownloadlink(Encoding.htmlDecode(finallink)));
            return decryptedLinks;
        }
        externID = br.getRegex("flashvars=\"file=(http.*?)\\&et_url=http").getMatch(0);
        if (externID != null) {
            br.getPage(Encoding.htmlDecode(externID));
            String finallink = br.getRegex("<location>(http://.*?)</location>").getMatch(0);
            if (finallink == null) {
                logger.warning("hqmaturetube decrypter broken for link: " + parameter);
                return null;
            }
            DownloadLink dl = createDownloadlink("directhttp://" + Encoding.htmlDecode(finallink));
            String type = br.getRegex("<meta rel=\"type\">(.*?)</meta>").getMatch(0);
            if (type == null) type = "flv";
            dl.setFinalFileName(filename + "." + type);
            decryptedLinks.add(dl);
            return decryptedLinks;
        }
        externID = br.getRegex("flashvars=\"file=(http://[a-z0-9\\-_]+\\.60plusmilfs\\.com/.*?)\\&image=http").getMatch(0);
        if (externID != null) {
            br.getPage(Encoding.htmlDecode(externID));
            String finallink = br.getRedirectLocation();
            if (finallink == null) {
                logger.warning("hqmaturetube decrypter broken for link: " + parameter);
                return null;
            }
            DownloadLink dl = createDownloadlink("directhttp://" + Encoding.htmlDecode(finallink));
            dl.setFinalFileName(filename + finallink.subSequence(finallink.length() - 4, finallink.length()));
            decryptedLinks.add(dl);
            return decryptedLinks;
        }
        if (externID == null) {
            logger.warning("hqmaturetube decrypter broken for link: " + parameter);
            return null;
        }
        return decryptedLinks;
    }
}
