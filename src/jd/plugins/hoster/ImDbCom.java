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

import jd.PluginWrapper;
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
import jd.utils.JDUtilities;

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "imdb.com" }, urls = { "http://(www\\.)?imdb\\.com/(video/(?!imdblink|internet\\-archive)[\\w\\-]+/vi\\d+|media/rm\\d+/(tt|nm|rg)\\d+)" })
public class ImDbCom extends PluginForHost {

    private String              dllink     = null;
    private static final String IDREGEX    = "(vi\\d+)$";
    private static final String TYPE_VIDEO = "http://(www\\.)?imdb\\.com/video/[\\w\\-]+/(vi|screenplay/)\\d+";
    private static final String TYPE_PHOTO = "http://(www\\.)?imdb\\.com/media/rm\\d+/[a-z]{2}\\d+";

    public ImDbCom(final PluginWrapper wrapper) {
        super(wrapper);
    }

    @SuppressWarnings("deprecation")
    @Override
    public void correctDownloadLink(final DownloadLink link) {
        if (link.getDownloadURL().matches(TYPE_VIDEO)) {
            link.setUrlDownload("http://www.imdb.com/video/screenplay/" + new Regex(link.getDownloadURL(), IDREGEX).getMatch(0));
        }
    }

    @Override
    public String getAGBLink() {
        return "http://www.imdb.com/help/show_article?conditions";
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return -1;
    }

    @Override
    public AvailableStatus requestFileInformation(final DownloadLink downloadLink) throws IOException, PluginException {
        setBrowserExclusive();
        br.setFollowRedirects(true);
        final String downloadURL = downloadLink.getDownloadURL();
        br.getPage(downloadURL);
        if (this.br.getHttpConnection().getResponseCode() == 404) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        String ending = null;
        String filename = null;
        if (downloadURL.matches(TYPE_PHOTO)) {
            if (!br.containsHTML("\"spinner\\-container\"")) {
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            }
            dllink = br.getRegex("id=\"primary\\-img\"[^\t\n\r]+src=\"(http://[^<>\"]*?)\"").getMatch(0);
            filename = br.getRegex("<div id=\"photo\\-caption\">([^<>\"]*?)</div>").getMatch(0);
            if (filename == null) {
                /* Fallback to url-filename */
                filename = new Regex(downloadURL, "/media/(.+)").getMatch(0).replace("/", "_");
            }
            if (filename == null || dllink == null) {
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            if (dllink.contains("@@")) {
                final String qualityPart = dllink.substring(dllink.lastIndexOf("@@") + 2);
                if (qualityPart != null) {
                    dllink = dllink.replace(qualityPart, "");
                }
            }
            filename = Encoding.htmlDecode(filename.trim());
            final String fid = new Regex(downloadLink.getDownloadURL(), "rm(\\d+)").getMatch(0);
            String artist = br.getRegex("itemprop=\\'url\\'>([^<>\"]*?)</a>").getMatch(0);
            if (artist != null) {
                filename = Encoding.htmlDecode(artist.trim()) + "_" + fid + "_" + filename;
            } else {
                filename = fid + "_" + filename;
            }
            ending = getFileNameExtensionFromString(dllink, ".jpg");
        } else {
            /*
             * get the fileName from main download link page because fileName on the /player subpage may be wrong
             */
            filename = br.getRegex("<title>(.*?) \\- IMDb</title>").getMatch(0);
            br.getPage(downloadURL + "/player");
            if (br.containsHTML("(<title>IMDb Video Player: </title>|This video is not available\\.)")) {
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            }
            if (filename == null) {
                filename = br.getRegex("<title>IMDb Video Player: (.*?)</title>").getMatch(0);
            }
            if (filename == null) {
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            br.getPage("http://www.imdb.com/video/imdb/" + new Regex(downloadLink.getDownloadURL(), IDREGEX).getMatch(0) + "/player?uff=3");
            dllink = br.getRegex("addVariable\\(\"file\", \"((http|rtmp).*?)\"\\)").getMatch(0);
            if (dllink == null) {
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            if (dllink.startsWith("rtmp")) {
                final String playPath = br.getRegex("addVariable\\(\"id\", \"(.*?)\"\\)").getMatch(0);
                if (playPath == null) {
                    throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                }
                dllink = dllink + "#" + playPath;
            }
            dllink = Encoding.htmlDecode(dllink);
            filename = filename.trim();
            ending = ".flv";
            if (dllink.contains(".mp4")) {
                ending = ".mp4";
            }
        }
        downloadLink.setFinalFileName(Encoding.htmlDecode(filename) + ending);
        if (!dllink.startsWith("rtmp")) {
            final Browser br2 = br.cloneBrowser();
            // In case the link redirects to the finallink
            br2.setFollowRedirects(true);
            URLConnectionAdapter con = null;
            try {
                con = openConnection(br2, dllink);
                if (!con.getContentType().contains("html")) {
                    downloadLink.setDownloadSize(con.getLongContentLength());
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
        return AvailableStatus.TRUE;
    }

    private URLConnectionAdapter openConnection(final Browser br, final String directlink) throws IOException {
        URLConnectionAdapter con;
        if (isJDStable()) {
            con = br.openGetConnection(directlink);
        } else {
            con = br.openHeadConnection(directlink);
            if (!con.isOK()) {
                br.followConnection();
                con = br.openGetConnection(directlink);
            }
        }
        return con;
    }

    private boolean isJDStable() {
        return System.getProperty("jd.revision.jdownloaderrevision") == null;
    }

    @Override
    public void handleFree(final DownloadLink downloadLink) throws Exception {
        requestFileInformation(downloadLink);
        if (dllink.startsWith("rtmp")) {
            if (isStableEnviroment()) {
                throw new PluginException(LinkStatus.ERROR_FATAL, "JD2 BETA needed!");
            }
            dl = new RTMPDownload(this, downloadLink, dllink);
            final jd.network.rtmp.url.RtmpUrlConnection rtmp = ((RTMPDownload) dl).getRtmpConnection();

            rtmp.setUrl(dllink.substring(0, dllink.indexOf("#")));
            rtmp.setPlayPath(dllink.substring(dllink.indexOf("#") + 1));
            rtmp.setApp(new Regex(dllink.substring(0, dllink.indexOf("#")), "[a-zA-Z]+://.*?/(.*?)$").getMatch(0));
            rtmp.setSwfVfy("http://www.imdb.com/images/js/app/video/mediaplayer.swf");
            rtmp.setResume(true);

            ((RTMPDownload) dl).startDownload();
        } else {
            int maxChunks = 0;
            if (downloadLink.getDownloadURL().matches(TYPE_VIDEO)) {
                maxChunks = 1;
            }
            dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, dllink, true, maxChunks);
            if (dl.getConnection().getContentType().contains("html")) {
                br.followConnection();
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            dl.startDownload();
        }
    }

    private boolean isStableEnviroment() {
        String prev = JDUtilities.getRevision();
        if (prev == null || prev.length() < 3) {
            prev = "0";
        } else {
            prev = prev.replaceAll(",|\\.", "");
        }
        final int rev = Integer.parseInt(prev);
        if (rev < 10000) {
            return true;
        }
        return false;
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