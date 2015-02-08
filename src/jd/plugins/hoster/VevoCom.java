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
import java.util.ArrayList;
import java.util.LinkedHashMap;

import jd.PluginWrapper;
import jd.config.Property;
import jd.http.Browser;
import jd.http.URLConnectionAdapter;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "vevo.com" }, urls = { "http://www\\.vevo\\.com/watch/([A-Za-z0-9\\-_]+/[A-Za-z0-9\\-_]+/[A-Z0-9]+|[A-Z0-9]+)" }, flags = { 0 })
public class VevoCom extends PluginForHost {

    public VevoCom(PluginWrapper wrapper) {
        super(wrapper);
    }

    /** URL that contains all kinds of API/URL/site information: http://cache.vevo.com/a/swf/assets/xml/base_config_v3.xml?cb=20130110 */
    /**
     * API url (token needed, got no source for that at the moment): https://apiv2.vevo.com/video/<VIDEOID>/streams/hls?token=<APITOKEN> or
     * https://apiv2.vevo.com/video/<VIDEOID>?token=<APITOKEN>
     */
    /**
     * Additional way to get video source (returns rtmp urls): http://smil.lvl3.vevo.com/Video/V2/VFILE/<VIDEOID>/<VIDEOID(LOWERCASE)>r.smil
     * rtmpurl is fixen, app =vevood, swfvy = player-String(see below), playpath is given in .smil source Last checked: 08.01.2015: The best
     * rtmp stream had worst quality than the best http stream!
     */
    /** Additional hint: Vevo also has Apps for a lot of platforms: http://www.vevo.com/c/DE/DE/apps */
    /** Additional thanks goes to: https://github.com/rg3/youtube-dl/blob/master/youtube_dl/extractor/vevo.py */

    /** Also possible: http://cache.vevo.com/a/swf/versions/3/player.swf?eurl=www.somewebsite.com&cb=<12-digit-number> */
    private static final String  player            = "http://cache.vevo.com/a/swf/versions/3/player.swf";

    /* Extension which will be used if no correct extension is found */
    private static final String  default_Extension = ".mp4";
    /* Connection stuff */
    private static final boolean free_resume       = true;
    private static final int     free_maxchunks    = 0;
    private static final int     free_maxdownloads = -1;

    private static final String  type_invalid      = "http://(www\\.)?vevo\\.com/watch/playlist";

    @Override
    public String getAGBLink() {
        return "http://www.vevo.com/c/DE/DE/legal/terms-conditions";
    }

