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
import org.appwork.utils.Time;
import org.appwork.utils.formatter.SizeFormatter;
import org.jdownloader.plugins.components.XFileSharingProBasic;

import jd.PluginWrapper;
import jd.http.Browser;
import jd.parser.Regex;
import jd.parser.html.Form;
import jd.plugins.Account;
import jd.plugins.Account.AccountType;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;

@HostPlugin(revision = "$Revision$", interfaceVersion = 3, names = {}, urls = {})
public class StreamsbNet extends XFileSharingProBasic {
    public StreamsbNet(final PluginWrapper wrapper) {
        super(wrapper);
        this.enablePremium(super.getPurchasePremiumURL());
    }

    @Override
    protected String getContentURL(final DownloadLink link) {
        final Regex special = new Regex(link.getPluginPatternMatcher(), PATTERN_SPECIAL);
        if (special.matches()) {
            /* Do not touch url structure. Only change domain if required. */
            return getMainPage(link) + "/" + special.getMatch(0);
        } else {
            return super.getContentURL(link);
        }
    }

    private static final String PATTERN_SPECIAL = "https://[^/]+/((d|e|c)/([a-z0-9]{12}))";

    /**
     * DEV NOTES XfileSharingProBasic Version SEE SUPER-CLASS<br />
     * mods: See overridden functions<br />
     * limit-info: 2020-08-11: No limits at all <br />
     * captchatype-info: 2020-08-11: null <br />
     * other:<br />
     */
    public static List<String[]> getPluginDomains() {
        final List<String[]> ret = new ArrayList<String[]>();
        // each entry in List<String[]> will result in one PluginForHost, Plugin.getHost() will return String[0]->main domain
        /**
         * 2021-06-17: streamsb.com = only user interface/website. streamsb.net and all other domains: used for video hosting/downloadlinks.
         * </br>
         * streamsb.com is basically only a dummy entry here as no downloadlinks exist that can be used via this domain.
         */
        ret.add(new String[] { "streamsb.com" });
        ret.add(new String[] { "sblanh.com", "streamsb.net", "embedsb.com", "sbembed.com", "sbembed1.com", "sbembed2.com", "sbcloud1.com", "tubesb.com", "sbvideo.net", "playersb.com", "sbplay2.com", "sbplay2.xyz", "sbembed4.com", "javside.com", "watchsb.com", "sbfast.com", "sbfull.com", "javplaya.com", "streamsss.net", "kbjrecord.com", "lvturbo.com" });
        return ret;
    }

    private final String EXTENDED_FILENAME_RESULT   = "extended_filename_result";
    private final String IS_OFFICIALLY_DOWNLOADABLE = "is_officially_downloadable";

    @Override
    protected boolean internal_supports_availablecheck_filename_abuse() {
        return false;
    }

    @Override
    public AvailableStatus requestFileInformationWebsite(final DownloadLink link, final Account account, final boolean isDownload) throws Exception {
        /* Beware! This is full of nasty workarounds! */
        AvailableStatus result = null;
        final String contentURL = this.getContentURL(link);
        result = super.requestFileInformationWebsite(link, account, isDownload);
        String specialFilenameResult = null;
        final Boolean isOfficiallyDownloadable = (Boolean) link.getProperty(IS_OFFICIALLY_DOWNLOADABLE);
        if (contentURL.matches("https?://[^/]+/c/[a-z0-9]{12}.*") && Boolean.FALSE.equals(isOfficiallyDownloadable)) {
            return result;
        } else {
            final String pluginpatternmatcherOld = link.getPluginPatternMatcher();
            try {
                link.setPluginPatternMatcher("https://" + this.getHost() + "/d/" + this.getFUIDFromURL(link));
                result = super.requestFileInformationWebsite(link, account, isDownload);
                specialFilenameResult = br.getRegex("(?i)<h3>\\s*Download ([^<]+)</h3>").getMatch(0); // 2023-01-11
                if (specialFilenameResult == null) {
                    /* 2023-04-19 */
                    specialFilenameResult = br.getRegex("(?i)<h1 class=\"h3 text-center mb-5\"[^>]*>Download ([^<]+)</h1>").getMatch(0);
                }
                final String officialDownloadFilesize = this.getDllinkViaOfficialVideoDownload(br, link, account, true);
                if (officialDownloadFilesize != null) {
                    /* Do not change pluginpatternmatcher back */
                    logger.info("Item is officially downloadable!");
                    link.setDownloadSize(SizeFormatter.getSize(officialDownloadFilesize));
                    link.setProperty(IS_OFFICIALLY_DOWNLOADABLE, true);
                } else {
                    link.setPluginPatternMatcher(contentURL);
                    link.setProperty(IS_OFFICIALLY_DOWNLOADABLE, false);
                }
            } finally {
                link.setPluginPatternMatcher(pluginpatternmatcherOld);
            }
        }
        if (br.getURL().matches("https?://[^/]+/d/[a-z0-9]{12}.*") && !link.hasProperty(EXTENDED_FILENAME_RESULT) && StringUtils.isEmpty(specialFilenameResult)) {
            /*
             * 2021-11-18: Workaround e.g. for items for which uploader has disabled download button because upper handling will fail to
             * find a nice filename.
             */
            final Browser brc = br.cloneBrowser();
            this.getPage(brc, "https://" + this.getHost() + "/" + this.getFUIDFromURL(link));
            final String[] fileInfo = super.scanInfo(brc.toString(), super.internal_getFileInfoArray());
            specialFilenameResult = fileInfo[0];
        }
        if (!StringUtils.isEmpty(specialFilenameResult)) {
            logger.info("Found nice file-title: " + specialFilenameResult);
            link.setProperty(EXTENDED_FILENAME_RESULT, specialFilenameResult);
            setFilename(specialFilenameResult, link, br);
        }
        return result;
    }

