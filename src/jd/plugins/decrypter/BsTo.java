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
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map.Entry;
import java.util.regex.Pattern;

import org.appwork.utils.StringUtils;
import org.jdownloader.captcha.v2.challenge.recaptcha.v2.CaptchaHelperCrawlerPluginRecaptchaV2;
import org.jdownloader.plugins.components.config.BsToConfig;
import org.jdownloader.plugins.config.PluginConfigInterface;
import org.jdownloader.plugins.config.PluginJsonConfig;

import jd.PluginWrapper;
import jd.controlling.ProgressController;
import jd.http.Browser;
import jd.http.Request;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.parser.html.Form;
import jd.parser.html.Form.MethodType;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForDecrypt;
import jd.plugins.components.PluginJSonUtils;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = {}, urls = {})
public class BsTo extends PluginForDecrypt {
    public BsTo(PluginWrapper wrapper) {
        super(wrapper);
        Browser.setRequestIntervalLimitGlobal("bs.to", 200);
    }

    public static List<String[]> getPluginDomains() {
        final List<String[]> ret = new ArrayList<String[]>();
        // each entry in List<String[]> will result in one PluginForDecrypt, Plugin.getHost() will return String[0]->main domain
        /* Full list of their current domains see: https://burningseries.domains/ */
        ret.add(new String[] { "bs.to", "burningseries.co", "burningseries.ac", "burningseries.sx", "burningseries.vc", "burningseries.cx", "burningseries.nz", "burningseries.se", "burning-series.io", "burningseries.tw" });
        return ret;
    }

