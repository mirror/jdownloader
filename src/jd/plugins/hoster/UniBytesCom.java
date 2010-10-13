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

    private static final String CAPTCHATEXT = "captcha\\.jpg";

    @Override
    public AvailableStatus requestFileInformation(DownloadLink link) throws IOException, PluginException {
        this.setBrowserExclusive();
        // Use the english language
        br.setCookie("http://www.unibytes.com/", "lang", "en");
        br.getPage(link.getDownloadURL());
        if (br.containsHTML("<p>File not found or removed</p>")) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        String filename = br.getRegex("style=\" font-weight: bold; color:#252525;\">(.*?)</span><br/>").getMatch(0);
        String filesize = br.getRegex("\\(([0-9\\.]+ [A-Za-z]+)\\)</h3><p").getMatch(0);
        if (filename == null || filesize == null) {
            // Leave this in
            logger.warning("Filename = " + filename);
            logger.warning("Filesize = " + filesize);
            logger.warning(br.toString());
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        link.setName(filename.trim());
        link.setDownloadSize(Regex.getSize(filesize));
        return AvailableStatus.TRUE;
    }

    @Override
    public void handleFree(DownloadLink downloadLink) throws Exception, PluginException {
        requestFileInformation(downloadLink);
        String addedLink = downloadLink.getDownloadURL();
        br.setFollowRedirects(false);
        br.postPage(addedLink, "step=timer&referer=&ad=");
        if (br.containsHTML("showNotUniqueIP\\(\\);")) throw new PluginException(LinkStatus.ERROR_IP_BLOCKED, "Too many simultan downloads");
        int iwait = 60;
        String regexedTime = br.getRegex("id=\"slowRest\">(\\d+)</").getMatch(0);
        if (regexedTime == null) regexedTime = br.getRegex("var timerRest = (\\d+);").getMatch(0);
        if (regexedTime != null) iwait = Integer.parseInt(regexedTime);
        String ipBlockedTime = br.getRegex("guestDownloadDelayValue\">(\\d+)</span>").getMatch(0);
        if (ipBlockedTime == null) ipBlockedTime = br.getRegex("guestDownloadDelay\\((\\d+)\\);").getMatch(0);
        if (ipBlockedTime != null) throw new PluginException(LinkStatus.ERROR_IP_BLOCKED, Integer.parseInt(ipBlockedTime) * 60 * 1001l);
        String s = br.getRegex("name=\"s\" value=\"(.*?)\"").getMatch(0);
        if (s == null) {
            logger.warning("s1 equals null!");
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        sleep(iwait * 1001l, downloadLink);
        br.postPage(downloadLink.getDownloadURL(), "step=next&s=" + s + "&referer=" + addedLink);
        s = br.getRegex("name=\"s\" value=\"(.*?)\"").getMatch(0);
        if (s == null) {
            logger.warning("s2 equals null!");
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        br.postPage(downloadLink.getDownloadURL(), "step=captcha&s=" + s + "&referer=" + addedLink);
        if (br.containsHTML(CAPTCHATEXT)) {
            logger.info("Captcha found");
            for (int i = 0; i <= 5; i++) {
                String code = getCaptchaCode("http://www.unibytes.com/captcha.jpg", downloadLink);
                String post = "s=" + s + "&referer=" + addedLink + "&step=last&captcha=" + code;
                br.postPage(downloadLink.getDownloadURL(), post);
                if (!br.containsHTML(CAPTCHATEXT)) break;
            }
            if (br.containsHTML(CAPTCHATEXT)) throw new PluginException(LinkStatus.ERROR_CAPTCHA);
        } else {
            logger.info("Captcha not found");
        }
        String dllink = br.getRegex("\"(http://st\\d+\\.unibytes\\.com/fdload/file.*?)\"").getMatch(0);
        if (dllink == null) dllink = br.getRegex("style=\"width: 650px; margin: 40px auto; text-align: center; font-size: 2em;\"><a href=\"(.*?)\"").getMatch(0);
        if (dllink == null) {
            logger.warning("dllink equals null!");
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, dllink, false, 1);
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
        return 1;
    }

    @Override
    public void resetDownloadlink(DownloadLink link) {
    }

}