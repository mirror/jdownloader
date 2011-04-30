//    jDownloader - Downloadmanager
//    Copyright (C) 2009  JD-Team support@jdownloader.org
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

import jd.PluginWrapper;
import jd.config.ConfigContainer;
import jd.config.ConfigEntry;
import jd.http.URLConnectionAdapter;
import jd.plugins.DownloadLink;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.utils.locale.JDL;

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "tube8.com" }, urls = { "http://[\\w\\.]*?tube8\\.com/.*?/.*?/[0-9]+" }, flags = { 2 })
public class Tube8Com extends PluginForHost {

    public Tube8Com(PluginWrapper wrapper) {
        super(wrapper);
        setConfigElements();
    }

    private static final String mobile = "mobile";

    private void setConfigElements() {
        ConfigEntry cond = new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, getPluginConfig(), mobile, JDL.L("plugins.hoster.Tube8Com.setting.preferVideosForMobilePhones", "Prefer videos for mobile phones (3gp format)")).setDefaultValue(false);
        config.addEntry(cond);
    }

    public String dllink = null;

    @Override
    public String getAGBLink() {
        return "http://www.tube8.com/info.html#dmca";
    }

    @Override
    public AvailableStatus requestFileInformation(DownloadLink downloadLink) throws IOException, PluginException {
        this.setBrowserExclusive();
        br.setFollowRedirects(false);
        br.getPage(downloadLink.getDownloadURL());
        if (br.getRedirectLocation() != null) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        String verifyAge = br.getRegex("(<div class=\"enter-btn\">)").getMatch(0);
        if (verifyAge != null) {
            br.postPage(downloadLink.getDownloadURL(), "processdisclaimer=");
        }
        String filename = br.getRegex("<div class=\"title-wrapper relative\">.*?<h1 class=\"main-title main-sprite-img\">(.*?)</h1>").getMatch(0);
        if (filename == null) {
            filename = br.getRegex("<title>(.*?) - ").getMatch(0);
            if (filename == null) {
                filename = br.getRegex("Tube8 bring you(.*?)for all").getMatch(0);
            }
        }
        boolean prefer3gp = getPluginConfig().getBooleanProperty(mobile, false);
        if (prefer3gp) {
            find3gpLinks();
            if (dllink == null) {
                logger.warning("Couldn't find 3gp links even if they're prefered, trying to get the normal links now!");
                findNormalLinks();
            }
        } else {
            findNormalLinks();
            if (dllink == null) {
                logger.warning("Couldn't find normal links even if they're prefered, trying to get the 3gp links now!");
                find3gpLinks();
            }
        }
        if (filename == null || dllink == null) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        filename = filename.trim();
        if (!dllink.contains(".3gp")) {
            downloadLink.setFinalFileName(filename + ".flv");
        } else {
            downloadLink.setFinalFileName((filename + ".3gp"));
        }
        URLConnectionAdapter con = null;
        try {
            con = br.openGetConnection(dllink);
            if (!con.getContentType().contains("html"))
                downloadLink.setDownloadSize(con.getLongContentLength());
            else
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            return AvailableStatus.TRUE;
        } finally {
            try {
                con.disconnect();
            } catch (final Throwable e) {
            }
        }
    }

    @Override
    public void handleFree(DownloadLink downloadLink) throws Exception {
        requestFileInformation(downloadLink);
        dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, dllink, true, 0);
        if (dl.getConnection().getContentType().contains("html")) {
            br.followConnection();
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        dl.startDownload();
    }

    public void findNormalLinks() throws NumberFormatException, PluginException {
        dllink = br.getRegex("var videourl=\"(http.*?)\"").getMatch(0);
        if (dllink == null) dllink = br.getRegex("\"(http://media[0-9]+\\.tube8\\.com/flv/.*?\\.flv)\"").getMatch(0);
    }

    public void find3gpLinks() throws NumberFormatException, PluginException {
        dllink = br.getRegex("\"(http://mobile[0-9]+\\.tube8\\.com/flv/.*?/.*?\\.3gp)\"").getMatch(0);
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return -1;
    }

    @Override
    public void reset() {
    }

    @Override
    public void resetPluginGlobals() {
    }

    @Override
    public void resetDownloadlink(DownloadLink link) {
    }
}
