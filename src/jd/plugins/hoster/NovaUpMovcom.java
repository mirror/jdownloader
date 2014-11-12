//    jDownloader - Downloadmanager
//    Copyright (C) 2009  JD-Team support@jdownloader.org
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

import java.io.IOException;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map.Entry;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;

import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;

import jd.PluginWrapper;
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
import jd.utils.locale.JDL;

import org.appwork.utils.formatter.SizeFormatter;
import org.appwork.utils.formatter.TimeFormatter;

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "novaup.com" }, urls = { "http://(www\\.)?(nova(up|mov)\\.com/(download|sound|video)/[a-z0-9]+|(embed\\.)?novamov\\.com/embed\\.php(\\?width=\\d+\\&height=\\d+\\&|\\?)v=[a-z0-9]+)" }, flags = { 2 })
public class NovaUpMovcom extends PluginForHost {

    private final String         TEMPORARYUNAVAILABLE         = "(The file is being transfered to our other servers\\.|This may take few minutes\\.</)";
    private final String         TEMPORARYUNAVAILABLEUSERTEXT = "Temporary unavailable";

    /* Connection stuff */
    private static final boolean FREE_RESUME                  = true;
    private static final int     FREE_MAXCHUNKS               = 0;
    private static final int     FREE_MAXDOWNLOADS            = 20;
    private static final boolean ACCOUNT_FREE_RESUME          = true;
    private static final int     ACCOUNT_FREE_MAXCHUNKS       = 0;
    private static final int     ACCOUNT_FREE_MAXDOWNLOADS    = 20;
    private static final boolean ACCOUNT_PREMIUM_RESUME       = true;
    private static final int     ACCOUNT_PREMIUM_MAXCHUNKS    = 0;
    private static final int     ACCOUNT_PREMIUM_MAXDOWNLOADS = 20;

    /* don't touch the following! */
    private static AtomicInteger maxPrem                      = new AtomicInteger(1);

    private String               DLLINK                       = "";

    public NovaUpMovcom(final PluginWrapper wrapper) {
        super(wrapper);
        this.enablePremium("http://www.novamov.com/premium.php");
    }

    @Override
    public void correctDownloadLink(final DownloadLink link) {
        final String videoID = new Regex(link.getDownloadURL(), "novamov\\.com/embed\\.php.*?v=([a-z0-9]+)$").getMatch(0);
        if (videoID != null) {
            link.setUrlDownload("http://www.novamov.com/video/" + videoID);
        }
    }

    @Override
    public String getAGBLink() {
        return "http://www.novamov.com/terms.php";
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return FREE_MAXDOWNLOADS;
    }

