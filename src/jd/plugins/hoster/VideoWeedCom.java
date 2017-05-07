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

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;

import org.appwork.utils.StringUtils;
import org.appwork.utils.formatter.TimeFormatter;
import org.jdownloader.scripting.JavaScriptEngineFactory;

import jd.PluginWrapper;
import jd.config.Property;
import jd.http.Browser;
import jd.http.Cookie;
import jd.http.Cookies;
import jd.http.URLConnectionAdapter;
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
import jd.plugins.components.SiteType.SiteTemplate;

@HostPlugin(revision = "$Revision$", interfaceVersion = 3, names = { "bitvid.sx" }, urls = { "https?://(?:www\\.)?bitvid\\.sx/file/[a-z0-9]+|https?://(?:www\\.)?videoweed\\.(?:com|es)/.+[a-z0-9]+" })
public class VideoWeedCom extends PluginForHost {
    public VideoWeedCom(PluginWrapper wrapper) {
        super(wrapper);
        this.enablePremium("http://www.bitvid.sx/premium.php");
    }

    @SuppressWarnings("deprecation")
    public void correctDownloadLink(DownloadLink link) {
        final String fileID = new Regex(link.getDownloadURL(), "([a-z0-9]+)$").getMatch(0);
        link.setUrlDownload("http://www.bitvid.sx/file/" + fileID);
    }

    @Override
    public String rewriteHost(String host) {
        final String currentHost = getHost();
        if ("videoweed.com".equals(currentHost) || "videoweed.es".equals(currentHost)) {
            if (host == null || "videoweed.com".equals(host) || "videoweed.es".equals(host)) {
                return DOMAIN;
            }
        }
        return super.rewriteHost(host);
    }

    @Override
    public String getAGBLink() {
        return "http://www.bitvid.sx/terms.php";
    }

    /* Similar plugins: NovaUpMovcom, VideoWeedCom, NowVideoEu, MovShareNet */
    private final String DOMAIN = "bitvid.sx";

    @Override
    public AvailableStatus requestFileInformation(final DownloadLink downloadLink) throws Exception {
        this.setBrowserExclusive();
        br.setConnectTimeout(180 * 1000);
        br.setReadTimeout(180 * 1000);
        br.setFollowRedirects(true);
        br.getPage(downloadLink.getDownloadURL());
        checkForContinueForm(br);
        if (br.containsHTML("(>This file no longer exists on our servers\\.<|The video file was removed)")) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        String filename = br.getRegex("<h1 class=\"text_shadow\">(.*?)</h1>").getMatch(0);
        if (StringUtils.isEmpty(filename)) {
            filename = br.getRegex("name=\"title\" content=\"Watch (.*?) online \\| \\w+ \"").getMatch(0);
            if (StringUtils.isEmpty(filename)) {
                filename = br.getRegex("\\w+\\.com/file/[a-z0-9]+\\&title=(.*?)\\+\\-\\+\\w+\\.com\"").getMatch(0);
                if (StringUtils.isEmpty(filename)) {
                    filename = br.getRegex("<td><strong>Title: </strong>(.*?)</td>").getMatch(0);
                    if (StringUtils.isEmpty(filename)) {
                        filename = br.getRegex("<td width=\"580\">[\t\n\r ]+<div class=\"div_titlu\">(.*?) \\- <a").getMatch(0);
                        if (StringUtils.isEmpty(filename)) {
                            filename = br.getRegex("colspan=\"2\"><strong>Title: </strong>(.*?)</td>").getMatch(0);
                        }
                    }
                }
            }
        }
        if (filename == null) {
            // could be error... not defect
            hasError = errorCheck(downloadLink);
            if (Boolean.FALSE.equals(hasError)) {
                return AvailableStatus.FALSE;
            } else if (Boolean.TRUE.equals(hasError)) {
                return AvailableStatus.TRUE;
            }
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        filename = Encoding.htmlDecode(filename.trim());
        filename = removeDoubleExtensions(filename, "flv");
        downloadLink.setFinalFileName(filename);
        hasError = errorCheck(downloadLink);
        if (Boolean.FALSE.equals(hasError)) {
            return AvailableStatus.FALSE;
        } else {
            return AvailableStatus.TRUE;
        }
    }

    Boolean hasError = null;

    private Boolean errorCheck(final DownloadLink downloadLink) {
        if (br.containsHTML("class=\"vidError\"><p>This video is not yet ready! Please try again later!</p></div>")) {
            downloadLink.getLinkStatus().setStatusText("Not downloadable at the moment, try again later...");
            return Boolean.TRUE;
        }
        if (br.containsHTML("error_msg=The video is being transfered")) {
            downloadLink.getLinkStatus().setStatusText("Not downloadable at the moment, try again later...");
            return Boolean.TRUE;
        }
        if (br.containsHTML("error_msg=The video has failed to convert")) {
            downloadLink.getLinkStatus().setStatusText("Failed to convert.");
            return Boolean.FALSE;
        }
        if (br.containsHTML("error_msg=The video is converting")) {
            downloadLink.getLinkStatus().setStatusText("Server says: This video is converting");
            return Boolean.TRUE;
        }
        return null;
    }

    private String removeDoubleExtensions(String filename, final String defaultExtension) {
        if (filename == null) {
            return filename;
        }
        String ext_temp = null;
        int index = 0;
        while (filename.contains(".")) {
            /* First let's remove all video extensions */
            index = filename.lastIndexOf(".");
            ext_temp = filename.substring(index);
            if (ext_temp != null && ext_temp.matches("\\.(mp4|flv|mkv|avi)")) {
                filename = filename.substring(0, index);
                continue;
            }
            break;
        }
        /* Add wished default video extension */
        if (!filename.endsWith("." + defaultExtension)) {
            filename += "." + defaultExtension;
        }
        return filename;
    }

    public static final void checkForContinueForm(final Browser br) throws Exception {
        final Form f = br.getFormbyKey("stepkey");
        if (f != null) {
            br.submitForm(f);
        }
    }

    public String getDllink() {
        return br.getRegex("(/download\\.php\\?file=[^<>\"]+)").getMatch(0);
    }

    @Override
    public void handleFree(final DownloadLink downloadLink) throws Exception, PluginException {
        requestFileInformation(downloadLink);
        // error handling
        if (Boolean.TRUE.equals(hasError)) {
            throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE);
        }
        doFree(downloadLink, true, 0, "free_directlink");
    }

