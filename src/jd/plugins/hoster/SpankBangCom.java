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

import java.util.LinkedHashMap;

import jd.PluginWrapper;
import jd.config.ConfigContainer;
import jd.config.ConfigEntry;
import jd.http.Browser;
import jd.http.URLConnectionAdapter;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.components.UserAgents.BrowserName;
import jd.utils.locale.JDL;

import org.jdownloader.plugins.components.antiDDoSForHost;

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "spankbang.com" }, urls = { "http://spankbangdecrypted\\.com/\\d+" })
public class SpankBangCom extends antiDDoSForHost {
    public SpankBangCom(PluginWrapper wrapper) {
        super(wrapper);
        this.setConfigElements();
    }

    @Override
    public String getAGBLink() {
        return "http://spankbang.com/info#dmca";
    }

    /** Settings stuff */
    private final static String FASTLINKCHECK = "FASTLINKCHECK";
    private final static String ALLOW_BEST    = "ALLOW_BEST";
    private final static String ALLOW_240p    = "ALLOW_240p";
    private final static String ALLOW_320p    = "ALLOW_320p";
    private final static String ALLOW_480p    = "ALLOW_480p";
    private final static String ALLOW_720p    = "ALLOW_720p";
    private final static String ALLOW_1080p   = "ALLOW_1080p";
    private String              dllink        = null;
    private boolean             server_issues = false;

    @Override
    protected boolean useRUA() {
        return true;
    }

    @Override
    protected BrowserName setBrowserName() {
        return BrowserName.Chrome;
    }

    @Override
    public AvailableStatus requestFileInformation(final DownloadLink link) throws Exception {
        this.setBrowserExclusive();
        server_issues = false;
        br.setFollowRedirects(true);
        br.getHeaders().put("Accept-Language", "en-US,en;q=0.5");
        final String filename = link.getStringProperty("plain_filename", null);
        dllink = link.getStringProperty("plain_directlink", null);
        if (filename == null) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        link.setFinalFileName(filename);
        if (dllink != null) {
            URLConnectionAdapter con = null;
            try {
                // this request isn't behind cloudflare.
                con = br.openHeadConnection(dllink);
                if (!con.getContentType().contains("html")) {
                    link.setDownloadSize(con.getLongContentLength());
                    return AvailableStatus.TRUE;
                } else {
                    final String mainlink = link.getStringProperty("mainlink", null);
                    final String quality = link.getStringProperty("quality", null);
                    if (mainlink == null || quality == null) {
                        /* Missing property - this should not happen! */
                        throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
                    }
                    getPage(mainlink);
                    if (jd.plugins.decrypter.SpankBangCom.isOffline(this.br)) {
                        /* Main videolink offline --> Offline */
                        throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
                    }
                    /* Main videolink online --> Refresh directlink ... */
                    final LinkedHashMap<String, String> foundQualities = jd.plugins.decrypter.SpankBangCom.findQualities(this.br, mainlink);
                    if (foundQualities != null) {
                        dllink = foundQualities.get(quality);
                    }
                    if (dllink != null) {
                        con = br.openHeadConnection(dllink);
                        if (!con.getContentType().contains("html")) {
                            link.setDownloadSize(con.getLongContentLength());
                            /* Save new directlink */
                            link.setProperty("plain_directlink", dllink);
                            return AvailableStatus.TRUE;
                        } else {
                            /* Link is still online but our directlink does not work for whatever reason ... */
                            server_issues = true;
                        }
                    }
                    link.setDownloadSize(con.getLongContentLength());
                }
            } finally {
                try {
                    con.disconnect();
                } catch (final Throwable e) {
                }
            }
        }
        return AvailableStatus.TRUE;
    }

    @Override
    public void getPage(String page) throws Exception {
        super.getPage(page);
    }

    @Override
    public void setBrowser(Browser brr) {
        super.setBrowser(brr);
    }

    @Override
    public void handleFree(final DownloadLink downloadLink) throws Exception, PluginException {
        requestFileInformation(downloadLink);
        if (server_issues) {
            throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Unknown server error", 10 * 60 * 1000l);
        } else if (dllink == null) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, dllink, true, 0);
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
        getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, getPluginConfig(), ALLOW_240p, JDL.L("plugins.hoster.SpankBangCom.ALLOW_240p", "Grab 240p?")).setDefaultValue(true));
        getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, getPluginConfig(), ALLOW_320p, JDL.L("plugins.hoster.SpankBangCom.ALLOW_320p", "Grab 320p?")).setDefaultValue(true));
        getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, getPluginConfig(), ALLOW_480p, JDL.L("plugins.hoster.SpankBangCom.ALLOW_480p", "Grab 480p?")).setDefaultValue(true));
        getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, getPluginConfig(), ALLOW_720p, JDL.L("plugins.hoster.SpankBangCom.ALLOW_720p", "Grab 720p?")).setDefaultValue(true));
        getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, getPluginConfig(), ALLOW_1080p, JDL.L("plugins.hoster.SpankBangCom.ALLOW_1080p", "Grab 1080p?")).setDefaultValue(true));
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