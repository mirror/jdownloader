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
import jd.plugins.DownloadLink;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;
import jd.plugins.DownloadLink.AvailableStatus;

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "unibytes.com" }, urls = { "http://[\\w\\.]*?unibytes\\.com/.+" }, flags = { 0 })
public class UniBytesCom extends PluginForHost {

    public UniBytesCom(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public String getAGBLink() {
        return "http://www.unibytes.com/page/terms";
    }

    private static final String CAPTCHATEXT = "captcha.jpg";

    @Override
    public AvailableStatus requestFileInformation(DownloadLink link) throws IOException, PluginException {
        this.setBrowserExclusive();
        // Use the english language
        br.setCookie("http://www.unibytes.com/", "lang", "en");
        br.getPage(link.getDownloadURL());
        if (br.containsHTML("<p>File not found or removed</p>")) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        Regex theStuff = br.getRegex("You are trying to download file: <span style=\".*?\">(.*?)</span> \\((.*?)\\)</h3>");
        String filename = theStuff.getMatch(0);
        String filesize = theStuff.getMatch(1);
        if (filename == null || filesize == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        link.setName(filename.trim());
        link.setDownloadSize(Regex.getSize(filesize));
        return AvailableStatus.TRUE;
    }

    @Override
    public void handleFree(DownloadLink downloadLink) throws Exception, PluginException {
        requestFileInformation(downloadLink);
        if (!br.containsHTML("captcha.jpg")) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        br.setFollowRedirects(false);
        br.postPage(downloadLink.getDownloadURL(), "ptl-ajax=true&ptl-action=startSlow&ptl-pagestate=&referer=" + Encoding.urlEncode("http://www.google.de/"));
        int iwait = 60;
        String regexedTime = br.getRegex("evaluate>startSlow\\('.*?', (\\d+)\\)</evaluate>").getMatch(0);
        if (br.containsHTML("showNotUniqueIP\\(\\);")) throw new PluginException(LinkStatus.ERROR_IP_BLOCKED, "Too many simultan downloads");
        if (regexedTime != null) iwait = Integer.parseInt(regexedTime);
        String s = br.getRegex("startSlow\\('(.*?)'").getMatch(0);
        if (s == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        sleep(iwait * 1001l, downloadLink);
        for (int i = 0; i <= 5; i++) {
            String code = getCaptchaCode("http://www.unibytes.com/captcha.jpg", downloadLink);
            String post = "s=" + s + "&referer=&captcha=" + code;
            br.postPage(downloadLink.getDownloadURL(), post);
            if (br.getRedirectLocation() != null) break;
            System.out.print(br.toString());
        }
        if (br.containsHTML(CAPTCHATEXT)) throw new PluginException(LinkStatus.ERROR_CAPTCHA);
        if (br.getRedirectLocation() == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, br.getRedirectLocation(), false, 0);
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