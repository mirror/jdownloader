//    jDownloader - Downloadmanager
//    Copyright (C) 2008  JD-Team support@jdownloader.org
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

import jd.PluginWrapper;
import jd.http.Browser;
import jd.http.RandomUserAgent;
import jd.nutils.encoding.Encoding;
import jd.plugins.Account;
import jd.plugins.AccountInfo;
import jd.plugins.DownloadLink;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;
import jd.plugins.DownloadLink.AvailableStatus;

import org.appwork.utils.formatter.TimeFormatter;

@HostPlugin(revision = "$Revision: 12761 $", interfaceVersion = 2, names = { "videobb.com" }, urls = { "http://(www\\.)?videobb\\.com/(video/|watch_video\\.php\\?v=|e/)\\w+" }, flags = { 2 })
public class VideoBbCom extends PluginForHost {

    private static final Object LOCK     = new Object();
    private static final String MAINPAGE = "http://www.videobb.com/";
    private final String        ua       = RandomUserAgent.generate();

    public VideoBbCom(final PluginWrapper wrapper) {
        super(wrapper);
        this.enablePremium("http://www.videobb.com/premium.php");
        setStartIntervall(3000l);
    }

    public void correctDownloadLink(DownloadLink link) {
        link.setUrlDownload(link.getDownloadURL().replaceAll("/(video|e)/", "/watch_video.php?v="));
    }

    @Override
    public AccountInfo fetchAccountInfo(final Account account) throws Exception {
        final AccountInfo ai = new AccountInfo();
        setBrowserExclusive();
        try {
            login(account);
        } catch (final PluginException e) {
            account.setValid(false);
            return ai;
        }
        if (isPremium()) {
            final String expire = br.getRegex(">Premium<.*?until (.*?)</span").getMatch(0);
            if (expire != null) {
                ai.setValidUntil(TimeFormatter.getMilliSeconds(expire, "dd MMM yyyy", null) + 1000l * 60 * 60 * 24);
            } else {
                logger.warning("Couldn't get the expire date, stopping premium!");
                ai.setExpired(true);
                account.setValid(false);
                return ai;
            }
            ai.setUnlimitedTraffic();
            account.setValid(true);
        } else {
            account.setValid(false);
        }
        return ai;
    }

    @Override
    public String getAGBLink() {
        return "http://www.videobb.com/terms.php";
    }

    private String getFinalLink(final DownloadLink downloadLink, final String token) throws IOException {
        final String link = downloadLink.getDownloadURL();
        br.getPage(link);
        final String setting = Encoding.Base64Decode(br.getRegex("<param value=\"setting=(.*?)\"").getMatch(0));
        if (setting == null || !setting.contains("http://")) { return null; }
        br.getPage(setting);
        if (!br.containsHTML("token")) { return null; }
        String dllink = Encoding.Base64Decode(br.getRegex(token + "\":\"(.*?)\",").getMatch(0));
        if (dllink == null) { return null; }
        if (isPremium()) {
            dllink = dllink + "&d=" + link.replaceFirst(":", Encoding.urlEncode(":"));
        }
        return dllink;
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return -1;
    }

    @Override
    public void handleFree(final DownloadLink downloadLink) throws Exception {
        requestFileInformation(downloadLink);
        final String dllink = getFinalLink(downloadLink, "token1");
        if (dllink == null) { throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT); }
        dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, dllink, false, 1);
        if (!dl.getConnection().isContentDisposition()) {
            br.followConnection();
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        dl.setFilenameFix(true);
        dl.startDownload();
    }

    @Override
    public void handlePremium(final DownloadLink downloadLink, final Account account) throws Exception {
        requestFileInformation(downloadLink);
        login(account);
        final String dllink = getFinalLink(downloadLink, "token3");
        if (dllink == null) { throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT); }
        dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, dllink, true, 1);
        if (dl.getConnection().getContentType().contains("html")) {
            logger.warning("The final dllink seems not to be a file!");
            br.followConnection();
            if (br.containsHTML("(<title>404 Not Found</title>|<h1>404 Not Found</h1>)")) { throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server error", 10 * 60 * 1000l); }
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        dl.startDownload();
    }

    public boolean isPremium() throws IOException {
        br.getPage(VideoBbCom.MAINPAGE + "my_profile.php");
        final String type = br.getRegex("Account Type:<.*?(>Premium<)").getMatch(0);
        if (type != null) { return true; }
        return false;
    }

    public void login(final Account account) throws Exception {
        synchronized (VideoBbCom.LOCK) {
            setBrowserExclusive();
            br.forceDebug(true);
            prepareBrowser(br);
            br.setFollowRedirects(true);
            final String user = Encoding.urlEncode(account.getUser());
            final String pass = Encoding.urlEncode(account.getPass());
            br.getPage("http://www.videobb.com/index.php");
            br.postPage(VideoBbCom.MAINPAGE + "login.php", "login_username=" + user + "&login_password=" + pass);
            final String cookie = br.getCookie("http://www.videobb.com", "P_sk");
            if (cookie == null || "deleted".equalsIgnoreCase(cookie)) { throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_DISABLE); }
            if (br.containsHTML("The username or password you entered is incorrect")) {
                final String error = br.getRegex("msgicon_error\">(.*?)</div>").getMatch(0);
                logger.warning("Error: " + error);
                throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_DISABLE);
            }
        }
    }

    private void prepareBrowser(final Browser br) {
        try {
            if (br == null) { return; }
            this.br.getHeaders().put("Accept-Encoding", "");
            br.getHeaders().put("User-Agent", ua);
            br.getHeaders().put("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8");
            br.getHeaders().put("Accept-Language", "en-us,de;q=0.7,en;q=0.3");
            br.getHeaders().put("Pragma", null);
            br.getHeaders().put("Cache-Control", null);
        } catch (final Throwable e) {
            /* setCookie throws exception in 09580 */
        }
    }

    @Override
    public AvailableStatus requestFileInformation(final DownloadLink downloadLink) throws IOException, InterruptedException, PluginException {
        try {
            setBrowserExclusive();
            br.getPage(downloadLink.getDownloadURL());
        } catch (final Exception e) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        if (br.containsHTML("(>The page or video you are looking for cannot be found|>Video is not available<|<title>videobb \\- Free Video Hosting \\- Your #1 Video Site</title>)")) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        String filename = br.getRegex("Content\\-Disposition: attachment; filename\\*= UTF\\-8\\'\\'(.*?)(<|\")").getMatch(0);
        if (filename == null) filename = br.getRegex("content=\"videobb \\- (.*?)\"  name=\"title\"").getMatch(0);
        if (filename == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        downloadLink.setName(filename.trim());
        return AvailableStatus.TRUE;

    }

    @Override
    public void reset() {
    }

    @Override
    public void resetDownloadlink(final DownloadLink link) {

    }

    @Override
    public void resetPluginGlobals() {
    }
}
