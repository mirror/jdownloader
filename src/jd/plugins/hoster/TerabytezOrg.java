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

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

import org.jdownloader.plugins.components.XFileSharingProBasic;

import jd.PluginWrapper;
import jd.http.Browser;
import jd.parser.Regex;
import jd.plugins.Account;
import jd.plugins.Account.AccountType;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.PluginException;

@HostPlugin(revision = "$Revision$", interfaceVersion = 3, names = {}, urls = {})
public class TerabytezOrg extends XFileSharingProBasic {
    public TerabytezOrg(final PluginWrapper wrapper) {
        super(wrapper);
        this.enablePremium(super.getPurchasePremiumURL());
    }

    /**
     * DEV NOTES XfileSharingProBasic Version SEE SUPER-CLASS<br />
     * mods: See overridden functions<br />
     * limit-info:<br />
     * captchatype-info: 2023-12-04: null <br />
     * other:<br />
     */
    public static List<String[]> getPluginDomains() {
        final List<String[]> ret = new ArrayList<String[]>();
        // each entry in List<String[]> will result in one PluginForHost, Plugin.getHost() will return String[0]->main domain
        ret.add(new String[] { "terabytez.org" });
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

    /** Pattern of their old YetiShare links. */
    final Pattern PATTERN_OLD = Pattern.compile("(?i)https?://[^/]+/([a-f0-9]{16})(/([^/]+))?");

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
            return 1;
        } else if (AccountType.PREMIUM.equals(type) || AccountType.LIFETIME.equals(type)) {
            /* Premium account */
            return 1;
        } else {
            /* Free(anonymous) and unknown account type */
            return 1;
        }
    }

    @Override
    public int getMaxSimultaneousFreeAnonymousDownloads() {
        return 1;
    }

    @Override
    public int getMaxSimultaneousFreeAccountDownloads() {
        return 1;
    }

    @Override
    public int getMaxSimultanPremiumDownloadNum() {
        return -1;
    }

    @Override
    public String[] scanInfo(final String html, final String[] fileInfo) {
        /* 2023-12-04 */
        super.scanInfo(html, fileInfo);
        final String betterFilename = new Regex(html, "class=\"name\"[^>]*>\\s*<h4>([^<]+)</h4>").getMatch(0);
        if (betterFilename != null) {
            fileInfo[0] = betterFilename;
        }
        return fileInfo;
    }

    @Override
    public String regexFilenameAbuse(final Browser br) {
        /* 2023-12-04 */
        final String betterFilename = br.getRegex("Filename\\s*</label>([^<]+)</div>").getMatch(0);
        if (betterFilename != null) {
            return betterFilename;
        } else {
            return super.regexFilenameAbuse(br);
        }
    }

    private final String PROPERTY_XFS_FUID = "xfs_fuid";

    @Override
    public AvailableStatus requestFileInformationWebsite(final DownloadLink link, final Account account, final boolean isDownload) throws Exception {
        final AvailableStatus status = super.requestFileInformationWebsite(link, account, isDownload);
        specialLegacyHandlingYetiToXFS(br, link);
        return status;
    }

    @Override
    protected String getFnameViaAbuseLink(final Browser br, final DownloadLink link) throws Exception {
        /* Small workaround */
        specialLegacyHandlingYetiToXFS(br, link);
        return super.getFnameViaAbuseLink(br, link);
    }

    private void specialLegacyHandlingYetiToXFS(final Browser br, final DownloadLink link) {
        if (!link.hasProperty(PROPERTY_XFS_FUID) && new Regex(link.getPluginPatternMatcher(), PATTERN_OLD).patternFind()) {
            final String xfs_fuid = new Regex(br.getURL(), "(?:/|=)([a-z0-9]{12})$").getMatch(0);
            if (xfs_fuid != null) {
                link.setProperty(PROPERTY_XFS_FUID, xfs_fuid);
            }
        }
    }

    @Override
    public AvailableStatus requestFileInformationWebsiteMassLinkcheckerSingle(final DownloadLink link) throws IOException, PluginException {
        final String xfs_fuid = link.getStringProperty(PROPERTY_XFS_FUID);
        if (new Regex(link.getPluginPatternMatcher(), PATTERN_OLD).patternFind() && xfs_fuid == null) {
            /* Mass-linkchecker can't be used for older links && when new XFS unique fileID is not known. */
            return AvailableStatus.UNCHECKED;
        } else {
            return super.requestFileInformationWebsiteMassLinkcheckerSingle(link);
        }
    }

    @Override
    protected String getFUID(final String url, URL_TYPE type) {
        final Regex type_old = new Regex(url, PATTERN_OLD);
        if (type_old.patternFind()) {
            return type_old.getMatch(0);
        } else {
            return super.getFUID(url, type);
        }
    }

    @Override
    public String getFUIDFromURL(final DownloadLink link) {
        final String xfs_fuid = link.getStringProperty(PROPERTY_XFS_FUID);
        if (xfs_fuid != null) {
            return xfs_fuid;
        } else {
            final URL_TYPE type = getURLType(link);
            return getFUID(link, type);
        }
    }

    @Override
    public boolean isPremiumOnly(final Browser br) {
        if (br.getURL().matches("(?i).*/login\\?redirect=.*")) {
            return true;
        } else {
            return super.isPremiumOnly(br);
        }
    }
}