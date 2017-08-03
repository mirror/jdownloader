//    jDownloader - Downloadmanager
//    Copyright (C) 2013  JD-Team support@jdownloader.org
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

import org.appwork.utils.formatter.SizeFormatter;

import jd.PluginWrapper;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "mkxk.com" }, urls = { "http://(www\\.)?mkxk\\.com/(?:play|down)/\\d+\\.html" })
public class MkxkCom extends PluginForHost {
    public MkxkCom(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public String getAGBLink() {
        return "http://www.mkxk.com/";
    }

    @SuppressWarnings("deprecation")
    public void correctDownloadLink(final DownloadLink link) {
        link.setUrlDownload(link.getDownloadURL().replace("/down/", "/play/"));
    }

    /** 2017-08-03: Downloads (without logging in) seem to be impossible. */
    @SuppressWarnings("deprecation")
    @Override
    public AvailableStatus requestFileInformation(final DownloadLink link) throws IOException, PluginException {
        this.setBrowserExclusive();
        br.setFollowRedirects(true);
        br.setCustomCharset("UTF-8");
        br.getPage(link.getDownloadURL());
        if (br.containsHTML(">We\\'re sorry but the page your are looking for is Not|>window\\.setTimeout") || br.getHttpConnection().getResponseCode() == 404) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        String filename = br.getRegex("class=\"jp\\-title split\">([^<>\"]*?)</span>").getMatch(0);
        if (filename == null) {
            filename = br.getRegex("title:\"([^<>\"]*?)\"").getMatch(0);
        }
        if (filename == null) {
            filename = br.getRegex("class=\"aleft\"><a href=\"#\">([^<>\"]*?)</a>").getMatch(0);
        }
        if (filename == null) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        if (br.containsHTML("\\.mp3\"")) {
            link.setFinalFileName(Encoding.htmlDecode(filename.trim()) + ".mp3");
        } else {
            link.setFinalFileName(Encoding.htmlDecode(filename.trim()) + ".m4a");
        }
        final String filesize = br.getRegex(">大小：([^<>\"]*?)</li>").getMatch(0);
        if (filesize != null) {
            link.setDownloadSize(SizeFormatter.getSize(filesize));
        }
        return AvailableStatus.TRUE;
    }

    @SuppressWarnings({ "deprecation" })
    @Override
    public void handleFree(final DownloadLink downloadLink) throws Exception, PluginException {
        String m4a = null;
        final String fuid = new Regex(downloadLink.getDownloadURL(), "(\\d+)\\.html$").getMatch(0);
        requestFileInformation(downloadLink);
        final String special_tudou_id = br.getRegex(">mplayer\\(\"(\\d+)\"\\)").getMatch(0);
        if (special_tudou_id != null) {
            m4a = "http://vr.tudou.com/v2proxy/v2?it=" + special_tudou_id;
        } else {
            // br.getPage("/index.php/url/f/" + fuid);
            String pt1 = null;
            final String pt2 = br.getRegex("m4a: furl\\+\"([^<>\"]*?)\"").getMatch(0);
            if (pt2 == null) {
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            br.getPage("/index.php/url/f/" + fuid);
            pt1 = br.getRegex("var furl=\"(http://[^<>\"/]*?/)\";").getMatch(0);
            if (pt1 == null) {
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            m4a = pt1 + pt2;
        }
        dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, m4a, true, 0);
        if (dl.getConnection().getContentType().contains("html")) {
            if (dl.getConnection().getResponseCode() == 404) {
                throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server error 404", 30 * 60 * 1000l);
            }
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