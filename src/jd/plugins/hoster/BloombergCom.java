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

import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map.Entry;

import jd.PluginWrapper;
import jd.config.ConfigContainer;
import jd.config.ConfigEntry;
import jd.http.Browser.BrowserException;
import jd.http.URLConnectionAdapter;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;
import jd.utils.locale.JDL;

@HostPlugin(revision = "$Revision$", interfaceVersion = 3, names = { "bloomberg.com" }, urls = { "http://bloomberg\\.comdecrypted\\d+" }) 
public class BloombergCom extends PluginForHost {

    /** Settings stuff */
    private static final String                   FAST_LINKCHECK = "FAST_LINKCHECK";

    public static LinkedHashMap<String, String[]> formats        = new LinkedHashMap<String, String[]>(new LinkedHashMap<String, String[]>() {
        {
            /*
             * Format-name:videoCodec, videoBitrate, videoResolution,
             * audioCodec, audioBitrate
             */
            put("h264_320x180_150_aac_16", new String[] { "MPEG-4 Visual", "150", "320x180", "AAC LC", "45,6" });
            put("h264_240x140_150_aac_64", new String[] { "AVC", "150", "240x140", "AAC LC", "128" });
            put("h264_320x180_240_aac_64", new String[] { "AVC", "240", "320x180", "AAC LC", "128" });
            put("h264_640x360_440_aac_64", new String[] { "AVC", "440", "640x360", "AAC LC", "128" });
            put("h264_640x360_700_aac_64", new String[] { "AVC", "700", "640x360", "AAC LC", "128" });
            put("h264_1280x720_1200_aac_64", new String[] { "AVC", "1200", "1280x720", "AAC LC", "128" });
            put("h264_1280x720_1800_aac_64", new String[] { "AVC", "1800", "1280x720", "AAC LC", "128" });
            put("h264_1280x720_2400_aac_64", new String[] { "AVC", "2400", "1280x720", "AAC LC", "128" });
            put("mpg2_720x480_mpg2", new String[] { "MPEG-2 Video", "6000", "720x480", "MPEG-1 Audio layer 2", "192" });

        }
    });

    private String                                DLLINK         = null;

    @SuppressWarnings("deprecation")
    public BloombergCom(final PluginWrapper wrapper) {
        super(wrapper);
        setConfigElements();
    }

    @Override
    public String getAGBLink() {
        return "http://www.bloomberg.com/tos";
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return -1;
    }

    @Override
    public AvailableStatus requestFileInformation(final DownloadLink link) throws Exception {
        setBrowserExclusive();
        // final String mainlink = link.getStringProperty("mainlink", null);
        // br.getPage(mainlink);
        // if (br.getHttpConnection().getResponseCode() == 404) {
        // throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        // }
        DLLINK = link.getStringProperty("directlink", null);
        URLConnectionAdapter con = null;
        try {
            try {
                if (isJDStable()) {
                    /* @since JD2 */
                    con = br.openHeadConnection(DLLINK);
                } else {
                    /* Not supported in old 0.9.581 Stable */
                    con = br.openGetConnection(DLLINK);
                }
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

    private boolean isJDStable() {
        return System.getProperty("jd.revision.jdownloaderrevision") == null;
    }

    @Override
    public void handleFree(final DownloadLink link) throws Exception {
        requestFileInformation(link);
        br.setFollowRedirects(true);
        dl = jd.plugins.BrowserAdapter.openDownload(br, link, DLLINK, true, 0);
        if (dl.getConnection().getContentType().contains("html")) {
            if (dl.getConnection().getResponseCode() == 403) {
                throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server error 403", 60 * 60 * 1000l);
            } else if (dl.getConnection().getResponseCode() == 404) {
                throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server error 404", 60 * 60 * 1000l);
            }
            br.followConnection();
            try {
                dl.getConnection().disconnect();
            } catch (final Throwable e) {
            }
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        dl.startDownload();
    }

    @Override
    public String getDescription() {
        return "JDownloader's Bloomberg plugin helps downloading videoclips from bloomberg.com.";
    }

    private void setConfigElements() {
        /* Currently not needed as we get the filesize from the XML */
        // getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, getPluginConfig(), FAST_LINKCHECK,
        // JDL.L("plugins.hoster.BloombergCom.FastLinkcheck",
        // "Enable fast linkcheck?\r\nNOTE: If enabled, links will appear faster but filesize won't be shown before downloadstart.")).setDefaultValue(false));
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
            final ConfigEntry vidcfg = new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, getPluginConfig(), internalname, JDL.L("plugins.hoster.BloombergCom.ALLOW_" + internalname, usertext)).setDefaultValue(true);
            getConfig().addEntry(vidcfg);
        }
    }

    @Override
    public void reset() {
    }

    @Override
    public void resetDownloadlink(final DownloadLink link) {
    }

    @Override
    public void resetPluginGlobals() {
    }
}