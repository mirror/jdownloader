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

import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;
import java.util.Map;

import org.appwork.storage.TypeRef;
import org.appwork.utils.StringUtils;
import org.jdownloader.captcha.v2.challenge.recaptcha.v2.CaptchaHelperHostPluginRecaptchaV2;
import org.jdownloader.plugins.components.XFileSharingProBasic;
import org.jdownloader.plugins.components.config.XFSConfigVideo.DownloadMode;
import org.jdownloader.plugins.components.config.XFSConfigVideoVoeSx;

import jd.PluginWrapper;
import jd.http.Browser;
import jd.http.Cookie;
import jd.http.Cookies;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.parser.html.Form;
import jd.plugins.Account;
import jd.plugins.Account.AccountType;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginDependencies;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;
import jd.plugins.decrypter.VoeSxCrawler;

@HostPlugin(revision = "$Revision$", interfaceVersion = 3, names = {}, urls = {})
@PluginDependencies(dependencies = { VoeSxCrawler.class })
public class VoeSx extends XFileSharingProBasic {
    public VoeSx(final PluginWrapper wrapper) {
        super(wrapper);
        this.enablePremium(super.getPurchasePremiumURL());
    }

    public String getPurchasePremiumURL() {
        return this.getMainPage() + "/register";
    }

    /**
     * DEV NOTES XfileSharingProBasic Version SEE SUPER-CLASS<br />
     * mods: See overridden functions<br />
     * limit-info: 2020-11-27: Premium untested, set FREE limits <br />
     * captchatype-info: 2020-08-19: null<br />
     * other:<br />
     */
    public static List<String[]> getPluginDomains() {
        return VoeSxCrawler.getPluginDomains();
    }

    public static String[] getAnnotationNames() {
        return buildAnnotationNames(getPluginDomains());
    }

    @Override
    public String[] siteSupportedNames() {
        return buildSupportedNames(getPluginDomains());
    }

    public static String[] getAnnotationUrls() {
        return VoeSxCrawler.getAnnotationUrls();
    }

