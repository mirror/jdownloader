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
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;
import jd.plugins.components.PluginJSonUtils;

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "multcloud.com" }, urls = { "https?://(www\\.)?multcloud\\.com/download/[A-Z0-9\\-]+" }, flags = { 0 })
public class MultCloudCom extends PluginForHost {

    public MultCloudCom(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public String getAGBLink() {
        return "https://www.multcloud.com/terms";
    }

    public void correctDownloadLink(final DownloadLink link) {
        link.setUrlDownload(link.getDownloadURL().replace("http://", "https://"));
    }

    private String fid      = null;
    private String filename = null;

    @Override
    public AvailableStatus requestFileInformation(final DownloadLink link) throws IOException, PluginException {
        this.setBrowserExclusive();
        fid = new Regex(link.getDownloadURL(), "/download/(.+)").getMatch(0);
        br.setFollowRedirects(true);
        this.br.getPage(link.getDownloadURL());
        if (this.br.getHttpConnection().getResponseCode() == 404 || this.br.containsHTML("Sorry, the share has been canceled")) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        this.br.getHeaders().put("X-Requested-With", "XMLHttpRequest");
        this.br.postPage("https://" + this.getHost() + "/action/share!getRootDirectory", "shareId=" + Encoding.urlEncode(fid).toLowerCase() + "&sharePwd=");
        if (this.br.getHttpConnection().getResponseCode() == 404 || this.br.toString().length() < 10) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        filename = PluginJSonUtils.getJson(this.br, "name");
        if (filename != null && !filename.equals("")) {
            link.setFinalFileName(filename);
        } else {
            link.setName(fid);
        }
        return AvailableStatus.TRUE;
    }

    @Override
    public void handleFree(final DownloadLink downloadLink) throws Exception, PluginException {
        requestFileInformation(downloadLink);
        final String cloudType = PluginJSonUtils.getJson(this.br, "cloudType");
        final String tokenId = PluginJSonUtils.getJson(this.br, "tokenId");
        final String fileId = PluginJSonUtils.getJson(this.br, "fileId");
        if (filename == null || cloudType == null || tokenId == null || fileId == null) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        final String dllink = "https://multcloud.com/action/share!downloadShare?drives.cloudType=" + Encoding.urlEncode(cloudType) + "&drives.tokenId=" + tokenId + "&drives.fileId=" + Encoding.urlEncode(fileId) + "&drives.fileName=" + Encoding.urlEncode(this.filename) + "&drives.fileSize=0&shareId=" + this.fid.toLowerCase();
        dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, dllink, true, 0);
        if (dl.getConnection().getContentType().contains("html")) {
            br.followConnection();
            if (this.br.getHttpConnection().getResponseCode() == 404) {
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            }
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        downloadLink.setFinalFileName(Encoding.htmlDecode(getFileNameFromHeader(dl.getConnection())));
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