    protected void checkErrors(final Browser br, final String html, final DownloadLink link, final Account account, final boolean checkAll) throws NumberFormatException, PluginException {
        super.checkErrors(br, html, link, account, checkAll);
        if (br.containsHTML("(?i)>\\s*File owner disabled downloads")) {
            throw new PluginException(LinkStatus.ERROR_FATAL, "File owner disabled downloads");
        }
    }

    @Override
    public String rewriteHost(final String host) {
        return this.rewriteHost(getPluginDomains(), host);
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
            ret.add("https?://(?:www\\.)?" + buildHostsPatternPart(domains) + "/(?:d/[a-z0-9]{12}|e/[a-z0-9]{12}|c/[a-z0-9]{12}|(?:embed-)?[a-z0-9]{12}(?:/[^/]+(?:\\.html)?)?)");
        }
        return ret.toArray(new String[0]);
    }

    @Override
    public String getFUIDFromURL(final DownloadLink link) {
        if (link == null || link.getPluginPatternMatcher() == null) {
            return null;
        } else if (link.getPluginPatternMatcher().matches(PATTERN_SPECIAL)) {
            return new Regex(link.getPluginPatternMatcher(), PATTERN_SPECIAL).getMatch(2);
        } else {
            return super.getFUIDFromURL(link);
        }
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

    /** Copy and pasted from main class. Contains ugly workarounds! Do not use anywhere else! */
    @Override
    protected String getDllinkViaOfficialVideoDownload(final Browser brc, final DownloadLink link, final Account account, final boolean returnFilesize) throws Exception {
        if (returnFilesize) {
            logger.info("[FilesizeMode] Trying to find official video downloads");
        } else {
            logger.info("[DownloadMode] Trying to find official video downloads");
        }
        String dllink = null;
        /* Info in table. E.g. xvideosharing.com, watchvideo.us */
        String[] videoQualityHTMLs = brc.getRegex("<tr><td>[^\r\t\n]+download_video\\(.*?</td></tr>").getColumn(-1);
        if (videoQualityHTMLs.length == 0) {
            /* Match on line - safe attempt but this may not include filesize! */
            videoQualityHTMLs = brc.getRegex("download_video\\([^\r\t\n]+").getColumn(-1);
        }
        if (videoQualityHTMLs.length == 0) {
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
        qualityMap.put("x", 100); // downloads
        long maxInternalQualityValue = 0;
        String filesizeStr = null;
        String videoQualityStr = null;
        String videoHash = null;
        String targetHTML = null;
        final String userSelectedQualityValue = getPreferredDownloadQuality();
        boolean foundUserSelectedQuality = false;
        if (userSelectedQualityValue == null) {
            logger.info("Trying to find highest quality for official video download");
        } else {
            logger.info(String.format("Trying to find user selected quality %s for official video download", userSelectedQualityValue));
        }
        int selectedQualityIndex = 0;
        for (int currentQualityIndex = 0; currentQualityIndex < videoQualityHTMLs.length; currentQualityIndex++) {
            final String videoQualityHTML = videoQualityHTMLs[currentQualityIndex];
            final String filesizeStrTmp = new Regex(videoQualityHTML, "(([0-9\\.]+)\\s*(KB|MB|GB|TB))").getMatch(0);
            // final String vid = videoinfo.getMatch(0);
            final Regex videoinfo = new Regex(videoQualityHTML, "download_video\\('([a-z0-9]+)','([^<>\"\\']*)','([^<>\"\\']*)'");
            // final String vid = videoinfo.getMatch(0);
            /* Usually this will be 'o' standing for "original quality" */
            final String videoQualityStrTmp = videoinfo.getMatch(1);
            final String videoHashTmp = videoinfo.getMatch(2);
            if (StringUtils.isEmpty(videoQualityStrTmp) || StringUtils.isEmpty(videoHashTmp)) {
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
            if (userSelectedQualityValue != null && videoQualityStrTmp.equalsIgnoreCase(userSelectedQualityValue)) {
                logger.info("Found user selected quality: " + userSelectedQualityValue);
                foundUserSelectedQuality = true;
                videoQualityStr = videoQualityStrTmp;
                videoHash = videoHashTmp;
                if (filesizeStrTmp != null) {
                    /*
                     * Usually, filesize for official video downloads will be given but not in all cases. It may also happen that our upper
                     * RegEx fails e.g. for supervideo.tv.
                     */
                    filesizeStr = filesizeStrTmp;
                }
                targetHTML = videoQualityHTML;
                selectedQualityIndex = currentQualityIndex;
                break;
            } else {
                /* Look for best quality */
                final int internalQualityValueTmp = qualityMap.get(videoQualityStrTmp);
                if (internalQualityValueTmp < maxInternalQualityValue) {
                    /* Only continue with qualities that are higher than the highest we found so far. */
                    continue;
                }
                maxInternalQualityValue = internalQualityValueTmp;
                videoQualityStr = videoQualityStrTmp;
                videoHash = videoHashTmp;
                if (filesizeStrTmp != null) {
                    /*
                     * Usually, filesize for official video downloads will be given but not in all cases. It may also happen that our upper
                     * RegEx fails e.g. for supervideo.tv.
                     */
                    filesizeStr = filesizeStrTmp;
                }
                targetHTML = videoQualityHTML;
                selectedQualityIndex = currentQualityIndex;
            }
        }
        if (targetHTML == null || videoQualityStr == null || videoHash == null) {
            if (videoQualityHTMLs != null && videoQualityHTMLs.length > 0) {
                /* This should never happen */
                logger.info(String.format("Failed to find officially downloadable video quality although there are %d qualities available", videoQualityHTMLs.length));
            }
            return null;
        }
        if (filesizeStr == null) {
            /*
             * Last chance attempt to find filesize for selected quality. Only allow units "MB" and "GB" as most filesizes will have one of
             * these units.
             */
            final String[] filesizeCandidates = br.getRegex("(\\d+(?:\\.\\d{1,2})? *(MB|GB))").getColumn(0);
            /* Are there as many filesizes available as there are video qualities --> Chose correct filesize by index */
            if (filesizeCandidates.length == videoQualityHTMLs.length) {
                filesizeStr = filesizeCandidates[selectedQualityIndex];
            }
        }
        if (foundUserSelectedQuality) {
            logger.info("Found user selected quality: " + userSelectedQualityValue);
        } else {
            logger.info("Picked BEST quality: " + videoQualityStr);
        }
        if (filesizeStr == null) {
            /* No dramatic failure */
            logger.info("Failed to find filesize");
        } else {
            logger.info("Found filesize of official video download: " + filesizeStr);
        }
        if (returnFilesize) {
            /* E.g. in availablecheck */
            return filesizeStr;
        }
        /* 2019-08-29: Waittime here is possible but a rare case e.g. deltabit.co */
        this.waitTime(link, Time.systemIndependentCurrentJVMTimeMillis());
        getPage(brc, "/dl?op=download_orig&id=" + this.getFUIDFromURL(link) + "&mode=" + videoQualityStr + "&hash=" + videoHash);
        /* 2019-08-29: This Form may sometimes be given e.g. deltabit.co */
        Form download1 = brc.getFormByInputFieldKeyValue("op", "download1");
        download1 = brc.getFormbyProperty("id", "F1");
        if (download1 != null) {
            this.handleCaptcha(link, brc, download1);
            this.submitForm(brc, download1);
            this.checkErrors(brc, brc.toString(), link, account, false);
        }
        /*
         * 2019-10-04: TODO: Unsure whether we should use the general 'getDllink' method here as it contains a lot of RegExes (e.g. for
         * streaming URLs) which are completely useless here.
         */
        dllink = this.getDllink(link, account, brc, brc.toString());
        if (StringUtils.isEmpty(dllink)) {
            /*
             * 2019-05-30: Test - worked for: xvideosharing.com - not exactly required as getDllink will usually already return a result.
             */
            dllink = brc.getRegex("(?i)<a href=\"(https?[^\"]+)\"[^>]*>\\s*Direct Download Link\\s*</a>").getMatch(0);
            if (StringUtils.isEmpty(dllink)) {
                /* 2022-09-03 */
                dllink = brc.getRegex("(?i)<a href=\"(https?://[^\"]+)\"[^>]*>\\s*Download Video\\s*</a>").getMatch(0);
            }
        }
        if (StringUtils.isEmpty(dllink)) {
            logger.warning("Failed to find dllink via official video download");
        } else {
            logger.info("Successfully found dllink via official video download");
        }
        return dllink;
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
    protected boolean isVideohoster_enforce_video_filename() {
        return true;
    }

    @Override
    protected boolean isShortURL(final DownloadLink link) {
        return false;
    }
}