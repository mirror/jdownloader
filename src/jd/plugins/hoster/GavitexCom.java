//    jDownloader - Downloadmanager
//    Copyright (C) 2016  JD-Team support@jdownloader.org
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

import jd.PluginWrapper;
import jd.http.Browser;
import jd.parser.Regex;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;
import jd.plugins.components.PluginJSonUtils;

import org.appwork.utils.StringUtils;

/**
 * This plugin supports gavitex.com, simple plugin for simple site!
 *
 * @author raztoki
 */
@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "gavitex.com" }, urls = { "https?://(?:www\\.)?gavitex\\.com/share/(?-i)([a-z0-9]{9})" })
public class GavitexCom extends PluginForHost {

    public GavitexCom(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public String getAGBLink() {
        return "http://gavitex.com";
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return -1;
    }

    private String[] dllink = null;

    @Override
    public AvailableStatus requestFileInformation(final DownloadLink downloadLink) throws Exception {
        this.setBrowserExclusive();
        br.getPage(downloadLink.getDownloadURL());
        final Browser ajax = br.cloneBrowser();
        ajax.getHeaders().put("Accept", "application/json, text/plain, */*");
        ajax.getPage("/api/sharedlinks/download?share_id=" + getFuid(downloadLink));
        // error handling
        if (StringUtils.equalsIgnoreCase(PluginJSonUtils.getJsonValue(ajax, "status"), "error")) {
            if (StringUtils.equalsIgnoreCase(PluginJSonUtils.getJsonValue(ajax, "error_code"), "210")) {
                // {"status":"Error","status_code":0,"error":"FileNotFound","error_code":210}
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            }
            // unknown error
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        final String filename = PluginJSonUtils.getJsonValue(ajax, "name");
        final String filesize = PluginJSonUtils.getJsonValue(ajax, "size");
        if (filename == null) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        downloadLink.setName(filename);
        if (filesize != null) {
            downloadLink.setDownloadSize(Long.parseLong(filesize));
        }
        dllink = PluginJSonUtils.getJsonResultsFromArray(PluginJSonUtils.getJsonArray(ajax, "download_urls"));
        return AvailableStatus.TRUE;
    }

    private String getFuid(final DownloadLink downloadLink) throws PluginException {
        final String fuid = new Regex(downloadLink.getDownloadURL(), this.getSupportedLinks()).getMatch(0);
        if (fuid == null) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        return fuid;
    }

    @Override
    public void handleFree(final DownloadLink downloadLink) throws Exception {
        requestFileInformation(downloadLink);
        if (this.dllink == null) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        final String dllink = this.dllink[0];
        dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, dllink, true, 0);
        if (!dl.getConnection().isContentDisposition()) {
            br.followConnection();
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        dl.startDownload();
    }

    @Override
    public void reset() {
    }

    @Override
    public void resetDownloadlink(DownloadLink link) {
    }

    @Override
    public void resetPluginGlobals() {
    }
}