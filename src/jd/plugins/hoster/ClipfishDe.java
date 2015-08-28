//    jDownloader - Downloadmanager
//    Copyright (C) 2008  JD-Team support@jdownloader.org
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

import java.util.Locale;
import java.util.regex.Pattern;

import jd.PluginWrapper;
import jd.http.URLConnectionAdapter;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;
import jd.plugins.download.DownloadInterface;

import org.appwork.utils.StringUtils;

@HostPlugin(revision = "$Revision$", interfaceVersion = 3, names = { "clipfish.de" }, urls = { "http://(?:www\\.)?clipfish\\.de/(?:.*?channel/\\d+/video/\\d+|video/\\d+(?:/.+)?|special/.*?/video/\\d+|musikvideos/video/\\d+(?:/.+)?)" }, flags = { 0 })
public class ClipfishDe extends PluginForHost {

    public ClipfishDe(final PluginWrapper wrapper) {
        super(wrapper);
    }

    /* Tags: rtl-interactive.de */
    /* HbbTV also available */

    private final Pattern PATTERN_CAHNNEL_VIDEO  = Pattern.compile("http://[w\\.]+?clipfish\\.de/.*?channel/\\d+/video/(\\d+)");
    private final Pattern PATTERN_MUSIK_VIDEO    = Pattern.compile("http://[w\\.]+?clipfish\\.de/musikvideos/video/(\\d+)(/.+)?");
    private final Pattern PATTERN_STANDARD_VIDEO = Pattern.compile("http://[w\\.]+?clipfish\\.de/video/(\\d+)(/.+)?");
    private final Pattern PATTERN_SPECIAL_VIDEO  = Pattern.compile("http://[w\\.]+?clipfish\\.de/special/.*?/video/(\\d+)");
    private final Pattern PATTERN_FLV_FILE       = Pattern.compile("&url=(http://.+?\\....)&|<filename><\\!\\[CDATA\\[(.*?)\\]\\]></filename>", Pattern.CASE_INSENSITIVE);
    private final Pattern PATTERN_TITEL          = Pattern.compile("<meta property=\"og:title\" content=\"(.+?)\"/>", Pattern.CASE_INSENSITIVE);

    private final String  NEW_XMP_PATH           = "http://www.clipfish.de/devxml/videoinfo/";

    private String        dllink                 = null;
    private String        mediatype              = null;
    private String        videoid                = null;
    String                flashplayer            = null;

    @SuppressWarnings("deprecation")
    @Override
    public void correctDownloadLink(final DownloadLink link) {
        if (link.getDownloadURL().startsWith("clipfish2")) {
            link.setUrlDownload(link.getDownloadURL().replaceAll("clipfish2://", "http://"));
        }
    }

    @Override
    public String getAGBLink() {
        return "http://www.clipfish.de/agb/";
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return -1;
    }

