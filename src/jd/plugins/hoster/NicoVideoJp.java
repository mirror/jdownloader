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

import jd.PluginWrapper;
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

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "nicovideo.jp" }, urls = { "http://(www\\.)?nicovideo\\.jp/watch/(sm|nm)\\d+" }, flags = { 2 })
public class NicoVideoJp extends PluginForHost {

    private static final String MAINPAGE               = "http://www.nicovideo.jp/";

    private static final String ONLYREGISTEREDUSERTEXT = "Only downloadable for registered users";

    public NicoVideoJp(PluginWrapper wrapper) {
        super(wrapper);
        this.enablePremium("https://secure.nicovideo.jp/secure/register");
    }

    @Override
    public AccountInfo fetchAccountInfo(Account account) throws Exception {
        AccountInfo ai = new AccountInfo();
        try {
            login(account);
        } catch (PluginException e) {
            account.setValid(false);
            return ai;
        }
        ai.setUnlimitedTraffic();
        ai.setStatus("Registered User");
        return ai;
    }

    @Override
    public String getAGBLink() {
        return "http://info.nicovideo.jp/base/rule.html";
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return -1;
    }

    @Override
    public int getMaxSimultanPremiumDownloadNum() {
        // More simultan downloads are possible but cause errors!
        return 4;
    }

    @Override
    public void handleFree(DownloadLink downloadLink) throws Exception, PluginException {
        requestFileInformation(downloadLink);
        throw new PluginException(LinkStatus.ERROR_FATAL, JDL.L("plugins.hoster.nicovideojp.only4registered", ONLYREGISTEREDUSERTEXT));
    }

    @Override
    public void handlePremium(DownloadLink link, Account account) throws Exception {
        requestFileInformation(link);
        // if
        // (link.getDownloadURL().matches("http://(www\\.)?nicovideo\\.jp/watch/nm\\d+"))
        // throw new PluginException(LinkStatus.ERROR_FATAL,
        // "This linktype isn't supported yet!");
        login(account);
        br.setFollowRedirects(false);
        // Important, without accessing the link we cannot get the downloadurl!
        br.getPage(link.getDownloadURL());
        if (Encoding.htmlDecode(br.toString()).contains("closed=1\\&done=true")) throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server error", 10 * 60 * 1000l);
        String vid = new Regex(link.getDownloadURL(), "nicovideo\\.jp/watch/((sm|nm)\\d+)").getMatch(0);
        br.postPage("http://flapi.nicovideo.jp/api/getflv", "eco=4&as3=1&v=" + vid);
        String dllink = new Regex(Encoding.htmlDecode(br.toString()), "\\&url=(http://.*?)\\&").getMatch(0);
        if (dllink == null) dllink = new Regex(Encoding.htmlDecode(br.toString()), "(http://smile-com\\d+\\.nicovideo\\.jp/smile\\?v=[0-9\\.]+)").getMatch(0);
        if (dllink == null) {
            logger.warning("Final downloadlink (String is \"dllink\") regex didn't match!");
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        dl = jd.plugins.BrowserAdapter.openDownload(br, link, dllink, true, 0);
        if (dl.getConnection().getContentType().contains("html")) {
            logger.warning("The final dllink seems not to be a file!");
            br.followConnection();
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        dl.startDownload();
    }

    private void login(Account account) throws Exception {
        this.setBrowserExclusive();
        br.setFollowRedirects(false);
        br.postPage("https://secure.nicovideo.jp/secure/login?site=niconico", "next_url=&mail=" + Encoding.urlEncode(account.getUser()) + "&password=" + Encoding.urlEncode(account.getPass()));
        if (br.getCookie(MAINPAGE, "user_session") == null) throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_DISABLE);
    }

    @Override
    public AvailableStatus requestFileInformation(DownloadLink link) throws IOException, PluginException {
        this.setBrowserExclusive();
        br.setCustomCharset("utf-8");
        br.setFollowRedirects(true);
        br.getPage(link.getDownloadURL());
        if (br.containsHTML("<title>ニコニコ動画　ログインフォーム</title>") || br.getURL().contains("/secure/") || br.getURL().contains("login_form?")) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        String filename = br.getRegex("<meta property=\"og:title\" content=\"([^<>\"]*?)\"").getMatch(0);
        if (filename == null) filename = br.getRegex("<h1 itemprop=\"name\">([^<>\"]*?)</h1>").getMatch(0);
        if (filename == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        link.setFinalFileName(filename.trim() + ".flv");
        link.getLinkStatus().setStatusText(JDL.L("plugins.hoster.nicovideojp.only4registered", ONLYREGISTEREDUSERTEXT));
        return AvailableStatus.TRUE;
    }

    @Override
    public void reset() {
    }

    @Override
    public void resetDownloadlink(DownloadLink link) {
    }

}