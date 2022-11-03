//jDownloader - Downloadmanager
//Copyright (C) 2013  JD-Team support@jdownloader.org
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
import java.util.HashSet;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

import jd.PluginWrapper;
import jd.controlling.AccountController;
import jd.controlling.ProgressController;
import jd.http.Request;
import jd.nutils.encoding.Encoding;
import jd.parser.html.Form;
import jd.parser.html.HTMLParser;
import jd.plugins.Account;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForDecrypt;
import jd.plugins.PluginForHost;

import org.appwork.utils.Time;
import org.jdownloader.captcha.v2.challenge.hcaptcha.AbstractHCaptcha;
import org.jdownloader.captcha.v2.challenge.hcaptcha.CaptchaHelperCrawlerPluginHCaptcha;
import org.jdownloader.captcha.v2.challenge.recaptcha.v2.AbstractRecaptchaV2;
import org.jdownloader.captcha.v2.challenge.recaptcha.v2.CaptchaHelperCrawlerPluginRecaptchaV2;

/**
 * @author typek_pb
 */
@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = {}, urls = {})
public class AvxHmeW extends PluginForDecrypt {
    @SuppressWarnings("deprecation")
    public AvxHmeW(PluginWrapper wrapper) {
        super(wrapper);
    }

    public static List<String[]> getPluginDomains() {
        final List<String[]> ret = new ArrayList<String[]>();
        /* Always add current domain to first position! */
        ret.add(new String[] { "avh.world", "avaxhome.ws", "avaxhome.bz", "avaxhome.cc", "avaxhome.in", "avaxhome.pro", "avaxho.me", "avaxhm.com", "avxhm.is", "avxhm.se", "avxhome.se", "avxhome.in", "avxde.org" });
        return ret;
    }

    public static String[] getAnnotationNames() {
        return buildAnnotationNames(getPluginDomains());
    }

    @Override
    public String[] siteSupportedNames() {
        return buildSupportedNames(getPluginDomains());
    }

    public static String[] getAnnotationUrls() {
        return buildAnnotationUrls(getPluginDomains());
    }

    public static String[] buildAnnotationUrls(final List<String[]> pluginDomains) {
        final List<String> ret = new ArrayList<String>();
        for (final String[] domains : pluginDomains) {
            ret.add("https?://(?:www\\.)?" + buildHostsPatternPart(domains) + "/(ebooks|music|software|video|magazines|newspapers|games|graphics|misc|hraphile|comics|go)/.+");
        }
        return ret.toArray(new String[0]);
    }

    private static final String TYPE_REDIRECT = "https?://[^/]+/go/([a-f0-9]{32}/\\d+/?|[A-Za-z0-9\\-_:%]+/?|\\d+/[^\"]+)";
    private static AtomicLong   LAST_DIRECT   = new AtomicLong(-1);

    @Override
    public int getMaxConcurrentProcessingInstances() {
        return 1;
    }

