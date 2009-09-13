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

import jd.PluginWrapper;
import jd.http.Browser;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.parser.html.Form;
import jd.plugins.Account;
import jd.plugins.AccountInfo;
import jd.plugins.DownloadLink;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;
import jd.plugins.DownloadLink.AvailableStatus;

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "ugotfile.com" }, urls = { "http://[\\w\\.]*?ugotfile.com/file/\\d+/.+" }, flags = { 2 })
public class UgotFileCom extends PluginForHost {

    public UgotFileCom(PluginWrapper wrapper) {
        super(wrapper);
        this.enablePremium("http://ugotfile.com/user/register");
    }

    public String getAGBLink() {
        return "http://ugotfile.com/doc/terms/";
    }

    public void login(Account account) throws Exception {
        this.setBrowserExclusive();
        br.setCookie("http://my-share.at/", "lang", "english");
        br.getPage("http://ugotfile.com/user/login/");
        Form form = br.getForm(0);
        form.put(form.getBestVariable("userName"), Encoding.urlEncode(account.getUser()));
        form.put(form.getBestVariable("password"), Encoding.urlEncode(account.getPass()));
        br.submitForm(form);
        br.getPage("http://ugotfile.com/my/profile/");
        if (!br.containsHTML("Your premium membership is expired")) throw new PluginException(LinkStatus.ERROR_PREMIUM, LinkStatus.VALUE_ID_PREMIUM_DISABLE);
    }

    public AccountInfo fetchAccountInfo(Account account) throws Exception {
        AccountInfo ai = new AccountInfo();
        this.setBrowserExclusive();
        try {
            login(account);
        } catch (PluginException e) {
            account.setValid(false);
            return ai;
        }

        String validUntil = br.getRegex("<h3>Your premium membership is expired on (.*?).</h3>").getMatch(0);
        if (validUntil == null) {
            account.setValid(false);
        } else {
            ai.setValidUntil(Regex.getMilliSeconds(validUntil, "yyyy-MM-dd", null));
            account.setValid(true);
        }
        br.getPage("http://ugotfile.com/traffic/summary");
        String trafficleft = br.getRegex("Remaining Downloads</h3>.*?>((\\d+.*?)/.*?\\d+.*?)<").getMatch(1);
        if (trafficleft != null) ai.setTrafficLeft(Encoding.htmlDecode(trafficleft));
        String trafficmax = br.getRegex("Remaining Downloads</h3>.*?>(\\d+.*?/(.*?\\d+.*?))<").getMatch(1);
        if (trafficmax != null) ai.setTrafficMax(Regex.getSize(Encoding.htmlDecode(trafficmax)));
        return ai;
    }

    public void handlePremium(DownloadLink downloadLink, Account account) throws Exception {
        requestFileInformation(downloadLink);
        login(account);
        String finalUrl = null;
        br.getPage(downloadLink.getDownloadURL());
        if (br.getRedirectLocation() != null) {
            finalUrl = br.getRedirectLocation();
        } else {
            finalUrl = br.getRegex("Content.*?<a.*?href='(http://.*?ugotfile.com/.*?)'>").getMatch(0);
        }
        if (finalUrl == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFEKT);
        br.setFollowRedirects(true);
        dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, finalUrl, true, 0);
        dl.startDownload();
    }

    public void handleFree(DownloadLink link) throws Exception {
        this.setBrowserExclusive();
        br.clearCookies(link.getDownloadURL());
        br.getPage(link.getDownloadURL());
        int sleep = Integer.parseInt(br.getRegex("seconds: (\\d+)").getMatch(0));
        if (br.containsHTML("Your hourly traffic limit is exceeded.")) {
            int block = Integer.parseInt(br.getRegex("<div id='sessionCountDown' style='font-weight:bold; font-size:20px;'>(.*?)</div>").getMatch(0)) * 1000 + 1;
            throw new PluginException(LinkStatus.ERROR_IP_BLOCKED, block);
        }
        for (int i = 0; i <= 20; i++) {
            String cUrl = "http://ugotfile.com" + br.getRegex("<td><img src=\"(.*?)\" onclick=\"captchaReload\\(\\);\"").getMatch(0);
            String Captcha = getCaptchaCode(cUrl, link);
            Browser br2 = br.cloneBrowser();
            br2.getPage("http://ugotfile.com/captcha?key=" + Captcha);
            if (!br2.containsHTML("invalid key")) break;
            continue;
        }
        if (br.containsHTML("invalid key")) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFEKT);
        sleep(sleep * 1001, link);
        br.getPage("http://ugotfile.com/file/get-file");
        String dllink = null;
        dllink = br.getRegex("(.*)").getMatch(0);
        if (dllink == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFEKT);
        dl = jd.plugins.BrowserAdapter.openDownload(br, link, dllink, false, 1);
        dl.startDownload();
    }

    public AvailableStatus requestFileInformation(DownloadLink parameter) throws Exception {
        this.setBrowserExclusive();
        br.getPage(parameter.getDownloadURL());
        if (br.containsHTML("FileId and filename mismatched or file does not exist!")) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        if (br.containsHTML("has been deleted")) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        String filename = br.getRegex("<title>(.*?) - Free File Hosting - uGotFile</title>").getMatch(0);
        String filesize = br.getRegex("<span style=\"font-size: 14px;\">(.*?)</span>").getMatch(0);
        if (filename == null || filesize == null) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        filesize = filesize.replace("&nbsp;", "");
        parameter.setName(filename.trim());
        parameter.setDownloadSize(Regex.getSize(filesize.replaceAll(",", "")));
        return AvailableStatus.TRUE;
    }

    public void reset() {
    }

    public void resetDownloadlink(DownloadLink link) {
    }

    public int getMaxSimultanFreeDownloadNum() {
        return 1;
    }

}
