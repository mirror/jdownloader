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

import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import org.appwork.utils.formatter.TimeFormatter;
import org.jdownloader.plugins.components.YetiShareCore;

import jd.PluginWrapper;
import jd.parser.Regex;
import jd.plugins.Account;
import jd.plugins.Account.AccountType;
import jd.plugins.AccountInfo;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = {}, urls = {})
public class BrfilesCom extends YetiShareCore {
    public BrfilesCom(PluginWrapper wrapper) {
        super(wrapper);
        this.enablePremium(getPurchasePremiumURL());
    }

    /**
     * DEV NOTES YetiShare<br />
     ****************************
     * mods: See overridden functions<br />
     * limit-info:<br />
     * captchatype-info: 2020-03-16: reCaptchaV2<br />
     * other: They are probably GEO-blocking everything except brazilian IPs --> All files will appear as offline then, only self uploaded
     * files can be downloaded using your own account then! Linkcheck is disabled to prevent bad offline message! <br />
     */
    public static List<String[]> getPluginDomains() {
        final List<String[]> ret = new ArrayList<String[]>();
        // each entry in List<String[]> will result in one PluginForHost, Plugin.getHost() will return String[0]->main domain
        ret.add(new String[] { "brfiles.com" });
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
        return buildAnnotationUrls(getPluginDomains());
    }

    public static String[] buildAnnotationUrls(final List<String[]> pluginDomains) {
        /* 2020-03-10: Special */
        final List<String> ret = new ArrayList<String>();
        for (final String[] domains : pluginDomains) {
            ret.add("https?://(?:www\\.)?" + YetiShareCore.buildHostsPatternPart(domains) + "/f/([A-Za-z0-9]+)(?:/[^/]+)?");
        }
        return ret.toArray(new String[0]);
    }

    @Override
    public String getFUIDFromURL(final String url) {
        /* 2020-03-10: Special */
        try {
            final String result = new Regex(new URL(url).getPath(), "^/f/([A-Za-z0-9]+)").getMatch(0);
            return result;
        } catch (MalformedURLException e) {
            e.printStackTrace();
        }
        return null;
    }

    @Override
    public void correctDownloadLink(final DownloadLink link) {
        /* 2020-03-10: Special */
        final String fid = getFUID(link);
        final String protocol;
        if (supports_https()) {
            protocol = "https";
        } else {
            protocol = "http";
        }
        link.setPluginPatternMatcher(String.format("%s://%s/f/%s", protocol, this.getHost(), fid));
        link.setLinkID(this.getHost() + "://" + fid);
    }

    @Override
    public boolean isResumeable(final DownloadLink link, final Account account) {
        if (account != null && account.getType() == AccountType.FREE) {
            /* Free Account */
            return false;
        } else if (account != null && account.getType() == AccountType.PREMIUM) {
            /* Premium account */
            return false;
        } else {
            /* Free(anonymous) and unknown account type */
            return false;
        }
    }

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
    public int getMaxSimultanFreeDownloadNum() {
        return 1;
    }

    public int getMaxSimultaneousFreeAccountDownloads() {
        return 1;
    }

    @Override
    public int getMaxSimultanPremiumDownloadNum() {
        return 1;
    }

    @Override
    public boolean supports_availablecheck_over_info_page(DownloadLink link) {
        return false;
    }

    @Override
    public boolean requires_WWW() {
        return false;
    }

    @Override
    public String[] scanInfo(final DownloadLink link, final String[] fileInfo) {
        /* 2020-03-10: Special */
        fileInfo[0] = this.br.getRegex("<title>([^<>\"]+) - BRFiles</title>").getMatch(0);
        return super.scanInfo(link, fileInfo);
    }

    @Override
    protected boolean isLoggedin() {
        return br.containsHTML("/logout/?\"");
    }

    public AvailableStatus requestFileInformation(final DownloadLink link, final Account account, final boolean isDownload) throws Exception {
        if (account == null) {
            /* 2020-03-16: Special: Without account, all files will be shown as offline by this website */
            return AvailableStatus.UNCHECKABLE;
        }
        this.loginWebsite(account, false);
        return super.requestFileInformation(link, account, isDownload);
    }