    @SuppressWarnings("deprecation")
    @Override
    public ArrayList<DownloadLink> decryptIt(final CryptedLink param, ProgressController progress) throws Exception {
        final ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        br.setAllowedResponseCodes(new int[] { 401 });
        /* 2021-03-11: Do not replace hosts inside URLs anymore as this can lead to wrong redirectURLs breaking the crawling process. */
        // final String parameter = param.toString().replaceFirst("(?i)" + Regex.escape(Browser.getHost(param.toString())), this.getHost());
        if (param.getCryptedUrl().matches(TYPE_REDIRECT)) {
            /* 2021-01-20: Login whenever possible -> No captchas required then */
            final Account acc = AccountController.getInstance().getValidAccount("avxhm.se");
            if (acc != null) {
                /* Logged in users can often skip the captchas but sometimes they will be asked to solve captchas too! */
                final PluginForHost hostPlugin = this.getNewPluginForHostInstance("avxhm.se");
                ((jd.plugins.hoster.AvxHmeW) hostPlugin).login(acc, false);
            }
            br.setFollowRedirects(false);
            /* 2022-01-18: Some tests */
            // br.getHeaders().put("sec-ch-ua", "\" Not;A Brand\";v=\"99\", \"Google Chrome\";v=\"97\", \"Chromium\";v=\"97\"");
            // br.getHeaders().put("sec-ch-ua-mobile", "?0");
            // br.getHeaders().put("sec-ch-ua-platform", "\"Windows\"");
            // br.getHeaders().put("Upgrade-Insecure-Requests", "1");
            // br.getHeaders().put("Accept",
            // "text/html,application/xhtml+xml,application/xml;q=0.9,image/avif,image/webp,image/apng,*/*;q=0.8,application/signed-exchange;v=b3;q=0.9");
            // br.getHeaders().put("Sec-Fetch-Site", "same-origin");
            // br.getHeaders().put("Sec-Fetch-Mode", "navigate");
            // br.getHeaders().put("Sec-Fetch-User", "?1");
            // br.getHeaders().put("Sec-Fetch-Dest", "document");
            // br.getHeaders().put("Accept-Language", "de-DE,de;q=0.9,en;q=0.8,en-US;q=0.7");
            synchronized (LAST_DIRECT) {
                final long last = LAST_DIRECT.get();
                final long wait = 10000 - (Time.systemIndependentCurrentJVMTimeMillis() - last);
                if (wait > 0) {
                    sleep(wait, param);
                }
                br.getPage(param.getCryptedUrl());
                followInternalRedirects();
                LAST_DIRECT.set(Time.systemIndependentCurrentJVMTimeMillis());
            }
            String link = br.getRedirectLocation();
            if (link == null) {
                boolean captchaError = false;
                int counter = 0;
                do {
                    counter++;
                    logger.info("Captcha attempt " + counter);
                    final Form captchaForm = br.getForm(0);
                    if (AbstractRecaptchaV2.containsRecaptchaV2Class(br)) {
                        final String siteURL = br.getURL("/").toString();
                        final String recaptchaV2Response = new CaptchaHelperCrawlerPluginRecaptchaV2(this, br) {
                            protected String getSiteUrl() {
                                // special handling
                                // being logged in can result in auto redirect/no captcha
                                return siteURL;
                            };
                        }.getToken();
                        captchaForm.put("g-recaptcha-response", Encoding.urlEncode(recaptchaV2Response));
                    } else if (AbstractHCaptcha.containsHCaptcha(br)) {
                        final String siteURL = br.getURL("/").toString();
                        final String recaptchaV2Response = new CaptchaHelperCrawlerPluginHCaptcha(this, br) {
                            protected String getSiteUrl() {
                                // special handling
                                // being logged in can result in auto redirect/no captcha
                                return siteURL;
                            };
                        }.getToken();
                        captchaForm.put("g-recaptcha-response", Encoding.urlEncode(recaptchaV2Response));
                    }
                    br.submitForm(captchaForm);
                    LAST_DIRECT.set(Time.systemIndependentCurrentJVMTimeMillis());
                    followInternalRedirects();
                    /*
                     * 2021-03-11: Sometimes they may first ask for an invisible reCaptchaV2 and then for a normal reCaptchaV2 afterwards...
                     */
                    captchaError = br.containsHTML(">\\s*Captcha error");
                    link = br.getRedirectLocation();
                } while (link == null && captchaError && counter <= 3);
                if (captchaError) {
                    /* This should never happen */
                    throw new PluginException(LinkStatus.ERROR_CAPTCHA);
                }
            }
            if (link != null && !link.matches(this.getSupportedLinks().pattern())) {
                decryptedLinks.add(createDownloadlink(link));
            } else {
                logger.warning("Failed to find any result:" + link);
            }
        } else {
            br.setFollowRedirects(true);
            br.getPage(param.getCryptedUrl());
            if (br.getHttpConnection().getResponseCode() == 404) {
                decryptedLinks.add(this.createOfflinelink(param.getCryptedUrl()));
                return decryptedLinks;
            }
            final String notThis = "(?:https?:)?" + buildHostsPatternPart(getPluginDomains().get(0)) + "[\\S&]+";
            final HashSet<String> dupe = new HashSet<String>();
            // 1.st try: <a href="LINK" target="_blank" rel="nofollow"> but ignore
            // images/self site refs + imdb refs
            String[] links = br.getRegex("<a href=\"(" + notThis + ")\"(?:\\s+[^>]*target=\"_blank\" rel=\"nofollow[^>]*|>Download from)").getColumn(0);
            if (links != null && links.length != 0) {
                for (String link : links) {
                    if (!dupe.add(link)) {
                        continue;
                    }
                    if (!this.canHandle(link)) {
                        decryptedLinks.add(createDownloadlink(br.getURL(link).toString()));
                    }
                }
            }
            /* Now find single redirect-URLs */
            final String[] allURLs = HTMLParser.getHttpLinks(this.br.toString(), br.getURL());
            for (final String url : allURLs) {
                if (url.matches(TYPE_REDIRECT)) {
                    decryptedLinks.add(createDownloadlink(url));
                }
            }
            // try also LINK</br>, but ignore self site refs + imdb refs
            links = br.getRegex("(" + notThis + ")<br\\s*/\\s*>").getColumn(0);
            if (links.length > 0) {
                for (String link : links) {
                    // strip html tags
                    link = link.replaceAll("<[^>]+>", "");
                    if (!dupe.add(link)) {
                        continue;
                    }
                    if (!this.canHandle(link)) {
                        decryptedLinks.add(createDownloadlink(link));
                    }
                }
            }
            final String[] covers = br.getRegex("\"((?:https?:)?//(pi?xhst|pixhost)\\.(com|co|icu)[^<>\"]*?)\"").getColumn(0);
            if (covers != null && covers.length != 0) {
                for (String coverlink : covers) {
                    coverlink = Request.getLocation(coverlink, br.getRequest());
                    if (!dupe.add(coverlink)) {
                        continue;
                    }
                    decryptedLinks.add(createDownloadlink(coverlink));
                }
            }
            String fpName = br.getRegex("<title>(.*?)\\s*[\\|/]\\s*AvaxHome.*?</title>").getMatch(0);
            if (fpName == null) {
                fpName = br.getRegex("<h1>(.*?)</h1>").getMatch(0);
            }
            if (fpName != null) {
                FilePackage fp = FilePackage.getInstance();
                fp.setName(Encoding.htmlOnlyDecode(fpName.trim()));
                fp.addLinks(decryptedLinks);
            }
        }
        return decryptedLinks;
    }

    private void followInternalRedirects() throws IOException {
        while (true) {
            final String link = br.getRedirectLocation();
            if (link != null && link.matches(this.getSupportedLinks().pattern()) && link.matches("^https?://.+")) {
                br.followRedirect();
            } else {
                break;
            }
        }
    }

    /* NO OVERRIDE!! */
    public boolean hasCaptcha(CryptedLink link, jd.plugins.Account acc) {
        return false;
    }
}