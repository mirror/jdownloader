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

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;

import org.appwork.utils.StringUtils;
import org.jdownloader.captcha.v2.challenge.recaptcha.v2.CaptchaHelperHostPluginRecaptchaV2;
import org.jdownloader.plugins.components.antiDDoSForHost;
import org.jdownloader.plugins.controller.host.LazyHostPlugin.FEATURE;

import jd.PluginWrapper;
import jd.config.Property;
import jd.http.Browser;
import jd.http.Cookies;
import jd.http.URLConnectionAdapter;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.plugins.Account;
import jd.plugins.Account.AccountType;
import jd.plugins.AccountInfo;
import jd.plugins.AccountUnavailableException;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.components.MultiHosterManagement;

@HostPlugin(revision = "$Revision$", interfaceVersion = 3, names = { "superdown.com.br" }, urls = { "https?://[\\w]+\\.superdown\\.com\\.br/(?:superdown/)?\\w+/[a-zA-Z0-9]+/\\d+/\\S+" })
public class SuperdownComBr extends antiDDoSForHost {
    /* Tags: conexaomega.com.br, megarapido.net, superdown.com.br */
    private static MultiHosterManagement mhm           = new MultiHosterManagement("superdown.com.br");
    private final String                 html_loggedin = "href=\"[^<>\"]*?logout[^<>\"]*?\"";
    private static Object                LOCK          = new Object();

    public SuperdownComBr(PluginWrapper wrapper) {
        super(wrapper);
        this.enablePremium("http://www.superdown.com.br/en/planos");
    }

    @Override
    public String getAGBLink() {
        return "http://www.superdown.com.br/en/termos";
    }

    @Override
    protected Browser prepBrowser(final Browser prepBr, final String host) {
        if (!(browserPrepped.containsKey(prepBr) && browserPrepped.get(prepBr) == Boolean.TRUE)) {
            super.prepBrowser(prepBr, host);
            /* define custom browser headers and language settings */
            prepBr.setCustomCharset("utf-8");
            prepBr.setCookie(this.getHost(), "locale", "en");
            prepBr.setFollowRedirects(true);
        }
        return prepBr;
    }

    @Override
    public AvailableStatus requestFileInformation(DownloadLink link) throws PluginException {
        final boolean checked = checkLinks(new DownloadLink[] { link });
        // we can't throw exception in checklinks! This is needed to prevent multiple captcha events!
        if (!checked && hasAntiddosCaptchaRequirement()) {
            throw new PluginException(LinkStatus.ERROR_CAPTCHA);
        } else if (!checked || !link.isAvailabilityStatusChecked()) {
            link.setAvailableStatus(AvailableStatus.UNCHECKABLE);
        } else if (!link.isAvailable()) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        return getAvailableStatus(link);
    }

    private AvailableStatus getAvailableStatus(DownloadLink link) {
        try {
            final Field field = link.getClass().getDeclaredField("availableStatus");
            field.setAccessible(true);
            Object ret = field.get(link);
            if (ret != null && ret instanceof AvailableStatus) {
                return (AvailableStatus) ret;
            }
        } catch (final Throwable e) {
        }
        return AvailableStatus.UNCHECKED;
    }

    @Override
    public boolean canHandle(DownloadLink downloadLink, Account account) throws Exception {
        if (downloadLink != null) {
            if (!StringUtils.equals((String) downloadLink.getProperty("usedPlugin", getHost()), getHost())) {
                return false;
            }
        }
        // direct link... shouldn't need account
        if (downloadLink.getPluginPatternMatcher().matches(this.getSupportedLinks().pattern())) {
            return true;
        }
        if (account == null) {
            /* without account its not possible to download the link */
            return false;
        }
        return true;
    }

    @Override
    public void handleFree(DownloadLink downloadLink) throws Exception, PluginException {
        handleDL(null, downloadLink, downloadLink.getDownloadURL());
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return 0;
    }

    @Override
    public void handlePremium(DownloadLink link, Account account) throws Exception {
        /* handle premium should never be called */
        handleDL(account, link, link.getPluginPatternMatcher());
    }

    public boolean checkLinks(DownloadLink[] urls) {
        if (urls == null || urls.length == 0) {
            return false;
        }
        try {
            final Browser checkbr = br.cloneBrowser();
            checkbr.setFollowRedirects(true);
            checkbr.setAllowedResponseCodes(new int[] { 500 });
            for (DownloadLink dl : urls) {
                URLConnectionAdapter con = null;
                try {
                    con = openAntiDDoSRequestConnection(checkbr, checkbr.createGetRequest(dl.getDownloadURL()));
                    if (con.isContentDisposition()) {
                        dl.setFinalFileName(getFileNameFromHeader(con));
                        dl.setDownloadSize(con.getLongContentLength());
                        dl.setAvailable(true);
                    } else {
                        dl.setAvailable(false);
                    }
                } finally {
                    try {
                        /* make sure we close connection */
                        con.disconnect();
                    } catch (final Throwable e) {
                    }
                }
            }
        } catch (Exception e) {
            return false;
        }
        return true;
    }

