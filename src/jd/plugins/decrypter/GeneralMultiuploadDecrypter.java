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

import org.jdownloader.captcha.v2.challenge.recaptcha.v2.CaptchaHelperCrawlerPluginRecaptchaV2;
import org.jdownloader.plugins.components.antiDDoSForDecrypt;
import org.jdownloader.scripting.JavaScriptEngineFactory;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Random;

import jd.PluginWrapper;
import jd.controlling.ProgressController;
import jd.http.Browser;
import jd.http.Browser.BrowserException;
import jd.http.Request;
import jd.nutils.JDHash;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.parser.html.Form;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterException;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.PluginException;
import jd.plugins.components.SiteType.SiteTemplate;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 3,

        names = { "mirrorcop.com", "multiupfile.com", "multfile.com", "maxmirror.com", "exoshare.com", "go4up.com", "uploadonall.com", "qooy.com", "uploader.ro", "uploadmirrors.com", "megaupper.com", "calabox.com" },

        urls = { "http://(www\\.)?mirrorcop\\.com/downloads/[A-Z0-9]+", "http://(www\\.)?multiupfile\\.com/f/[a-f0-9]+", "http://(www\\.)?multfile\\.com/files/[0-9A-Za-z]{1,15}", "http://(www\\.)?maxmirror\\.com/download/[0-9A-Z]{8}", "http://(www\\.)?(exoshare\\.com|multi\\.la)/(download\\.php\\?uid=|s/)[A-Z0-9]{8}", "https?://(\\w+\\.)?go4up\\.com/(dl/|link\\.php\\?id=)\\w{1,15}", "https?://(www\\.)?uploadonall\\.com/(download|files)/[A-Z0-9]{8}", "http://(www\\.)?qooy\\.com/files/[0-9A-Z]{8,10}", "http://[\\w\\.]*?uploader\\.ro/files/[0-9A-Z]{8}", "http://[\\w\\.]*?uploadmirrors\\.(com|org)/download/[0-9A-Z]{8}", "http://[\\w\\.]*?megaupper\\.com/files/[0-9A-Z]{8}", "http://[\\w\\.]*?(?:shrta|calabox)\\.com/files/[0-9A-Z]{8}" }

)
public class GeneralMultiuploadDecrypter extends antiDDoSForDecrypt {

    public GeneralMultiuploadDecrypter(PluginWrapper wrapper) {
        super(wrapper);
    }

    // Tags: Multi file upload, mirror, mirrorstack, GeneralMultiuploadDecrypter

    private final String DEFAULTREGEX = "<frame name=\"main\" src=\"(.*?)\">";
    private CryptedLink  param;

    @Override
    protected boolean useRUA() {
        return true;
    }

    @Override
    protected Browser prepBrowser(Browser prepBr, String host) {
        if (!(this.browserPrepped.containsKey(prepBr) && this.browserPrepped.get(prepBr) == Boolean.TRUE)) {
            super.prepBrowser(prepBr, host);
            prepBr.setReadTimeout(2 * 60 * 1000);
            prepBr.setConnectTimeout(2 * 60 * 1000);
            prepBr.setFollowRedirects(true);
        }
        return prepBr;
    }

