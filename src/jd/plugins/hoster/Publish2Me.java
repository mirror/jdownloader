//jDownloader - Downloadmanager
//Copyright (C) 2009  JD-Team support@jdownloader.org
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

import java.io.File;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import jd.PluginWrapper;
import jd.config.ConfigContainer;
import jd.config.ConfigEntry;
import jd.config.Property;
import jd.http.Browser;
import jd.http.Cookie;
import jd.http.Cookies;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.plugins.Account;
import jd.plugins.Account.AccountType;
import jd.plugins.AccountInfo;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;

import org.appwork.utils.formatter.SizeFormatter;
import org.appwork.utils.formatter.TimeFormatter;
import org.jdownloader.captcha.v2.challenge.recaptcha.v1.Recaptcha;

/**
 *
 * @author raztoki
 *
 */
@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "publish2.me" }, urls = { "https?://(www\\.)?publish2\\.me/file/[a-z0-9]{13,}/" })
public class Publish2Me extends K2SApi {

    private final String MAINPAGE = "http://publish2.me";

    public Publish2Me(PluginWrapper wrapper) {
        super(wrapper);
        this.enablePremium(MAINPAGE + "/#premium");
        setConfigElements();
    }

    @Override
    public String getAGBLink() {
        return MAINPAGE + "/page/terms.html";
    }

    @Override
    public void correctDownloadLink(final DownloadLink link) {
        // link cleanup, but respect users protocol choosing.
        link.setUrlDownload(link.getDownloadURL().replaceFirst("^https?://", getProtocol()));
    }

    /* K2SApi setters */

    /**
     * sets domain the API will use!
     */
    @Override
    protected String getDomain() {
        return "publish2.me";
    }

    @Override
    public long getVersion() {
        return (Math.max(super.getVersion(), 0) * 100000) + getAPIRevision();
    }

    @Override
    protected String getFUID(final DownloadLink downloadLink) {
        return new Regex(downloadLink.getDownloadURL(), "/([a-z0-9]+)/$").getMatch(0);
    }

    /**
     * easiest way to set variables, without the need for multiple declared references
     *
     * @param account
     */
    private void setConstants(final Account account) {
        if (account != null) {
            if (account.getType() == AccountType.FREE) {
                // free account
                chunks = 1;
                resumes = true;
                isFree = true;
                directlinkproperty = "freelink2";
            } else {
                // premium account
                chunks = 0;
                resumes = true;
                isFree = false;
                directlinkproperty = "premlink";
            }
            logger.finer("setConstants = " + account.getUser() + " @ Account Download :: isFree = " + isFree + ", upperChunks = " + chunks + ", Resumes = " + resumes);
        } else {
            // free non account
            chunks = 1;
            resumes = true;
            isFree = true;
            directlinkproperty = "freelink1";
            logger.finer("setConstants = Guest Download :: isFree = " + isFree + ", upperChunks = " + chunks + ", Resumes = " + resumes);
        }
    }

    /* end of K2SApi stuff */

