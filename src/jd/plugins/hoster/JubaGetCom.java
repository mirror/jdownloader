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
import java.util.Map;

import org.appwork.storage.TypeRef;
import org.appwork.utils.Regex;
import org.appwork.utils.StringUtils;
import org.appwork.utils.formatter.TimeFormatter;
import org.appwork.utils.parser.UrlQuery;
import org.jdownloader.captcha.v2.challenge.recaptcha.v2.CaptchaHelperHostPluginRecaptchaV2;
import org.jdownloader.plugins.controller.LazyPlugin;

import jd.PluginWrapper;
import jd.http.Browser;
import jd.http.Cookies;
import jd.nutils.encoding.Encoding;
import jd.parser.html.Form;
import jd.plugins.Account;
import jd.plugins.Account.AccountType;
import jd.plugins.AccountInfo;
import jd.plugins.AccountInvalidException;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;
import jd.plugins.components.MultiHosterManagement;

@HostPlugin(revision = "$Revision$", interfaceVersion = 3, names = { "juba-get.com" }, urls = { "" })
public class JubaGetCom extends PluginForHost {
    @Override
    public boolean isResumeable(final DownloadLink link, final Account account) {
        return true;
    }

    public int getMaxChunks(final Account account) {
        return 0;
    }

    private String getDirecturlProperty() {
        return this.getHost() + "directlink";
    }

    private static MultiHosterManagement mhm = new MultiHosterManagement("juba-get.com");

    public JubaGetCom(PluginWrapper wrapper) {
        super(wrapper);
        this.enablePremium("https://juba-get.com/plans");
    }

    @Override
    public LazyPlugin.FEATURE[] getFeatures() {
        return new LazyPlugin.FEATURE[] { LazyPlugin.FEATURE.MULTIHOST, LazyPlugin.FEATURE.USERNAME_IS_EMAIL };
    }

    @Override
    public String getAGBLink() {
        return "https://juba-get.com/terms";
    }

    private Browser prepBRWebsite(final Browser br) {
        br.setCookiesExclusive(true);
        br.getHeaders().put("User-Agent", "JDownloader");
        br.setCookie(this.getHost(), "locale", "en");
        br.setFollowRedirects(true);
        return br;
    }

    @Override
    public AvailableStatus requestFileInformation(final DownloadLink link) throws PluginException {
        return AvailableStatus.UNCHECKABLE;
    }

    @Override
    public boolean canHandle(final DownloadLink link, final Account account) throws Exception {
        if (account == null) {
            return false;
        } else {
            mhm.runCheck(account, link);
            return true;
        }
    }

    @Override
    public void handleFree(DownloadLink downloadLink) throws Exception, PluginException {
        throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
    }

    @Override
    public void handlePremium(DownloadLink link, Account account) throws Exception {
        /* handlePremium should never be called */
        throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
    }

    @Override
    public void handleMultiHost(final DownloadLink link, final Account account) throws Exception {
        prepBRWebsite(this.br);
        if (!attemptStoredDownloadurlDownload(link)) {
            mhm.runCheck(account, link);
            login(account, true, "https://" + this.getHost() + "/generator");
            final String csrftoken = br.getRegex("<meta name=\"csrf-token\" content=\"([^\"]+)\"").getMatch(0);
            final UrlQuery query = new UrlQuery();
            query.add("url", Encoding.urlEncode(link.getDefaultPlugin().buildExternalDownloadURL(link, this)));
            br.getHeaders().put("Accept", "application/json, text/javascript, */*; q=0.01");
            if (csrftoken != null) {
                br.getHeaders().put("x-csrf-token", csrftoken);
            }
            br.postPage("/api/generate", query);
            final Map<String, Object> entries = restoreFromString(br.toString(), TypeRef.MAP);
            String dllink = (String) entries.get("download");
            if (StringUtils.isEmpty(dllink)) {
                /* E.g. {"error":true,"error_message":"Error generate"} */
                final String error_message = (String) entries.get("error_message");
                if (!StringUtils.isEmpty(error_message)) {
                    mhm.handleErrorGeneric(account, link, error_message, 50, 1 * 60 * 1000l);
                } else {
                    mhm.handleErrorGeneric(account, link, "Failed to generate downloadlink", 50, 1 * 60 * 1000l);
                }
            }
            link.setProperty(this.getDirecturlProperty(), dllink);
            dl = jd.plugins.BrowserAdapter.openDownload(br, link, dllink, this.isResumeable(link, account), this.getMaxChunks(account));
            if (!this.looksLikeDownloadableContent(dl.getConnection())) {
                br.followConnection(true);
                mhm.handleErrorGeneric(account, link, "Final downloadurl did not lead to downloadable content", 50, 1 * 60 * 1000l);
            }
        }
        this.dl.startDownload();
    }

