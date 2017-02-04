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

import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.LinkedHashMap;

import jd.PluginWrapper;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;

import org.jdownloader.downloader.hls.HLSDownloader;
import org.jdownloader.plugins.components.hls.HlsContainer;
import org.jdownloader.scripting.JavaScriptEngineFactory;

@HostPlugin(revision = "$Revision$", interfaceVersion = 3, names = { "hotstar.com" }, urls = { "https?://(?:www\\.)?hotstar\\.com/.+/\\d{10}" })
public class HotstarCom extends PluginForHost {

    public HotstarCom(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public String getAGBLink() {
        return "http://www.hotstar.com/terms-of-use";
    }

    /* Possible values for "channel": ANDROID|IOS|TABLET|PCTV */
    /* Using "TABLET" will get us hls urls (instead of hds) */
    private static final String   CHANNEL   = "TABLET";

    private String                contentId = null;
    LinkedHashMap<String, Object> entries   = null;

    /** thanks goes to: https://github.com/biezom/hotstarsportslivestreamer/releases/ */
    @SuppressWarnings({ "deprecation", "unchecked" })
    @Override
    public AvailableStatus requestFileInformation(final DownloadLink link) throws Exception {
        contentId = new Regex(link.getDownloadURL(), "(\\d+)$").getMatch(0);
        link.setLinkID(contentId);
        this.setBrowserExclusive();
        br.setFollowRedirects(true);
        this.br.getPage("https://account.hotstar.com/AVS/besc?action=GetAggregatedContentDetails&channel=" + CHANNEL + "&contentId=" + this.contentId);
        if (br.getHttpConnection().getResponseCode() == 404) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        entries = (LinkedHashMap<String, Object>) JavaScriptEngineFactory.jsonToJavaObject(br.toString());
        final String errorDescription = (String) entries.get("errorDescription");
        if (!inValidate(errorDescription)) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        entries = (LinkedHashMap<String, Object>) JavaScriptEngineFactory.walkJson(entries, "resultObj/contentInfo/{0}");

        final DecimalFormat df = new DecimalFormat("00");
        String season_str = this.br.getRegex("\"categoryName\":\"Chapter (\\d+)\"").getMatch(0);
        if (season_str == null) {
            season_str = this.br.getRegex("s(\\d+)e\\d+").getMatch(0);
        }
        if (season_str == null) {
            season_str = this.br.getRegex("season (\\d+)").getMatch(0);
        }
        long season = -1;
        final long episode = JavaScriptEngineFactory.toLong(entries.get("episodeNumber"), -1);

        final String objectType = (String) entries.get("objectType");
        final String objectSubtype = (String) entries.get("objectSubtype");
        final long date = JavaScriptEngineFactory.toLong(entries.get("broadcastDate"), -1);
        final String description = (String) entries.get("description");
        String title = (String) entries.get("episodeTitle");
        if (inValidate(title)) {
            /* Fallback */
            title = this.contentId;
        }
        if (inValidate(objectType) || inValidate(objectSubtype)) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }

        /* WTF-case! */
        if (!objectType.equals("VIDEO")) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }

        title = Encoding.htmlDecode(title.trim());
        final String date_formatted = formatDate(date);
        String filename = "";
        if (date_formatted != null) {
            filename = date_formatted + "_";
        }
        filename += "_hotstar_" + title;
        /* Make nicer filenames in case we have a full episode of a series. */
        if (objectSubtype.equals("EPISODE") && episode != -1 && !inValidate(season_str)) {
            season = Long.parseLong(season_str);
            filename += "_S" + df.format(season) + "E" + df.format(episode);
        }
        filename += ".mp4";
        filename = encodeUnicode(filename);
        link.setFinalFileName(filename);

        if (description != null) {
            link.setComment(description);
        }

        return AvailableStatus.TRUE;
    }

    @SuppressWarnings("unchecked")
    @Override
    public void handleFree(final DownloadLink downloadLink) throws Exception, PluginException {
        requestFileInformation(downloadLink);
        /* This (first) request is not necessarily needed */
        this.br.getPage("https://account.hotstar.com/AVS/besc?action=KeepAlive&channel=PCTV&contentId=" + this.contentId + "&type=VOD");
        this.br.getPage("https://getcdn.hotstar.com/AVS/besc?action=GetCDN&asJson=Y&channel=" + CHANNEL + "&id=" + this.contentId + "&type=VOD");
        entries = (LinkedHashMap<String, Object>) JavaScriptEngineFactory.jsonToJavaObject(br.toString());
        final String hls_main_url = (String) JavaScriptEngineFactory.walkJson(entries, "resultObj/src");
        if (hls_main_url == null) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        this.br.getPage(hls_main_url);
        /* 2015-11-29: As far as I can tell there is no way around their GEO-block! */
        // X-ErrorType: geo-blocked
        if (this.br.getHttpConnection().getResponseCode() == 403) {
            throw new PluginException(LinkStatus.ERROR_FATAL, "GEO-blocked: This content is not downloadable in your country");
        }
        final HlsContainer hlsbest = HlsContainer.findBestVideoByBandwidth(HlsContainer.getHlsQualities(this.br));
        if (hlsbest == null) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        final String url_hls = hlsbest.getDownloadurl();
        checkFFmpeg(downloadLink, "Download a HLS Stream");
        dl = new HLSDownloader(downloadLink, br, url_hls);
        dl.startDownload();
    }

    private String formatDate(long date) {
        if (date == -1) {
            return null;
        }
        date = date * 1000;
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

    /**
     * Validates string to series of conditions, null, whitespace, or "". This saves effort factor within if/for/while statements
     *
     * @param s
     *            Imported String to match against.
     * @return <b>true</b> on valid rule match. <b>false</b> on invalid rule match.
     * @author raztoki
     */
    protected boolean inValidate(final String s) {
        if (s == null || s.matches("\\s+") || s.equals("")) {
            return true;
        } else {
            return false;
        }
    }

    @Override
    public void reset() {
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return -1;
    }

    @Override
    public void resetDownloadlink(final DownloadLink link) {
    }

}