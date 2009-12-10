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
import jd.parser.Regex;
import jd.parser.html.Form;
import jd.plugins.DownloadLink;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.pluginUtils.Recaptcha;

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
        String filesize = br.getRegex("File size: <span class=.*?>(.*?)</").getMatch(0);
        if (filename == null) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        downloadLink.setName(filename.trim());
        if (filesize != null) downloadLink.setDownloadSize(Regex.getSize(filesize));
        return AvailableStatus.TRUE;
    }

    @Override
    public void handleFree(DownloadLink link) throws Exception {
        this.setBrowserExclusive();
        requestFileInformation(link);
        String addedlink = br.getURL();
        br.getPage(addedlink + "?go");
        // If the waittime was forced it yould be here but it isn't!
        // Re Captcha handling
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
            // *Normal* captcha handling
            Form dlform = br.getFormbyProperty("id", "cmn_form");
            String captchaurl = br.getRegex("\"(/capture.*?\\?-?[0-9]+)\"").getMatch(0);
            if (dlform == null || captchaurl == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            captchaurl = "http://extabit.com" + captchaurl;
            String code = getCaptchaCode(captchaurl, link);
            dlform.put("capture", code);
            br.submitForm(dlform);
        }
        if (br.containsHTML("(api.recaptcha.net|/capture)")) throw new PluginException(LinkStatus.ERROR_CAPTCHA);
        br.getPage(addedlink + "?af");
        String dllink = br.getRegex("color:black;font-weight:normal;font-family:arial;\" href=\"(.*?)\"").getMatch(0);
        if (dllink == null) dllink = br.getRegex("\"(http://[a-zA-Z]+[0-9]+\\.extabit\\.com/.*?/.*?)\"").getMatch(0);
        if (dllink == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        jd.plugins.BrowserAdapter.openDownload(br, link, dllink, false, 1);
        if ((dl.getConnection().getContentType().contains("html"))) {
            if (dl.getConnection().getResponseCode() == 503) throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, 60 * 60 * 1000l);
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
        return 2;
    }

}
