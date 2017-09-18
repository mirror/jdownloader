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
import jd.http.Browser;
import jd.http.Browser.BrowserException;
import jd.http.URLConnectionAdapter;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;
import jd.utils.locale.JDL;

import org.jdownloader.scripting.JavaScriptEngineFactory;

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "dmax.de", "tlc.de", "discovery.de", "animalplanet.de" }, urls = { "https?://dmax\\.dedecrypted\\d+", "http://tlc\\.dedecrypted\\d+", "http://discovery\\.dedecrypted\\d+", "http://animalplanet\\.dedecrypted\\d+" })
public class DmaxDe extends PluginForHost {
    public DmaxDe(PluginWrapper wrapper) {
        super(wrapper);
        setConfigElements();
    }

    @Override
    public String getAGBLink() {
        return "http://www.dmax.de/agb/";
    }

    /* Tags: Discovery Communications Inc */
    /* Connection stuff */
    private static final boolean                  FREE_RESUME            = true;
    private static final int                      FREE_MAXCHUNKS         = 0;
    private static final int                      FREE_MAXDOWNLOADS      = 20;
    /* Last updated: 01.11.13 */
    // private static final String playerKey = "AAAAAGLvCOI~,a0C3h1Jh3aQKs2UcRZrrxyrjE0VH93xl";
    // private static final String playerID = "586587148001";
    // private static final String publisherID = "1659832546";
    /* Last updated: 06.07.14 */
    public static final String                    apiTokenDmax           = "XoVA15ecuocTY5wBbxNImXVFbQd72epyxxVcH3ZVmOA.";
    public static final String                    apiTokenTlc            = "XoVA15ecuocTY5wBbxNImXVFbQd72epyxxVcH3ZVmOA.";
    public static final String                    apiTokenDiscoveryDe    = "XoVA15ecuocTY5wBbxNImXVFbQd72epyxxVcH3ZVmOA.";
    public static final String                    apiTokenAnimalplanetDe = "XoVA15ecuocTY5wBbxNImXVFbQd72epyxxVcH3ZVmOA.";
    private String                                apiTokenCurrent        = null;
    private String                                DLLINK                 = null;
    private boolean                               downloadMode           = false;
    public static LinkedHashMap<String, String[]> formats                = new LinkedHashMap<String, String[]>(new LinkedHashMap<String, String[]>() {
        {
            /*
             * Format-name:videoCodec, videoBitrate, videoResolution,
             * audioCodec, audioBitrate
             */
            put("240", new String[] { "AVC", "440", "240x140", "AAC LC", "128" });
            put("480", new String[] { "AVC", "440", "480x268", "AAC LC", "128" });
            put("640", new String[] { "AVC", "700", "640x360", "AAC LC", "128" });
            put("720", new String[] { "AVC", "950", "720x204", "AAC LC", "128" });
            put("960", new String[] { "AVC", "950", "960x540", "AAC LC", "128" });
            put("1024", new String[] { "AVC", "950", "1024x576", "AAC LC", "128" });
            put("1280", new String[] { "AVC", "1600", "1280x720", "AAC LC", "128" });
        }
    });

