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

import java.util.HashMap;
import java.util.Locale;
import java.util.Map.Entry;
import java.util.regex.Pattern;

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
import jd.plugins.components.SiteType.SiteTemplate;
import jd.utils.locale.JDL;

import org.appwork.utils.formatter.SizeFormatter;
import org.appwork.utils.formatter.TimeFormatter;
import org.jdownloader.scripting.JavaScriptEngineFactory;

@HostPlugin(revision = "$Revision$", interfaceVersion = 3, names = { "auroravid.to" }, urls = { "http://(?:www\\.)?(?:(novamov\\.com|novaup\\.com|auroravid\\.to)/(?:download|sound|video)/[a-z0-9]+|(?:embed\\.)?novamov\\.com/embed\\.php(\\?width=\\d+\\&height=\\d+\\&|\\?)v=[a-z0-9]+)" })
public class AuroravidTo extends PluginForHost {
    @Override
    public String[] siteSupportedNames() {
        return new String[] { "auroravid.to", "novamov.com" };
        // novaup.com rip. no dns
    }

    /* Similar plugins: NovaUpMovcom, VideoWeedCom, NowVideoEu, MovShareNet */
    private final String        TEMPORARYUNAVAILABLE         = "(The file is being transfered to our other servers\\.|This may take few minutes\\.</)";
    private final String        TEMPORARYUNAVAILABLEUSERTEXT = "Temporary unavailable";
    private static final String DOMAIN                       = "auroravid.to";
    private String              dllink                       = "";
    private boolean             server_issues                = false;

    public AuroravidTo(final PluginWrapper wrapper) {
        super(wrapper);
        this.enablePremium("http://www.auroravid.to/premium.php");
    }

    @Override
    public String rewriteHost(String host) {
        if (host == null || "novaup.com".equals(host) || "novamov.com".equals(host) || "auroravid.to".equals(host)) {
            return "auroravid.to";
        }
        return super.rewriteHost(host);
    }

    @Override
    public void correctDownloadLink(final DownloadLink link) {
        final String videoID = new Regex(link.getDownloadURL(), "/embed\\.php.*?v=([a-z0-9]+)$").getMatch(0);
        if (videoID != null) {
            link.setUrlDownload("http://www." + DOMAIN + "/video/" + videoID);
        }
    }

    @Override
    public String getAGBLink() {
        return "http://www." + DOMAIN + "/terms.php";
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return -1;
    }

