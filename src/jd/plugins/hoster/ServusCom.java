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

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.Locale;

import org.appwork.utils.StringUtils;
import org.appwork.utils.formatter.TimeFormatter;
import org.jdownloader.downloader.hls.HLSDownloader;
import org.jdownloader.plugins.components.hls.HlsContainer;
import org.jdownloader.scripting.JavaScriptEngineFactory;

import jd.PluginWrapper;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;

@HostPlugin(revision = "$Revision$", interfaceVersion = 3, names = { "servustv.com" }, urls = { "https?://(?:www\\.)?servus\\.com/(?:tv/videos/|(?:de|at)/p/[^/]+/)([A-Za-z0-9\\-]+)" })
public class ServusCom extends PluginForHost {
    public ServusCom(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public String getAGBLink() {
        return "http://www.servustv.com/Nutzungsbedingungen";
    }

    @Override
    public String rewriteHost(String host) {
        if ("servustv.com".equals(getHost())) {
            if (host == null || "servustv.com".equals(host)) {
                return "servus.com";
            }
        }
        return super.rewriteHost(host);
    }

    @Override
    public String getLinkID(final DownloadLink link) {
        final String linkid = getFID(link);
        if (linkid != null) {
            return this.getHost() + "://" + linkid;
        } else {
            return super.getLinkID(link);
        }
    }

    private String getFID(final DownloadLink link) {
        String fid = new Regex(link.getPluginPatternMatcher(), this.getSupportedLinks()).getMatch(0);
        if (fid != null) {
            fid = fid.toUpperCase();
        }
        return fid;
    }

    @SuppressWarnings("deprecation")
    @Override
    public AvailableStatus requestFileInformation(final DownloadLink link) throws Exception {
        this.setBrowserExclusive();
        br.setFollowRedirects(true);
        br.getPage("https://stv.rbmbtnx.net/api/v1/manifests/" + this.getFID(link) + "/metadata");
        if (br.getHttpConnection().getResponseCode() == 404) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        final LinkedHashMap<String, Object> entries = (LinkedHashMap<String, Object>) JavaScriptEngineFactory.jsonToJavaMap(br.toString());
        // final ArrayList<Object> ressourcelist = (ArrayList<Object>) entries.get("");
        final String episodenumber = new Regex(link.getDownloadURL(), "pisode\\-(\\d+)").getMatch(0);
        String date = (String) JavaScriptEngineFactory.walkJson(entries, "playability/*/{0}/startDate");
        String title = (String) entries.get("titleStv");
        String episodename = null;
        String labelGroup = (String) entries.get("labelGroup");
        if (StringUtils.isEmpty(labelGroup)) {
            labelGroup = "ServusTV";
        }
        if (StringUtils.isEmpty(title)) {
            /* Fallback */
            title = this.getFID(link);
        }
        title = title.trim();
        final String date_formatted = formatDate(date);
        String filename = "";
        if (date_formatted != null) {
            filename = date_formatted + "_";
        }
        filename += labelGroup + "_" + title;
        if (episodenumber != null && !title.contains(episodenumber)) {
            filename += "_" + episodenumber;
        }
        if (episodename != null) {
            filename += " - " + episodename;
        }
        filename = Encoding.htmlDecode(filename);
        filename += ".mp4";
        link.setFinalFileName(filename);
        return AvailableStatus.TRUE;
    }

    @Override
    public void handleFree(final DownloadLink link) throws Exception, PluginException {
        requestFileInformation(link);
        String url_hls = null;
        HlsContainer hlsbest = null;
        /*
         * 2017-10-04: It is unlikely that they still have official http urls (at this place) but we'll leave in these few lines of code
         * anyways.
         */
        String httpstream = null;
        if (httpstream == null) {
            /* 2017-10-04: Only hls available and it is very easy to create the master URL --> Do not access Brightcove stuff at all! */
            /* Use this to get some more information about the video [in json]: https://www.servus.com/at/p/<videoid>/personalize */
            final String hls_master = String.format("https://stv.rbmbtnx.net/api/v1/manifests/%s.m3u8", this.getFID(link));
            br.getPage(hls_master);
            hlsbest = HlsContainer.findBestVideoByBandwidth(HlsContainer.getHlsQualities(this.br));
            if (hlsbest == null) {
                /* No content available --> Probably the user wants to download hasn't aired yet --> Wait and retry later! */
                throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Sendung wurde noch nicht ausgestrahlt", 60 * 60 * 1000l);
            }
        }
        if (hlsbest != null) {
            url_hls = hlsbest.getDownloadurl();
            checkFFmpeg(link, "Download a HLS Stream");
            dl = new HLSDownloader(link, br, url_hls);
            dl.startDownload();
        } else {
            if (httpstream == null) {
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            /* Use http as fallback. */
            dl = jd.plugins.BrowserAdapter.openDownload(br, link, httpstream, true, 0);
            if (dl.getConnection().getContentType().contains("html")) {
                try {
                    if (dl.getConnection().getResponseCode() == 403) {
                        throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server error 403", 60 * 60 * 1000l);
                    } else if (dl.getConnection().getResponseCode() == 404) {
                        throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server error 404", 60 * 60 * 1000l);
                    }
                    br.followConnection();
                    throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                } finally {
                    try {
                        dl.getConnection().disconnect();
                    } catch (final Throwable e) {
                    }
                }
            }
            dl.startDownload();
        }
    }

    @SuppressWarnings({ "static-access" })
    private String formatDate(String input) {
        if (input == null) {
            return null;
        }
        String formattedDate = null;
        long date = 0;
        if (input.matches("\\d+")) {
            date = Long.parseLong(input) * 1000;
        } else if (input.matches("\\d{2}\\.\\d{2}\\.\\d{4}")) {
            date = TimeFormatter.getMilliSeconds(input, "dd.MM.yyyy", Locale.GERMAN);
        } else if (input.matches("\\d{1,2}\\. [A-Za-z]+ \\d{4} \\| \\d{1,2}:\\d{1,2}")) {
            date = TimeFormatter.getMilliSeconds(input, "dd. MMMM yyyy '|' HH:mm", Locale.GERMAN);
        } else if (input.matches("\\d{4}-\\d{2}-\\d{2}.+")) {
            /* New 2019-12-04 */
            formattedDate = new Regex(input, "^(\\d{4}-\\d{2}-\\d{2})").getMatch(0);
        } else {
            final Calendar cal = Calendar.getInstance();
            input += cal.get(cal.YEAR);
            date = TimeFormatter.getMilliSeconds(input, "E '|' dd.MM.yyyy", Locale.GERMAN);
        }
        if (formattedDate == null) {
            final String targetFormat = "yyyy-MM-dd";
            Date theDate = new Date(date);
            try {
                final SimpleDateFormat formatter = new SimpleDateFormat(targetFormat);
                formattedDate = formatter.format(theDate);
            } catch (Exception e) {
                /* prevent input error killing plugin */
                formattedDate = input;
            }
        }
        return formattedDate;
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