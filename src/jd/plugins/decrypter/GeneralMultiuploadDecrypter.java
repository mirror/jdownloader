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
import java.util.LinkedHashSet;
import java.util.Random;

import jd.PluginWrapper;
import jd.controlling.ProgressController;
import jd.http.Browser;
import jd.parser.Regex;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.PluginForDecrypt;
import jd.utils.JDUtilities;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2,

names = { "multiupfile.com", "multfile.com", "filetobox.com", "maxmirror.com", "exzip.net", "bikupload.com", "uploadseeds.com", "indirbindir.biz", "createmirror.com", "exoshare.com", "3ll3.in", "go4up.com", "uploadonall.com", "up4vn.com", "directmirror.com", "nextdown.net", "mirrorafile.com", "lougyl.com", "neo-share.com", "qooy.com", "uploader.ro", "uploadmirrors.com", "indirdur.net", "megaupper.com", "shrta.com", "1filesharing.com", "mirrorfusion.com", "needmirror.com" },

urls = { "http://(www\\.)?multiupfile\\.com/f/[a-f0-9]+", "http://(www\\.)?multfile\\.com/files/[0-9A-Za-z]{1,15}", "http://(www\\.)?filetobox\\.com/download\\.php\\?uid=[0-9A-Z]{8}", "http://(www\\.)?maxmirror\\.com/download/[0-9A-Z]{8}", "http://(www\\.)?exzip\\.net/download/[0-9A-Z]{8}", "http://(www\\.)?bikupload\\.com/download\\.php\\?uid=[0-9A-Z]{8}", "http://(www\\.)?uploadseeds\\.com/(download\\.php\\?uid=|download/)[0-9A-Z]{8}", "http://(www\\.)?indirbindir\\.biz/files/[0-9A-Z]{8}", "http://(www\\.)?createmirror\\.com/files/[0-9A-Z]{8}", "http://(www\\.)?(exoshare\\.com|multi\\.la)/(download\\.php\\?uid=|s/)[A-Z0-9]{8}", "http://(www\\.)?3ll3\\.in/(files|dl)/\\w{14,18}", "http://(www\\.)?go4up\\.com/(dl/|link\\.php\\?id=)\\w{1,15}", "https?://(www\\.)?uploadonall\\.com/(download|files)/[A-Z0-9]{8}", "https?://(www\\.)?up4vn\\.com/\\?go=[A-Z0-9]{8}",
        "http://(www\\.)?nextdown\\.net/files/[0-9A-Z]{8}", "http://(www\\.)?directmirror\\.com/files/[0-9A-Z]{8}", "http://[\\w\\.]*?mirrorafile\\.com/files/[0-9A-Z]{8}", "http://[\\w\\.]*?lougyl\\.com/files/[0-9A-Z]{8}", "http://(www\\.)?neo\\-share\\.com/files/[0-9A-Z]{8}", "http://(www\\.)?qooy\\.com/files/[0-9A-Z]{8,10}", "http://[\\w\\.]*?uploader\\.ro/files/[0-9A-Z]{8}", "http://[\\w\\.]*?uploadmirrors\\.(com|org)/download/[0-9A-Z]{8}", "http://[\\w\\.]*?indirdur\\.net/files/[0-9A-Z]{8}", "http://[\\w\\.]*?megaupper\\.com/files/[0-9A-Z]{8}", "http://[\\w\\.]*?shrta\\.com/files/[0-9A-Z]{8}", "http://[\\w\\.]*?1filesharing\\.com/(mirror|download)/[0-9A-Z]{8}", "http://[\\w\\.]*?mirrorfusion\\.com/files/[0-9A-Z]{8}", "http://(www\\.)?needmirror\\.com/files/[0-9A-Z]{8}" },

flags = { 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0 })
public class GeneralMultiuploadDecrypter extends PluginForDecrypt {

    public GeneralMultiuploadDecrypter(PluginWrapper wrapper) {
        super(wrapper);
    }

    private final String DEFAULTREGEX = "<frame name=\"main\" src=\"(.*?)\">";

