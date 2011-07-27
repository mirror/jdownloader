//jDownloader - Downloadmanager
//Copyright (C) 2010  JD-Team support@jdownloader.org
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
import jd.parser.Regex;
import jd.parser.html.Form;
import jd.plugins.DownloadLink;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;
import jd.plugins.DownloadLink.AvailableStatus;

import org.appwork.utils.formatter.SizeFormatter;

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "moviesnxs.com" }, urls = { "http://(www\\.)?moviesnxs\\.com/web/gallery/mnxs\\.php\\?id=\\d+" }, flags = { 0 })
public class MoviesNxsCom extends PluginForHost {

    public MoviesNxsCom(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public String getAGBLink() {
        return "http://www.moviesnxs.com/web/movieboard/register.php";
    }

    private static final String CAPTCHATEXT = "securimage_show\\.php";

    @Override
    public AvailableStatus requestFileInformation(DownloadLink link) throws IOException, PluginException {
        this.setBrowserExclusive();
        br.setFollowRedirects(false);
        br.getPage(link.getDownloadURL());
        if (br.containsHTML(">Clip not found")) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        Regex fInfo = br.getRegex("Type:(.{1,5})<BR>Dimensions: .{1,40} <BR>Duration: \\d+:\\d+ <BR>Audio: .{1,40}<BR>File Size: (.*?)<br>(Title: (.*?)<br>)?");
        String filename = fInfo.getMatch(2);
        String filesize = fInfo.getMatch(1);
        if (filename != null) {
            String ext = fInfo.getMatch(0);
            if (ext == null)
                ext = "";
            else
                ext = "." + ext;
            link.setName(filename.trim() + ext);
        }
        if (filesize != null) link.setDownloadSize(SizeFormatter.getSize(filesize));
        return AvailableStatus.TRUE;
    }

    @Override
    public void handleFree(DownloadLink downloadLink) throws Exception, PluginException {
        requestFileInformation(downloadLink);
        long timeBefore = System.currentTimeMillis();
        Form[] allForms = br.getForms();
        if (allForms == null || allForms.length == 0) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        Form captchaForm = null;
        for (Form lol : allForms) {
            if (lol.containsHTML("captcha_code")) {
                captchaForm = lol;
                break;
            }
        }
        if (!br.containsHTML(CAPTCHATEXT) || captchaForm == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        String code = getCaptchaCode("http://www.moviesnxs.com/web/gallery/securimage_show.php", downloadLink);
        captchaForm.put("captcha_code", code);
        int passedTime = (int) ((System.currentTimeMillis() - timeBefore) / 1000) - 1;
        String waittime = br.getRegex("var seconds=(\\d+);").getMatch(0);
        int wait = 120;
        if (waittime != null) wait = Integer.parseInt(waittime);
        sleep((wait - passedTime) * 1001l, downloadLink);
        br.submitForm(captchaForm);
        if (br.containsHTML(CAPTCHATEXT) || br.containsHTML("(The text code you entered was incorrect|Go back and try again\\.)")) throw new PluginException(LinkStatus.ERROR_CAPTCHA);
        if (!br.containsHTML("/web/gallery/free_download\\.php\\?mnxs_id=")) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        String dllink = "http://www.moviesnxs.com/web/gallery/free_download.php?mnxs_id=" + new Regex(downloadLink.getDownloadURL(), "web/gallery/mnxs\\.php\\?id=(\\d+)").getMatch(0);
        dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, dllink, true, 1);
        if (dl.getConnection().getContentType().contains("html")) {
            br.followConnection();
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        dl.startDownload();
    }

    @Override
    public void reset() {
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return -1;
    }

    @Override
    public void resetDownloadlink(DownloadLink link) {
    }

}