    @Override
    public AvailableStatus requestFileInformation(final DownloadLink downloadLink) throws Exception {
        setBrowserExclusive();
        br.setFollowRedirects(true);
        br.getPage(downloadLink.getDownloadURL());
        if (br.containsHTML("This file no longer exists on our servers|The file has failed to convert!") || br.getURL().contains("novamov.com/index.php")) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        // onlinecheck für Videolinks
        if (downloadLink.getDownloadURL().contains("video")) {
            String filename = br.getRegex("name=\"title\" content=\"Watch(.*?)online\"").getMatch(0);
            if (filename == null) {
                filename = br.getRegex("<title>Watch(.*?)online \\| NovaMov - Free and reliable flash video hosting</title>").getMatch(0);
            }
            if (filename == null) {
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            filename = filename.trim();
            downloadLink.setFinalFileName(filename.replace(filename.substring(filename.length() - 4, filename.length()), "") + ".flv");
            getVideoLink();
            if (br.containsHTML("error_msg=The video is being transfered")) {
                downloadLink.getLinkStatus().setStatusText("Not downloadable at the moment, try again later...");
                return AvailableStatus.TRUE;
            } else if (br.containsHTML("error_msg=The video has failed to convert")) {
                downloadLink.getLinkStatus().setStatusText("Not downloadable at the moment, try again later ('video has failed to convert')...");
                return AvailableStatus.TRUE;
            } else if (br.containsHTML("error_msg=invalid token")) {
                downloadLink.getLinkStatus().setStatusText("Server error 'invalid token'");
                return AvailableStatus.TRUE;
            }
            if (DLLINK == null) {
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            DLLINK = Encoding.urlDecode(DLLINK, false);
            final URLConnectionAdapter con = br.openGetConnection(DLLINK);
            try {
                downloadLink.setDownloadSize(con.getLongContentLength());
            } finally {
                con.disconnect();
            }

        } else {
            // Onlinecheck für "nicht"-video Links
            String filename = br.getRegex("<h3><a href=\"#\"><h3>(.*?)</h3></a></h3>").getMatch(0);
            if (filename == null) {
                filename = br.getRegex("style=\"text-indent:0;\"><h3>(.*?)</h3></h5>").getMatch(0);
            }
            final String filesize = br.getRegex("strong>File size :</strong>(.*?)</div>").getMatch(0);
            if (filename == null || filesize == null) {
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            downloadLink.setName(filename.trim());
            downloadLink.setDownloadSize(SizeFormatter.getSize(filesize.replaceAll(",", "")));
        }

        return AvailableStatus.TRUE;
    }

    @Override
    public void handleFree(final DownloadLink downloadLink) throws Exception, PluginException {
        doFree(downloadLink, FREE_RESUME, FREE_MAXCHUNKS, "free_directlink");
    }

    private void doFree(final DownloadLink downloadLink, final boolean resumable, final int maxchunks, final String directlinkproperty) throws Exception, PluginException {
        String dllink = checkDirectLink(downloadLink, directlinkproperty);
        if (dllink == null) {
            if (downloadLink.getDownloadURL().contains("video")) {
                /* Generate new link */
                br.getPage(downloadLink.getDownloadURL());
                getVideoLink();
                if (br.containsHTML("error_msg=The video is being transfered")) {
                    throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Not downloadable at the moment, try again later...", 60 * 60 * 1000l);
                } else if (br.containsHTML("error_msg=The video has failed to convert")) {
                    throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server error 'The video has failed to convert'", 30 * 60 * 1000l);
                } else if (br.containsHTML("error_msg=invalid token")) {
                    throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server error 'invalid token'", 30 * 60 * 1000l);
                }
            } else {
                // handling für "nicht"-video Links
                if (br.containsHTML(TEMPORARYUNAVAILABLE)) {
                    throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, JDL.L("plugins.hoster.novaupmovcom.temporaryunavailable", TEMPORARYUNAVAILABLEUSERTEXT), 30 * 60 * 1000l);
                }
                br.setFollowRedirects(false);
                br.getPage(downloadLink.getDownloadURL());
                DLLINK = br.getRegex("class= \"click_download\"><a href=\"(http://.*?)\"").getMatch(0);
                if (DLLINK == null) {
                    DLLINK = br.getRegex("\"(http://e\\d+\\.novaup\\.com/dl/[a-z0-9]+/[a-z0-9]+/.*?)\"").getMatch(0);
                }
                if (DLLINK == null) {
                    throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                }
                if (!DLLINK.contains("http://")) {
                    DLLINK = "http://novaup.com" + DLLINK;
                }
            }
        }
        dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, dllink, resumable, maxchunks);
        if (dl.getConnection().getContentType().contains("html")) {
            handleServerErrors();
            br.followConnection();
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        downloadLink.setProperty(directlinkproperty, dllink);
        dl.startDownload();
    }

    private void handleServerErrors() throws PluginException {
        if (dl.getConnection().getResponseCode() == 403) {
            throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server error 403", 5 * 60 * 1000l);
        }
    }

    private void getVideoLink() throws PluginException, IOException {
        String result = unWise();
        // Maybe it's directly in the html
        if (result == null) {
            result = br.toString();
        }
        final String fileId = new Regex(result, "flashvars\\.file=\"(.*?)\"").getMatch(0);
        final String fileKey = new Regex(result, "flashvars\\.filekey=\"(.*?)\"").getMatch(0);
        final String fileCid = new Regex(result, "flashvars\\.cid=\"(.*?)\"").getMatch(0);
        if (fileId == null || fileKey == null || fileCid == null) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        br.getPage("http://www.novamov.com/api/player.api.php?user=undefined&codes=" + fileCid + "&file=" + fileId + "&pass=undefined&key=" + fileKey);
        DLLINK = br.getRegex("url=(.*?)\\&").getMatch(0);
    }

    private String unWise() {
        String result = null;
        String fn = br.getRegex("eval\\((function\\(.*?\'\\))\\);").getMatch(0);
        if (fn == null) {
            return null;
        }
        final ScriptEngineManager manager = jd.plugins.hoster.DummyScriptEnginePlugin.getScriptEngineManager(this);
        final ScriptEngine engine = manager.getEngineByName("javascript");
        try {
            engine.eval("var res = " + fn);
            result = (String) engine.get("res");
            result = new Regex(result, "eval\\((.*?)\\);$").getMatch(0);
            engine.eval("res = " + result);
            result = (String) engine.get("res");
            String res[] = result.split(";\\s;");
            engine.eval("res = " + new Regex(res[res.length - 1], "eval\\((.*?)\\);$").getMatch(0));
            result = (String) engine.get("res");
        } catch (final Exception e) {
            logger.log(Level.SEVERE, e.getMessage(), e);
            return null;
        }
        return result;
    }

    private String checkDirectLink(final DownloadLink downloadLink, final String property) {
        String dllink = downloadLink.getStringProperty(property);
        if (dllink != null) {
            try {
                final Browser br2 = br.cloneBrowser();
                br2.setFollowRedirects(true);
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

    private static final String MAINPAGE = "http://novamov.com";
    private static Object       LOCK     = new Object();

    @SuppressWarnings("unchecked")
    private void login(final Account account, final boolean force) throws Exception {
        synchronized (LOCK) {
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
                        for (final Entry<String, String> cookieEntry : cookies.entrySet()) {
                            final String key = cookieEntry.getKey();
                            final String value = cookieEntry.getValue();
                            br.setCookie(MAINPAGE, key, value);
                        }
                        return;
                    }
                }
                br.setFollowRedirects(true);
                br.postPage("http://www.novamov.com/login.php?return=", "user=" + Encoding.urlEncode(account.getUser()) + "&pass=" + Encoding.urlEncode(account.getPass()));
                final String passcookie = br.getCookie(MAINPAGE, "pass");
                if (passcookie == null || "deleted".equals(passcookie)) {
                    if ("de".equalsIgnoreCase(System.getProperty("user.language"))) {
                        throw new PluginException(LinkStatus.ERROR_PREMIUM, "\r\nUngültiger Benutzername oder ungültiges Passwort!\r\nSchnellhilfe: \r\nDu bist dir sicher, dass dein eingegebener Benutzername und Passwort stimmen?\r\nFalls dein Passwort Sonderzeichen enthält, ändere es und versuche es erneut!", PluginException.VALUE_ID_PREMIUM_DISABLE);
                    } else {
                        throw new PluginException(LinkStatus.ERROR_PREMIUM, "\r\nInvalid username/password!\r\nQuick help:\r\nYou're sure that the username and password you entered are correct?\r\nIf your password contains special characters, change it (remove them) and try again!", PluginException.VALUE_ID_PREMIUM_DISABLE);
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
        final AccountInfo ai = new AccountInfo();
        try {
            login(account, true);
        } catch (PluginException e) {
            account.setValid(false);
            throw e;
        }
        br.getPage("/premium.php");
        ai.setUnlimitedTraffic();
        final String expire = br.getRegex("<h3>Your premium membership expires on: ([^<>\"/]*?)</h3>").getMatch(0);
        if (expire != null) {
            account.setProperty("free", false);
            ai.setValidUntil(TimeFormatter.getMilliSeconds(expire, "yyyy-MMM-dd", Locale.ENGLISH));
            maxPrem.set(ACCOUNT_PREMIUM_MAXDOWNLOADS);
            try {
                account.setType(AccountType.PREMIUM);
                account.setMaxSimultanDownloads(maxPrem.get());
                account.setConcurrentUsePossible(true);
            } catch (final Throwable e) {
                /* not available in old Stable 0.9.581 */
            }
            ai.setStatus("Premium Account");
        } else {
            account.setProperty("free", true);
            maxPrem.set(ACCOUNT_FREE_MAXDOWNLOADS);
            try {
                account.setType(AccountType.FREE);
                /* free accounts can still have captcha */
                account.setMaxSimultanDownloads(maxPrem.get());
                account.setConcurrentUsePossible(false);
            } catch (final Throwable e) {
                /* not available in old Stable 0.9.581 */
            }
            ai.setStatus("Registered (free) user");
        }
        account.setValid(true);
        return ai;
    }

    @Override
    public void handlePremium(final DownloadLink link, final Account account) throws Exception {
        requestFileInformation(link);
        login(account, false);
        br.setFollowRedirects(false);
        br.getPage(link.getDownloadURL());
        if (account.getBooleanProperty("free", false)) {
            doFree(link, ACCOUNT_FREE_RESUME, ACCOUNT_FREE_MAXCHUNKS, "account_free_directlink");
        } else {
            String dllink = this.checkDirectLink(link, "premium_directlink");
            if (dllink == null) {
                dllink = br.getRegex("\"(https?://[^<>\"]*?)\" class=\"btn\">Download this video</a>").getMatch(0);
                if (dllink == null) {
                    logger.warning("Final downloadlink (String is \"dllink\") regex didn't match!");
                    throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                }
            }
            dl = jd.plugins.BrowserAdapter.openDownload(br, link, dllink, ACCOUNT_PREMIUM_RESUME, ACCOUNT_PREMIUM_MAXCHUNKS);
            if (dl.getConnection().getContentType().contains("html")) {
                handleServerErrors();
                logger.warning("The final dllink seems not to be a file!");
                br.followConnection();
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            link.setProperty("premium_directlink", dllink);
            dl.startDownload();
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
    public void resetDownloadlink(final DownloadLink link) {
    }

}