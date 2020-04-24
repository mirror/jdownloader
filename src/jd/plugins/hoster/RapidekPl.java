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

import java.io.IOException;
import java.util.ArrayList;
import java.util.Locale;

import org.appwork.utils.StringUtils;
import org.appwork.utils.formatter.TimeFormatter;
import org.jdownloader.captcha.v2.challenge.recaptcha.v2.CaptchaHelperHostPluginRecaptchaV2;
import org.jdownloader.plugins.controller.host.LazyHostPlugin.FEATURE;

import jd.PluginWrapper;
import jd.config.Property;
import jd.http.Browser;
import jd.http.Cookies;
import jd.http.URLConnectionAdapter;
import jd.plugins.Account;
import jd.plugins.Account.AccountType;
import jd.plugins.AccountInfo;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;
import jd.plugins.components.PluginJSonUtils;

@HostPlugin(revision = "$Revision$", interfaceVersion = 3, names = { "rapidek.pl" }, urls = { "" })
public class RapidekPl extends PluginForHost {
    private static final String  WEBSITE_BASE                 = "https://rapidek.pl";
    private static final boolean account_PREMIUM_resume       = true;
    /** 2020-03-21: phg: In my tests, it is OK for the chunkload with the value of 5 */
    private static final int     account_PREMIUM_maxchunks    = 0;
    private static final int     account_PREMIUM_maxdownloads = -1;
    private static final boolean account_FREE_resume          = true;
    private static final int     account_FREE_maxchunks       = 1;
    private static final int     account_FREE_maxdownloads    = 1;

    @SuppressWarnings("deprecation")
    public RapidekPl(PluginWrapper wrapper) {
        super(wrapper);
        this.enablePremium("https://rapidek.pl/doladuj");
    }

    @Override
    public String getAGBLink() {
        return "https://rapidek.pl/";
    }

    private Browser newBrowser() {
        br = new Browser();
        br.setCookiesExclusive(true);
        br.getHeaders().put("User-Agent", "JDownloader");
        return br;
    }

    @Override
    public AvailableStatus requestFileInformation(final DownloadLink link) throws IOException, PluginException {
        return AvailableStatus.UNCHECKABLE;
    }