    @SuppressWarnings("deprecation")
    @Override
    public AvailableStatus requestFileInformation(final DownloadLink downloadLink) throws Exception {
        dllink = null;
        server_issues = false;
        br = new Browser();
        setBrowserExclusive();
        br.setFollowRedirects(true);
        accessMainURL(downloadLink);
        if (br.containsHTML("This file no longer exists on our servers|The file has failed to convert!|/download\\.php\\?file=\"") || br.getURL().contains("novamov.com/index.php") || this.br.getHttpConnection().getResponseCode() == 404) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        final String fid = new Regex(downloadLink.getDownloadURL(), "([a-z0-9]+)$").getMatch(0);
        // onlinecheck für Videolinks
        if (downloadLink.getDownloadURL().contains("video")) {
            String filename = br.getRegex("property=\"og:title\" content=\"Watch ([^<>\"]+) online \\| AuroraVid\"").getMatch(0);
            if (filename == null) {
                /* More open RegEx */
                filename = br.getRegex("name=\"title\" content=\"Watch([^<>\"]+)online \\|[^<>\"]*?\"").getMatch(0);
            }
            if (filename == null) {
                // filename isn't always present!
                filename = fid;
            } else {
                filename = filename.trim();
                if (filename.length() > 4 && filename.contains(".")) {
                    /* Remove old extension */
                    filename = filename.replace(filename.substring(filename.length() - 4, filename.length()), "");
                }
            }
            /* Add correct extension */
            dllink = getDllink(br);
            if (dllink == null) {
                dllink = br.getRegex("<source src=\"([^<>\"]+mp4)\"").getMatch(0);
            }
            if (dllink != null) {
                final String ext = dllink.substring(dllink.lastIndexOf("."));
                filename += ext;
            } else {
                filename += ".flv";
            }
            downloadLink.setFinalFileName(filename);
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
            br.setFollowRedirects(true);
            URLConnectionAdapter con = null;
            try {
                try {
                    con = br.openGetConnection(dllink);
                } catch (final Throwable e) {
                    /* 2017-03-30: Do not fail here during availablecheck! */
                    return AvailableStatus.TRUE;
                }
                if (!con.getContentType().contains("html") && con.isOK()) {
                    downloadLink.setDownloadSize(con.getLongContentLength());
                    downloadLink.setProperty("free_directlink", con.getURL().toString());
                } else {
                    server_issues = true;
                    throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
                }
            } catch (Throwable e) { // connect timed out
                throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Unknown server error", 10 * 60 * 1000l);
            } finally {
                try {
                    con.disconnect();
                } catch (Throwable e) {
                }
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

    private void accessMainURL(final DownloadLink downloadLink) throws Exception {
        br.getPage(downloadLink.getDownloadURL());
        jd.plugins.hoster.VideoWeedCom.checkForContinueForm(this.br);
    }

    @Override
    public void handleFree(final DownloadLink downloadLink) throws Exception, PluginException {
        requestFileInformation(downloadLink);
        doFree(downloadLink, true, 0, "free_directlink");
    }

    private void doFree(final DownloadLink downloadLink, final boolean resumable, final int maxchunks, final String directlinkproperty) throws Exception, PluginException {
        dllink = checkDirectLink(downloadLink, directlinkproperty);
        if (server_issues) {
            throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Unknown server error", 10 * 60 * 1000l);
        }
        if (dllink == null) {
            if (downloadLink.getDownloadURL().contains("video")) {
                accessMainURL(downloadLink);
                dllink = getDllink(br);
                if (dllink == null) {
                    throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                }
            } else {
                // handling für "nicht"-video Links
                if (br.containsHTML(TEMPORARYUNAVAILABLE)) {
                    throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, JDL.L("plugins.hoster.novaupmovcom.temporaryunavailable", TEMPORARYUNAVAILABLEUSERTEXT), 30 * 60 * 1000l);
                }
                br.setFollowRedirects(false);
                br.getPage(downloadLink.getDownloadURL());
                dllink = br.getRegex("class= \"click_download\"><a href=\"(http://.*?)\"").getMatch(0);
                if (dllink == null) {
                    dllink = br.getRegex("\"(http://e\\d+\\.(?:novaup\\.com|" + Pattern.quote(DOMAIN) + "/dl/[a-z0-9]+/[a-z0-9]+/.*?)\"").getMatch(0);
                }
                if (dllink == null) {
                    throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                }
            }
        }
        dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, dllink, resumable, maxchunks);
        if (dl.getConnection().getContentType().contains("html")) {
            handleServerErrors();
            br.followConnection();
            throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Unknown server error", 10 * 60 * 1000l);
        }
        downloadLink.setProperty(directlinkproperty, dl.getConnection().getURL().toString());
        dl.startDownload();
    }

    private void handleServerErrors() throws PluginException {
        if (dl.getConnection().getResponseCode() == 403) {
            throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server error 403", 5 * 60 * 1000l);
        }
    }

    private String unWise() {
        String result = null;
        String fn = br.getRegex("eval\\((function\\(.*?\'\\))\\);").getMatch(0);
        if (fn == null) {
            return null;
        }
        final ScriptEngineManager manager = JavaScriptEngineFactory.getScriptEngineManager(this);
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
            logger.log(e);
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

    public static String getDllink(final Browser br) {
        return br.getRegex("(/download\\.php\\?file=[^<>\"]+)").getMatch(0);
    }

    private static Object LOCK = new Object();

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
                            br.setCookie(DOMAIN, key, value);
                        }
                        return;
                    }
                }
                br.setFollowRedirects(true);
                br.postPage("http://www." + DOMAIN + "/login.php?return=", "user=" + Encoding.urlEncode(account.getUser()) + "&pass=" + Encoding.urlEncode(account.getPass()));
                final String passcookie = br.getCookie(DOMAIN, "pass");
                if (passcookie == null || "deleted".equals(passcookie)) {
                    if ("de".equalsIgnoreCase(System.getProperty("user.language"))) {
                        throw new PluginException(LinkStatus.ERROR_PREMIUM, "\r\nUngültiger Benutzername oder ungültiges Passwort!\r\nSchnellhilfe: \r\nDu bist dir sicher, dass dein eingegebener Benutzername und Passwort stimmen?\r\nFalls dein Passwort Sonderzeichen enthält, ändere es und versuche es erneut!", PluginException.VALUE_ID_PREMIUM_DISABLE);
                    } else {
                        throw new PluginException(LinkStatus.ERROR_PREMIUM, "\r\nInvalid username/password!\r\nQuick help:\r\nYou're sure that the username and password you entered are correct?\r\nIf your password contains special characters, change it (remove them) and try again!", PluginException.VALUE_ID_PREMIUM_DISABLE);
                    }
                }
                // Save cookies
                final HashMap<String, String> cookies = new HashMap<String, String>();
                final Cookies add = br.getCookies(DOMAIN);
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
            ai.setValidUntil(TimeFormatter.getMilliSeconds(expire, "yyyy-MMM-dd", Locale.ENGLISH));
            account.setType(AccountType.PREMIUM);
            account.setMaxSimultanDownloads(-1);
            account.setConcurrentUsePossible(true);
            ai.setStatus("Premium Account");
        } else {
            account.setType(AccountType.FREE);
            /* free accounts can still have captcha */
            account.setMaxSimultanDownloads(-1);
            account.setConcurrentUsePossible(false);
            ai.setStatus("Free Account");
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
            doFree(link, true, 0, "account_free_directlink");
        } else {
            String dllink = this.checkDirectLink(link, "premium_directlink");
            if (dllink == null) {
                dllink = br.getRegex("\"(https?://[^<>\"]*?)\" class=\"btn\">Download this video</a>").getMatch(0);
                if (dllink == null) {
                    logger.warning("Final downloadlink (String is \"dllink\") regex didn't match!");
                    throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                }
            }
            dl = jd.plugins.BrowserAdapter.openDownload(br, link, dllink, true, 0);
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
    public void reset() {
    }

    @Override
    public void resetDownloadlink(final DownloadLink link) {
    }

    @Override
    public SiteTemplate siteTemplateType() {
        return SiteTemplate.Unknown_VideoHosting;
    }
}