    /*
     * Thanks goes to: https://github.com/bromix/plugin.video.bromix.dmax_de/blob/master/discoverychannel/fusion.py AND
     * https://github.com/dethfeet/plugin.video.dmax/blob/master/default.py
     */
    @SuppressWarnings({ "unchecked" })
    @Override
    public AvailableStatus requestFileInformation(final DownloadLink link) throws Exception {
        DLLINK = link.getStringProperty("directlink", null);
        final String videoID = link.getStringProperty("videoid", null);
        long foundFilesize = link.getLongProperty("directsize", -1);
        this.setBrowserExclusive();
        initAPI(link);
        try {
            link.setLinkID(videoID);
        } catch (final Throwable e) {
            /* Not available in old 0.9.581 Stable */
        }
        if (DLLINK == null) {
            /* User has old links in downloadlist --> Rare case */
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        link.setFinalFileName(link.getStringProperty("directfilename", null));
        accessAPI("find_video_by_id", "video_id=" + videoID + "&video_fields=name,renditions");
        if (br.getHttpConnection().getResponseCode() == 404 || br.toString().equals("null")) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        final LinkedHashMap<String, Object> entries = (LinkedHashMap<String, Object>) JavaScriptEngineFactory.jsonToJavaObject(br.toString());
        if (entries.get("error") != null) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        /*-1 = defaultvalue, 0 =  no Akamai url --> Filesize not given in API json (or by decrypter) --> We have to find it via the headers.*/
        if (foundFilesize < 1) {
            URLConnectionAdapter con = null;
            try {
                try {
                    con = openConnection(this.br, DLLINK);
                    foundFilesize = con.getLongContentLength();
                    link.setProperty("free_directlink", DLLINK);
                } catch (final BrowserException e) {
                    link.getLinkStatus().setStatusText("Es bestehen möglicherweise Serverprobleme");
                    if (downloadMode) {
                        throw e;
                    }
                    /* We know that the link is online but for some reason an Exception happened here. This never happened in my tests. */
                    return AvailableStatus.TRUE;
                }
            } finally {
                try {
                    con.disconnect();
                } catch (final Throwable e) {
                }
            }
        }
        link.setDownloadSize(foundFilesize);
        return AvailableStatus.TRUE;
    }

    private URLConnectionAdapter openConnection(final Browser br, final String directlink) throws IOException {
        URLConnectionAdapter con;
        if (isJDStable()) {
            con = br.openGetConnection(directlink);
        } else {
            con = br.openHeadConnection(directlink);
        }
        return con;
    }

    @Override
    public void handleFree(final DownloadLink downloadLink) throws Exception, PluginException {
        downloadMode = true;
        requestFileInformation(downloadLink);
        doFree(downloadLink, FREE_RESUME, FREE_MAXCHUNKS, "free_directlink");
    }

    private void doFree(final DownloadLink downloadLink, final boolean resumable, final int maxchunks, final String directlinkproperty) throws Exception, PluginException {
        dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, DLLINK, resumable, maxchunks);
        if (dl.getConnection().getContentType().contains("html")) {
            if (dl.getConnection().getResponseCode() == 403) {
                throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server error 403", 60 * 60 * 1000l);
            } else if (dl.getConnection().getResponseCode() == 404) {
                throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server error 404", 60 * 60 * 1000l);
            }
            br.followConnection();
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        downloadLink.setProperty(directlinkproperty, DLLINK);
        dl.startDownload();
    }

    // private String checkDirectLink(final DownloadLink downloadLink, final String property) {
    // String dllink = downloadLink.getStringProperty(property);
    // if (dllink != null) {
    // URLConnectionAdapter con = null;
    // try {
    // final Browser br2 = br.cloneBrowser();
    // if (isJDStable()) {
    // con = br2.openGetConnection(dllink);
    // } else {
    // con = br2.openHeadConnection(dllink);
    // }
    // if (con.getContentType().contains("html") || con.getLongContentLength() == -1) {
    // downloadLink.setProperty(property, Property.NULL);
    // dllink = null;
    // }
    // } catch (final Exception e) {
    // downloadLink.setProperty(property, Property.NULL);
    // dllink = null;
    // } finally {
    // try {
    // con.disconnect();
    // } catch (final Throwable e) {
    // }
    // }
    // }
    // return dllink;
    // }
    private void initAPI(final DownloadLink dl) throws PluginException {
        final String host = dl.getHost();
        if (host.equals("dmax.de")) {
            this.apiTokenCurrent = apiTokenDmax;
        } else if (host.equals("discovery.de")) {
            this.apiTokenCurrent = apiTokenDiscoveryDe;
        } else if (host.equalsIgnoreCase("tlc.de")) {
            this.apiTokenCurrent = apiTokenTlc;
        } else if (host.equals("animalplanet.de")) {
            this.apiTokenCurrent = apiTokenAnimalplanetDe;
        } else {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        this.br.getHeaders().put("User-Agent", "stagefright/1.2 (Linux;Android 4.4.2)");
    }

    private void accessAPI(final String command, final String params) throws IOException {
        // /* Request to get 'officially'-downloadable URLs (but lower quality) */
        // br.getPage("https://api.brightcove.com/services/library?command=find_video_by_id&video_fields=name%2CFLVURL%2CreferenceId%2CitemState%2Cid&media_delivery=http&video_id=2827406067001&token=XoVA15ecuocTY5wBbxNImXVFbQd72epyxxVcH3ZVmOA.");
        final String url = "https://api.brightcove.com/services/library?token=" + this.apiTokenCurrent + "&command=" + command + "&" + params;
        this.br.getPage(url);
    }

    @Override
    public String getDescription() {
        return "JDownloader's DMAX plugin helps downloading videoclips from dmax.de.";
    }

    private void setConfigElements() {
        /* Currently not needed as we get the filesize from the XML */
        // getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, getPluginConfig(), FAST_LINKCHECK,
        // JDL.L("plugins.hoster.DmaxDe.FastLinkcheck",
        // "Enable fast linkcheck?\r\nNOTE: If enabled, links will appear faster but filesize won't be shown before downloadstart.")).setDefaultValue(false));
        getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_LABEL, "Video Formatseinstellungen:\r\n<html><b>Wichtig: In manchen Fällen ist nur eine (undefinierte) Qualitätsstufe da.\r\nIn solchen Fällen wird diese, unabhängig von diesen Einstellungen hinzugefügt!</b></p></html>"));
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
            final ConfigEntry vidcfg = new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, getPluginConfig(), internalname, JDL.L("plugins.hoster.DmaxDe.ALLOW_" + internalname, usertext)).setDefaultValue(true);
            getConfig().addEntry(vidcfg);
        }
    }

    private boolean isJDStable() {
        return System.getProperty("jd.revision.jdownloaderrevision") == null;
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return FREE_MAXDOWNLOADS;
    }

    @Override
    public void reset() {
    }

    @Override
    public void resetDownloadlink(DownloadLink link) {
    }
}