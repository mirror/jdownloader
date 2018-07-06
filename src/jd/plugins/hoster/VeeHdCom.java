//jDownloader - Downloadmanager
//Copyright (C) 2012  JD-Team support@jdownloader.org
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
import java.util.Map;

import jd.PluginWrapper;
import jd.config.Property;
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

import org.jdownloader.plugins.components.antiDDoSForHost;

@HostPlugin(revision = "$Revision$", interfaceVersion = 3, names = { "veehd.com" }, urls = { "https?://(?:www\\.)?veehd\\.com/video/\\d+" })
public class VeeHdCom extends antiDDoSForHost {
    public VeeHdCom(PluginWrapper wrapper) {
        super(wrapper);
        this.enablePremium("http://veehd.com/register");
    }

    @Override
    public String getAGBLink() {
        return "http://veehd.com/guidelines";
    }

    /* More simultan downloads will cause server errors */
    private static final int     FREE_MAXDOWNLOADS = 2;
    private static final boolean registered_only   = false;

    @Override
    public AvailableStatus requestFileInformation(final DownloadLink link) throws Exception {
        this.setBrowserExclusive();
        br.setFollowRedirects(true);
        br.setCookie("http://veehd.com/", "pref", "1.1");
        getPage(link.getDownloadURL());
        if (br.containsHTML(">This is a private video") || br.getURL().contains("/?removed=") || br.containsHTML("This video has been removed due")) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        String filename = br.getRegex("<h2 style=\"\">([^<>]*?) \\| <font").getMatch(0);
        if (filename == null) {
            filename = br.getRegex("<title>([^<>]*?) on Veehd</title>").getMatch(0);
        }
        if (filename == null) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        link.setName(Encoding.htmlDecode(filename.trim()).replace("\"", "'") + ".avi");
        return AvailableStatus.TRUE;
    }

    @Override
    public void handleFree(final DownloadLink downloadLink) throws Exception, PluginException {
        requestFileInformation(downloadLink);
        doFree(downloadLink, null);
    }

