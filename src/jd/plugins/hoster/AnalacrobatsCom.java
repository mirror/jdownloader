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

import org.jdownloader.plugins.components.antiDDoSForHost;

import jd.PluginWrapper;
import jd.controlling.AccountController;
import jd.http.Browser.BrowserException;
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
import jd.utils.JDUtilities;

@HostPlugin(revision = "$Revision$", interfaceVersion = 3, names = { "analacrobats.com" }, urls = { "https?://members\\.analacrobats\\.com/(?:en/)?[^/]+/scene/\\d+" })
public class AnalacrobatsCom extends antiDDoSForHost {

    public AnalacrobatsCom(PluginWrapper wrapper) {
        super(wrapper);
        this.enablePremium("http://members.analacrobats.com/");
    }

    @Override
    public String getAGBLink() {
        return "http://www.analacrobats.com/en";
    }

    private static final String  type_normal                  = "https?://members\\.analacrobats\\.com/(?:en/)?([^/]+)/scene/(\\d+)";

    /* Connection stuff */
    private static final boolean FREE_RESUME                  = false;
    private static final int     FREE_MAXCHUNKS               = 1;
    private static final int     FREE_MAXDOWNLOADS            = 0;
    private static final boolean ACCOUNT_PREMIUM_RESUME       = true;
    private static final int     ACCOUNT_PREMIUM_MAXCHUNKS    = 0;
    private static final int     ACCOUNT_PREMIUM_MAXDOWNLOADS = 20;

    private String               dllink                       = null;
    public static final long     trust_cookie_age             = 300000l;
    private static final String  HTML_LOGGEDIN                = "\"headerToolbarlinkLogout\"";
    public static final String   LOGIN_PAGE                   = "http://members.analacrobats.com/en";
    private static final String  HTML_ACCOUNTNEEDED           = "class=\"alertTitle\"";

    @SuppressWarnings("deprecation")
    @Override
    public AvailableStatus requestFileInformation(final DownloadLink link) throws Exception {
        dllink = null;
        this.setBrowserExclusive();
        final Account aa = AccountController.getInstance().getValidAccount(this);
        if (aa == null) {
            /* Account needed! */
            return AvailableStatus.UNCHECKABLE;
        }
        this.login(aa);
        this.br.setFollowRedirects(true);
        getPage(link.getDownloadURL());
        if (this.br.getHttpConnection().getResponseCode() == 404) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        final String filename_url = new Regex(link.getDownloadURL(), type_normal).getMatch(0) + "_" + new Regex(link.getDownloadURL(), type_normal).getMatch(1);
        if (this.br.containsHTML("class=\"alertTitle\"")) {
            link.setName(filename_url + ".mp4");
            return AvailableStatus.TRUE;
        }

        String filename = br.getRegex("<h3 class=\"sceneTitle\">([^<>\"]*?)</h3>").getMatch(0);
        if (filename == null) {
            filename = new Regex(link.getDownloadURL(), type_normal).getMatch(0) + "_" + new Regex(link.getDownloadURL(), type_normal).getMatch(1);
        }
        filename = Encoding.htmlDecode(filename).trim();

        dllink = jd.plugins.hoster.EvilAngelCom.getDllink(this.br);
        if (dllink == null) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        dllink = "http://members.analacrobats.com" + dllink;
        final String quality = new Regex(dllink, "(\\d+p)").getMatch(0);
        if (quality == null) {
            filename += ".mp4";
        } else {
            filename = filename + "-" + quality + ".mp4";
        }

        link.setFinalFileName(filename);

        URLConnectionAdapter con = null;
        try {
            try {
                con = br.openHeadConnection(dllink);
            } catch (final BrowserException e) {
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            }
            if (!con.getContentType().contains("html")) {
                link.setDownloadSize(con.getLongContentLength());
            } else {
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            }
            link.setProperty("free_directlink", dllink);
        } finally {
            try {
                con.disconnect();
            } catch (final Throwable e) {
            }
        }

        return AvailableStatus.TRUE;
    }

    @Override
    public void handleFree(final DownloadLink downloadLink) throws Exception, PluginException {
        requestFileInformation(downloadLink);
        doFree(downloadLink, FREE_RESUME, FREE_MAXCHUNKS, "free_directlink");
    }

    private void doFree(final DownloadLink downloadLink, final boolean resumable, final int maxchunks, final String directlinkproperty) throws Exception, PluginException {
        /* Premiumonly */
        throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_ONLY);
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return FREE_MAXDOWNLOADS;
    }

    private static Object LOCK = new Object();

    private void login(final Account account) throws Exception {
        synchronized (LOCK) {
            final PluginForHost hostplugin = JDUtilities.getPluginForHost("evilangel.com");
            ((jd.plugins.hoster.EvilAngelCom) hostplugin).loginEvilAngelNetwork(this.br, account, LOGIN_PAGE, HTML_LOGGEDIN);
        }
    }

    @SuppressWarnings("deprecation")
    @Override
    public AccountInfo fetchAccountInfo(final Account account) throws Exception {
        final AccountInfo ai = new AccountInfo();
        try {
            login(account);
        } catch (PluginException e) {
            account.setValid(false);
            throw e;
        }
        ai.setUnlimitedTraffic();
        account.setType(AccountType.PREMIUM);
        account.setMaxSimultanDownloads(ACCOUNT_PREMIUM_MAXDOWNLOADS);
        account.setConcurrentUsePossible(true);
        ai.setStatus("Premium Account");
        account.setValid(true);
        return ai;
    }

    @Override
    public void handlePremium(final DownloadLink link, final Account account) throws Exception {
        requestFileInformation(link);
        if (this.br.containsHTML(HTML_ACCOUNTNEEDED)) {
            throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_ONLY);
        }
        dl = jd.plugins.BrowserAdapter.openDownload(br, link, dllink, ACCOUNT_PREMIUM_RESUME, ACCOUNT_PREMIUM_MAXCHUNKS);
        if (dl.getConnection().getContentType().contains("html")) {
            if (dl.getConnection().getResponseCode() == 403) {
                throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server error 403", 60 * 60 * 1000l);
            } else if (dl.getConnection().getResponseCode() == 404) {
                throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server error 404", 60 * 60 * 1000l);
            }
            logger.warning("The final dllink seems not to be a file!");
            br.followConnection();
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        link.setProperty("premium_directlink", dllink);
        dl.startDownload();
    }

    @Override
    public void reset() {
    }

    @Override
    public void resetDownloadlink(DownloadLink link) {
    }

}