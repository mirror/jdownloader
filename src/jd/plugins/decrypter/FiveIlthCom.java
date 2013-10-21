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
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.PluginForDecrypt;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "5ilth.com" }, urls = { "http://(www\\.)?5ilth\\.com/(hosted|out\\-static|out)\\-id\\d+\\-.*?\\.html" }, flags = { 0 })
public class FiveIlthCom extends PluginForDecrypt {

    public FiveIlthCom(PluginWrapper wrapper) {
        super(wrapper);
    }

    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        br.setFollowRedirects(false);
        String parameter = param.toString();
        br.getPage(parameter);
        if (br.containsHTML("No htmlCode read")) {
            logger.info("Link offline: " + parameter);
            return decryptedLinks;
        }
        String externID = br.getRedirectLocation();
        if (externID != null) {
            final DownloadLink dl = createDownloadlink(externID);
            decryptedLinks.add(dl);
            return decryptedLinks;
        }
        String filename = br.getRegex("<div class=\"hed videotitle\"><h1>(.*?)</h1></div>").getMatch(0);
        if (filename == null) {
            logger.warning("Decrypter broken for link: " + parameter);
            return null;
        }
        filename = filename.trim();
        externID = br.getRegex("\\&file=(http://static\\.mofos\\.com/.*?)\\&enablejs").getMatch(0);
        if (externID != null) {
            final DownloadLink dl = createDownloadlink("directhttp://" + externID);
            dl.setFinalFileName(filename + ".flv");
            decryptedLinks.add(dl);
            return decryptedLinks;

        }
        externID = br.getRegex("flashvars=\"\\&file=(.*?)\\&link").getMatch(0);
        if (externID != null) {
            final DownloadLink dl = createDownloadlink("directhttp://http://flash.serious-cash.com/" + externID + ".flv");
            decryptedLinks.add(dl);
            dl.setFinalFileName(filename + ".flv");
            return decryptedLinks;
        }
        externID = br.getRegex("file=(http://(www\\.)?hostave\\d+\\.net/.*?)\\&screenfile").getMatch(0);
        if (externID != null) {
            final DownloadLink dl = createDownloadlink("directhttp://" + externID);
            dl.setFinalFileName(filename + ".flv");
            decryptedLinks.add(dl);
            return decryptedLinks;

        }
        externID = br.getRegex("flashvars=\"videopath=height=\\d+\\&width=\\d+\\&file=(http://.*?)\\&beginimage").getMatch(0);
        if (externID != null) {
            final DownloadLink dl = createDownloadlink("directhttp://" + externID);
            dl.setFinalFileName(filename + ".flv");
            decryptedLinks.add(dl);
            return decryptedLinks;

        }
        externID = br.getRegex("(http://(www\\.)?gfssex\\.com/playerConfig\\.php[^<>\"]*?)\"").getMatch(0);
        if (externID != null) {
            final DownloadLink dl = createDownloadlink(externID);
            decryptedLinks.add(dl);
            return decryptedLinks;
        }
        // Complicated stuff below
        externID = br.getRegex("(http://(www\\.)?5ilthy\\.com/playerConfig\\.php\\?[a-z0-9]+\\.(flv|mp4))").getMatch(0);
        if (externID != null) {
            final DownloadLink dl = createDownloadlink(externID);
            dl.setProperty("5ilthydirectfilename", filename);
            decryptedLinks.add(dl);
            return decryptedLinks;

        }
        externID = br.getRegex("http://(www\\.)?5ilthy\\.com/playerConfig\\.php\\?(http://[^<>\"]*?\\.(flv|mp4))").getMatch(1);
        if (externID != null) {
            final DownloadLink dl = createDownloadlink("directhttp://" + externID);
            if (externID.endsWith(".flv"))
                dl.setFinalFileName(filename + ".flv");
            else
                dl.setFinalFileName(filename + ".mp4");
            decryptedLinks.add(dl);
            return decryptedLinks;

        }
        externID = br.getRegex("(http://(www\\.)?5ilthy\\.com/playerConfig\\.php\\?[^<>\"/]*?\\.(flv|mp4))").getMatch(0);
        if (externID != null) {
            br.getPage(externID);
            externID = br.getRegex("flvMask:(http://[^<>\"]*?);").getMatch(0);
            if (externID == null) {
                logger.warning("Decrypter broken for link: " + parameter);
                return null;
            }
            final DownloadLink dl = createDownloadlink("directhttp://" + externID);
            if (externID.endsWith(".flv"))
                dl.setFinalFileName(filename + ".flv");
            else
                dl.setFinalFileName(filename + ".mp4");
            decryptedLinks.add(dl);
            return decryptedLinks;

        }
        externID = br.getRegex("(http://(www\\.)?filthyrx\\.com/playerConfig\\.php[^<>\"]*?)\"").getMatch(0);
        if (externID != null) {
            br.getPage(externID);
            externID = br.getRegex("flvMask:(http://[^<>\"]*?);").getMatch(0);
            if (externID == null) {
                logger.warning("Decrypter broken for link: " + parameter);
                return null;
            }
            // Maybe we have multiple links - then always use the last one
            final String[] firectlinks = externID.split("http://");
            externID = "http://" + firectlinks[firectlinks.length - 1];
            final DownloadLink dl = createDownloadlink("directhttp://" + externID);
            dl.setFinalFileName(filename + ".flv");
            decryptedLinks.add(dl);
            return decryptedLinks;

        }
        externID = br.getRegex("static\\.xvideos\\.com/swf/.*?value=\"id_video=(\\d+)\"").getMatch(0);
        if (externID != null) {
            final DownloadLink dl = createDownloadlink("http://www.xvideos.com/video" + externID);
            decryptedLinks.add(dl);
            return decryptedLinks;

        }
        logger.warning("Decrypter broken for link: " + parameter);
        return null;
    }

    /* NO OVERRIDE!! */
    public boolean hasCaptcha(CryptedLink link, jd.plugins.Account acc) {
        return false;
    }

}