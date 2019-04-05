//jDownloader - Downloadmanager
//Copyright (C) 2011  JD-Team support@jdownloader.org
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

import org.appwork.utils.formatter.SizeFormatter;

import jd.PluginWrapper;
import jd.http.URLConnectionAdapter;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "sugarsync.com" }, urls = { "https?://(www\\.)?sugarsync\\.com/pf/D[\\d\\_]+" })
public class SugarSyncCom extends PluginForHost {
    public SugarSyncCom(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public String getAGBLink() {
        return "https://www.sugarsync.com/terms.html";
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return 10;
    }

    private String DLLINK = null;

    @SuppressWarnings("deprecation")
    @Override
    public AvailableStatus requestFileInformation(final DownloadLink link) throws IOException, PluginException {
        DLLINK = null;
        this.setBrowserExclusive();
        br.setFollowRedirects(true);
        br.setCookie("https://www.sugarsync.com/", "lang", "en");
        URLConnectionAdapter con = null;
        try {
            con = br.openGetConnection(link.getDownloadURL());
            if (con.getContentType().contains("html")) {
                br.followConnection();
            } else {
                /* We have a directlink */
                DLLINK = link.getDownloadURL();
                link.setDownloadSize(con.getLongContentLength());
                link.setFinalFileName(Encoding.htmlDecode(getFileNameFromHeader(con)));
                return AvailableStatus.TRUE;
            }
        } finally {
            try {
                con.disconnect();
            } catch (final Throwable e) {
            }
        }
        br.getPage(link.getDownloadURL());
        if (br.containsHTML("class=\"pf-down-unshared-main-message pf-down-unshared-unavailable-file-message\"")) {
            // || br.containsHTML("class=\"pf-error-icon\"")) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        String filename = br.getRegex("<span class=\"displayFileName\" title=\"(.*?)\"></span>").getMatch(0);
        if (filename == null) {
            filename = br.getRegex("class=\"displayFileName\">([^<>\"]*?)<").getMatch(0);
        }
        if (filename == null) {
            filename = br.getRegex("name : \\'(.*?)\\'\\,").getMatch(0);
        }
        if (filename == null) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        String filesize = br.getRegex("<span class=\"fileSize\">\\((.*?)\\)</span>").getMatch(0);
        if (filesize == null) {
            filesize = br.getRegex("class=\"pf-down-file-size\">\\(([^<>\"]*?)\\)</span>").getMatch(0);
        }
        if (filesize == null) {
            filesize = br.getRegex("size : \\'(.*?)\\'\\,").getMatch(0);
        }
        link.setName(filename.trim());
        link.setDownloadSize(SizeFormatter.getSize(filesize));
        return AvailableStatus.TRUE;
    }

    @Override
    public void handleFree(DownloadLink downloadLink) throws Exception, PluginException {
        requestFileInformation(downloadLink);
        if (DLLINK == null) {
            String uid = new Regex(br.getURL(), "sugarsync\\.com/pf/(D[\\d\\_]+)").getMatch(0);
            DLLINK = "https://www.sugarsync.com/pf/" + uid + "?directDownload=true";
        }
        dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, DLLINK, true, -10);
        if (dl.getConnection().getContentType().contains("html")) {
            br.followConnection();
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        downloadLink.setProperty("freelink", DLLINK);
        dl.startDownload();
    }

    @Override
    public void reset() {
    }

    @Override
    public void resetDownloadlink(DownloadLink link) {
    }
}