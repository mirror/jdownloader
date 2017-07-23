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
import jd.http.Browser.BrowserException;
import jd.http.Request;
import jd.parser.Regex;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.PluginForDecrypt;
import jd.plugins.components.SiteType.SiteTemplate;
import jd.plugins.components.UserAgents;
import jd.utils.JDUtilities;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 3,

        names = { "mirrorcop.com", "multiupfile.com", "multfile.com", "maxmirror.com", "exoshare.com", "go4up.com", "uploadonall.com", "qooy.com", "uploader.ro", "uploadmirrors.com", "megaupper.com", "calabox.com" },

        urls = { "http://(www\\.)?mirrorcop\\.com/downloads/[A-Z0-9]+", "http://(www\\.)?multiupfile\\.com/f/[a-f0-9]+", "http://(www\\.)?multfile\\.com/files/[0-9A-Za-z]{1,15}", "http://(www\\.)?maxmirror\\.com/download/[0-9A-Z]{8}", "http://(www\\.)?(exoshare\\.com|multi\\.la)/(download\\.php\\?uid=|s/)[A-Z0-9]{8}", "https?://(\\w+\\.)?go4up\\.com/(dl/|link\\.php\\?id=)\\w{1,15}", "https?://(www\\.)?uploadonall\\.com/(download|files)/[A-Z0-9]{8}", "http://(www\\.)?qooy\\.com/files/[0-9A-Z]{8,10}", "http://[\\w\\.]*?uploader\\.ro/files/[0-9A-Z]{8}", "http://[\\w\\.]*?uploadmirrors\\.(com|org)/download/[0-9A-Z]{8}", "http://[\\w\\.]*?megaupper\\.com/files/[0-9A-Z]{8}", "http://[\\w\\.]*?(?:shrta|calabox)\\.com/files/[0-9A-Z]{8}" }

)
public class GeneralMultiuploadDecrypter extends PluginForDecrypt {

    public GeneralMultiuploadDecrypter(PluginWrapper wrapper) {
        super(wrapper);
    }

    // Tags: Multi file upload, mirror, mirrorstack, GeneralMultiuploadDecrypter

    private final String DEFAULTREGEX = "<frame name=\"main\" src=\"(.*?)\">";
    private CryptedLink  param;

    /**
     * defines custom browser requirements.
     */
    private Browser prepBrowser(Browser prepBr) {
        /* we first have to load the plugin, before we can reference it */
        JDUtilities.getPluginForHost("mediafire.com");
        prepBr.getHeaders().put("User-Agent", UserAgents.stringUserAgent());
        prepBr.getHeaders().put("Accept-Language", "en-gb, en;q=0.8");
        prepBr.getHeaders().put("Accept-Charset", null);
        prepBr.getHeaders().put("Pragma", null);
        prepBr.setReadTimeout(2 * 60 * 1000);
        prepBr.setConnectTimeout(2 * 60 * 1000);
        prepBr.setFollowRedirects(true);
        return prepBr;
    }

