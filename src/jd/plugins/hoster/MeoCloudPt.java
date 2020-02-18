//jDownloader - Downloadmanager
//Copyright (C) 2012  JD-Team support@jdownloader.org
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
import jd.parser.html.Form;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "meocloud.pt" }, urls = { "https?://(?:www\\.)?meocloud\\.pt/link/([a-z0-9\\-]+/[^<>\"]+)" })
public class MeoCloudPt extends PluginForHost {
    public MeoCloudPt(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public String getAGBLink() {
        return "https://meocloud.pt/";
    }

    @Override
    public AvailableStatus requestFileInformation(final DownloadLink link) throws IOException, PluginException {
        this.setBrowserExclusive();
        br.setFollowRedirects(true);
        br.getPage(link.getPluginPatternMatcher());
        if (br.getHttpConnection().getResponseCode() == 404 || br.containsHTML("class=\"error type404\"|class=\"no_link_available\"")) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        String filename = br.getRegex("/dl/zipdir/[a-z0-9\\-]+/.*?/([^<>\"/]*?)\\?(public|download)=").getMatch(0);
        if (filename == null) {
            filename = br.getRegex("class=\"pick_file\" value=\"/([^<>\"]*?)\">").getMatch(0);
        }
        if (filename == null) {
            /* Fallback */
            filename = new Regex(link.getPluginPatternMatcher(), this.getSupportedLinks()).getMatch(0);
        }
        link.setName(Encoding.htmlDecode(filename.trim()));
        return AvailableStatus.TRUE;
    }

    @Override
    public void handleFree(final DownloadLink downloadLink) throws Exception, PluginException {
        requestFileInformation(downloadLink);
        final Form pwform = br.getFormbyKey("passwd");
        if (pwform != null) {
            /* 2020-02-18: PW protected URLs are not yet supported */
            throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "PW protected: Contact support and ask for implementation", 8 * 60 * 1000l);
        }
        String dllink = br.getRegex("\"(https?://[a-z0-9\\.]+/dl/[^<>\"]*?)\"").getMatch(0);
        if (dllink == null) {
            final String publ = new Regex(downloadLink.getDownloadURL(), "meocloud\\.pt/link/([a-z0-9\\-]+)/").getMatch(0);
            dllink = "https://cld.pt/dl/download/" + publ + "/" + Encoding.urlEncode(downloadLink.getName()) + "?public=" + publ + "&download=true";
        }
        // if (dllink == null) throw new
        // PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
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