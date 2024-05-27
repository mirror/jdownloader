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
import java.util.List;

import org.appwork.utils.StringUtils;
import org.jdownloader.plugins.components.XFileSharingProBasic;

import jd.PluginWrapper;
import jd.http.Browser;
import jd.http.URLConnectionAdapter;
import jd.parser.Regex;
import jd.plugins.Account;
import jd.plugins.Account.AccountType;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;

@HostPlugin(revision = "$Revision$", interfaceVersion = 3, names = {}, urls = {})
public class GounlimitedTo extends XFileSharingProBasic {
    public GounlimitedTo(final PluginWrapper wrapper) {
        super(wrapper);
        this.enablePremium(super.getPurchasePremiumURL());
    }

    /**
     * DEV NOTES XfileSharingProBasic Version SEE SUPER-CLASS<br />
     * mods: See overridden functions<br />
     * limit-info:<br />
     * captchatype-info: null<br />
     * other:<br />
     */
    public static List<String[]> getPluginDomains() {
        final List<String[]> ret = new ArrayList<String[]>();
        // each entry in List<String[]> will result in one PluginForHost, Plugin.getHost() will return String[0]->main domain
        ret.add(new String[] { "gounlimited.to" });
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
        return XFileSharingProBasic.buildAnnotationUrls(getPluginDomains());
    }

    @Override
    public boolean isResumeable(final DownloadLink link, final Account account) {
        if (account != null && account.getType() == AccountType.FREE) {
            /* Free Account */
            return true;
        } else if (account != null && account.getType() == AccountType.PREMIUM) {
            /* Premium account */
            return true;
        } else {
            /* Free(anonymous) and unknown account type */
            return true;
        }
    }

    @Override
    public int getMaxChunks(final Account account) {
        if (account != null && account.getType() == AccountType.FREE) {
            /* Free Account */
            return -2;
        } else if (account != null && account.getType() == AccountType.PREMIUM) {
            /* Premium account */
            return -2;
        } else {
            /* Free(anonymous) and unknown account type */
            return -2;
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
        return 1;
    }

    @Override
    public boolean isVideohosterEmbed() {
        return true;
    }

    @Override
    public boolean isVideohoster_enforce_video_filename() {
        return true;
    }

    @Override
    public boolean supports_availablecheck_alt() {
        return false;
    }

    @Override
    public boolean supports_availablecheck_filename_abuse() {
        return false;
    }

    @Override
    protected boolean supports_availablecheck_filesize_html() {
        /* 2019-05-21: Special: Usually videohosts do not support this! */
        return true;
    }

    @Override
    public boolean supports_availablecheck_filesize_via_embedded_video() {
        return true;
    }

    @Override
    public String[] scanInfo(final String[] fileInfo) {
        /* 2020-02-04: Prefer filename from other place as it sometimes contains more details than what template plugin would grab. */
        super.scanInfo(fileInfo);
        final String better_filename = new Regex(correctedBR, "<h2 class=\"the_title mb-2\">([^<>\"]+)</h2>").getMatch(0);
        if (better_filename != null) {
            logger.info("Found better_filename");
            fileInfo[0] = better_filename;
        } else {
            logger.info("Failed to find better_filename");
        }
        return fileInfo;
    }

    @Override
    protected boolean isOffline(final DownloadLink link, final Browser br) {
        if (br.containsHTML("<title>Watch 404 not found</title>|content=\"Watch video 404 not found\"")) {
            /* 2020-10-14: Offline content will be liked to a sample video instead lol example: https://gounlimited.to/jdexamplebla */
            return true;
        } else {
            return super.isOffline(link, br);
        }
    }

    @Override
    protected void checkErrors(final Browser br, final String html, final DownloadLink link, final Account account, final boolean checkAll) throws NumberFormatException, PluginException {
        super.checkErrors(br, html, link, account, checkAll);
        if (br.containsHTML("No such file=")) {
            /* 2024-05-27: Special offline: Broken download of original file [looks like offline] */
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND, "Server error 'No such file'");
        }
    }

    /**
     * 2020-08-10: Special: Some offline files are not displayed as offline. Instead, final downloadurl leads to error 404 and browser shows
     * "404 this video was removed" animation on play button click.
     */
    @Override
    public AvailableStatus requestFileInformationWebsite(final DownloadLink link, final Account account, final boolean downloadsStarted) throws Exception {
        final AvailableStatus status = super.requestFileInformationWebsite(link, account, downloadsStarted);
        logger.info("File appears to be online --> Let's deep-check");
        final String dllink = this.getDllink(link, account, br, correctedBR);
        if (!StringUtils.isEmpty(dllink)) {
            /* Get- and set filesize from directurl */
            boolean dllink_is_valid = false;
            URLConnectionAdapter con = null;
            try {
                final Browser br2 = br.cloneBrowser();
                br2.setFollowRedirects(true);
                con = openAntiDDoSRequestConnection(br2, br2.createHeadRequest(dllink));
                /* For video streams we often don't get a Content-Disposition header. */
                if (con.getResponseCode() == 404) {
                    /* 2020-08-10: Special */
                    throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
                } else if (con.getResponseCode() == 503) {
                    /* Ok */
                    /*
                     * Too many connections but that does not mean that our directlink is invalid. Accept it and if it still returns 503 on
                     * download-attempt this error will get displayed to the user - such directlinks should work again once there are less
                     * active connections to the host!
                     */
                    logger.info("directurl lead to 503 | too many connections");
                    dllink_is_valid = true;
                } else if (con.getCompleteContentLength() > 0 && looksLikeDownloadableContent(con)) {
                    if (con.getCompleteContentLength() == 1301046l) {
                        /* 2020-08-10: "This video is offline" video ... */
                        throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
                    }
                    if (/* StringUtils.equalsIgnoreCase(con.getContentType(), "application/octet-stream") && */con.getCompleteContentLength() < 100) {
                        throw new Exception("very likely no file but an error message!length=" + con.getCompleteContentLength());
                    } else {
                        dllink_is_valid = true;
                    }
                } else {
                    /* Failure */
                    // throw new Exception("no downloadable content?" + con.getResponseCode() + "|" + con.getContentType() + "|" +
                    // con.isContentDisposition());
                }
            } catch (final Exception e) {
                /* Failure */
                logger.log(e);
                if (downloadsStarted) {
                    throw e;
                }
            } finally {
                if (con != null) {
                    try {
                        con.disconnect();
                    } catch (final Throwable e) {
                    }
                }
            }
            /* Store directurl if it is valid */
            if (dllink_is_valid) {
                storeDirecturl(link, account, dllink);
            }
        }
        return status;
    }

    @Override
    protected String getDllinkViaOfficialVideoDownload(final Browser br, final DownloadLink link, final Account account, final boolean returnFilesize) throws Exception {
        String result = super.getDllinkViaOfficialVideoDownload(br, link, account, returnFilesize);
        if (result == null) {
            this.checkErrors(br, br.getRequest().getHtmlCode(), link, account, false);
        }
        return result;
    }
}