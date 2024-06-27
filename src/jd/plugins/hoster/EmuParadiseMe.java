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
import org.appwork.utils.encoding.URLEncode;
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

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "emuparadise.me" }, urls = { "https?://(?:www\\.)?emuparadise\\.me/(roms/roms\\.php\\?gid=\\d+|roms/get-download\\.php\\?gid=\\d+|[^<>/]+/[^<>/]+/\\d+)" })
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
    public static final String   TYPE_NORMAL                  = "https?://[^/]+/[^<>/]+/[^<>/]+/(\\d+)";
    public static final String   TYPE_GID                     = "https?://[^/]+/roms/roms\\.php\\?gid=(\\d+)";
    public static final String   TYPE_GID_DOWNLOAD            = "https?://[^/]+/roms/get-download\\.php\\?gid=(\\d+)";
    public static final String   TYPE_SEMICOLON_DOWNLOAD      = "https?://[^/]+/[^<>/]+/[^<>/]+/(\\d+)-download-\\d+";
    /*
     * note: this is on every bloody page, but format is slightly different.
     */
    /* Books, movies and specials are "directlinks" (no captcha/waittime) */
    private final String         HTML_TYPE_DIRECT             = "Direct Download:</h2>";

    @Override
    public String getLinkID(final DownloadLink link) {
        final String linkid = getFID(link);
        if (linkid != null) {
            return this.getHost() + "://" + linkid;
        } else {
            return super.getLinkID(link);
        }
    }

    private String getFID(final DownloadLink link) {
        if (link.getPluginPatternMatcher() == null) {
            return null;
        } else {
            if (link.getPluginPatternMatcher().matches(TYPE_NORMAL)) {
                return new Regex(link.getPluginPatternMatcher(), TYPE_NORMAL).getMatch(0);
            } else if (link.getPluginPatternMatcher().matches(TYPE_GID)) {
                return new Regex(link.getPluginPatternMatcher(), TYPE_GID).getMatch(0);
            } else if (link.getPluginPatternMatcher().matches(TYPE_GID_DOWNLOAD)) {
                return new Regex(link.getPluginPatternMatcher(), TYPE_GID_DOWNLOAD).getMatch(0);
            } else if (link.getPluginPatternMatcher().matches(TYPE_SEMICOLON_DOWNLOAD)) {
                return new Regex(link.getPluginPatternMatcher(), TYPE_SEMICOLON_DOWNLOAD).getMatch(0);
            } else {
                /* This should never happen */
                return null;
            }
        }
    }

    @Override
    public AvailableStatus requestFileInformation(final DownloadLink link) throws Exception {
        final String fid = this.getFID(link);
        if (fid == null) {
            /* This should never happen */
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        if (!link.isNameSet()) {
            link.setName(fid + ".zip");
        }
        this.setBrowserExclusive();
        br.setFollowRedirects(true);
        prepBrowser(this.br);
        setCookies();
        /* This should redirect to TYPE_NORMAL. */
        br.getPage("https://www." + this.getHost() + "/roms/roms.php?gid=" + fid);
        if (isOffline()) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        parseFileInfo(link);
        if (!link.isAvailable()) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        return AvailableStatus.TRUE;
    }

    public void parseFileInfo(final DownloadLink link) throws PluginException {
        String filename = null;
        String filesize = null;
        if (isUrlSemicolonDownload(br.getURL()) || br.containsHTML(HTML_TYPE_DIRECT)) {
            final Regex result = new Regex(br, ">Download\\s*(.*?)</a><font[^>]+>\\s*-\\s*File Size:\\s*(\\d+(?:\\.\\d+)?[KMG]{1}[B]{0,1})</font>");
            filename = result.getMatch(0);
            filesize = result.getMatch(1);
        } else {
            if (!br.containsHTML("id=\"Download\"")) {
                link.setAvailable(false);
                return;
            }
            link.setAvailable(true);
            filename = br.getRegex("itemprop=\"name\">([^<>\"]*?)<br>").getMatch(0);
            if (filename == null) {
                filename = br.getRegex("\"name\"\\s*:\\s*\"(.*?)\"").getMatch(0);
            }
            filesize = br.getRegex("\\s*title=\"Download.*?\\((\\d+(?:\\.\\d+)?[KMG]{1}[B]{0,1})\\)").getMatch(0);
            if (filesize == null) {
                filesize = br.getRegex("\\((\\d+(?:\\.\\d+)?[KMG]{1}[B]{0,1})\\)").getMatch(0);
            }
        }
        if (filename != null) {
            link.setName(Encoding.htmlDecode(filename).trim() + ".zip");
        }
        if (filesize != null) {
            link.setDownloadSize(SizeFormatter.getSize(correctFilesize(filesize)));
        }
    }

    public boolean isUrlSemicolonDownload(final String url) {
        if (url.matches("(?:https?)?(?://(?:www\\.)?emuparadise\\.me)?/[^<>/]+/[^<>/]+/\\d{4,}-download-\\d+")) {
            return true;
        } else {
            return false;
        }
    }

    public String correctFilesize(String fileSize) {
        if (fileSize != null && !fileSize.trim().endsWith("B")) {
            return fileSize.trim() + "B";
        } else {
            return fileSize;
        }
    }

    public boolean isOffline() {
        if (br.getHttpConnection().getResponseCode() == 404) {
            return true;
        } else {
            return false;
        }
    }

    public Browser prepBrowser(final Browser br) {
        br.getHeaders().put("User-Agent", "Mozilla/5.0 (Windows NT 6.3; WOW64; rv:40.0) Gecko/20100101 Firefox/40.0");
        return br;
    }

    public void setCookies() {
        synchronized (LOCK) {
            /* Re-uses saved cookies to avoid captchas */
            final Object ret = this.getPluginConfig().getProperty("cookies", null);
            if (ret != null && ret instanceof Map) {
                final Map<String, String> cookies = (Map<String, String>) ret;
                for (final Map.Entry<String, String> cookieEntry : cookies.entrySet()) {
                    final String key = cookieEntry.getKey();
                    final String value = cookieEntry.getValue();
                    br.setCookie(getHost(), key, value);
                }
            } else {
                /* Skips the captcha (tries to). */
                br.setCookie(getHost(), "downloadcaptcha", "1");
            }
        }
    }

    @Override
    public void handleFree(final DownloadLink link) throws Exception, PluginException {
        requestFileInformation(link);
        handleDownload(null, link, FREE_RESUME, FREE_MAXCHUNKS, "directlink");
    }

    public void handleDownload(final Account account, final DownloadLink link, final boolean resume, final int maxchunks, final String directlinkproperty) throws Exception, PluginException {
        String dllink = null;
        if (br.containsHTML(HTML_TYPE_DIRECT)) {
            dllink = br.getRegex("\"(https?://[0-9]+\\.[0-9]+\\.[0-9]+\\.[0-9]+[^<>\"]*?/[^<>\"/]*?)\"").getMatch(0);
        }
        if (dllink == null) {
            synchronized (LOCK) {
                dllink = checkDirectLink(link, directlinkproperty);
                if (dllink == null) {
                    // redirects can be here
                    br.setFollowRedirects(true);
                    final boolean preferWorkaroundRightAway = true;
                    if (preferWorkaroundRightAway) {
                        dllink = getDirectDownloadurlViaWorkaround(this.br, link);
                    } else {
                        if (!isUrlSemicolonDownload(br.getURL())) {
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
                            final String code = getCaptchaCode(cf, link);
                            final String chid = sm.getChallenge(code);
                            br.postPage(br.getURL(), "submit=+Verify+%26+Download&adcopy_response=" + Encoding.urlEncode(code) + "&adcopy_challenge=" + chid);
                            if (br.containsHTML("solvemedia\\.com/papi/")) {
                                throw new PluginException(LinkStatus.ERROR_CAPTCHA);
                            }
                            /* Save cookies to avoid captchas in the future */
                            final HashMap<String, String> cookies = new HashMap<String, String>();
                            final Cookies add = br.getCookies(getHost());
                            for (final Cookie c : add.getCookies()) {
                                cookies.put(c.getKey(), c.getValue());
                            }
                            this.getPluginConfig().setProperty("cookies", cookies);
                        }
                        dllink = br.getRegex("\"[^<>\"]*?(/roms/get\\-download\\.php[^<>\"]*?)\"").getMatch(0);
                        // if (dllink == null && br.containsHTML("(?i)>\\s*This game is unavailable")) {
                        // errorNoDownloadlinkAvailable();
                        // }
                        if (dllink != null) {
                            dllink = Encoding.htmlOnlyDecode(dllink);
                        } else {
                            /* 2021-09-13: No downloadlinks available for a lot of content but there is a workaround. */
                            dllink = getDirectDownloadurlViaWorkaround(this.br, link);
                        }
                    }
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
        dl = new jd.plugins.BrowserAdapter().openDownload(br, link, dllink, resume, maxchunks);
        if (!this.looksLikeDownloadableContent(dl.getConnection())) {
            br.followConnection(true);
            final long responsecode = dl.getConnection().getResponseCode();
            if (responsecode == 400) {
                throw new PluginException(LinkStatus.ERROR_HOSTER_TEMPORARILY_UNAVAILABLE, "Server error 400", 2 * 60 * 1000l);
            } else if (responsecode == 503) {
                /* Too many connections --> Happy hour is definitly not active --> Only allow 1 simultaneous download. */
                maxFree.set(1);
                throw new PluginException(LinkStatus.ERROR_HOSTER_TEMPORARILY_UNAVAILABLE, "Server error 503 - Too many concurrent connections - wait before starting new downloads", 1 * 60 * 1000l);
            } else {
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
        }
        link.setProperty(directlinkproperty, dllink);
        link.setFinalFileName(Encoding.htmlDecode(getFileNameFromConnection(dl.getConnection())).trim());
        dl.startDownload();
    }

    /** Source: https://gist.github.com/infval/c69b479ff0bd590f2dd7e1975fe2fcad */
    private String getDirectDownloadurlViaWorkaround(final Browser br, final DownloadLink link) throws PluginException {
        /* 2021-09-13: No downloadlinks available for a lot of content. */
        final String gid = getFID(link);
        if (gid == null) {
            errorNoDownloadlinkAvailable();
        }
        String dllink;
        if (StringUtils.containsIgnoreCase(br.getURL(), "Sega_Dreamcast_ISOs")) {
            /* Special case */
            /* Download can be available in multiple versions (or is it only multiple archive types e.g. .7z and .zip?). */
            final String[] downloadCandidates = br.getRegex("-download-\\d+\" title=\"Download ([^\"]+) ISO for Sega Dreamcast").getColumn(0);
            if (downloadCandidates.length == 0) {
                errorNoDownloadlinkAvailable();
            }
            dllink = "http://50.7.92.186/happyUUKAm8913lJJnckLiePutyNak/Dreamcast/" + URLEncode.encodeURIComponent(downloadCandidates[0]);
        } else {
            dllink = "/roms/get-download.php?gid=" + gid + "&test=true";
        }
        return dllink;
    }

    private void errorNoDownloadlinkAvailable() throws PluginException {
        throw new PluginException(LinkStatus.ERROR_FATAL, "No downloadlink available");
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

    private String checkDirectLink(final DownloadLink link, final String property) {
        String dllink = link.getStringProperty(property);
        if (dllink != null) {
            try {
                final Browser br2 = br.cloneBrowser();
                /* Without this the directlink won't be accepted! */
                br2.getHeaders().put("Referer", "http://www.emuparadise.me/");
                URLConnectionAdapter con = br2.openGetConnection(dllink);
                if (!this.looksLikeDownloadableContent(con)) {
                    link.setProperty(property, Property.NULL);
                    dllink = null;
                }
                con.disconnect();
            } catch (final Exception e) {
                link.setProperty(property, Property.NULL);
                dllink = null;
            }
        }
        return dllink;
    }

    public void login(final Account account, final boolean force) throws Exception {
        synchronized (account) {
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

    @Override
    public AccountInfo fetchAccountInfo(final Account account) throws Exception {
        final AccountInfo ai = new AccountInfo();
        login(account, true);
        ai.setUnlimitedTraffic();
        /* 2016-12-14: Every account gets treated as premium - I guess if an account expires, login is not possible anymore. */
        account.setType(AccountType.PREMIUM);
        account.setMaxSimultanDownloads(ACCOUNT_PREMIUM_MAXDOWNLOADS);
        account.setConcurrentUsePossible(true);
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
            if (!StringUtils.equals(input.getUser(), output.getUser())) {
                output.setUser(input.getUser());
                return true;
            } else if (!StringUtils.equals(input.getPass(), output.getPass())) {
                output.setPass(input.getPass());
                return true;
            } else {
                return false;
            }
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