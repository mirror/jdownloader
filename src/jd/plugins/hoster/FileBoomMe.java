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
import java.util.Locale;

import org.appwork.utils.formatter.SizeFormatter;
import org.appwork.utils.formatter.TimeFormatter;
import org.jdownloader.captcha.v2.challenge.recaptcha.v1.Recaptcha;

import jd.PluginWrapper;
import jd.config.ConfigContainer;
import jd.config.ConfigEntry;
import jd.http.Browser;
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
import jd.plugins.components.PluginJSonUtils;

/**
 *
 * @author raztoki
 *
 */
@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "fileboom.me" }, urls = { "https?://(www\\.)?(fboom|fileboom)\\.me/file/[a-z0-9]{13,}" })
public class FileBoomMe extends K2SApi {

    private final String MAINPAGE = "http://fboom.me";

    public FileBoomMe(PluginWrapper wrapper) {
        super(wrapper);
        this.enablePremium(MAINPAGE + "/premium.html");
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
        link.setUrlDownload(link.getDownloadURL().replace("fileboom.me/", "fboom.me/"));
    }

    @Override
    public String[] siteSupportedNames() {
        // keep2.cc no dns
        return new String[] { "fileboom.me", "fboom.me" };
    }

    @Override
    public String rewriteHost(String host) {
        if (host == null) {
            return "fileboom.me";
        }
        for (final String supportedName : siteSupportedNames()) {
            if (supportedName.equals(host)) {
                return "fileboom.me";
            }
        }
        return super.rewriteHost(host);
    }

    @Override
    protected String getUseAPIPropertyID() {
        return super.getUseAPIPropertyID() + "_2";
    }

    @Override
    protected boolean isUseAPIDefaultEnabled() {
        return false;
    }

    /* K2SApi setters */

    /**
     * sets domain the API will use!
     */
    @Override
    protected String getDomain() {
        return "fileboom.me";
    }

    @Override
    public long getVersion() {
        return (Math.max(super.getVersion(), 0) * 100000) + getAPIRevision();
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
        super.prepBrowserForWebsite(br);
        getPage(link.getDownloadURL());
        if (br.getRequest().getHttpConnection().getResponseCode() == 404) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        final String filename = br.getRegex("<i class=\"icon-download\"></i>([^<>\"]*?)</").getMatch(0);
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
        if (checkShowFreeDialog(getHost())) {
            showFreeDialog(getHost());
        }
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
        String dllink = getDirectLinkAndReset(downloadLink, true);
        // because opening the link to test it, uses up the availability, then reopening it again = too many requests too quickly issue.
        if (!inValidate(dllink)) {
            final Browser obr = br.cloneBrowser();
            logger.info("Reusing cached final link!");
            dl = new jd.plugins.BrowserAdapter().openDownload(br, downloadLink, dllink, resumes, chunks);
            if (!isValidDownloadConnection(dl.getConnection())) {
                logger.info("Refresh final link");
                dllink = null;
                try {
                    dl.getConnection().setAllowedResponseCodes(new int[] { dl.getConnection().getResponseCode() });
                    br.followConnection();
                } catch (final Throwable e) {
                    logger.log(e);
                } finally {
                    br = obr;
                    dl.getConnection().disconnect();
                }
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
                if (inValidate(id)) {
                    throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                }
                br.getHeaders().put("X-Requested-With", "XMLHttpRequest");
                postPage(br.getURL(), "slow_id=" + id);
                handleFreeErrors();
                dllink = getDllink();
                if (inValidate(dllink)) {
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
                    dllink = getDllink();
                    if (inValidate(dllink)) {
                        handleFreeErrors();
                        throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                    }
                }
            }
            logger.info("dllink = " + dllink);
            dl = new jd.plugins.BrowserAdapter().openDownload(br, downloadLink, dllink, resumes, chunks);
            if (!isValidDownloadConnection(dl.getConnection())) {
                dl.getConnection().setAllowedResponseCodes(new int[] { dl.getConnection().getResponseCode() });
                br.followConnection();
                dllink = br.getRegex("\"url\":\"(https?:[^<>\"]*?)\"").getMatch(0);
                if (dllink == null) {
                    handleGeneralServerErrors(account, downloadLink);
                    throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                }
                dllink = dllink.replace("\\", "");
                dl = new jd.plugins.BrowserAdapter().openDownload(br, downloadLink, dllink, resumes, chunks);
                if (!isValidDownloadConnection(dl.getConnection())) {
                    dl.getConnection().setAllowedResponseCodes(new int[] { dl.getConnection().getResponseCode() });
                    logger.warning("The final dllink seems not to be a file!");
                    br.followConnection();
                    handleGeneralServerErrors(account, downloadLink);
                    throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                }
            }
        }
        // add download slot
        controlSlot(+1, account);
        try {
            downloadLink.setProperty("directlink", dllink);
            dl.startDownload();
        } finally {
            // remove download slot
            controlSlot(-1, account);
        }
    }

