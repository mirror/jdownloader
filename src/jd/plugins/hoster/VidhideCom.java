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
package jd.plugins.hoster;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.appwork.utils.StringUtils;
import org.jdownloader.plugins.components.XFileSharingProBasic;

import jd.PluginWrapper;
import jd.http.Browser;
import jd.parser.Regex;
import jd.parser.html.Form;
import jd.plugins.Account;
import jd.plugins.Account.AccountType;
import jd.plugins.DownloadLink;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;

@HostPlugin(revision = "$Revision$", interfaceVersion = 3, names = {}, urls = {})
public class VidhideCom extends XFileSharingProBasic {
    public VidhideCom(final PluginWrapper wrapper) {
        super(wrapper);
        this.enablePremium(super.getPurchasePremiumURL());
    }

    /**
     * DEV NOTES XfileSharingProBasic Version SEE SUPER-CLASS<br />
     * mods: See overridden functions<br />
     * limit-info:<br />
     * captchatype-info: null 4dignum reCaptchaV2, hcaptcha<br />
     * other:<br />
     */
    public static List<String[]> getPluginDomains() {
        final List<String[]> ret = new ArrayList<String[]>();
        // each entry in List<String[]> will result in one PluginForHost, Plugin.getHost() will return String[0]->main domain
        ret.add(new String[] { "vidhide.com", "vidhidepro.com", "moflix-stream.click", "javplaya.com", "filelions.to", "filelions.com", "filelions.online", "filelions.site", "alions.pro", "azipcdn.com", "vidhidepre.com", "dlions.pro", "playrecord.biz", "mycloudz.cc" });
        return ret;
    }

    @Override
    protected List<String> getDeadDomains() {
        final ArrayList<String> deadDomains = new ArrayList<String>();
        deadDomains.add("azipcdn.com");
        deadDomains.add("filelions.com"); // 2024-08-02
        deadDomains.add("filelions.site"); // 2024-08-02
        deadDomains.add("alions.pro"); // 2024-08-02
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
        return VidhideCom.buildAnnotationUrls(getPluginDomains());
    }

    public static final String getDefaultAnnotationPatternPartVidhideCom() {
        return "/(?:d/[A-Za-z0-9]+|(?:embed-|e/|f/|v/)?[a-z0-9]{12}(?:/[^/]+(?:\\.html)?)?)";
    }

    public static String[] buildAnnotationUrls(final List<String[]> pluginDomains) {
        final List<String> ret = new ArrayList<String>();
        for (final String[] domains : pluginDomains) {
            ret.add("https?://(?:www\\.)?" + buildHostsPatternPart(domains) + "(?::\\d+)?" + VidhideCom.getDefaultAnnotationPatternPartVidhideCom());
        }
        return ret.toArray(new String[0]);
    }

    @Override
    public boolean isResumeable(final DownloadLink link, final Account account) {
        final AccountType type = account != null ? account.getType() : null;
        if (AccountType.FREE.equals(type)) {
            /* Free Account */
            return true;
        } else if (AccountType.PREMIUM.equals(type) || AccountType.LIFETIME.equals(type)) {
            /* Premium account */
            return true;
        } else {
            /* Free(anonymous) and unknown account type */
            return true;
        }
    }

    @Override
    public int getMaxChunks(final Account account) {
        final AccountType type = account != null ? account.getType() : null;
        if (AccountType.FREE.equals(type)) {
            /* Free Account */
            return 0;
        } else if (AccountType.PREMIUM.equals(type) || AccountType.LIFETIME.equals(type)) {
            /* Premium account */
            return 0;
        } else {
            /* Free(anonymous) and unknown account type */
            return 0;
        }
    }

    @Override
    public int getMaxSimultaneousFreeAnonymousDownloads() {
        return -1;
    }

    @Override
    public int getMaxSimultaneousFreeAccountDownloads() {
        return -1;
    }

    @Override
    public int getMaxSimultanPremiumDownloadNum() {
        return -1;
    }

    @Override
    protected boolean supportsShortURLs() {
        return false;
    }

    private final String PATTERN_SPECIAL   = "(?i)^https?://[^/]+/f/([a-z0-9]{12}).*";
    private final String PATTERN_SPECIAL_2 = "(?i)^https?://[^/]+/v/([a-z0-9]{12}).*";

    @Override
    protected URL_TYPE getURLType(final String url) {
        if (url == null) {
            return null;
        }
        if (url.matches(PATTERN_SPECIAL)) {
            return URL_TYPE.OFFICIAL_VIDEO_DOWNLOAD;
        } else if (url.matches(PATTERN_SPECIAL_2)) {
            return URL_TYPE.OFFICIAL_VIDEO_DOWNLOAD;
        } else {
            return super.getURLType(url);
        }
    }

    @Override
    public String getFUIDFromURL(final DownloadLink link) {
        if (link != null && link.getPluginPatternMatcher() != null) {
            final Regex special1 = new Regex(link.getPluginPatternMatcher(), PATTERN_SPECIAL);
            final Regex special2;
            if (special1.patternFind()) {
                return special1.getMatch(0);
            } else if ((special2 = new Regex(link.getPluginPatternMatcher(), PATTERN_SPECIAL_2)).patternFind()) {
                return special2.getMatch(0);
            }
        }
        return super.getFUIDFromURL(link);
    }

