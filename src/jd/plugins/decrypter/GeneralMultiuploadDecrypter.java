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

import jd.PluginWrapper;
import jd.controlling.ProgressController;
import jd.http.Browser;
import jd.parser.Regex;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterException;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.PluginForDecrypt;
import jd.utils.locale.JDL;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "maxmirror.com", "exzip.net", "bikupload.com", "uploadseeds.com", "indirbindir.biz", "createmirror.com", "exoshare.com", "3ll3.in", "go4up.com", "uploadonall.com", "up4vn.com", "directmirror.com", "nextdown.net", "bitsor.com", "mirrorafile.com", "lougyl.com", "neo-share.com", "qooy.com", "share2many.com", "uploader.ro", "uploadmirrors.com", "indirdur.net", "megaupper.com", "shrta.com", "1filesharing.com", "mirrorfusion.com", "digzip.com", "needmirror.com" }, urls = { "http://(www\\.)?maxmirror\\.com/download/[0-9A-Z]{8}", "http://(www\\.)?exzip\\.net/download/[0-9A-Z]{8}", "http://(www\\.)?bikupload\\.com/download\\.php\\?uid=[0-9A-Z]{8}", "http://(www\\.)?uploadseeds\\.com/download\\.php\\?uid=[0-9A-Z]{8}", "http://(www\\.)?indirbindir\\.biz/files/[0-9A-Z]{8}",
        "http://(www\\.)?createmirror\\.com/files/[0-9A-Z]{8}", "http://(www\\.)?exoshare\\.com/download\\.php\\?uid=[A-Z0-9]{8}", "http://(www\\.)?3ll3\\.in/(files|dl)/\\w{14,18}", "http://(www\\.)?go4up\\.com/(dl/|link\\.php\\?id=)\\w{1,15}", "https?://(www\\.)?uploadonall\\.com/(download|files)/[A-Z0-9]{8}", "https?://(www\\.)?up4vn\\.com/\\?go=[A-Z0-9]{8}", "http://(www\\.)?nextdown\\.net/files/[0-9A-Z]{8}", "http://(www\\.)?directmirror\\.com/files/[0-9A-Z]{8}", "http://(www\\.)?bitsor\\.com/files/[0-9A-Z]{8}", "http://[\\w\\.]*?mirrorafile\\.com/files/[0-9A-Z]{8}", "http://[\\w\\.]*?lougyl\\.com/files/[0-9A-Z]{8}", "http://(www\\.)?neo\\-share\\.com/files/[0-9A-Z]{8}", "http://(www\\.)?qooy\\.com/files/[0-9A-Z]{8,10}", "http://[\\w\\.]*?share2many\\.com/files/[0-9A-Z]{8}", "http://[\\w\\.]*?uploader\\.ro/files/[0-9A-Z]{8}",
        "http://[\\w\\.]*?uploadmirrors\\.(com|org)/download/[0-9A-Z]{8}", "http://[\\w\\.]*?indirdur\\.net/files/[0-9A-Z]{8}", "http://[\\w\\.]*?megaupper\\.com/files/[0-9A-Z]{8}", "http://[\\w\\.]*?shrta\\.com/files/[0-9A-Z]{8}", "http://[\\w\\.]*?1filesharing\\.com/(mirror|download)/[0-9A-Z]{8}", "http://[\\w\\.]*?mirrorfusion\\.com/files/[0-9A-Z]{8}", "http://(www\\.)?digzip\\.com/files/[0-9A-Z]{8}", "http://(www\\.)?needmirror\\.com/files/[0-9A-Z]{8}" }, flags = { 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0 })
public class GeneralMultiuploadDecrypter extends PluginForDecrypt {

    public GeneralMultiuploadDecrypter(PluginWrapper wrapper) {
        super(wrapper);
    }

    private static final String DEFAULTREGEX = "<frame name=\"main\" src=\"(.*?)\">";