    @Override
    public void handleFree(final DownloadLink link) throws Exception, PluginException {
        throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_ONLY);
    }

    @Override
    public void handlePremium(final DownloadLink link, final Account account) throws Exception {
        throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_ONLY);
    }

    @Override
    public FEATURE[] getFeatures() {
        return new FEATURE[] { FEATURE.MULTIHOST };
    }

    @Override
    public void handleMultiHost(final DownloadLink link, final Account account) throws Exception {
        this.br = newBrowser();
        String dllink = checkDirectLink(link, this.getHost() + "directlink");
        br.setFollowRedirects(true);
        if (dllink == null) {
            this.loginWebsite(account);
            br.postPageRaw(WEBSITE_BASE + "/api/file-download/url-check", String.format("{\"SessionId\":null,\"Urls\":\"%s\"}", link.getDefaultPlugin().buildExternalDownloadURL(link, this)));
            String internalid = br.toString();
            if (StringUtils.isEmpty(internalid) || !internalid.matches("\"?[a-f0-9\\-]+\"?")) {
                throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Bad ID", 5 * 60 * 1000l);
            }
            internalid = internalid.replace("\"", "");
            /* 2020-04-21: Serverside, downloads do not seem to work at all (?) */
            dllink = WEBSITE_BASE + "/file?id=" + internalid;
            // if (dllink == null) {
            // throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "dllinknull", 5 * 60 * 1000l);
            // }
        }
        link.setProperty(this.getHost() + "directlink", dllink);
        final boolean resume;
        final int maxchunks;
        if (account.getType() == AccountType.PREMIUM) {
            resume = account_PREMIUM_resume;
            maxchunks = account_PREMIUM_maxchunks;
        } else {
            resume = account_FREE_resume;
            maxchunks = account_FREE_maxchunks;
        }
        dl = jd.plugins.BrowserAdapter.openDownload(br, link, dllink, resume, maxchunks);
        if (dl.getConnection().getContentType().contains("html")) {
            br.followConnection(true);
            throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Unknown download error", 5 * 60 * 1000l);
        }
        this.dl.startDownload();
    }

    private String checkDirectLink(final DownloadLink downloadLink, final String property) {
        String dllink = downloadLink.getStringProperty(property);
        if (dllink != null) {
            URLConnectionAdapter con = null;
            try {
                final Browser br2 = br.cloneBrowser();
                br2.setFollowRedirects(true);
                con = br2.openHeadConnection(dllink);
                if (con.getContentType().contains("html") || !con.isOK() || con.getLongContentLength() == -1) {
                    downloadLink.setProperty(property, Property.NULL);
                    dllink = null;
                }
            } catch (final Exception e) {
                logger.log(e);
                downloadLink.setProperty(property, Property.NULL);
                dllink = null;
            } finally {
                if (con != null) {
                    con.disconnect();
                }
            }
        }
        return dllink;
    }

    @Override
    public AccountInfo fetchAccountInfo(final Account account) throws Exception {
        this.br = newBrowser();
        final AccountInfo ai = new AccountInfo();
        loginWebsite(account);
        final String expireDate = br.getRegex("TODO(.+)TODOFIXME").getMatch(0);
        if (expireDate == null) {
            /* Free or plugin failure */
            /*
             * account.setType(AccountType.FREE); ai.setTrafficMax("10 GB"); ai.setStatus("Free Account");
             * account.setMaxSimultanDownloads(account_FREE_maxdownloads); account.setValid(true);
             */
            account.setType(AccountType.FREE);
            /* 2020-04-24: Set unlimited traffic on all accounts for testing */
            // ai.setTrafficLeft(0);
        } else {
            /* Premium */
            account.setType(AccountType.PREMIUM);
            ai.setStatus("Premium account");
            account.setMaxSimultanDownloads(account_PREMIUM_maxdownloads);
            ai.setValidUntil(TimeFormatter.getMilliSeconds(expireDate, "yyyy-MM-dd HH:mm:ss", Locale.ENGLISH));
            ai.setUnlimitedTraffic();
        }
        // account.setType(AccountType.UNKNOWN);
        /*
         * 2020-04-21: TODO website is broken? https://rapidek.pl/lista-serwisow
         */
        /* 2020-04-24: The most WTF way to distribute a list of supported hosts */
        br.getPage("https://rapidekforum.pl/all.php");
        final ArrayList<String> supportedHosts = new ArrayList<String>();
        final String[] hosts = br.getRegex("([^;]+);").getColumn(0);
        for (final String host : hosts) {
            /* Do not add domain of this multihost plugin */
            if (host.equalsIgnoreCase(this.getHost())) {
                continue;
            }
            supportedHosts.add(host);
        }
        ai.setMultiHostSupport(this, supportedHosts);
        /* Debug test */
        // ai.setUnlimitedTraffic();
        account.setConcurrentUsePossible(true);
        return ai;
    }

    private void loginWebsite(final Account account) throws IOException, PluginException, InterruptedException {
        synchronized (account) {
            try {
                br.setFollowRedirects(true);
                final Cookies cookies = account.loadCookies("");
                String token = account.getStringProperty("token");
                if (cookies != null) {
                    logger.info("Trying to login via cookies");
                    br.setCookies(WEBSITE_BASE, cookies);
                    br.getPage(WEBSITE_BASE + "/");
                    if (this.isLoggedIN()) {
                        logger.info("Cookie login successful");
                        account.saveCookies(br.getCookies(br.getHost()), "");
                        br.getHeaders().put("Authorization", "Bearer " + token);
                        return;
                    } else {
                        logger.info("Cookie login failed");
                    }
                }
                logger.info("Performing full login");
                br.getPage(WEBSITE_BASE);
                String captchaResponse = "null";
                if (CaptchaHelperHostPluginRecaptchaV2.containsRecaptchaV2Class(br.toString())) {
                    /* 2020-03-19: reCaptchaV2 is always required but they may make it optional in the future. */
                    final DownloadLink dlinkbefore = this.getDownloadLink();
                    try {
                        final DownloadLink dl_dummy;
                        if (dlinkbefore != null) {
                            dl_dummy = dlinkbefore;
                        } else {
                            dl_dummy = new DownloadLink(this, "Account", this.getHost(), "https://" + account.getHoster() + "/dashboard.php", true);
                            this.setDownloadLink(dl_dummy);
                        }
                        captchaResponse = new CaptchaHelperHostPluginRecaptchaV2(this, br).getToken();
                    } finally {
                        this.setDownloadLink(dlinkbefore);
                    }
                }
                br.getHeaders().put("x-requested-with", "XMLHttpRequest");
                br.postPageRaw(WEBSITE_BASE + "/account/login", String.format("{\"Username\":\"%s\",\"Password\":\"%s\",\"CaptchaResponse\":%s}", account.getUser(), account.getPass(), captchaResponse));
                token = PluginJSonUtils.getJson(br, "Token");
                if (!isLoggedIN() || StringUtils.isEmpty(token)) {
                    throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_DISABLE);
                }
                account.saveCookies(br.getCookies(br.getHost()), "");
                account.setProperty("token", token);
                br.getHeaders().put("Authorization", "Bearer " + token);
            } catch (PluginException e) {
                if (e.getLinkStatus() == LinkStatus.ERROR_PREMIUM) {
                    account.clearCookies("");
                }
                throw e;
            }
        }
    }

    private boolean isLoggedIN() throws PluginException {
        return br.getCookie(br.getHost(), "auth", Cookies.NOTDELETEDPATTERN) != null;
    }

    @Override
    public void reset() {
    }

    @Override
    public void resetDownloadlink(DownloadLink link) {
    }
}