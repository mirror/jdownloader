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
import java.net.SocketTimeoutException;

import jd.PluginWrapper;
import jd.http.Browser;
import jd.http.Browser.BrowserException;
import jd.http.URLConnectionAdapter;
import jd.parser.Regex;
import jd.parser.html.Form;
import jd.plugins.Account;
import jd.plugins.AccountInfo;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;
import jd.utils.locale.JDL;

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "unlimit.co.il" }, urls = { "http://[\\w\\.]*?unlimit\\.co\\.il/getfile\\.php\\?name=\\d+-\\d+-.+" })
public class UnLimitCoIl extends PluginForHost {

    private static final String ONLY4PREMIUMUSERTEXT = "Download is only available for premium users";

    public UnLimitCoIl(PluginWrapper wrapper) {
        super(wrapper);
        this.setAccountwithoutUsername(true);
        this.enablePremium();
    }

    @Override
    public void correctDownloadLink(final DownloadLink link) {
        String url = link.getDownloadURL();
        // without www the urls will return 404 in a redirect issue.
        url = url.replaceFirst("http://.*?/", "http://www.unlimit.co.il/");
        link.setUrlDownload(url);
    }

    @Override
    public AccountInfo fetchAccountInfo(Account account) throws Exception {
        AccountInfo ai = new AccountInfo();
        account.setValid(true);
        ai.setStatus("Status can only be checked while downloading!");
        return ai;
    }

    @Override
    public String getAGBLink() {
        return "http://www.unlimit.co.il/takanon.php";
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return -1;
    }

    @Override
    public int getMaxSimultanPremiumDownloadNum() {
        return -1;
    }

    @Override
    public void handleFree(DownloadLink downloadLink) throws Exception, PluginException {
        requestFileInformation(downloadLink);
        throw new PluginException(LinkStatus.ERROR_FATAL, JDL.L("plugins.hoster.unlimitcoil.only4premium", ONLY4PREMIUMUSERTEXT));
    }

    @Override
    public void handlePremium(DownloadLink link, Account account) throws Exception {
        requestFileInformation(link);
        Form loginForm = br.getFormbyProperty("name", "frmLogin");
        if (loginForm == null) {
            loginForm = br.getFormbyProperty("id", "frmLogin");
        }
        if (loginForm == null) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        loginForm.put("phoneNumber", account.getPass());
        // chunks are broken at the moment, response contains invalid
        // content-range
        dl = jd.plugins.BrowserAdapter.openDownload(br, link, loginForm, true, 1);
        if (dl.getConnection().getContentType().contains("html")) {
            logger.warning("The final dllink seems not to be a file!");
            br.followConnection();
            if (br.containsHTML(">שימו לב כי זהו האתר היחידי שלא מגביל תעבורה<") || br.containsHTML(">לאחר קבלת התשובה, עליך להזין את מספר הפלאפון שלך בתיבה למטה. כמו כן, מספר זה אישי ואינו ניתן להעברה")) {
                logger.info("Account probably invalid, disabling it...");
                throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_DISABLE);
            }
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        dl.startDownload();
    }

    @Override
    public AvailableStatus requestFileInformation(DownloadLink link) throws IOException, PluginException {
        br = new Browser();
        correctDownloadLink(link);
        this.setBrowserExclusive();
        // We have to set this charset, utf-8 doesn't work here!
        br.setCustomCharset("windows-1255");
        br.setFollowRedirects(true);
        URLConnectionAdapter con = null;
        boolean offline = false;
        String filename = new Regex(link.getDownloadURL(), "unlimit\\.co\\.il/getfile\\.php\\?name=\\d+-\\d+-(.+)").getMatch(0);
        if (filename == null) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        link.setName(filename.trim().replaceAll("\\&hdd=\\d+", ""));
        try {
            con = br.openGetConnection(link.getDownloadURL());
            if (con.getResponseCode() == 404) {
                offline = true;
            } else {
                br.followConnection();
            }
        } catch (final BrowserException be) {
            if (be.getCause() != null && be.getCause() instanceof SocketTimeoutException) {
                offline = true;
            }
        } finally {
            try {
                con.disconnect();
            } catch (Throwable e) {
            }
        }
        if (offline || br.containsHTML("(>404 Not Found<|<H1>Not Found</H1>|was not found on this server\\.)")) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        link.getLinkStatus().setStatusText(JDL.L("plugins.hoster.unlimitcoil.only4premium", ONLY4PREMIUMUSERTEXT));
        return AvailableStatus.TRUE;
    }

    @Override
    public void reset() {
    }

    @Override
    public void resetDownloadlink(DownloadLink link) {
    }

}