    // This decrypter should handle nearly all sites using the qooy.com script!
    public ArrayList<DownloadLink> decryptIt(final CryptedLink param, final ProgressController progress) throws Exception {
        this.param = param;
        final ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        final LinkedHashSet<String> dupeList = new LinkedHashSet<String>();
        prepBrowser(br);
        String parameter = param.toString();
        // Only uploadmirrors.com has those "/download/" links so we need to correct them
        if (parameter.contains("go4up.com")) {
            parameter = parameter.replace("link.php?id=", "dl/").replace("http://", "https://");
        } else if (parameter.contains("uploadmirrors.com/")) {
        } else if (parameter.matches("http://(www\\.)?(exoshare\\.com|multi\\.la)/(download\\.php\\?uid=|s/)[A-Z0-9]{8}")) {
            parameter = "http://exoshare.com/download.php?uid=" + new Regex(parameter, "([A-Z0-9]{8})$").getMatch(0);
        } else {
            parameter = parameter.replaceAll("(/dl/|/mirror/|/download/)", "/files/");
        }
        // Links need a "/" at the end to be valid
        if (!param.getCryptedUrl().matches(".+exoshare\\.com/.+") && !param.getCryptedUrl().endsWith("/")) {
            param.setCryptedUrl(param.getCryptedUrl().toString() + "/");
        }
        String id = new Regex(parameter, "https?://.+/(\\?go=|download\\.php\\?uid=)?([0-9A-Za-z]{8,18})").getMatch(1);
        if (id == null && parameter.matches("(?i).+multiupfile\\.com/.+")) {
            id = new Regex(parameter, "([A-Za-z0-9]+)/?$").getMatch(0);
        }
        // This should never happen but in case a dev changes the plugin without
        // much testing he'll see the error later!
        if (id == null) {
            logger.warning("A critical error happened! Please inform the support. : " + param.toString());
            return null;
        }
        String customFileName = null;
        if (parameter.contains("uploadmirrors.com")) {
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
                decryptedLinks.add(createOfflinelink(parameter));
                return decryptedLinks;
            }
            // we apparently need a filename
            customFileName = br.getRegex("<title>Download (.*?)</title>").getMatch(0);
            // if (br.containsHTML("golink")) br.postPage(br.getURL(), "golink=Access+Links");
            getPage(br, "/download/gethosts/" + id + "/" + customFileName);
            br.getRequest().setHtmlCode(br.toString().replaceAll("\\\\/", "/").replaceAll("\\\\\"", "\""));
            final String urls[] = this.br.getRegex("\"link\":\"(.*?)\",\"button\"").getColumn(0);
            final String urls_broken[] = this.br.getRegex("\"link\":\"(File currently in queue\\.|Error occured)\"").getColumn(0);
            if (urls.length == urls_broken.length) {
                final DownloadLink link;
                /* No ne of these mirrors was successfully uploaded --> Link offline! */
                decryptedLinks.add(link = createOfflinelink(parameter));
                link.setName(customFileName);
                return decryptedLinks;
            }
        } else if (parameter.matches("(?i).+multiupfile\\.com/.+")) {
            // use standard page, status.php doesn't exist
            // br.getHeaders().put("Accept-Encoding", "identity");
            getPage(br, parameter);
            if (br.containsHTML(">File not found<")) {
                decryptedLinks.add(createOfflinelink(parameter));
                return decryptedLinks;
            }
            final String token = br.getRegex("value=\"([a-z0-9]+)\" name=\"YII_CSRF_TOKEN\"").getMatch(0);
            if (token == null) {
                logger.warning("Decrypter broken for link: " + param.toString());
                return null;
            }
            String pssd = br.getRegex("name=\"pssd\" type=\"hidden\" value=\"([a-z0-9]+)\"").getMatch(0);
            if (pssd == null) {
                pssd = id;
            }
            postPage(br, br.getURL(), "YII_CSRF_TOKEN=" + token + "&pssd=" + pssd);
        } else {
            getPage(br, Request.getLocation("/status.php?uid=" + id, br.createGetRequest(parameter)));
        }
        /* Error handling */
        if (!br.containsHTML("<img src=") && !br.containsHTML("<td class=\"host\">")) {
            logger.info("The following link should be offline: " + param.toString());
            decryptedLinks.add(createOfflinelink(parameter));
            return decryptedLinks;
        }
        br.setFollowRedirects(false);
        String[] redirectLinks = br.getRegex("(/(rd?|redirect|dl|mirror)/[0-9A-Z]+/[a-z0-9]+)").getColumn(0);
        if (redirectLinks == null || redirectLinks.length == 0) {
            redirectLinks = br.getRegex("><a href=(.*?)target=").getColumn(0);
        }
        if (redirectLinks == null || redirectLinks.length == 0) {
            // So far only tested for maxmirror.com, happens when all links have
            // the status "Unavailable"
            if (br.containsHTML("<td><img src=/images/Upload\\.gif")) {
                logger.info("All links are unavailable: " + parameter);
                return decryptedLinks;
            }
            // exoshare have just advertising crapola for dead/offline links
            if ("exoshare.com".equals(getHost())) {
                return decryptedLinks;
            }
            logger.warning("Decrypter broken for link: " + parameter);
            return null;
        }
        logger.info("Found " + redirectLinks.length + " " + " links to decrypt...");
        String fileName = null;
        if (parameter.contains("mirrorcop")) {
            final Browser brc = br.cloneBrowser();
            getPage(brc, parameter);
            fileName = brc.getRegex("h3 style=\"color:.*?\">Name :(.*?)</h3").getMatch(0);
            if (fileName != null) {
                fileName = fileName.trim();
            }
        } else {
            fileName = customFileName;
        }
        for (String singleLink : redirectLinks) {
            if (!dupeList.add(singleLink)) {
                continue;
            }
            singleLink = singleLink.replace("\"", "").trim();
            final Browser brc = br.cloneBrowser();
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
                if (brc.containsHTML("Error link not available")) {
                    logger.info("No link Available");
                } else {
                    logger.warning("Possible plugin error: " + param.toString());
                }
                logger.warning("Continuing...");
                continue;
            }
            final DownloadLink link = createDownloadlink(dllink);
            if (fileName != null) {
                link.setName(fileName);
            }
            decryptedLinks.add(link);
        }
        logger.info("Task Complete! : " + param.toString());
        return decryptedLinks;
    }

    private String decryptLink(Browser brc, final String parameter) throws Exception {
        String dllink = null;
        if (parameter.contains("go4up.com/")) {
            dllink = brc.getRedirectLocation();
            if (dllink == null) {
                dllink = brc.getRegex("window\\.location = (\"|\\')(http.*?)\\1").getMatch(1);
                if (dllink == null) {
                    dllink = brc.getRegex("<b><a href=\"([^\"]+)").getMatch(0);
                }
            }
        } else if (parameter.contains("maxmirror.com/")) {
            dllink = brc.getRegex("\"(http[^<>\"]*?)\"><img border=\"0\" src=\"http://(www\\.)?maxmirror\\.com/").getMatch(0);
        } else if (parameter.matches(".+(qooy\\.com|multfile\\.com|mirrorcop\\.com)/.+")) {
            dllink = brc.getRegex("<a style=\"text-decoration:\\s*none;\\s*border:\\s*none;\\s*\"\\s*href=(\"|')(https?://.*?)\\1").getMatch(1);
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

    private Browser getPage(final Browser ibr, final String url) throws Exception {
        if (ibr == null || url == null) {
            return null;
        }
        boolean failed = false;
        int repeat = 4;
        for (int i = 0; i <= repeat; i++) {
            if (failed) {
                long meep = new Random().nextInt(5) * 1000;
                sleep(meep, param);
                failed = false;
            }
            try {
                ibr.getPage(url);
                if (ibr.getRedirectLocation() != null || ibr.getURL().contains(url)) {
                    break;
                }
            } catch (final BrowserException e) {
                failed = true;
                continue;
            }
        }
        if (failed) {
            logger.warning("Exausted getPage retry count");
        }
        return ibr;
    }

    private Browser postPage(final Browser ibr, final String url, final String args) throws Exception {
        if (ibr == null || url == null) {
            return null;
        }
        boolean failed = false;
        int repeat = 4;
        for (int i = 0; i <= repeat; i++) {
            if (failed) {
                long meep = new Random().nextInt(5) * 1000;
                sleep(meep, param);
                failed = false;
            }
            try {
                ibr.postPage(url, args);
                if (ibr.getRedirectLocation() != null || ibr.getURL().contains(url)) {
                    break;
                }
            } catch (final BrowserException e) {
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

    @Override
    public SiteTemplate siteTemplateType() {
        return SiteTemplate.Qooy_Mirrors;
    }

}