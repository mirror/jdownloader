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

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import jd.PluginWrapper;
import jd.config.Property;
import jd.http.Browser;
import jd.http.Cookie;
import jd.http.Cookies;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.plugins.Account;
import jd.plugins.AccountInfo;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;
import jd.utils.JDUtilities;

import org.appwork.utils.formatter.SizeFormatter;
import org.appwork.utils.formatter.TimeFormatter;

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "datei.to", "sharebase.to" }, urls = { "http://(www\\.)?datei\\.to/(datei/[A-Za-z0-9]+\\.html|\\?[A-Za-z0-9]+)", "blablablaInvalid_regexbvj54zjhrß96ujß" }, flags = { 2, 0 })
public class DateiTo extends PluginForHost {

    private static final String  APIPAGE                       = "http://datei.to/api/jdownloader/";
    private static AtomicInteger maxPrem                       = new AtomicInteger(1);
    private static final String  MAINPAGE                      = "http://datei.to";
    private static final String  NICE_HOST                     = "datei.to";

    // Limit stuff
    private static final boolean FREE_RESUME_API               = false;
    private static final int     FREE_MAXCHUNKS_API            = 1;
    private static final int     FREE_MAXDOWNLOADS_API         = 1;

    private static final boolean FREE_RESUME_WEB               = true;
    private static final int     FREE_MAXCHUNKS_WEB            = 0;
    private static final int     FREE_MAXDOWNLOADS_WEB         = 20;

    private static final boolean ACCOUNT_FREE_RESUME_API       = false;
    private static final int     ACCOUNT_FREE_MAXCHUNKS_API    = 1;
    private static final int     ACCOUNT_FREE_MAXDOWNLOADS_API = 1;

    private static final boolean ACCOUNT_FREE_RESUME_WEB       = true;
    private static final int     ACCOUNT_FREE_MAXCHUNKS_WEB    = 0;
    private static final int     ACCOUNT_FREE_MAXDOWNLOADS_WEB = 20;

    private static final boolean ACCOUNT_PREMIUM_RESUME        = true;
    private static final int     ACCOUNT_PREMIUM_MAXCHUNKS     = 0;
    private static final int     ACCOUNT_PREMIUM_MAXDOWNLOADS  = 20;

    // Switches to enable/disable API
    private static final boolean LOGIN_API_GENERAL             = true;
    private static final boolean FREE_DOWNLOAD_API             = false;
    private static final boolean ACCOUNT_FREE_DOWNLOAD_API     = false;
    private static final boolean ACCOUNT_PREMIUM_DOWNLOAD_API  = true;

    private static final String  NOCHUNKS                      = "NOCHUNKS";

    public DateiTo(PluginWrapper wrapper) {
        super(wrapper);
        this.enablePremium("http://datei.to/premium");
    }

    @Override
    public String getAGBLink() {
        return "http://datei.to/agb";
    }

    // do not add @Override here to keep 0.* compatibility
    public boolean hasAutoCaptcha() {
        return true;
    }

    // do not add @Override here to keep 0.* compatibility
    public boolean hasCaptcha() {
        return true;
    }

    @Override
    public void correctDownloadLink(final DownloadLink link) throws MalformedURLException {
        String id = new Regex(link.getDownloadURL(), "([A-Za-z0-9]+)\\.html$").getMatch(0);
        if (id != null) link.setUrlDownload("http://datei.to/?" + id);
    }

    private void prepbrowser_api(final Browser br) {
        br.getHeaders().put("User-Agent", "JDownloader");
    }

    private void prepbrowser_web(final Browser br) {
        br.getHeaders().put("User-Agent", "Mozilla/5.0 (Windows NT 6.1; WOW64; rv:27.0) Gecko/20100101 Firefox/27.0");
    }

