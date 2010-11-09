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
import jd.http.RandomUserAgent;
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

    @Override
    public String getAGBLink() {
        return "http://uploadbox.com/en/terms/";
    }

    @Override
    public void correctDownloadLink(DownloadLink parameter) {
        String id = new Regex(parameter.getDownloadURL(), "files/([0-9a-zA-Z]+)").getMatch(0);
        parameter.setUrlDownload("http://www.uploadbox.com/en/files/" + id);
    }

    public void login(Account account) throws IOException, PluginException {
        this.setBrowserExclusive();
        br.setDebug(true);
        br.getHeaders().put("User-Agent", RandomUserAgent.generate());
        br.getPage("http://uploadbox.com/?ac=lang&lang_new=en");
        br.getPage("http://uploadbox.com/en");
        br.postPage("http://uploadbox.com/en", "login=" + Encoding.urlEncode(account.getUser()) + "&passwd=" + Encoding.urlEncode(account.getPass()) + "&ac=auth&back=");
        if (br.containsHTML("You enter wrong user name or password")) throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_DISABLE);
        br.getPage("http://uploadbox.com/");
        if (br.containsHTML("Type: FREE")) throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_DISABLE);
    }

    @Override
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

    @Override
    public AvailableStatus requestFileInformation(DownloadLink parameter) throws Exception {
        this.setBrowserExclusive();
        br.getHeaders().put("User-Agent", RandomUserAgent.generate());
        br.getPage(parameter.getDownloadURL());
        if (br.containsHTML("(class=\"not_found\">|File deleted from service)") || !br.containsHTML("<h3>Downloading</h3>")) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        if (br.containsHTML("id=\"error\">")) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        String filename = br.getRegex(">File name:</td>.*?<b>(.*?)</b>").getMatch(0);
        if (filename == null) {
            filename = br.getRegex("<title>UploadBox.*?Downloading(.*?)</title>").getMatch(0);
            if (filename == null) {
                filename = br.getRegex("description\" content=\"Downloading(.*?)! Free").getMatch(0);

            }
        }
        String filesize = br.getRegex("Size:</span>(.*?)<s").getMatch(0);
        if (filename == null) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        parameter.setName(filename.trim());
        if (filesize != null) {
            parameter.setDownloadSize(Regex.getSize(filesize.trim()));
        }
        return AvailableStatus.TRUE;
    }

    @Override
    public void handlePremium(DownloadLink parameter, Account account) throws Exception {
        requestFileInformation(parameter);
        login(account);
        br.setFollowRedirects(false);
        br.getHeaders().put("User-Agent", RandomUserAgent.generate());
        br.getPage(parameter.getDownloadURL());
        String dlUrl = br.getRegex("title=\"Direct link\">(http://.*?)</a>").getMatch(0);
        if (dlUrl == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        dl = jd.plugins.BrowserAdapter.openDownload(br, parameter, dlUrl, true, 0);
        dl.startDownload();
    }

    @Override
    public void handleFree(DownloadLink link) throws Exception {
        requestFileInformation(link);
        Form form = br.getForm(1);
        if (form == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        br.submitForm(form);
        if (br.containsHTML("The last download from your IP was done less than 30 minutes ago")) {
            String strWaittime = br.getRegex("(\\d{2}:\\d{2}:\\d{2}) before you can download more").getMatch(0);
            String strWaittimeArray[] = strWaittime.split(":");
            throw new PluginException(LinkStatus.ERROR_IP_BLOCKED, ((Integer.parseInt(strWaittimeArray[0]) * 3600) + (Integer.parseInt(strWaittimeArray[1]) * 60) + Integer.parseInt(strWaittimeArray[2])) * 1000l);
        }
        if (br.containsHTML("The limit of traffic for you is exceeded")) {
            String wait = br.getRegex("traffic for you is exceeded.*?Please.*?wait.*?>(\\d+)<").getMatch(0);
            int waittime = 30;
            if (wait != null) waittime = Integer.parseInt(wait.trim());
            throw new PluginException(LinkStatus.ERROR_IP_BLOCKED, waittime * 60 * 1000l);
        }
        form = br.getFormbyProperty("id", "free");
        String captchaUrl = br.getRegex("\"(/\\?ac=captcha\\&code=.*?)\"").getMatch(0);
        if (captchaUrl == null) {
            captchaUrl = br.getRegex("<p><span>Downloaded:</span>[0-9 ]+</p>.*?<img id=\".*?(ccaptcha)?.*?\" src=\"(/.*?)\"").getMatch(1);
            if (captchaUrl == null) {
                captchaUrl = br.getRegex("class=\"anti_bot\" id=\"capbox\">.*?<div class=\"kapch_img\">.*?<img id=\"ccaptcha\" src=\"(/.*?)\"").getMatch(0);
                if (captchaUrl == null) {
                    captchaUrl = br.getRegex("<h3>File download</h3>.*?src=\"(/.*?)\"").getMatch(0);
                }
            }
        }
        if (form == null || captchaUrl == null) {
            logger.warning("Form or captchaurl is not found!");
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        if (!captchaUrl.startsWith("http")) captchaUrl = "http://www.uploadbox.com" + captchaUrl;
        captchaUrl = captchaUrl.replace("amp;", "");
        String code = getCaptchaCode(captchaUrl, link);
        form.put("enter", code);
        br.submitForm(form);
        if (br.containsHTML("read the code")) throw new PluginException(LinkStatus.ERROR_CAPTCHA);
        String wait = br.getRegex("Please use premium account or wait (\\d+) minutes to unblock your ip").getMatch(0);
        if (wait != null) throw new PluginException(LinkStatus.ERROR_IP_BLOCKED, Integer.parseInt(wait) * 60 * 1001l);
        String dllink = br.getRegex("id=\"file_download\".*?src=\"(.*?)\"").getMatch(0);
        if (dllink == null) {
            dllink = br.getRegex("If downloading hasn't started.*?<a href=\"(.*?)\"").getMatch(0);
            if (dllink == null) {
                dllink = br.getRegex("\"(http://[a-z0-9]+-get\\.uploadbox\\.com/get/.*?)\"").getMatch(0);
            }
        }
        br.setDebug(true);
        if (dllink == null) {
            logger.warning("dllink is null...");
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        dl = jd.plugins.BrowserAdapter.openDownload(br, link, dllink, true, 1);
        if (dl.getConnection().getContentType().contains("html")) {
            br.followConnection();
            if (br.containsHTML("(<title>404 Not Found</title>|<h1>404 Not Found</h1>)")) throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server error", 60 * 60 * 1000l);
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        dl.startDownload();
    }

    @Override
    public void reset() {
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return 1;
    }

    @Override
    public void resetDownloadlink(DownloadLink link) {
    }

}
