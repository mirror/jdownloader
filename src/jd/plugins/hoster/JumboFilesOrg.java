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
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import jd.PluginWrapper;
import jd.config.Property;
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
import org.appwork.utils.formatter.TimeFormatter;

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "jumbofiles.org" }, urls = { "http://(www\\.)?jumbofiles\\.org/(linkfile|newfile)\\?n=\\d+" }, flags = { 2 })
public class JumboFilesOrg extends PluginForHost {

    public JumboFilesOrg(PluginWrapper wrapper) {
        super(wrapper);
        this.enablePremium("http://jumbofiles.org/premium");
    }

    @Override
    public String getAGBLink() {
        return "http://jumbofiles.org/tos";
    }

    private static final String PREMIUMONLYLINK = "http://(www\\.)?jumbofiles\\.org/linkfile\\?n=\\d+";

    @Override
    public AvailableStatus requestFileInformation(final DownloadLink link) throws IOException, PluginException {
        this.setBrowserExclusive();
        br.setFollowRedirects(true);
        br.getPage(link.getDownloadURL());
        if (br.getURL().equals("http://jumbofiles.org/index")) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        String filename = null;
        if (link.getDownloadURL().matches(PREMIUMONLYLINK)) {
            filename = br.getRegex("<h2>Download File ([^<>\"]*?)</h2>").getMatch(0);
            if (filename == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            link.getLinkStatus().setStatusText("Only downloadable for premium users!");
        } else {
            final Regex fileInfo = br.getRegex("<h2>Download File ([^<>\"]*?)<span id=\"span1\">\\(([^<>\"]*?)\\)</span></h2>");
            filename = fileInfo.getMatch(0);
            final String filesize = fileInfo.getMatch(1);
            if (filename == null || filesize == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            link.setDownloadSize(SizeFormatter.getSize(filesize));
        }
        link.setName(Encoding.htmlDecode(filename.trim()));
        return AvailableStatus.TRUE;
    }

    @Override
    public void handleFree(final DownloadLink downloadLink) throws Exception, PluginException {
        requestFileInformation(downloadLink);
        if (downloadLink.getDownloadURL().matches(PREMIUMONLYLINK)) throw new PluginException(LinkStatus.ERROR_FATAL, "Only downloadable for premium users!");
        if (br.containsHTML("of free download slots for your country")) throw new PluginException(LinkStatus.ERROR_HOSTER_TEMPORARILY_UNAVAILABLE, "No free slots available for your country", 10 * 60 * 1000l);
        if (true) throw new PluginException(LinkStatus.ERROR_FATAL, "Free mode is not (yet) supported!");
        // final String dllink = br.getRegex("").getMatch(0);
        // if (dllink == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        // dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, dllink, true, 0);
        // if (dl.getConnection().getContentType().contains("html")) {
        // br.followConnection();
        // throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        // }
        // dl.startDownload();
    }

    private static final String MAINPAGE = "http://jumbofiles.org";
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
                            this.br.setCookie(MAINPAGE, key, value);
                        }
                        return;
                    }
                }
                br.setFollowRedirects(true);
                /** Login captcha is needed via site but admin gave us a bypass parameter, do NOT implement the login captcha!! */
                br.postPage("http://jumbofiles.org/login", "sukey=yes&id=" + Encoding.urlEncode(account.getUser()) + "&password=" + Encoding.urlEncode(account.getPass()));
                if (!br.containsHTML("Account type:<br> <h3>Premium<")) throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_DISABLE);
                // Save cookies
                final HashMap<String, String> cookies = new HashMap<String, String>();
                final Cookies add = this.br.getCookies(MAINPAGE);
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
            return ai;
        }
        br.getPage("http://jumbofiles.org/my_account");
        ai.setUnlimitedTraffic();
        final String expire = br.getRegex("<\\!\\-\\-Premium exp: ([^<>\"]*?)\\-\\->").getMatch(0);
        if (expire == null) {
            account.setValid(false);
            return ai;
        } else {
            ai.setValidUntil(TimeFormatter.getMilliSeconds(expire, "yyyy-MM-dd", Locale.ENGLISH));
        }
        account.setValid(true);
        ai.setStatus("Premium User");
        return ai;
    }

    @Override
    public void handlePremium(final DownloadLink link, final Account account) throws Exception {
        requestFileInformation(link);
        login(account, false);
        br.setFollowRedirects(true);
        dl = jd.plugins.BrowserAdapter.openDownload(br, link, link.getDownloadURL(), true, 0);
        if (dl.getConnection().getContentType().contains("html")) {
            logger.warning("The final dllink seems not to be a file!");
            br.followConnection();
            if (br.containsHTML("HTTP/1\\.1 404 Not Found")) throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server error", 10 * 60 * 1000l);
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        dl.startDownload();
    }

    @Override
    public int getMaxSimultanPremiumDownloadNum() {
        return -1;
    }

    @Override
    public void reset() {
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return -1;
    }

    @Override
    public void resetDownloadlink(DownloadLink link) {
    }

}