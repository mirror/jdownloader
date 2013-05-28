//    jDownloader - Downloadmanager
//    Copyright (C) 2012  JD-Team support@jdownloader.org
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
import java.util.Map;

import jd.PluginWrapper;
import jd.config.Property;
import jd.http.Browser;
import jd.http.Cookie;
import jd.http.Cookies;
import jd.nutils.encoding.Encoding;
import jd.parser.html.Form;
import jd.plugins.Account;
import jd.plugins.AccountInfo;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;

import org.appwork.utils.formatter.SizeFormatter;

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "deviantart.com" }, urls = { "https?://[\\w\\.\\-]*?deviantart\\.com/art/[\\w\\-]+" }, flags = { 2 })
public class DeviantArtCom extends PluginForHost {

    /**
     * @author raztoki
     */

    private static final String COOKIE_HOST = "http://www.deviantart.com";
    private static Object       LOCK        = new Object();

    public DeviantArtCom(PluginWrapper wrapper) {
        super(wrapper);
        this.enablePremium(COOKIE_HOST.replace("http://", "https://") + "/join/");
    }

    @Override
    public String getAGBLink() {
        return COOKIE_HOST + "/";
    }

    @Override
    public void correctDownloadLink(final DownloadLink link) throws Exception {
        link.setUrlDownload(link.getDownloadURL().replace("DEVART://", ""));
    }

    @Override
    public AvailableStatus requestFileInformation(final DownloadLink link) throws IOException, PluginException {
        this.setBrowserExclusive();
        br.setFollowRedirects(true);
        br.getPage(link.getDownloadURL());
        if (br.containsHTML("/error\\-title\\-oops\\.png\\)")) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        final String filename = br.getRegex("<title>([^<>\"]*?) on deviantART</title>").getMatch(0);
        final String ext = br.getRegex("<strong>Download Image</strong><br><small>([A-Za-z0-9]{1,5}),").getMatch(0);
        final String filesize = br.getRegex("<label>Image Size:</label>([^<>\"]*?)<br>").getMatch(0);
        if (filename == null || filesize == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        link.setFinalFileName(Encoding.htmlDecode(filename.trim()) + "." + ext.trim().toLowerCase());
        link.setDownloadSize(SizeFormatter.getSize(filesize.replace(",", "")));
        return AvailableStatus.TRUE;
    }

    @Override
    public void handleFree(final DownloadLink downloadLink) throws Exception, PluginException {
        requestFileInformation(downloadLink);
        final String dllink = getDllink();
        // Disable chunks as we only download pictures
        dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, dllink, true, 1);
        if (dl.getConnection().getContentType().contains("html")) {
            br.followConnection();
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        dl.startDownload();
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return -1;
    }

    @Override
    public void handlePremium(final DownloadLink downloadLink, final Account account) throws Exception {
        login(account, br, false);
        br.setFollowRedirects(true);
        br.getPage(downloadLink.getDownloadURL());
        final String dllink = getDllink();
        // Disable chunks as we only download pictures
        dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, dllink, true, 1);
        if (dl.getConnection().getContentType().contains("html")) {
            br.followConnection();
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        dl.startDownload();
    }

    private String getDllink() throws PluginException {
        String dllink = null;
        if (br.containsHTML(">Mature Content</span>") && !br.containsHTML(">Mature Content Filter<")) {
            dllink = br.getRegex("class=\"thumb ismature\" href=\"" + br.getURL() + "\" title=\"[^<>\"/]+\" data\\-super\\-img=\"(http://[^<>\"]*?)\"").getMatch(0);
        } else {
            dllink = br.getRegex("name=\"og:image\" content=\"(http://[^<>\"]*?)\"").getMatch(0);
        }
        if (dllink == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        return dllink;
    }

    @Override
    public int getMaxSimultanPremiumDownloadNum() {
        return -1;
    }

    @Override
    public AccountInfo fetchAccountInfo(Account account) throws Exception {
        AccountInfo ai = new AccountInfo();
        try {
            login(account, this.br, true);
        } catch (PluginException e) {
            account.setValid(false);
            return ai;
        }
        account.setValid(true);
        ai.setStatus("Free Registered User");
        return ai;
    }

    @SuppressWarnings("unchecked")
    public void login(Account account, Browser br, final boolean force) throws Exception {
        synchronized (LOCK) {
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
                            br.setCookie(this.getHost(), key, value);
                        }
                        return;
                    }
                }
                br.setCookie(this.getHost(), "lang", "english");
                br.getPage("https://www.deviantart.com/users/login");
                Form loginform = br.getFormbyProperty("id", "login");
                if (loginform == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                loginform.put("username", Encoding.urlEncode(account.getUser()));
                loginform.put("password", Encoding.urlEncode(account.getPass()));
                loginform.put("remember_me", "1");
                br.submitForm(loginform);
                if (br.getRedirectLocation() != null) {
                    if (br.getRedirectLocation().contains("deviantart.com/users/wrong-password?")) throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_DISABLE);
                }
                /** Save cookies */
                final HashMap<String, String> cookies = new HashMap<String, String>();
                final Cookies add = br.getCookies(this.getHost());
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
    public void reset() {
    }

    @Override
    public void resetDownloadlink(DownloadLink link) {
    }

}