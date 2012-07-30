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
import jd.parser.html.Form;
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

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "4fastfile.com" }, urls = { "http://(www\\.)?4fastfile\\.com(/[a-z]+)?/abv\\-fs/\\d+.+" }, flags = { 2 })
public class FourFastFileCom extends PluginForHost {

    public FourFastFileCom(PluginWrapper wrapper) {
        super(wrapper);
        this.enablePremium("http://4fastfile.com/premium");
    }

    public void correctDownloadLink(DownloadLink link) {
        String languageText = new Regex(link.getDownloadURL(), "4fastfile\\.com/([a-z-]+/)").getMatch(0);
        if (!"abv-fs/".equals(languageText)) link.setUrlDownload(link.getDownloadURL().replace(languageText, ""));
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
        account.setValid(true);
        String expire = br.getRegex("<dd>Your Premium Membership will expire on (\\d+/\\d+/\\d+ \\- \\d+:\\d+)</dd>").getMatch(0);
        if (expire == null) {
            ai.setExpired(true);
            account.setValid(false);
            return ai;
        } else {
            ai.setValidUntil(TimeFormatter.getMilliSeconds(expire, "MM/dd/yyyy - HH:mm", null));
        }
        ai.setStatus("Premium User");
        return ai;
    }

    // Using same script as 1-upload.com and 1clickshare.net
    @Override
    public String getAGBLink() {
        return "http://4fastfile.com/terms";
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return 1;
    }

    @Override
    public int getMaxSimultanPremiumDownloadNum() {
        return -1;
    }

    @Override
    public void handleFree(DownloadLink downloadLink) throws Exception, PluginException {
        requestFileInformation(downloadLink);
        Form dlform = br.getFormbyProperty("id", "abv-fs-download-form");
        if (dlform == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        String waitfbid = dlform.getRegex("form_build_id\" id=\"(.*?)\"").getMatch(0);
        if (waitfbid == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        dlform.put("waitfbid", waitfbid);
        dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, dlform, false, 1);
        if (dl.getConnection().getContentType().contains("html")) {
            br.followConnection();
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        dl.startDownload();
    }

    @Override
    public void handlePremium(DownloadLink link, Account account) throws Exception {
        requestFileInformation(link);
        login(account);
        br.getPage(link.getDownloadURL());
        Form dlform = br.getFormbyProperty("id", "abv-fs-download-form");
        if (dlform == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        dl = jd.plugins.BrowserAdapter.openDownload(br, link, dlform, true, 0);
        if (dl.getConnection().getContentType().contains("html")) {
            br.followConnection();
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        dl.startDownload();

    }

    private void login(Account account) throws Exception {
        this.setBrowserExclusive();
        br.setDebug(true);
        br.setFollowRedirects(true);
        br.getPage("http://4fastfile.com/user/login");
        Form loginform = br.getFormbyProperty("id", "user-login");
        if (loginform == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        loginform.put("name", Encoding.urlEncode(account.getUser()));
        loginform.put("pass", Encoding.urlEncode(account.getPass()));
        br.submitForm(loginform);
        if (!br.containsHTML("<dd>Your Premium Membership will expire on")) throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_DISABLE);
    }

    @Override
    public AvailableStatus requestFileInformation(DownloadLink link) throws IOException, PluginException {
        this.setBrowserExclusive();
        br.getPage(link.getDownloadURL());
        if (br.containsHTML("This file is either removed due to copyright claim or deleted by his owner\\.")) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        if (br.containsHTML("(>4FastFile\\.com \\- Access Denied|You may need to login below or register to access this page\\.<)")) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        String filename = br.getRegex("<title>4FastFile\\.com \\- Download (.*?)</title>").getMatch(0);
        if (filename == null) filename = br.getRegex("<td class=\"file\\-name\">(.*?)</td>").getMatch(0);
        String filesize = br.getRegex("<td class=\"file-size\">(.*?)</td>").getMatch(0);
        if (filename == null || filesize == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        link.setName(filename.trim());
        link.setDownloadSize(SizeFormatter.getSize(filesize));
        return AvailableStatus.TRUE;
    }

    @Override
    public void reset() {
    }

    @Override
    public void resetDownloadlink(DownloadLink link) {
    }

}