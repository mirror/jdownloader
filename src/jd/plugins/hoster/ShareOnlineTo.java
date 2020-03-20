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

import org.appwork.utils.DebugMode;
import org.appwork.utils.formatter.SizeFormatter;
import org.jdownloader.plugins.components.XFileSharingProBasic;

import jd.PluginWrapper;
import jd.http.Browser;
import jd.parser.Regex;
import jd.plugins.Account;
import jd.plugins.Account.AccountType;
import jd.plugins.AccountInfo;
import jd.plugins.DownloadLink;
import jd.plugins.HostPlugin;
import jd.plugins.components.PluginJSonUtils;

@HostPlugin(revision = "$Revision$", interfaceVersion = 3, names = {}, urls = {})
public class ShareOnlineTo extends XFileSharingProBasic {
    public ShareOnlineTo(final PluginWrapper wrapper) {
        super(wrapper);
        this.enablePremium(super.getPurchasePremiumURL());
    }

    /**
     * DEV NOTES XfileSharingProBasic Version SEE SUPER-CLASS<br />
     * mods: See overridden functions<br />
     * limit-info:<br />
     * captchatype-info: 2020-03-19: reCaptchaV2<br />
     * other:<br />
     */
    public static List<String[]> getPluginDomains() {
        final List<String[]> ret = new ArrayList<String[]>();
        // each entry in List<String[]> will result in one PluginForHost, Plugin.getHost() will return String[0]->main domain
        ret.add(new String[] { "share-online.to" });
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
            return 1;
        } else if (account != null && account.getType() == AccountType.PREMIUM) {
            /* Premium account */
            return 1;
        } else {
            /* Free(anonymous) and unknown account type */
            return 1;
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
        return 10;
    }

    @Override
    public String getLoginURL() {
        return getMainPage() + "/login";
    }

    @Override
    protected String regexAPIKey(final String src) {
        return new Regex(src, "([a-z0-9]+)<br>\\s*<a href=\"\\?op=my_account").getMatch(0);
    }

    @Override
    protected boolean allow_api_availablecheck_in_premium_mode_if_apikey_is_available(final Account account) {
        final boolean apikey_is_available = this.getAPIKey(account) != null;
        /* Enable this switch to be able to use this in dev mode. Default = off as we do not use the API by default! */
        final boolean allow_api_premium_download = true;
        return DebugMode.TRUE_IN_IDE_ELSE_FALSE && apikey_is_available && allow_api_premium_download;
    }

    @Override
    protected AccountInfo fetchAccountInfoAPI(final Browser br, final Account account, final boolean setAndAnonymizeUsername) throws Exception {
        final Browser brc = br.cloneBrowser();
        final AccountInfo ai = super.fetchAccountInfoAPI(brc, account, setAndAnonymizeUsername);
        /* Original XFS API ('API Mod') does not return trafficleft but theirs is modified and more useful! */
        /* 2019-11-27: Not sure but this must be the traffic you can buy via 'extend traffic': https://ddl.to/?op=payments */
        final String premium_extra_trafficStr = PluginJSonUtils.getJson(brc, "premium_traffic_left");
        final String trafficleftStr = PluginJSonUtils.getJson(brc, "traffic_left");
        // final String trafficusedStr = PluginJSonUtils.getJson(brc, "traffic_used");
        if (account.getType() != null && account.getType() == AccountType.PREMIUM && trafficleftStr != null && trafficleftStr.matches("\\d+")) {
            long traffic_left = Long.parseLong(trafficleftStr) * 1000 * 1000;
            if (premium_extra_trafficStr != null && premium_extra_trafficStr.matches("\\d+")) {
                final long premium_extra_traffic = Long.parseLong(premium_extra_trafficStr) * 1000 * 1000;
                traffic_left += premium_extra_traffic;
                if (premium_extra_traffic > 0) {
                    if (ai.getStatus() != null) {
                        ai.setStatus(ai.getStatus() + " | Extra traffic available: " + SizeFormatter.formatBytes(premium_extra_traffic));
                    } else {
                        ai.setStatus("Premium account | Extra traffic available: " + SizeFormatter.formatBytes(premium_extra_traffic));
                    }
                }
            }
            ai.setTrafficLeft(traffic_left);
        } else {
            /*
             * They will return "traffic_left":"0" for free accounts which is wrong. It is unlimited on their website. By setting it to
             * unlimited here it will be re-checked via website by our XFS template!
             */
            ai.setUnlimitedTraffic();
        }
        return ai;
    }
}