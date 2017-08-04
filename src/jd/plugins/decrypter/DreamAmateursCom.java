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

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 3, names = { "dreamamateurs.com" }, urls = { "http://(www\\.)?dreamamateurs\\.com/(?:[A-Za-z0-9]+/\\d+/\\w+\\.html|link/\\d+/|\\d+/\\w+\\.html|\\w+/\\d+/\\w+)" })
public class DreamAmateursCom extends PornEmbedParser {

    public DreamAmateursCom(PluginWrapper wrapper) {
        super(wrapper);
    }

    /* DEV NOTES */
    /* Porn_plugin */

    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        final ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        br.setFollowRedirects(false);
        final String parameter = param.toString();
        br.getPage(parameter);
        String externID = br.getRedirectLocation();
        if (externID != null && !externID.contains("dreamamateurs.com/")) {
            decryptedLinks.add(createDownloadlink(externID));
            return decryptedLinks;
        } else if (externID != null) {
            /* Follow redirect */
            br.getPage(externID);
            externID = null;
        }
        if (br.getHttpConnection().getResponseCode() == 404) {
            decryptedLinks.add(this.createOfflinelink(parameter));
            return decryptedLinks;
        }
        String filename = br.getRegex("<h1 class=\"title\">\\s*(.*?)\\s*</h1>").getMatch(0);
        if (filename == null) {
            filename = br.getRegex("<meta name=\"description\" content=\"(.*?)\"").getMatch(0);
        }
        decryptedLinks.addAll(findEmbedUrls(filename));
        if (!decryptedLinks.isEmpty()) {
            return decryptedLinks;
        }
        if (filename == null) {
            logger.warning("Decrypter broken for link: " + parameter);
            return null;
        }
        filename = filename.trim();
        externID = br.getRegex("file=(http://hostave4\\.net/.*?)\\&screenfile").getMatch(0);
        if (externID != null) {
            final DownloadLink dl = createDownloadlink("directhttp://" + externID);
            dl.setFinalFileName(filename + ".flv");
            decryptedLinks.add(dl);
            return decryptedLinks;

        }
        externID = br.getRegex("file=(.*?)\\&link=http%3A%2F%2F").getMatch(0);
        if (externID != null) {
            final DownloadLink dl = createDownloadlink("directhttp://" + "http://flash.serious-cash.com/" + externID + ".flv");
            dl.setFinalFileName(filename + ".flv");
            decryptedLinks.add(dl);
            return decryptedLinks;

        }
        externID = br.getRegex("\\&file=(http://(www\\.)?revengetv\\.net/[^<>\"]*?)\\&beginimage").getMatch(0);
        if (externID != null) {
            final DownloadLink dl = createDownloadlink("directhttp://" + externID);
            dl.setFinalFileName(filename + ".flv");
            decryptedLinks.add(dl);
            return decryptedLinks;

        }
        externID = br.getRegex("flashvars=\"file=(http://(www\\.)?hostave3\\.net/[^<>\"]*?)\\&screenfile=").getMatch(0);
        if (externID != null) {
            final DownloadLink dl = createDownloadlink("directhttp://" + externID);
            dl.setFinalFileName(filename + ".flv");
            decryptedLinks.add(dl);
            return decryptedLinks;

        }
        externID = br.getRegex("\\&file=(https?://static\\.mofos\\.com/scenes/[^<>\"]*?)\\&").getMatch(0);
        if (externID != null) {
            final DownloadLink dl = createDownloadlink("directhttp://" + externID);
            dl.setFinalFileName(filename + ".flv");
            decryptedLinks.add(dl);
            return decryptedLinks;

        }
        externID = br.getRegex("so\\.addVariable\\(\\'file\\',\\'(http://(www\\.)?dreamamateurs\\.com/videos/[^<>\"]*?)\\'\\)").getMatch(0);
        if (externID != null) {
            final DownloadLink dl = createDownloadlink("directhttp://" + externID);
            dl.setFinalFileName(filename + ".flv");
            decryptedLinks.add(dl);
            return decryptedLinks;

        }
        if (br.containsHTML("\\&file=http://embed\\.kickassratios\\.com/")) {
            logger.info("Link offline: " + parameter);
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