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
import java.util.regex.Pattern;

import jd.PluginWrapper;
import jd.config.Property;
import jd.http.Browser;
import jd.http.Cookie;
import jd.http.Cookies;
import jd.http.URLConnectionAdapter;
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

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "flyfiles.net" }, urls = { "https?://(www\\.)?flyfiles\\.net/[a-z0-9]{10}" }, flags = { 2 })
public class FileFilesNet extends PluginForHost {

    private static final String HOST = "http://flyfiles.net";
    private static Object       LOCK = new Object();

    // TODO: rename plugin when jd2 goes stable, FlyFilesNet

    // DEV NOTES
    // mods:
    // non account: 3 * 1
    // free account:
    // premium account: 6 * 1
    // protocol: has https but is fubar.
    // other: no redirects

    @Override
    public void correctDownloadLink(DownloadLink link) {
        link.setUrlDownload(link.getDownloadURL().replace("https://", "http://"));
    }

    public FileFilesNet(PluginWrapper wrapper) {
        super(wrapper);
        this.enablePremium(HOST + "/");
    }

    // do not add @Override here to keep 0.* compatibility
    public boolean hasAutoCaptcha() {
        return false;
    }

    // do not add @Override here to keep 0.* compatibility
    public boolean hasCaptcha() {
        return false;
    }

    @Override
    public String getAGBLink() {
        return HOST + "/terms.php";
    }

    public void prepBrowser() {
        // define custom browser headers and language settings.
        br.getHeaders().put("Accept-Language", "en-gb, en;q=0.9, de;q=0.8");
        br.setCookie(HOST, "lang", "english");
    }