    private static final List<String> getDeadDomains() {
        final ArrayList<String> deadDomains = new ArrayList<String>();
        /* 2023-10-10: Strange advertisement copy-website(?) */
        deadDomains.add("burning-series.io");
        /* 2023-10-10: Down */
        deadDomains.add("burningseries.cx");
        deadDomains.add("burningseries.nz");
        deadDomains.add("burningseries.se");
        deadDomains.add("burningseries.tw");
        deadDomains.add("");
        return deadDomains;
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
            ret.add("https?://(?:www\\.)?" + buildHostsPatternPart(domains) + "/(serie/.*|out/\\d+)");
        }
        return ret.toArray(new String[0]);
    }

    private static final String TYPE_SINGLE = "https?://[^/]+/serie/([^/]+)/(\\d+)/([^/]+)/[^/]+/[^/]+";

    public ArrayList<DownloadLink> decryptIt(final CryptedLink param, ProgressController progress) throws Exception {
        final ArrayList<DownloadLink> ret = new ArrayList<DownloadLink>();
        String contenturl = param.getCryptedUrl();
        final String addedLinkDomain = Browser.getHost(contenturl, true);
        String domainToUse = addedLinkDomain;
        if (getDeadDomains().contains(addedLinkDomain)) {
            domainToUse = this.getHost();
            contenturl = contenturl.replaceFirst(Pattern.quote(addedLinkDomain), domainToUse);
        }
        if (contenturl.matches("(?i)https?://[^/]+/out.*")) {
            /* 2022-02-01: Old single link(?) */
            br.setFollowRedirects(false);
            br.getPage(contenturl);
            if (br.getRedirectLocation() == null || br.containsHTML("g-recaptcha")) {
                Form form = br.getFormbyProperty("id", "gateway");
                if (form == null) {
                    form = new Form();
                    form.setMethod(MethodType.GET);
                    form.setAction(br.getURL());
                }
                final String recaptchaV2Response = new CaptchaHelperCrawlerPluginRecaptchaV2(this, br).getToken();
                form.put("t", recaptchaV2Response);
                br.submitForm(form);
            }
            final String finallink = br.getRedirectLocation();
            ret.add(createDownloadlink(finallink));
            return ret;
        }
        br.setFollowRedirects(true);
        br.getPage(contenturl);
        if (br.getHttpConnection().getResponseCode() == 404) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        } else if (br.containsHTML("(?i)>\\s*Seite nicht gefunden<")) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        // final String urlpart = new Regex(parameter, "(serie/.+)").getMatch(0);
        if (contenturl.matches(TYPE_SINGLE)) {
            String finallink = br.getRegex("\"(https?[^<>\"]*?)\" target=\"_blank\"><span class=\"icon link_go\"").getMatch(0);
            if (finallink == null) {
                finallink = br.getRegex("<iframe\\s+[^>]+src\\s*=\\s*(\"|'|)(.*?)\\1").getMatch(1);
                // hoster player
                if (finallink == null) {
                    finallink = br.getRegex("\"(https?[^<>\"]*?)\" target=\"_blank\" class=\"hoster-player\">").getMatch(0);
                    if (finallink == null) {
                        // final failover?
                        finallink = br.getRegex("https?://(\\w+\\.)?[^/]+/out/\\d+").getMatch(-1);
                    }
                }
            }
            if (finallink == null) {
                /* 2019-07-26: New */
                final String security_token = br.getRegex("<meta name=\"security_token\" content=\"([a-f0-9]+)\" />").getMatch(0);
                final String lid = br.getRegex("data\\-lid=\"(\\d+)\"").getMatch(0);
                if (security_token == null || lid == null) {
                    throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                }
                String rcKey = br.getRegex("<script>series\\.init\\s*\\(\\d+, \\d+, '([^<>\"\\']+)'\\);</script>").getMatch(0);
                if (rcKey == null) {
                    /* 2021-01-18: Hardcoded reCaptchaV2Key */
                    rcKey = "6LfG_SYaAAAAABmtgbmBRni8SvFepX0EEun1f5-5";
                }
                final String recaptchaV2Response = new CaptchaHelperCrawlerPluginRecaptchaV2(this, br, rcKey).getToken();
                br.postPage("/ajax/embed.php", "token=" + security_token + "&LID=" + lid + "&ticket=" + Encoding.urlEncode(recaptchaV2Response));
                finallink = PluginJSonUtils.getJson(br, "link");
                if (StringUtils.isEmpty(finallink) || !finallink.startsWith("http")) {
                    logger.warning("Failed to find finallink");
                    throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                }
                /* 2019-07-26: Sadly we cannot re-use these tokens! */
                // this.getPluginConfig().setProperty("recaptchaV2Response", recaptchaV2Response);
            } else if (StringUtils.containsIgnoreCase(finallink, "bs.to/out") || StringUtils.containsIgnoreCase(finallink, "burningseries.co/out")) {
                br.setFollowRedirects(false);
                br.getPage(finallink);
                if (br.getRedirectLocation() == null || br.containsHTML("g-recaptcha")) {
                    final Form form = br.getForm(0);
                    if (form == null) {
                        throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                    }
                    final String recaptchaV2Response = new CaptchaHelperCrawlerPluginRecaptchaV2(this, br).getToken();
                    form.put("token", Encoding.urlEncode(recaptchaV2Response));
                    br.submitForm(form);
                }
                finallink = br.getRedirectLocation();
            }
            ret.add(createDownloadlink(finallink));
        } else {
            /* Crawl all mirrors of a single download */
            String mirrorlist = br.getRegex("<ul class=\"hoster-tabs top\">(.*?)<ul class=\"hoster-tabs bottom\">").getMatch(0);
            if (mirrorlist == null || mirrorlist.length() == 0) {
                /* Crawl all episodes of a series --> All mirrors in that */
                mirrorlist = br.getRegex("<table class=\"episodes\">.*?</table>").getMatch(-1);
            }
            final String[] mirrorURLs = new Regex(mirrorlist, "<a[^>]*href=\"(serie/[^/]+/\\d+/[^/]+/[a-z]{2,}/[^/\"]+)\"").getColumn(0);
            if (mirrorURLs == null || mirrorURLs.length == 0) {
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            logger.info("Number of possible downloadlinks TOTAL: " + mirrorURLs.length);
            /* Add only user preferred host or all available hosts. */
            String userHosterPrioListStr = PluginJsonConfig.get(BsToConfig.class).getHosterPriorityString();
            if (userHosterPrioListStr != null) {
                userHosterPrioListStr = userHosterPrioListStr.replace(" ", "").toLowerCase(Locale.ENGLISH);
                final String[] hosterPrioList = userHosterPrioListStr.split(",");
                logger.info("Trying to add only one user priorized host");
                final HashMap<String, List<String>> packages = new HashMap<String, List<String>>();
                for (final String mirrorURL : mirrorURLs) {
                    final String uniqueID = mirrorURL.substring(0, mirrorURL.lastIndexOf("/"));
                    if (packages.containsKey(uniqueID)) {
                        packages.get(uniqueID).add(mirrorURL);
                    } else {
                        final List<String> newList = new ArrayList<String>();
                        newList.add(mirrorURL);
                        packages.put(uniqueID, newList);
                    }
                }
                logger.info("Number of packages found: " + packages.size());
                final List<String> userAllowedMirrorURLs = new ArrayList<String>();
                /* Now decide which mirrors we actually want to crawl based on a user setting. */
                for (final Entry<String, List<String>> entry : packages.entrySet()) {
                    final List<String> packageMirrorIDs = entry.getValue();
                    if (packageMirrorIDs.size() == 1) {
                        /* Only 1 host available -> Crawl that */
                        userAllowedMirrorURLs.add(packageMirrorIDs.get(0));
                    } else if (hosterPrioList != null) {
                        /* Try to prefer user selected hosts/mirrors */
                        boolean hasFoundPreferredHost = false;
                        mirrorLoop: for (final String userPreferredHost : hosterPrioList) {
                            for (final String mirrorURL : packageMirrorIDs) {
                                final String hoster = new Regex(mirrorURL, "/([^/]+)$").getMatch(0);
                                if (hoster.equalsIgnoreCase(userPreferredHost)) {
                                    userAllowedMirrorURLs.add(mirrorURL);
                                    hasFoundPreferredHost = true;
                                    break mirrorLoop;
                                }
                            }
                        }
                        if (!hasFoundPreferredHost) {
                            /* Fallback */
                            userAllowedMirrorURLs.addAll(packageMirrorIDs);
                        }
                    } else {
                        /* Crawl all mirrors */
                        userAllowedMirrorURLs.addAll(packageMirrorIDs);
                    }
                }
                logger.info("Number of user allowed mirrors via priorized hosts handling: " + userAllowedMirrorURLs.size());
                for (final String singleLink : userAllowedMirrorURLs) {
                    final String url = Request.getLocation("/" + singleLink, br.getRequest());
                    ret.add(createDownloadlink(url));
                }
            } else {
                logger.info("User didn't define priorized hosts -> Crawling all");
                for (final String singleLink : mirrorURLs) {
                    final String url = Request.getLocation("/" + singleLink, br.getRequest());
                    ret.add(createDownloadlink(url));
                }
            }
        }
        return ret;
    }

    @Override
    public int getMaxConcurrentProcessingInstances() {
        return 5;
    }

    /* NO OVERRIDE!! */
    public boolean hasCaptcha(CryptedLink link, jd.plugins.Account acc) {
        return true;
    }

    @Override
    public Class<? extends PluginConfigInterface> getConfigInterface() {
        return BsToConfig.class;
    }
}