    @Override
    protected String getDllinkViaOfficialVideoDownloadNew(final Browser br, final DownloadLink link, final Account account, final boolean returnFilesize) throws Exception {
        if (returnFilesize) {
            logger.info("[FilesizeMode] Trying to find official video downloads");
        } else {
            logger.info("[DownloadMode] Trying to find official video downloads");
        }
        final String[][] videoInfo = br.getRegex("href=\"(/download/[a-z0-9]{12}_[a-z]{1})\".*?<small class=\"text-muted\">\\s*\\d+x\\d+ ([^<]+)</small>").getMatches();
        if (videoInfo == null || videoInfo.length == 0) {
            logger.info("Failed to find any official video downloads");
            return null;
        }
        /*
         * Internal quality identifiers highest to lowest (inside 'download_video' String): o = original, h = high, n = normal, l=low
         */
        final HashMap<String, Integer> qualityMap = new HashMap<String, Integer>();
        qualityMap.put("l", 20); // low
        qualityMap.put("n", 40); // normal
        qualityMap.put("h", 60); // high
        qualityMap.put("o", 80); // original
        qualityMap.put("x", 100); // download
        long maxInternalQualityValue = 0;
        String filesizeStrBest = null;
        String filesizeStrSelected = null;
        String videoURLBest = null;
        String videoURLSelected = null;
        final String userSelectedQualityValue = getPreferredDownloadQualityStr();
        if (userSelectedQualityValue == null) {
            logger.info("Trying to find highest quality for official video download");
        } else {
            logger.info(String.format("Trying to find user selected quality %s for official video download", userSelectedQualityValue));
        }
        for (final String videoInfos[] : videoInfo) {
            final String videoURL = videoInfos[0];
            final String filesizeStr = videoInfos[1];
            final String videoQualityStrTmp = new Regex(videoURL, "_([a-z]{1})$").getMatch(0);
            if (StringUtils.isEmpty(videoQualityStrTmp)) {
                /*
                 * Possible plugin failure but let's skip bad items. Upper handling will fallback to stream download if everything fails!
                 */
                logger.warning("Found unidentifyable video quality");
                continue;
            } else if (!qualityMap.containsKey(videoQualityStrTmp)) {
                /*
                 * 2020-01-18: There shouldn't be any unknown values but we should consider allowing such in the future maybe as final
                 * fallback.
                 */
                logger.info("Skipping unknown quality: " + videoQualityStrTmp);
                continue;
            }
            /* Look for best quality */
            final int internalQualityValueTmp = qualityMap.get(videoQualityStrTmp);
            if (internalQualityValueTmp > maxInternalQualityValue || videoURLBest == null) {
                maxInternalQualityValue = internalQualityValueTmp;
                videoURLBest = videoURL;
                filesizeStrBest = filesizeStr;
            }
            if (userSelectedQualityValue != null && videoQualityStrTmp.equalsIgnoreCase(userSelectedQualityValue)) {
                logger.info("Found user selected quality: " + userSelectedQualityValue);
                videoURLSelected = videoURL;
                if (filesizeStr != null) {
                    /*
                     * Usually, filesize for official video downloads will be given but not in all cases. It may also happen that our upper
                     * RegEx fails e.g. for supervideo.tv.
                     */
                    filesizeStrSelected = filesizeStr;
                }
                break;
            }
        }
        if (videoURLBest == null && videoURLSelected == null) {
            logger.warning("Video selection handling failed");
            return null;
        }
        final String filesizeStrChosen;
        final String continueURL;
        if (filesizeStrSelected == null) {
            if (userSelectedQualityValue == null) {
                logger.info("Returning BEST quality according to user preference");
            } else {
                logger.info("Returning BEST quality as fallback");
            }
            filesizeStrChosen = filesizeStrBest;
            continueURL = videoURLBest;
        } else {
            logger.info("Returning user selected quality: " + userSelectedQualityValue);
            filesizeStrChosen = filesizeStrSelected;
            continueURL = videoURLSelected;
        }
        if (returnFilesize) {
            /* E.g. in availablecheck */
            return filesizeStrChosen;
        }
        getPage(br, continueURL);
        final Form download1 = br.getFormByInputFieldKeyValue("op", "download_orig");
        if (download1 != null) {
            this.handleCaptcha(link, br, download1);
            this.submitForm(br, download1);
            this.checkErrors(br, br.getRequest().getHtmlCode(), link, account, false);
        }
        String dllink = this.getDllink(link, account, br, br.getRequest().getHtmlCode());
        if (StringUtils.isEmpty(dllink)) {
            /*
             * 2019-05-30: Test - worked for: xvideosharing.com - not exactly required as getDllink will usually already return a result.
             */
            dllink = br.getRegex("<a href\\s*=\\s*\"(https?[^\"]+)\"[^>]*>\\s*Direct Download Link\\s*</a>").getMatch(0);
        }
        if (StringUtils.isEmpty(dllink)) {
            logger.warning("Failed to find dllink via official video download");
            String specialErrorDownloadImpossible = br.getRegex("<b class=\"err\"[^>]*>([^<]+)</b>").getMatch(0);
            if (specialErrorDownloadImpossible != null) {
                /* 2024-04-02: e.g. "Downloads disabled 6210" */
                final String msgRemove = new Regex(specialErrorDownloadImpossible, ".+ (\\(\\{.+)").getMatch(0);
                if (msgRemove != null) {
                    specialErrorDownloadImpossible = specialErrorDownloadImpossible.replace(msgRemove, "");
                }
                throw new PluginException(LinkStatus.ERROR_FATAL, specialErrorDownloadImpossible);
            } else {
                return null;
            }
        }
        logger.info("Successfully found dllink via official video download");
        return dllink;
    }
}