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
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

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

import org.appwork.utils.formatter.TimeFormatter;

@HostPlugin(revision = "$Revision$", interfaceVersion = 3, names = { "wdrmaus.de" }, urls = { "https?://(?:www\\.)?wdrmaus\\.de/.+" }) 
public class WdrDeMediathekWdrmaus extends PluginForHost {

    public WdrDeMediathekWdrmaus(PluginWrapper wrapper) {
        super(wrapper);
    }

    /* DEV NOTES */
    // Tags:
    // protocol: no https
    // other:

    /* Extension which will be used if no correct extension is found */
    private static final String  default_Extension = ".mp4";
    /* Connection stuff */
    private static final boolean free_resume       = true;
    private static final int     free_maxchunks    = 0;
    private static final int     free_maxdownloads = -1;

    private String               dllink            = null;
    private boolean              server_issues     = false;

    @Override
    public String getAGBLink() {
        return "http://www.wdrmaus.de/hilfe.php5";
    }

    @SuppressWarnings("deprecation")
    @Override
    public AvailableStatus requestFileInformation(final DownloadLink link) throws IOException, PluginException {
        dllink = null;
        server_issues = false;
        this.setBrowserExclusive();
        br.setFollowRedirects(true);
        final String url_filename = new Regex(link.getDownloadURL(), "wdrmaus\\.de/(.+)").getMatch(0).replace("/", "_").replace("#", "").replaceAll("(\\.php\\d+)", "");
        link.setName(url_filename + ".mp4");
        String filename = null;
        String date = null;
        if (link.getDownloadURL().contains(".php")) {
            br.getPage(link.getDownloadURL().replace("https://", "http://"));
            if (br.getHttpConnection().getResponseCode() == 404) {
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            }
            final Regex hds_convert = this.br.getRegex("/CMS2010/md[a-z0-9]+/ondemand/([a-z]+)/(fsk\\d+/\\d+/\\d+)/,([a-z0-9_,]+),\\.mp4\\.csmil/");
            String region = hds_convert.getMatch(0);
            final String fsk_url = hds_convert.getMatch(1);
            final String quality_string = hds_convert.getMatch(2);
            if (region == null || fsk_url == null || quality_string == null) {
                /* Seems like the url the user added does not contain any downloadable content! */
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            }
            region = jd.plugins.decrypter.WdrDeDecrypt.correctRegionString(region);
            /* Avoid HDS */
            final String[] qualities = quality_string.split(",");
            if (qualities == null || qualities.length == 0) {
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            /* Build highest-quality HTTP URL */
            dllink = "http://ondemand-ww.wdr.de/medp/" + fsk_url + "/" + qualities[0] + ".mp4";
            date = this.br.getRegex("Sendedatum: (\\d{1,2}\\.\\d{1,2}\\.\\d{4})").getMatch(0);
            filename = this.br.getRegex("<title>([^<>\"]+) \\- Die Seite mit der Maus \\- WDR Fernsehen</title>").getMatch(0);
        } else if (link.getDownloadURL().matches("^https?://(?:www\\.)?wdrmaus\\.de/elefantenseite/#/[^/]+$")) {
            final String id_target = new Regex(link.getDownloadURL(), "([^/]+)$").getMatch(0);
            this.br.getPage("http://www.wdrmaus.de/elefantenseite/data/tableOfContents.php5");
            final String[] pages = this.br.getRegex("<page>(.*?)</page>").getColumn(0);
            if (pages == null || pages.length == 0) {
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            }
            String continue_url = null;
            String id_temp = null;
            String title_temp = null;
            for (final String pagexml : pages) {
                id_temp = new Regex(pagexml, "<id>(.*?)</id>").getMatch(0);
                title_temp = new Regex(pagexml, "<title>(.*?)</title>").getMatch(0);
                continue_url = new Regex(pagexml, "<xmlPath>(.*?)</xmlPath>").getMatch(0);
                if (id_temp == null || continue_url == null) {
                    continue;
                }
                id_temp = id_temp.replace("<![CDATA[", "").replace("]]>", "");
                if (id_temp.equals(id_target)) {
                    break;
                }
            }
            if (continue_url == null) {
                /* Probably older offline content */
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            }
            continue_url = continue_url.replace("<![CDATA[", "").replace("]]>", "");
            if (title_temp != null) {
                title_temp = title_temp.replace("<![CDATA[", "").replace("]]>", "");
                filename = title_temp;
            }
            if (!continue_url.startsWith("http")) {
                continue_url = "http://www.wdrmaus.de/elefantenseite/" + continue_url;
            }
            this.br.getPage(continue_url);
            date = this.br.getRegex("<pubstart>(.*?)</pubstart>").getMatch(0);
            continue_url = this.br.getRegex("<zmdb_url>(https?://[^<>\"]+)</zmdb_url>").getMatch(0);
            if (continue_url == null) {
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            this.br.getPage(continue_url);
            final Regex hds_convert = this.br.getRegex("adaptiv\\.wdr\\.de/[a-z0-9]+/med[a-z0-9]+/([a-z]{2})/(fsk\\d+/\\d+/\\d+)/,([a-z0-9_,]+),\\.mp4\\.csmil/");
            String region = hds_convert.getMatch(0);
            final String fsk_url = hds_convert.getMatch(1);
            final String quality_string = hds_convert.getMatch(2);
            if (region == null || fsk_url == null || quality_string == null) {
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            region = jd.plugins.decrypter.WdrDeDecrypt.correctRegionString(region);
            /* Avoid HDS */
            final String[] qualities = quality_string.split(",");
            if (qualities == null || qualities.length == 0) {
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            /* Build highest-quality HTTP URL */
            dllink = "http://ondemand-ww.wdr.de/medp/" + fsk_url + "/" + qualities[0] + ".mp4";
            if (date != null) {
                date = date.replace("<![CDATA[", "").replace("]]>", "");
            }
        } else {
            /* Unsupported/invalid linktype */
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        if (filename == null) {
            filename = url_filename;
        }
        if (filename == null || dllink == null) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        filename = "wdrmaus_" + filename;
        final String date_formatted = formatDate(date);
        if (date_formatted != null) {
            filename = date_formatted + "_" + filename;
        }
        filename = Encoding.htmlDecode(filename);
        filename = filename.trim();
        filename = encodeUnicode(filename);
        final String ext = default_Extension;
        if (!filename.endsWith(ext)) {
            filename += ext;
        }
        link.setFinalFileName(filename);
        final Browser br2 = br.cloneBrowser();
        // In case the link redirects to the finallink
        br2.setFollowRedirects(true);
        URLConnectionAdapter con = null;
        try {
            con = br2.openHeadConnection(dllink);
            if (!con.getContentType().contains("html")) {
                link.setDownloadSize(con.getLongContentLength());
                link.setProperty("directlink", dllink);
            } else {
                server_issues = true;
            }
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
        if (server_issues) {
            throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Unknown server error", 10 * 60 * 1000l);
        } else if (dllink == null) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
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

    private String formatDate(String input) {
        if (input == null) {
            return null;
        }
        final long date;
        if (input.matches("\\d{2}\\.\\d{2}\\.\\d{4}")) {
            /* From html */
            date = TimeFormatter.getMilliSeconds(input, "dd.MM.yyyy", Locale.GERMAN);
        } else {
            /* From xml, key 'pubstart' */
            date = TimeFormatter.getMilliSeconds(input, "yyyy-MM-dd HH:mm:ss", Locale.GERMAN);
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
