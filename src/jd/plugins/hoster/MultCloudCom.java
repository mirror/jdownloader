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
import jd.plugins.components.PluginJSonUtils;

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "multcloud.com" }, urls = { "https?://(www\\.)?multcloud\\.com/(?:download/[A-Z0-9\\-]+|action/share!downloadShare\\?.+)" })
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
    public AvailableStatus requestFileInformation(final DownloadLink link) throws Exception {
        if (link.getPluginPatternMatcher().contains("/action/share!downloadShare")) {
            // direct link
            final String filename = new Regex(link.getPluginPatternMatcher(), "drives\\.fileName=(.*?)&").getMatch(0);
            final String filesize = new Regex(link.getPluginPatternMatcher(), "drives\\.fileSize=(.*?)&").getMatch(0);
            link.setFinalFileName(Encoding.urlDecode(filename, false));
            link.setDownloadSize(SizeFormatter.getSize(filesize));
        } else {
            this.setBrowserExclusive();
            fid = new Regex(link.getDownloadURL(), "/download/(.+)").getMatch(0);
            br.setFollowRedirects(true);
            br.getPage(link.getDownloadURL());
            if (br.getHttpConnection().getResponseCode() == 404 || br.containsHTML("Sorry, the share has been canceled")) {
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            }
            br.getHeaders().put("X-Requested-With", "XMLHttpRequest");
            br.postPage("https://" + this.getHost() + "/action/share!getRootDirectory", "shareId=" + Encoding.urlEncode(fid).toLowerCase() + "&sharePwd=");
            if (br.getHttpConnection().getResponseCode() == 404 || br.toString().length() < 10) {
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            }
            filename = PluginJSonUtils.getJsonValue(br, "name");
            if (filename != null && !filename.equals("")) {
                link.setFinalFileName(filename);
            } else {
                link.setName(fid);
            }
        }
        return AvailableStatus.TRUE;
    }

    @Override
    public void handleFree(final DownloadLink downloadLink) throws Exception, PluginException {
        String dllink;
        if (downloadLink.getPluginPatternMatcher().contains("/action/share!downloadShare")) {
            dllink = downloadLink.getPluginPatternMatcher();
        } else {
            requestFileInformation(downloadLink);
            final String cloudType = PluginJSonUtils.getJsonValue(br, "cloudType");
            final String tokenId = PluginJSonUtils.getJsonValue(br, "tokenId");
            final String fileId = PluginJSonUtils.getJsonValue(br, "fileId");
            if (filename == null || cloudType == null || tokenId == null || fileId == null) {
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            dllink = "https://multcloud.com/action/share!downloadShare?drives.cloudType=" + Encoding.urlEncode(cloudType) + "&drives.tokenId=" + tokenId + "&drives.fileId=" + Encoding.urlEncode(fileId) + "&drives.fileName=" + Encoding.urlEncode(this.filename) + "&drives.fileSize=0&shareId=" + this.fid.toLowerCase();
        }
        dl = new jd.plugins.BrowserAdapter().openDownload(br, downloadLink, dllink, true, 0);
        if (dl.getConnection().getContentType().contains("html")) {
            br.followConnection();
            if (br.getHttpConnection().getResponseCode() == 404) {
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