//    jDownloader - Downloadmanager
//    Copyright (C) 2008  JD-Team support@jdownloader.org
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
import java.io.IOException;

import java.util.regex.Pattern;

import jd.PluginWrapper;
import jd.http.Browser;
import jd.http.Encoding;
import jd.http.URLConnectionAdapter;
import jd.parser.Regex;
import jd.parser.html.Form;

import jd.plugins.DownloadLink;
import jd.plugins.LinkStatus;
import jd.plugins.Plugin;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;

public class FileloadUs extends PluginForHost {

    public FileloadUs(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public void handleFree(DownloadLink downloadLink) throws Exception {
        getFileInformation(downloadLink);
        br.setFollowRedirects(false);
        br.setDebug(true);
        Form[] Forms = br.getForms();
        Form Form1 = Forms[0];
        Form1.setAction(downloadLink.getDownloadURL());
        Form1.remove("method_premium");
        Form1.put("referer", Encoding.urlEncode(downloadLink.getDownloadURL()));
        br.submitForm(Form1);
        if (br.containsHTML("You have to wait")) {
            int minutes, seconds = 0;
            minutes = Integer.parseInt(br.getRegex("have\\s+to\\s+wait\\s+(\\d+)\\s+minutes").getMatch(0));
            seconds = Integer.parseInt(br.getRegex("minutes\\,\\s+(\\d+)\\s+seconds").getMatch(0));
            int waittime = ((60 * minutes) + seconds + 1) * 1000;
            throw new PluginException(LinkStatus.ERROR_IP_BLOCKED, null, waittime);
        } else {
            Forms = br.getForms();
            Form1 = br.getFormbyProperty("name", "F1");
            if (Form1 == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFEKT);
            // TODO: AntiCaptcha Method would allow simultanous connections
            String captchaurl = br.getRegex(Pattern.compile("below:</b></td></tr>\\s+<tr><td><img src=\"(.*?)\"", Pattern.DOTALL | Pattern.CASE_INSENSITIVE)).getMatch(0);
            URLConnectionAdapter con = br.openGetConnection(captchaurl);
            File file = this.getLocalCaptchaFile(this);
            Browser.download(file, con);
            String code = Plugin.getCaptchaCode(file, this, downloadLink);
            Form1.put("code", code);
            Form1.setAction(downloadLink.getDownloadURL());
            this.sleep(15000, downloadLink);
            br.submitForm(Form1);
            URLConnectionAdapter con2 = br.getHttpConnection();
            if (con2.getContentType().contains("html")) {
                String error = br.getRegex("class=\"err\">(.*?)</font>").getMatch(0);
                logger.warning(error);
                if (error.equalsIgnoreCase("Wrong captcha") || error.equalsIgnoreCase("Session expired"))
                    throw new PluginException(LinkStatus.ERROR_CAPTCHA);
                else
                    throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, error, 10000);
            }
            dl = br.openDownload(downloadLink, br.getRedirectLocation());
            dl.startDownload();
        }
    }

    @Override
    // TODO: AntiCaptcha Method would allow simultanous connections
    // if user is quick; he can enter captchas one-by-one and then server allow
    // him simulatanous downloads
    // that's why I left it 10.
    public int getMaxSimultanFreeDownloadNum() {
        return 10;
    }

    @Override
    public String getAGBLink() {
        return "http://fileload.us/tos.html";
    }

    @Override
    public boolean getFileInformation(DownloadLink downloadLink) throws IOException, PluginException {
        this.setBrowserExclusive();
        br.getPage("http://fileload.us/?op=change_lang&lang=english");
        br.getPage(downloadLink.getDownloadURL());
        if (br.containsHTML("File Not Found")) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        String filename = Encoding.htmlDecode(br.getRegex("You\\s+have\\s+requested:\\s+(.*?)\\s+-").getMatch(0));
        String filesize = br.getRegex("Size\\s+\\((.*?)\\)</font>").getMatch(0);
        if (filename == null || filesize == null) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        downloadLink.setName(filename);
        downloadLink.setDownloadSize(Regex.getSize(filesize));
        return true;
    }

    @Override
    public String getVersion() {
        return getVersion("$Revision$");
    }

    @Override
    public void reset() {
    }

    @Override
    public void resetPluginGlobals() {
    }

}