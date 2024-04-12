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

import org.jdownloader.plugins.components.XFileSharingProBasic;

import jd.PluginWrapper;
import jd.http.Browser;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.plugins.Account;
import jd.plugins.Account.AccountType;
import jd.plugins.DownloadLink;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;

@HostPlugin(revision = "$Revision$", interfaceVersion = 3, names = {}, urls = {})
public class StreamwishCom extends XFileSharingProBasic {
    public StreamwishCom(final PluginWrapper wrapper) {
        super(wrapper);
        this.enablePremium(super.getPurchasePremiumURL());
    }

    /**
     * DEV NOTES XfileSharingProBasic Version SEE SUPER-CLASS<br />
     * mods: See overridden functions<br />
     * limit-info:<br />
     * captchatype-info: 2024-04-04: null <br />
     * other:<br />
     */
    public static List<String[]> getPluginDomains() {
        final List<String[]> ret = new ArrayList<String[]>();
        // each entry in List<String[]> will result in one PluginForHost, Plugin.getHost() will return String[0]->main domain
        ret.add(new String[] { "streamwish.com", "streamwish.to", "awish.pro", "embedwish.com", "wishembed.pro", "vidcloud.top", "gdplry.online" });
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
        return StreamwishCom.buildAnnotationUrls(getPluginDomains());
    }

    public static final String getDefaultAnnotationPatternPartStreamwish() {
        return "/(?:d/[A-Za-z0-9]+|(?:embed-|e/|f/)?[a-z0-9]{12}(?:/[^/]+(?:\\.html)?)?)";
    }

    public static String[] buildAnnotationUrls(final List<String[]> pluginDomains) {
        final List<String> ret = new ArrayList<String>();
        for (final String[] domains : pluginDomains) {
            ret.add("https?://(?:www\\.)?" + buildHostsPatternPart(domains) + "(?::\\d+)?" + StreamwishCom.getDefaultAnnotationPatternPartStreamwish());
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
    protected boolean supports_availablecheck_filesize_html() {
        /* 2023-09-06 */
        return false;
    }

    @Override
    public String[] scanInfo(final String html, final String[] fileInfo) {
        super.scanInfo(html, fileInfo);
        final String betterFilename = br.getRegex("<h1 class=\"h5 mb-3\">([^<]+)</h1>").getMatch(0);
        if (betterFilename != null) {
            fileInfo[0] = betterFilename;
        }
        /**
         * Small workarounds for "sharebox2" regex in upper code which needs to be improved. </br>
         * It can catch wrong information.
         */
        fileInfo[1] = null;
        return fileInfo;
    }

    @Override
    public String getFUIDFromURL(final DownloadLink link) {
        final Regex patternSpecial = new Regex(link.getPluginPatternMatcher(), "https://[^/]+/f/([a-z0-9]{12})");
        if (patternSpecial.patternFind()) {
            return patternSpecial.getMatch(0);
        } else {
            return super.getFUIDFromURL(link);
        }
    }

    @Override
    protected URL_TYPE getURLType(final String url) {
        if (url == null) {
            return null;
        }
        if (url.matches("(?i)^https?://[^/]+/f/([a-z0-9]{12}).*")) {
            return URL_TYPE.OFFICIAL_VIDEO_DOWNLOAD;
        } else {
            return super.getURLType(url);
        }
    }

    @Override
    protected String buildURLPath(final DownloadLink link, final String fuid, final URL_TYPE type) {
        if (type == null || type == URL_TYPE.OFFICIAL_VIDEO_DOWNLOAD) {
            /* 2023-09-07: Special: They do not have working "/d/..." links anymore but users are still spreading them. */
            return buildNormalURLPath(link, fuid);
        } else {
            return super.buildURLPath(link, fuid, type);
        }
    }

    @Override
    protected boolean supportsShortURLs() {
        return false;
    }

    @Override
    protected boolean supports_availablecheck_alt() {
        return false;
    }

    @Override
    protected boolean supports_availablecheck_filename_abuse() {
        return false;
    }

    @Override
    protected boolean isOffline(final DownloadLink link, final Browser br) {
        if (br.containsHTML("<div>\\s*This video has been locked watch or does not exist")) {
            return true;
        } else {
            return super.isOffline(link, br);
        }
    }

    @Override
    protected void checkErrors(final Browser br, final String html, final DownloadLink link, final Account account, final boolean checkAll) throws NumberFormatException, PluginException {
        if (br.containsHTML(">\\s*Video temporarily not available")) {
            throw new PluginException(LinkStatus.ERROR_HOSTER_TEMPORARILY_UNAVAILABLE, "Video temporarily not available");
        }
        super.checkErrors(br, html, link, account, checkAll);
        final String errorsMisc = br.getRegex("class=\"icon icon-info icon-size-16 me-3\"[^>]*></i>\\s*<div>([^<]+)</div>").getMatch(0);
        if (errorsMisc != null) {
            throw new PluginException(LinkStatus.ERROR_FATAL, Encoding.htmlDecode(errorsMisc).trim());
        }
    }

    @Override
    protected boolean isVideohoster_enforce_video_filename() {
        return true;
    }
}