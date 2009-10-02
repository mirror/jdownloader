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
import jd.parser.html.Form;
import jd.plugins.Account;
import jd.plugins.AccountInfo;
import jd.plugins.DownloadLink;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;
import jd.plugins.DownloadLink.AvailableStatus;

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "uploadbox.com" }, urls = { "http://[\\w\\.]*?uploadbox\\.com/.*?files/[0-9a-zA-Z]+" }, flags = { 2 })
public class UploadBoxCom extends PluginForHost {

    public UploadBoxCom(PluginWrapper wrapper) {
        super(wrapper);
        this.enablePremium("http://uploadbox.com/en/premium/");
    }

    public String getAGBLink() {
        return "http://uploadbox.com/en/terms/";
    }

    public void correctDownloadLink(DownloadLink parameter) {
        String id = new Regex(parameter.getDownloadURL(), "files/([0-9a-zA-Z]+)").getMatch(0);
        parameter.setUrlDownload("http://www.uploadbox.com/en/files/" + id);
    }

    public void login(Account account) throws IOException, PluginException {
        this.setBrowserExclusive();
        br.getPage("http://uploadbox.com/en/premium/?ac=lang&lang_new=en");
        br.getPage("http://uploadbox.com/en/premium/");
        br.postPage("http://uploadbox.com/en", "login=" + Encoding.urlEncode(account.getUser()) + "&passwd=" + Encoding.urlEncode(account.getPass()) + "&ac=auth&back=");
        if (br.containsHTML("You enter wrong user name or password")) throw new PluginException(LinkStatus.ERROR_PREMIUM, LinkStatus.VALUE_ID_PREMIUM_DISABLE);
        br.getPage("http://uploadbox.com/en/");
        if (br.containsHTML("Your account type:</strong> FREE")) throw new PluginException(LinkStatus.ERROR_PREMIUM, LinkStatus.VALUE_ID_PREMIUM_DISABLE);
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
        /*
         * TODO: seems like hoster has some problems, becase there is now way to
         * get infos like trafficlimit, expiredate
         */
        account.setValid(true);
        return ai;
    }

    public AvailableStatus requestFileInformation(DownloadLink parameter) throws Exception {
        this.setBrowserExclusive();
        br.getPage(parameter.getDownloadURL());
        if (br.containsHTML("class=\"not_found\">")) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        if (br.containsHTML("id=\"error\">")) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        String filename = br.getRegex(">File name:</td>.*?<b>(.*?)</b>").getMatch(0);
        if (filename == null) {
            filename = br.getRegex("<title>UploadBox.*?Downloading(.*?)</title>").getMatch(0).trim();
            if (filename == null) {
                filename = br.getRegex("description\" content=\"Downloading(.*?)! Free").getMatch(0).trim();

            }
        }
        String filesize = br.getRegex(">S[ie]+ze:</td>.*?<td>.*?<b>(.*?)</b>").getMatch(0);
        if (filename == null) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        parameter.setName(filename.trim());
        if (filesize != null) {
            parameter.setDownloadSize(Regex.getSize(filesize));
        }
        return AvailableStatus.TRUE;
    }

    public void handlePremium(DownloadLink parameter, Account account) throws Exception {
        requestFileInformation(parameter);
        login(account);
        br.setFollowRedirects(false);
        br.getPage(parameter.getDownloadURL());
        String dlUrl = br.getRegex("title=\"Direct link\">(http://.*?)</a>").getMatch(0);
        if (dlUrl == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFEKT);
        dl = jd.plugins.BrowserAdapter.openDownload(br, parameter, dlUrl, true, 0);
        dl.startDownload();
    }

    public void handleFree(DownloadLink link) throws Exception {
        requestFileInformation(link);
        Form form = br.getFormbyProperty("id", "free");
        if (form == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFEKT);
        br.submitForm(form);
        if (br.containsHTML("The last download from your IP was done less than 30 minutes ago")) {
            String strWaittime = br.getRegex("(\\d{2}:\\d{2}:\\d{2}) before you can download more").getMatch(0);
            String strWaittimeArray[] = strWaittime.split(":");
            throw new PluginException(LinkStatus.ERROR_IP_BLOCKED, null, ((Integer.parseInt(strWaittimeArray[0]) * 3600) + (Integer.parseInt(strWaittimeArray[1]) * 60) + Integer.parseInt(strWaittimeArray[2])) * 1000l);
        }
        form = br.getFormbyProperty("id", "free");
        String captchaUrl = form.getRegex("captcha.*?src=\"(.*?)\"").getMatch(0);
        if (captchaUrl == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFEKT);
        String code = getCaptchaCode(captchaUrl, link);
        form.put("enter", code);
        br.submitForm(form);
        if (br.containsHTML("read the captcha code")) throw new PluginException(LinkStatus.ERROR_CAPTCHA);
        br.setDebug(true);
        String dlUrl = br.getRegex("please <a href=\"(.*?)\">click").getMatch(0);
        if (dlUrl == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFEKT);
        dl = jd.plugins.BrowserAdapter.openDownload(br, link, dlUrl, true, 1);
        dl.startDownload();
    }

    public void reset() {
    }

    public int getMaxSimultanFreeDownloadNum() {
        return 1;
    }

    public void resetDownloadlink(DownloadLink link) {
    }

}
