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

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "amateurgalore.net" }, urls = { "http://(www\\.)?amateurgalore\\.net/index/video/[a-z0-9_\\-]+" }, flags = { 0 })
public class AmateurGaloreNet extends PluginForDecrypt {

    public AmateurGaloreNet(PluginWrapper wrapper) {
        super(wrapper);
    }

    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        String parameter = param.toString();
        br.getPage(parameter);
        String filename = br.getRegex("<meta name=\"DC\\.title\" content=\"([^<>\"]*?) \\- Amateur Porn \\- AmateurGalore \\- Free Amateur Porn\"").getMatch(0);
        if (filename != null) filename = Encoding.htmlDecode(filename.trim());
        String externID = br.getRegex("\"http://videobam\\.com/widget/(.*?)/custom").getMatch(0);
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
        externID = br.getRegex("player\\.empflix\\.com/video/(\\d+)\"").getMatch(0);
        if (externID != null) {
            decryptedLinks.add(createDownloadlink("http://www.empflix.com/videos/" + System.currentTimeMillis() + "-" + externID + ".html"));
            return decryptedLinks;
        }
        if (br.containsHTML("(name=\"movie\" value=\"http://(www\\.)?megaporn\\.com/|name=\"movie\" value=\"http://video\\.megarotic\\.com/|<h3><center><a href=\"http://seemygf\\.com/vod/)")) {
            logger.info("Link offline: " + parameter);
            return decryptedLinks;
        }
        if (externID == null) {
            logger.warning("Decrypter broken for link: " + parameter);
            return null;
        }
        return decryptedLinks;
    }

}