    @Override
    public void handlePremium(final DownloadLink link, final Account account) throws Exception {
        /* 2020-03-16: Special */
        if (account.getType() == AccountType.FREE) {
            /* Free account --> Login --> Availablecheck --> Download */
            loginWebsite(account, false);
            requestFileInformation(link, account, true);
            br.setFollowRedirects(false);
            if (supports_availablecheck_over_info_page(link)) {
                getPage(link.getPluginPatternMatcher());
            }
            handleDownloadWebsite(link, account);
        } else {
            /* Premium - no availablecheck at all */
            loginWebsite(account, false);
            // requestFileInformation(link, account, true);
            br.setFollowRedirects(false);
            br.getPage(link.getPluginPatternMatcher());
            final String redirect = br.getRedirectLocation();
            if (redirect != null) {
                final String fid = this.getFUID(link);
                if (!redirect.contains(fid)) {
                    throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
                }
                final String directlinkproperty = getDownloadModeDirectlinkProperty(account);
                link.setProperty(directlinkproperty, redirect);
            }
            handleDownloadWebsite(link, account);
        }
    }

    @Override
    protected AccountInfo fetchAccountInfoWebsite(final Account account) throws Exception {
        final AccountInfo ai = new AccountInfo();
        loginWebsite(account, true);
        if (br.getURL() == null || !br.getURL().contains("account_edit")) {
            getPage("/account_edit/");
        }
        boolean isPremium = br.containsHTML(">Tipo de conta\\s*:\\s*</label>.*?<label[^>]+>Premium</label>");
        if (!isPremium) {
            account.setType(AccountType.FREE);
            account.setMaxSimultanDownloads(this.getMaxSimultaneousFreeAccountDownloads());
            /* All accounts get the same (IP-based) downloadlimits --> Simultaneous free account usage makes no sense! */
            account.setConcurrentUsePossible(false);
            ai.setStatus("Registered (free) account");
        } else {
            /* If the premium account is expired we'll simply accept it as a free account. */
            String expireStr = br.getRegex("Sua conta expira em (\\d{2}/\\d{2}/\\d{4})").getMatch(0);
            if (expireStr == null) {
                if (expireStr == null) {
                    /* More wide RegEx to be more language independant (e.g. required for freefile.me) */
                    expireStr = br.getRegex("(\\d{2}/\\d{2}/\\d{4})").getMatch(0);
                }
            }
            if (expireStr == null) {
                /*
                 * 2019-03-01: As far as we know, EVERY premium account will have an expire-date given but we will still accept accounts for
                 * which we fail to find the expire-date.
                 */
                logger.info("Failed to find expire-date");
                return ai;
            }
            long expire_milliseconds = parseExpireTimeStamp(account, expireStr);
            isPremium = expire_milliseconds > System.currentTimeMillis();
            if (!isPremium) {
                /* Expired premium == FREE */
                account.setType(AccountType.FREE);
                account.setMaxSimultanDownloads(this.getMaxSimultaneousFreeAccountDownloads());
                /* All accounts get the same (IP-based) downloadlimits --> Simultan free account usage makes no sense! */
                account.setConcurrentUsePossible(false);
                ai.setStatus("Registered (free) user");
            } else {
                ai.setValidUntil(expire_milliseconds, this.br);
                account.setType(AccountType.PREMIUM);
                account.setMaxSimultanDownloads(this.getMaxSimultanPremiumDownloadNum());
                ai.setStatus("Premium account");
            }
        }
        ai.setUnlimitedTraffic();
        return ai;
    }

    @Override
    protected long parseExpireTimeStamp(Account account, final String expireString) {
        if (expireString != null && expireString.matches("\\d{2}/\\d{2}/\\d{4}")) {
            final long timestamp_daysfirst = TimeFormatter.getMilliSeconds(expireString, "dd/MM/yyyy", Locale.ENGLISH);
            return timestamp_daysfirst;
        } else {
            return super.parseExpireTimeStamp(account, expireString);
        }
    }

    @Override
    public boolean canHandle(final DownloadLink downloadLink, final Account account) throws Exception {
        if (account == null) {
            /* Without account its not possible to download any link for this host */
            return false;
        }
        return true;
    }
}