    /** TODO: Add support for embed links */
    @SuppressWarnings("deprecation")
    @Override
    public AvailableStatus requestFileInformation(final DownloadLink downloadLink) throws IOException, PluginException {
        if (downloadLink.getDownloadURL().matches(type_invalid)) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        this.setBrowserExclusive();
        br.setFollowRedirects(true);
        br.getPage(downloadLink.getDownloadURL());
        if (br.getHttpConnection().getResponseCode() == 404) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        String filename = br.getRegex("<title>([^<>]*?) \\- Vevo</title>").getMatch(0);
        if (filename == null) {
            filename = br.getRegex("property=\"og:title\" content=\"([^<>]*?)\"").getMatch(0);
        }
        if (filename == null) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        filename = Encoding.htmlDecode(filename);
        filename = filename.trim();
        filename = encodeUnicode(filename);
        downloadLink.setFinalFileName(filename + default_Extension);
        return AvailableStatus.TRUE;
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    @Override
    public void handleFree(final DownloadLink downloadLink) throws Exception {
        requestFileInformation(downloadLink);
        final String[] possibleQualities = { "High", "Med", "Low" };
        final String videoid = br.getRegex("\"isrc\":\"([^<>\"]*?)\"").getMatch(0);
        if (videoid == null) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        br.getHeaders().put("Referer", player);
        /*
         * Also possible:
         * http://videoplayer.vevo.com/VideoService/AuthenticateVideo?isrc=<videoid>&domain=<some_domain>&authToken=X-X-X-X-X&pkey=X-X-X-X-X
         * This way is usually used for embedded videos.
         */
        br.getPage("http://videoplayer.vevo.com/VideoService/AuthenticateVideo?isrc=" + videoid);
        String dllink = checkDirectLink(downloadLink, "directlink");
        if (dllink == null) {
            String html_videosource = null;
            final LinkedHashMap<String, Object> entries = (LinkedHashMap<String, Object>) jd.plugins.hoster.DummyScriptEnginePlugin.jsonToJavaObject(br.toString());
            final LinkedHashMap<String, Object> video = (LinkedHashMap<String, Object>) entries.get("video");
            final ArrayList<Object> ressourcelist = (ArrayList) video.get("videoVersions");
            int count = ressourcelist.size();
            /* Explanation of sourceType: 0=undefined, 1=?Probably HDS?, 2=HTTP, 3=HLS iOS,4=HLS, 10=SmoothStreaming */
            /*
             * Explanation of version: Seems to be different vevo data servers as it has no influence on the videoquality: 0==, 1=?,
             * 2=aka.vevo.com, 3=lvl3.vevo.com, 4=aws.vevo.com --> version 2 never worked for me
             */
            /* Last checked: 08.01.2015 */
            LinkedHashMap<String, Object> tempmap = null;
            for (int counter = count - 1; counter >= 0; counter--) {
                tempmap = (LinkedHashMap<String, Object>) ressourcelist.get(counter);
                final int sourceType = ((Number) tempmap.get("sourceType")).intValue();
                /* We prefer http */
                if (sourceType == 2) {
                    html_videosource = (String) tempmap.get("data");
                    break;
                }
            }
            /* Clean that */
            html_videosource = html_videosource.replace("\\", "");
            for (final String possibleQuality : possibleQualities) {
                dllink = new Regex(html_videosource, "name=\"" + possibleQuality + "\" url=\"(http[^<>\"]*?)\"").getMatch(0);
                if (dllink != null) {
                    logger.info("Found video quality: " + possibleQuality);
                    break;
                }
            }
        }
        dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, dllink, free_resume, free_maxchunks);
        if (dl.getConnection().getContentType().contains("html")) {
            if (dl.getConnection().getResponseCode() == 403) {
                throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server error 403", 60 * 60 * 1000l);
            } else if (dl.getConnection().getResponseCode() == 404) {
                throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server error 404", 60 * 60 * 1000l);
            }
            br.followConnection();
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        downloadLink.setProperty("directlink", dllink);
        dl.startDownload();
    }

    private String checkDirectLink(final DownloadLink downloadLink, final String property) {
        String dllink = downloadLink.getStringProperty(property);
        if (dllink != null) {
            URLConnectionAdapter con = null;
            try {
                final Browser br2 = br.cloneBrowser();
                con = br2.openGetConnection(dllink);
                if (con.getContentType().contains("html") || con.getLongContentLength() == -1) {
                    downloadLink.setProperty(property, Property.NULL);
                    dllink = null;
                }
            } catch (final Exception e) {
                downloadLink.setProperty(property, Property.NULL);
                dllink = null;
            } finally {
                try {
                    con.disconnect();
                } catch (final Throwable e) {
                }
            }
        }
        return dllink;
    }

    /* Avoid chars which are not allowed in filenames under certain OS' */
    private static String encodeUnicode(final String input) {
        String output = input;
        output = output.replace(":", ";");
        output = output.replace("|", "¦");
        output = output.replace("<", "[");
        output = output.replace(">", "]");
        output = output.replace("/", "/");
        output = output.replace("\\", "");
        output = output.replace("*", "#");
        output = output.replace("?", "¿");
        output = output.replace("!", "¡");
        output = output.replace("\"", "'");
        return output;
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return free_maxdownloads;
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