    @Override
    public AvailableStatus requestFileInformation(DownloadLink link) throws IOException, PluginException {
        this.setBrowserExclusive();
        br.getPage(link.getDownloadURL());
        if (br.containsHTML(">[\r\n\t ]+File not found\\![\r\n\t ]+<")) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        String[][] fileInfo = br.getRegex(Pattern.compile("<div id=\"file_det\" style=\"top:\\d+%;\">[\r\n\t ]+(.+) \\- ([\\d\\.]+ (KB|MB|GB|TB))", Pattern.CASE_INSENSITIVE)).getMatches();
        if (fileInfo == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        link.setName(Encoding.htmlDecode(fileInfo[0][0].trim()));
        link.setDownloadSize(SizeFormatter.getSize(fileInfo[0][1]));
        return AvailableStatus.TRUE;
    }

    @Override
    public void handleFree(DownloadLink downloadLink) throws Exception, PluginException {
        String dllink = (checkDirectLink(downloadLink, "directlink"));
        if (dllink == null) {
            requestFileInformation(downloadLink);
            br.postPage(HOST + "/", "getDownLink=" + new Regex(downloadLink.getDownloadURL(), "net/(.*)").getMatch(0));
            // they don't show any info about limits or waits. You seem to just
            // get '#' instead of link.
            if (br.containsHTML("#downlink\\|#")) throw new PluginException(LinkStatus.ERROR_IP_BLOCKED, "Hoster connection limit reached.", 10 * 60 * 1000l);
            dllink = getDllink();
            if (dllink == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, dllink, true, 1);
        if (dl.getConnection().getContentType().contains("html")) {
            br.followConnection();
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        downloadLink.setProperty("directlink", dllink);
        dl.startDownload();
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return 1;
    }

    private String checkDirectLink(DownloadLink downloadLink, String property) {
        String dllink = downloadLink.getStringProperty(property);
        if (dllink != null) {
            try {
                Browser br2 = br.cloneBrowser();
                URLConnectionAdapter con = br2.openGetConnection(dllink);
                if (con.getContentType().contains("html") || con.getLongContentLength() == -1) {
                    downloadLink.setProperty(property, Property.NULL);
                    dllink = null;
                }
                con.disconnect();
            } catch (Exception e) {
                downloadLink.setProperty(property, Property.NULL);
                dllink = null;
            }
        }
        return dllink;
    }

    public String getDllink() {
        String dllink = br.getRedirectLocation();
        if (dllink == null) {
            dllink = br.getRegex("#downlink\\|(https?://\\w+\\.flyfiles\\.net/\\w+/.+)").getMatch(0);
            if (dllink == null) {
                dllink = br.getRegex("(https?://.+)").getMatch(0);
            }
        }
        return dllink;
    }

    @Override
    public AccountInfo fetchAccountInfo(Account account) throws Exception {
        AccountInfo ai = new AccountInfo();
        try {
            login(account, true);
        } catch (PluginException e) {
            account.setValid(false);
            return ai;
        }
        String expire = new Regex(br, "(?i)<u>Premium</u>: <span>(\\d{4}\\-\\d{2}\\-\\d{2} \\d{2}:\\d{2}:\\d{2})</span>").getMatch(0);
        if (expire == null) {
            ai.setExpired(true);
            account.setValid(false);
            return ai;
        }
        ai.setValidUntil(TimeFormatter.getMilliSeconds(expire, "yyyy-MM-dd hh:mm:ss", null));
        ai.setUnlimitedTraffic();
        ai.setStatus("Premium User");
        account.setValid(true);
        return ai;
    }

    @SuppressWarnings("unchecked")
    private void login(Account account, boolean force) throws Exception {
        synchronized (LOCK) {
            try {
                /** Load cookies */
                prepBrowser();
                br.setCookiesExclusive(true);
                prepBrowser();
                final Object ret = account.getProperty("cookies", null);
                boolean acmatch = Encoding.urlEncode(account.getUser()).equals(account.getStringProperty("name", Encoding.urlEncode(account.getUser())));
                if (acmatch) acmatch = Encoding.urlEncode(account.getPass()).equals(account.getStringProperty("pass", Encoding.urlEncode(account.getPass())));
                if (acmatch && ret != null && ret instanceof HashMap<?, ?> && !force) {
                    final HashMap<String, String> cookies = (HashMap<String, String>) ret;
                    if (account.isValid()) {
                        for (final Map.Entry<String, String> cookieEntry : cookies.entrySet()) {
                            final String key = cookieEntry.getKey();
                            final String value = cookieEntry.getValue();
                            this.br.setCookie(HOST, key, value);
                        }
                        return;
                    }
                }
                br.getPage(HOST);
                br.postPage(HOST + "/login.html", "user=" + Encoding.urlEncode(account.getUser()) + "&pass=" + Encoding.urlEncode(account.getPass()));
                if (!br.containsHTML("#login\\|1")) throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_DISABLE);
                br.getPage("/");
                // only support premium at this stage
                if (!new Regex(br, "(?i)(<u>Premium</u>: <span>\\d+{4}\\-\\d{2}\\-\\d{2} \\d{2}:\\d{2}:\\d{2}</span>)").matches()) throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_DISABLE);
                /** Save cookies */
                final HashMap<String, String> cookies = new HashMap<String, String>();
                final Cookies add = this.br.getCookies(HOST);
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
    public void handlePremium(DownloadLink link, Account account) throws Exception {
        requestFileInformation(link);
        login(account, false);
        String dllink = checkDirectLink(link, "premlink");
        if (dllink == null) {
            br.postPage(HOST + "/", "getDownLink=" + new Regex(link.getDownloadURL(), "net/(.*)").getMatch(0));
            dllink = getDllink();
            // they don't show any info about limits or waits. You seem to just
            // get '#' instead of link.
            if (br.containsHTML("#downlink\\|#")) throw new PluginException(LinkStatus.ERROR_IP_BLOCKED, "Hoster connection limit reached.", 10 * 60 * 1000l);
            if (dllink == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        dl = jd.plugins.BrowserAdapter.openDownload(br, link, dllink, true, -6);
        if (dl.getConnection().getContentType().contains("html")) {
            br.followConnection();
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        link.setProperty("premlink", dllink);
        dl.startDownload();
    }

    @Override
    public int getMaxSimultanPremiumDownloadNum() {
        return 1;
    }

    @Override
    public void reset() {
    }

    @Override
    public void resetDownloadlink(DownloadLink link) {
    }

}