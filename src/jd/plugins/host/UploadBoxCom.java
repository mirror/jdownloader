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
package jd.plugins.host;

import java.io.File;

import jd.PluginWrapper;
import jd.http.Browser;
import jd.http.URLConnectionAdapter;
import jd.parser.Regex;
import jd.parser.html.Form;
import jd.plugins.DownloadLink;
import jd.plugins.LinkStatus;
import jd.plugins.Plugin;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;

public class UploadBoxCom extends PluginForHost {

    public UploadBoxCom(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public String getAGBLink() {
        return "http://uploadbox.com/en/terms/";
    }

    @Override
    public boolean getFileInformation(DownloadLink parameter) throws Exception {
        this.setBrowserExclusive();
        String id = new Regex(parameter.getDownloadURL(), "files/([0-9a-zA-Z]+)").getMatch(0);
        parameter.setUrlDownload("http://www.uploadbox.com/en/files/" + id);
        br.getPage(parameter.getDownloadURL());
        if (br.containsHTML("class=\"not_found\">")) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        String filename = br.getRegex("<li><strong>File name:</strong>(.*?)</li>").getMatch(0);
        String filesize = br.getRegex("<li><strong>File size:</strong>(.*?)</li>").getMatch(0);
        if (filename == null || filesize == null) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        parameter.setName(filename.trim());
        parameter.setDownloadSize(Regex.getSize(filesize));
        return true;
    }

    @Override
    public void handleFree(DownloadLink link) throws Exception {
        getFileInformation(link);
        Form form = br.getForm(1);
        if (form == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFEKT);
        br.submitForm(form);
        if (br.containsHTML("The last download from your IP was done less than 30 minutes ago")) {
            String strWaittime = br.getRegex("(\\d{2}:\\d{2}:\\d{2}) before you can download more").getMatch(0);
            String strWaittimeArray[] = strWaittime.split(":");
            throw new PluginException(LinkStatus.ERROR_IP_BLOCKED, "", ((Integer.parseInt(strWaittimeArray[0]) * 3600) + (Integer.parseInt(strWaittimeArray[1]) * 60) + Integer.parseInt(strWaittimeArray[2])) * 1000l);
        }
        form = br.getForm(1);
        File file = this.getLocalCaptchaFile(this);
        String captchaUrl = form.getRegex("captcha.*?src=\"(.*?)\"").getMatch(0);
        if (captchaUrl == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFEKT);
        URLConnectionAdapter con = br.cloneBrowser().openGetConnection(captchaUrl);
        Browser.download(file, con);
        String code = Plugin.getCaptchaCode(file, this, link);
        form.put("enter", code);
        br.submitForm(form);
        if (br.containsHTML("read the captcha code")) throw new PluginException(LinkStatus.ERROR_CAPTCHA);
        br.setDebug(true);
        String dlUrl = br.getRegex("please <a href=\"(.*?)\">click").getMatch(0);
        if (dlUrl == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFEKT);
        dl = br.openDownload(link, dlUrl, true, 1);
        dl.startDownload();
    }

    @Override
    public void reset() {
        // TODO Auto-generated method stub
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return 1;
    }

    @Override
    public String getVersion() {
        return getVersion("$Revision$");
    }

}
