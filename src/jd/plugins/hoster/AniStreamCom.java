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
import java.util.regex.Pattern;

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
public class AniStreamCom extends XFileSharingProBasic {
    public AniStreamCom(final PluginWrapper wrapper) {
        super(wrapper);
        /* 2020-06-16: Login not supported - not needed! Also, login does not fit template code: https://www.ani-stream.com/login */
        // this.enablePremium(super.getPurchasePremiumURL());
    }

    /**
     * DEV NOTES XfileSharingProBasic Version SEE SUPER-CLASS<br />
     * mods: See overridden functions<br />
     * limit-info: 2020-06-16: No limits at all <br />
     * captchatype-info: 2020-06-16: null<br />
     * other:<br />
     */
    public static List<String[]> getPluginDomains() {
        final List<String[]> ret = new ArrayList<String[]>();
        // each entry in List<String[]> will result in one PluginForHost, Plugin.getHost() will return String[0]->main domain
        ret.add(new String[] { "ani-stream.com" });
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

    @Override
    protected boolean isVideohosterEmbed() {
        return true;
    }

    @Override
    public String[] scanInfo(final String[] fileInfo) {
        /* 2020-06-16: Special */
        fileInfo[0] = new Regex(correctedBR, Pattern.compile(this.fuid + "\">([^<>\"]+)</a>", Pattern.CASE_INSENSITIVE)).getMatch(0);
        fileInfo[1] = new Regex(correctedBR, "\\(([0-9]+ byte)\\)").getMatch(0);
        if (StringUtils.isEmpty(fileInfo[0]) || StringUtils.isEmpty(fileInfo[1])) {
            super.scanInfo(fileInfo);
        }
        return fileInfo;
    }

    @Override
    protected boolean isOffline(final DownloadLink link) {
        boolean offline = super.isOffline(link);
        if (!offline) {
            /* 2020-08-04 */
            offline = new Regex(correctedBR, ">\\s*File deleted|>\\s*This file is not available anymore").matches();
        }
        return offline;
    }

    @Override
    protected String getDllink(final DownloadLink link, final Account account, final Browser br, String src) {
        String dllink = super.getDllink(link, account, br, src);
        if (StringUtils.isEmpty(dllink)) {
            /* 2020-08-04 */
            dllink = "/manifest-" + this.fuid + ".m3u8";
            logger.info("Fallback to static downloadurl: " + dllink);
        }
        return dllink;
    }
}