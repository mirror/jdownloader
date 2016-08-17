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

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicInteger;

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
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;
import jd.plugins.components.PluginJSonUtils;

import org.appwork.utils.formatter.TimeFormatter;
import org.jdownloader.scripting.JavaScriptEngineFactory;

@HostPlugin(revision = "$Revision$", interfaceVersion = 3, names = { "bitster.cz" }, urls = { "https?://(?:www\\.)?bitster\\.(?:cz|sk)/(?:#?file|download)/[a-z0-9]+" }) 
public class BitsterCz extends PluginForHost {

    public BitsterCz(PluginWrapper wrapper) {
        super(wrapper);
        this.enablePremium("https://bitster.cz/premium");
    }

    @Override
    public String getAGBLink() {
        return "https://bitster.cz/terms";
    }

    /* Connection stuff */
    private static final boolean FREE_RESUME                  = true;
    private static final int     FREE_MAXCHUNKS               = 1;                   // asked by hoster, they have server issues
    private static final int     FREE_MAXDOWNLOADS            = 1;                   // asked by hoster, they have server issues
    private static final boolean ACCOUNT_FREE_RESUME          = true;
    private static final int     ACCOUNT_FREE_MAXCHUNKS       = 1;                   // asked by hoster, they have server issues
    private static final int     ACCOUNT_FREE_MAXDOWNLOADS    = 1;                   // asked by hoster, they have server issues
    private static final boolean ACCOUNT_PREMIUM_RESUME       = true;
    private static final int     ACCOUNT_PREMIUM_MAXCHUNKS    = -4;                  // asked by hoster, they have server issues
    private static final int     ACCOUNT_PREMIUM_MAXDOWNLOADS = 10;                  // asked by hoster, they have server issues

    private static final String  HTML_ERROR_DELETED           = "\"DELETED\"";
    private static final String  HTML_ERROR_NOTFOUND          = "\"NOTFOUND\"";
    private static final String  HTML_ERROR_ABUSED            = "\"ABUSED\"";
    private static final String  HTML_ERROR_LOCKED            = "\"LOCKED\"";
    private static final String  HTML_ERROR_TEMPUNAVAILABLE   = "\"NOTAVAILABLE\"";

    /* don't touch the following! */
    private static AtomicInteger maxPrem                      = new AtomicInteger(1);

    private String               fid                          = null;
    private String               password                     = "";
    private DownloadLink         currDownloadlink             = null;
    private boolean              passwordprotected            = false;

    private Browser prepBR(final Browser br) {
        br.getHeaders().put("User-Agent", "JDownloader");
        br.setCustomCharset("utf-8");
        br.setCookie(this.getHost(), "lang", "en");
        br.setCookie(this.getHost(), "drones-modal-hidden", "true");
        br.setCookie(this.getHost(), "cookies_accepted", "true");
        /* Typically this is their response code for IP/country block. */
        br.setAllowedResponseCodes(502);
        return br;
    }

    @SuppressWarnings("unchecked")
    @Override
    public AvailableStatus requestFileInformation(final DownloadLink link) throws Exception {
        passwordprotected = false;
        currDownloadlink = link;
        String filename = null;
        long filesize = -1;
        String description = null;
        String md5 = null;
        boolean set_finalname = true;
        this.fid = getFID(link);
        link.setLinkID(this.fid);
        this.setBrowserExclusive();
        prepBR(this.br);
        this.br.getPage("https://bitster.cz/api/file_getinfo?param=" + this.fid + (password != null ? "&pw=" + password : ""));
        if (this.br.getHttpConnection().getResponseCode() == 404 || this.br.toString().equals(HTML_ERROR_NOTFOUND) || this.br.toString().equals(HTML_ERROR_ABUSED) || this.br.toString().equals(HTML_ERROR_DELETED)) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        if (this.br.toString().equals(HTML_ERROR_LOCKED)) {
            link.getLinkStatus().setStatusText("This url is password protected");
            passwordprotected = true;
        } else if (this.br.toString().equals(HTML_ERROR_TEMPUNAVAILABLE)) {
            link.getLinkStatus().setStatusText("This file is temporary unavailable");
        } else {
            /* Check for further problems e.g. country/IP range blocked by host. */
            this.apiHandleErrors();
            final LinkedHashMap<String, Object> entries = (LinkedHashMap<String, Object>) JavaScriptEngineFactory.jsonToJavaObject(br.toString());
            filesize = JavaScriptEngineFactory.toLong(entries.get("length"), -1);
            filename = (String) entries.get("title");
            md5 = (String) entries.get("md5");
            description = (String) entries.get("longdescription");
            /* This shouldn't be needed but okay let's double check/set passwordprotected state here. */
            final Object passwordprotected_o = entries.get("passwordprotected");
            if (passwordprotected_o instanceof String) {
                passwordprotected = Boolean.parseBoolean((String) passwordprotected_o);
            } else {
                passwordprotected = ((Boolean) entries.get("passwordprotected")).booleanValue();
            }
            if (inValidate(description)) {
                description = (String) entries.get("shortdescription");
            }
        }
        if (filename == null) {
            /* Ultimate fallback for password protected urls or unknown states. */
            filename = this.fid;
            set_finalname = false;
        }
        filename = Encoding.htmlDecode(filename.trim());
        filename = encodeUnicode(filename);
        if (set_finalname) {
            link.setFinalFileName(filename);
        } else {
            link.setName(filename);
        }
        if (filesize > -1) {
            link.setVerifiedFileSize(filesize);
        }
        if (link.getComment() == null) {
            link.setComment(description);
        }
        if (md5 != null) {
            link.setMD5Hash(md5);
        }
        return AvailableStatus.TRUE;
    }