    @Override
    public boolean checkLinks(final DownloadLink[] urls) {
        if (urls == null || urls.length == 0) { return false; }
        try {
            final Browser br = new Browser();
            br.setCookiesExclusive(true);
            prepbrowser_api(br);
            final StringBuilder sb = new StringBuilder();
            final ArrayList<DownloadLink> links = new ArrayList<DownloadLink>();
            int index = 0;
            while (true) {
                links.clear();
                while (true) {
                    /* we test 50 links at once */
                    if (index == urls.length || links.size() > 50) {
                        break;
                    }
                    links.add(urls[index]);
                    index++;
                }
                sb.delete(0, sb.capacity());
                sb.append("op=check&file=");
                for (final DownloadLink dl : links) {
                    correctDownloadLink(dl);
                    sb.append(getFID(dl));
                    sb.append(";");
                }
                br.postPage(APIPAGE, sb.toString());
                for (final DownloadLink dllink : links) {
                    final String fid = getFID(dllink);
                    final String[] linkInfo = br.getRegex(fid + ";([^;]+);([^<>\"/;]*?);(\\d+)").getRow(0);
                    if (linkInfo == null) {
                        logger.warning("Linkchecker for datei.to is broken!");
                        return false;
                    }
                    if (!"online".equalsIgnoreCase(linkInfo[1]))
                        dllink.setAvailable(false);
                    else
                        dllink.setAvailable(true);
                    if (linkInfo[1] != null) dllink.setFinalFileName(Encoding.htmlDecode(linkInfo[1]));
                    if (linkInfo[2] != null) dllink.setDownloadSize(Long.parseLong(linkInfo[2]));
                }
                if (index == urls.length) {
                    break;
                }
            }
        } catch (final Exception e) {
            return false;
        }
        return true;
    }

    private String getFID(final DownloadLink dl) {
        return new Regex(dl.getDownloadURL(), "([A-Za-z0-9]+)$").getMatch(0);
    }

    private String err_temp_unvail = ">Die Datei steht aus technischen Gründen leider nicht zur Verfügung\\.";

