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

import org.appwork.utils.StringUtils;
import org.jdownloader.captcha.v2.challenge.recaptcha.v2.CaptchaHelperHostPluginRecaptchaV2;
import org.jdownloader.plugins.components.XFileSharingProBasic;
import org.jdownloader.plugins.components.config.XFSConfigVideo.DownloadMode;
import org.jdownloader.plugins.components.config.XFSConfigVideoFilemoonSx;

import jd.PluginWrapper;
import jd.http.Browser;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.parser.html.Form;
import jd.parser.html.Form.MethodType;
import jd.plugins.Account;
import jd.plugins.Account.AccountType;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.decrypter.FilemoonSxCrawler;

@HostPlugin(revision = "$Revision$", interfaceVersion = 3, names = {}, urls = {})
public class FilemoonSx extends XFileSharingProBasic {
    public FilemoonSx(final PluginWrapper wrapper) {
        super(wrapper);
        this.enablePremium(super.getPurchasePremiumURL());
    }

    /**
     * DEV NOTES XfileSharingProBasic Version SEE SUPER-CLASS<br />
     * mods: See overridden functions<br />
     * limit-info: 2022-06-21: No limits at all <br />
     * captchatype-info: 2022-06-21: reCaptchaV2<br />
     * other:<br />
     */
    public static List<String[]> getPluginDomains() {
        return FilemoonSxCrawler.getPluginDomains();
    }

    public static String[] getAnnotationNames() {
        return buildAnnotationNames(getPluginDomains());
    }

    @Override
    public String[] siteSupportedNames() {
        return buildSupportedNames(getPluginDomains());
    }

    public static String[] getAnnotationUrls() {
        return FilemoonSxCrawler.getAnnotationUrls();
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
        /* 2022-06-21: Special */
        return true;
    }

    @Override
    public String getFUIDFromURL(final DownloadLink link) {
        try {
            final String url = link.getPluginPatternMatcher();
            if (url != null) {
                final String result = new Regex(new URL(url).getPath(), "/./([a-z0-9]+)").getMatch(0);
                return result;
            }
        } catch (MalformedURLException e) {
            logger.log(e);
        }
        return null;
    }

    @Override
    public String getFilenameFromURL(final DownloadLink link) {
        return new Regex(link.getPluginPatternMatcher(), "https?://[^/]+/./[^/]+/(.+)").getMatch(0);
    }

    @Override
    protected String buildURLPath(final DownloadLink link, final String fuid, final URL_TYPE type) {
        switch (type) {
        case EMBED_VIDEO:
            return "/e/" + fuid;
        case NORMAL:
            return "/d/" + fuid;
        default:
            throw new IllegalArgumentException("Unsupported type:" + type + "|" + fuid);
        }
    }

    @Override
    protected boolean supports_availablecheck_alt() {
        return false;
    }

    @Override
    protected boolean supports_availablecheck_filesize_alt_fast() {
        return false;
    }

    @Override
    protected boolean supports_availablecheck_filename_abuse() {
        return false;
    }

    @Override
    protected boolean isShortURL(DownloadLink link) {
        return false;
    }

    @Override
    public String[] scanInfo(final String html, final String[] fileInfo) {
        super.scanInfo(html, fileInfo);
        /* 2022-11-04 */
        final String betterFilename = new Regex(html, "<h3[^>]*>([^<]+)</h3>").getMatch(0);
        if (betterFilename != null) {
            fileInfo[0] = betterFilename;
        }
        return fileInfo;
    }

    @Override
    public AvailableStatus requestFileInformationWebsite(final DownloadLink link, final Account account, final boolean isDownload) throws Exception {
        final URL_TYPE type = this.getURLType(link);
        if (type == URL_TYPE.EMBED_VIDEO_2) {
            /* Special handling */
            if (!link.isNameSet()) {
                /* Set fallback-filename */
                setWeakFilename(link, null);
            }
            final String url = link.getPluginPatternMatcher();
            final boolean isFollowRedirect = br.isFollowingRedirects();
            try {
                br.setFollowRedirects(true);
                if (probeDirectDownload(link, account, br, br.createGetRequest(url), true)) {
                    return AvailableStatus.TRUE;
                }
            } finally {
                br.setFollowRedirects(isFollowRedirect);
            }
            final boolean isRefererBlocked = this.isRefererBlocked(br);
            if (isRefererBlocked) {
                /* We know that the item is online but we can't download it. */
                return AvailableStatus.TRUE;
            }
            /* Try to find filename */
            /* Check for errors */
            this.checkErrors(br, url, link, account, false);
            /* Try to find filename */
            final Browser brc = br.cloneBrowser();
            this.getPage(brc, "/d/" + this.getFUIDFromURL(link));
            if (this.isOffline(link, brc, brc.getRequest().getHtmlCode())) {
                logger.info("Video item looks to be not downloadable");
            }
            final String[] fileInfo = internal_getFileInfoArray();
            scanInfo(fileInfo);
            processFileInfo(fileInfo, brc, link);
            if (!StringUtils.isEmpty(fileInfo[0])) {
                /* Correct- and set filename */
                setFilename(fileInfo[0], link, brc);
            } else {
                /*
                 * Fallback. Do this again as now we got the html code available so we can e.g. know if this is a video-filehoster or not.
                 */
                this.setWeakFilename(link, brc);
            }
            /* No filename given -> Return AvailableStatus */
            return AvailableStatus.TRUE;
        } else {
            return super.requestFileInformationWebsite(link, account, isDownload);
        }
    }