    @Override
    public void handleFree(final DownloadLink downloadLink) throws Exception, PluginException {
        requestFileInformation(downloadLink);
        doFree(downloadLink, FREE_RESUME, FREE_MAXCHUNKS, "free_directlink");
    }

    private void doFree(final DownloadLink downloadLink, final boolean resumable, final int maxchunks, final String directlinkproperty) throws Exception, PluginException {
        String dllink = checkDirectLink(downloadLink, directlinkproperty);
        if (dllink == null) {
            dllink = getDllink(false);
        }
        dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, dllink, resumable, maxchunks);
        if (dl.getConnection().getContentType().contains("html")) {
            if (dl.getConnection().getResponseCode() == 403) {
                throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server error 403", 60 * 60 * 1000l);
            } else if (dl.getConnection().getResponseCode() == 404) {
                throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server error 404", 60 * 60 * 1000l);
            }
            br.followConnection();
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        downloadLink.setProperty(directlinkproperty, dllink);
        dl.startDownload();
    }

    private String getDllink(final boolean premium) throws Exception {
        String dllink = null;
        String download_get_data = "?param=" + this.fid;
        if (premium) {
            /* If file is password protected && owner tries to download it --> Password is not needed! */
            download_get_data += "&premium=true";
            if (this.passwordprotected) {
                this.br.getPage("/api/file_download" + download_get_data);
                if (!this.br.containsHTML(HTML_ERROR_LOCKED)) {
                    /*
                     * File owner tried to download his own password protected file --> Password not needed --> Check for further errors and
                     * set final download url
                     */
                    apiHandleErrors();
                    dllink = this.br.toString().replace("\"", "");
                }
            }
        }
        if (this.passwordprotected && dllink == null) {
            this.password = this.currDownloadlink.getStringProperty("pass", null);
            if (this.password == null) {
                this.password = getUserInput("Password?", this.currDownloadlink);
            }
            this.requestFileInformation(this.currDownloadlink);
            /* Wrong password entered? Will be caught here! */
            apiHandleErrors();
            /* Password seems to be correct --> Save it */
            this.currDownloadlink.setProperty("pass", this.password);
            if (this.password != null) {
                download_get_data += "&pw=" + Encoding.urlEncode(this.password);
            }
        }
        if (dllink == null) {
            /* E.g. free download or url which is not password protected. */
            getPage("/api/file_download" + download_get_data);
            dllink = this.br.toString().replace("\"", "");
        }
        if (dllink == null || !dllink.startsWith("http") || dllink.length() > 500) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        return dllink;
    }

    private String checkDirectLink(final DownloadLink downloadLink, final String property) {
        String dllink = downloadLink.getStringProperty(property);
        if (dllink != null) {
            URLConnectionAdapter con = null;
            try {
                final Browser br2 = br.cloneBrowser();
                con = br2.openHeadConnection(dllink);
                if (con.getContentType().contains("html") || con.getLongContentLength() == -1) {
                    downloadLink.setProperty(property, Property.NULL);
                    dllink = null;
                }
            } catch (final Exception e) {
                downloadLink.setProperty(property, Property.NULL);
                dllink = null;
            } finally {
                try {
                    con.disconnect();
                } catch (final Throwable e) {
                }
            }
        }
        return dllink;
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return FREE_MAXDOWNLOADS;
    }

    // private static final String MAINPAGE = "http://bitster.cz";
    private static Object LOCK = new Object();

