//jDownloader - Downloadmanager
//Copyright (C) 2013  JD-Team support@jdownloader.org
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
import jd.config.Property;
import jd.http.Browser;
import jd.http.URLConnectionAdapter;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;
import jd.utils.locale.JDL;

import org.appwork.utils.formatter.SizeFormatter;

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "datacloud.to" }, urls = { "http://(www\\.)?(datacloud|mediacloud)\\.to/download/[a-z0-9]+/.{1}" }, flags = { 0 })
public class DataCloudTo extends PluginForHost {

    public DataCloudTo(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public String getAGBLink() {
        return "http://datacloud.to/page/terms";
    }

    public void correctDownloadLink(DownloadLink link) {
        link.setUrlDownload(link.getDownloadURL().replace("mediacloud.to/", "datacloud.to/").replace("http://www.", "http://"));
    }

    private static final String NOCHUNKS = "NOCHUNKS";
    private static final String NORESUME = "NORESUME";

    @Override
    public AvailableStatus requestFileInformation(final DownloadLink link) throws IOException, PluginException {
        this.setBrowserExclusive();
        br.setFollowRedirects(true);
        br.getPage(link.getDownloadURL());
        if (br.containsHTML(">File does not exist")) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        String filename = br.getRegex("<h1>Download <b>([^<>\"]*?)</b>").getMatch(0);
        if (filename == null) filename = br.getRegex("<title>Download ([^<>\"]*?)\\- datacloud\\.to</title>").getMatch(0);
        String filesize = br.getRegex("<span>Size:</span>([^<>\"]*?)</div>").getMatch(0);
        if (filename == null || filesize == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        link.setName(Encoding.htmlDecode(filename.trim()));
        link.setDownloadSize(SizeFormatter.getSize(filesize));
        return AvailableStatus.TRUE;
    }

    @Override
    public void handleFree(final DownloadLink downloadLink) throws Exception, PluginException {
        requestFileInformation(downloadLink);
        String dllink = checkDirectLink(downloadLink, "directlink");
        if (dllink == null) {
            final String fid = new Regex(downloadLink.getDownloadURL(), "([a-z0-9]+)/.{1}$").getMatch(0);
            final String captchaLink = br.getRegex("\"(/process/imageverification\\?\\d+)\"").getMatch(0);
            if (captchaLink == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            br.getHeaders().put("X-Requested-With", "XMLHttpRequest");
            final String code = getCaptchaCode("http://datacloud.to" + captchaLink, downloadLink);
            br.postPage("http://datacloud.to/process", "action=checkCapchaDownload&captcha=" + Encoding.urlEncode(code) + "&link=" + fid);
            if (br.containsHTML("\"Wrong captcha\"")) throw new PluginException(LinkStatus.ERROR_CAPTCHA);
            br.getPage(downloadLink.getDownloadURL() + "/c");
            br.getPage(downloadLink.getDownloadURL() + "/r");
            dllink = br.getRegex("\"(http://(mediacloud|datacloud)\\.to/get/[^<>\"]*?)\"").getMatch(0);
            if (dllink == null) dllink = br.getRegex("<a class=\"csbutton yellow\" href=\"(http://[^<>\"]*?)\"").getMatch(0);
            // Streamlinks (playerlinks)
            if (dllink == null) {
                dllink = br.getRegex("file: \"(http://[^<>\"]*?)\"").getMatch(0);
                if (dllink == null) {
                    dllink = br.getRegex("\"(http://[a-z0-9]+\\.datacloud\\.to/uploads/files\\d+/[^<>\"]*?)\"").getMatch(0);
                }
            }
            if (dllink == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }

        boolean resume = true;
        int chunks = -5;
        if (downloadLink.getBooleanProperty(DataCloudTo.NOCHUNKS, false) || resume == false) {
            chunks = 1;
        }

        dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, dllink, resume, chunks);
        if (dl.getConnection().getContentType().contains("html")) {
            br.followConnection();
            if (br.containsHTML(">404 Not Found<")) throw new PluginException(LinkStatus.ERROR_FATAL, "Hoster issue: final link no longer present on server");
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        downloadLink.setProperty("directlink", dllink);
        if (!this.dl.startDownload()) {
            try {
                if (dl.externalDownloadStop()) return;
            } catch (final Throwable e) {
            }
            if (downloadLink.getLinkStatus().getErrorMessage() != null && downloadLink.getLinkStatus().getErrorMessage().startsWith(JDL.L("download.error.message.rangeheaders", "Server does not support chunkload"))) {
                if (downloadLink.getBooleanProperty(DataCloudTo.NORESUME, false) == false) {
                    downloadLink.setChunksProgress(null);
                    downloadLink.setProperty(DataCloudTo.NORESUME, Boolean.valueOf(true));
                    throw new PluginException(LinkStatus.ERROR_RETRY);
                }
            } else {
                /* unknown error, we disable multiple chunks */
                if (downloadLink.getBooleanProperty(DataCloudTo.NOCHUNKS, false) == false) {
                    downloadLink.setProperty(DataCloudTo.NOCHUNKS, Boolean.valueOf(true));
                    throw new PluginException(LinkStatus.ERROR_RETRY);
                }
            }
        }
    }

    private String checkDirectLink(final DownloadLink downloadLink, final String property) {
        String dllink = downloadLink.getStringProperty(property);
        if (dllink != null) {
            try {
                Browser br2 = br.cloneBrowser();
                URLConnectionAdapter con = br2.openGetConnection(dllink);
                if (con.getContentType().contains("html") || con.getLongContentLength() == -1) {
                    downloadLink.setProperty(property, Property.NULL);
                    dllink = null;
                }
                con.disconnect();
            } catch (Exception e) {
                downloadLink.setProperty(property, Property.NULL);
                dllink = null;
            }
        }
        return dllink;
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