    private void doFree(final DownloadLink downloadLink, final boolean resumable, final int maxchunks, final String directlinkproperty) throws Exception, PluginException {
        String dllink = checkDirectLink(downloadLink, directlinkproperty);
        if (dllink == null) {
            dllink = getDllink();
            if (dllink == null) {
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
        }
        dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, dllink, resumable, maxchunks);
        if (dl.getConnection().getContentType().contains("html")) {
            br.followConnection();
            throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Unknown server error", 10 * 60 * 1000l);
        }
        downloadLink.setProperty(directlinkproperty, dl.getConnection().getURL().toString());
        dl.startDownload();
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

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return -1;
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
                        for (final Map.Entry<String, String> cookieEntry : cookies.entrySet()) {
                            final String key = cookieEntry.getKey();
                            final String value = cookieEntry.getValue();
                            br.setCookie(DOMAIN, key, value);
                        }
                        return;
                    }
                }
                br.setFollowRedirects(true);
                br.postPage("http://www.bitvid.sx/login.php", "user=" + Encoding.urlEncode(account.getUser()) + "&pass=" + Encoding.urlEncode(account.getPass()));
                if (br.getCookie(DOMAIN, "pass") == null || br.getURL().contains("e=1")) {
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
        login(account, true);
        br.getPage("/premium.php");
        ai.setUnlimitedTraffic();
        final String expire = br.getRegex(">Your premium membership expires on: ([^<>\"]*?)<").getMatch(0);
        if (expire == null) {
            account.setType(AccountType.FREE);
            /* free accounts can still have captcha */
            account.setMaxSimultanDownloads(-1);
            account.setConcurrentUsePossible(false);
            ai.setStatus("Free Account");
        } else {
            ai.setValidUntil(TimeFormatter.getMilliSeconds(expire, "yyyy-MMM-dd", Locale.ENGLISH));
            account.setType(AccountType.PREMIUM);
            account.setMaxSimultanDownloads(-1);
            account.setConcurrentUsePossible(true);
            ai.setStatus("Premium Account");
        }
        account.setValid(true);
        return ai;
    }

    @Override
    public void handlePremium(final DownloadLink link, final Account account) throws Exception {
        requestFileInformation(link);
        // error handling
        if (Boolean.TRUE.equals(hasError)) {
            throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE);
        }
        login(account, false);
        br.setFollowRedirects(false);
        br.getPage(link.getDownloadURL());
        if (AccountType.FREE.equals(account.getType())) {
            doFree(link, true, 0, "account_free_directlink");
        } else {
            String dllink = this.checkDirectLink(link, "premium_directlink");
            if (dllink == null) {
                br.getPage(link.getDownloadURL());
                dllink = br.getRegex("\"(http[^<>\"]*?/dl/[^<>\"]*?)\"").getMatch(0);
                if (dllink == null) {
                    logger.warning("Final downloadlink (String is \"dllink\") regex didn't match!");
                    throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                }
            }
            dl = jd.plugins.BrowserAdapter.openDownload(br, link, dllink, true, 0);
            if (dl.getConnection().getContentType().contains("html")) {
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
    public void resetDownloadlink(DownloadLink link) {
    }

    @Override
    public void resetPluginGlobals() {
    }

    @Override
    public SiteTemplate siteTemplateType() {
        return SiteTemplate.Unknown_VideoHosting;
    }
}