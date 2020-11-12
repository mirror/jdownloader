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

import org.appwork.utils.formatter.SizeFormatter;
import org.jdownloader.plugins.components.YetiShareCore;

import jd.PluginWrapper;
import jd.http.Browser;
import jd.parser.Regex;
import jd.plugins.Account;
import jd.plugins.Account.AccountType;
import jd.plugins.AccountInfo;
import jd.plugins.AccountRequiredException;
import jd.plugins.DownloadLink;
import jd.plugins.HostPlugin;
import jd.plugins.PluginException;

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = {}, urls = {})
public class OxycloudCom extends YetiShareCoreSpecialOxycloud {
    public OxycloudCom(PluginWrapper wrapper) {
        super(wrapper);
        this.enablePremium("https://" + this.getHost() + "/upgrade");
    }

    /**
     * DEV NOTES YetiShare<br />
     ****************************
     * mods: See overridden functions<br />
     * limit-info:<br />
     * captchatype-info: 2020-10-12: null<br />
     * other: <br />
     */
    public static List<String[]> getPluginDomains() {
        final List<String[]> ret = new ArrayList<String[]>();
        // each entry in List<String[]> will result in one PluginForHost, Plugin.getHost() will return String[0]->main domain
        ret.add(new String[] { "oxycloud.com", "beta.oxycloud.com" });
        return ret;
    }

    @Override
    public String rewriteHost(final String host) {
        return this.rewriteHost(getPluginDomains(), host, new String[0]);
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
    public boolean requires_WWW() {
        /* 2020-10-12: Disabled because this website is currently running ob subdomain "beta.". */
        return false;
    }

    @Override
    protected AccountInfo fetchAccountInfoWebsite(final Account account) throws Exception {
        final AccountInfo ai = new AccountInfo();
        loginWebsite(account, true);
        if (br.getURL() == null || !br.getURL().contains("/upgrade")) {
            getPage("/upgrade");
        }
        if (!isPremiumAccount(br)) {
            logger.info("Looks like we have a free account");
            setAccountLimitsByType(account, AccountType.FREE);
        } else {
            /* Accounts can have multiple packages at the same time --> See possible packages here: https://oxycloud.com/upgrade */
            final String premiumAccountPackagesText = br.getRegex("<td class=\"text-right\"><strong>Reverts To Free Account</strong></td>\\s*<td>(.*?)</td>").getMatch(0);
            if (premiumAccountPackagesText != null) {
                final String[] premiumPackages = premiumAccountPackagesText.split("<br>");
                long summedTrafficLeft = 0;
                long highestTrafficMax = 0;
                long highestExpireTimestamp = 0;
                for (final String premiumPackage : premiumPackages) {
                    /* E.g. Transfer odnawialny 1 miesiąc: 50 GB/50 GB - 31/12/2020 00:00:00 */
                    final Regex dailyTrafficRegex = new Regex(premiumPackage, "(\\d+(?:.\\d{2})? [A-Za-z]+)/(\\d+(?:\\.\\d{2})? [A-Za-z]+)");
                    final String dailyTrafficLeftStr = dailyTrafficRegex.getMatch(0);
                    final String dailyTrafficMaxStr = dailyTrafficRegex.getMatch(1);
                    long trafficLeftTmp;
                    long trafficMaxTmp;
                    String packageExpireStr = new Regex(premiumPackage, "(\\d{2}/\\d{2}/\\d{4} \\d{2}:\\d{2}:\\d{2})").getMatch(0);
                    // final String expireStr = br.getRegex("Period premium\\s*:\\s*(\\d{2}/\\d{2}/\\d{4} \\d{2}:\\d{2}:\\d{2})")
                    // .getMatch(0); /* Fits for: /account/edit */
                    final String trafficStr = br.getRegex("Transfer package\\s*:\\s*(\\d+[^<>\"]+)<").getMatch(0);
                    if (dailyTrafficLeftStr != null && dailyTrafficMaxStr != null) {
                        /* Daily traffic package ... usually also comes with an expire-date */
                        trafficLeftTmp = SizeFormatter.getSize(dailyTrafficLeftStr);
                        trafficMaxTmp = SizeFormatter.getSize(dailyTrafficMaxStr);
                        summedTrafficLeft += trafficLeftTmp;
                        if (trafficMaxTmp > highestTrafficMax) {
                            highestTrafficMax = trafficMaxTmp;
                        }
                    } else if (trafficStr != null) {
                        /* 2020-10-15: Hmm traffic package ... but we have no idea how much traffic of that package is left?! */
                        trafficLeftTmp = SizeFormatter.getSize(trafficStr);
                        summedTrafficLeft += trafficLeftTmp;
                    } else {
                        logger.warning("WTF cannot parse package: " + premiumPackage);
                    }
                    /* User can have multiple packages. User can e.g. have daily traffic, extra traffic and expire date. */
                    if (packageExpireStr != null) {
                        logger.info("Found premium expiredate");
                        long packageExpire = parseExpireTimeStamp(account, packageExpireStr);
                        if (packageExpire > highestExpireTimestamp) {
                            highestExpireTimestamp = packageExpire;
                        }
                    }
                }
                if (summedTrafficLeft > 0) {
                    ai.setTrafficLeft(summedTrafficLeft);
                }
                if (highestTrafficMax > summedTrafficLeft) {
                    ai.setTrafficMax(highestTrafficMax);
                }
                if (highestExpireTimestamp > System.currentTimeMillis()) {
                    ai.setValidUntil(highestExpireTimestamp, br);
                }
                ai.setStatus(premiumPackages.length + " premium packages:\r\n" + premiumAccountPackagesText.replace("<br>", "\r\n"));
            } else {
                logger.info("WTF unknown premium account type??");
                setAccountLimitsByType(account, AccountType.PREMIUM);
                ai.setUnlimitedTraffic();
                ai.setStatus("Premium time with unknown limits (possible JD plugin failure)");
            }
        }
        return ai;
    }

    @Override
    protected boolean isPremiumAccount(final Browser br) {
        return br.containsHTML("Typ Konta</strong></td>\\s*<td>Premium</td>");
    }

    @Override
    public void checkErrors(final DownloadLink link, final Account account) throws PluginException {
        super.checkErrors(link, account);
        if (br.containsHTML(">\\s*Musisz być użytkownikiem premium aby pobrać ten plik")) {
            /* 2020-10-21 */
            throw new AccountRequiredException();
        }
    }
}