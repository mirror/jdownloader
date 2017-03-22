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
import jd.http.Browser;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "72dj.com" }, urls = { "https?://(?:www\\.)?72dj\\.com/(?:down|play)/\\d+\\.htm" })
public class SevenTwoDjCom extends PluginForHost {

    public SevenTwoDjCom(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public String getAGBLink() {
        return "http://72dj.com/";
    }

    public void correctDownloadLink(final DownloadLink link) {
        final String fid = this.getFID(link);
        link.setLinkID(fid);
        link.setUrlDownload(String.format("http://www.72dj.com/down/%s.htm", fid));
    }

    @Override
    public AvailableStatus requestFileInformation(final DownloadLink link) throws IOException, PluginException {
        this.setBrowserExclusive();
        br.setFollowRedirects(true);
        br.setCustomCharset("gbk");
        br.getPage(link.getDownloadURL());
        if (this.br.getHttpConnection().getResponseCode() == 404 || br.containsHTML("<TITLE>无法找到该页</TITLE>")) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        String filename = br.getRegex("<TITLE>下载湛江Dj小帅\\-([^<>\"]*?)</TITLE>").getMatch(0);
        if (filename == null) {
            filename = br.getRegex("<TITLE>([^<>\"]*?)</TITLE>").getMatch(0);
        }
        if (filename == null) {
            /* Fallback */
            filename = this.getFID(link);
        }
        link.setFinalFileName(Encoding.htmlDecode(filename.trim()) + ".mp3");
        return AvailableStatus.TRUE;
    }

    @Override
    public void handleFree(final DownloadLink downloadLink) throws Exception, PluginException {
        requestFileInformation(downloadLink);
        String dllink = br.getRegex("\"(http://[^<>\"]*?down_mp3[^<>\"]*?)\"").getMatch(0);
        if (dllink == null) {
            dllink = br.getRegex("var thunder_url= \"\"\\+durl\\+\"([^<>\"]*?)\"").getMatch(0);
            if (dllink != null) {
                /* Old way */
                br.setCookie("http://72dj.com", "uuauth", "ok");
                Browser brc = br.cloneBrowser();
                brc.getPage("http://data.72dj.com/getuuauthcode/");
                String code = brc.getRegex("UUAuthCode=\"(.*?)\"").getMatch(0);
                if (code != null) {
                    code = "?" + code;
                } else {
                    code = "";
                }
                dllink = "http://data.72dj.com/mp3/" + Encoding.htmlDecode(dllink) + code;
            } else {
                /* 2017-03-22: New */
                final String code = this.getCaptchaCode("http://www.72dj.com/d/imgcode.asp?" + System.currentTimeMillis(), downloadLink);
                br.getPage(String.format("/d/down_mp3url.asp?CheckCode=%s&id=%s", Encoding.urlEncode(code), this.getFID(downloadLink)));
                dllink = br.getRegex("\"(http[^<>\"\\']+type=down[^<>\"\\']+)\"").getMatch(0);
                if (dllink == null) {
                    if (this.br.containsHTML("window\\.alert\\(")) {
                        throw new PluginException(LinkStatus.ERROR_CAPTCHA);
                    }
                    throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                }
            }
        }
        dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, dllink, true, 0);
        if (dl.getConnection().getContentType().contains("html")) {
            br.followConnection();
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        dl.startDownload();
    }

    private String getFID(final DownloadLink dl) {
        return new Regex(dl.getDownloadURL(), "(\\d+)\\.htm$").getMatch(0);
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