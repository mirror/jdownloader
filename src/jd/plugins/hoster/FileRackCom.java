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

import java.util.regex.Pattern;

import jd.PluginWrapper;
import jd.parser.Regex;
import jd.parser.html.Form;
import jd.plugins.DownloadLink;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;
import jd.plugins.DownloadLink.AvailableStatus;

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "file-rack.com" }, urls = { "http://[\\w\\.]*?file-rack.com/files/[0-9A-Za-z]+/[0-9A-Za-z.-]+" }, flags = { 0 })
public class FileRackCom extends PluginForHost {

    public FileRackCom(PluginWrapper wrapper) {
        super(wrapper);
        // TODO Auto-generated constructor stub
    }

    @Override
    public String getAGBLink() {
        return "http://www.file-rack.com/tos.html";
    }

    @Override
    public void handleFree(DownloadLink link) throws Exception {
        br.setReadTimeout(30000);
        requestFileInformation(link);
        this.setBrowserExclusive();
        br.getPage(link.getDownloadURL());
        Form[] form = br.getForms();
        form[0].remove("method_premium");
        br.submitForm(form[0]);
        if (br.containsHTML("File is deleted.")) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        int sleep = Integer.parseInt(br.getRegex("Wait <span id=\"countdown\">(.*?)</span> seconds").getMatch(0));
        sleep(sleep * 1001, link);
        int retry = 0;
        do {
            if (retry == 5) throw new PluginException(LinkStatus.ERROR_CAPTCHA);
            form = br.getForms();
            String captchaUrl = "http://www.file-rack.com/vimage/img.php?size=" + br.getRegex("<td><img src='vimage/img.php\\?size=(.*?)'></td>").getMatch(0);
            String captchaCode = getCaptchaCode(captchaUrl, link);
            form[0].put("vImageCodP", captchaCode);
            br.submitForm(form[0]);
            retry++;
        } while (br.containsHTML("<b>Verification Code doesn't match </b>"));
        String dllink = br.getRegex(Pattern.compile("<span id=\"btn_download2\">.*<a href=\"(.*?)\" onclick=\"disableimg\\(\\)\">", Pattern.DOTALL)).getMatch(0);
        dl = br.openDownload(link, dllink, true, -20);
        dl.startDownload();

    }

    @Override
    public AvailableStatus requestFileInformation(DownloadLink parameter) throws Exception {
        this.setBrowserExclusive();
        br.getPage(parameter.getDownloadURL());
        String filename = br.getRegex("<h2>Download File (.*?)</h2>").getMatch(0);
        String filesize = br.getRegex("You have requested <i>http://www.file-rack.com/files/[0-9A-Za-z]+/(.*?)</i> <b>(.*?) </b><small>.*</font>").getMatch(1);
        if (filename == null || filesize == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFEKT);
        parameter.setName(filename.trim());
        parameter.setDownloadSize(Regex.getSize(filesize.replaceAll(",", "\\.")));
        return AvailableStatus.TRUE;
    }

    @Override
    public void reset() {
        // TODO Auto-generated method stub

    }

    @Override
    public void resetDownloadlink(DownloadLink link) {
        // TODO Auto-generated method stub

    }
    
    // @Override
    public int getMaxSimultanFreeDownloadNum() {
        return 20;
    }
}
