//    jDownloader - Downloadmanager
//    Copyright (C) 2009  JD-Team support@jdownloader.org
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
import jd.config.ConfigContainer;
import jd.config.ConfigEntry;
import jd.http.Browser;
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
import jd.utils.locale.JDL;

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "tube8.com" }, urls = { "http://(www\\.)?tube8\\.com/(?!(cat|latest)/).*?/.*?/[0-9]+" }, flags = { 2 })
public class Tube8Com extends PluginForHost {

    private boolean             setEx  = true;
    private static final String mobile = "mobile";
    public String               dllink = null;

    public Tube8Com(PluginWrapper wrapper) {
        super(wrapper);
        setConfigElements();
        this.enablePremium("http://www.tube8.com/signin.html");
    }

    @Override
    public AvailableStatus requestFileInformation(DownloadLink downloadLink) throws IOException, PluginException {
        if (setEx) this.setBrowserExclusive();
        br.setFollowRedirects(false);
        br.getPage(downloadLink.getDownloadURL());
        if (br.getRedirectLocation() != null || br.containsHTML("No htmlCode read")) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        String verifyAge = br.getRegex("(<div class=\"enter\\-btn\">)").getMatch(0);
        if (verifyAge != null) {
            br.postPage(downloadLink.getDownloadURL(), "processdisclaimer=");
        }
        String filename = br.getRegex("<title>(.*?) \\- ").getMatch(0);
        if (filename == null) {
            filename = br.getRegex("var videotitle=\"(.*?)\"").getMatch(0);
            if (filename == null) {
                filename = br.getRegex("Tube8 bring you(.*?)for all").getMatch(0);
            }
        }
        Browser br2 = br.cloneBrowser();
        br2.getHeaders().put("X-Requested-With", "XMLHttpRequest");
        final String hash = br.getRegex("var hash = \"([a-z0-9]+)\"").getMatch(0);
        if (hash == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        br2.getPage("http://www.tube8.com/ajax/getVideoDownloadURL.php?_=" + System.currentTimeMillis() + "&hash=" + hash + "&video=" + new Regex(downloadLink.getDownloadURL(), ".*?(\\d+)$").getMatch(0));
        final String correctedBR = br2.toString().replace("\\", "");
        boolean prefer3gp = getPluginConfig().getBooleanProperty(mobile, false);
        if (prefer3gp) {
            find3gpLinks(correctedBR);
            if (dllink == null) {
                logger.warning("Couldn't find 3gp links even if they're prefered, trying to get the normal links now!");
                findNormalLinks(correctedBR);
            }
        } else {
            findNormalLinks(correctedBR);
            if (dllink == null) {
                logger.warning("Couldn't find normal links even if they're prefered, trying to get the 3gp links now!");
                find3gpLinks(correctedBR);
            }
        }
        if (filename == null || dllink == null) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        dllink = Encoding.htmlDecode(dllink);
        filename = filename.trim();
        if (dllink.contains(".3gp")) {
            downloadLink.setFinalFileName((filename + ".3gp"));
        } else {
            downloadLink.setFinalFileName(filename + ".flv");
        }
        URLConnectionAdapter con = null;
        try {
            con = br.openGetConnection(Encoding.htmlDecode(dllink));
            if (!con.getContentType().contains("html"))
                downloadLink.setDownloadSize(con.getLongContentLength());
            else
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            return AvailableStatus.TRUE;
        } finally {
            try {
                con.disconnect();
            } catch (final Throwable e) {
            }
        }
    }

    @Override
    public AccountInfo fetchAccountInfo(final Account account) throws Exception {
        final AccountInfo ai = new AccountInfo();
        try {
            this.login(account, this.br);
        } catch (final PluginException e) {
            account.setValid(false);
            return ai;
        }
        /* only support for free accounts at the moment */
        ai.setUnlimitedTraffic();
        ai.setStatus("Account ok");
        account.setValid(true);
        return ai;
    }

    public void find3gpLinks(final String correctedBR) throws NumberFormatException, PluginException {
        dllink = new Regex(correctedBR, "\"mobile_url\":\"(http:.*?)\"").getMatch(0);
        if (dllink == null) dllink = new Regex(correctedBR, "\"(http://cdn\\d+\\.mobile\\.tube8\\.com/.*?)\"").getMatch(0);
    }

    public void findNormalLinks(final String correctedBR) throws NumberFormatException, PluginException, IOException {
        dllink = new Regex(correctedBR, "\"standard_url\":\"(http.*?)\"").getMatch(0);
        if (dllink == null) dllink = new Regex(correctedBR, "\"(http://cdn\\d+\\.public\\.tube8\\.com/.*?)\"").getMatch(0);
    }

    @Override
    public String getAGBLink() {
        return "http://www.tube8.com/info.html#terms";
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return -1;
    }

    @Override
    public void handleFree(DownloadLink downloadLink) throws Exception {
        requestFileInformation(downloadLink);
        dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, dllink, true, 0);
        if (dl.getConnection().getContentType().contains("html")) {
            br.followConnection();
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        dl.startDownload();
    }

    @Override
    public void handlePremium(final DownloadLink downloadLink, final Account account) throws Exception {
        login(account, br);
        setEx = false;
        requestFileInformation(downloadLink);
        dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, dllink, true, 0);
        if (dl.getConnection().getContentType().contains("html")) {
            br.followConnection();
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        dl.startDownload();
    }

    private void login(Account account, Browser br) throws IOException, PluginException {
        if (br == null) {
            br = new Browser();
        }
        this.setBrowserExclusive();
        boolean follow = br.isFollowingRedirects();
        try {
            br.setFollowRedirects(true);
            br.postPage("http://www.tube8.com/ajax/login.php", "username=" + Encoding.urlEncode(account.getUser()) + "&password=" + Encoding.urlEncode(account.getPass()));
            if (br.containsHTML("invalid") || br.containsHTML("0\\|")) throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_DISABLE);
        } finally {
            br.setFollowRedirects(follow);
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

    private void setConfigElements() {
        ConfigEntry cond = new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, getPluginConfig(), mobile, JDL.L("plugins.hoster.Tube8Com.setting.preferVideosForMobilePhones", "Prefer videos for mobile phones (3gp format)")).setDefaultValue(false);
        getConfig().addEntry(cond);
    }
}