    // This decrypter should handle nearly all sites using the qooy.com script!
    public ArrayList<DownloadLink> decryptIt(final CryptedLink param, final ProgressController progress) throws Exception {
        this.param = param;
        final ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        final LinkedHashSet<String> dupeList = new LinkedHashSet<String>();
        Browser brc;
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
            brc = br.cloneBrowser();
            getPage(brc, status);
        } else if (parameter.contains("go4up.com/")) {
            getPage(br, parameter);
            if (br.containsHTML(">File not Found<")) {
                decryptedLinks.add(createOfflinelink(parameter));
                return decryptedLinks;
            }
            // we apparently need a filename
            customFileName = br.getRegex("<title>Download (.*?)</title>").getMatch(0);
            // if (br.containsHTML("golink")) br.postPage(br.getURL(), "golink=Access+Links");
            brc = br.cloneBrowser();
            // this is required!
            br.setCookie(this.getHost(), "__unam", determineHash());
            getPage(brc, "/download/gethosts/" + id + "/" + customFileName);
            final String urls[] = brc.getRegex("\"link\":\"(.*?)\",\"button\"").getColumn(0);
            final String urls_broken[] = brc.getRegex("\"link\":\"(File currently in queue\\.|Error occured)\"").getColumn(0);
            if (urls.length == urls_broken.length) {
                final DownloadLink link;
                /* None of these mirrors was successfully uploaded --> Link offline! */
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
            brc = br.cloneBrowser();
            postPage(brc, br.getURL(), "YII_CSRF_TOKEN=" + token + "&pssd=" + pssd);
        } else {
            brc = br.cloneBrowser();
            getPage(brc, Request.getLocation("/status.php?uid=" + id, br.createGetRequest(parameter)));
        }
        /* Error handling */
        if (!br.containsHTML("<img src=") && !br.containsHTML("<td class=\"host\">")) {
            logger.info("The following link should be offline: " + param.toString());
            decryptedLinks.add(createOfflinelink(parameter));
            return decryptedLinks;
        }
        br.setFollowRedirects(false);
        final ArrayList<String> redirectLinks = getRedirectsLinks(brc);
        if (redirectLinks == null || redirectLinks.isEmpty()) {
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
        logger.info("Found " + redirectLinks.size() + " " + " links to decrypt...");
        String fileName = null;
        if (parameter.contains("mirrorcop")) {
            brc = br.cloneBrowser();
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
            brc = br.cloneBrowser();
            String dllink = null;
            // Handling for links that need to be regexed or that need to be get by redirect
            if (singleLink.matches("(?i)/(redirect|rd?|dl|mirror).+")) {
                getPage(brc, singleLink);
                handleCaptcha(brc);
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

    /**
     * method is required because some are claimed to be offline/dead. These still have active links and can contain captcha.
     *
     * @return
     * @throws Exception
     */
    private ArrayList<String> getRedirectsLinks(final Browser br) throws Exception {
        // try with json, not sure which hosts use what though try catch will help as final failover anyway.
        try {
            final ArrayList<String> links = new ArrayList<String>();
            final ArrayList<Object> jsonArray = (ArrayList<Object>) JavaScriptEngineFactory.jsonToJavaObject(br.toString());
            for (final Object jsonObject : jsonArray) {
                final LinkedHashMap<String, Object> json = (LinkedHashMap<String, Object>) jsonObject;
                final String status = (String) json.get("status");
                if (!"ok".equalsIgnoreCase(status)) {
                    continue;
                }
                final String link = (String) json.get("link");
                if (link != null) {
                    final String url = new Regex(link, "href=(\"|'|)(.*?)\\1").getMatch(1);
                    if (url != null) {
                        links.add(url);
                    }
                }
            }
            return links;
        } catch (final Exception t) {

        }
        String[] redirectLinks = br.getRegex("(/(rd?|redirect|dl|mirror)/[0-9A-Z]+/[a-z0-9]+)").getColumn(0);
        if (redirectLinks == null || redirectLinks.length == 0) {
            redirectLinks = br.getRegex("><a href=(.*?)target=").getColumn(0);
        }
        return redirectLinks == null ? null : new ArrayList<String>(Arrays.asList(redirectLinks));
    }

    /**
     * can contain captcha task here
     *
     * @throws DecrypterException
     * @throws InterruptedException
     * @throws PluginException
     *
     * @raztoki
     */
    private void handleCaptcha(final Browser br) throws Exception {
        final Form captcha;
        {
            Form f = br.getFormByInputFieldPropertyKeyValue("id", "captchaInput");
            if (f == null) {
                f = br.getFormbyProperty("id", "captcha");
            }
            if (f == null) {
                return;
            } else {
                captcha = f;
            }
        }
        // recaptcha
        if (captcha.containsHTML("class=(\"|')g-recaptcha\\1")) {
            final String recaptchaV2Response = new CaptchaHelperCrawlerPluginRecaptchaV2(this, br) {

                {
                    // since i made support earlier in the week, this is now required O_o
                    if ("go4up.com".equals(getHost())) {
                        boundToDomain = true;
                    }
                }

                @Override
                public String getSiteKey() {
                    return getSiteKey(captcha.getHtmlCode());
                };

            }.getToken();
            // some reason twice.
            if ("go4up.com".equals(getHost())) {
                // altered length...
                captcha.put("captcha-response", Encoding.urlEncode(recaptchaV2Response));
                // no referrer
                br.getHeaders().put("Referer", null);
                // also note that captcha form is present again even though answer is right. though the link is in page
            }
            captcha.put("g-recaptcha-response", Encoding.urlEncode(recaptchaV2Response));
            submitForm(br, captcha);
        }
    }

    private String decryptLink(Browser brc, final String parameter) throws Exception {
        String dllink = null;
        if (parameter.contains("go4up.com/")) {
            dllink = brc.getRedirectLocation();
            if (dllink == null) {
                dllink = brc.getRegex("window\\.location = (\"|')(http.*?)\\1").getMatch(1);
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

    @Override
    protected void getPage(final Browser ibr, final String url) throws Exception {
        if (ibr == null || url == null) {
            return;
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
                super.getPage(ibr, url);
                return;
            } catch (final BrowserException e) {
                failed = true;
                continue;
            }
        }
        if (failed) {
            logger.warning("Exausted getPage retry count");
        }
        return;
    }

    @Override
    protected void postPage(final Browser ibr, final String url, final String args) throws Exception {
        if (ibr == null || url == null) {
            return;
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
                super.postPage(ibr, url, args);
                return;
            } catch (final BrowserException e) {
                failed = true;
                continue;
            }
        }
        if (failed) {
            logger.warning("Exausted getPage retry count");
        }
        return;
    }

    private String determineHash() throws Exception {
        try {
            final String d = JDHash.getCRC32("" + Math.round(Math.random() * 2147483647));
            final String g = JDHash.getCRC32("" + System.currentTimeMillis());
            final String f = "c22e7e7";
            return f.concat("-").concat(d).concat("-").concat(g).concat("-2");
        } catch (Exception e) {
            throw e;
        }
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