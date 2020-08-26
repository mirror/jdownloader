//jDownloader - Downloadmanager
//Copyright (C) 2016  JD-Team support@jdownloader.org
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
import java.util.Locale;
import java.util.Map;

import org.appwork.storage.JSonStorage;
import org.appwork.storage.TypeRef;
import org.appwork.utils.DebugMode;
import org.appwork.utils.StringUtils;
import org.appwork.utils.formatter.SizeFormatter;
import org.appwork.utils.formatter.TimeFormatter;
import org.jdownloader.plugins.components.YetiShareCore;

import jd.PluginWrapper;
import jd.http.Browser;
import jd.plugins.Account;
import jd.plugins.Account.AccountType;
import jd.plugins.AccountInfo;
import jd.plugins.DownloadLink;
import jd.plugins.HostPlugin;

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = {}, urls = {})
public class OxycloudPl extends YetiShareCore {
    public OxycloudPl(PluginWrapper wrapper) {
        super(wrapper);
        this.enablePremium(getPurchasePremiumURL());
    }

    /**
     * DEV NOTES YetiShare<br />
     ****************************
     * mods: See overridden functions<br />
     * limit-info:<br />
     * captchatype-info: null solvemedia reCaptchaV2<br />
     * other: <br />
     */
    public static List<String[]> getPluginDomains() {
        final List<String[]> ret = new ArrayList<String[]>();
        // each entry in List<String[]> will result in one PluginForHost, Plugin.getHost() will return String[0]->main domain
        ret.add(new String[] { "oxycloud.pl" });
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
        return YetiShareCore.buildAnnotationUrls(getPluginDomains());
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

    public int getMaxChunks(final Account account) {
        if (account != null && account.getType() == AccountType.FREE) {
            /* Free Account */
            return 0;
        } else if (account != null && account.getType() == AccountType.PREMIUM) {
            /* Premium account */
            return 0;
        } else {
            /* Free(anonymous) and unknown account type */
            return 0;
        }
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return -1;
    }

    public int getMaxSimultaneousFreeAccountDownloads() {
        return -1;
    }

    @Override
    public int getMaxSimultanPremiumDownloadNum() {
        return -1;
    }

    @Override
    public String[] scanInfo(final DownloadLink link, final String[] fileInfo) {
        super.scanInfo(link, fileInfo);
        if (supports_availablecheck_over_info_page(link)) {
            /* 2020-08-14: Special */
            if (StringUtils.isEmpty(fileInfo[1])) {
                fileInfo[1] = br.getRegex("File size</th>\\s*<td>([^<>\"]+)</td>").getMatch(0);
            }
        }
        return fileInfo;
    }

    @Override
    protected AccountInfo fetchAccountInfoWebsite(final Account account) throws Exception {
        final AccountInfo ai = new AccountInfo();
        loginWebsite(account, true);
        this.getPage("/download-limits-calculator");
        final String trafficLeft = br.getRegex("class=\"fa fa-download\"></i>([^<>]*)</span>").getMatch(0);
        final String expireDate = br.getRegex("This package is active until (\\d{4}-\\d{2}-\\d{2} \\d{2}:\\d{2})").getMatch(0);
        if (trafficLeft != null && expireDate != null) {
            ai.setValidUntil(TimeFormatter.getMilliSeconds(expireDate, "yyyy-MM-dd hh:mm", Locale.ENGLISH), this.br);
            account.setType(AccountType.PREMIUM);
            account.setMaxSimultanDownloads(this.getMaxSimultanPremiumDownloadNum());
            ai.setStatus("Premium account");
            ai.setTrafficLeft(SizeFormatter.getSize(trafficLeft));
        } else {
            account.setType(AccountType.FREE);
            account.setMaxSimultanDownloads(this.getMaxSimultaneousFreeAccountDownloads());
            /* All accounts get the same (IP-based) downloadlimits --> Simultan free account usage makes no sense! */
            account.setConcurrentUsePossible(false);
            ai.setStatus("Registered (free) user");
            ai.setUnlimitedTraffic();
        }
        return ai;
    }
    /* *************************** PUT API RELATED METHODS HERE *************************** */

    @Override
    protected String getAPIBase() {
        return "https://" + this.getHost() + "/api/v1";
    }

    @Override
    protected boolean supports_api() {
        return DebugMode.TRUE_IN_IDE_ELSE_FALSE;
    }

    private void setAPIHeaders(final Browser br, final Account account) {
        br.getHeaders().put("authentication", this.getAPIAccessToken(account));
        br.getHeaders().put("account", this.getAPIAccountID(account));
    }

    @Override
    protected AccountInfo fetchAccountInfoAPI(final Account account) throws Exception {
        final AccountInfo ai = super.fetchAccountInfoAPI(account);
        /* 2020-08-26: They've built some stuff on top of the normal YetiShare API --> Handle this here */
        final Browser brc = br.cloneBrowser();
        setAPIHeaders(brc, account);
        this.getPage(brc, this.getAPIBase() + "/package/limits");
        Map<String, Object> entries = JSonStorage.restoreFromString(brc.toString(), TypeRef.HASHMAP);
        entries = (Map<String, Object>) entries.get("account");
        final String accType = (String) entries.get("type");
        final boolean isUnlimited = ((Boolean) entries.get("isUnlimited")).booleanValue();
        final boolean isPremium = ((Boolean) entries.get("isPremium")).booleanValue();
        if (isUnlimited) {
            ai.setUnlimitedTraffic();
        } else {
            /*
             * 2020-08-26: These values are usually null for free accounts but we'll try to set them anyways in case they change this in the
             * future.
             */
            try {
                final long maxDailyBytes = ((Long) entries.get("maxDailyBytes")).longValue();
                final long dailyBytesLeft = ((Long) entries.get("dailyBytesLeft")).longValue();
                ai.setTrafficMax(maxDailyBytes);
                ai.setTrafficLeft(dailyBytesLeft);
            } catch (final Throwable e) {
                /* Double-check! If the total quota is 0, there is no traffic left at all! */
                final Object totalBytesLeftO = entries.get("totalBytesLeft");
                if (totalBytesLeftO == null) {
                    ai.setTrafficLeft(0);
                } else {
                    try {
                        final long totalBytesLeft = ((Long) totalBytesLeftO).longValue();
                        ai.setTrafficLeft(totalBytesLeft);
                    } catch (final Throwable e2) {
                        ai.setTrafficLeft(0);
                    }
                }
            }
        }
        if ("paid".equalsIgnoreCase(accType) || isPremium) {
            account.setType(AccountType.PREMIUM);
        } else {
            account.setType(AccountType.FREE);
        }
        /* Do not set account status here as upper code already did that! */
        // ai.setStatus("Bla");
        return ai;
    }
}