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
import jd.plugins.DownloadLink;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;
import jd.plugins.DownloadLink.AvailableStatus;

import org.appwork.utils.formatter.SizeFormatter;

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "upload24.net" }, urls = { "http://(www\\.)?upload24\\.net/[a-z0-9]+\\.[a-z0-9]+" }, flags = { 0 })
public class Upload24Net extends PluginForHost {

    public Upload24Net(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public String getAGBLink() {
        return "http://upload24.net/?act=agreement";
    }

    private static final String MAINPAGE = "http://upload24.net";

    @Override
    public AvailableStatus requestFileInformation(DownloadLink link) throws IOException, PluginException {
        this.setBrowserExclusive();
        br.setCustomCharset("utf-8");
        br.setFollowRedirects(false);
        br.getPage(link.getDownloadURL());
        if (br.containsHTML("<title>upload24\\.net </title>")) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        String filename = br.getRegex("<div class=\"fname\">(.*?)</div>").getMatch(0);
        if (filename == null) filename = br.getRegex("<title>upload24\\.net скачать (.*?)</title>").getMatch(0);
        if (filename == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        Regex otherInfo = br.getRegex("<div class=\"ss\">md5: +([a-z0-9]+)</div> +<br>[\t\n\r ]+\\[ <strong>([0-9\\.]+</strong>.{1,10}) \\]");
        String filesize = otherInfo.getMatch(1);
        String md5 = otherInfo.getMatch(0);
        link.setName(filename.trim());
        if (filesize != null) link.setDownloadSize(SizeFormatter.getSize(filesize.replace("</strong>", "")));
        if (md5 != null) link.setMD5Hash(md5);
        return AvailableStatus.TRUE;
    }

    @Override
    public void handleFree(DownloadLink downloadLink) throws Exception, PluginException {
        requestFileInformation(downloadLink);
        // Link is always changing
        String tempLink = getTempLink();
        if (tempLink == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        br.getPage(MAINPAGE + tempLink);
        tempLink = getTempLink();
        if (tempLink == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        br.getPage(MAINPAGE + tempLink);
        tempLink = getTempLink();
        if (tempLink == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        String waittime = br.getRegex("var mcount=(\\d+);").getMatch(0);
        int wait = 45;
        if (waittime != null) wait = Integer.parseInt(waittime);
        sleep(wait * 1001l, downloadLink);
        br.getPage(MAINPAGE + tempLink);
        tempLink = getTempLink();
        if (tempLink == null || !br.containsHTML("/capcha\\.php")) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        String code = getCaptchaCode(MAINPAGE + "/capcha.php", downloadLink);
        br.postPage(tempLink, "capcha=" + code);
        if (br.containsHTML("(без ограничений<|td>потоков</td>|высокая <|да <)")) throw new PluginException(LinkStatus.ERROR_CAPTCHA);
        String dllink = br.getRegex("\"(http://df\\d+\\.upload24\\.net/[a-z0-9]+/.*?)\"").getMatch(0);
        if (dllink == null) dllink = br.getRegex("\\[ <a href=\"(http://.*?)\"").getMatch(0);
        if (dllink == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, dllink, true, 1);
        if (dl.getConnection().getContentType().contains("html")) {
            br.followConnection();
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        dl.startDownload();
    }

    private String getTempLink() {
        String tlink = br.getRegex("<h2 class=\"pay\"><a href=\"(/.*?)\"").getMatch(0);
        if (tlink == null) tlink = br.getRegex("\"(/?act=dl\\d+_free\\&f=[a-z0-9]+)\"").getMatch(0);
        return tlink;
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