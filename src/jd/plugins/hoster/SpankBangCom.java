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
import java.util.ArrayList;

import jd.PluginWrapper;
import jd.config.ConfigContainer;
import jd.config.ConfigEntry;
import jd.http.Browser;
import jd.http.Browser.BrowserException;
import jd.http.URLConnectionAdapter;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.FilePackage;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;
import jd.utils.locale.JDL;

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "spankbang.com" }, urls = { "http://spankbangdecrypted\\.com/\\d+" }, flags = { 2 })
public class SpankBangCom extends PluginForHost {

    public SpankBangCom(PluginWrapper wrapper) {
        super(wrapper);
        this.setConfigElements();
    }

    @Override
    public String getAGBLink() {
        return "http://spankbang.com/info#dmca";
    }

    /** Settings stuff */
    private static String FASTLINKCHECK = "FASTLINKCHECK";
    private static String ALLOW_BEST    = "ALLOW_BEST";
    private static String ALLOW_240p    = "ALLOW_240p";
    private static String ALLOW_480p    = "ALLOW_480p";
    private static String ALLOW_720p    = "ALLOW_720p";

    private String        DLLINK        = null;

    @Override
    public ArrayList<DownloadLink> getDownloadLinks(String data, FilePackage fp) {
        ArrayList<DownloadLink> ret = super.getDownloadLinks(data, fp);

        return ret;
    }

    @Override
    public AvailableStatus requestFileInformation(final DownloadLink link) throws IOException, PluginException {
        /* Old links before the decrypter --> hosterplugin + qualityselection change! */
        if (!link.getDownloadURL().matches("http://spankbangdecrypted\\.com/\\d+")) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        this.setBrowserExclusive();
        br.setFollowRedirects(true);
        br.getHeaders().put("Accept-Language", "en-US,en;q=0.5");
        br.getPage(link.getStringProperty("mainlink", null));
        if ("http://spankbang.com/".equals(br.getURL()) || br.containsHTML(">this video is no longer available\\.<")) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        final String filename = link.getStringProperty("plain_filename", null);
        DLLINK = link.getStringProperty("plain_directlink", null);
        if (filename == null || DLLINK == null) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        link.setFinalFileName(filename);

        final Browser br2 = br.cloneBrowser();
        // In case the link redirects to the finallink
        br2.setFollowRedirects(true);
        URLConnectionAdapter con = null;
        try {
            try {
                con = br2.openGetConnection(DLLINK);
            } catch (final BrowserException e) {
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            }
            if (!con.getContentType().contains("html")) {
                link.setDownloadSize(con.getLongContentLength());
            } else {
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            }
            return AvailableStatus.TRUE;
        } finally {
            try {
                con.disconnect();
            } catch (final Throwable e) {
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
    public String getDescription() {
        return "JDownloader's SpankBang Plugin helps downloading Videoclips from spankbang.com. SpankBang provides different video qualities.";
    }

    private void setConfigElements() {
        getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, getPluginConfig(), FASTLINKCHECK, JDL.L("plugins.hoster.SpankBangCom.fastLinkcheck", "Fast linkcheck (filesize won't be shown in linkgrabber)?")).setDefaultValue(true));
        getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_SEPARATOR));
        final ConfigEntry cfg = new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, getPluginConfig(), ALLOW_BEST, JDL.L("plugins.hoster.SpankBangCom.ALLOW_BEST", "Always only grab best available resolution?")).setDefaultValue(true);
        getConfig().addEntry(cfg);
        getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, getPluginConfig(), ALLOW_240p, JDL.L("plugins.hoster.SpankBangCom.ALLOW_240p", "Grab 240p?")).setDefaultValue(true).setEnabledCondidtion(cfg, false));
        getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, getPluginConfig(), ALLOW_480p, JDL.L("plugins.hoster.SpankBangCom.ALLOW_480p", "Grab 480p?")).setDefaultValue(true).setEnabledCondidtion(cfg, false));
        getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, getPluginConfig(), ALLOW_720p, JDL.L("plugins.hoster.SpankBangCom.ALLOW_720p", "Grab 720p?")).setDefaultValue(true).setEnabledCondidtion(cfg, false));
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