    private boolean attemptStoredDownloadurlDownload(final DownloadLink link) throws Exception {
        final String url = link.getStringProperty(getDirecturlProperty());
        if (StringUtils.isEmpty(url)) {
            return false;
        }
        boolean valid = false;
        try {
            final Browser brc = br.cloneBrowser();
            dl = new jd.plugins.BrowserAdapter().openDownload(brc, link, url, isResumeable(link, null), this.getMaxChunks(null));
            if (this.looksLikeDownloadableContent(dl.getConnection())) {
                valid = true;
                return true;
            } else {
                link.removeProperty(getDirecturlProperty());
                brc.followConnection(true);
                throw new IOException();
            }
        } catch (final Throwable e) {
            logger.log(e);
            return false;
        } finally {
            if (!valid) {
                try {
                    dl.getConnection().disconnect();
                } catch (Throwable ignore) {
                }
                this.dl = null;
            }
        }
    }

    @Override
    public AccountInfo fetchAccountInfo(final Account account) throws Exception {
        final AccountInfo ai = new AccountInfo();
        login(account, true, "https://" + this.getHost() + "/generator");
        final String expireDate = br.getRegex("(?i)Expires in\\s*:\\s*([^<>\n\r\t]+)").getMatch(0);
        if (expireDate != null) {
            ai.setValidUntil(TimeFormatter.getMilliSeconds(expireDate, "MMMM dd, yyyy", Locale.ENGLISH), br);
            account.setType(AccountType.PREMIUM);
            ai.setUnlimitedTraffic();
        } else {
            /* No expire date found --> Assume it's a free account. */
            account.setType(AccountType.FREE);
            /* 2022-08-12: Free accounts are not supported and/or have no traffic at all. */
            ai.setExpired(true);
            return ai;
        }
        final String hostsHTML = br.getRegex("class=\"fas fa-cloud-download-alt\"></i> Hosts</div>\\s*<div class=\"card-body\">(.*?)</div>").getMatch(0);
        if (hostsHTML == null) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        final String[] htmls = hostsHTML.split("<img");
        final ArrayList<String> supportedHosts = new ArrayList<String>();
        for (final String html : htmls) {
            String hostWithoutTLD = new Regex(html, "data-original-title=\"([^\" \\(]+)\"").getMatch(0);
            if (hostWithoutTLD == null) {
                // data-original-title="forum.com (hoster.com)">
                hostWithoutTLD = new Regex(html, "data-original-title=\"[^\"]*\\(([^\"\\)]+)\\)").getMatch(0);
            }
            if (hostWithoutTLD == null) {
                /* Skip invalid items */
                continue;
            }
            if (html.contains("servidor_online")) {
                if (!supportedHosts.contains(hostWithoutTLD)) {
                    supportedHosts.add(hostWithoutTLD);
                }
            } else {
                /* servidor_offline */
                logger.info("Skipping offline host: " + hostWithoutTLD);
            }
        }
        // br.getPage("/hosts");
        // final String[] htmls = br.getRegex("<tr>(.*?)</tr>").getColumn(0);
        // final ArrayList<String> supportedHosts = new ArrayList<String>();
        // for (final String html : htmls) {
        // final String host = new Regex(html, "class=\"servidor_online\"[^>]*alt=\"([^\"]+)\"").getMatch(0);
        // if (host == null) {
        // /* Bad HTML or broken plugin. */
        // continue;
        // }
        // supportedHosts.add(host);
        // }
        ai.setMultiHostSupport(this, supportedHosts);
        return ai;
    }

    private void login(final Account account, final boolean force, final String loginCheckURL) throws Exception {
        synchronized (account) {
            br.setCookiesExclusive(true);
            prepBRWebsite(this.br);
            final Cookies cookies = account.loadCookies("");
            if (cookies != null) {
                this.br.setCookies(this.getHost(), cookies);
                if (!force) {
                    return;
                }
                br.getPage(loginCheckURL);
                if (this.isLoggedIN(br)) {
                    account.saveCookies(this.br.getCookies(br.getHost()), "");
                    return;
                }
                account.clearCookies("");
                br.clearCookies(br.getHost());
            }
            br.getPage("https://" + this.getHost() + "/locale/en");
            br.getPage("/login");
            final Form loginform = br.getFormbyActionRegex(".*/login.*");
            if (loginform == null) {
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            loginform.put("email", Encoding.urlEncode(account.getUser()));
            loginform.put("password", Encoding.urlEncode(account.getPass()));
            loginform.put("remember", "on"); // make sure that cookies last long
            loginform.put("g-recaptcha-response", Encoding.urlEncode(new CaptchaHelperHostPluginRecaptchaV2(this, br).getToken()));
            br.submitForm(loginform);
            final String errorMsg = br.getRegex("div class=\"alert alert-danger\">\\s*<ul>\\s*<li>([^<]+)</li>").getMatch(0);
            /*
             * 2022-08-10: Sometimes even after successful login website will redirect us to /generator and display error 500 this we'll try
             * this small workaround.
             */
            if (!this.isLoggedIN(br)) {
                br.getPage("/");
            }
            if (!this.isLoggedIN(br)) {
                if (errorMsg != null) {
                    throw new AccountInvalidException(Encoding.htmlDecode(errorMsg));
                } else {
                    throw new AccountInvalidException();
                }
            }
            account.saveCookies(this.br.getCookies(br.getHost()), "");
        }
    }

    private boolean isLoggedIN(final Browser br) {
        if (br.containsHTML("/logout")) {
            return true;
        } else {
            return false;
        }
    }

    @Override
    public void reset() {
    }

    @Override
    public void resetDownloadlink(DownloadLink link) {
    }
}