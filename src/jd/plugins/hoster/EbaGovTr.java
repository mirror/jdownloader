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
import jd.http.URLConnectionAdapter;
import jd.nutils.encoding.Encoding;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "eba.gov.tr" }, urls = { "http://(www\\.)?eba\\.gov\\.tr/(dergi/goster/\\d+|video/izle/[a-z0-9]+|ses/dinle/[a-z0-9]+|gorsel/bak/[a-z0-9]+)" }, flags = { 0 })
public class EbaGovTr extends PluginForHost {

    public EbaGovTr(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public String getAGBLink() {
        return "http://www.eba.gov.tr/";
    }

    private static final String FILELINK    = "http://(www\\.)?eba\\.gov\\.tr/dergi/goster/\\d+";
    private static final String VIDEOLINK   = "http://(www\\.)?eba\\.gov\\.tr/video/izle/[a-z0-9]+";
    private static final String AUDIOLINK   = "http://(www\\.)?eba\\.gov\\.tr/ses/dinle/[a-z0-9]+";
    private static final String PICTURELINK = "http://(www\\.)?eba\\.gov\\.tr/gorsel/bak/[a-z0-9]+";
    private String              DLLINK      = null;

    @Override
    public AvailableStatus requestFileInformation(final DownloadLink link) throws IOException, PluginException {
        this.setBrowserExclusive();
        br.setFollowRedirects(true);
        final String addedlink = link.getDownloadURL();
        br.getPage(addedlink);
        String filename = null;
        if (addedlink.matches(FILELINK)) {
            if (br.containsHTML(">Aradığınız Sayfa Bulunamadı<|>Bu sayfa kaldırılmış olabilir\\.<")) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            filename = br.getRegex("<h3>Bilim ve Teknik <small>([^<>\"]*?)</small></h3>").getMatch(0);
            DLLINK = br.getRegex("\"(/download\\.php\\?type=[^<>\"]*?)\"").getMatch(0);
            if (DLLINK != null) DLLINK = "http://www.eba.gov.tr" + DLLINK;
        } else if (addedlink.matches(VIDEOLINK)) {
            if (br.containsHTML(">Video Bulunamamıştır<")) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            filename = br.getRegex("<title>Video \\| ([^<>\"]*?)\\- Eğitim Bilişim Ağı</title>").getMatch(0);
            DLLINK = br.getRegex("assets/themes/base/images/download\\.png\" /><br><a href=\"(http://[^<>\"]*?)\"").getMatch(0);
        } else if (addedlink.matches(AUDIOLINK)) {
            if (br.containsHTML(">Ses Bulunamamıştır<")) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            filename = br.getRegex("<title>([^<>\"]*?)\\- Eğitim Bilişim Ağı</title>").getMatch(0);
            DLLINK = br.getRegex("assets/themes/base/images/download\\.png\" /><br><a href=\"(http://[^<>\"]*?)\"").getMatch(0);
        } else if (addedlink.matches(PICTURELINK)) {
            if (br.containsHTML(">Görsel Bulunamamıştır<")) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            filename = br.getRegex("class=\"active\">([^<>\"]*?)</li>").getMatch(0);
            DLLINK = br.getRegex("<div class=\"center\">[\t\n\r ]+<a title=\"[^<>\"/]+\" href=\"(http://[^<>\"]*?)\"").getMatch(0);
        }
        if (filename == null || DLLINK == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        DLLINK = Encoding.htmlDecode(DLLINK);
        final Browser br2 = br.cloneBrowser();
        // In case the link redirects to the finallink
        br2.setFollowRedirects(true);
        URLConnectionAdapter con = null;
        try {
            con = br2.openGetConnection(DLLINK);
            if (!con.getContentType().contains("html")) {
                link.setDownloadSize(con.getLongContentLength());
                link.setFinalFileName(Encoding.htmlDecode(filename.trim()) + DLLINK.substring(DLLINK.lastIndexOf(".")));
            } else {
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            }
            return AvailableStatus.TRUE;
        } finally {
            try {
                con.disconnect();
            } catch (Throwable e) {
            }
        }
    }

    @Override
    public void handleFree(final DownloadLink downloadLink) throws Exception, PluginException {
        requestFileInformation(downloadLink);
        dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, DLLINK, true, 0);
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