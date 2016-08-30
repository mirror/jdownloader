//    jDownloader - Downloadmanager
//    Copyright (C) 2008  JD-Team support@jdownloader.org
//
//    This program is free software: you can redistribute it and/or modify
//    it under the terms of the GNU General Public License as published by
//    the Free Software Foundation, either version 3 of the License, or
//    (at your option) any later version.
//
//    This program is distributed in the hope that it will be useful,
//    but WITHOUT ANY WARRANTY; without even the implied warranty of
//    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
//    GNU General Public License for more details.
//
//    You should have received a copy of the GNU General Public License
//    along with this program.  If not, see <http://www.gnu.org/licenses/>.

package jd.plugins.hoster;

import java.io.File;
import java.io.IOException;

import jd.PluginWrapper;
import jd.config.Property;
import jd.http.Browser;
import jd.http.Cookies;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.parser.html.Form;
import jd.plugins.Account;
import jd.plugins.Account.AccountType;
import jd.plugins.AccountInfo;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;
import jd.utils.locale.JDL;

import org.jdownloader.captcha.v2.challenge.recaptcha.v1.Recaptcha;

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "filer.net" }, urls = { "https?://(www\\.)?filer\\.net/(get|dl)/[a-z0-9]+" })
public class FilerNet extends PluginForHost {

    private static Object       LOCK                            = new Object();
    private int                 statusCode                      = 0;
    private String              fuid                            = null;
    private String              recapID                         = null;
    private static final int    APIDISABLED                     = 400;
    private static final String APIDISABLEDTEXT                 = "API is disabled, please wait or use filer.net from your browser";
    private static final int    DOWNLOADTEMPORARILYDISABLED     = 500;
    private static final String DOWNLOADTEMPORARILYDISABLEDTEXT = "Download temporarily disabled!";
    private static final int    UNKNOWNERROR                    = 599;
    private static final String UNKNOWNERRORTEXT                = "Unknown file error";
    private static final String NORESUME                        = "NORESUME";

    @SuppressWarnings("deprecation")
    public FilerNet(PluginWrapper wrapper) {
        super(wrapper);
        this.enablePremium("http://filer.net/upgrade");
        this.setStartIntervall(2000l);
    }

    @SuppressWarnings("deprecation")
    @Override
    public void correctDownloadLink(DownloadLink link) {
        link.setUrlDownload("http://filer.net/get/" + new Regex(link.getDownloadURL(), "([a-z0-9]+)$").getMatch(0));
    }

    private void prepBrowser() {
        br.setFollowRedirects(false);
        br.getHeaders().put("User-Agent", "JDownloader");
    }

    @Override
    public String getAGBLink() {
        return "http://filer.net/agb.htm";
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return 1;
    }

    @Override
    public int getMaxSimultanPremiumDownloadNum() {
        return 10;
    }

    @Override
    public int getTimegapBetweenConnections() {
        return 500;
    }

