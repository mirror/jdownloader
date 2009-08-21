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

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "storage.to" }, urls = { "http://[\\w\\.]*?storage.to/get/[a-zA-Z0-9]+/.+" }, flags = { 2 })
public class StorageTo extends PluginForHost {

    public StorageTo(PluginWrapper wrapper) {
        super(wrapper);
        this.enablePremium("http://www.storage.to/affiliate/Slh9BLxH");
    }

    @Override
    public String getAGBLink() {
        return "http://www.storage.to/tos";
    }

    public void login(Account account) throws IOException, PluginException {
        this.setBrowserExclusive();
        br.getPage("http://www.storage.to/login?language=en");
        br.postPage("http://storage.to/login", "email=" + Encoding.urlEncode(account.getUser()) + "&password=" + Encoding.urlEncode(account.getPass()) + "&=Login");
        br.getPage("http://www.storage.to/account");
        if (!br.containsHTML("Status:</span> premium")) throw new PluginException(LinkStatus.ERROR_PREMIUM, LinkStatus.VALUE_ID_PREMIUM_DISABLE);
    }

    // @Override
    public AccountInfo fetchAccountInfo(Account account) throws Exception {
        AccountInfo ai = new AccountInfo(this, account);
        this.setBrowserExclusive();
        try {
            login(account);
        } catch (PluginException e) {
            account.setValid(false);
            return ai;
        }
        String validUntil = br.getRegex("Your premium account expires in.*?</div>.*?premium_dark.>(.*?)days</span>").getMatch(0);
        if (validUntil != null) {
            long expire = System.currentTimeMillis() + (long) (Float.parseFloat(validUntil.trim()) * 1000l * 60 * 60 * 24);
            account.setValid(true);
            ai.setValidUntil(expire);
        }
        return ai;
    }

    public void handlePremium(DownloadLink link, Account account) throws Exception {
        requestFileInformation(link);
        login(account);
        String infolink = link.getDownloadURL().replaceFirst("storage.to/get", "storage.to/getlink");
        br.getPage(infolink);
        if (br.getRegex("'state' : '(.*?)'").getMatch(0).equals("failed")) { throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE); }
        String dllink;
        dllink = br.getRegex("'link' : '(.*?)',").getMatch(0);
        if (dllink == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFEKT);
        link.setFinalFileName(null);
        br.setDebug(true);
        dl = jd.plugins.BrowserAdapter.openDownload(br, link, dllink, false, 1);
        if (!dl.getConnection().isContentDisposition()) {
            br.followConnection();
            if (br.containsHTML("File not found")) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            throw new PluginException(LinkStatus.ERROR_FATAL);
        }
        dl.startDownload();
    }

    @Override
    public void handleFree(DownloadLink link) throws Exception {
        requestFileInformation(link);
        br.setFollowRedirects(false);
        String infolink = link.getDownloadURL().replaceFirst("storage.to/get", "storage.to/getlink");
        br.getPage(infolink);
        if (br.getRegex("'state' : '(.*?)'").getMatch(0).equals("wait")) {
            int wait = Integer.valueOf(br.getRegex("'countdown' : (.*?),").getMatch(0)).intValue();
            throw new PluginException(LinkStatus.ERROR_IP_BLOCKED, 1000 * wait);
        }
        if (br.getRegex("'state' : '(.*?)'").getMatch(0).equals("failed")) { throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, 10 * 60 * 1000l); }
        String time = br.getRegex("'countdown' : (.*?),").getMatch(0);
        long sleeptime = 0;
        try {
            sleeptime = Long.parseLong(time);
        } catch (Exception e) {
        }
        if (sleeptime > 0) sleep((sleeptime + 1) * 1000, link);
        String dllink;
        dllink = br.getRegex("'link' : '(.*?)',").getMatch(0);
        if (dllink == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFEKT);
        link.setFinalFileName(null);
        dl = jd.plugins.BrowserAdapter.openDownload(br, link, dllink, false, 1);
        if (!dl.getConnection().isContentDisposition()) {
            br.followConnection();
            if (br.containsHTML("File not found")) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            throw new PluginException(LinkStatus.ERROR_FATAL);
        }
        dl.startDownload();
    }

    @Override
    public AvailableStatus requestFileInformation(DownloadLink parameter) throws Exception {
        this.setBrowserExclusive();
        br.getPage("http://www.storage.to/login?language=en");
        br.getPage(parameter.getDownloadURL());
        if (br.containsHTML("File not found.")) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        String filename = br.getRegex("<span class=\"orange\">Downloading:</span>(.*?)<span class=\"light\">(.*?)</span>").getMatch(0);
        String filesize = br.getRegex("<span class=\"orange\">Downloading:</span>(.*?)<span class=\"light\">(.*?)</span>").getMatch(1);
        if (filename == null || filesize == null) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        parameter.setName(filename.trim());
        parameter.setDownloadSize(Regex.getSize(filesize.replaceAll(",", "\\.")));
        return AvailableStatus.TRUE;
    }

    @Override
    public void reset() {
    }

    @Override
    public void resetDownloadlink(DownloadLink link) {
    }

    public int getMaxSimultanFreeDownloadNum() {
        return 1;
    }

}
