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
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map.Entry;

import org.appwork.utils.formatter.TimeFormatter;
import org.jdownloader.scripting.JavaScriptEngineFactory;

import jd.PluginWrapper;
import jd.http.Browser;
import jd.http.Browser.BrowserException;
import jd.http.URLConnectionAdapter;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;

@HostPlugin(revision = "$Revision$", interfaceVersion = 3, names = { "wsj.com" }, urls = { "https?://(?:www\\.)?((?:wsj|barrons)\\.com/video/[^/]+/[A-F0-9]{8}\\-[A-F0-9]{4}\\-[A-F0-9]{4}\\-[A-F0-9]{4}\\-[A-F0-9]{12}\\.html|allthingsd\\.com/video/\\?video_id=[A-F0-9]{8}\\-[A-F0-9]{4}\\-[A-F0-9]{4}\\-[A-F0-9]{4}\\-[A-F0-9]{12})" })
public class WsjCom extends PluginForHost {

    public WsjCom(PluginWrapper wrapper) {
        super(wrapper);
    }

    /* DEV NOTES */
    // Tags:
    // protocol: no https
    // other:
    /*
     * E.g. HTTP- and HLS URL comparison:
     *
     *
     * http://www.wsj.com/video/mossberg-reviews-the-roku-3/3B86D721-7315-494C-BB6A-44A0B13DDAEE.html
     *
     * http://m.wsj.net/video/20130305/030513ptechroku/030513ptechroku_v2_ec2564k.mp4
     *
     *
     * http://wsjvod-i.akamaihd.net/i/video/20130305/030513ptechroku/030513ptechroku_v2_ec,464,174,264,664,1264,1864,2564,k.mp4.csmil/
     * master. m3u8
     */

    /* Connection stuff */
    private static final boolean free_resume       = true;
    private static final int     free_maxchunks    = 0;
    private static final int     free_maxdownloads = -1;

    private String               dllink            = null;

    @Override
    public String getAGBLink() {
        return "http://www.wsj.com/policy/privacy-policy?mod=video";
    }

    @SuppressWarnings({ "unchecked", "deprecation" })
    @Override
    public AvailableStatus requestFileInformation(final DownloadLink downloadLink) throws Exception {
        final String vid = getVIDEOID(downloadLink);
        final String domain = new Regex(downloadLink.getDownloadURL(), "https?://(?:www\\.)?([^/]+)\\.com/").getMatch(0);
        dllink = null;
        this.setBrowserExclusive();
        br.setFollowRedirects(true);
        downloadLink.setLinkID(vid);
        br.getPage("http://video-api.wsj.com/api-video/find_all_videos.asp?type=guid&count=1&query=" + vid + "&fields=video174kMP4Url,video320kMP4Url,video664kMP4Url,video1264kMP4Url,video1864kMP4Url,video2564kMP4Url,hls,adZone,thumbnailList,guid,state,secondsUntilStartTime,author,description,name,linkURL,videoStillURL,duration,videoURL,adCategory,catastrophic,linkShortURL,doctypeID,youtubeID,titletag,rssURL,wsj-section,wsj-subsection,allthingsd-section,allthingsd-subsection,sm-section,sm-subsection,provider,formattedCreationDate,keywords,keywordsOmniture,column,editor,emailURL,emailPartnerID,showName,omnitureProgramName,omnitureVideoFormat,linkRelativeURL,touchCastID,omniturePublishDate");
        if (this.br.toString().length() < 100 || br.getHttpConnection().getResponseCode() == 404) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        LinkedHashMap<String, Object> entries = (LinkedHashMap<String, Object>) JavaScriptEngineFactory.jsonToJavaObject(this.br.toString());
        entries = (LinkedHashMap<String, Object>) JavaScriptEngineFactory.walkJson(entries, "items/{0}");
        final String description = (String) entries.get("description");
        final String date = (String) entries.get("formattedCreationDate");
        String filename = (String) entries.get("name");

        int bitrate_max = 0;
        int bitrate_temp = 0;
        String dllink_temp = null;
        final Iterator<Entry<String, Object>> it = entries.entrySet().iterator();
        while (it.hasNext()) {
            final Entry<String, Object> ipentry = it.next();
            final String qualityname = ipentry.getKey();
            if (qualityname.matches("video\\d+kMP4Url")) {
                bitrate_temp = Integer.parseInt(new Regex(qualityname, "video(\\d+)kMP4Url").getMatch(0));
                dllink_temp = (String) ipentry.getValue();
                if (bitrate_temp > bitrate_max) {
                    bitrate_max = bitrate_temp;
                    dllink = dllink_temp;
                }
            }

        }

        if (filename == null || dllink == null || date == null) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        final String date_formatted = formatDate(date);
        if (downloadLink.getComment() == null && description != null) {
            downloadLink.setComment(description);
        }
        dllink = Encoding.htmlDecode(dllink);
        filename = Encoding.htmlDecode(filename);
        filename = filename.trim();
        filename = date_formatted + "_" + domain + "_" + encodeUnicode(filename);
        String ext = getFileNameExtensionFromString(dllink, ".mp4");
        if (!filename.endsWith(ext)) {
            filename += ext;
        }
        downloadLink.setFinalFileName(filename);
        final Browser br2 = br.cloneBrowser();
        // In case the link redirects to the finallink
        br2.setFollowRedirects(true);
        URLConnectionAdapter con = null;
        try {
            try {
                con = br2.openHeadConnection(dllink);
            } catch (final BrowserException e) {
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            }
            if (!con.getContentType().contains("html")) {
                downloadLink.setDownloadSize(con.getLongContentLength());
            } else {
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            }
            downloadLink.setProperty("directlink", dllink);
            return AvailableStatus.TRUE;
        } finally {
            try {
                con.disconnect();
            } catch (final Throwable e) {
            }
        }
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

    @SuppressWarnings("deprecation")
    private String getVIDEOID(final DownloadLink dl) {
        return new Regex(dl.getDownloadURL(), "([A-F0-9]{8}\\-[A-F0-9]{4}\\-[A-F0-9]{4}\\-[A-F0-9]{4}\\-[A-F0-9]{12})(\\.html)?$").getMatch(0);
    }

    private String formatDate(final String input) {
        final long date = TimeFormatter.getMilliSeconds(input, "MM/dd/yyyy hh:mm:ss aa", Locale.US);
        String formattedDate = null;
        final String targetFormat = "yyyy-MM-dd";
        Date theDate = new Date(date);
        try {
            final SimpleDateFormat formatter = new SimpleDateFormat(targetFormat);
            formattedDate = formatter.format(theDate);
        } catch (Exception e) {
            /* prevent input error killing plugin */
            formattedDate = input;
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