    @Override
    public String getFUIDFromURL(final DownloadLink dl) {
        try {
            final String result = new Regex(new URL(dl.getPluginPatternMatcher()).getPath(), "/(?:embed-|e/)?([a-z0-9]{12})").getMatch(0);
            return result;
        } catch (MalformedURLException e) {
            logger.log(e);
        }
        return null;
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
    protected boolean isVideohoster_enforce_video_filename() {
        /* 2020-08-19 */
        return true;
    }

    @Override
    protected boolean supportsAPIMassLinkcheck() {
        return looksLikeValidAPIKey(this.getAPIKey());
    }

    @Override
    protected boolean supportsAPISingleLinkcheck() {
        return looksLikeValidAPIKey(this.getAPIKey());
    }

    @Override
    protected boolean isVideohosterEmbed() {
        /* 2021-03-09 */
        return true;
    }

    @Override
    protected String getDllinkVideohost(DownloadLink link, Account account, Browser br, final String src) {
        final String mp4Master = new Regex(src, "(?i)(\"|')mp4\\1\\s*:\\s*(\"|')(https?://[^\"']+)").getMatch(2);
        if (mp4Master != null) {
            return mp4Master;
        }
        String hlsMaster = new Regex(src, "(?i)(\"|')hls\\1\\s*:\\s*(\"|')(https?://[^\"']+)").getMatch(2);
        if (hlsMaster == null) {
            /* 2023-11-21 */
            hlsMaster = new Regex(src, "(?i)\"(https?://[^/]+/engine/hls[^\"]+)").getMatch(0);
        }
        String altSourceB64 = br.getRegex("let wc0 = '([^\\']+)").getMatch(0);
        if (altSourceB64 == null) {
            /* 2024-02-23 */
            altSourceB64 = br.getRegex("let [^=]+ = '(ey[^\\']+)").getMatch(0);
            if (altSourceB64 == null) {
                /* 2024-02-26 */
                altSourceB64 = br.getRegex("let [a-f0-9]+ = '([^\\']+)").getMatch(0);
            }
        }
        if (altSourceB64 != null && hlsMaster == null) {
            /* 2024-01-23 */
            String json = Encoding.Base64Decode(altSourceB64);
            if (json.startsWith("}")) {
                /* 2024-02-26 */
                json = new StringBuilder(json).reverse().toString();
            }
            final Map<String, Object> entries = restoreFromString(json, TypeRef.MAP);
            hlsMaster = (String) entries.get("file");
        }
        if (hlsMaster != null) {
            return hlsMaster;
        } else {
            return super.getDllinkVideohost(link, account, br, src);
        }
    }

    @Override
    protected void checkErrors(final Browser br, final String html, final DownloadLink link, final Account account, final boolean checkAll) throws NumberFormatException, PluginException {
        super.checkErrors(br, html, link, account, checkAll);
        // if (br.containsHTML(">\\s*This video can be watched as embed only")) {
        // throw new PluginException(LinkStatus.ERROR_FATAL, "This video can be watched as embed only");
        // }
        if (br.containsHTML(">\\s*Server overloaded, download temporary disabled|The server of this file is currently over")) {
            /* 2023-10-26 */
            throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server overloaded");
        } else if (br.containsHTML(">\\s*Access to this file has been temporarily restricted")) {
            /* 2023-11-29 */
            throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Access to this file has been temporarily restricted");
        }
    }

    @Override
    public AvailableStatus requestFileInformationWebsite(final DownloadLink link, final Account account, final boolean isDownload) throws Exception {
        if (link.getPluginPatternMatcher().matches("(?i)https?://[^/]+/(e/|embed-).+")) {
            this.requestFileInformationVideoEmbed(br, link, account, isDownload);
            return AvailableStatus.TRUE;
        } else {
            return super.requestFileInformationWebsite(link, account, isDownload);
        }
    }

    @Override
    protected String requestFileInformationVideoEmbed(final Browser br, final DownloadLink link, final Account account, final boolean findFilesize) throws Exception {
        /* 2021-03-09: Special: New browser required else they won't let us stream some videos at all! */
        final boolean embedOnly = br.containsHTML(">\\s*This video can be watched as embed only");
        br.setFollowRedirects(true);
        boolean fallBackFileName = true;
        try {
            getPage(this.getMainPage(link) + "/e/" + this.getFUIDFromURL(link));
            if (this.isOffline(link, br)) {
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            }
            final String[] fileInfo = internal_getFileInfoArray();
            scanInfo(br.getRequest().getHtmlCode(), fileInfo);
            processFileInfo(fileInfo, br, link);
            if (!StringUtils.isEmpty(fileInfo[0])) {
                /* Correct- and set filename */
                setFilename(fileInfo[0], link, br);
                fallBackFileName = false;
            } else {
                /*
                 * Fallback. Do this again as now we got the html code available so we can e.g. know if this is a video-filehoster or not.
                 */
                fallBackFileName = true;
            }
            final String dllink = getDllinkVideohost(link, account, br, br.getRequest().getHtmlCode());
            if (StringUtils.isEmpty(dllink) && embedOnly) {
                throw new PluginException(LinkStatus.ERROR_FATAL, "This video can be watched as embed only");
            } else {
                return dllink;
            }
        } finally {
            if (fallBackFileName) {
                this.setWeakFilename(link, br);
            }
        }
    }

    @Override
    public String[] scanInfo(final String html, final String[] fileInfo) {
        super.scanInfo(html, fileInfo);
        String betterTitle = new Regex(html, "class=\"player-title\"[^>]*>([^<]+)").getMatch(0);
        if (betterTitle == null) {
            /* 2024-01-03 */
            betterTitle = new Regex(html, "name=\"og:title\" content=\"([^\"]+)").getMatch(0);
        }
        if (betterTitle != null) {
            fileInfo[0] = betterTitle;
        }
        return fileInfo;
    }

    @Override
    protected void processFileInfo(String[] fileInfo, Browser altbr, DownloadLink link) {
        if (fileInfo != null && fileInfo[0] != null) {
            fileInfo[0] = fileInfo[0].replaceFirst("(\\s*-\\s*VOE\\s*\\|\\s*Content\\s*Delivery\\s*Network\\s*\\(CDN\\)\\s*&\\s*Video\\s*Cloud)", "");
        }
        super.processFileInfo(fileInfo, altbr, link);
    }

    @Override
    protected String getDllinkViaOfficialVideoDownload(final Browser br, final DownloadLink link, final Account account, final boolean returnFilesize) throws Exception {
        if (returnFilesize) {
            return null;
        }
        logger.info("[DownloadMode] Trying to find official video downloads");
        String continueLink = br.getRegex("(?:\"|')(/dl\\?op=download_orig[^\"\\']+)").getMatch(0);
        if (continueLink == null) {
            /* 2023-10-07 */
            continueLink = br.getRegex("(?:\"|')((https?://[^/]+)?/[a-z0-9]{12}/download)").getMatch(0);
        }
        if (continueLink == null) {
            /* No official download available */
            logger.info("Failed to find any official video downloads");
            return null;
        }
        final String streamDownloadlink = getDllinkVideohost(link, account, br, br.getRequest().getHtmlCode());
        final DownloadMode mode = this.getPreferredDownloadModeFromConfig();
        if (streamDownloadlink != null && (mode == DownloadMode.STREAM || mode == DownloadMode.AUTO) && Boolean.TRUE.equals(requiresCaptchaForOfficialVideoDownload())) {
            /*
             * User wants to download stream. Obtaining an official downloadlink would require the user to enter a captcha -> Skip that.
             */
            return null;
        }
        if (br.containsHTML("&embed=&adb=")) {
            /* 2022-08-24: This might give us more download-speed, not sure though. */
            continueLink += "&embed=&adb=0";
        }
        getPage(br, continueLink);
        final Form dlform = br.getFormbyActionRegex("(?i).+/download$");
        if (dlform != null) {
            try {
                reCaptchaSiteurlWorkaround = br.getURL();
                this.handleCaptcha(link, br, dlform);
            } finally {
                reCaptchaSiteurlWorkaround = null;
            }
            this.submitForm(br, dlform);
        }
        String dllink = this.getDllink(link, account, br, br.getRequest().getHtmlCode());
        if (StringUtils.isEmpty(dllink)) {
            /*
             * 2019-05-30: Test - worked for: xvideosharing.com - not exactly required as getDllink will usually already return a result.
             */
            dllink = br.getRegex("(?i)>\\s*Download Link\\s*</td>\\s*<td><a href=\"(https?://[^\"]+)\"").getMatch(0);
            if (dllink == null) {
                /* 2023-10-07 */
                dllink = br.getRegex("<a href=\"(http[^\"]+)\"[^>]*class=\"btn btn-primary\" target=\"_blank\"").getMatch(0);
                if (dllink == null) {
                    /* 2023-11-21 */
                    dllink = br.getRegex("\"(https?://[^/]+/engine/download/[^\"]+)\"").getMatch(0);
                }
            }
            if (dllink != null) {
                dllink = Encoding.htmlOnlyDecode(dllink);
            }
        }
        if (StringUtils.isEmpty(dllink)) {
            logger.warning("Failed to find dllink via official video download");
        } else {
            logger.info("Successfully found dllink via official video download");
            final String filesizeBytesStr = br.getRegex("File Size \\(bytes\\)</td>\\s*<td>\\s*(\\d+)\\s*<").getMatch(0);
            if (filesizeBytesStr != null) {
                link.setVerifiedFileSize(Long.parseLong(filesizeBytesStr));
            }
        }
        return dllink;
    }

    @Override
    public Form findLoginform(final Browser br) {
        final Form loginform = br.getFormbyActionRegex(".*/login$");
        if (loginform != null) {
            return loginform;
        } else {
            return super.findLoginform(br);
        }
    }

    @Override
    public boolean isLoggedin(final Browser brc) {
        final boolean logout = brc.containsHTML("/logout");
        boolean cookie1 = false;
        boolean cookie2 = false;
        for (final Cookies cookies : brc.getCookies().values()) {
            for (final Cookie cookie : cookies.getCookies()) {
                if (cookie.getKey().matches("remember_web_.+")) {
                    cookie1 = true;
                } else if (cookie.getKey().equalsIgnoreCase("voe_session")) {
                    cookie2 = true;
                }
            }
        }
        logger.info("cookie1:" + cookie1);
        logger.info("cookie2:" + cookie2);
        logger.info("logout:" + logout);
        if (logout && (cookie1 || cookie2)) {
            return true;
        } else {
            return super.isLoggedin(brc);
        }
    }

    private String reCaptchaSiteurlWorkaround = null;

    @Override
    protected CaptchaHelperHostPluginRecaptchaV2 getCaptchaHelperHostPluginRecaptchaV2(PluginForHost plugin, Browser br) throws PluginException {
        return new CaptchaHelperHostPluginRecaptchaV2(this, br) {
            @Override
            protected String getSiteUrl() {
                if (reCaptchaSiteurlWorkaround != null) {
                    return reCaptchaSiteurlWorkaround;
                } else {
                    return super.getSiteUrl();
                }
            }
        };
    }

    @Override
    protected void getPage(final Browser ibr, final String page) throws Exception {
        super.getPage(ibr, page);
        final String redirect = ibr.getRegex("else \\{\\s*window\\.location\\.href = '(https?://[^\"\\']+)';").getMatch(0);
        if (redirect != null) {
            logger.info("Handle special js redirect: " + redirect);
            super.getPage(ibr, redirect);
        }
    }

    @Override
    public Class<? extends XFSConfigVideoVoeSx> getConfigInterface() {
        return XFSConfigVideoVoeSx.class;
    }

    protected Boolean requiresCaptchaForOfficialVideoDownload() {
        // TODO: Add override annotation once this gets added to XFS core
        return Boolean.TRUE;
    }
}