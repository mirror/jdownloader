//jDownloader - Downloadmanager
//Copyright (C) 2010  JD-Team support@jdownloader.org
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

import java.awt.Color;
import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import javax.swing.JComponent;
import javax.swing.JLabel;

import org.appwork.swing.MigPanel;
import org.appwork.swing.components.ExtTextField;
import org.appwork.utils.StringUtils;
import org.appwork.utils.formatter.SizeFormatter;
import org.jdownloader.gui.InputChangedCallbackInterface;
import org.jdownloader.plugins.accounts.AccountBuilderInterface;

import jd.PluginWrapper;
import jd.config.ConfigContainer;
import jd.config.ConfigEntry;
import jd.config.Property;
import jd.http.Browser;
import jd.http.Cookie;
import jd.http.Cookies;
import jd.http.URLConnectionAdapter;
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
import jd.plugins.PluginForHost;

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "emuparadise.me" }, urls = { "https?://(www\\.)?emuparadise\\.me/[^<>/]+/[^<>/]+/(?:\\d{4,}-download-\\d+|\\d{4,})" })
public class EmuParadiseMe extends PluginForHost {

    public EmuParadiseMe(PluginWrapper wrapper) {
        super(wrapper);
        enablePremium();
        setConfigElements();
    }

    private void setConfigElements() {
        getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_COMBOBOX_INDEX, getPluginConfig(), "Download Servers! Note: Just because you select a region doesn't ensure data actually comes from that location", new String[] { "Off", "Auto", "Europe", "North America" }, "Choose default download server").setDefaultValue(0));
    }

    @Override
    public AccountBuilderInterface getAccountFactory(InputChangedCallbackInterface callback) {
        return new EmuParadiseMeAccountFactory(callback);
    }

    @Override
    public String getAGBLink() {
        return "http://www.emuparadise.me/contact.php";
    }

    private static Object        LOCK                         = new Object();
    private static final String  COOKIE_HOST                  = "http://emuparadise.me/";
    private static AtomicInteger maxFree                      = new AtomicInteger(1);

    /* Connection stuff */
    private final boolean        FREE_RESUME                  = true;
    private final int            FREE_MAXCHUNKS               = 1;
    private final int            FREE_MAXDOWNLOADS            = 2;
    private final boolean        ACCOUNT_FREE_RESUME          = true;
    private final int            ACCOUNT_FREE_MAXCHUNKS       = 1;
    private final int            ACCOUNT_FREE_MAXDOWNLOADS    = 2;
    private final boolean        ACCOUNT_PREMIUM_RESUME       = true;
    private final int            ACCOUNT_PREMIUM_MAXCHUNKS    = -4;
    private final int            ACCOUNT_PREMIUM_MAXDOWNLOADS = 4;

    public final String          subURL                       = "(?:https?:)?(?://(?:www\\.)?emuparadise\\.me)?/[^<>/]+/[^<>/]+/\\d{4,}-download-\\d+";
    private boolean              isSubURL                     = false;

    /*
     * note: this is on every bloody page, but format is slightly different.
     */
    /* Books, movies and specials are "directlinks" (no captcha/waittime) */
    private final String         HTML_TYPE_DIRECT             = "Direct Download:</h2>";

    @SuppressWarnings({ "unchecked", "deprecation" })
    @Override
    public AvailableStatus requestFileInformation(final DownloadLink link) throws Exception {
        final String url = Encoding.htmlDecode(link.getPluginPatternMatcher());
        isSubURL = new Regex(url, subURL).matches();
        link.setName(new Regex(url, "emuparadise\\.me/[^<>/]+/([^<>/]+)/").getMatch(0) + ".zip");
        this.setBrowserExclusive();
        br.setFollowRedirects(true);
        prepBrowser();
        setCookies();
        br.getPage(url);
        if (offlineCheck()) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        String filename = null;
        String filesize = null;
        if (isSubURL || br.containsHTML(HTML_TYPE_DIRECT)) {
            final Regex result = new Regex(br, ">Download\\s*(.*?)</a><font[^>]+>\\s*-\\s*File Size:\\s*(\\d+(?:\\.\\d+)?[KMG]{1}[B]{0,1})</font>");
            filename = result.getMatch(0);
            filesize = result.getMatch(1);
        } else {
            if (!br.containsHTML("id=\"Download\"")) {
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            }
            filename = getFileName();
            filesize = getFileSize();
        }
        if (false && filename == null) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        if (filename != null) {
            link.setName(encodeUnicode(Encoding.htmlDecode(filename.trim())) + ".zip");
        }
        link.setDownloadSize(SizeFormatter.getSize(correctFilesize(filesize)));
        return AvailableStatus.TRUE;
    }

    public String correctFilesize(String fileSize) {
        if (fileSize != null && !fileSize.trim().endsWith("B")) {
            fileSize = fileSize.trim() + "B";
        }
        return fileSize;
    }

    public String getFileSize() {
        String filesize = br.getRegex("\\s*title=\"Download.*?\\((\\d+(?:\\.\\d+)?[KMG]{1}[B]{0,1})\\)").getMatch(0);
        if (filesize == null) {
            filesize = br.getRegex("\\((\\d+(?:\\.\\d+)?[KMG]{1}[B]{0,1})\\)").getMatch(0);
        }
        return filesize;
    }

    public String getFileName() {
        String filename = br.getRegex("itemprop=\"name\">([^<>\"]*?)<br>").getMatch(0);
        if (filename == null) {
            filename = br.getRegex("\"name\"\\s*:\\s*\"(.*?)\"").getMatch(0);
        }
        return filename;
    }

    public boolean offlineCheck() {
        if (br.getHttpConnection().getResponseCode() == 404) {
            return true;
        }
        return false;
    }

    public void prepBrowser() {
        br.getHeaders().put("User-Agent", "Mozilla/5.0 (Windows NT 6.3; WOW64; rv:40.0) Gecko/20100101 Firefox/40.0");
    }

    public void setCookies() {
        synchronized (LOCK) {
            /* Re-uses saved cookies to avoid captchas */
            final Object ret = this.getPluginConfig().getProperty("cookies", null);
            if (ret != null) {
                final HashMap<String, String> cookies = (HashMap<String, String>) ret;
                for (final Map.Entry<String, String> cookieEntry : cookies.entrySet()) {
                    final String key = cookieEntry.getKey();
                    final String value = cookieEntry.getValue();
                    br.setCookie(COOKIE_HOST, key, value);
                }
            } else {
                /* Skips the captcha (tries to). */
                br.setCookie(COOKIE_HOST, "downloadcaptcha", "1");
            }
        }
    }

    @Override
    public void handleFree(final DownloadLink downloadLink) throws Exception, PluginException {
        requestFileInformation(downloadLink);
        handleDownload(null, downloadLink, FREE_RESUME, FREE_MAXCHUNKS, "directlink");
    }

    public void handleDownload(final Account account, final DownloadLink downloadLink, final boolean resume, final int maxchunks, final String directlinkproperty) throws Exception, PluginException {
        String dllink = null;
        if (br.containsHTML(HTML_TYPE_DIRECT)) {
            dllink = br.getRegex("\"(https?://[0-9]+\\.[0-9]+\\.[0-9]+\\.[0-9]+[^<>\"]*?/[^<>\"/]*?)\"").getMatch(0);
        }
        if (dllink == null) {
            synchronized (LOCK) {
                dllink = checkDirectLink(downloadLink, directlinkproperty);
                if (dllink == null) {
                    // redirects can be here
                    br.setFollowRedirects(true);
                    if (!isSubURL) {
                        br.getPage(br.getURL() + "-download");
                    }
                    /* As long as the static cookie set captcha workaround works fine, */
                    if (br.containsHTML("solvemedia\\.com/papi/")) {
                        /* Premium users should of course not have to enter captchas here! */
                        logger.info("Detected captcha method \"solvemedia\" for this host");
                        final org.jdownloader.captcha.v2.challenge.solvemedia.SolveMedia sm = new org.jdownloader.captcha.v2.challenge.solvemedia.SolveMedia(br);
                        File cf = null;
                        try {
                            cf = sm.downloadCaptcha(getLocalCaptchaFile());
                        } catch (final Exception e) {
                            if (org.jdownloader.captcha.v2.challenge.solvemedia.SolveMedia.FAIL_CAUSE_CKEY_MISSING.equals(e.getMessage())) {
                                throw new PluginException(LinkStatus.ERROR_FATAL, "Host side solvemedia.com captcha error - please contact the " + this.getHost() + " support");
                            }
                            throw e;
                        }
                        final String code = getCaptchaCode(cf, downloadLink);
                        final String chid = sm.getChallenge(code);
                        br.postPage(br.getURL(), "submit=+Verify+%26+Download&adcopy_response=" + Encoding.urlEncode(code) + "&adcopy_challenge=" + chid);
                        if (br.containsHTML("solvemedia\\.com/papi/")) {
                            throw new PluginException(LinkStatus.ERROR_CAPTCHA);
                        }
                        /* Save cookies to avoid captchas in the future */
                        final HashMap<String, String> cookies = new HashMap<String, String>();
                        final Cookies add = br.getCookies(COOKIE_HOST);
                        for (final Cookie c : add.getCookies()) {
                            cookies.put(c.getKey(), c.getValue());
                        }
                        this.getPluginConfig().setProperty("cookies", cookies);
                    }
                    dllink = br.getRegex("\"[^<>\"]*?(/roms/get\\-download\\.php[^<>\"]*?)\"").getMatch(0);
                    if (dllink == null) {
                        throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                    }
                    dllink = Encoding.htmlOnlyDecode(dllink);
                }
            }
            // do not analyse if accounts been used.
            if (account == null) {
                boolean happyHour = false;
                if (br.containsHTML("id=\"happy\\-hour\"")) {
                    if (br.containsHTML("src=\"/happy_hour.php\"")) {
                        final Browser clone = br.cloneBrowser();
                        clone.getPage("/happy_hour.php");
                        if (clone.containsHTML(".style.display=\"block\"")) {
                            happyHour = true;
                        }
                    }
                }
                if (happyHour) {
                    maxFree.set(FREE_MAXDOWNLOADS);
                } else {
                    maxFree.set(1);
                }
            }
        }
        setDownloadServerCookie();
        /* Without this the directlink won't be accepted! */
        br.getHeaders().put("Referer", br.getURL());
        dl = new jd.plugins.BrowserAdapter().openDownload(br, downloadLink, dllink, resume, maxchunks);
        if (dl.getConnection().getContentType().contains("html")) {
            final long responsecode = dl.getConnection().getResponseCode();
            if (responsecode == 400) {
                throw new PluginException(LinkStatus.ERROR_HOSTER_TEMPORARILY_UNAVAILABLE, "Server error 400", 2 * 60 * 1000l);
            } else if (responsecode == 503) {
                /* Too many connections --> Happy hour is definitly not active --> Only allow 1 simultaneous download. */
                maxFree.set(1);
                throw new PluginException(LinkStatus.ERROR_HOSTER_TEMPORARILY_UNAVAILABLE, "Server error 503 - Too many concurrent connections - wait before starting new downloads", 1 * 60 * 1000l);
            }
            br.followConnection();
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        downloadLink.setProperty(directlinkproperty, dllink);
        downloadLink.setFinalFileName(Encoding.htmlDecode(getFileNameFromHeader(dl.getConnection())).trim());
        dl.startDownload();
    }

    private void setDownloadServerCookie() {
        // 0 = off
        // 1 = auto
        // 2 = europe
        // 3 = north america
        final String cookieKey = "epdprefs";
        final String domain = br.getHost();
        final int server = getPluginConfig().getIntegerProperty("servers", 0);
        if (server == 0) {
            // do nothing!
            return;
        } else if (server == 1) {
            br.setCookie(domain, cookieKey, "ephttpdownload");
        } else if (server == 2) {
            br.setCookie(domain, cookieKey, "ephttpdownloadloceu");
        } else if (server == 3) {
            br.setCookie(domain, cookieKey, "ephttpdownloadlocna");
        }
    }

    private String checkDirectLink(final DownloadLink downloadLink, final String property) {
        String dllink = downloadLink.getStringProperty(property);
        if (dllink != null) {
            try {
                final Browser br2 = br.cloneBrowser();
                /* Without this the directlink won't be accepted! */
                br2.getHeaders().put("Referer", "http://www.emuparadise.me/");
                URLConnectionAdapter con = br2.openGetConnection(dllink);
                if (con.getContentType().contains("html") || con.getLongContentLength() == -1) {
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

    private static Object LOCK_ACC = new Object();

    public void login(final Account account, final boolean force) throws Exception {
        synchronized (LOCK_ACC) {
            try {
                br.setFollowRedirects(true);
                br.setCookiesExclusive(true);
                final Cookies cookies = account.loadCookies("");
                if (cookies != null && !force) {
                    br.setCookies(this.getHost(), cookies);
                    br.getPage("http://www." + this.getHost() + "/");
                    if (br.containsHTML("logout=1")) {
                        return;
                    }
                    /* Full login */
                }
                final String txnId = account.getStringProperty("txnid", null);
                if (txnId != null) {
                    br.getPage("http://www." + this.getHost() + "/premium-login.php?txn_id=" + txnId);
                    /* premuser value == txnId */
                    if (br.getCookie(this.getHost(), "premuser") == null) {
                        if ("de".equalsIgnoreCase(System.getProperty("user.language"))) {
                            throw new PluginException(LinkStatus.ERROR_PREMIUM, "\r\nUngültiger Benutzername oder ungültiges Passwort!\r\nSchnellhilfe: \r\nDu bist dir sicher, dass dein eingegebener Benutzername und Passwort stimmen?\r\nFalls dein Passwort Sonderzeichen enthält, ändere es und versuche es erneut!", PluginException.VALUE_ID_PREMIUM_DISABLE);
                        } else {
                            throw new PluginException(LinkStatus.ERROR_PREMIUM, "\r\nInvalid username/password!\r\nQuick help:\r\nYou're sure that the username and password you entered are correct?\r\nIf your password contains special characters, change it (remove them) and try again!", PluginException.VALUE_ID_PREMIUM_DISABLE);
                        }
                    }
                } else {
                    throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_DISABLE);
                }
                account.saveCookies(br.getCookies(this.getHost()), "");
            } catch (final PluginException e) {
                account.clearCookies("");
                throw e;
            }
        }
    }

    @SuppressWarnings("deprecation")
    @Override
    public AccountInfo fetchAccountInfo(final Account account) throws Exception {
        final AccountInfo ai = new AccountInfo();
        try {
            login(account, true);
        } catch (PluginException e) {
            account.setValid(false);
            throw e;
        }
        ai.setUnlimitedTraffic();
        /* 2016-12-14: Every account gets treated as premium - I guess if an account expires, login is not possible anymore. */
        account.setType(AccountType.PREMIUM);
        account.setMaxSimultanDownloads(ACCOUNT_PREMIUM_MAXDOWNLOADS);
        account.setConcurrentUsePossible(true);
        account.setValid(true);
        return ai;
    }

    @Override
    public void handlePremium(final DownloadLink link, final Account account) throws Exception {
        requestFileInformation(link);
        final String url = br.getURL();
        login(account, false);
        br.setFollowRedirects(false);
        br.getPage(url);
        if (account.getType() == AccountType.FREE) {
            handleDownload(account, link, ACCOUNT_FREE_RESUME, ACCOUNT_FREE_MAXCHUNKS, "account_free_directlink");
        } else {
            handleDownload(account, link, ACCOUNT_PREMIUM_RESUME, ACCOUNT_PREMIUM_MAXCHUNKS, "account_premium_directlink");
        }
    }

    @Override
    public int getMaxSimultanPremiumDownloadNum() {
        return ACCOUNT_FREE_MAXDOWNLOADS;
    }

    public static class EmuParadiseMeAccountFactory extends MigPanel implements AccountBuilderInterface {

        /**
         *
         */
        private static final long serialVersionUID = 1L;

        private final String      txnIdHelp        = "Enter in your TxnId";

        private String getTxnId() {
            if (txnIdHelp.equals(this.txnId.getText())) {
                return null;
            }
            return this.txnId.getText();
        }

        private final ExtTextField txnId;

        private final JLabel       jlTxnId;

        public EmuParadiseMeAccountFactory(final InputChangedCallbackInterface callback) {
            super("ins 0, wrap 2", "[][grow,fill]", "");
            // txnid
            add(jlTxnId = new JLabel("TxnId: (must be at least 8 digits)"));
            add(this.txnId = new ExtTextField() {

                @Override
                public void onChanged() {
                    callback.onChangedInput(this);
                }

            });
            txnId.setHelpText(txnIdHelp);
        }

        @Override
        public JComponent getComponent() {
            return this;
        }

        @Override
        public void setAccount(Account defaultAccount) {
            if (defaultAccount != null) {
                txnId.setText(defaultAccount.getStringProperty("txnId", null));
            }
        }

        @Override
        public boolean validateInputs() {
            final String txnId = getTxnId();
            /* Either username & password or txnId only. */
            if (!validatetxnId(txnId)) {
                jlTxnId.setForeground(Color.RED);
                return false;
            } else {
                jlTxnId.setForeground(Color.BLACK);
                return true;
            }
        }

        private boolean validatetxnId(final String txnId) {
            return txnId != null && txnId.matches("^\\d{8,}$");
        }

        @Override
        public Account getAccount() {
            final String txnId = getTxnId();
            final Account account = new Account(txnId, "");
            if (this.validatetxnId(txnId)) {
                account.setProperty("txnid", txnId);
            } else {
                account.setProperty("txnid", Property.NULL);
            }
            return account;
        }

        public boolean updateAccount(Account input, Account output) {
            boolean changed = false;
            if (!StringUtils.equals(input.getUser(), output.getUser())) {
                output.setUser(input.getUser());
                changed = true;
            }
            if (!StringUtils.equals(input.getPass(), output.getPass())) {
                output.setPass(input.getPass());
                changed = true;
            }
            return changed;
        }
    }

    @Override
    public void reset() {
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return maxFree.get();
    }

    @Override
    public void resetDownloadlink(final DownloadLink link) {
    }

}