    private void handleDL(final Account account, final DownloadLink link, final String dllink) throws Exception {
        /* we want to follow redirects in final stage */
        br.setFollowRedirects(true);
        link.setProperty(this.getHost() + "directlink", dllink);
        dl = jd.plugins.BrowserAdapter.openDownload(br, link, dllink, true, 0, true);
        if (dl.getConnection().getContentType().contains("html")) {
            br.followConnection();
            mhm.handleErrorGeneric(account, link, "unknowndlerror", 50, 5 * 60 * 1000l);
        }
        link.setProperty("usedPlugin", getHost());
        dl.startDownload();
    }

    @Override
    public FEATURE[] getFeatures() {
        return new FEATURE[] { FEATURE.MULTIHOST };
    }

    @Override
    public void handleMultiHost(final DownloadLink link, final Account account) throws Exception {
        mhm.runCheck(account, link);
        login(account, false);
        String dllink = checkDirectLink(link, this.getHost() + "directlink");
        if (dllink == null) {
            br.getHeaders().put("X-Requested-With", "XMLHttpRequest");
            /* request Download */
            final String passCode = link.getStringProperty("pass", null);
            final String passwordParam;
            if (StringUtils.isNotEmpty(passCode)) {
                passwordParam = "&password=" + Encoding.urlEncode(passCode);
            } else {
                passwordParam = "";
            }
            getPage("http://www.superdown.com.br/_gerar?link=" + Encoding.urlEncode(link.getDefaultPlugin().buildExternalDownloadURL(link, this)) + "&rnd=0." + System.currentTimeMillis() + passwordParam);
            dllink = br.getRegex("(https?://[^<>\"]*?)\\|").getMatch(0);
            if (dllink == null) {
                handleSpecificErrors(link, account);
            }
            if (dllink == null || (dllink != null && dllink.length() > 500)) {
                mhm.handleErrorGeneric(account, link, "dllinknull", 50, 5 * 60 * 1000l);
            }
            dllink = dllink.replaceAll("\\\\/", "/");
        }
        showMessage(link, "Task 2: Download begins!");
        handleDL(account, link, dllink);
    }

    private void handleSpecificErrors(final DownloadLink link, final Account account) throws PluginException {
        if (br.containsHTML("Sua sess[^ ]+ expirou por inatividade\\. Efetue o login novamente\\.")) {
            account.setProperty("cookies", Property.NULL);
            throw new PluginException(LinkStatus.ERROR_RETRY);
        }
        if (br.containsHTML("Password Request")) {
            throw new PluginException(LinkStatus.ERROR_FATAL, "Password Request");
        }
        if (br.containsHTML("não é um servidor suportado pelo")) {
            // host has been picked up due to generic supported host adding (matches)
            final ArrayList supportedHosts = (ArrayList) Arrays.asList(account.getProperty("multiHostSupport", new String[] {}));
            supportedHosts.remove(link.getHost());
            account.getAccountInfo().setMultiHostSupport(this, supportedHosts);
            throw new PluginException(LinkStatus.ERROR_HOSTER_TEMPORARILY_UNAVAILABLE, "Not supported at this provider", 6 * 60 * 60 * 1000l);
        }
        if (br.containsHTML("Seu gerador premium está zerado, por favor, compre um de nossos planos")) {
            // Your premium generator is zeroed, please purchase one of our plans
            synchronized (LOCK) {
                account.getAccountInfo().setTrafficLeft(0);
            }
            throw new AccountUnavailableException("Download limit reached", 1 * 60 * 60 * 1000l);
        }
    }

    private String checkDirectLink(final DownloadLink downloadLink, final String property) {
        String dllink = downloadLink.getStringProperty(property);
        if (dllink != null) {
            try {
                final Browser br2 = br.cloneBrowser();
                URLConnectionAdapter con = br2.openGetConnection(dllink);
                if (!con.isOK() || con.getContentType().contains("html") || con.getLongContentLength() == -1) {
                    downloadLink.setProperty(property, Property.NULL);
                    dllink = null;
                }
                con.disconnect();
            } catch (final Exception e) {
                downloadLink.setProperty(property, Property.NULL);
                dllink = null;
            }
        }
        return dllink;
    }