    // This decrypter should handle nearly all sites using the qooy.com script!
    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        br.setFollowRedirects(true);
        br.setReadTimeout(3 * 60 * 1000);
        String parameter = param.toString();
        // Only uploadmirrors.com has those "/download/" links so we need to correct them
        if (parameter.contains("go4up.com")) {
            parameter = parameter.replace("link.php?id=", "dl/");
        } else if (parameter.contains("uploadmirrors.com/")) {
        } else {

            parameter = parameter.replaceAll("(/dl/|/mirror/|/download/)", "/files/").replace("flameupload.co/", "flameupload.com/");
        }
        // Links need a "/" at the end to be valid
        if (!param.getCryptedUrl().matches(".+(up4vn\\.com/|exoshare\\.com/).+") && !param.getCryptedUrl().endsWith("/")) param.setCryptedUrl(param.getCryptedUrl().toString() + "/");
        String protocol = new Regex(parameter, "(https?://)").getMatch(0);
        String host = new Regex(parameter, "://([^/]+)/").getMatch(0);
        String id = new Regex(parameter, "https?://.+/(\\?go=|download\\.php\\?uid=)?([0-9A-Za-z]{8,18})").getMatch(1);
        // This should never happen but in case a dev changes the plugin without much testing he'll see the error later!
        if (host == null || id == null) {
            logger.warning("A critical error happened! Please inform the support. : " + param.toString());
            return null;
        }
        if (parameter.contains("3ll3.in/")) {
            // do some more processing as status always claims its been removed.
            br.getPage(parameter);
            String url = br.getRegex("url=(http[^\"]+)").getMatch(0);
            if (url == null) {
                logger.warning("Couldn't find url=!! Please inform the support. : " + param.toString());
            } else
                br.getPage(url);
        } else if (parameter.contains("uploadmirrors.com")) {
            br.getPage(parameter);
            String status = br.getRegex("ajaxRequest\\.open\\(\"GET\", \"(/[A-Za-z0-9]+\\.php\\?uid=" + id + "&name=[^<>\"/]*?)\"").getMatch(0);
            if (status == null) {
                logger.warning("Couldn't find status : " + param.toString());
                return null;
            }
            br.getPage(protocol + host + status);
        } else if (parameter.matches(".+(up4vn\\.com|go4up\\.com)/.+")) {
            // use standard page, status.php doesn't exist
            br.getPage(parameter);
        } else {
            br.getPage(protocol + host + "/status.php?uid=" + id);
        }
        /* Error handling */
        if (!br.containsHTML("<img src=") && !br.containsHTML("<td class=\"host\">") || ((parameter.contains("3ll3.in/")) && br.containsHTML("<h1>FILE NOT FOUND</h1>"))) {
            logger.info("The following link should be offline: " + param.toString());
            throw new DecrypterException(JDL.L("plugins.decrypt.errormsg.unavailable", "Perhaps wrong URL or the download is not available anymore."));
        }
        br.setFollowRedirects(false);
        String[] redirectLinks = br.getRegex("(/(r|redirect|rd)/[0-9A-Z]+/[a-z0-9]+)").getColumn(0);
        if ((redirectLinks == null || redirectLinks.length == 0) && host.contains("3ll3.in")) redirectLinks = br.getRegex("(/dl/[0-9A-Z]+/[a-z0-9]+)").getColumn(0);
        if (redirectLinks == null || redirectLinks.length == 0) redirectLinks = br.getRegex("><a href=(.*?)target=").getColumn(0);
        if (redirectLinks == null || redirectLinks.length == 0) {
            logger.warning("Decrypter broken for link: " + parameter);
            return null;
        }
        logger.info("Found " + redirectLinks.length + " " + host.replaceAll("www\\.", "") + " links to decrypt...");
        for (String singlelink : redirectLinks) {
            singlelink = singlelink.replace("\"", "").trim();
            Browser brc = br.cloneBrowser();
            String dllink = null;
            // Handling for links that need to be regexed or that need to be get by redirect
            if (singlelink.contains("/redirect") || singlelink.contains("/rd/") || singlelink.matches("/r/.+") || singlelink.matches("/dl/.+") || singlelink.matches("/mirror/.+")) {
                brc.getPage(protocol + host + singlelink);
                dllink = decryptLink(brc, parameter);
            } else {
                // Handling for already regexed final-links
                dllink = singlelink;
            }
            if (dllink == null || dllink.equals("")) {
                // Continue away, randomised pages can cause failures.
                logger.warning("Possible plugin error: " + param.toString());
                logger.warning("Continuing...");
                continue;
            }
            if (dllink.contains("flameupload")) {
                logger.info("Recursion? " + param.toString() + "->" + dllink);
            }
            decryptedLinks.add(createDownloadlink(dllink));
        }
        logger.info("Task Complete! : " + param.toString());
        return decryptedLinks;
    }

    private String decryptLink(Browser brc, final String parameter) throws IOException {
        String dllink = null;
        if (parameter.contains("flameupload.com/")) {
            dllink = brc.getRegex(">Download Link:<br><a href=\"([^\"]+)").getMatch(0);
        } else if (parameter.contains("1filesharing.com/")) {
            dllink = brc.getRegex("<iframe id=\"download\" src=\"(.*?)\"").getMatch(0);
        } else if (parameter.contains("needmirror.com/")) {
            dllink = brc.getRegex(">Please <a href=\"([^\"\\']+)\"").getMatch(0);
        } else if (parameter.contains("go4up.com/")) {
            dllink = brc.getRegex("window\\.location = \"(http[^<>\"]*?)\"").getMatch(0);
        } else if (parameter.contains("qooy.com/") || parameter.contains("maxmirror.com/")) {
            dllink = brc.getRegex("<a style=\"text\\-decoration: none;border:none;\" href=\"(https?://[^<>\"]*?)\"").getMatch(0);
        } else if (parameter.contains("exzip.net/")) {
            dllink = brc.getRegex("\"(/down/[A-Z0-9]{8}/\\d+)\"").getMatch(0);
            if (dllink != null) {
                brc.getPage("http://www.exzip.net" + dllink);
                dllink = brc.getRegex(DEFAULTREGEX).getMatch(0);
            }
        } else {
            dllink = brc.getRedirectLocation();
            if (dllink == null) {
                dllink = brc.getRegex(DEFAULTREGEX).getMatch(0);
            }
        }
        return dllink;
    }
}
