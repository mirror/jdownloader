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
import jd.http.Browser;
import jd.parser.Regex;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.PluginForDecrypt;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "multisiteupload.com" }, urls = { "http://(www\\.)?multisiteupload\\.com/files/[0-9A-Z]{8}" }, flags = { 0 })
public class MultiSiteUploadCom extends PluginForDecrypt {

    public MultiSiteUploadCom(PluginWrapper wrapper) {
        super(wrapper);
    }

    // This decrypter should handle nearly all sites using the qooy.com script!
    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        br.setFollowRedirects(true);
        // Servers might be slow sometimes
        br.setConnectTimeout(3 * 60 * 1000);
        br.setReadTimeout(3 * 60 * 1000);
        String parameter = param.toString();
        // Tohse links need a "/" at the end to be valid
        if (!param.getCryptedUrl().endsWith("/")) param.setCryptedUrl(param.getCryptedUrl().toString() + "/");
        String host = "http://www.multisiteupload.com";
        String id = new Regex(parameter, "files/([0-9A-Z]+)").getMatch(0);
        // This should never happen but in case a dev changes the plugin without
        // much testing he ll see the error later!
        if (host == null || id == null) {
            logger.warning("A critical error happened! Please inform the support.");
            return null;
        }
        parameter = host + "/status.php?uid=" + id;
        br.getPage(parameter);
        /* Error handling */
        if (!br.containsHTML("<img src=") && !br.containsHTML("<td class=\"host\">")) {
            logger.info("The following link should be offline: " + parameter);
            return decryptedLinks;
        }
        br.setFollowRedirects(false);
        final String adfLink = br.getRegex("(http://adf\\.ly/\\d+/)").getMatch(0);
        String[] redirectLinks = br.getRegex("(/(redirect|rd)/[0-9A-Z]+/[a-z0-9]+)").getColumn(0);
        if (redirectLinks == null || redirectLinks.length == 0) redirectLinks = br.getRegex("><a href=(.*?)target=").getColumn(0);
        if (redirectLinks == null || redirectLinks.length == 0 || adfLink == null) {
            logger.info("No links are available: " + parameter);
            return decryptedLinks;
        }
        progress.setRange(redirectLinks.length);
        String nicelookinghost = host.replaceAll("(www\\.|http://|/)", "");
        logger.info("Found " + redirectLinks.length + " " + nicelookinghost + " links to decrypt...");
        for (String singlelink : redirectLinks) {
            Browser brc = br.cloneBrowser();
            String dllink = null;
            // Handling for links that need to be regexed or that need to be get
            // by redirect
            if (singlelink.contains("/redirect/") || singlelink.contains("/rd/")) {
                brc.getHeaders().put("Referer", adfLink + host.replace("www.", "") + singlelink);
                brc.getPage(host.replace("www.", "") + singlelink);
                dllink = brc.getRedirectLocation();
                if (dllink != null && dllink.contains("multisiteupload.com/")) {
                    brc.getPage(dllink);
                    dllink = null;
                }
                if (dllink == null) {
                    dllink = brc.getRegex("<frame name=\"main\" src=\"(.*?)\">").getMatch(0);
                    // For 1filesharing.com links
                    if (dllink == null) {
                        dllink = brc.getRegex("<iframe id=\"download\" src=\"(.*?)\"").getMatch(0);
                        // For needmirror.com links
                        if (dllink == null) dllink = brc.getRegex(">Please <a href=\"([^\"\\']+)\"").getMatch(0);
                    }
                }
            } else {
                // Handling for already regexed final-links
                dllink = singlelink;
            }
            progress.increase(1);
            if (dllink == null) continue;
            decryptedLinks.add(createDownloadlink(dllink));
        }
        return decryptedLinks;
    }

    /* NO OVERRIDE!! */
    public boolean hasCaptcha(CryptedLink link, jd.plugins.Account acc) {
        return false;
    }

}