    @SuppressWarnings("deprecation")
    @Override
    public AvailableStatus requestFileInformation(final DownloadLink downloadLink) throws Exception {
        this.br.setAllowedResponseCodes(410);
        this.br.setFollowRedirects(true);
        dllink = null;
        mediatype = null;
        videoid = null;
        flashplayer = null;
        final String added_url = downloadLink.getDownloadURL();

        this.br.getPage(added_url);
        final Regex regexInfo = new Regex(this.br.toString(), PATTERN_TITEL);
        if (br.getURL().contains("clipfish.de/special/cfhome/home/")) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        if (!br.containsHTML("CFAdBrandedPlayer\\.js") && !br.containsHTML("CFPlayerBasic(\\.min)?\\.js")) {
            logger.info("Link offline/unsupported");
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        if (this.br.getHttpConnection().getResponseCode() == 410) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }

        String tmpStr = regexInfo.getMatch(0);
        if (tmpStr == null) {
            tmpStr = br.getRegex("<h2>([^<]+)</h2>").getMatch(0);
            if (tmpStr != null) {
                tmpStr = br.getRegex("title=\"(" + tmpStr + "[^\"]+)\"").getMatch(0);
            }
        }
        if (tmpStr == null) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        String name = tmpStr.substring(0, tmpStr.lastIndexOf("-"));
        mediatype = tmpStr.substring(tmpStr.lastIndexOf("-") + 1, tmpStr.length()).toLowerCase(Locale.ENGLISH);
        if (name == null || mediatype == null) {
            return null;
        }
        mediatype = mediatype.trim();

        if (new Regex(added_url, PATTERN_STANDARD_VIDEO).matches()) {
            videoid = new Regex(added_url, PATTERN_STANDARD_VIDEO).getMatch(0);
        } else if (new Regex(added_url, PATTERN_CAHNNEL_VIDEO).matches()) {
            videoid = new Regex(added_url, PATTERN_CAHNNEL_VIDEO).getMatch(0);
        } else if (new Regex(added_url, PATTERN_SPECIAL_VIDEO).matches()) {
            videoid = br.getRegex("vid\\s*:\\s*\"(\\d+)\"").getMatch(0);
        } else if (new Regex(added_url, PATTERN_MUSIK_VIDEO).matches()) {
            videoid = new Regex(added_url, PATTERN_MUSIK_VIDEO).getMatch(0);
        } else {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }

        this.flashplayer = br.getRegex("(clipfish_player_\\d+\\.swf)").getMatch(0);

        String spc = br.getRegex("specialId\\s*=\\s*(\\d+)").getMatch(0);
        int specialID = spc != null ? Integer.parseInt(spc) : -1;
        // specialID

        /*
         * Example http url: http://video.clipfish.de/media/<vid_url_id>/<videohash>.mp4 Videohashes can also be found in pic urls:
         * /autoimg/<videohash> /
         */
        /* Example HDS url (first req): http://hds.fra.clipfish.de/hds-vod-enc/media/<vid_url_id>/<videohash>.mp4.f4m */

        dllink = br.getRegex("videourl: \"(http[^<>\"]*?)\"").getMatch(0);
        if (dllink == null) {
            dllink = this.br.getRegex("var videourl[\t\n\r ]*?=[\t\n\r ]*?\"(http[^<>\"]*?)\";").getMatch(0);
        }
        if (dllink == null) {
            if (specialID >= 0) {
                br.getPage(NEW_XMP_PATH + videoid + "/" + specialID + "/" + "?ts=" + System.currentTimeMillis());
            } else {
                br.getPage(NEW_XMP_PATH + videoid + "/" + "?ts=" + System.currentTimeMillis());
            }
            String page = br.toString();
            dllink = getDllink(page);
            if (dllink.startsWith("rtmp")) {
                String dataPath = new Regex(dllink, "(media/.+?\\.mp4)").getMatch(0);
                if (dataPath != null) {
                    dllink = "http://video.clipfish.de/" + dataPath;
                }

            }
        }

        name = Encoding.htmlDecode(name.trim());

        String ext = dllink.substring(dllink.lastIndexOf(".") + 1, dllink.length());
        if (dllink.startsWith("rtmp")) {
            ext = new Regex(dllink, "(\\w+):media/").getMatch(0);
            ext = ext.length() > 3 ? null : ext;
            if (flashplayer != null) {
                this.flashplayer = "http://www.clipfish.de/cfng/flash/" + this.flashplayer;
            }
        }

        ext = ext == null || ext.equals("f4v") || ext.equals("") ? "mp4" : ext;
        downloadLink.setFinalFileName(name + "." + ext);

        if ("".equals(dllink)) {
            dllink = downloadLink.getDownloadURL();
        } else if (dllink == null) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        if (dllink.startsWith("http") && !dllink.contains(".hds.")) {
            final URLConnectionAdapter con = br.openHeadConnection(dllink);
            try {
                if (con.getResponseCode() == 200 && StringUtils.containsIgnoreCase(con.getContentType(), "video") && con.getCompleteContentLength() > 0) {
                    downloadLink.setVerifiedFileSize(con.getCompleteContentLength());
                } else {
                    return AvailableStatus.FALSE;
                }
            } finally {
                if (con != null) {
                    con.disconnect();
                }
            }
        }
        return AvailableStatus.TRUE;
    }

    @SuppressWarnings("deprecation")
    @Override
    public void handleFree(final DownloadLink downloadLink) throws Exception {
        requestFileInformation(downloadLink);
        if (dllink == null) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        } else if ("".equals(dllink)) {
            dllink = downloadLink.getDownloadURL();
        } else if (dllink.contains(".hds.")) {
            throw new PluginException(LinkStatus.ERROR_FATAL, "HDS protocol not (yet) supported");
        }
        if (this.mediatype != null && !"video".equalsIgnoreCase(this.mediatype)) {
            // this will throw statserv errors.
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT, "Unsupported Media Type: " + this.mediatype);
        }
        if (dllink.startsWith("http")) {
            dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, dllink);
            if (dl.getConnection().getLongContentLength() == 0) {
                br.followConnection();
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            dl.startDownload();
        } else if (dllink.startsWith("rtmp")) {
            if (this.flashplayer == null) {
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            dl = new RTMPDownload(this, downloadLink, dllink);
            setupRTMPConnection(dllink, dl, this.flashplayer);
            ((RTMPDownload) dl).startDownload();
        } else {
            logger.severe("Plugin out of date for link: " + dllink);
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
    }

    private String getDllink(final String page) {
        String out = null;
        final String[] allContent = new Regex(page, PATTERN_FLV_FILE).getRow(0);
        for (final String c : allContent) {
            if (c != null) {
                out = c;
                break;
            }
        }
        return out;
    }

    @Override
    public void reset() {
    }

    @Override
    public void resetDownloadlink(final DownloadLink link) {
    }

    private void setupRTMPConnection(String stream, DownloadInterface dl, String fp) {
        jd.network.rtmp.url.RtmpUrlConnection rtmp = ((RTMPDownload) dl).getRtmpConnection();
        if (stream.contains("mp4:") && stream.contains("auth=")) {
            String pp = "mp4:" + stream.split("mp4:")[1];
            rtmp.setPlayPath(pp);
            rtmp.setUrl(stream.split("mp4:")[0]);
            rtmp.setApp("ondemand?ovpfv=2.0&" + new Regex(pp, "(auth=.*?)$").getMatch(0));
            rtmp.setSwfUrl(fp);
            rtmp.setResume(true);
        }
    }

}