    private void setConfigElements() {
        final ConfigEntry cfgapi = new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, this.getPluginConfig(), getUseAPIPropertyID(), "Use API (recommended!)").setDefaultValue(isUseAPIDefaultEnabled());
        getConfig().addEntry(cfgapi);
        getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, this.getPluginConfig(), EXPERIMENTALHANDLING, "Enable reconnect workaround (only for API mode!)?").setDefaultValue(default_eh).setEnabledCondidtion(cfgapi, true));
        getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_TEXTFIELD, this.getPluginConfig(), super.CUSTOM_REFERER, "Set custom Referer here (only non NON-API mode!)").setDefaultValue(null).setEnabledCondidtion(cfgapi, false));
        getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, this.getPluginConfig(), SSL_CONNECTION, "Use Secure Communication over SSL (HTTPS://)").setDefaultValue(default_SSL_CONNECTION));
    }

    @Override
    public AvailableStatus requestFileInformation(final DownloadLink link) throws Exception {
        // for multihosters which call this method directly.
        if (useAPI()) {
            return super.requestFileInformation(link);
        }
        correctDownloadLink(link);
        this.setBrowserExclusive();
        super.prepBrowserForWebsite(this.br);
        getPage(link.getDownloadURL());
        if (br.getRequest().getHttpConnection().getResponseCode() == 404) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        final String filename = br.getRegex("class=\"icon-download-alt\" style=\"\"></i>([^<>\"]*?)</div>").getMatch(0);
        final String filesize = br.getRegex(">File size: ([^<>\"]*?)</").getMatch(0);
        if (filename == null || filesize == null) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        link.setName(Encoding.htmlDecode(filename.trim()));
        link.setDownloadSize(SizeFormatter.getSize(filesize));
        return AvailableStatus.TRUE;
    }

    @Override
    public void handleFree(final DownloadLink downloadLink) throws Exception, PluginException {
        setConstants(null);
        if (useAPI()) {
            super.handleDownload(downloadLink, null);
        } else {
            requestFileInformation(downloadLink);
            doFree(downloadLink, null);
        }
    }

    private final String freeAccConLimit = "Free account does not allow to download more than one file at the same time";
    private final String reCaptcha       = "api\\.recaptcha\\.net|google\\.com/recaptcha/api/";
    private final String formCaptcha     = "/file/captcha\\.html\\?v=[a-z0-9]+";

    public void doFree(final DownloadLink downloadLink, final Account account) throws Exception, PluginException {
        String dllink = downloadLink.getStringProperty(directlinkproperty, null);
        // because opening the link to test it, uses up the availability, then reopening it again = too many requests too quickly issue.
        if (!inValidate(dllink)) {
            final Browser obr = br.cloneBrowser();
            logger.info("Reusing cached finallink!");
            dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, dllink, resumes, chunks);
            if (dl.getConnection().getContentType().contains("html") || dl.getConnection().getLongContentLength() == -1 || dl.getConnection().getResponseCode() == 401) {
                br.followConnection();
                handleGeneralServerErrors(account, downloadLink);
                // we now want to restore!
                br = obr;
                dllink = null;
                downloadLink.setProperty(directlinkproperty, Property.NULL);
            }
        }
        // if above has failed, dllink will be null
        if (inValidate(dllink)) {
            dllink = getDllink();
            if (dllink == null) {
                if (br.containsHTML(">\\s*This file is available<br>only for premium members\\.\\s*</div>")) {
                    premiumDownloadRestriction("This file can only be downloaded by premium users");
                }
                final String id = br.getRegex("data-slow-id=\"([a-z0-9]+)\"").getMatch(0);
                if (id == null) {
                    throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                }
                br.getHeaders().put("X-Requested-With", "XMLHttpRequest");
                postPage(br.getURL(), "slow_id=" + id);
                if (br.containsHTML("Free user can't download large files")) {
                    premiumDownloadRestriction("This file can only be downloaded by premium users");
                } else if (br.containsHTML(freeAccConLimit)) {
                    // could be shared network or a download hasn't timed out yet or user downloading in another program?
                    throw new PluginException(LinkStatus.ERROR_IP_BLOCKED, "Connection limit reached", 10 * 60 * 60 * 1001);
                }
                if (br.containsHTML(">Downloading is not possible<")) {
                    final Regex waittime = br.getRegex("Please wait (\\d{2}):(\\d{2}):(\\d{2}) to download this");
                    String tmphrs = waittime.getMatch(0);
                    String tmpmin = waittime.getMatch(1);
                    String tmpsec = waittime.getMatch(2);
                    if (tmphrs == null && tmpmin == null && tmpsec == null) {
                        logger.info("Waittime regexes seem to be broken");
                        throw new PluginException(LinkStatus.ERROR_IP_BLOCKED, null, 60 * 60 * 1000l);
                    } else {
                        int minutes = 0, seconds = 0, hours = 0;
                        if (tmphrs != null) {
                            hours = Integer.parseInt(tmphrs);
                        }
                        if (tmpmin != null) {
                            minutes = Integer.parseInt(tmpmin);
                        }
                        if (tmpsec != null) {
                            seconds = Integer.parseInt(tmpsec);
                        }
                        int totalwaittime = ((3600 * hours) + (60 * minutes) + seconds + 1) * 1000;
                        logger.info("Detected waittime #2, waiting " + waittime + "milliseconds");
                        throw new PluginException(LinkStatus.ERROR_IP_BLOCKED, null, totalwaittime);
                    }
                }
                dllink = getDllink();
                if (dllink == null) {
                    final Browser cbr = br.cloneBrowser();
                    String captcha = null;
                    final int repeat = 4;
                    for (int i = 1; i <= repeat; i++) {
                        if (br.containsHTML(reCaptcha)) {
                            final Recaptcha rc = new Recaptcha(br, this);
                            rc.findID();
                            rc.load();
                            final File cf = rc.downloadCaptcha(getLocalCaptchaFile());
                            final String c = getCaptchaCode("recaptcha", cf, downloadLink);
                            postPage(br.getURL(), "recaptcha_challenge_field=" + rc.getChallenge() + "&recaptcha_response_field=" + Encoding.urlEncode(c) + "&free=1&freeDownloadRequest=1&uniqueId=" + id);
                            if (br.containsHTML(reCaptcha) && i + 1 != repeat) {
                                continue;
                            } else if (br.containsHTML(reCaptcha) && i + 1 == repeat) {
                                throw new PluginException(LinkStatus.ERROR_CAPTCHA);
                            } else {
                                break;
                            }
                        } else if (br.containsHTML(formCaptcha)) {
                            if (captcha == null) {
                                captcha = br.getRegex(formCaptcha).getMatch(-1);
                                if (captcha == null) {
                                    throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                                }
                            }
                            String code = getCaptchaCode(captcha, downloadLink);
                            postPage(br.getURL(), "CaptchaForm%5Bcode%5D=" + code + "&free=1&freeDownloadRequest=1&uniqueId=" + id);
                            if (br.containsHTML(formCaptcha) && i + 1 != repeat) {
                                getPage(cbr, "/file/captcha.html?refresh=1&_=" + System.currentTimeMillis());
                                captcha = cbr.getRegex("\"url\":\"([^<>\"]*?)\"").getMatch(0);
                                if (captcha != null) {
                                    captcha = captcha.replace("\\", "");
                                }
                                continue;
                            } else if (br.containsHTML(formCaptcha) && i + 1 == repeat) {
                                throw new PluginException(LinkStatus.ERROR_CAPTCHA);
                            } else {
                                break;
                            }
                        }
                    }
                    int wait = 30;
                    final String waittime = br.getRegex("class=\"tik-tak\"[\t\r\n ]{0,}>(\\d+)</div>").getMatch(0);
                    if (waittime != null) {
                        wait = Integer.parseInt(waittime);
                    }
                    this.sleep(wait * 1001l, downloadLink);
                    postPage(br.getURL(), "free=1&uniqueId=" + id);
                    if (br.containsHTML(freeAccConLimit)) {
                        // could be shared network or a download hasn't timed out yet or user downloading in another program?
                        throw new PluginException(LinkStatus.ERROR_IP_BLOCKED, "Connection limit reached", 10 * 60 * 60 * 1001);
                    }
                    dllink = getDllink();
                    if (dllink == null) {
                        throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                    }
                }
            }
            logger.info("dllink = " + dllink);
            dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, dllink, resumes, chunks);
            if (dl.getConnection().getContentType().contains("html")) {
                br.followConnection();
                handleGeneralServerErrors(account, downloadLink);
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
        }
        downloadLink.setProperty("directlink", dllink);
        // add download slot
        controlSlot(+1, account);
        try {
            dl.startDownload();
        } finally {
            // remove download slot
            controlSlot(-1, account);
        }
    }

    @SuppressWarnings("unchecked")
    private void login(final Account account, final boolean force) throws Exception {
        synchronized (ACCLOCK) {
            try {
                // Load cookies
                br.setCookiesExclusive(true);
                final Object ret = account.getProperty("cookies", null);
                boolean acmatch = Encoding.urlEncode(account.getUser()).equals(account.getStringProperty("name", Encoding.urlEncode(account.getUser())));
                if (acmatch) {
                    acmatch = Encoding.urlEncode(account.getPass()).equals(account.getStringProperty("pass", Encoding.urlEncode(account.getPass())));
                }
                if (acmatch && ret != null && ret instanceof HashMap<?, ?> && !force) {
                    final HashMap<String, String> cookies = (HashMap<String, String>) ret;
                    if (account.isValid()) {
                        for (final Map.Entry<String, String> cookieEntry : cookies.entrySet()) {
                            final String key = cookieEntry.getKey();
                            final String value = cookieEntry.getValue();
                            br.setCookie(MAINPAGE, key, value);
                        }
                        return;
                    }
                }
                br.setFollowRedirects(false);
                getPage(MAINPAGE.replaceFirst("^https?://", getProtocol()) + "/login.html");
                String logincaptcha = br.getRegex("\"(/auth/captcha\\.html[^<>\"]*?)\"").getMatch(0);
                String postData = "LoginForm%5BrememberMe%5D=0&LoginForm%5BrememberMe%5D=1&LoginForm%5Busername%5D=" + Encoding.urlEncode(account.getUser()) + "&LoginForm%5Bpassword%5D=" + Encoding.urlEncode(account.getPass());
                if (logincaptcha != null) {
                    final DownloadLink dummyLink = new DownloadLink(this, "Account", Browser.getHost(MAINPAGE), MAINPAGE, true);
                    final String c = getCaptchaCode(logincaptcha, dummyLink);
                    postData += "&LoginForm%5BverifyCode%5D=" + Encoding.urlEncode(c);
                }
                br.getHeaders().put("X-Requested-With", "XMLHttpRequest");
                postPage("/login.html", postData);
                if (!br.containsHTML("\"url\":\"")) {
                    if ("de".equalsIgnoreCase(System.getProperty("user.language"))) {
                        throw new PluginException(LinkStatus.ERROR_PREMIUM, "\r\nUngültiger Benutzername, ungültiges Passwort oder ungültiges Login Captcha!\r\nSchnellhilfe: \r\nDu bist dir sicher, dass dein eingegebener Benutzername und Passwort stimmen?\r\nFalls dein Passwort Sonderzeichen enthält, ändere es und versuche es erneut!", PluginException.VALUE_ID_PREMIUM_DISABLE);
                    } else {
                        throw new PluginException(LinkStatus.ERROR_PREMIUM, "\r\nInvalid username/password or login captcha!\r\nQuick help:\r\nYou're sure that the username and password you entered are correct?\r\nIf your password contains special characters, change it (remove them) and try again!", PluginException.VALUE_ID_PREMIUM_DISABLE);
                    }
                }
                // Save cookies
                final HashMap<String, String> cookies = new HashMap<String, String>();
                final Cookies add = br.getCookies(MAINPAGE);
                for (final Cookie c : add.getCookies()) {
                    cookies.put(c.getKey(), c.getValue());
                }
                account.setProperty("name", Encoding.urlEncode(account.getUser()));
                account.setProperty("pass", Encoding.urlEncode(account.getPass()));
                account.setProperty("cookies", cookies);
            } catch (final PluginException e) {
                account.setProperty("cookies", Property.NULL);
                throw e;
            }
        }
    }

    @Override
    public AccountInfo fetchAccountInfo(final Account account) throws Exception {
        AccountInfo ai = new AccountInfo();
        if (account.getUser() == null || !account.getUser().contains("@")) {
            account.setValid(false);
            ai.setStatus("Please use E-Mail as login/name!\r\nBitte E-Mail Adresse als Benutzername benutzen!");
            return ai;
        }
        if (useAPI()) {
            ai = super.fetchAccountInfo(account);
        } else {
            try {
                login(account, true);
            } catch (PluginException e) {
                account.setValid(false);
                throw e;
            }
            getPage("/site/profile.html");
            ai.setUnlimitedTraffic();
            final String expire = br.getRegex("Premium expires:[\t\n\r ]+<b>([^<>\"]*?)</b>").getMatch(0);
            if (expire == null) {
                ai.setStatus("Free Account");
                account.setType(AccountType.FREE);
            } else {
                ai.setValidUntil(TimeFormatter.getMilliSeconds(expire, "yyyy.MM.dd", Locale.ENGLISH));
                ai.setStatus("Premium Account");
                account.setType(AccountType.PREMIUM);
            }
            final String trafficleft = br.getRegex("Available traffic \\(today\\):[\t\n\r ]+<b><a href=\"/user/statistic\\.html\">([^<>\"]*?)</a>").getMatch(0);
            if (trafficleft != null) {
                ai.setTrafficLeft(SizeFormatter.getSize(trafficleft));
            }
            setAccountLimits(account);
            account.setValid(true);
        }
        return ai;
    }

    @Override
    protected void setAccountLimits(Account account) {
        if (account != null && account.getType() == AccountType.FREE) {
            maxPrem.set(1);
        } else if (account != null && account.getType() == AccountType.PREMIUM) {
            maxPrem.set(20);
        }
    }

    @Override
    public void handlePremium(final DownloadLink link, final Account account) throws Exception {
        setConstants(account);
        if (useAPI()) {
            super.handleDownload(link, account);
        } else {
            requestFileInformation(link);
            login(account, false);
            br.setFollowRedirects(false);
            getPage(link.getDownloadURL());
            if (account.getType() == AccountType.FREE) {
                doFree(link, account);
            } else {
                String dllink = br.getRedirectLocation();
                /* Maybe user has direct downloads disabled */
                if (inValidate(dllink)) {
                    dllink = getDllink();
                    if (inValidate(dllink)) {
                        throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                    }
                }
                dl = jd.plugins.BrowserAdapter.openDownload(br, link, Encoding.htmlDecode(dllink), resumes, chunks);
                if (dl.getConnection().getContentType().contains("html")) {
                    logger.warning("The final dllink seems not to be a file!");
                    br.followConnection();
                    handleGeneralServerErrors(account, link);
                    throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                }
                // add download slot
                controlSlot(+1, account);
                try {
                    dl.startDownload();
                } finally {
                    // remove download slot
                    controlSlot(-1, account);
                }
            }
        }
    }

    private String getDllink() throws Exception {
        String dllink = br.getRegex("(\"|')(/file/url\\.html\\?file=[a-z0-9]+)\\1").getMatch(1);
        if (dllink != null) {
            getPage(dllink);
            dllink = br.getRegex("\"url\":\"(http[^<>\"]*?)\"").getMatch(0);
            if (dllink == null) {
                dllink = br.getRedirectLocation();
            }
            if (dllink == null) {
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            dllink = dllink.replace("\\", "");
        }
        return dllink;
    }

    @Override
    public int getMaxSimultanPremiumDownloadNum() {
        return maxPrem.get();
    }

    @Override
    public void reset() {
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return 1;
    }

    @Override
    public void resetDownloadlink(DownloadLink link) {
    }

    /* NO OVERRIDE!! We need to stay 0.9*compatible */
    public boolean hasCaptcha(final DownloadLink link, final jd.plugins.Account acc) {
        if (acc == null) {
            /* no account, yes we can expect captcha */
            return true;
        }
        if (acc.getType() == AccountType.FREE) {
            /* free accounts also have captchas */
            return true;
        }
        if (acc.getStringProperty("session_type") != null && !"premium".equalsIgnoreCase(acc.getStringProperty("session_type"))) {
            return true;
        }
        return false;
    }

}