    /**
     * defines custom browser requirements.
     * */
    private Browser prepBrowser(Browser prepBr) {
        /* we first have to load the plugin, before we can reference it */
        JDUtilities.getPluginForHost("mediafire.com");
        prepBr.getHeaders().put("User-Agent", jd.plugins.hoster.MediafireCom.stringUserAgent());
        prepBr.getHeaders().put("Accept-Language", "en-gb, en;q=0.8");
        prepBr.getHeaders().put("Accept-Charset", null);
        prepBr.getHeaders().put("Pragma", null);
        prepBr.setReadTimeout(3 * 60 * 1000);
        prepBr.setFollowRedirects(true);
        return prepBr;
    }

    // This decrypter should handle nearly all sites using the qooy.com script!
    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        LinkedHashSet<String> dupeList = new LinkedHashSet<String>();
        prepBrowser(br);
        String parameter = param.toString();
        // Only uploadmirrors.com has those "/download/" links so we need to
        // correct them
        if (parameter.contains("go4up.com")) {
            parameter = parameter.replace("link.php?id=", "dl/");
        } else if (parameter.contains("uploadmirrors.com/")) {
        } else if (parameter.matches("http://(www\\.)?(exoshare\\.com|multi\\.la)/(download\\.php\\?uid=|s/)[A-Z0-9]{8}")) {
            parameter = "http://exoshare.com/download.php?uid=" + new Regex(parameter, "([A-Z0-9]{8})$").getMatch(0);
        } else {
            parameter = parameter.replaceAll("(/dl/|/mirror/|/download/)", "/files/").replace("flameupload.co/", "flameupload.com/");
        }
        // Links need a "/" at the end to be valid
        if (!param.getCryptedUrl().matches(".+(up4vn\\.com/|exoshare\\.com/).+") && !param.getCryptedUrl().endsWith("/")) param.setCryptedUrl(param.getCryptedUrl().toString() + "/");
        String protocol = new Regex(parameter, "(https?://)").getMatch(0);
        String host = new Regex(parameter, "://([^/]+)/").getMatch(0);
        String id = new Regex(parameter, "https?://.+/(\\?go=|download\\.php\\?uid=)?([0-9A-Za-z]{8,18})").getMatch(1);
        // This should never happen but in case a dev changes the plugin without
        // much testing he'll see the error later!
        if (host == null || id == null) {
            logger.warning("A critical error happened! Please inform the support. : " + param.toString());
            return null;
        }
        if (parameter.contains("3ll3.in/")) {
            // do some more processing as status always claims its been removed.
            getPage(br, parameter);
            String url = br.getRegex("url=(http[^\"]+)").getMatch(0);
            if (url == null) {
                logger.warning("Couldn't find url=!! Please inform the support. : " + param.toString());
            } else
                getPage(br, url);
        } else if (parameter.contains("uploadmirrors.com")) {
            getPage(br, parameter);
            String status = br.getRegex("ajaxRequest\\.open\\(\"GET\", \"(/[A-Za-z0-9]+\\.php\\?uid=" + id + "&name=[^<>\"/]*?)\"").getMatch(0);
            if (status == null) {
                logger.warning("Couldn't find status : " + param.toString());
                return null;
            }
            getPage(br, status);
        } else if (parameter.contains("go4up.com/")) {
            getPage(br, parameter);
            if (br.containsHTML(">File not Found<")) {
                logger.info("Link offline: " + parameter);
                return decryptedLinks;
            }
            // if (br.containsHTML("golink")) br.postPage(br.getURL(), "golink=Access+Links");
            br.getPage("http://go4up.com/download/gethosts/" + new Regex(parameter, "(\\w{1,15})/?$").getMatch(0));
        } else if (parameter.matches("(?i).+(up4vn\\.com|multiupfile\\.com)/.+")) {
            // use standard page, status.php doesn't exist
            getPage(br, parameter);
        } else {
            getPage(br, protocol + host + "/status.php?uid=" + id);
        }
        /* Error handling */
        if (!br.containsHTML("<img src=") && !br.containsHTML("<td class=\"host\">") || ((parameter.contains("3ll3.in/")) && br.containsHTML("<h1>FILE NOT FOUND</h1>"))) {
            logger.info("The following link should be offline: " + param.toString());
            return decryptedLinks;
        }
        br.setFollowRedirects(false);
        String[] redirectLinks = br.getRegex("(/(rd?|redirect|dl|mirror)/[0-9A-Z]+/[a-z0-9]+)").getColumn(0);
        if (redirectLinks == null || redirectLinks.length == 0) redirectLinks = br.getRegex("><a href=(.*?)target=").getColumn(0);
        if (redirectLinks == null || redirectLinks.length == 0) {
            // So far only tested for maxmirror.com, happens when all links have
            // the status "Unavailable"
            if (br.containsHTML("<td><img src=/images/Upload\\.gif")) {
                logger.info("All links are unavailable: " + parameter);
                return decryptedLinks;
            } else if (host.contains("1filesharing.com") && br.containsHTML("/images/removed\\.gif")) {
                logger.info("All links are unavailable (abused): " + parameter);
                return decryptedLinks;
            }
            logger.warning("Decrypter broken for link: " + parameter);
            return null;
        }
        logger.info("Found " + redirectLinks.length + " " + host.replaceAll("www\\.", "") + " links to decrypt...");
        for (String singleLink : redirectLinks) {
            if (!dupeList.add(singleLink)) continue;
            singleLink = singleLink.replace("\"", "").trim();
            Browser brc = br.cloneBrowser();
            String dllink = null;
            // Handling for links that need to be regexed or that need to be get
            // by redirect
            if (singleLink.matches("(?i)/(redirect|rd?|dl|mirror).+")) {
                getPage(brc, singleLink);
                dllink = decryptLink(brc, parameter);
            } else {
                // Handling for already regexed final-links
                dllink = singleLink;
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

    private String decryptLink(Browser brc, final String parameter) throws Exception {
        String dllink = null;
        if (parameter.contains("flameupload.com/")) {
            dllink = brc.getRegex(">Download Link:<br><a href=\"([^\"]+)").getMatch(0);
        } else if (parameter.contains("1filesharing.com/")) {
            dllink = brc.getRegex("<iframe id=\"download\" src=\"(.*?)\"").getMatch(0);
        } else if (parameter.contains("needmirror.com/")) {
            dllink = brc.getRegex(">Please <a href=\"([^\"\\']+)\"").getMatch(0);
        } else if (parameter.contains("go4up.com/")) {
            dllink = brc.getRedirectLocation();
            if (dllink == null) {
                dllink = brc.getRegex("window\\.location = (\"|\\')(http[^<>\"]*?)(\"|\\')").getMatch(1);
                if (dllink == null) {
                    dllink = brc.getRegex("<b><a href=\"([^\"]+)").getMatch(0);
                }
            }
        } else if (parameter.contains("maxmirror.com/")) {
            dllink = brc.getRegex("\"(http[^<>\"]*?)\"><img border=\"0\" src=\"http://(www\\.)?maxmirror\\.com/").getMatch(0);
        } else if (parameter.matches(".+(qooy\\.com|multfile\\.com)/.+")) {
            dllink = brc.getRegex("<a style=\"text\\-decoration: none;border:none;\" href=\"(https?://[^<>\"]*?)\"").getMatch(0);
        } else if (parameter.contains("exzip.net/")) {
            dllink = brc.getRegex("\"(/down/[A-Z0-9]{8}/\\d+)\"").getMatch(0);
            if (dllink != null) {
                getPage(brc, dllink);
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

    private Browser getPage(Browser ibr, String url) throws Exception {
        if (ibr == null || url == null) return null;
        boolean failed = false;
        int repeat = 4;
        for (int i = 0; i <= repeat; i++) {
            long meep = new Random().nextInt(5) * 1000;
            if (failed) {
                Thread.sleep(meep);
                failed = false;
            }
            try {
                ibr.getPage(url);
                if (ibr.getRedirectLocation() != null || ibr.getURL().contains(url)) {
                    break;
                }
            } catch (Throwable e) {
                failed = true;
                continue;
            }
        }
        if (failed) {
            logger.warning("Exausted getPage retry count");
        }
        return ibr;
    }

    /* NO OVERRIDE!! */
    public boolean hasCaptcha(CryptedLink link, jd.plugins.Account acc) {
        return false;
    }

}