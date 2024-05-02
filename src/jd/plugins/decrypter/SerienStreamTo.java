//jDownloader - Downloadmanager
//Copyright (C) 2012  JD-Team support@jdownloader.org
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
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;

import org.appwork.utils.Regex;
import org.appwork.utils.StringUtils;
import org.jdownloader.captcha.v2.challenge.recaptcha.v2.AbstractRecaptchaV2;
import org.jdownloader.captcha.v2.challenge.recaptcha.v2.CaptchaHelperCrawlerPluginRecaptchaV2;
import org.jdownloader.plugins.components.config.SerienStreamToConfig;
import org.jdownloader.plugins.config.PluginConfigInterface;
import org.jdownloader.plugins.config.PluginJsonConfig;

import jd.PluginWrapper;
import jd.controlling.ProgressController;
import jd.http.Browser;
import jd.nutils.encoding.Encoding;
import jd.parser.html.Form;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterException;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForDecrypt;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 3, names = {}, urls = {})
public class SerienStreamTo extends PluginForDecrypt {
    @SuppressWarnings("deprecation")
    public SerienStreamTo(final PluginWrapper wrapper) {
        super(wrapper);
    }

    public static List<String[]> getPluginDomains() {
        final List<String[]> ret = new ArrayList<String[]>();
        // each entry in List<String[]> will result in one PluginForDecrypt, Plugin.getHost() will return String[0]->main domain
        ret.add(new String[] { "s.to", "serienstream.sx", "serienstream.to", "serienstream.ch", "serien.sx", "serien.domains", "serien.cam", "190.115.18.20" });
        ret.add(new String[] { "aniworld.to" });
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
        final List<String> ret = new ArrayList<String>();
        for (final String[] domains : getPluginDomains()) {
            ret.add("https?://(?:www\\.)?" + buildHostsPatternPart(domains) + "/(anime|serie|redirect)/.*");
        }
        return ret.toArray(new String[0]);
    }

    private final String TYPE_SINGLE_REDIRECT = "(?i)https?://[^/]+/redirect/(\\d+).*";

    @SuppressWarnings("deprecation")
    public ArrayList<DownloadLink> decryptIt(final CryptedLink param, final ProgressController progress) throws Exception {
        final String contenturl = param.getCryptedUrl().replaceAll("[/]+$", "");
        if (param.getCryptedUrl().matches(TYPE_SINGLE_REDIRECT)) {
            final ArrayList<DownloadLink> ret = new ArrayList<DownloadLink>();
            ret.add(this.crawlSingleRedirect(contenturl, br));
            return ret;
        } else {
            return this.crawlMirrors(contenturl);
        }
    }

