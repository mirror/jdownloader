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
import org.appwork.utils.formatter.SizeFormatter;
import org.jdownloader.plugins.components.XFileSharingProBasic;
import org.jdownloader.plugins.components.config.XFSConfigDropapk;
import org.jdownloader.plugins.config.PluginJsonConfig;

import jd.PluginWrapper;
import jd.http.Browser;
import jd.parser.Regex;
import jd.plugins.Account;
import jd.plugins.Account.AccountType;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;

@HostPlugin(revision = "$Revision$", interfaceVersion = 3, names = {}, urls = {})
public class DropapkCom extends XFileSharingProBasic {
    public DropapkCom(final PluginWrapper wrapper) {
        super(wrapper);
        this.enablePremium(super.getPurchasePremiumURL());
    }

    /**
     * DEV NOTES XfileSharingProBasic Version SEE SUPER-CLASS<br />
     * mods: See overridden functions<br />
     * limit-info: 2019-07-08: Premium untested, set FREE account limits <br />
     * captchatype-info: 2019-07-08: reCaptchaV2<br />
     * other:<br />
     */
    public static List<String[]> getPluginDomains() {
        final List<String[]> ret = new ArrayList<String[]>();
        // each entry in List<String[]> will result in one PluginForHost, Plugin.getHost() will return String[0]->main domain
        ret.add(new String[] { "drop.download", "dropapk.to", "dropapk.com", "fastclick.to" });
        return ret;
    }

    public static String[] getAnnotationNames() {
        return buildAnnotationNames(getPluginDomains());
    }

    @Override
    public String rewriteHost(final String host) {
        return rewriteHost(getPluginDomains(), host);
    }

    @Override
    public String[] scanInfo(String[] fileInfo) {
        String[] ret = super.scanInfo(fileInfo);
        if (StringUtils.isEmpty(ret[1])) {
            ret[1] = new Regex(correctedBR, "\\(\\s*(\\d+(?:\\.\\d+)?(?: |\\&nbsp;)?(KB|MB|GB|B))").getMatch(0);
        }
        return ret;
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
            return -4;
        } else if (account != null && account.getType() == AccountType.PREMIUM) {
            /* Premium account */
            return -4;
        } else {
            /* Free(anonymous) and unknown account type */
            return -4;
        }
    }

    @Override
    protected boolean supports_availablecheck_filesize_html() {
        /* 2019-07-08: Special */
        return false;
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
    protected AvailableStatus massLinkcheckerParseFileInfo(final Browser br, final DownloadLink dl) {
        final String fuid = this.getFUIDFromURL(dl);
        /* 2021-05-17: Special */
        final String html_for_fuid = br.getRegex("<li[^>]*>\\s*<span>[^>]*" + fuid + "[^>]*</span>.*?</li>").getMatch(-1);
        if (html_for_fuid == null) {
            return AvailableStatus.UNCHECKED;
        }
        if (html_for_fuid.contains("Not found")) {
            dl.setAvailable(false);
            return AvailableStatus.FALSE;
        } else {
            /* We know that the file is online - let's try to find the filesize ... */
            dl.setAvailable(true);
            try {
                final String size = new Regex(html_for_fuid, "<strong class=\"ml-2\">\\s*(\\d+(\\.\\d+)? [^<]+)</strong>\\s*</li").getMatch(0);
                if (size != null) {
                    /*
                     * Filesize should definitly be given - but at this stage we are quite sure that the file is online so let's not throw a
                     * fatal error if the filesize cannot be found.
                     */
                    dl.setDownloadSize(SizeFormatter.getSize(size));
                }
            } catch (final Throwable e) {
                logger.log(e);
            }
            return AvailableStatus.TRUE;
        }
    }

    @Override
    protected boolean supportsMassLinkcheckOverWebsite() {
        /* 2021-05-20: Try to prevent their antiddos ddos-guard.net from kicking in: https://svn.jdownloader.org/issues/87111 */
        return PluginJsonConfig.get(XFSConfigDropapk.class).isWebsiteAllowMassLinkcheck();
    }

    @Override
    protected boolean supportsAPIMassLinkcheck() {
        return isAPIKey(this.getAPIKey());
    }

    @Override
    protected boolean supportsAPISingleLinkcheck() {
        return isAPIKey(this.getAPIKey());
    }

    @Override
    public Class<? extends XFSConfigDropapk> getConfigInterface() {
        return XFSConfigDropapk.class;
    }

    @Override
    protected String regExTrafficLeft(final Browser br) {
        /* 2023-03-02 */
        final String betterResult = br.getRegex("(?i)>\\s*Traffic available today</div>\\s*<div[^>]*>([^<>\"]+)</div>").getMatch(0);
        if (betterResult != null) {
            return betterResult;
        } else {
            return super.regExTrafficLeft(br);
        }
    }
}