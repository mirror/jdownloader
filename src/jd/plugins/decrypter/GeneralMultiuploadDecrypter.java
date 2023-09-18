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
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;

import org.jdownloader.captcha.v2.challenge.recaptcha.v2.CaptchaHelperCrawlerPluginRecaptchaV2;
import org.jdownloader.plugins.components.antiDDoSForDecrypt;
import org.jdownloader.scripting.JavaScriptEngineFactory;

import jd.PluginWrapper;
import jd.controlling.ProgressController;
import jd.http.Browser;
import jd.http.Browser.BrowserException;
import jd.http.Request;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.parser.html.Form;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterException;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.components.SiteType.SiteTemplate;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 3, names = { "uploadonall.com" }, urls = { "https?://(?:www\\.)?uploadonall\\.com/((download|files)/|download\\.php\\?uid=)[A-Z0-9]{8}" })
public class GeneralMultiuploadDecrypter extends antiDDoSForDecrypt {
    public GeneralMultiuploadDecrypter(PluginWrapper wrapper) {
        super(wrapper);
    }

    // Tags: Multi file upload, mirror, mirrorstack, GeneralMultiuploadDecrypter
    private final String DEFAULTREGEX = "<frame name=\"main\" src=\"(.*?)\">";
    private CryptedLink  param;
    private boolean      notOK        = false;

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
        final ArrayList<DownloadLink> ret = new ArrayList<DownloadLink>();
        final LinkedHashSet<String> dupeList = new LinkedHashSet<String>();
        final String parameter = param.getCryptedUrl().replaceAll("(/dl/|/mirror/|/download/)", "/files/");
        String id = new Regex(parameter, "https?://.+/(\\?go=|download\\.php\\?uid=)?([0-9A-Za-z]{8,18})").getMatch(1);
        // This should never happen but in case a dev changes the plugin without
        // much testing he'll see the error later!
        if (id == null) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        String customFileName = null;
        getPage(br, Request.getLocation("/status.php?uid=" + id, br.createGetRequest(parameter)));
        /* Error handling */
        final boolean looksLikeOffline = !br.containsHTML("<img src=") && !br.containsHTML("<td class=\"host\">");
        br.setFollowRedirects(false);
        final ArrayList<String> redirectLinks = getRedirectsLinks(br);
        if (redirectLinks == null || redirectLinks.isEmpty()) {
            // So far only tested for maxmirror.com, happens when all links have "Unavailable" status
            // and go4up.com when all links have !"ok" status
            if (looksLikeOffline) {
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            } else if (br.containsHTML("<td><img src=/images/Upload\\.gif") || notOK) {
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            }
            throw new DecrypterException("Decrypter broken for link: " + parameter);
        }
        logger.info("Found " + redirectLinks.size() + " " + " links to decrypt...");
        String fileName = customFileName;
        int numberofUnavailableItems = 0;
        for (String singleLink : redirectLinks) {
            if (!dupeList.add(singleLink)) {
                continue;
            }
            singleLink = singleLink.replace("\"", "").trim();
            String dllink = null;
            // Handling for links that need to be regexed or that need to be get by redirect
            if (singleLink.matches("(?i)/(redirect|rd?|dl|mirror).+")) {
                getPage(br, singleLink);
                handleCaptcha(br);
                dllink = decryptLink(br, parameter);
            } else {
                // Handling for already regexed final-links
                dllink = singleLink;
            }
            if (dllink == null || dllink.equals("")) {
                // Continue away, randomised pages can cause failures.
                if (br.containsHTML("Error link not available")) {
                    logger.info("No link Available");
                    numberofUnavailableItems++;
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
            ret.add(link);
        }
        if (numberofUnavailableItems == redirectLinks.size()) {
            /* All items are unavailable -> Content is offline. */
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        return ret;
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
            boolean noneOK = true;
            final ArrayList<String> links = new ArrayList<String>();
            final List<Object> jsonArray = (List<Object>) JavaScriptEngineFactory.jsonToJavaObject(br.toString());
            for (final Object jsonObject : jsonArray) {
                final Map<String, Object> json = (Map<String, Object>) jsonObject;
                final String status = (String) json.get("status");
                if (!"ok".equalsIgnoreCase(status) && !"checking".equalsIgnoreCase(status)) {
                    notOK = true;
                    continue;
                }
                noneOK = false;
                final String link = (String) json.get("link");
                if (link != null) {
                    final String url = new Regex(link, "href=(\"|'|)(.*?)\\1").getMatch(1);
                    if (url != null) {
                        links.add(url);
                    }
                }
            }
            if (noneOK == false) {
                notOK = false;
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
                @Override
                public String getSiteKey() {
                    return getSiteKey(captcha.getHtmlCode());
                };
            }.getToken();
            captcha.put("g-recaptcha-response", Encoding.urlEncode(recaptchaV2Response));
            submitForm(br, captcha);
        }
    }

    private String decryptLink(Browser brc, final String parameter) throws Exception {
        String dllink = brc.getRedirectLocation();
        if (dllink == null) {
            dllink = brc.getRegex(DEFAULTREGEX).getMatch(0);
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

    public boolean hasCaptcha(CryptedLink link, jd.plugins.Account acc) {
        return false;
    }

    @Override
    public SiteTemplate siteTemplateType() {
        return SiteTemplate.Qooy_Mirrors;
    }
}