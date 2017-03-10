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

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

import jd.PluginWrapper;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;

import org.appwork.utils.formatter.TimeFormatter;
import org.jdownloader.downloader.hls.HLSDownloader;
import org.jdownloader.plugins.components.hls.HlsContainer;

@HostPlugin(revision = "$Revision$", interfaceVersion = 3, names = { "servustv.com" }, urls = { "https?://(?:www\\.)?servustv\\.com/(?:de|at)/Medien/.+" })
public class ServustvCom extends PluginForHost {

    public ServustvCom(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public String getAGBLink() {
        return "http://www.servustv.com/Nutzungsbedingungen";
    }

    @SuppressWarnings("deprecation")
    @Override
    public AvailableStatus requestFileInformation(final DownloadLink link) throws IOException, PluginException {
        this.setBrowserExclusive();
        br.setFollowRedirects(true);
        br.getPage(link.getDownloadURL());
        if (br.getHttpConnection().getResponseCode() == 404) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        final String episodenumber = new Regex(link.getDownloadURL(), "(\\d+)$").getMatch(0);
        String date = br.getRegex("itemprop=\"uploaddate \" content=\"(\\d+)\"").getMatch(0);
        if (date == null) {
            date = this.br.getRegex("itemprop=\"description\">(\\d{2}\\.\\d{2}\\.\\d{4})<").getMatch(0);
        }
        if (date == null) {
            date = this.br.getRegex("class=\"ato programm\\-datum\\-uhrzeit \" >[\t\n\r ]+<span>([^<>\"]*?\\d{1,2}\\.)</span>").getMatch(0);
        }
        if (date == null) {
            date = this.br.getRegex("Sendung vom (\\d{1,2}\\. [A-Za-z]+ \\d{4} \\| \\d{1,2}:\\d{1,2})").getMatch(0);
        }
        String title = br.getRegex("<h1 class=\"[^\"]+\">([^<>]+)</h1>").getMatch(0);
        if (title == null) {
            title = br.getRegex("itemprop=\"name\">([^<>\"]*?)<").getMatch(0);
        }
        if (title == null) {
            title = new Regex(link.getDownloadURL(), "edien/(.+)$").getMatch(0);
        }
        String episodename = this.br.getRegex("<h2 class=\"HeadlineSub Headline\\-\\-serif\">([^<>]+)</h2>").getMatch(0);
        if (episodename == null) {
            /* Description sometimes acts as a subtitle/episodename. */
            episodename = this.br.getRegex("itemprop=\"description\">([^<>]+)</span>").getMatch(0);
        }
        if (title == null) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        title = title.trim();
        final String date_formatted = formatDate(date);
        String filename = "";
        if (date_formatted != null) {
            filename = date_formatted + "_";
        }
        filename += "servustv_" + title;
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
    public void handleFree(final DownloadLink downloadLink) throws Exception, PluginException {
        requestFileInformation(downloadLink);
        String httpstream = this.br.getRegex("<meta property=\"twitter:player:stream\" content=\"(http[^<>\"]*?)\">").getMatch(0);
        if (httpstream == null) {
            httpstream = this.br.getRegex("\"(https?://stvmedia\\.pmd\\.servustv\\.com/media/hds/[^<>\"\\']+\\.mp4)\"").getMatch(0);
        }
        HlsContainer hlsbest = null;
        String videoid = br.getRegex("name=\"@videoPlayer\" value=\"(\\d+)\"").getMatch(0);
        if (videoid == null) {
            /* Age restricted videos can only be viewed between 8PM and 6AM --> This way we can usually download them anyways! */
            videoid = br.getRegex("data\\-videoid=\"(\\d+)\"").getMatch(0);
        }
        if (videoid == null && httpstream == null) {
            /* Hmm maybe age-restriction workaround failed */
            if (this.br.containsHTML("<img src=\"/img/content/FSK\\d+\\.jpg\" alt=\"FSK\\d+\"")) {
                throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Inhalt ist altersbeschränkt - Download wird später erneut versucht", 30 * 60 * 1000l);
            }
            /* Probably the user wants to download hasn't aired yet --> Wait and retry later! */
            throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Sendung wurde noch nicht ausgestrahlt", 60 * 60 * 1000l);
        }
        if (videoid != null) {
            /* Prefer hls as we might get a better videoquality. */
            br.getPage(jd.plugins.decrypter.BrightcoveDecrypter.getHlsMasterHttp(videoid));
            /* E.g. here, (mobile) HLS download is not possible: http://www.servustv.com/at/Medien/Spielberg-Musikfestival-20162 */
            hlsbest = HlsContainer.findBestVideoByBandwidth(HlsContainer.getHlsQualities(this.br));
        }
        if (hlsbest != null) {
            final String url_hls = hlsbest.getDownloadurl();
            checkFFmpeg(downloadLink, "Download a HLS Stream");
            dl = new HLSDownloader(downloadLink, br, url_hls);
            dl.startDownload();
        } else {
            if (httpstream == null) {
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            /* Use http as fallback. */
            dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, httpstream, true, 0);
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
        final long date;
        if (input.matches("\\d+")) {
            date = Long.parseLong(input) * 1000;
        } else if (input.matches("\\d{2}\\.\\d{2}\\.\\d{4}")) {
            date = TimeFormatter.getMilliSeconds(input, "dd.MM.yyyy", Locale.GERMAN);
        } else if (input.matches("\\d{1,2}\\. [A-Za-z]+ \\d{4} \\| \\d{1,2}:\\d{1,2}")) {
            date = TimeFormatter.getMilliSeconds(input, "dd. MMMM yyyy '|' HH:mm", Locale.GERMAN);
        } else {
            final Calendar cal = Calendar.getInstance();
            input += cal.get(cal.YEAR);
            date = TimeFormatter.getMilliSeconds(input, "E '|' dd.MM.yyyy", Locale.GERMAN);
        }
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