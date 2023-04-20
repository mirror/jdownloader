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

import jd.PluginWrapper;
import jd.http.Browser;
import jd.plugins.Account;
import jd.plugins.Account.AccountType;
import jd.plugins.AccountInfo;
import jd.plugins.AccountRequiredException;
import jd.plugins.DownloadLink;
import jd.plugins.HostPlugin;
import jd.plugins.PluginException;

import org.jdownloader.plugins.components.YetiShareCore;

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = {}, urls = {})
public class ArchivosMe extends YetiShareCore {
    public ArchivosMe(PluginWrapper wrapper) {
        super(wrapper);
        this.enablePremium(getPurchasePremiumURL());
    }

    /**
     * DEV NOTES YetiShare<br />
     ****************************
     * mods: See overridden functions<br />
     * limit-info:<br />
     * captchatype-info: null<br />
     * other: <br />
     */
    public static final String mainDownloadDomain = "212.162.153.174";

    public static List<String[]> getPluginDomains() {
        final List<String[]> ret = new ArrayList<String[]>();
        // each entry in List<String[]> will result in one PluginForHost, Plugin.getHost() will return String[0]->main domain
        /* 2021-04-28: IP workaround is still required! */
        ret.add(new String[] { "archivos.me", mainDownloadDomain, "95.215.205.103", "archivos.club" });
        return ret;
    }

    @Override
    protected List<String> getDeadDomains() {
        final ArrayList<String> deadDomains = new ArrayList<String>();
        deadDomains.add("95.215.205.103");
        deadDomains.add("archivos.club");
        return deadDomains;
    }

    @Override
    public String rewriteHost(final String host) {
        /* 2020-02-26: archivos.club --> archivos.me */
        /* 2023-02-03: 95.215.205.103 -> 212.162.153.174 */
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
        final List<String[]> pluginDomains = getPluginDomains();
        final List<String> ret = new ArrayList<String>();
        for (final String[] domains : pluginDomains) {
            ret.add("https?://(?:www\\.)?" + buildHostsPatternPart(domains) + YetiShareCore.getDefaultAnnotationPatternPart());
        }
        return ret.toArray(new String[0]);
    }

    @Override
    public boolean isResumeable(final DownloadLink link, final Account account) {
        if (account != null && account.getType() == AccountType.FREE) {
            /* Free Account */
            return false;
        } else if (account != null && account.getType() == AccountType.PREMIUM) {
            /* Premium account */
            return true;
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
        // 2020-01-12: tested by user, more result in Error: Too many concurrent download requests.
        return 5;
    }

    @Override
    protected boolean isOfflineWebsite(final Browser br, final DownloadLink link) throws PluginException {
        /* 2020-01-18: Special */
        return br.getHttpConnection().getResponseCode() == 404;
    }

    @Override
    public void checkErrors(Browser br, final DownloadLink link, final Account account) throws PluginException {
        /* 2020-01-18: Special */
        if (br.containsHTML(">\\s*You must have a premium status to download")) {
            throw new AccountRequiredException();
        }
        super.checkErrors(br, link, account);
    }

    private boolean domainHackActive = false;

    @Override
    protected AccountInfo fetchAccountInfoWebsite(final Account account) throws Exception {
        /* 2020-01-18: Special */
        domainHackActive = true;
        loginWebsite(account, true);
        if (br.getURL() == null || !br.getURL().contains("/account_edit.html")) {
            getPage("/account_edit.html");
        }
        final String expireStr = br.getRegex(">\\s*(\\d{2}/\\d{2}/\\d{4} \\d{2}:\\d{2}:\\d{2})\\s*<").getMatch(0);
        if (expireStr != null) {
            final AccountInfo ai = new AccountInfo();
            long expire_milliseconds = parseExpireTimeStamp(account, expireStr);
            final boolean isPremium = expire_milliseconds > System.currentTimeMillis();
            if (!isPremium) {
                /* Expired premium == FREE */
                account.setType(AccountType.FREE);
                account.setMaxSimultanDownloads(this.getMaxSimultaneousFreeAccountDownloads());
                /* All accounts get the same (IP-based) downloadlimits --> Simultan free account usage makes no sense! */
                account.setConcurrentUsePossible(false);
            } else {
                ai.setValidUntil(expire_milliseconds, this.br);
                account.setType(AccountType.PREMIUM);
                account.setMaxSimultanDownloads(this.getMaxSimultanPremiumDownloadNum());
            }
            ai.setUnlimitedTraffic();
            return ai;
        } else {
            /* Fallback to template handling */
            return super.fetchAccountInfoWebsite(account);
        }
    }

    @Override
    public boolean loginWebsite(final Account account, boolean force) throws Exception {
        domainHackActive = true;
        try {
            return super.loginWebsite(account, true);
        } finally {
            domainHackActive = false;
        }
    }

    @Override
    public String getHost() {
        if (domainHackActive) {
            return mainDownloadDomain;
        } else {
            return super.getHost();
        }
    }

    @Override
    public boolean supports_https() {
        /* 2020-03-04: Special */
        return false;
    }

    @Override
    protected boolean allowGetProtocolHttpsAutoHandling(String url) {
        return false;
    }

    @Override
    public boolean requiresWWW() {
        /* 2020-03-04: Special */
        return false;
    }

    /** Below there are some hacks to be able to internally use one domain but use another domain as plugin display-domain/main-domain. */
    @Override
    protected String getMainPage(final String url) {
        return this.getProtocol(url) + this.appendWWWIfRequired(mainDownloadDomain);
    }

    @Override
    protected String getMainPage(final DownloadLink link) {
        final String protocol = getProtocol(link.getPluginPatternMatcher());
        final String domainToUse = this.appendWWWIfRequired(mainDownloadDomain);
        return protocol + domainToUse;
    }
}