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

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

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

import org.appwork.utils.formatter.SizeFormatter;

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "magnovideo.com" }, urls = { "http://(www\\.)?magnovideo\\.com/\\?(v|d)=[A-Z0-9]+" }, flags = { 2 })
public class MagnoVideoCom extends PluginForHost {

    public MagnoVideoCom(PluginWrapper wrapper) {
        super(wrapper);
        this.enablePremium("http://www.magnovideo.com/pay_subscriptions.php");
    }

    @Override
    public String getAGBLink() {
        return "http://www.magnovideo.com/pages.php?p=DMCA";
    }

    public void correctDownloadLink(final DownloadLink link) {
        link.setUrlDownload("http://www.magnovideo.com/?d=" + getFID(link));
    }

    @Override
    public AvailableStatus requestFileInformation(final DownloadLink link) throws IOException, PluginException {
        this.setBrowserExclusive();
        // Offline links should also have nice filenames
        link.setName(getFID(link));
        br.setFollowRedirects(true);
        prepBR(this.br);
        br.getPage("http://www.magnovideo.com/?d=" + getFID(link));
        if (br.containsHTML("<title>File share metatitle \\- New MagnoVideo</title>|<title>Magnovideo\\.com \\- Almacenamiento y uso compartido de v\\&iacute;deo gratis</title>|<title>Magnovideo\\.com \\- Almacenamiento y uso compartido de vídeo gratis</title>")) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        final String filename = br.getRegex("<title>([^<>\"]*?)</title>").getMatch(0);
        final Regex info = br.getRegex("<h3>You would like to download the following file:</h3>[\t\n\r ]+<p>([^<>\"]*?) \\| (\\d+(\\.\\d+)? [A-Za-z]{1,5})</p>");
        final String filesize = info.getMatch(1);
        if (filename == null || filesize == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        link.setName(Encoding.htmlDecode(filename.trim()));
        link.setDownloadSize(SizeFormatter.getSize(filesize));

        return AvailableStatus.TRUE;
    }

    @Override
    public void handleFree(final DownloadLink downloadLink) throws Exception, PluginException {
        requestFileInformation(downloadLink);
        final String fid = getFID(downloadLink);
        br.getPage("http://www.magnovideo.com/?v=JCRFATY" + fid);
        br.setCookie(br.getHost(), "prepage", fid);
        br.setCookie(br.getHost(), "sharez_key", fid);
        br.setCookie(br.getHost(), "user_timezone", "2");
        br.getPage("http://www.magnovideo.com/player_config.php?mdid=" + fid);
        final String firstFrame = br.getRegex("<first_frame>(http://[^<>\"]*?)</first_frame>").getMatch(0);
        if (firstFrame == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        final String dllink = firstFrame.replace("/large/1.jpg", "/1.mp4");

        dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, dllink, true, 1);
        fixFilename(downloadLink);
        if (dl.getConnection().getContentType().contains("html")) {
            br.followConnection();
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        dl.startDownload();
    }

    private static final String MAINPAGE = "http://magnovideo.com";
    private static Object       LOCK     = new Object();

    @SuppressWarnings("unchecked")
    private void login(final Account account, final boolean force) throws Exception {
        synchronized (LOCK) {
            try {
                // Load cookies
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
                            br.setCookie(MAINPAGE, key, value);
                        }
                        return;
                    }
                }
                br.setFollowRedirects(false);
                prepBR(this.br);
                br.postPage("http://www.magnovideo.com/login.php", "rememberme=on&user=" + Encoding.urlEncode(account.getUser()) + "&pass=" + Encoding.urlEncode(account.getPass()));
                if (br.containsHTML(">User or password invalid<")) {
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
        AccountInfo ai = new AccountInfo();
        try {
            login(account, true);
        } catch (PluginException e) {
            account.setValid(false);
            throw e;
        }
        ai.setUnlimitedTraffic();
        br.getPage("http://www.magnovideo.com/my_profile_edit.php");
        final String daysLeft = br.getRegex(">Premium days left:&nbsp;(\\d+)</label>").getMatch(0);
        if (daysLeft == null) {
            account.setValid(false);
            return ai;
        } else {
            ai.setValidUntil(System.currentTimeMillis() + Integer.parseInt(daysLeft) * 24 * 60 * 60 * 1000l);
        }
        account.setValid(true);
        ai.setStatus("Premium User");
        return ai;
    }

    @Override
    public void handlePremium(final DownloadLink link, final Account account) throws Exception {
        requestFileInformation(link);
        login(account, true);
        br.setFollowRedirects(false);
        String dllink = "http://www.magnovideo.com/?v=" + getFID(link) + "&ref=fd";
        br.getPage(dllink);
        dllink = br.getRedirectLocation();
        if (dllink == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        dl = jd.plugins.BrowserAdapter.openDownload(br, link, Encoding.htmlDecode(dllink), true, 0);
        if (dl.getConnection().getContentType().contains("html")) {
            logger.warning("The final dllink seems not to be a file!");
            br.followConnection();
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        dl.startDownload();
    }

    @Override
    public int getMaxSimultanPremiumDownloadNum() {
        return -1;
    }

    private void fixFilename(final DownloadLink downloadLink) {
        final String serverFilename = Encoding.htmlDecode(getFileNameFromHeader(dl.getConnection()));
        final String newExtension = serverFilename.substring(serverFilename.lastIndexOf("."));
        if (newExtension != null && !downloadLink.getName().endsWith(newExtension)) {
            final String oldExtension = downloadLink.getName().substring(downloadLink.getName().lastIndexOf("."));
            if (oldExtension != null)
                downloadLink.setFinalFileName(downloadLink.getName().replace(oldExtension, newExtension));
            else
                downloadLink.setFinalFileName(downloadLink.getName() + newExtension);
        }
    }

    private String getFID(final DownloadLink dl) {
        return new Regex(dl.getDownloadURL(), "([A-Z0-9]+)$").getMatch(0);
    }

    private void prepBR(final Browser br) {
        br.getHeaders().put("Accept-Language", "de,en-us;q=0.7,en;q=0.3");
        br.setCookie(MAINPAGE, "lang", "en-us");
    }

    @Override
    public void reset() {
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return 3;
    }

    @Override
    public void resetDownloadlink(final DownloadLink link) {
    }

}