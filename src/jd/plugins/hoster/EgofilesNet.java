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
import jd.plugins.AccountUnavailableException;
import jd.plugins.DownloadLink;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = {}, urls = {})
public class EgofilesNet extends YetiShareCore {
    public EgofilesNet(PluginWrapper wrapper) {
        super(wrapper);
        this.enablePremium(getPurchasePremiumURL());
    }

    /**
     * DEV NOTES YetiShare<br />
     ****************************
     * mods: See overridden functions<br />
     * limit-info:<br />
     * captchatype-info: null solvemedia reCaptchaV2, hcaptcha<br />
     * other: <br />
     */
    public static List<String[]> getPluginDomains() {
        final List<String[]> ret = new ArrayList<String[]>();
        // each entry in List<String[]> will result in one PluginForHost, Plugin.getHost() will return String[0]->main domain
        ret.add(new String[] { "egofiles.net" });
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
            return 1;
        } else if (account != null && account.getType() == AccountType.PREMIUM) {
            /* Premium account */
            return -4;
        } else {
            /* Free(anonymous) and unknown account type */
            return 1;
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
    public boolean requiresWWW() {
        return false;
    }

    @Override
    protected boolean isOfflineWebsiteInfoPage(final Browser br, final DownloadLink link) throws Exception {
        if (br.containsHTML("(?i)>\\s*File not found")) {
            /* E.g. https://egofiles.net/yyyyyyyxxxxxxxx~i */
            return true;
        } else {
            return super.isOfflineWebsiteInfoPage(br, link);
        }
    }

    @Override
    public String[] scanInfo(final DownloadLink link, final String[] fileInfo) {
        super.scanInfo(link, fileInfo);
        /* 2023-03-27 */
        final String betterFilesize = br.getRegex(">\\s*Filesize\\s*</p>\\s*<h5 class=\"text-white\">([^<]+)").getMatch(0);
        if (betterFilesize != null) {
            fileInfo[1] = betterFilesize;
        }
        return fileInfo;
    }

    @Override
    protected AccountInfo fetchAccountInfoWebsite(final Account account) throws Exception {
        final AccountInfo ai = new AccountInfo();
        loginWebsite(account, true);
        if (br.getURL() == null || !br.getURL().contains(this.getAccountNameSpaceUpgrade(account))) {
            getPage(this.getAccountNameSpaceUpgrade(account));
        }
        if (!isPremiumAccount(account, this.br)) {
            logger.info("Looks like we have a free account");
            setAccountLimitsByType(account, AccountType.FREE);
        } else {
            /**
             * Heavily modified YetiShare script: </br>
             * Accounts can have multiple packages at the same time --> See possible packages here: https://egofiles.net/upgrade
             */
            final String premiumAccountPackagesText = br.getRegex(">\\s*(?:Wygasa|Reverts To Free Account)\\s*</div>\\s*<div[^>]*>(.*?)</div>").getMatch(0);
            if (premiumAccountPackagesText != null) {
                logger.info("Found premium packages text");
                final String[] premiumPackages = premiumAccountPackagesText.split("<br>");
                long summedTrafficLeft = 0;
                long highestTrafficMax = 0;
                long highestExpireTimestamp = 0;
                boolean hasPackageWithoutTrafficLimit = false;
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
                    /* Example: Premium bez limitów: 18/11/2020 19:31:49 */
                    if (premiumPackage.contains("Premium bez limitów")) {
                        /* Package with unlimited traffic --> Usually also has expire-date */
                        logger.info("Found package with unlimited traffic");
                        hasPackageWithoutTrafficLimit = true;
                    }
                    if (dailyTrafficLeftStr != null && dailyTrafficMaxStr != null) {
                        /* Daily traffic package ... usually also comes with an expire-date */
                        trafficLeftTmp = SizeFormatter.getSize(dailyTrafficLeftStr);
                        trafficMaxTmp = SizeFormatter.getSize(dailyTrafficMaxStr);
                        summedTrafficLeft += trafficLeftTmp;
                        if (trafficMaxTmp > highestTrafficMax) {
                            highestTrafficMax = trafficMaxTmp;
                        }
                    } else if (trafficStr != null) {
                        /* 2020-10-15: Hmm traffic package ... but wes have no idea how much traffic of that package is left?! */
                        trafficLeftTmp = SizeFormatter.getSize(trafficStr);
                        summedTrafficLeft += trafficLeftTmp;
                    } else {
                        logger.warning("WTF cannot parse package: " + premiumPackage);
                    }
                    /* User can have multiple packages. User can e.g. have daily traffic, extra traffic and expire date. */
                    if (packageExpireStr != null) {
                        logger.info("Found package with expiredate: " + packageExpireStr);
                        long packageExpire = parseExpireTimeStamp(account, packageExpireStr);
                        if (packageExpire > highestExpireTimestamp) {
                            highestExpireTimestamp = packageExpire;
                        }
                    }
                }
                if (summedTrafficLeft > 0) {
                    ai.setTrafficLeft(summedTrafficLeft);
                    if (hasPackageWithoutTrafficLimit) {
                        logger.info("Enabling special traffic so account is allowed to download more than the traffic that's displayed in UI");
                        ai.setSpecialTraffic(true);
                    } else {
                        /* Make sure to reset this if e.g. user had traffic with unlimited traffic but it expired */
                        ai.setSpecialTraffic(false);
                    }
                } else {
                    logger.info("Failed to find traffic --> Assuming user has unlimited traffic");
                    ai.setUnlimitedTraffic();
                }
                if (highestTrafficMax > summedTrafficLeft) {
                    ai.setTrafficMax(highestTrafficMax);
                }
                if (highestExpireTimestamp > System.currentTimeMillis()) {
                    ai.setValidUntil(highestExpireTimestamp, br);
                }
                ai.setStatus(premiumPackages.length + " premium packages:\r\n" + premiumAccountPackagesText.replace("<br>", "\r\n").trim());
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
    protected boolean isPremiumAccount(final Account account, final Browser br) {
        return br.containsHTML("Typ Konta</div>\\s*<div[^>]*>\\s*Premium");
    }

    @Override
    public void checkErrors(Browser br, final DownloadLink link, final Account account) throws PluginException {
        super.checkErrors(br, link, account);
        if (br.containsHTML(">\\s*Musisz być użytkownikiem premium aby pobrać ten plik")) {
            /* 2020-10-21 */
            throw new AccountRequiredException();
        } else if (br.containsHTML(">\\s*You do not have enough transfer available to download this file")) {
            /* 2020-11-13: WTF different errormessages are in different languages */
            throw new AccountUnavailableException("Download limit reached (Failed to download file " + link.getName() + ")", 5 * 60 * 1000l);
        } else if (br.containsHTML(">\\s*Plik nie jest publicznie dostępny")) {
            throw new PluginException(LinkStatus.ERROR_FATAL, "Private file");
        }
    }
    // @Override
    // public boolean canHandle(final DownloadLink link, final Account account) throws Exception {
    // /** 2020-11-13: Seems like only premium users can download. */
    // return account != null && account.getType() == AccountType.PREMIUM;
    // }
}