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
import jd.nutils.encoding.Encoding;
import jd.plugins.Account;
import jd.plugins.AccountInfo;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;
import jd.utils.locale.JDL;

import org.appwork.utils.formatter.TimeFormatter;

@HostPlugin(revision = "$Revision: 12761 $", interfaceVersion = 2, names = { "videobb.com" }, urls = { "http://(www\\.)?videobb\\.com/video/\\w+" }, flags = { 2 })
public class VideoBbCom extends PluginForHost {

    private static final Object LOCK     = new Object();
    private static final String MAINPAGE = "http://www.videobb.com/";

    public VideoBbCom(final PluginWrapper wrapper) {
        super(wrapper);
        this.enablePremium("http://www.videobb.com/premium.php");
        this.setStartIntervall(3000l);
    }

    @Override
    public AccountInfo fetchAccountInfo(final Account account) throws Exception {
        final AccountInfo ai = new AccountInfo();
        this.setBrowserExclusive();
        try {
            this.login(account);
        } catch (final PluginException e) {
            account.setValid(false);
            return ai;
        }
        if (!this.isPremium()) {
            final String balance = this.br.getMatch("content_detail profile.*?\\$([\\d\\.]+)\r\n");
            ai.setAccountBalance((long) (Double.parseDouble(balance) * 100));
            ai.setValidUntil(System.currentTimeMillis() + (356 * 24 * 60 * 60 * 1000l));
            ai.setStatus(JDL.L("plugins.hoster.videobbcom.accounttype", "Accounttype: Collectors Account"));
        } else {
            final String expire = this.br.getRegex("Premium active until.*?<strong>(.*?)</strong>").getMatch(0);
            if (expire != null) {
                ai.setValidUntil(TimeFormatter.getMilliSeconds(expire, "dd.MM.yyyy", null) + (1000l * 60 * 60 * 24));
            } else {
                this.logger.warning("Couldn't get the expire date, stopping premium!");
                ai.setExpired(true);
                account.setValid(false);
                return ai;
            }
            final String status = this.br.getRegex("Premium status.*?<strong>(.*?)</strong>").getMatch(0);
            if (status != null) {
                ai.setStatus("Premium Status: " + status);
            }
            final String fileUpload = this.br.getRegex("Files overall.*?<strong>(\\d+)</strong>").getMatch(0);
            if (fileUpload != null) {
                ai.setFilesNum(Integer.parseInt(fileUpload));
            }
            account.setValid(true);
        }
        return ai;
    }

    @Override
    public String getAGBLink() {
        return "http://www.videobb.com/terms.php";
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return -1;
    }

    @Override
    public void handleFree(final DownloadLink downloadLink) throws Exception {
        this.setBrowserExclusive();
        this.br.getPage(downloadLink.getDownloadURL());
        final String setting = Encoding.Base64Decode(this.br.getRegex("<param value=\"setting=(.*?)\"").getMatch(0));
        if ((setting == null) || !setting.contains("http://")) { throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT); }
        this.br.getPage(setting);
        if (!this.br.containsHTML("token")) { throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT); }
        final String dllink = Encoding.Base64Decode(this.br.getRegex("token1\":\"(.*?)\",").getMatch(0));
        this.dl = jd.plugins.BrowserAdapter.openDownload(this.br, downloadLink, dllink, false, 1);
        if (!(this.dl.getConnection().isContentDisposition())) {
            this.br.followConnection();
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        this.dl.setFilenameFix(true);
        this.dl.startDownload();
    }

    @Override
    public void handlePremium(final DownloadLink downloadLink, final Account account) throws Exception {
        this.requestFileInformation(downloadLink);
        this.login(account);
        this.br.forceDebug(true);
        final String link = downloadLink.getDownloadURL();
        if (link == null) { throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT); }
        this.br.setFollowRedirects(true);
        this.dl = jd.plugins.BrowserAdapter.openDownload(this.br, downloadLink, link, true, 1);
        if (this.dl.getConnection().getContentType().contains("html")) {
            this.logger.warning("The final dllink seems not to be a file!");
            this.br.followConnection();
            if (this.br.containsHTML("(<title>404 Not Found</title>|<h1>404 Not Found</h1>)")) { throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server error", 10 * 60 * 1000l); }
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        this.dl.startDownload();
    }

    public boolean isPremium() throws IOException {
        this.br.getPage(VideoBbCom.MAINPAGE + "my_profile.php");
        if (this.br.getRegex("Account Type:</span> <span><b>Premium</b>").matches()) { return true; }
        return false;
    }

    public void login(final Account account) throws Exception {
        synchronized (VideoBbCom.LOCK) {
            this.setBrowserExclusive();
            this.br.forceDebug(true);
            this.br.setFollowRedirects(true);
            this.br.getHeaders().put("Referer", VideoBbCom.MAINPAGE + "index.php");
            final String user = Encoding.urlEncode(account.getUser());
            final String pass = Encoding.urlEncode(account.getPass());
            this.br.postPage(VideoBbCom.MAINPAGE + "login.php", "login_username=" + user + "&login_password=" + pass);
            if (this.br.containsHTML("msgicon_error")) {
                final String error = this.br.getRegex("msgicon_error\">(.*?)</div>").getMatch(0);
                this.logger.warning("Error: " + error);
                throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_DISABLE);
            }
            // if (!this.isPremium()) {
            // this.logger.warning("This account is no a premium account!");
            // throw new PluginException(LinkStatus.ERROR_PREMIUM,
            // PluginException.VALUE_ID_PREMIUM_DISABLE);
            // }
        }
    }

    @Override
    public AvailableStatus requestFileInformation(final DownloadLink downloadLink) throws IOException, InterruptedException, PluginException {
        try {
            this.br.getPage(downloadLink.getDownloadURL());
            if (this.br.containsHTML(">Video is not available</font>")) { throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND); }
            final String filename = this.br.getRegex("content=\"videobb - (.*?)\"  name=\"title\"").getMatch(0);
            if (filename == null) { throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND); }
            downloadLink.setName(filename.trim());
            return AvailableStatus.TRUE;
        } catch (final Exception e) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
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
