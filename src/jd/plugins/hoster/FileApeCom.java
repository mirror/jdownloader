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
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.plugins.Account;
import jd.plugins.AccountInfo;
import jd.plugins.DownloadLink;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;
import jd.plugins.DownloadLink.AvailableStatus;

import org.appwork.utils.formatter.SizeFormatter;

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "fileape.com" }, urls = { "http://(www\\.)?fileape\\.com/(index\\.php\\?act=download\\&id=|dl/)\\w+" }, flags = { 2 })
public class FileApeCom extends PluginForHost {

    public FileApeCom(PluginWrapper wrapper) {
        super(wrapper);
        this.enablePremium("http://fileape.com/?act=purchase");
    }

    @Override
    public String getAGBLink() {
        return "http://fileape.com/?act=tos";
    }

    private static final String MAINPAGE = "http://fileape.com/";

    public void correctDownloadLink(DownloadLink link) {
        if (link.getDownloadURL().contains("fileape.com/dl/")) link.setUrlDownload("http://fileape.com/index.php?act=download&id=" + new Regex(link.getDownloadURL(), "fileape\\.com/dl/(.+)").getMatch(0));
    }

    @Override
    public AvailableStatus requestFileInformation(DownloadLink downloadLink) throws IOException, PluginException {
        this.setBrowserExclusive();
        br.setFollowRedirects(true);
        br.getPage(downloadLink.getDownloadURL());
        if (br.containsHTML("This file is either temporarily unavailable or does not exist\\.")) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        downloadLink.setName(new Regex(downloadLink.getDownloadURL(), "download\\&id=(.+)").getMatch(0));
        return AvailableStatus.TRUE;
    }

    @Override
    public void handleFree(DownloadLink downloadLink) throws Exception {
        requestFileInformation(downloadLink);
        if (br.containsHTML("Free users can only download <em>")) throw new PluginException(LinkStatus.ERROR_IP_BLOCKED, 15 * 60 * 1000l);
        br.getPage(downloadLink.getDownloadURL() + "&g=1");
        String continuePage = br.getRegex("window\\.location = \\'(http://.*?)\\'").getMatch(0);
        if (continuePage == null) continuePage = br.getRegex("\\'(http://fileape\\.com/\\?act=download\\&t=[A-Za-z0-9_-]+)\\'").getMatch(0);
        if (continuePage == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        int wait = 60;
        String waitRegexed = br.getRegex("wait = (\\d+);").getMatch(0);
        waitRegexed = null;
        if (waitRegexed == null) waitRegexed = br.getRegex("id=\"waitnumber\" style=\"font-size:2em; text-align:center; width:33px; height:33px;\">(\\d+)</span>").getMatch(0);
        if (waitRegexed != null) {
            logger.info("Waittime found, waiting " + waitRegexed + " seconds...");
            wait = Integer.parseInt(waitRegexed);
        }
        sleep((wait + 3) * 1000l, downloadLink);
        br.getPage(continuePage);
        String dllink = br.getRegex("<div style=\"text-align:center; font-size: 30px;\"><a href=\"(http://.*?)\"").getMatch(0);
        if (dllink == null) dllink = br.getRegex("\"(http://tx\\d+\\.fileape\\.com/[a-z]+/.*?)\"").getMatch(0);
        if (dllink == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, dllink, true, 0);
        if (dl.getConnection().getContentType().contains("html")) {
            br.followConnection();
            if (br.containsHTML("Download ticket expired") || br.getURL().contains("ct=download&expired=true")) {
                logger.info("Ticket expired, retrying...");
                throw new PluginException(LinkStatus.ERROR_RETRY, "Ticket expired");
            }
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        dl.startDownload();
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return -1;
    }

    private void login(Account account) throws Exception {
        this.setBrowserExclusive();
        // br.getPage("http://fileape.com/?act=premium");
        br.postPage("http://fileape.com/?act=login&redir=premium", "username=" + Encoding.urlEncode(account.getUser()) + "&password=" + Encoding.urlEncode(account.getPass()));
        if (br.getCookie(MAINPAGE, "usr_id") == null || br.getCookie(MAINPAGE, "usr_session_ident") == null) throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_DISABLE);
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
        br.getPage("http://fileape.com/?act=premium");
        String availabletraffic = br.getRegex("<div class=\"aform left\">[\t\n\r ]+<div class=\"ahead\"  style=\"font\\-weight: normal\">(.*?)</div>").getMatch(0);
        if (availabletraffic != null) {
            ai.setTrafficLeft(SizeFormatter.getSize(availabletraffic));
        } else {
            ai.setUnlimitedTraffic();
        }
        String expire = br.getRegex("<div class=\"aform right\">[\t\n\r ]+<div class=\"ahead\" style=\"font\\-weight: normal\">(\\d+)\\.\\d+ days</div>").getMatch(0);
        if (expire == null) {
            ai.setExpired(true);
            account.setValid(false);
            return ai;
        } else {
            double ohCrap = Double.parseDouble(expire);
            ohCrap = ohCrap * 24 * 60 * 60 * 1000;
            ai.setValidUntil(System.currentTimeMillis() + (long) ohCrap);
        }
        account.setValid(true);
        ai.setStatus("Premium User");
        return ai;
    }

    @Override
    public void handlePremium(DownloadLink link, Account account) throws Exception {
        requestFileInformation(link);
        login(account);
        br.setFollowRedirects(true);
        dl = jd.plugins.BrowserAdapter.openDownload(br, link, link.getDownloadURL(), true, 0);
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

    @Override
    public void reset() {
    }

    @Override
    public void resetPluginGlobals() {
    }

    @Override
    public void resetDownloadlink(DownloadLink link) {
    }
}
