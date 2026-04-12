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
import java.util.Map;

import org.appwork.utils.Regex;
import org.appwork.utils.StringUtils;
import org.jdownloader.plugins.components.XFileSharingProBasic;

import jd.PluginWrapper;
import jd.http.Browser;
import jd.plugins.Account;
import jd.plugins.Account.AccountType;
import jd.plugins.DownloadLink;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;

@HostPlugin(revision = "$Revision$", interfaceVersion = 3, names = {}, urls = {})
public class DarkiboxCom extends XFileSharingProBasic {
    public DarkiboxCom(final PluginWrapper wrapper) {
        super(wrapper);
        this.enablePremium(super.getPurchasePremiumURL());
    }

    /**
     * DEV NOTES XfileSharingProBasic Version SEE SUPER-CLASS<br />
     * mods: See overridden functions<br />
     * limit-info: <br />
     * captchatype-info: 2023-10-06: reCaptchaV2<br />
     * other: 2026-04-12: Switched from website mode to API mode<br />
     */
    public static List<String[]> getPluginDomains() {
        final List<String[]> ret = new ArrayList<String[]>();
        // each entry in List<String[]> will result in one PluginForHost, Plugin.getHost() will return String[0]->main domain
        ret.add(new String[] { "darkibox.com" });
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

    /** 2026-04-12: Use API key based login instead of website login. API docs: https://darkibox.com/api.html */
    @Override
    protected boolean enableAccountApiOnlyMode() {
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

    /**
     * 2026-04-12: Override needed because darkibox API returns direct links in a "versions" array format:
     * {"result":{"versions":[{"name":"o","url":"..."},{"name":"h","url":"..."}]}} instead of the standard XFS format:
     * {"result":{"o":{"url":"..."},"h":{"url":"..."}}}
     */
    @Override
    protected String getDllinkAPI(final DownloadLink link, final Account account) throws Exception {
        logger.info("Trying to get dllink via API");
        final String apikey = getAPIKeyFromAccount(account);
        if (StringUtils.isEmpty(apikey)) {
            logger.warning("Cannot do this without apikey");
            return null;
        }
        final String fileid = this.getFUIDFromURL(link);
        getPage(this.getAPIBase() + "/file/direct_link?key=" + apikey + "&file_code=" + fileid);
        final Map<String, Object> entries = this.checkErrorsAPI(this.br, link, account);
        final Map<String, Object> result = (Map<String, Object>) entries.get("result");
        String dllink = null;
        /* Darkibox returns qualities in a "versions" array */
        final List<Map<String, Object>> versions = (List<Map<String, Object>>) result.get("versions");
        if (versions != null && !versions.isEmpty()) {
            final String[] preferredQualities = new String[] { "o", "h", "n", "l" };
            for (final String quality : preferredQualities) {
                for (final Map<String, Object> version : versions) {
                    final String name = (String) version.get("name");
                    if (StringUtils.equalsIgnoreCase(name, quality)) {
                        dllink = (String) version.get("url");
                        if (!StringUtils.isEmpty(dllink)) {
                            break;
                        }
                    }
                }
                if (!StringUtils.isEmpty(dllink)) {
                    break;
                }
            }
            /* Fallback: pick the first available version */
            if (StringUtils.isEmpty(dllink)) {
                dllink = (String) versions.get(0).get("url");
            }
        }
        if (StringUtils.isEmpty(dllink)) {
            /* Fallback: try standard XFS format */
            return super.getDllinkAPI(link, account);
        }
        logger.info("Successfully found dllink via API: " + dllink);
        return dllink;
    }

    @Override
    protected boolean supportsShortURLs() {
        return false;
    }

    @Override
    public String[] scanInfo(final String html, final String[] fileInfo) {
        super.scanInfo(html, fileInfo);
        /* 2023-10-06: For "/d/..." links. */
        final String betterFilename = new Regex(html, "(?i)<h3 [^>]*>\\s*Download\\s*([^<]*?)\\s*</h\\d+>").getMatch(0);
        if (betterFilename != null) {
            fileInfo[0] = betterFilename;
        }
        return fileInfo;
    }

    @Override
    protected void checkErrors(final Browser br, final String html, final DownloadLink link, final Account account, final boolean checkAll) throws NumberFormatException, PluginException {
        super.checkErrors(br, html, link, account, checkAll);
        if (br.containsHTML(">\\s*You are not able to download Files")) {
            throw new PluginException(LinkStatus.ERROR_FATAL, "Website error 'You are not able to download Files'");
        }
    }

    @Override
    protected boolean isVideohoster_enforce_video_filename() {
        return true;
    }

    @Override
    protected String regexWaittime(final String html) {
        final String waitSeconds = new Regex(html, ">(\\d+)</span>\\s*seconds\\s*</span>").getMatch(0);
        if (waitSeconds != null) {
            return waitSeconds;
        } else {
            return super.regexWaittime(html);
        }
    }
}