    @SuppressWarnings("unused")
    public void doFree(final DownloadLink downloadLink, final Account acc) throws Exception, PluginException {
        if (registered_only && acc == null) {
            throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_ONLY);
        }
        final String ts = br.getRegex("var ts = \"([^<>\"]*?)\"").getMatch(0);
        final String sign = br.getRegex("var sgn = \"([^<>\"]*?)\"").getMatch(0);
        if (ts != null && sign != null) {
            /* Pretend to use their stupid toolbar/plugin* - broken/not needed anymore! */
            // br.cloneBrowser().postPage("http://veehd.com/xhrp", "v=c2&p=1&ts=" + Encoding.urlEncode(ts) + "&sgn=" +
            // Encoding.urlEncode(sign));
            /* Count as view */
            getPage(br.cloneBrowser(), "/xhr?h=views." + new Regex(downloadLink.getDownloadURL(), "(\\d+)$").getMatch(0) + ".0");
        }
        String dllink = null;
        String way_download = br.getRegex("\"(/vpi\\?[^<>\"/]*?\\&do=d[^<>\"]*?)\"").getMatch(0);
        if (way_download != null) {
            synchronized (LOCK) {
                getPage(way_download);
                final String iframe = br.getRegex("<iframe id=\"iframe\" src=\"(/va/[^<>\"]*?)\"").getMatch(0);
                if (iframe != null) {
                    /*
                     * Seems to be some kind of ad-stuff - happens one time every time a user has a NEW ip and logs into his account for the
                     * first time
                     */
                    getPage(iframe);
                    /* Access downloadlink again - final link should now be in the HTML code */
                    getPage(way_download);
                }
            }
            dllink = getDirectlink();
        } else {
            final String frame = br.getRegex("\"(/vpi\\?h=[^<>\"]*?)\"").getMatch(0);
            if (frame == null) {
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            getPage(frame);
            if (br.containsHTML("Too Many Requests")) {
                throw new PluginException(LinkStatus.ERROR_HOSTER_TEMPORARILY_UNAVAILABLE, "Too many connections - wait before starting new downloads", 5 * 60 * 1000l);
            }
            dllink = getDirectlink();
            if (dllink == null) {
                dllink = br.getRegex("\"url\":\"(http[^<>\"]*?)\"").getMatch(0);
                // Only available when plugin needed
                if (dllink == null) {
                    dllink = br.getRegex("<embed type=\"video/divx\" src=\"(https?://[^<>\"]*?)\"").getMatch(0);
                }
            }
        }
        if (br.containsHTML(">429 Too Many Requests")) {
            logger.info("Server: '429 Too Many Requests' --> Wait 5 minutes");
            throw new PluginException(LinkStatus.ERROR_HOSTER_TEMPORARILY_UNAVAILABLE, "Wait before starting new downloads", 5 * 60 * 1001l);
        }
        /* This case usually won't happen */
        if (dllink == null && acc == null) {
            try {
                throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_ONLY);
            } catch (final Throwable e) {
                if (e instanceof PluginException) {
                    throw (PluginException) e;
                }
            }
            throw new PluginException(LinkStatus.ERROR_FATAL, "This file can only be downloaded by registered users");
        } else if (dllink == null) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        dllink = Encoding.htmlDecode(dllink.trim());
        String finalfilename = downloadLink.getName();
        if (dllink.contains(".mp4")) {
            finalfilename = finalfilename.replace(".avi", ".mp4");
        }
        downloadLink.setFinalFileName(finalfilename);
        // More chunks possible but will cause server errors
        dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, dllink, true, 1);
        if (dl.getConnection().getContentType().contains("html")) {
            if (dl.getConnection().getResponseCode() == 403) {
                throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server error", 30 * 30 * 1000l);
            }
            br.followConnection();
            if (br.containsHTML("No htmlCode read")) {
                throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server error", 30 * 30 * 1000l);
            }
            if (br.containsHTML("^404$")) {
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            }
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        dl.startDownload();
    }

    private String getDirectlink() {
        return br.getRegex("\"(https?://v\\d+\\.veehd\\.com/dl/[^<>\"]*?)\"").getMatch(0);
    }

    private static final String MAINPAGE = "http://veehd.com";
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
                        for (final Map.Entry<String, String> cookieEntry : cookies.entrySet()) {
                            final String key = cookieEntry.getKey();
                            final String value = cookieEntry.getValue();
                            this.br.setCookie(MAINPAGE, key, value);
                        }
                        return;
                    }
                }
                br.setFollowRedirects(false);
                postPage("https://veehd.com/login", "submit=Login&terms=on&remember_me=on&login_invisible=on&ref=http%3A%2F%2Fveehd.com%2F&uname=" + Encoding.urlEncode(account.getUser()) + "&pword=" + Encoding.urlEncode(account.getPass()));
                if (br.getCookie(MAINPAGE, "remember") == null || br.getCookie(MAINPAGE, "remember").equals("0")) {
                    if ("de".equalsIgnoreCase(System.getProperty("user.language"))) {
                        throw new PluginException(LinkStatus.ERROR_PREMIUM, "\r\nUngültiger Benutzername oder ungültiges Passwort!\r\nSchnellhilfe: \r\nDu bist dir sicher, dass dein eingegebener Benutzername und Passwort stimmen?\r\nFalls dein Passwort Sonderzeichen enthält, ändere es und versuche es erneut!", PluginException.VALUE_ID_PREMIUM_DISABLE);
                    } else {
                        throw new PluginException(LinkStatus.ERROR_PREMIUM, "\r\nInvalid username/password!\r\nQuick help:\r\nYou're sure that the username and password you entered are correct?\r\nIf your password contains special characters, change it (remove them) and try again!", PluginException.VALUE_ID_PREMIUM_DISABLE);
                    }
                }
                account.setProperty("name", Encoding.urlEncode(account.getUser()));
                account.setProperty("pass", Encoding.urlEncode(account.getPass()));
                account.setProperty("cookies", fetchCookies(MAINPAGE));
            } catch (final PluginException e) {
                account.setProperty("cookies", Property.NULL);
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
        try {
            account.setType(AccountType.FREE);
            account.setMaxSimultanDownloads(FREE_MAXDOWNLOADS);
            account.setConcurrentUsePossible(true);
        } catch (final Throwable e) {
            /* not available in old Stable 0.9.581 */
        }
        ai.setStatus("Free Account");
        account.setValid(true);
        return ai;
    }

    @Override
    public void handlePremium(final DownloadLink link, final Account account) throws Exception {
        requestFileInformation(link);
        login(account, false);
        br.setFollowRedirects(false);
        getPage(link.getDownloadURL());
        doFree(link, account);
    }

    @Override
    public void reset() {
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return FREE_MAXDOWNLOADS;
    }

    @Override
    public int getMaxSimultanPremiumDownloadNum() {
        return FREE_MAXDOWNLOADS;
    }

    @Override
    public void resetDownloadlink(DownloadLink link) {
    }
}