    private DownloadLink crawlSingleRedirect(String url, final Browser br) throws PluginException, InterruptedException, DecrypterException, IOException {
        br.setFollowRedirects(false);
        /* Enforce https */
        url = url.replaceFirst("(?i)http://", "https://");
        final String initialHost = Browser.getHost(url, true);
        String redirectPage = br.getPage(url);
        String finallink = null;
        if (br.getRedirectLocation() != null) {
            finallink = br.getRedirectLocation();
        } else if (AbstractRecaptchaV2.containsRecaptchaV2Class(br) || br.containsHTML("grecaptcha")) {
            br.setFollowRedirects(true);
            final Form captcha = br.getForm(0);
            final String sitekey = new Regex(redirectPage, "grecaptcha\\.execute\\('([^']+)'").getMatch(0);
            final String recaptchaV2Response = new CaptchaHelperCrawlerPluginRecaptchaV2(this, br, sitekey) {
                @Override
                public TYPE getType() {
                    return TYPE.INVISIBLE;
                }
            }.getToken();
            captcha.put("original", "");
            captcha.put("token", Encoding.urlEncode(recaptchaV2Response));
            try {
                redirectPage = br.submitForm(captcha);
            } catch (IOException e) {
                logger.log(e);
            }
            finallink = br.getURL().toString();
        }
        if (br.getHttpConnection().getResponseCode() == 404) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        } else if (finallink == null || this.canHandle(finallink)) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        } else if (Browser.getHost(finallink, true).equalsIgnoreCase(initialHost)) {
            /* E.g. redirect to mainpage */
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        return createDownloadlink(finallink);
    }

    private ArrayList<DownloadLink> crawlMirrors(final String contenturl) throws PluginException, InterruptedException, DecrypterException, IOException {
        br.setFollowRedirects(true);
        br.getPage(contenturl);
        if (br.getHttpConnection().getResponseCode() == 404) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        final Set<String> dupes = new HashSet<String>();
        final ArrayList<DownloadLink> ret = new ArrayList<DownloadLink>();
        final String title = br.getRegex("<meta property=\"og:title\" content=\"(?:Episode\\s*\\d+\\s|Staffel\\s*\\d+\\s|Filme?\\s*\\d*\\s|von\\s)+([^\"]+)\"/>").getMatch(0);
        final String itemSlug = new Regex(br.getURL(), "https?://[^/]+/[^/]+/[^/]+/(.*)").getMatch(0);
        // If we're on a show site, add the seasons, if we're on a season page, add the episodes and so on ...
        final String[][] itemLinks = br.getRegex("href=\"([^\"]+" + Pattern.quote(itemSlug) + "/[^\"]+)\"").getMatches();
        for (String[] itemLink : itemLinks) {
            final String url = br.getURL(Encoding.htmlDecode(itemLink[0])).toExternalForm();
            if (dupes.add(url)) {
                ret.add(createDownloadlink(url));
            }
        }
        /* Videos are on external sites (not in embeds), so harvest those if we can get our hands on them. */
        final String[] episodeHTMLs = br.getRegex("<li class=\"[^\"]*episodeLink\\d+\"(.*?)</a>").getColumn(0);
        if (episodeHTMLs.length > 0) {
            final Set<String> userLanguageIDsPrioList = new LinkedHashSet<String>();
            final Set<String> userHosterPrioList = new LinkedHashSet<String>();
            /* Collect language name -> ID mapping if needed */
            final String userLanguagePrioListStr = PluginJsonConfig.get(SerienStreamToConfig.class).getLanguagePriorityString();
            if (userLanguagePrioListStr != null) {
                /* Find internal ID of user preferred languages e.g. */
                final List<String> userAllowedLanguageTitles = new ArrayList<String>();
                userAllowedLanguageTitles.addAll(Arrays.asList(userLanguagePrioListStr.split(",")));
                final String languageFlagsHTML = br.getRegex("<div class=\"changeLanguage\">(.*?)</div>").getMatch(0);
                if (languageFlagsHTML != null) {
                    final String[][] languageTitleIDMappings = new Regex(languageFlagsHTML, "<img[^>]*data-lang-key=\"(\\d+)\" title=\"([^\"]+)\"").getMatches();
                    if (languageTitleIDMappings.length > 0) {
                        for (final String userPreferredLanguageTitle : userAllowedLanguageTitles) {
                            final String userPreferredLanguageTitleMatcher[] = userPreferredLanguageTitle.split("\\s+");
                            for (String[] languageTitleIDMapping : languageTitleIDMappings) {
                                boolean match = true;
                                for (String userPreferredLanguageTitleMatch : userPreferredLanguageTitleMatcher) {
                                    if (!StringUtils.containsIgnoreCase(languageTitleIDMapping[1], userPreferredLanguageTitleMatch)) {
                                        match = false;
                                        break;
                                    }
                                }
                                if (match) {
                                    userLanguageIDsPrioList.add(languageTitleIDMapping[0]);
                                }
                            }
                        }
                        logger.info("Found " + userLanguageIDsPrioList.size() + "/" + userAllowedLanguageTitles.size() + " user preferred languages");
                    } else {
                        logger.warning("Failed to find any languageTitleIDMappings");
                    }
                } else {
                    logger.warning("Failed to find languagesFlagsHTML");
                }
            }
            String userHosterPrioListStr = PluginJsonConfig.get(SerienStreamToConfig.class).getHosterPriorityString();
            if (userHosterPrioListStr != null) {
                userHosterPrioListStr = userHosterPrioListStr.replace(" ", "").toLowerCase(Locale.ENGLISH);
                userHosterPrioList.addAll(Arrays.asList(userHosterPrioListStr.split(",")));
            }
            /* Now check which URLs/mirrors our user prefers --> If configured properly, user will need to enter less captchas this way */
            final ArrayList<String> allRedirectURLs = new ArrayList<String>();
            /* Group results by host */
            final HashMap<String, List<String>> packagesByHoster = new HashMap<String, List<String>>();
            final HashMap<String, List<String>> packagesByLanguageKey = new HashMap<String, List<String>>();
            for (final String episodeHTML : episodeHTMLs) {
                final String redirectURL = new Regex(episodeHTML, "href=\"([^\"]+redirect[^\"]+)\" target=\"_blank\"").getMatch(0);
                final String languageKey = new Regex(episodeHTML, "data-lang-key=\"(\\d+)\"").getMatch(0);
                final String hoster = new Regex(episodeHTML, "(?i)title=\"Hoster ([^\"]+)\"").getMatch(0).toLowerCase(Locale.ENGLISH);
                if (redirectURL == null || languageKey == null || hoster == null) {
                    logger.warning("Something is null: redirectURL =" + redirectURL + " | languageKey = " + languageKey + " | hoster = " + hoster);
                    throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                }
                // boolean allowByPrio;
                // if (!userLanguageIDsPrioList.isEmpty() && userLanguageIDsPrioList.contains(languageKey) && !userHosterPrioList.isEmpty()
                // && userHosterPrioList.contains(hoster)) {
                // /* Hoster + language */
                // allowByPrio = true;
                // } else if (!userHosterPrioList.isEmpty() && userLanguageIDsPrioList.isEmpty() && userHosterPrioList.contains(hoster)) {
                // /* Allow only by host */
                // allowByPrio = true;
                // } else if (!userLanguageIDsPrioList.isEmpty() && userHosterPrioList.isEmpty() &&
                // userLanguageIDsPrioList.contains(languageKey)) {
                // /* Allow only by languageKey */
                // allowByPrio = true;
                // } else {
                // /* Not preferred by user not in prio at all) */
                // allowByPrio = false;
                // }
                allRedirectURLs.add(redirectURL);
                /* Update language-packages */
                if (packagesByLanguageKey.containsKey(languageKey)) {
                    packagesByLanguageKey.get(languageKey).add(redirectURL);
                } else {
                    final List<String> newList = new ArrayList<String>();
                    newList.add(redirectURL);
                    packagesByLanguageKey.put(languageKey, newList);
                }
                /* Update hoster packages */
                if (packagesByHoster.containsKey(hoster)) {
                    packagesByHoster.get(hoster).add(redirectURL);
                } else {
                    final List<String> newList = new ArrayList<String>();
                    newList.add(redirectURL);
                    packagesByHoster.put(hoster, newList);
                }
                allRedirectURLs.add(redirectURL);
            }
            List<String> urlsToProcess = null;
            if (!userHosterPrioList.isEmpty()) {
                /* Get user preferred mirrors by host (+ language) */
                for (final String userAllowedHoster : userHosterPrioList) {
                    if (urlsToProcess != null && urlsToProcess.size() > 0) {
                        break;
                    }
                    if (packagesByHoster.containsKey(userAllowedHoster)) {
                        final List<String> preferredMirrorsByHost = packagesByHoster.get(userAllowedHoster);
                        if (preferredMirrorsByHost != null && preferredMirrorsByHost.size() > 0) {
                            logger.info("Found user priorized mirrors by host:" + userAllowedHoster);
                            /* Combine this with users' language priority if given. */
                            for (final String languageKey : userLanguageIDsPrioList) {
                                if (packagesByLanguageKey.containsKey(languageKey)) {
                                    final List<String> preferredMirrorsByLanguage = packagesByLanguageKey.get(languageKey);
                                    if (preferredMirrorsByLanguage != null) {
                                        logger.info("Combining users preferred mirrors by host + language:" + userAllowedHoster + "|" + languageKey);
                                        urlsToProcess = new ArrayList<String>();
                                        for (final String preferredMirrorByHost : preferredMirrorsByHost) {
                                            if (preferredMirrorsByLanguage.contains(preferredMirrorByHost)) {
                                                urlsToProcess.add(preferredMirrorByHost);
                                            }
                                        }
                                        if (urlsToProcess.size() > 0) {
                                            break;
                                        }
                                    } else {
                                        urlsToProcess = preferredMirrorsByHost;
                                    }
                                }
                            }
                        }
                    }
                }
                if (urlsToProcess == null) {
                    logger.info("Failed to find user priorized mirrors by host");
                }
            } else if (!userLanguageIDsPrioList.isEmpty()) {
                /* Get user preferred mirrors by language only */
                for (final String languageKey : userLanguageIDsPrioList) {
                    if (packagesByLanguageKey.containsKey(languageKey)) {
                        urlsToProcess = packagesByLanguageKey.get(languageKey);
                    }
                }
                if (urlsToProcess != null) {
                    logger.info("Found user priorized mirrors by language");
                } else {
                    logger.info("Failed to find user priorized mirrors by language");
                }
            }
            if (urlsToProcess != null) {
                logger.info("Crawling " + urlsToProcess.size() + "/" + allRedirectURLs.size() + " URLs");
            } else {
                logger.info("Crawling ALL URLs: " + allRedirectURLs.size());
                urlsToProcess = allRedirectURLs;
            }
            final FilePackage filePackage;
            if (title != null) {
                filePackage = FilePackage.getInstance();
                filePackage.setName(Encoding.htmlDecode(title).trim());
                filePackage.setAllowMerge(true);
            } else {
                filePackage = null;
            }
            int index = 0;
            final HashSet<String> dup = new HashSet<String>();
            for (String videoURL : urlsToProcess) {
                if (!dup.add(videoURL)) {
                    continue;
                }
                logger.info("Working on item " + index + "/" + urlsToProcess.size());
                final Browser br2 = br.cloneBrowser();
                br2.setFollowRedirects(false);
                videoURL = br.getURL(Encoding.htmlDecode(videoURL)).toExternalForm();
                String redirectPage = br2.getPage(videoURL);
                if (br2.getRedirectLocation() != null) {
                    videoURL = br2.getRedirectLocation();
                } else if (br2.containsHTML("grecaptcha")) {
                    final Form captcha = br2.getForm(0);
                    final String sitekey = new Regex(redirectPage, "grecaptcha\\.execute\\('([^']+)'").getMatch(0);
                    final String recaptchaV2Response = new CaptchaHelperCrawlerPluginRecaptchaV2(this, br2, sitekey) {
                        @Override
                        public TYPE getType() {
                            return TYPE.INVISIBLE;
                        }
                    }.getToken();
                    captcha.put("original", "");
                    captcha.put("token", Encoding.urlEncode(recaptchaV2Response));
                    try {
                        redirectPage = br2.submitForm(captcha);
                    } catch (IOException e) {
                        logger.log(e);
                    }
                    videoURL = br2.getURL().toString();
                }
                final DownloadLink link = createDownloadlink(videoURL);
                if (filePackage != null) {
                    filePackage.add(link);
                }
                ret.add(link);
                distribute(link);
                index += 1;
            }
        }
        return ret;
    }

    @Override
    public Class<? extends PluginConfigInterface> getConfigInterface() {
        return SerienStreamToConfig.class;
    }
}