    @Override
    public void doFree(final DownloadLink link, final Account account) throws Exception, PluginException {
        /* First bring up saved final links */
        String dllink = checkDirectLink(link, account);
        String streamDownloadurl = null;
        grabOfficialVideoDownloadDirecturl: if (StringUtils.isEmpty(dllink)) {
            requestFileInformationWebsite(link, account, true);
            final DownloadMode mode = this.getPreferredDownloadModeFromConfig();
            streamDownloadurl = this.getDllink(link, account, br, br.getRequest().getHtmlCode());
            if (!StringUtils.isEmpty(streamDownloadurl) && (mode == DownloadMode.STREAM || mode == DownloadMode.AUTO)) {
                /* User prefers to download stream -> We can skip the captcha required to find official video downloadurl. */
                break grabOfficialVideoDownloadDirecturl;
            }
            this.checkErrors(br, this.getCorrectBR(br), link, account, false);
            if (!br.getURL().matches(".*/download/.*")) {
                this.getPage("/download/" + this.getFUIDFromURL(link));
            }
            final String recaptchaV2Response = new CaptchaHelperHostPluginRecaptchaV2(this, br, "6LdiBGAgAAAAAIQm_arJfGYrzjUNP_TCwkvPlv8k").getToken();
            final Form dlform = new Form();
            dlform.setMethod(MethodType.POST);
            dlform.put("b", "download");
            dlform.put("file_code", this.getFUIDFromURL(link));
            dlform.put("g-recaptcha-response", Encoding.urlEncode(recaptchaV2Response));
            this.submitForm(dlform);
            if (isSpecialError404(br)) {
                /* 2023-05-04 */
                if (streamDownloadurl != null) {
                    logger.info("Official download is not possible -> Fallback to stream download");
                } else {
                    throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Error 404 download impossible at this moment");
                }
            }
            dllink = this.getDllink(link, account, br, br.getRequest().getHtmlCode());
            if (StringUtils.isEmpty(dllink) && !StringUtils.isEmpty(streamDownloadurl)) {
                logger.info("Failed to find official downloadurl -> Fallback to stream download");
                // dllink = streamDownloadurl;
                /* Fallback happens in upper code */
            }
        }
        handleDownload(link, account, dllink, streamDownloadurl, null);
    }

    @Override
    protected String getDllink(final DownloadLink link, final Account account, final Browser br, String src) {
        /* 2023-04-20: New */
        final String dllink = br.getRegex("(?i)>\\s*Your download link</div>\\s*<a href=\"(https?://[^\"]+)").getMatch(0);
        if (dllink != null) {
            return dllink;
        } else {
            return super.getDllink(link, account, br, src);
        }
    }

    @Override
    protected void checkErrors(final Browser br, final String html, final DownloadLink link, final Account account, final boolean checkAll) throws NumberFormatException, PluginException {
        super.checkErrors(br, html, link, account, checkAll);
        /* 2022-11-04: Website failure after captcha on "/download/..." page */
        if (isSpecialError404(br)) {
            throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server error 404");
        } else if (isRefererBlocked(br)) {
            throw new PluginException(LinkStatus.ERROR_FATAL, "Custom referer needed to download this item");
        } else if (br.containsHTML(">\\s*This video is not available in your country")) {
            throw new PluginException(LinkStatus.ERROR_FATAL, "GEO-blocked");
        }
    }

    private boolean isSpecialError404(final Browser br) {
        return br.containsHTML("(?i)class=\"error e404\"|>\\s*Page not found");
    }

    private boolean isRefererBlocked(final Browser br) {
        return br.containsHTML(">\\s*This video cannot be watched under this domain");
    }

    @Override
    protected boolean isOffline(final DownloadLink link, final Browser br, final String correctedBR) {
        if (br.containsHTML("(?i)<h1>\\s*Page not found|class=\"error e404\"")) {
            return true;
        } else {
            return super.isOffline(link, br, correctedBR);
        }
    }

    @Override
    public Class<? extends XFSConfigVideoFilemoonSx> getConfigInterface() {
        return XFSConfigVideoFilemoonSx.class;
    }
}