    @Override
    public AvailableStatus requestFileInformation(final DownloadLink link) throws IOException, PluginException {
        prepBrowser();
        fuid = getFID(link);
        if (fuid == null) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        callAPI("http://api.filer.net/api/status/" + fuid + ".json");
        if (statusCode == APIDISABLED) {
            link.getLinkStatus().setStatusText(APIDISABLEDTEXT);
            return AvailableStatus.UNCHECKABLE;
        } else if (statusCode == 505) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        } else if (statusCode == DOWNLOADTEMPORARILYDISABLED) {
            link.getLinkStatus().setStatusText(DOWNLOADTEMPORARILYDISABLEDTEXT);
        } else if (statusCode == UNKNOWNERROR) {
            link.getLinkStatus().setStatusText(UNKNOWNERRORTEXT);
            return AvailableStatus.UNCHECKABLE;
        }
        link.setFinalFileName(getJson("name", br.toString()));
        link.setDownloadSize(Long.parseLong(getJson("size", br.toString())));
        /* hash != md5, its the hash of fileID */
        link.setMD5Hash(null);
        return AvailableStatus.TRUE;
    }

    @Override
    public void handleFree(final DownloadLink downloadLink) throws Exception {
        doFree(downloadLink);
    }

    @SuppressWarnings({ "deprecation", "static-access" })
    public void doFree(final DownloadLink downloadLink) throws Exception {
        requestFileInformation(downloadLink);
        handleDownloadErrors();
        if (checkShowFreeDialog(getHost())) {
            showFreeDialog(getHost());
        }
        callAPI("http://filer.net/get/" + fuid + ".json");
        handleFreeErrorsAPI();
        int wait = getWait();
        final String token = getJson("token", br.toString());
        if (token == null) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }

        if (statusCode == 203) {
            sleep(wait * 1001l, downloadLink);
        }
        callAPI("http://filer.net/get/" + fuid + ".json?token=" + token);
        handleFreeErrorsAPI();
        String dllink = null;
        if (statusCode == 202) {
            int maxCaptchaTries = 5;
            int tries = 0;
            while (tries < maxCaptchaTries) {
                final Recaptcha rc = new Recaptcha(br, this);
                if (recapID == null) {
                    recapID = getJson("recaptcha_challange", br.toString());
                }
                if (recapID == null) {
                    throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                }
                rc.setId(recapID);
                rc.load();
                final File cf = rc.downloadCaptcha(getLocalCaptchaFile());
                final String c = getCaptchaCode("recaptcha", cf, downloadLink);
                tries++;
                br.postPage("http://filer.net/get/" + fuid + ".json", "recaptcha_challenge_field=" + Encoding.urlEncode(rc.getChallenge()) + "&recaptcha_response_field=" + Encoding.urlEncode(c) + "&hash=" + fuid);
                dllink = br.getRedirectLocation();
                if (dllink == null) {
                    updateStatuscode();
                    if (statusCode == 501) {
                        throw new PluginException(LinkStatus.ERROR_HOSTER_TEMPORARILY_UNAVAILABLE, "No free slots available, wait or buy premium!", 2 * 60 * 1000l);
                    } else if (statusCode == 502) {
                        throw new PluginException(LinkStatus.ERROR_HOSTER_TEMPORARILY_UNAVAILABLE, "Max free simultan-downloads-limit reached, please finish running downloads before starting new ones!", 1 * 60 * 1000l);
                    } else {
                        continue;
                    }
                } else {
                    break;
                }
            }
            if (dllink == null) {
                throw new PluginException(LinkStatus.ERROR_CAPTCHA);
            }
        }
        /* This should never happen! */
        if (dllink == null) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        boolean resume = true;
        if (downloadLink.getBooleanProperty(FilerNet.NORESUME, false)) {
            resume = false;
        }
        dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, dllink, resume, 1);
        if (dl.getConnection().getContentType().contains("html")) {
            br.followConnection();
            // Temporary errorhandling for a bug which isn't handled by the API
            if (br.getURL().equals("http://filer.net/error/500")) {
                throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Serverfehler", 60 * 60 * 1000l);
            }
            if (br.getURL().equals("http://filer.net/error/430") || br.containsHTML("Diese Adresse ist nicht bekannt oder")) {
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            }
            if (br.containsHTML("filer\\.net/register")) {
                throw new PluginException(LinkStatus.ERROR_HOSTER_TEMPORARILY_UNAVAILABLE, "No free slots available, wait or buy premium!", 10 * 60 * 1000l);
            }
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        dl.setAllowFilenameFromURL(true);
        if (!this.dl.startDownload()) {
            try {
                if (dl.externalDownloadStop()) {
                    return;
                }
            } catch (final Throwable e) {
            }
            if (downloadLink.getLinkStatus().getErrorMessage() != null && downloadLink.getLinkStatus().getErrorMessage().startsWith(JDL.L("download.error.message.rangeheaders", "Server does not support chunkload"))) {
                if (downloadLink.getBooleanProperty(FilerNet.NORESUME, false) == false) {
                    downloadLink.setChunksProgress(null);
                    downloadLink.setProperty(FilerNet.NORESUME, Boolean.valueOf(true));
                    throw new PluginException(LinkStatus.ERROR_RETRY);
                }
            }
        }
    }

    private int getWait() {
        final String tiaw = getJson("wait", br.toString());
        if (tiaw != null) {
            return Integer.parseInt(tiaw);
        } else {
            return 15;
        }
    }

    // TODO: remove this when v2 becomes stable. use br.getFormbyKey(String key, String value)
    /**
     * Returns the first form that has a 'key' that equals 'value'.
     *
     * @param key
     *            name
     * @param value
     *            expected value
     * @param ibr
     *            import browser
     */
    @SuppressWarnings("unused")
    private Form getFormByInput(final Browser ibr, final String key, final String value) {
        Form[] workaround = ibr.getForms();
        if (workaround != null) {
            for (Form f : workaround) {
                if (f.containsHTML(key + "=(\"|')" + value + "\\1")) {
                    return f;
                }
            }
        }
        return null;
    }

    private Browser prepBrowser(final Browser prepBr) {
        prepBr.setFollowRedirects(false);
        prepBr.getHeaders().put("Accept-Charset", null);
        prepBr.getHeaders().put("User-Agent", "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_9_1) AppleWebKit/537.73.11 (KHTML, like Gecko) Version/7.0.1 Safari/537.73.11");
        return prepBr;
    }

    // do not add @Override here to keep 0.* compatibility
    public boolean hasAutoCaptcha() {
        return true;
    }

    public void login(final Account account) throws IOException, PluginException, InterruptedException {
        synchronized (LOCK) {
            /** Load cookies */
            br.setCookiesExclusive(true);
            prepBrowser();
            br.getHeaders().put("Authorization", "Basic " + Encoding.Base64Encode(account.getUser() + ":" + account.getPass()));
            callAPI("http://api.filer.net/api/profile.json");
            if (br.getRedirectLocation() != null) {
                callAPI(br.getRedirectLocation());
            }
        }
    }

    /* E.g. used for free account downloads */
    private void loginWebsite(final Account account, final boolean force) throws Exception {
        synchronized (LOCK) {
            try {
                /* Load cookies */
                br.setCookiesExclusive(true);
                prepBrowser(br);
                final Cookies cookies = account.loadCookies("");
                if (cookies != null && !force) {
                    this.br.setCookies(this.getHost(), cookies);
                    return;
                }
                br.setFollowRedirects(true);
                this.br.getPage("https://filer.net/login");
                final Form loginform = this.br.getFormbyKey("_username");
                if (loginform == null) {
                    if ("de".equalsIgnoreCase(System.getProperty("user.language"))) {
                        throw new PluginException(LinkStatus.ERROR_PREMIUM, "\r\nPlugin defekt, bitte den JDownloader Support kontaktieren!", PluginException.VALUE_ID_PREMIUM_DISABLE);
                    } else {
                        throw new PluginException(LinkStatus.ERROR_PREMIUM, "\r\nPlugin broken, please contact the JDownloader Support!", PluginException.VALUE_ID_PREMIUM_DISABLE);
                    }
                }
                loginform.put("_username", Encoding.urlEncode(account.getUser()));
                loginform.put("_password", Encoding.urlEncode(account.getPass()));
                loginform.remove("_remember_me");
                loginform.put("_remember_me", "on");
                this.br.submitForm(loginform);
                if (br.getCookie(this.getHost(), "REMEMBERME") == null) {
                    if ("de".equalsIgnoreCase(System.getProperty("user.language"))) {
                        throw new PluginException(LinkStatus.ERROR_PREMIUM, "\r\nUngültiger Benutzername oder Passwort!\r\nDu bist dir sicher, dass dein eingegebener Benutzername und Passwort stimmen? Versuche folgendes:\r\n1. Falls dein Passwort Sonderzeichen enthält, ändere es (entferne diese) und versuche es erneut!\r\n2. Gib deine Zugangsdaten per Hand (ohne kopieren/einfügen) ein.", PluginException.VALUE_ID_PREMIUM_DISABLE);
                    } else {
                        throw new PluginException(LinkStatus.ERROR_PREMIUM, "\r\nInvalid username or password!\r\nYou're sure that the username and password you entered are correct? Some hints:\r\n1. If your password contains special characters, change it (remove them) and try again!\r\n2. Type in your username/password by hand without copy & paste.", PluginException.VALUE_ID_PREMIUM_DISABLE);
                    }
                }
                account.saveCookies(this.br.getCookies(this.getHost()), "");
            } catch (final PluginException e) {
                account.clearCookies("");
                throw e;
            }
        }
    }

    @SuppressWarnings("deprecation")
    @Override
    public AccountInfo fetchAccountInfo(final Account account) throws Exception {
        AccountInfo ai = new AccountInfo();
        this.setBrowserExclusive();
        try {
            login(account);
        } catch (PluginException e) {
            account.setValid(false);
            throw e;
        }
        if (getJson("state", br.toString()).equals("free")) {
            account.setType(AccountType.FREE);
            ai.setStatus("Free User");
            ai.setUnlimitedTraffic();
        } else {
            account.setType(AccountType.PREMIUM);
            ai.setStatus("Premium User");
            ai.setTrafficLeft(Long.parseLong(getJson("traffic", br.toString())));
            ai.setValidUntil(Long.parseLong(getJson("until", br.toString())) * 1000);
        }
        account.setValid(true);
        return ai;
    }

    @SuppressWarnings("deprecation")
    @Override
    public void handlePremium(final DownloadLink downloadLink, final Account account) throws Exception {
        setBrowserExclusive();
        if (account.getType() == AccountType.FREE) {
            loginWebsite(account, false);
            doFree(downloadLink);
        } else {
            requestFileInformation(downloadLink);
            handleDownloadErrors();
            br.getHeaders().put("Authorization", "Basic " + Encoding.Base64Encode(account.getUser() + ":" + account.getPass()));
            callAPI("http://filer.net/api/dl/" + fuid + ".json");
            if (statusCode == 504) {
                logger.info("No traffic available!");
                throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_TEMP_DISABLE);
            } else if (statusCode == UNKNOWNERROR) {
                throw new PluginException(LinkStatus.ERROR_FATAL, UNKNOWNERRORTEXT);
            }
            final String dllink = br.getRedirectLocation();
            if (dllink == null) {
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            /* Important!! */
            br.getHeaders().put("Authorization", "");
            boolean resume = true;
            if (downloadLink.getBooleanProperty(FilerNet.NORESUME, false)) {
                resume = false;
            }
            dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, dllink, resume, 1);
            if (dl.getConnection().getContentType().contains("html")) {
                br.followConnection();
                /**
                 * there error handling is fubared, the message is the same for all /error/\d+ <br />
                 * logs show they can be downloaded, at least in free mode test I've done -raztoki20160510 <br />
                 * just retry!
                 */
                // // Temporary errorhandling for a bug which isn't handled by the API
                // if (br.getURL().equals("http://filer.net/error/500")) {
                // throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Serverfehler", 60 * 60 * 1000l);
                // }
                // if (br.getURL().equals("http://filer.net/error/430") || br.containsHTML("Diese Adresse ist nicht bekannt oder")) {
                // throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
                // }
                if (br.getURL().matches(".+/error/\\d+")) {
                    final int failed = downloadLink.getIntegerProperty("errorFailure", 0) + 1;
                    downloadLink.setProperty("errorFailure", failed);
                    if (failed > 10) {
                        throw new PluginException(LinkStatus.ERROR_FATAL, "Retry count over 10, count=" + failed);
                    } else if (failed > 4) {
                        throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Hoster has issues, count=" + failed, 15 * 60 * 1000l);
                    } else {
                        throw new PluginException(LinkStatus.ERROR_RETRY);
                    }
                }
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            dl.setAllowFilenameFromURL(true);
            if (!this.dl.startDownload()) {
                try {
                    if (dl.externalDownloadStop()) {
                        return;
                    }
                } catch (final Throwable e) {
                }
                if (downloadLink.getLinkStatus().getErrorMessage() != null && downloadLink.getLinkStatus().getErrorMessage().startsWith(JDL.L("download.error.message.rangeheaders", "Server does not support chunkload"))) {
                    if (downloadLink.getBooleanProperty(FilerNet.NORESUME, false) == false) {
                        downloadLink.setChunksProgress(null);
                        downloadLink.setProperty(FilerNet.NORESUME, Boolean.valueOf(true));
                        throw new PluginException(LinkStatus.ERROR_RETRY);
                    }
                }
            }
        }
    }

    private void handleDownloadErrors() throws PluginException {
        if (statusCode == APIDISABLED) {
            throw new PluginException(LinkStatus.ERROR_HOSTER_TEMPORARILY_UNAVAILABLE, APIDISABLEDTEXT, 2 * 60 * 60 * 1000l);
        } else if (statusCode == DOWNLOADTEMPORARILYDISABLED) {
            throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, DOWNLOADTEMPORARILYDISABLEDTEXT, 2 * 60 * 60 * 1000l);
        } else if (statusCode == UNKNOWNERROR) {
            throw new PluginException(LinkStatus.ERROR_FATAL, UNKNOWNERRORTEXT);
        }
    }

    private void handleFreeErrorsAPI() throws PluginException {
        if (statusCode == 501) {
            throw new PluginException(LinkStatus.ERROR_HOSTER_TEMPORARILY_UNAVAILABLE, "No free slots available, wait or buy premium!", 2 * 60 * 1000l);
        } else if (statusCode == 502) {
            throw new PluginException(LinkStatus.ERROR_HOSTER_TEMPORARILY_UNAVAILABLE, "Max free simultan-downloads-limit reached, please finish running downloads before starting new ones!", 1 * 60 * 1000l);
        }
        // 203 503 wait
        int wait = getWait();
        if (statusCode == 503) {
            // Waittime too small->Don't reconnect
            if (wait < 61) {
                throw new PluginException(LinkStatus.ERROR_HOSTER_TEMPORARILY_UNAVAILABLE, "Wait before starting new downloads...", wait * 1001l);
            }
            throw new PluginException(LinkStatus.ERROR_IP_BLOCKED, wait * 1001l);
        }
    }

    @SuppressWarnings("deprecation")
    private String getFID(final DownloadLink link) {
        return new Regex(link.getDownloadURL(), "([a-z0-9]+)$").getMatch(0);
    }

    private void callAPI(final String url) throws IOException {
        br.getPage(url);
        updateStatuscode();
    }

    private void updateStatuscode() {
        final String code = getJson("code", br.toString());
        if ("hour download limit reached".equals(code)) {
            statusCode = 503;
        } else if ("user download slots filled".equals(code)) {
            statusCode = 501; // unsure if 501 or 502
        } else if ("file captcha input needed".equals(code)) {
            statusCode = 202;
        } else if ("file wait needed".equals(code)) {
            statusCode = 203;
        } else if (code != null) {
            statusCode = Integer.parseInt(code);
        }
    }

    private String getJson(final String parameter, final String source) {
        String result = new Regex(source, "\"" + parameter + "\":(\\d+)").getMatch(0);
        if (result == null) {
            result = new Regex(source, "\"" + parameter + "\":\"([^<>\"]*?)\"").getMatch(0);
        }
        return result;
    }

    @Override
    public void reset() {
    }

    @Override
    public void resetDownloadlink(final DownloadLink link) {
        link.setProperty("errorFailure", Property.NULL);
    }

    @Override
    public void resetPluginGlobals() {
    }

    /* NO OVERRIDE!! We need to stay 0.9*compatible */
    public boolean hasCaptcha(DownloadLink link, jd.plugins.Account acc) {
        if (acc == null) {
            /* no account, yes we can expect captcha */
            return true;
        }
        if (Boolean.TRUE.equals(acc.getBooleanProperty("free"))) {
            /* free accounts also have captchas */
            return true;
        }
        return false;
    }
}