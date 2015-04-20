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
import jd.http.Browser;
import jd.http.Browser.BrowserException;
import jd.http.URLConnectionAdapter;
import jd.parser.Regex;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "dmax.de", "tlc.de", "discovery.de", "animalplanet.de" }, urls = { "http://(www\\.)?dmax\\.de/(programme/[a-z0-9\\-]+/videos/[a-z0-9\\-]+/|videos/#\\d+)", "http://(www\\.)?tlc\\.de/[^<>\"]*?videos/#\\d+", "http://(www\\.)?discovery\\.de/[^<>\"]*?(video|highlights)/#\\d+", "http://(www\\.)?animalplanet\\.de/[^<>\"]*?video/#\\d+" }, flags = { 0, 0, 0, 0 })
public class DmaxDe extends PluginForHost {

    public DmaxDe(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public String getAGBLink() {
        return "http://www.dmax.de/agb/";
    }

    /* Tags: Discovery Communications Inc */
    private static final String  type_videoid           = "https?://.+/videos?/#\\d+$";

    /* Connection stuff */
    private static final boolean FREE_RESUME            = true;
    private static final int     FREE_MAXCHUNKS         = 0;
    private static final int     FREE_MAXDOWNLOADS      = 20;

    /* Last updated: 01.11.13 */
    // private static final String playerKey = "AAAAAGLvCOI~,a0C3h1Jh3aQKs2UcRZrrxyrjE0VH93xl";
    // private static final String playerID = "586587148001";
    // private static final String publisherID = "1659832546";

    /* Last updated: 06.07.14 */
    private static final String  apiTokenDmax           = "XoVA15ecuocTY5wBbxNImXVFbQd72epyxxVcH3ZVmOA.";
    private static final String  apiTokenTlc            = "XoVA15ecuocTY5wBbxNImXVFbQd72epyxxVcH3ZVmOA.";
    private static final String  apiTokenDiscoveryDe    = "XoVA15ecuocTY5wBbxNImXVFbQd72epyxxVcH3ZVmOA.";
    private static final String  apiTokenAnimalplanetDe = "XoVA15ecuocTY5wBbxNImXVFbQd72epyxxVcH3ZVmOA.";

    private String               apiTokenCurrent        = null;
    private String               DLLINK                 = null;
    private boolean              downloadMode           = false;

    /*
     * Thanks goes to: https://github.com/bromix/plugin.video.bromix.dmax_de/blob/master/discoverychannel/fusion.py AND
     * https://github.com/dethfeet/plugin.video.dmax/blob/master/default.py
     */
    @SuppressWarnings({ "deprecation", "unchecked", "rawtypes" })
    @Override
    public AvailableStatus requestFileInformation(final DownloadLink link) throws Exception {
        String vpath = null;
        String videoID;
        this.setBrowserExclusive();
        initAPI(link);
        if (link.getDownloadURL().matches(type_videoid)) {
            videoID = link.getDownloadURL().substring(link.getDownloadURL().lastIndexOf("#") + 1);
        } else {
            br.getPage(link.getDownloadURL());
            if (br.getHttpConnection().getResponseCode() == 404) {
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            }
            videoID = br.getRegex("\"@videoPlayer\".value=\"([^\"]+)\"").getMatch(0);
            /* Get rid of cookies/Headers before we do the API request! */
            this.br = new Browser();
        }
        if (videoID == null) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        try {
            link.setLinkID(videoID);
        } catch (final Throwable e) {
            /* Not available in old 0.9.581 Stable */
        }
        /* Offline links should also have nice filenames */
        link.setName(videoID + ".mp4");
        this.br.getHeaders().put("User-Agent", "stagefright/1.2 (Linux;Android 4.4.2)");
        accessAPI("find_video_by_id", "video_id=" + videoID + "&video_fields=name,renditions");
        if (br.getHttpConnection().getResponseCode() == 404 || br.toString().equals("null")) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }

        final LinkedHashMap<String, Object> entries = (LinkedHashMap<String, Object>) jd.plugins.hoster.DummyScriptEnginePlugin.jsonToJavaObject(br.toString());
        if (entries.get("error") != null) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        String title = (String) entries.get("name");
        final ArrayList<Object> ressourcelist = (ArrayList) entries.get("renditions");
        long foundFilesize = -1;
        long width = -1;
        long height = -1;
        for (final Object o : ressourcelist) {
            final LinkedHashMap<String, Object> vdata = (LinkedHashMap<String, Object>) o;
            DLLINK = (String) vdata.get("url");
            final Object size = vdata.get("size");
            final Object owidth = vdata.get("frameWidth");
            final Object oheigth = vdata.get("frameHeight");
            long fsize = -1;
            if (owidth != null && oheigth != null) {
                width = getLongValue(owidth);
                height = getLongValue(oheigth);
            }
            if (size != null) {
                fsize = getLongValue(size);
            }
            if (fsize > foundFilesize) {
                foundFilesize = fsize;
            }

        }
        if (title == null || DLLINK == null) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        /* Okay now let's convert the standard Akamai HD URLs to standard HTTP URLs. */
        /*
         * Akamai URL:
         * http://discintlhdflash-f.akamaihd.net/byocdn/media/1659832546/201503/3129/1234567890_4133464537001_mp4-DCB366660004000-204.mp4
         */
        /*
         * HTTP URL: http://discoveryint1.edgeboss.net/download/discoveryint1/byocdn/media/1659832546/201503/3129/
         * 1234567890_4133464537001_mp4-DCB366660004000-204.mp4
         */
        /* "byocdn/media/.+" == similarity */
        /* Akamai is not always used so 'vpath' can also be null in some rare cases as we already have a normal http url. */
        vpath = new Regex(DLLINK, "(/byocdn/media/.+)").getMatch(0);
        if (vpath != null) {
            DLLINK = "http://discoveryint1.edgeboss.net/download/discoveryint1/" + vpath;
        }
        title = encodeUnicode(title);
        if (width > -1 && height > -1) {
            title = title + "_" + width + "x" + height;
        }
        title += ".mp4";
        link.setFinalFileName(title);
        /*-1 = defaultvalue, 0 =  no Akamai url --> Filesize not given in API json --> We have to find it via the headers.*/
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

    private long getLongValue(final Object o) {
        long lo = -1;
        if (o instanceof Long) {
            lo = ((Long) o).longValue();
        } else {
            lo = ((Integer) o).intValue();
        }
        return lo;
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
    }

    private void accessAPI(final String command, final String params) throws IOException {
        // /* Request to get 'officially'-downloadable URLs (but lower quality) */
        // br.getPage("https://api.brightcove.com/services/library?command=find_video_by_id&video_fields=name%2CFLVURL%2CreferenceId%2CitemState%2Cid&media_delivery=http&video_id=2827406067001&token=XoVA15ecuocTY5wBbxNImXVFbQd72epyxxVcH3ZVmOA.");
        final String url = "https://api.brightcove.com/services/library?token=" + this.apiTokenCurrent + "&command=" + command + "&" + params;
        this.br.getPage(url);
    }

    /** Avoid chars which are not allowed in filenames under certain OS' */
    private static String encodeUnicode(final String input) {
        String output = input;
        output = output.replace(":", ";");
        output = output.replace("|", "¦");
        output = output.replace("<", "[");
        output = output.replace(">", "]");
        output = output.replace("/", "⁄");
        output = output.replace("\\", "∖");
        output = output.replace("*", "#");
        output = output.replace("?", "¿");
        output = output.replace("!", "¡");
        output = output.replace("\"", "'");
        return output;
    }

    // private void getAMFRequest(final Browser amf, final byte[] b, String s) throws IOException {
    // amf.getHeaders().put("Content-Type", "application/x-amf");
    // amf.setKeepResponseContentBytes(true);
    // PostRequest request = (PostRequest) amf.createPostRequest("http://c.brightcove.com/services/messagebroker/amf?playerKey=" + s,
    // (String) null);
    // request.setPostBytes(b);
    // amf.openRequestConnection(request);
    // amf.loadConnection(null);
    // }
    //
    // private String build_amf_request(final String cnst, final String playerID, final String videoPlayer, final String publisherID) throws
    // IOException {
    // br.postPage("http://c.brightcove.com/services/messagebroker/amf?playerKey=" + playerID, "");
    // return null;
    // }
    //
    // private byte[] createAMFMessage(String... s) {
    // /* TODO Update this to work with dmax! */
    // String data =
    // "0A0000000202002838363436653830346539333531633838633464643962306630383166316236643062373464363039110A6363636F6D2E627269676874636F76652E657870657269656E63652E566965776572457870657269656E63655265717565737419657870657269656E636549641964656C6976657279547970650755524C13706C617965724B657921636F6E74656E744F76657272696465731154544C546F6B656E054281B0158F580800057FFFFFFFE0000000";
    // data += "06" + getHexLength(s[0], true) + JDHexUtils.getHexString(s[0]); // 0x06(String marker) + length + String b
    // data += "06" + getHexLength(s[1], true) + JDHexUtils.getHexString(s[1]);
    // data +=
    // "0903010A810353636F6D2E627269676874636F76652E657870657269656E63652E436F6E74656E744F7665727269646515666561747572656449641B6665617475726564526566496417636F6E74656E745479706513636F6E74656E7449640D74617267657415636F6E74656E744964731B636F6E74656E7452656649647319636F6E74656E745265664964057FFFFFFFE0000000010400057FFFFFFFE00000000617766964656F506C617965720101";
    // data += "06" + getHexLength(s[2], true) + JDHexUtils.getHexString(s[2]);
    // data += "0601";
    // return
    // JDHexUtils.getByteArray("0003000000010046636F6D2E627269676874636F76652E657870657269656E63652E457870657269656E636552756E74696D654661636164652E67657444617461466F72457870657269656E636500022F310000"
    // + getHexLength(JDHexUtils.toString(data), false) + data);
    // }
    //
    // private String getHexLength(final String s, boolean amf3) {
    // String result = Integer.toHexString(s.length() | 1);
    // if (amf3) {
    // result = "";
    // for (int i : getUInt29(s.length() << 1 | 1)) {
    // if (i == 0) {
    // break;
    // }
    // result += Integer.toHexString(i);
    // }
    // }
    // return result.length() % 2 > 0 ? "0" + result : result;
    // }
    //
    // private int[] getUInt29(int ref) {
    // int[] buf = new int[4];
    // if (ref < 0x80) {
    // buf[0] = ref;
    // } else if (ref < 0x4000) {
    // buf[0] = (((ref >> 7) & 0x7F) | 0x80);
    // buf[1] = ref & 0x7F;
    // } else if (ref < 0x200000) {
    // buf[0] = (((ref >> 14) & 0x7F) | 0x80);
    // buf[1] = (((ref >> 7) & 0x7F) | 0x80);
    // buf[2] = ref & 0x7F;
    // } else if (ref < 0x40000000) {
    // buf[0] = (((ref >> 22) & 0x7F) | 0x80);
    // buf[1] = (((ref >> 15) & 0x7F) | 0x80);
    // buf[2] = (((ref >> 8) & 0x7F) | 0x80);
    // buf[3] = ref & 0xFF;
    // } else {
    // logger.warning("about.com(amf3): Integer out of range: " + ref);
    // }
    // return buf;
    // }

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