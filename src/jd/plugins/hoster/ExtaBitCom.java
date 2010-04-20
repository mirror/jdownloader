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

import java.io.File;

import jd.PluginWrapper;
import jd.http.Browser;
import jd.parser.Regex;
import jd.plugins.DownloadLink;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.pluginUtils.Recaptcha;
import jd.utils.locale.JDL;

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "extabit.com" }, urls = { "http://[\\w\\.]*?extabit\\.com/file/[a-z0-9]+" }, flags = { 0 })
public class ExtaBitCom extends PluginForHost {

    public ExtaBitCom(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public String getAGBLink() {
        return "http://extabit.com/static/terms.jsp";
    }

    @Override
    public AvailableStatus requestFileInformation(DownloadLink downloadLink) throws Exception {
        this.setBrowserExclusive();
        // To get the english version of the page
        br.setCookie("http://extabit.com", "language", "en");
        br.setFollowRedirects(true);
        br.getPage(downloadLink.getDownloadURL());
        if (br.containsHTML("(File not found|Such file doesn't exsist)")) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        String filename = br.getRegex("<title>(.*?)download Extabit.com - file hosting</title>").getMatch(0);
        if (filename == null) {
            filename = br.getRegex("download_filename\".*?>(.*?)</div").getMatch(0);
            if (filename == null) {
                filename = br.getRegex("extabit\\.com/file/.*?'>(.*?)</a>").getMatch(0);
            }
        }
        String filesize = br.getRegex("class=\"download_filesize(_en)\">.*?\\[(.*?)\\]").getMatch(1);
        if (filename == null) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        downloadLink.setName(filename.trim());
        if (filesize != null) downloadLink.setDownloadSize(Regex.getSize(filesize));
        if (br.containsHTML(">Only premium users can download files of this size")) downloadLink.getLinkStatus().setStatusText(JDL.L("plugins.hoster.ExtaBitCom.errors.Only4Premium", "This file is only available for premium users"));
        return AvailableStatus.TRUE;
    }

    @Override
    public void handleFree(DownloadLink link) throws Exception {
        this.setBrowserExclusive();
        requestFileInformation(link);
        if (br.containsHTML(">Only premium users can download files of this size")) throw new PluginException(LinkStatus.ERROR_FATAL, JDL.L("plugins.hoster.ExtaBitCom.errors.Only4Premium", "This file is only available for premium users"));
        String addedlink = br.getURL();
        if (!addedlink.equals(link.getDownloadURL())) link.setUrlDownload(addedlink);
        if (br.containsHTML("The daily downloads limit from your IP is exceeded")) throw new PluginException(LinkStatus.ERROR_IP_BLOCKED, 60 * 60 * 1000l);
        // If the waittime was forced it yould be here but it isn't!
        // Re Captcha handling
        Browser xmlbrowser = br.cloneBrowser();
        xmlbrowser.getHeaders().put("X-Requested-With", "XMLHttpRequest");
        if (br.containsHTML("api.recaptcha.net")) {
            Recaptcha rc = new Recaptcha(br);
            rc.parse();
            rc.load();
            File cf = rc.downloadCaptcha(getLocalCaptchaFile());
            String c = getCaptchaCode(cf, link);
            rc.getForm().put("capture", "1");
            rc.getForm().setAction(addedlink);
            rc.setCode(c);
        } else {
            for (int i = 0; i <= 5; i++) {
                // *Normal* captcha handling
                // Form dlform = br.getFormbyProperty("id", "cmn_form");
                String captchaurl = br.getRegex("(/capture\\.gif\\?\\d+)").getMatch(0);
                if (captchaurl == null) captchaurl = br.getRegex("<div id=\"reload_captcha\">.*?<img src=\"(/.*?)\"").getMatch(0);
                if (captchaurl == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                captchaurl = "http://extabit.com" + captchaurl;
                String code = getCaptchaCode(captchaurl, link);
                // dlform.put("capture", code);
                // br.submitForm(dlform);
                xmlbrowser.getPage(link.getDownloadURL() + "?capture=" + code);
                if (!xmlbrowser.containsHTML("\"ok\":true")) {
                    br.getPage(br.getURL());
                    continue;
                }
                br.getPage(link.getDownloadURL());
                break;
            }
        }
        if (br.containsHTML("api.recaptcha.net") || !xmlbrowser.containsHTML("\"ok\":true")) throw new PluginException(LinkStatus.ERROR_CAPTCHA);
        String dllink = br.getRegex("Turn your download manager off and <a href=\"(http.*?)\">click here to download").getMatch(0);
        if (dllink == null) dllink = br.getRegex("\"(http://\\d+\\.\\d+\\.\\d+\\.\\d+/[a-z0-9]+/.*?)\"").getMatch(0);
        if (dllink == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        jd.plugins.BrowserAdapter.openDownload(br, link, dllink, false, 1);
        if ((dl.getConnection().getContentType().contains("html"))) {
            if (dl.getConnection().getResponseCode() == 503) throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, 60 * 60 * 1000l);
            if (dl.getConnection().getResponseCode() == 404) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        dl.startDownload();
    }

    @Override
    public void reset() {
    }

    @Override
    public void resetDownloadlink(DownloadLink link) {
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return 1;
    }

}
