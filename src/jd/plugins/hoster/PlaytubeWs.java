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
import jd.parser.Regex;
import jd.plugins.Account;
import jd.plugins.Account.AccountType;
import jd.plugins.DownloadLink;
import jd.plugins.HostPlugin;

@HostPlugin(revision = "$Revision$", interfaceVersion = 3, names = {}, urls = {})
public class PlaytubeWs extends XFileSharingProBasic {
    public PlaytubeWs(final PluginWrapper wrapper) {
        super(wrapper);
        this.enablePremium(super.getPurchasePremiumURL());
    }

    /**
     * DEV NOTES XfileSharingProBasic Version SEE SUPER-CLASS<br />
     * mods: See overridden functions<br />
     * limit-info: 2020-11-10: No limits at all <br />
     * captchatype-info: 2020-07-02: null<br />
     * other:<br />
     */
    public static List<String[]> getPluginDomains() {
        final List<String[]> ret = new ArrayList<String[]>();
        // each entry in List<String[]> will result in one PluginForHost, Plugin.getHost() will return String[0]->main domain
        ret.add(new String[] { "playtube.ws" });
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
    protected boolean isOffline(final DownloadLink link) {
        boolean offline = super.isOffline(link);
        if (!offline) {
            /* 2020-10-26: Special */
            offline = new Regex(correctedBR, ">\\s*This video was deleted by|>\\s*File was locked by Copyright Agent").matches();
        }
        return offline;
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
    protected boolean isVideohosterEmbed() {
        /* 2020-10-26 */
        return true;
    }

    @Override
    protected boolean isVideohoster_enforce_video_filename() {
        /* 2020-10-26 */
        return true;
    }

    @Override
    protected boolean supports_availablecheck_alt() {
        /* 2020-10-26 */
        return false;
    }

    @Override
    protected boolean supports_availablecheck_filename_abuse() {
        /* 2020-10-26 */
        return false;
    }

    @Override
    protected boolean useRUA() {
        /* 2020-10-26 */
        return true;
    }

    @Override
    public String[] scanInfo(final String[] fileInfo) {
        /* 2020-10-26: Special */
        if (StringUtils.isEmpty(fileInfo[0])) {
            /* 2019-06-12: TODO: Update this RegEx for e.g. up-4ever.org */
            fileInfo[0] = new Regex(correctedBR, "class=\"top\"><div class=\"title\">([^<>\"]+)<").getMatch(0);
        }
        return fileInfo;
    }

    @Override
    protected String regexWaittime() {
        /* 2020-11-10: For officialVideoDownload */
        String waitStr = super.regexWaittime();
        if (waitStr == null) {
            waitStr = new Regex(correctedBR, ">Please wait <span id=\"timer\">(\\d+)<").getMatch(0);
        }
        return waitStr;
    }

    @Override
    protected String getDllink(final DownloadLink link, final Account account, final Browser br, String src) {
        /* 2020-11-10: For officialVideoDownload */
        String dllink = super.getDllink(link, account, br, src);
        if (StringUtils.isEmpty(dllink)) {
            dllink = new Regex(src, "class=\"button-dl\"[^>]*onclick=\"location\\.href='(https?://[^<>\"\\']+)").getMatch(0);
        }
        return dllink;
    }
}