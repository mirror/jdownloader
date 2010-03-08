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

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "shareua.com" }, urls = { "http://[\\w\\.]*?shareua.com/get_file/.*?/\\d+" }, flags = { 0 })
public class ShareUaCom extends PluginForHost {

    public ShareUaCom(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public String getAGBLink() {
        return "http://shareua.com/terms";
    }

    @Override
    public AvailableStatus requestFileInformation(DownloadLink link) throws IOException, PluginException {
        this.setBrowserExclusive();
        // Use English language
        br.getPage("http://shareua.com/lang/en");
        br.getPage(link.getDownloadURL());
        if (br.containsHTML("(<b>Invalid File|The file is removed by the originating user)")) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        String filename = br.getRegex("<title>ShareUA :(.*?)</title>").getMatch(0);
        if (filename == null) {
            filename = br.getRegex("File Name:</span>(.*?)</div>").getMatch(0);
            if (filename == null) filename = br.getRegex("You requested(.*?)\\(.*?\\)").getMatch(0);
        }
        String filesize = br.getRegex("You requested.*?\\(((\\d+\\.\\d+|\\d+) .*?)\\)").getMatch(0);
        if (filename == null) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        link.setName(filename.trim());
        if (filesize != null) link.setDownloadSize(Regex.getSize(filesize));
        return AvailableStatus.TRUE;
    }

    @Override
    public void handleFree(DownloadLink downloadLink) throws Exception, PluginException {
        requestFileInformation(downloadLink);
        String fileid = new Regex(br.getURL(), ".*?get_file/.*?/(\\d+)").getMatch(0);
        // fileid should never be null but if someone changes the plugin it
        // could be
        if (fileid == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        br.postPage(br.getURL(), "file_id=" + fileid);
        String captchaurl = "http://shareua.com/files/get/sec_code/" + fileid;
        for (int i = 0; i <= 5; i++) {
            if (!br.containsHTML("/files/get/sec_code")) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            String code = getCaptchaCode(captchaurl, downloadLink);
            br.postPage(br.getURL(), "sec_num=" + code);
            if (!br.containsHTML("/files/get/sec_code")) break;
            continue;
        }
        if (br.containsHTML("/files/get/sec_code")) throw new PluginException(LinkStatus.ERROR_CAPTCHA);
        String tokenPart = new Regex(br.getURL(), "get_file/(.+)").getMatch(0);
        if (tokenPart == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        String ttt = br.getRegex("var tAll =.*?(\\d+);").getMatch(0);
        br.getPage("http://shareua.com/files/get/token/" + tokenPart);
        String token = br.toString();
        if (token == null || !token.trim().matches("\\d+")) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        int tt = 60;
        if (ttt != null) {
            logger.info("Waittime detected, waiting " + ttt.trim() + " seconds from now on...");
            tt = Integer.parseInt(ttt);
        }
        sleep(tt * 1001, downloadLink);
        br.getPage("http://shareua.com/files/get/get_link/" + tokenPart + "/" + token.trim());
        String dllink = br.toString();
        if (!dllink.contains("http") && !dllink.contains("www.")) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        dllink = dllink.trim();
        dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, dllink, true, -2);
        // Errors should only happen if you try to download more than 4 files at
        // the same time!
        if (dl.getConnection().getContentType().contains("html")) {
            br.setCustomCharset("koi8-r");
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
        return 4;
    }

    @Override
    public void resetDownloadlink(DownloadLink link) {
    }

}