    @Override
    public AvailableStatus requestFileInformation(final DownloadLink downloadLink) throws Exception {
        if (FREE_DOWNLOAD_API) {
            checkLinks(new DownloadLink[] { downloadLink });
            if (!downloadLink.isAvailabilityStatusChecked()) { return AvailableStatus.UNCHECKED; }
            if (downloadLink.isAvailabilityStatusChecked() && !downloadLink.isAvailable()) { throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND); }
            return AvailableStatus.TRUE;
        } else {
            prepbrowser_web(br);
            br.getPage(downloadLink.getDownloadURL());
            if (br.containsHTML("<div id=\"Name\">Diese Datei existiert nicht mehr...</div>")) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            String fname = br.getRegex("<div id=\"Name\">(.*?)</div>").getMatch(0);
            String fsize = br.getRegex("<div id=\"Details\">.*?(\\d+,\\d+ ?(B|KB|MB|GB))</div>").getMatch(0);
            if (fname != null) downloadLink.setName(fname);
            if (fsize != null) downloadLink.setDownloadSize(SizeFormatter.getSize(fsize));
            if (br.containsHTML(err_temp_unvail)) return AvailableStatus.UNCHECKABLE;
            return AvailableStatus.TRUE;
        }
    }

    @Override
    public void handleFree(final DownloadLink downloadLink) throws Exception {
        requestFileInformation(downloadLink);
        if (FREE_DOWNLOAD_API) {
            doFree_api(downloadLink, DateiTo.FREE_RESUME_API, DateiTo.FREE_MAXCHUNKS_API);
        } else {
            doFree_web(downloadLink, DateiTo.FREE_RESUME_WEB, DateiTo.FREE_MAXCHUNKS_WEB);
        }
    }

    private void doFree_api(final DownloadLink downloadLink, final boolean resume, final int maxchunks) throws Exception {
        br.postPage(APIPAGE, "op=free&step=1&file=" + getFID(downloadLink));
        generalAPIErrorhandling();
        generalFreeAPIErrorhandling();
        final Regex waitAndID = br.getRegex("(\\d+);([A-Za-z0-9]+)");
        if (waitAndID.getMatches().length != 1) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        this.sleep(Long.parseLong(waitAndID.getMatch(0)) * 1001l, downloadLink);
        final String id = waitAndID.getMatch(1);

        for (int i = 1; i <= 5; i++) {
            br.postPage(APIPAGE, "op=free&step=2&id=" + id);
            final String reCaptchaId = br.toString().trim();
            if (reCaptchaId == null) {
                logger.warning("reCaptchaId is null...");
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            final PluginForHost recplug = JDUtilities.getPluginForHost("DirectHTTP");
            final jd.plugins.hoster.DirectHTTP.Recaptcha rc = ((DirectHTTP) recplug).getReCaptcha(br);
            rc.setId(reCaptchaId);
            rc.load();
            final File cf = rc.downloadCaptcha(getLocalCaptchaFile());
            final String c = getCaptchaCode(cf, downloadLink);
            br.postPage(APIPAGE, "op=free&step=3&id=" + id + "&recaptcha_response_field=" + Encoding.urlEncode(c) + "&recaptcha_challenge_field=" + rc.getChallenge());
            if (!br.containsHTML("(wrong|no input)") && br.containsHTML("ok")) {
                break;
            }
        }
        if (br.containsHTML("(wrong|no input)")) { throw new PluginException(LinkStatus.ERROR_CAPTCHA); }

        br.postPage(APIPAGE, "op=free&step=4&id=" + id);
        generalFreeAPIErrorhandling();
        if (br.containsHTML("ticket expired")) throw new PluginException(LinkStatus.ERROR_RETRY, "Downloadticket expired");
        String dlUrl = br.toString();
        if (dlUrl == null || !dlUrl.startsWith("http") || dlUrl.length() > 500 || dlUrl.contains("no file")) throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_DISABLE);
        dlUrl = dlUrl.trim();

        dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, dlUrl, resume, maxchunks);
        if (dl.getConnection() == null || dl.getConnection().getContentType().contains("html")) {
            logger.warning("The dllink doesn't seem to be a file...");
            br.followConnection();
            // Shouldn't happen often
            handleServerErrors();
            if (br.containsHTML("(window\\.location\\.href=\\'http://datei\\.to/login|form id=\"UploadForm\")")) throw new PluginException(LinkStatus.ERROR_IP_BLOCKED, 60 * 60 * 1001l);
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        dl.startDownload();
    }

    private void doFree_web(final DownloadLink downloadLink, boolean resume, int maxchunks) throws Exception {
        br.getPage(downloadLink.getDownloadURL());
        if (br.containsHTML(err_temp_unvail)) throw new PluginException(LinkStatus.ERROR_HOSTER_TEMPORARILY_UNAVAILABLE, "File is unavailable for technical reasons ", 10 * 60 * 1000l);

        final String dlid = br.getRegex("<button id=\"([AS-Za-z0-9]+)\"").getMatch(0);
        if (dlid == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        br.postPage("http://datei.to/response/download", "Step=1&ID=" + dlid);
        if (br.containsHTML(">Ansonsten musst du warten, bis der aktuelle Download beendet ist")) throw new PluginException(LinkStatus.ERROR_HOSTER_TEMPORARILY_UNAVAILABLE, "Too many free downloads are active, please wait before starting new ones", 5 * 60 * 1000l);

        final Regex reconWait = br.getRegex("Du musst noch <strong>(\\d+):(\\d+) min</strong> warten");
        final String reconMin = reconWait.getMatch(0);
        final String reconSecs = reconWait.getMatch(1);
        if (reconMin != null && reconSecs != null) throw new PluginException(LinkStatus.ERROR_IP_BLOCKED, Integer.parseInt(reconMin) * 60 * 1000l + Integer.parseInt(reconSecs) * 1001l);

        String dllink = br.getRegex("iframe src=\"(http://[^<>\"]*?)\"").getMatch(0);
        if (dllink == null) {
            final String waittime = br.getRegex("id=\"DCS\">(\\d+)</span> Sekunden").getMatch(0);
            if (waittime == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            this.sleep(Integer.parseInt(waittime) * 1001l, downloadLink);
            for (int i = 1; i <= 5; i++) {
                br.postPage("http://datei.to/response/download", "Step=2&ID=" + dlid);
                final PluginForHost recplug = JDUtilities.getPluginForHost("DirectHTTP");
                final jd.plugins.hoster.DirectHTTP.Recaptcha rc = ((DirectHTTP) recplug).getReCaptcha(br);
                rc.setId("6LdBbL8SAAAAAI0vKUo58XRwDd5Tu_Ze1DA7qTao");
                rc.load();
                final File cf = rc.downloadCaptcha(getLocalCaptchaFile());
                final String c = getCaptchaCode(cf, downloadLink);
                br.postPage("http://datei.to/response/recaptcha", "modul=checkDLC&recaptcha_response_field=" + Encoding.urlEncode(c) + "&recaptcha_challenge_field=" + rc.getChallenge() + "&ID=" + dlid);
                if (br.containsHTML("Eingabe war leider falsch")) continue;
                break;
            }
            if (br.containsHTML("Eingabe war leider falsch")) { throw new PluginException(LinkStatus.ERROR_CAPTCHA); }
            if (br.containsHTML("Das Download\\-Ticket ist abgelaufen")) throw new PluginException(LinkStatus.ERROR_RETRY, "Downloadticket expired");
            br.postPage("http://datei.to/response/download", "Step=3&ID=" + dlid);
            dllink = br.getRegex("iframe src=\"(http://[^<>\"]*?)\"").getMatch(0);
            if (dllink == null) {
                logger.warning(NICE_HOST + ": dllink is null");
            }
            // Limited if we have waittime & captcha
            resume = false;
            maxchunks = 1;
        }
        br.setFollowRedirects(true);

        if (downloadLink.getBooleanProperty(NOCHUNKS, false)) maxchunks = 1;

        dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, dllink, resume, maxchunks);
        if (dl.getConnection() == null || dl.getConnection().getContentType().contains("html")) {
            logger.warning("The dllink doesn't seem to be a file...");
            br.followConnection();
            if (br.containsHTML("error/ticketexpired\\'")) {
                logger.info("error/ticketexpired --> Retrying");
                throw new PluginException(LinkStatus.ERROR_RETRY, "Ticket expired");
            }
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        try {
            if (!this.dl.startDownload()) {
                try {
                    if (dl.externalDownloadStop()) return;
                } catch (final Throwable e) {
                }
                /* unknown error, we disable multiple chunks */
                if (downloadLink.getBooleanProperty(DateiTo.NOCHUNKS, false) == false) {
                    downloadLink.setProperty(DateiTo.NOCHUNKS, Boolean.valueOf(true));
                    throw new PluginException(LinkStatus.ERROR_RETRY);
                }
            }
        } catch (final PluginException e) {
            // New V2 errorhandling
            /* unknown error, we disable multiple chunks */
            if (e.getLinkStatus() != LinkStatus.ERROR_RETRY && downloadLink.getBooleanProperty(DateiTo.NOCHUNKS, false) == false) {
                downloadLink.setProperty(DateiTo.NOCHUNKS, Boolean.valueOf(true));
                throw new PluginException(LinkStatus.ERROR_RETRY);
            }
        }
    }

    private void generalAPIErrorhandling() throws PluginException {
        if (br.containsHTML("temp down")) throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server error, file is temporarily not downloadable!", 30 * 60 * 1000l);
        if (br.containsHTML("no file")) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
    }

    private void generalFreeAPIErrorhandling() throws NumberFormatException, PluginException {
        if (br.containsHTML("limit reached")) throw new PluginException(LinkStatus.ERROR_IP_BLOCKED, Long.parseLong(br.getRegex("limit reached;(\\d+)").getMatch(0)));
        if (br.containsHTML("download active")) throw new PluginException(LinkStatus.ERROR_HOSTER_TEMPORARILY_UNAVAILABLE, "Too many free downloads are active, please wait before starting new ones", 5 * 60 * 1000l);
    }

    private void handleServerErrors() throws PluginException {
        if (br.containsHTML("datei\\.to/error/notfound")) throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server error", 60 * 60 * 1000l);
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        if (FREE_DOWNLOAD_API) {
            return DateiTo.FREE_MAXDOWNLOADS_API;
        } else {
            return DateiTo.FREE_MAXDOWNLOADS_WEB;
        }
    }

    @Override
    public AccountInfo fetchAccountInfo(final Account account) throws Exception {
        final AccountInfo ai = new AccountInfo();
        /* reset maxPrem workaround on every fetchaccount info */
        maxPrem.set(1);
        if (LOGIN_API_GENERAL) {
            try {
                login_api(account);
            } catch (final PluginException e) {
                account.setValid(false);
                throw e;
            }
            if (account.getBooleanProperty("isPremium")) {
                final Regex accInfo = br.getRegex("premium;(\\d+);(\\d+)");
                ai.setValidUntil(System.currentTimeMillis() + Long.parseLong(accInfo.getMatch(0)) * 1000l);
                ai.setTrafficLeft(Long.parseLong(accInfo.getMatch(1)));
            }
            if (!account.getBooleanProperty("isPremium", false)) {
                try {
                    maxPrem.set(DateiTo.ACCOUNT_FREE_MAXDOWNLOADS_API);
                    // free accounts can still have captcha.
                    account.setMaxSimultanDownloads(maxPrem.get());
                    account.setConcurrentUsePossible(false);
                } catch (final Throwable e) {
                    // not available in old Stable 0.9.581
                }
                ai.setStatus("Registered (free) user");
            } else {
                try {
                    maxPrem.set(ACCOUNT_PREMIUM_MAXDOWNLOADS);
                    account.setMaxSimultanDownloads(maxPrem.get());
                    account.setConcurrentUsePossible(true);
                } catch (final Throwable e) {
                    // not available in old Stable 0.9.581
                }
                ai.setStatus("Premium user");
            }
        } else {
            try {
                login_web(account, true);
            } catch (final PluginException e) {
                account.setValid(false);
                throw e;
            }
            br.getPage("/konto");
            final String accountType = br.getRegex(">Konto\\-Typ:</div><div[^>]+><span[^>]+>(.*?)\\-Account</span>").getMatch(0);
            if (accountType != null && accountType.equals("Premium") || account.getBooleanProperty("isPremium", false)) {
                account.setProperty("isPremium", true);
                // premium account
                final String space = br.getRegex(">loadSpaceUsed\\(\\d+, (\\d+)").getMatch(0);
                if (space != null) ai.setUsedSpace(space + " GB");
                final String traffic = br.getRegex("loadTrafficUsed\\((\\d+(\\.\\d+))").getMatch(0);
                if (traffic != null) ai.setTrafficLeft(SizeFormatter.getSize(traffic + " GB"));
                final String expire = br.getRegex(">Premium aktiv bis:</div><div[^>]+>(\\d{2}\\.\\d{2}\\.\\d{4} \\d{2}:\\d{2}:\\d{2}) Uhr<").getMatch(0);
                ai.setValidUntil(TimeFormatter.getMilliSeconds(expire, "dd.MM.yyyy hh:mm:ss", Locale.ENGLISH));
            }
            if (!account.getBooleanProperty("isPremium", false)) {
                try {
                    maxPrem.set(DateiTo.ACCOUNT_FREE_MAXDOWNLOADS_WEB);
                    // free accounts can still have captcha.
                    account.setMaxSimultanDownloads(maxPrem.get());
                    account.setConcurrentUsePossible(false);
                } catch (final Throwable e) {
                    // not available in old Stable 0.9.581
                }
                ai.setStatus("Registered (free) user");
            } else {
                try {
                    maxPrem.set(DateiTo.ACCOUNT_PREMIUM_MAXDOWNLOADS);
                    account.setMaxSimultanDownloads(maxPrem.get());
                    account.setConcurrentUsePossible(true);
                } catch (final Throwable e) {
                    // not available in old Stable 0.9.581
                }
                ai.setStatus("Premium user");
            }
        }
        return ai;
    }

    @Override
    public int getMaxSimultanPremiumDownloadNum() {
        return maxPrem.get();
    }

    public void login_api(Account account) throws IOException, PluginException {
        this.setBrowserExclusive();
        prepbrowser_api(br);
        br.postPage(APIPAGE, "op=login&user=" + Encoding.urlEncode(account.getUser()) + "&pass=" + Encoding.urlEncode(account.getPass()));

        final String lang = System.getProperty("user.language");
        if (br.containsHTML("wrong login")) {
            if ("de".equalsIgnoreCase(lang)) {
                throw new PluginException(LinkStatus.ERROR_PREMIUM, "\r\nUngültiger Benutzername oder ungültiges Passwort!\r\nSchnellhilfe: \r\nDu bist dir sicher, dass dein eingegebener Benutzername und Passwort stimmen?\r\nFalls dein Passwort Sonderzeichen enthält, ändere es und versuche es erneut!", PluginException.VALUE_ID_PREMIUM_DISABLE);
            } else {
                throw new PluginException(LinkStatus.ERROR_PREMIUM, "\r\nInvalid username/password!\r\nQuick help:\r\nYou're sure that the username and password you entered are correct?\r\nIf your password contains special characters, change it (remove them) and try again!", PluginException.VALUE_ID_PREMIUM_DISABLE);
            }
        }
        if (!br.containsHTML("premium;")) {
            account.setProperty("isPremium", false);
        } else {
            account.setProperty("isPremium", true);
        }
    }

    public void login_web(final Account account, final boolean force) throws Exception {
        this.setBrowserExclusive();
        try {
            /** Load cookies */
            br.setCookiesExclusive(true);
            final Object ret = account.getProperty("cookies", null);
            boolean acmatch = Encoding.urlEncode(account.getUser()).equals(account.getStringProperty("name", Encoding.urlEncode(account.getUser())));
            if (acmatch) acmatch = Encoding.urlEncode(account.getPass()).equals(account.getStringProperty("pass", Encoding.urlEncode(account.getPass())));
            if (acmatch && ret != null && ret instanceof HashMap<?, ?> && !force) {
                final HashMap<String, String> cookies = (HashMap<String, String>) ret;
                if (account.isValid()) {
                    for (final Map.Entry<String, String> cookieEntry : cookies.entrySet()) {
                        final String key = cookieEntry.getKey();
                        final String value = cookieEntry.getValue();
                        this.br.setCookie(this.getHost(), key, value);
                    }
                    return;
                }
            }
            br.setFollowRedirects(false);
            prepbrowser_web(this.br);
            br.postPage("http://datei.to/response/login", "Login_User=" + Encoding.urlEncode(account.getUser()) + "&Login_Pass=" + Encoding.urlEncode(account.getPass()));
            final String lang = System.getProperty("user.language");
            if (br.getCookie(this.getHost(), "User") == null || br.getCookie(this.getHost(), "Pass") == null) {
                if ("de".equalsIgnoreCase(lang)) {
                    throw new PluginException(LinkStatus.ERROR_PREMIUM, "\r\nUngültiger Benutzername oder ungültiges Passwort!\r\nSchnellhilfe: \r\nDu bist dir sicher, dass dein eingegebener Benutzername und Passwort stimmen?\r\nFalls dein Passwort Sonderzeichen enthält, ändere es und versuche es erneut!", PluginException.VALUE_ID_PREMIUM_DISABLE);
                } else {
                    throw new PluginException(LinkStatus.ERROR_PREMIUM, "\r\nInvalid username/password!\r\nQuick help:\r\nYou're sure that the username and password you entered are correct?\r\nIf your password contains special characters, change it (remove them) and try again!", PluginException.VALUE_ID_PREMIUM_DISABLE);
                }
            }
            /** Save cookies */
            final HashMap<String, String> cookies = new HashMap<String, String>();
            final Cookies add = this.br.getCookies(this.getHost());
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

    @Override
    public void handlePremium(final DownloadLink downloadLink, final Account account) throws Exception {
        requestFileInformation(downloadLink);
        if (!account.getBooleanProperty("isPremium")) {
            if (DateiTo.ACCOUNT_FREE_DOWNLOAD_API) {
                doFree_api(downloadLink, DateiTo.ACCOUNT_FREE_RESUME_API, DateiTo.ACCOUNT_FREE_MAXCHUNKS_API);
            } else {
                this.login_web(account, false);
                doFree_web(downloadLink, DateiTo.ACCOUNT_FREE_RESUME_WEB, DateiTo.ACCOUNT_FREE_MAXCHUNKS_WEB);
            }
        } else {
            if (DateiTo.ACCOUNT_PREMIUM_DOWNLOAD_API) {
                handlePremium_api(downloadLink, account);
            } else {
                this.login_web(account, false);
                handlePremium_web(downloadLink, account);
            }
        }
    }

    private void handlePremium_api(final DownloadLink downloadLink, final Account account) throws Exception {
        br.postPage(APIPAGE, "op=premium&user=" + Encoding.urlEncode(account.getUser()) + "&pass=" + Encoding.urlEncode(account.getPass()) + "&file=" + getFID(downloadLink));
        generalAPIErrorhandling();
        if (br.containsHTML("no premium")) {
            logger.info("Cannot start download, this is no premium account anymore...");
            throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_DISABLE);
        }
        if (br.containsHTML("wrong login")) {
            logger.info("Cannot start download, username or password wrong!");
            throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_DISABLE);
        }
        String dlUrl = br.toString();
        if (dlUrl == null || !dlUrl.startsWith("http") || dlUrl.length() > 500 || dlUrl.contains("no file")) throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_DISABLE);
        dlUrl = dlUrl.trim();
        dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, dlUrl, ACCOUNT_PREMIUM_RESUME, ACCOUNT_PREMIUM_MAXCHUNKS);
        br.setFollowRedirects(true);
        if (dl.getConnection() == null || dl.getConnection().getContentType().contains("html")) {
            br.followConnection();
            handleServerErrors();
            logger.severe("PremiumError: " + br.toString());
            throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_DISABLE);
        }
        dl.startDownload();
    }

    private void handlePremium_web(final DownloadLink downloadLink, final Account account) throws Exception {
        login_web(account, false);
        br.setFollowRedirects(false);
        // direct downloads
        br.getPage(downloadLink.getDownloadURL());
        String dllink = br.getRedirectLocation();
        if (dllink == null || !dllink.matches("(https?://\\w+\\.datei\\.to/file/[a-z0-9]{32}/[A-Za-z0-9]{8}/[A-Za-z0-9]{10}/[^\"\\']+)")) {
            // direct download failed to match or disabled feature in users
            // profile
            String id = br.getRegex("<button id=\"([^\"]+)\">Download starten<").getMatch(0);
            if (id == null) {
                logger.warning("'id' could not be found");
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            br.postPage("/response/download", "Step=1&ID=" + id);
            dllink = br.getRegex("(https?://\\w+\\.datei\\.to/dl/[A-Za-z0-9]+)").getMatch(0);
            br.setFollowRedirects(true);
        }
        if (dllink == null) {
            logger.warning("Could not find dllink");
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, dllink, ACCOUNT_PREMIUM_RESUME, ACCOUNT_PREMIUM_MAXCHUNKS);
        if (dl.getConnection().getContentType().contains("html")) {
            br.followConnection();
            handleServerErrors();
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        dl.startDownload();
    }

    @Override
    public void reset() {
    }

    @Override
    public void resetDownloadlink(DownloadLink link) {
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