    private void login(final Account account, final boolean force) throws Exception {
        synchronized (LOCK) {
            try {
                br.getHeaders().put("Authorization", "Basic " + Encoding.Base64Encode(account.getUser() + ":" + account.getPass()));
                br.setCookiesExclusive(true);
                prepBR(this.br);
                final Cookies cookies = account.loadCookies("");
                if (cookies != null && !force) {
                    this.br.setCookies(this.getHost(), cookies);
                    return;
                }
                br.setFollowRedirects(false);
                this.br.postPage("https://bitster.cz/api/validatebasicauth", "");
                final String result = PluginJSonUtils.getJsonValue(br, "result");
                if (!"true".equals(result)) {
                    if ("de".equalsIgnoreCase(System.getProperty("user.language"))) {
                        throw new PluginException(LinkStatus.ERROR_PREMIUM, "\r\nUngültiger Benutzername oder ungültiges Passwort!\r\nSchnellhilfe: \r\nDu bist dir sicher, dass dein eingegebener Benutzername und Passwort stimmen?\r\nFalls dein Passwort Sonderzeichen enthält, ändere es und versuche es erneut!", PluginException.VALUE_ID_PREMIUM_DISABLE);
                    } else {
                        throw new PluginException(LinkStatus.ERROR_PREMIUM, "\r\nInvalid username/password!\r\nQuick help:\r\nYou're sure that the username and password you entered are correct?\r\nIf your password contains special characters, change it (remove them) and try again!", PluginException.VALUE_ID_PREMIUM_DISABLE);
                    }
                }
                /* Basic Auth should be fine but let's save the cookies anyways! */
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
        final AccountInfo ai = new AccountInfo();
        try {
            login(account, true);
        } catch (PluginException e) {
            account.setValid(false);
            throw e;
        }
        this.br.getPage("/api/User_GetAccountInfo");
        final String joindate = PluginJSonUtils.getJsonValue(br, "joindate");
        final String trafficleft_str = PluginJSonUtils.getJsonValue(br, "creditbalance");
        long trafficleft = 0;
        if (trafficleft_str != null && trafficleft_str.matches("\\d+")) {
            trafficleft = Long.parseLong(trafficleft_str);
        }
        if (trafficleft <= 0) {
            /*
             * Okay let's assume they only have traffic based premium models so basically all accounts would be premium so only if there is
             * zero traffic --> Free account
             */
            maxPrem.set(ACCOUNT_FREE_MAXDOWNLOADS);
            account.setType(AccountType.FREE);
            /* free accounts can still have captcha */
            account.setMaxSimultanDownloads(maxPrem.get());
            account.setConcurrentUsePossible(false);
            ai.setStatus("Registered (free) user");
            ai.setTrafficLeft(0);
        } else {
            /**/
            maxPrem.set(ACCOUNT_PREMIUM_MAXDOWNLOADS);
            account.setType(AccountType.PREMIUM);
            account.setMaxSimultanDownloads(maxPrem.get());
            account.setConcurrentUsePossible(true);
            ai.setStatus("Premium account");
            ai.setTrafficLeft(trafficleft);
        }
        if (joindate != null) {
            ai.setCreateTime(TimeFormatter.getMilliSeconds(joindate, "ddd, dd MMM yyyy HH:mm:ss Z", Locale.US));
        }
        account.setValid(true);
        return ai;
    }

    @SuppressWarnings("deprecation")
    @Override
    public void handlePremium(final DownloadLink link, final Account account) throws Exception {
        requestFileInformation(link);
        login(account, false);
        br.setFollowRedirects(false);
        br.getPage(link.getDownloadURL());
        if (account.getBooleanProperty("free", false)) {
            /*
             * This will probably never happen as free accounts usually have 0 traffic so they will never be used - nevertheless, that might
             * be useful for the future ...
             */
            doFree(link, ACCOUNT_FREE_RESUME, ACCOUNT_FREE_MAXCHUNKS, "account_free_directlink");
        } else {
            String dllink = this.checkDirectLink(link, "premium_directlink");
            if (dllink == null) {
                dllink = getDllink(true);
            }
            dl = jd.plugins.BrowserAdapter.openDownload(br, link, dllink, ACCOUNT_PREMIUM_RESUME, ACCOUNT_PREMIUM_MAXCHUNKS);
            if (dl.getConnection().getContentType().contains("html")) {
                if (dl.getConnection().getResponseCode() == 403) {
                    throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server error 403", 60 * 60 * 1000l);
                } else if (dl.getConnection().getResponseCode() == 404) {
                    throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server error 404", 60 * 60 * 1000l);
                }
                logger.warning("The final dllink seems not to be a file!");
                br.followConnection();
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            link.setProperty("premium_directlink", dllink);
            dl.startDownload();
        }
    }

    private void getPage(final String url) throws IOException, PluginException {
        this.br.getPage(url);
        apiHandleErrors();
    }

    private void apiHandleErrors() throws PluginException {
        if (this.br.containsHTML(HTML_ERROR_DELETED)) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        } else if (this.br.containsHTML(HTML_ERROR_ABUSED)) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        } else if (this.br.containsHTML(HTML_ERROR_NOTFOUND)) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        } else if (this.br.containsHTML("\"NOTAVAILABLE\"")) {
            throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server error", 30 * 60 * 1000l);
        } else if (this.br.containsHTML("\"ERROR\"")) {
            throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Unknown server error", 1 * 60 * 60 * 1000l);
        } else if (this.br.containsHTML("\"NOTLOGGEDIN\"")) {
            /* I guess this happens if we try to download premium things without logging in (missing Authorization Header)?! */
            if ("de".equalsIgnoreCase(System.getProperty("user.language"))) {
                throw new PluginException(LinkStatus.ERROR_PREMIUM, "\r\nUngültiger Benutzername oder ungültiges Passwort!\r\nSchnellhilfe: \r\nDu bist dir sicher, dass dein eingegebener Benutzername und Passwort stimmen?\r\nFalls dein Passwort Sonderzeichen enthält, ändere es und versuche es erneut!", PluginException.VALUE_ID_PREMIUM_DISABLE);
            } else {
                throw new PluginException(LinkStatus.ERROR_PREMIUM, "\r\nInvalid username/password!\r\nQuick help:\r\nYou're sure that the username and password you entered are correct?\r\nIf your password contains special characters, change it (remove them) and try again!", PluginException.VALUE_ID_PREMIUM_DISABLE);
            }
        } else if (this.br.containsHTML("\"NOTENOUGHCREDITS\"")) {
            throw new PluginException(LinkStatus.ERROR_PREMIUM, "Not enough traffic left", PluginException.VALUE_ID_PREMIUM_TEMP_DISABLE);
        } else if (this.br.containsHTML("\"PRIVATE\"")) {
            throw new PluginException(LinkStatus.ERROR_FATAL, "Private file");
        } else if (this.br.containsHTML(HTML_ERROR_LOCKED)) {
            this.currDownloadlink.setProperty("pass", Property.NULL);
            throw new PluginException(LinkStatus.ERROR_RETRY, "Wrong password entered");
        } else if (this.br.containsHTML("User is not logged in\\!")) {
            /* E.g. FULL html: "User is not logged in!" (including the "") */
            /* Happens e.g. if you try this without- or with wrong Authorization-Header: bitster.cz/api/User_GetAccountInfo */
            if ("de".equalsIgnoreCase(System.getProperty("user.language"))) {
                throw new PluginException(LinkStatus.ERROR_PREMIUM, "\r\nUngültiger Benutzername oder ungültiges Passwort!\r\nSchnellhilfe: \r\nDu bist dir sicher, dass dein eingegebener Benutzername und Passwort stimmen?\r\nFalls dein Passwort Sonderzeichen enthält, ändere es und versuche es erneut!", PluginException.VALUE_ID_PREMIUM_DISABLE);
            } else {
                throw new PluginException(LinkStatus.ERROR_PREMIUM, "\r\nInvalid username/password!\r\nQuick help:\r\nYou're sure that the username and password you entered are correct?\r\nIf your password contains special characters, change it (remove them) and try again!", PluginException.VALUE_ID_PREMIUM_DISABLE);
            }
        } else if (this.br.containsHTML("Error occurred, please contact")) {
            /* Should never happen */
            throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server error", 1 * 60 * 60 * 1000l);
        }
        if (this.br.getHttpConnection().getResponseCode() == 502) {
            if (this.currDownloadlink != null) {
                this.currDownloadlink.getLinkStatus().setStatusText("This service blocks your country / IP range");
                throw new PluginException(LinkStatus.ERROR_HOSTER_TEMPORARILY_UNAVAILABLE, "This service blocks your country / IP range", 3 * 60 * 60 * 1000l);
            }
        }
    }

    @SuppressWarnings("deprecation")
    private String getFID(final DownloadLink dl) {
        return new Regex(dl.getDownloadURL(), "([a-z0-9]+)$").getMatch(0);
    }

    /**
     * Validates string to series of conditions, null, whitespace, or "". This saves effort factor within if/for/while statements
     *
     * @param s
     *            Imported String to match against.
     * @return <b>true</b> on valid rule match. <b>false</b> on invalid rule match.
     * @author raztoki
     */
    protected boolean inValidate(final String s) {
        if (s == null || s.matches("\\s+") || s.equals("")) {
            return true;
        } else {
            return false;
        }
    }

    @Override
    public int getMaxSimultanPremiumDownloadNum() {
        /* workaround for free/premium issue on stable 09581 */
        return maxPrem.get();
    }

    @Override
    public void reset() {
    }

    @Override
    public void resetDownloadlink(DownloadLink link) {
    }

}