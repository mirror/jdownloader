//jDownloader - Downloadmanager
//Copyright (C) 2009  JD-Team support@jdownloader.org
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
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map.Entry;

import jd.PluginWrapper;
import jd.config.ConfigContainer;
import jd.config.ConfigEntry;
import jd.http.URLConnectionAdapter;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;

@HostPlugin(revision = "$Revision$", interfaceVersion = 3, names = { "rtbf.be" }, urls = { "http://rtbf\\.bedecrypted/\\d+" }) 
public class RtbfBe extends PluginForHost {

    public RtbfBe(PluginWrapper wrapper) {
        super(wrapper);
        setConfigElements();
    }

    @Override
    public String getAGBLink() {
        return "http://www.rtbf.be/";
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return -1;
    }

    /** Settings stuff */
    private static final String                   FAST_LINKCHECK = "FAST_LINKCHECK";

    public static LinkedHashMap<String, String[]> formats        = new LinkedHashMap<String, String[]>(new LinkedHashMap<String, String[]>() {
        {
            /*
             * Format-name:videoCodec, videoBitrate, videoResolution,
             * audioCodec, audioBitrate
             */
            /*
             * We could also add audio- and videobitrate but from my tests they
             * looked quite variable so we prefer not to use wrong information
             * here as it will be used for the filenames later too!
             */
            put("download", new String[] { "AVC", null, "???x???", "AAC LC", null });
            put("high", new String[] { "AVC", "800 kbps", "720x400", "AAC LC", "128 kbps" });
            put("medium", new String[] { "AVC", "400 kbps", "640x360", "AAC LC", "77 kbps" });
            put("low", new String[] { "AVC", "200 kbps", "400x200", "AAC LC", "77 kbps" });
        }
    });

    private String                                dllink         = null;

    @Override
    public AvailableStatus requestFileInformation(final DownloadLink downloadLink) throws IOException, PluginException {
        this.setBrowserExclusive();
        this.br.setFollowRedirects(true);
        dllink = downloadLink.getStringProperty("directlink", null);
        final String filename = downloadLink.getStringProperty("directfilename", null);

        if (filename == null || dllink == null) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }

        downloadLink.setFinalFileName(filename);
        URLConnectionAdapter con = null;
        try {
            con = br.openHeadConnection(dllink);
            if (!con.getContentType().contains("html")) {
                downloadLink.setDownloadSize(con.getLongContentLength());
            } else {
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            }
        } finally {
            try {
                con.disconnect();
            } catch (Throwable e) {
            }
        }
        return AvailableStatus.TRUE;
    }

    @Override
    public void handleFree(final DownloadLink downloadLink) throws Exception {
        requestFileInformation(downloadLink);
        dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, dllink, true, 0);
        if (dl.getConnection().getContentType().contains("html")) {
            logger.warning("The final dllink seems not to be a file!");
            br.followConnection();
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        dl.startDownload();
    }

    public boolean allowHandle(final DownloadLink downloadLink, final PluginForHost plugin) {
        return downloadLink.getHost().equalsIgnoreCase(plugin.getHost());
    }

    @Override
    public String getDescription() {
        return "JDownloader's rtbf.be plugin helps downloading videoclips from rtbf.be.";
    }

    private void setConfigElements() {
        getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, getPluginConfig(), FAST_LINKCHECK, "Enable fast linkcheck?\r\nNOTE: If enabled, links will appear faster but filesize won't be shown before downloadstart.").setDefaultValue(false));
        final Iterator<Entry<String, String[]>> it = formats.entrySet().iterator();
        while (it.hasNext()) {
            /*
             * Format-name:videoCodec, videoBitrate, videoResolution, audioCodec, audioBitrate
             */
            String usertext = "Load ";
            final Entry<String, String[]> videntry = it.next();
            final String internalname = videntry.getKey();
            final String[] vidinfo = videntry.getValue();
            final String videoCodec = vidinfo[0];
            final String videoBitrate = vidinfo[1];
            final String videoResolution = vidinfo[2];
            final String audioCodec = vidinfo[3];
            final String audioBitrate = vidinfo[4];
            if (videoCodec != null) {
                usertext += videoCodec + " ";
            }
            if (videoBitrate != null) {
                usertext += videoBitrate + " ";
            }
            if (videoResolution != null) {
                usertext += videoResolution + " ";
            }
            if (audioCodec != null || audioBitrate != null) {
                usertext += "with audio ";
                if (audioCodec != null) {
                    usertext += audioCodec + " ";
                }
                if (audioBitrate != null) {
                    usertext += audioBitrate;
                }
            }
            if (usertext.endsWith(" ")) {
                usertext = usertext.substring(0, usertext.lastIndexOf(" "));
            }
            final ConfigEntry vidcfg = new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, getPluginConfig(), "ALLOW_" + internalname, usertext).setDefaultValue(true);
            getConfig().addEntry(vidcfg);
        }
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