    @Override
    public AccountInfo fetchAccountInfo(final Account account) throws Exception {
        final AccountInfo ai = new AccountInfo();
        login(account, true);
        getPage("/en/");
        final String expire_text = br.getRegex("class=\"clearfix pull-right contador\">(.*?)<li class=\"dias\"").getMatch(0);
        if (expire_text == null) {
            final String lang = System.getProperty("user.language");
            if ("de".equalsIgnoreCase(lang)) {
                throw new PluginException(LinkStatus.ERROR_PREMIUM, "\r\nPlugin defekt, bitte den JDownloader Support kontaktieren!", PluginException.VALUE_ID_PREMIUM_DISABLE);
            } else {
                throw new PluginException(LinkStatus.ERROR_PREMIUM, "\r\nPlugin broken, please contact the JDownloader Support!", PluginException.VALUE_ID_PREMIUM_DISABLE);
            }
        }
        final String[] days_digits = new Regex(expire_text, "<li>(\\d+)</li>").getColumn(0);
        String days_string = "";
        long expire_timestamp = 0;
        if (days_digits.length > 0) {
            for (final String digit : days_digits) {
                days_string += digit;
            }
            expire_timestamp = System.currentTimeMillis() + Long.parseLong(days_string) * 24 * 60 * 60 * 1000l;
        }
        if (expire_timestamp > System.currentTimeMillis()) {
            ai.setValidUntil(expire_timestamp);
            ai.setUnlimitedTraffic();
            ai.setStatus("Premium Account");
            account.setType(AccountType.PREMIUM);
        } else {
            ai.setTrafficLeft(0);
            ai.setStatus("Free Account");
            account.setType(AccountType.FREE);
        }
        account.setMaxSimultanDownloads(-1);
        account.setConcurrentUsePossible(true);
        final ArrayList<String> supportedHosts = new ArrayList<String>();
        boolean extendedCheckSuccessful = false;
        final String supportedHostsTableHTML = br.getRegex("<div[^>]*?class=\"servidores\"[^>]*?>\\s*?<ul>(.*?)</ul>").getMatch(0);
        if (supportedHostsTableHTML != null) {
            final String[] hostInfoHtmls = new Regex(supportedHostsTableHTML, "<li.*?</li>").getColumn(-1);
            for (final String hostInfoHtml : hostInfoHtmls) {
                String crippledHost = new Regex(hostInfoHtml, "<b>[^<]*?([A-Za-z0-9\\-\\.]+)[^<]*?</b>").getMatch(0);
                final boolean isOnline = new Regex(hostInfoHtml, "class=\"(soso|on)\"").matches() || new Regex(hostInfoHtml, "Novo|Disponível").matches();
                if (!StringUtils.isEmpty(crippledHost) && isOnline) {
                    crippledHost = crippledHost.toLowerCase();
                    extendedCheckSuccessful = true;
                    supportedHosts.add(crippledHost);
                }
            }
        }
        if (!extendedCheckSuccessful) {
            /* Fallback - wider RegEx without check for host-status */
            final String[] crippledHosts = br.getRegex("<b>[^<]*?([A-Za-z0-9\\-\\.]+)[^<]*?</b>").getColumn(0);
            for (String crippledHost : crippledHosts) {
                crippledHost = crippledHost.toLowerCase();
                supportedHosts.add(crippledHost);
            }
        }
        ai.setMultiHostSupport(this, supportedHosts);
        return ai;
    }

    private void showMessage(DownloadLink link, String message) {
        link.getLinkStatus().setStatusText(message);
    }

    private void login(final Account account, final boolean force) throws Exception {
        synchronized (LOCK) {
            try {
                /* Load cookies */
                br.setCookiesExclusive(true);
                final Cookies cookies = account.loadCookies("");
                if (cookies != null) {
                    /* Re-use cookies whenever possible to avoid login captcha prompts. */
                    br.setCookies(this.getHost(), cookies);
                    getPage("https://www." + this.getHost());
                    if (br.containsHTML(html_loggedin)) {
                        account.saveCookies(br.getCookies(this.getHost()), "");
                        return;
                    }
                    /* Clear cookies/headers to prevent unknown errors as we'll perform a full login below now. */
                    br = new Browser();
                }
                getPage("https://www." + this.getHost() + "/login");
                String postData = "lembrar=on&email=" + Encoding.urlEncode(account.getUser()) + "&senha=" + Encoding.urlEncode(account.getPass());
                if (new Regex(br.toString().replaceAll("<!--.*?-->", ""), "class=\"g-recaptcha\"").matches()) {
                    /* Handle login captcha */
                    final DownloadLink dlinkbefore = this.getDownloadLink();
                    if (dlinkbefore == null) {
                        this.setDownloadLink(new DownloadLink(this, "Account", this.getHost(), "https://" + account.getHoster(), true));
                    }
                    final String recaptchaV2Response = new CaptchaHelperHostPluginRecaptchaV2(this, br) {
                        @Override
                        protected String getSiteKey(String source) {
                            final String siteKey = new Regex(source, "var key\\s*=\\s*\"([\\w-]+)\";").getMatch(0);
                            return StringUtils.isEmpty(siteKey) ? super.getSiteKey(source) : siteKey;
                        };
                    }.getToken();
                    if (dlinkbefore != null) {
                        this.setDownloadLink(dlinkbefore);
                    }
                    postData += "&g-recaptcha-response=" + Encoding.urlEncode(recaptchaV2Response);
                }
                postPage("/login", postData);
                if (!br.containsHTML(html_loggedin)) {
                    throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_DISABLE);
                }
                account.saveCookies(br.getCookies(this.getHost()), "");
            } catch (final PluginException e) {
                account.clearCookies("");
                throw e;
            }
        }
    }

    @Override
    public void reset() {
    }

    @Override
    public void resetDownloadlink(DownloadLink link) {
    }
}