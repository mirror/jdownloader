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
import jd.plugins.DownloadLink;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;
import jd.plugins.DownloadLink.AvailableStatus;

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "yourfilehost.com" }, urls = { "http://[\\w\\.]*?yourfilehost\\.com/media\\.php\\?cat=.*?\\&file=.+" }, flags = { 0 })
public class YourFileHostCom extends PluginForHost {

    public YourFileHostCom(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public String getAGBLink() {
        return "http://www.yourfilehost.com/terms.php";
    }

    @Override
    public AvailableStatus requestFileInformation(DownloadLink link) throws IOException, PluginException {
        this.setBrowserExclusive();
        br.getPage(link.getDownloadURL());
        if (br.containsHTML("File not found")) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        String filename = br.getRegex("<strong class=\"style26\">(.*?)</strong>").getMatch(0);
        if (filename == null) {
            filename = br.getRegex("parent\\.location='linkshare.*?file=(.*?)'").getMatch(0);
            if (filename == null) filename = br.getRegex("\\&file=(.*?)\"").getMatch(0);
        }
        if (filename == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        link.setFinalFileName(filename.trim());
        return AvailableStatus.TRUE;
    }

    private static final String CAPTCHAFAILEDTEXTS = "(Hit your browsers REFRESH BUTTON|enter the correct code|Please also make sure browser cookies are enabled|Please fill out the marked fields correctly and try again\\.)";

    @Override
    public void handleFree(DownloadLink downloadLink) throws Exception, PluginException {
        requestFileInformation(downloadLink);
        if (downloadLink.getDownloadURL().contains("media.php?cat=video")) br.getPage(downloadLink.getDownloadURL().replace("media.php?cat=video", "downloadfile.php?cat=video"));
        String dllink = null;
        // Only for files you need to enter captchas, not for videos!
        String captchaOrNot = br.getRegex("\"(/downloadlink\\.php.*?)\"").getMatch(0);
        if (captchaOrNot != null) {
            br.getPage("http://www.yourfilehost.com" + captchaOrNot);
            for (int i = 0; i <= 3; i++) {
                Form captchaform = br.getForm(0);
                String captchaid = br.getRegex("\"(captcha\\.php.*?)\"").getMatch(0);
                if (captchaform == null || captchaid == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                String code = getCaptchaCode("http://www.yourfilehost.com/" + captchaid, downloadLink);
                captchaform.put("verify", code);
                br.submitForm(captchaform);
                if (br.containsHTML(CAPTCHAFAILEDTEXTS)) continue;
                break;
            }
            if (br.containsHTML(CAPTCHAFAILEDTEXTS)) throw new PluginException(LinkStatus.ERROR_CAPTCHA);
            dllink = br.getRegex("style2\"><a href=\"(http.*?)\"").getMatch(0);
            if (dllink == null) br.getRegex("href=\"(http.*?)\"").getMatch(0);
        } else {
            // I don't know if this part is still needed
            dllink = br.getRegex("src\" value=\"(http.*?)\"").getMatch(0);
            String sourcelink = br.getRegex("movie\" value=\"(.*?)\"").getMatch(0);
            if (sourcelink == null) sourcelink = br.getRegex("<embed src=\"(.*?)\"").getMatch(0);
            if (sourcelink == null && dllink == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            // Regex the internal id of the video
            sourcelink = new Regex(Encoding.htmlDecode(sourcelink), "\\&cid=(.*?)\\&").getMatch(0);
            if (sourcelink == null && dllink == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            sourcelink = "http://www.yourfilehost.com/video-embed-code-new.php?vidlink=&cid=" + sourcelink;
            if (sourcelink != null && dllink == null) {
                // Find the final link to download
                br.getPage(sourcelink);
                dllink = br.getRegex("(http.*?)\\&homeurl").getMatch(0);
            }
        }
        if (dllink == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, Encoding.htmlDecode(dllink), true, 0);
        if (dl.getConnection().getContentType().contains("html")) {
            br.followConnection();
            if (br.containsHTML("No htmlCode read")) throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server error");
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