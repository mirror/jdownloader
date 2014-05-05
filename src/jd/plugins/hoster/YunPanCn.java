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
import java.net.UnknownHostException;

import jd.PluginWrapper;
import jd.config.Property;
import jd.http.Browser;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.Plugin;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "yunpan.cn" }, urls = { "http://(www\\.)?(([a-z0-9]+\\.[a-z0-9]+\\.)?yunpan\\.cn/lk/[A-Za-z0-9]+|yunpan\\.cn/[a-zA-Z0-9]{13})" }, flags = { 0 })
public class YunPanCn extends PluginForHost {

    private final String preDownloadPassword = "<input class=\"pwd-input\" type=\"password\">";

    public YunPanCn(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public String getAGBLink() {
        return "http://yunpan.360.cn/resource/html/agreement.html";
    }

    @Override
    public AvailableStatus requestFileInformation(final DownloadLink link) throws IOException, PluginException {
        this.setBrowserExclusive();
        br.setFollowRedirects(true);
        try {
            br.getPage(link.getDownloadURL());
        } catch (final UnknownHostException e) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        if (br.containsHTML(preDownloadPassword)) {
            // if the link was removed, it wouldn't have a password!
            link.getLinkStatus().setStatusText("This file requires pre-download password!");
            return AvailableStatus.TRUE;
        }
        if (br.containsHTML("befrherthtu567mut|id=\"linkError\"") || br.getURL().contains("?")) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        fileCheck(link);
        return AvailableStatus.TRUE;
    }

    private void fileCheck(final DownloadLink link) throws PluginException {
        final String filename = br.getRegex("name\\s*:\\s*'(.*?)',").getMatch(0);
        final String filesize = br.getRegex("size\\s*:\\s*'(\\d+)',").getMatch(0);
        if (filename == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        link.setName(Encoding.htmlDecode(filename.trim()));
        if (filesize != null) link.setDownloadSize(Long.parseLong(filesize));
    }

    @Override
    public void handleFree(final DownloadLink downloadLink) throws Exception, PluginException {
        requestFileInformation(downloadLink);
        if (br.containsHTML(preDownloadPassword)) {
            for (int i = 0; i != 3; i++) {
                String passCode = downloadLink.getStringProperty("pass", null);
                if (passCode == null || "".equals(passCode)) passCode = Plugin.getUserInput("Password?", downloadLink);
                if (passCode == null || "".equals(passCode)) {
                    logger.info("User has entered blank password, exiting handlePassword");
                    downloadLink.setProperty("pass", Property.NULL);
                    throw new PluginException(LinkStatus.ERROR_FATAL, "Password required!");
                }
                Browser br2 = br.cloneBrowser();
                br2.postPage(new Regex(br.getURL(), "https?://[^/]+").getMatch(-1) + "/share/verifyPassword", "shorturl=" + new Regex(br.getURL(), "/lk/([a-zA-Z0-9]+)$").getMatch(0) + "&linkpassword=" + Encoding.urlEncode(passCode));
                if (br2.containsHTML("\"errno\":0,")) {
                    downloadLink.setProperty("pass", passCode);
                    break;
                } else if (i + 1 == 3)
                    throw new PluginException(LinkStatus.ERROR_FATAL, "Exausted Password Tries!");
                else
                    downloadLink.setProperty("pass", Property.NULL);
            }
            br.getPage(br.getURL());
            fileCheck(downloadLink);
        }
        final Regex urlRegex = new Regex(br.getURL(), "http://(www\\.)?([a-z0-9]+\\.[a-z0-9]+)\\.yunpan\\.cn/lk/(.+)");
        final String nid = br.getRegex("nid : \\'(\\d+)\\',").getMatch(0);
        final String domainPart = urlRegex.getMatch(1);
        final String shortUrl = urlRegex.getMatch(2);
        if (nid == null || domainPart == null || shortUrl == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        br.getHeaders().put("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8");
        br.getHeaders().put("X-Requested-With", "XMLHttpRequest");
        br.postPage("http://" + domainPart + ".yunpan.cn/share/downloadfile/", "shorturl=" + shortUrl + "&nid=" + nid);
        String dllink = br.getRegex("\"downloadurl\":\"(http:[^<>\"]*?)\"").getMatch(0);
        if (dllink == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        dllink = dllink.replace("\\", "");
        dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, dllink, true, 0);
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
    public void resetDownloadlink(final DownloadLink link) {
    }

}