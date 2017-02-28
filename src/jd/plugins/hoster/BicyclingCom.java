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

import java.text.SimpleDateFormat;
import java.util.Date;

import jd.PluginWrapper;
import jd.nutils.encoding.Encoding;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;

import org.jdownloader.plugins.components.antiDDoSForHost;

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "bicycling.com" }, urls = { "http://(?:www\\.)?bicycling\\.com/video/[a-z0-9\\-_]+" })
public class BicyclingCom extends antiDDoSForHost {

    public BicyclingCom(PluginWrapper wrapper) {
        super(wrapper);
    }

    /* DEV NOTES */
    // Tags:
    // protocol: no https
    // other:

    /* Connection stuff */
    private static final boolean free_resume       = true;
    private static final int     free_maxchunks    = 0;
    private static final int     free_maxdownloads = -1;

    private String               dllink            = null;

    @Override
    public String getAGBLink() {
        return "http://www.bicycling.com/";
    }

    @SuppressWarnings({ "deprecation" })
    @Override
    public AvailableStatus requestFileInformation(final DownloadLink downloadLink) throws Exception {
        dllink = null;
        this.setBrowserExclusive();
        br.setFollowRedirects(true);
        getPage(downloadLink.getDownloadURL());
        if (br.getHttpConnection().getResponseCode() == 404 || !this.br.containsHTML("class=\"video_page\"")) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        String playerID = br.getRegex("name=\"playerID\" value=\"([^<>\"]*?)\"").getMatch(0);
        if (playerID == null) {
            playerID = "1686393384001";
        }
        String playerKey = br.getRegex("name=\"playerKey\" value=\"([^<>\"]*?)\"").getMatch(0);
        if (playerKey == null) {
            playerKey = "AQ~~,AAAAAB9cZG8~,VsmvaF3_bYDedNqkpR7gMGQyInJKsvED";
        }
        String publisherID = br.getRegex("name=\"publisherID\" value=\"([^<>\"]*?)\"").getMatch(0);
        if (publisherID == null) {
            publisherID = "110743091001";
        }
        final String videoID = br.getRegex("name=\"@videoPlayer\" value=\"(\\d+)\"").getMatch(0);
        if (videoID == null) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        final String flashID = "myExperience" + videoID;

        final String brightcove_URL = "http://c.brightcove.com/services/viewer/htmlFederated?&width=340&height=192&flashID=" + flashID + "&includeAPI=true&templateLoadHandler=templateLoaded&templateReadyHandler=playerReady&bgcolor=%23FFFFFF&htmlFallback=true&playerID=" + playerID + "&publisherID=" + publisherID + "&playerKey=" + Encoding.urlEncode(playerKey) + "&isVid=true&isUI=true&dynamicStreaming=true&optimizedContentLoad=true&wmode=transparent&%40videoPlayer=" + videoID + "&allowScriptAccess=always";
        getPage(brightcove_URL);
        final jd.plugins.decrypter.BrightcoveDecrypter.BrightcoveClipData bestBrightcoveVersion = jd.plugins.decrypter.BrightcoveDecrypter.findBestVideoHttpByFilesize(this, this.br);

        String filename = bestBrightcoveVersion.getDisplayName();
        if (bestBrightcoveVersion == null || bestBrightcoveVersion.getCreationDate() == -1 || filename == null || bestBrightcoveVersion.getDownloadURL() == null) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        dllink = bestBrightcoveVersion.getDownloadURL();
        final String date_formatted = formatDate(bestBrightcoveVersion.getCreationDate());
        filename = Encoding.htmlDecode(filename);
        filename = filename.trim();
        filename = encodeUnicode(filename);
        filename = date_formatted + "_bicycling_com_" + filename;
        final String ext = getFileNameExtensionFromString(dllink, ".mp4");
        if (!filename.endsWith(ext)) {
            filename += ext;
        }
        downloadLink.setFinalFileName(filename);
        downloadLink.setDownloadSize(bestBrightcoveVersion.getFilesize());
        return AvailableStatus.TRUE;
    }

    @Override
    public void handleFree(final DownloadLink downloadLink) throws Exception {
        requestFileInformation(downloadLink);
        dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, dllink, free_resume, free_maxchunks);
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

    private String formatDate(final long date) {
        String formattedDate = null;
        final String targetFormat = "yyyy-MM-dd";
        Date theDate = new Date(date);
        try {
            final SimpleDateFormat formatter = new SimpleDateFormat(targetFormat);
            formattedDate = formatter.format(theDate);
        } catch (Exception e) {
            /* prevent input error killing plugin */
            formattedDate = Long.toString(date);
        }
        return formattedDate;
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