    private void handleFreeErrors() throws PluginException {
        if (br.containsHTML(freeAccConLimit)) {
            // could be shared network or a download hasn't timed out yet or user downloading in another program?
            throw new PluginException(LinkStatus.ERROR_IP_BLOCKED, "Connection limit reached", 1 * 60 * 60 * 1001);
        }
        if (br.containsHTML("\">\\s*Downloading is not possible\\s*<|>\\s*FREE download option is limited\\.\\s*<")) {
            int hours = 0, minutes = 0, seconds = 0;
            final Regex waitregex = br.getRegex("Please wait (\\d{2}):(\\d{2}):(\\d{2}) to download this file");
            final String hrs = waitregex.getMatch(0);
            if (hrs != null) {
                hours = Integer.parseInt(hrs);
            }
            final String mins = waitregex.getMatch(1);
            if (mins != null) {
                minutes = Integer.parseInt(mins);
            }
            final String secs = waitregex.getMatch(2);
            if (secs != null) {
                seconds = Integer.parseInt(secs);
            }
            final long totalwait = (hours * 60 * 60 * 1000) + (minutes * 60 * 1000l) + (seconds * 1000l);
            if (totalwait > 0) {
                throw new PluginException(LinkStatus.ERROR_IP_BLOCKED, totalwait + 10000l);
            }
            throw new PluginException(LinkStatus.ERROR_IP_BLOCKED);
        }
        if (br.containsHTML("Free user can't download large files")) {
            premiumDownloadRestriction("This file can only be downloaded by premium users");
        }
        if (br.containsHTML("\\s*At the moment all free slots are busy, try later\\.<br>")) {
            throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "No free slots available", 10 * 60 * 1000l);
        }
    }

    private void login(final Account account, final boolean force) throws Exception {
        synchronized (ACCLOCK) {
            try {
                boolean login = true;
                final Cookies cookies = account.loadCookies("");
                if (cookies != null) {
                    /* 2017-04-25: Always check cookies here */
                    br.setCookies(this.getHost(), cookies);
                    getPage(MAINPAGE.replaceFirst("^https?://", getProtocol()));
                    if (br.containsHTML("/auth/logout")) {
                        login = false;
                    }
                }
                if (login) {
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
                }
                account.saveCookies(br.getCookies(this.getHost()), "");
            } catch (final PluginException e) {
                account.clearCookies("");
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
            } catch (final PluginException e) {
                account.setValid(false);
                throw e;
            }
            getPage("/site/profile.html");
            ai.setUnlimitedTraffic();
            final String expire = br.getRegex("Premium expires:[\t\n\r ]+<b>([^<>\"]*?)</b>").getMatch(0);
            if ("LifeTime".equalsIgnoreCase(expire)) {
                ai.setStatus("Premium Account(LifeTime)");
                ai.setValidUntil(-1);
                account.setType(AccountType.PREMIUM);
            } else if (expire == null) {
                ai.setStatus("Free Account");
                account.setType(AccountType.FREE);
            } else {
                final long expireTimeStamp = TimeFormatter.getMilliSeconds(expire, "yyyy.MM.dd", Locale.ENGLISH);
                if (expireTimeStamp == -1) {
                    throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                }
                ai.setValidUntil(expireTimeStamp + (24 * 60 * 60 * 1000l));
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
        if (account.getType() == AccountType.FREE) {
            if (checkShowFreeDialog(getHost())) {
                showFreeDialog(getHost());
            }
        }
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
                if (inValidate(dllink) && br.toString().startsWith("{")) {
                    /* 2017-04-27: Strange - accessing just the downloadurl in premium mode, the website returns json (sometimes?). */
                    dllink = PluginJSonUtils.getJson(br, "url");
                }
                if (inValidate(dllink)) {
                    /* Maybe user has direct downloads disabled */
                    dllink = getDllink();
                    if (inValidate(dllink)) {
                        throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                    }
                }
                logger.info("dllink = " + dllink);
                dl = new jd.plugins.BrowserAdapter().openDownload(br, link, dllink, resumes, chunks);
                if (!isValidDownloadConnection(dl.getConnection())) {
                    dl.getConnection().setAllowedResponseCodes(new int[] { dl.getConnection().getResponseCode() });
                    br.followConnection();
                    if (br.containsHTML("Download of file will start in")) {
                        dllink = br.getRegex("document\\.location\\.href\\s*=\\s*'(https?://.*?)'").getMatch(0);
                    } else {
                        dllink = null;
                    }
                    if (dllink == null) {
                        logger.warning("The final dllink seems not to be a file!");
                        handleGeneralServerErrors(account, link);
                        throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                    }
                    dl = new jd.plugins.BrowserAdapter().openDownload(br, link, dllink, resumes, chunks);
                    if (!isValidDownloadConnection(dl.getConnection())) {
                        dl.getConnection().setAllowedResponseCodes(new int[] { dl.getConnection().getResponseCode() });
                        logger.warning("The final dllink seems not to be a file!");
                        br.followConnection();
                        handleGeneralServerErrors(account, link);
                        throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                    }
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
    public int getMaxSimultanFreeDownloadNum() {
        return maxFree.get();
    }

    @Override
    public void reset() {
    }

    @Override
    public void resetDownloadlink(DownloadLink link) {
        super.resetLink(link);
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