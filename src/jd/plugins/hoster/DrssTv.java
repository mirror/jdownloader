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

import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import jd.PluginWrapper;
import jd.config.ConfigContainer;
import jd.config.ConfigEntry;
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
import jd.utils.locale.JDL;

import org.appwork.utils.formatter.TimeFormatter;

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "drss.tv" }, urls = { "http://(www\\.)?drssdecrypted\\.tv/sendung/\\d{2}\\-\\d{2}\\-\\d{4}/\\?video=\\d+" }, flags = { 2 })
public class DrssTv extends PluginForHost {

    public DrssTv(PluginWrapper wrapper) {
        super(wrapper);
        setConfigElements();
    }

    @Override
    public String getAGBLink() {
        return "http://www.drss.tv/index/agb/";
    }

    @SuppressWarnings("deprecation")
    public void correctDownloadLink(final DownloadLink link) {
        link.setUrlDownload(link.getDownloadURL().replace("drssdecrypted.tv/", "drss.tv/"));
    }

    /* Settings stuff */
    private static final String  ALLOW_TRAILER     = "ALLOW_TRAILER";
    private static final String  ALLOW_TEASER_PIC  = "ALLOW_TEASER_PIC";
    private static final String  ALLOW_GALLERY     = "ALLOW_GALLERY";
    private static final String  ALLOW_OTHERS      = "ALLOW_OTHERS";

    /* Connection stuff */
    private static final boolean free_resume       = true;
    private static final int     free_maxchunks    = 0;
    private static final int     free_maxdownloads = -1;

    private String               DLLINK            = null;

    @SuppressWarnings("deprecation")
    @Override
    public AvailableStatus requestFileInformation(final DownloadLink link) throws Exception {
        final String date_formatted;
        String title;
        if (link.getBooleanProperty("offline", false)) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        this.setBrowserExclusive();
        br.setFollowRedirects(true);
        br.getPage(link.getDownloadURL());
        if (br.getHttpConnection().getResponseCode() == 404) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        final DecimalFormat df = new DecimalFormat("00");
        final boolean is_trailer = link.getBooleanProperty("trailer", false);
        final String vimeo_direct = link.getStringProperty("vimeo_direct", null);
        final short part = Short.parseShort(new Regex(link.getDownloadURL(), "(\\d+)$").getMatch(0));
        String date = br.getRegex("class=\"subline\">Sendung vom (\\d{2}\\.\\d{2}\\.\\d{4})</span>").getMatch(0);
        if (date == null) {
            /* Only get date from url when site fails as url date is sometimes wrong/not accurate! */
            date = new Regex(link.getDownloadURL(), "sendung/(\\d{2}\\-\\d{2}\\-\\d{4})/").getMatch(0).replace("-", ".");
        }
        title = this.br.getRegex("class=\"text\\-cutted\">Komplette Sendung</h4>[\t\n\r ]+<p class=\"descr\">([^<>]*?)</p>").getMatch(0);
        if (inValidate(title)) {
            title = this.getSiteTitle();
        }
        if (link.getBooleanProperty("special_vimeo", false)) {
            /* Sometimes we got special vimeo 1080p URLs! */
            DLLINK = br.getRegex("data\\-url=\"(https?://player\\.vimeo\\.com/external/\\d+\\.hd\\.mp4[^<>\"]*?)\"").getMatch(0);
        } else if (vimeo_direct != null) {
            /* Special case: http://www.drss.tv/sendung/27-08-2015/ */
            final String vimeo_id = new Regex(vimeo_direct, "(\\d+)$").getMatch(0);
            final Browser br2 = new Browser();
            br2.getHeaders().put("Referer", link.getDownloadURL());
            br2.getPage(vimeo_direct);
            final String[][] qualities = jd.plugins.hoster.VimeoCom.getQualities(br2, vimeo_id);
            /*
             * Pick the highest quality - we know they got 3 qualities and it is in the middle - so this is not done very good but should be
             * okay for this plugin!
             */
            DLLINK = qualities[1][0];
        } else {
            DLLINK = br.getRegex("\"(http://[^<>\"]*?(\\.mp4|\\.flv|/flv))\"").getMatch(0);
        }
        if (title == null || date == null || DLLINK == null) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        final Regex finfo = new Regex(title, "(.*?)Staffel (\\d+).+Folge (\\d+).*?");
        final String name_actress = getNameActress();
        final String season_str = finfo.getMatch(1);
        final String episode_str = finfo.getMatch(2);
        date_formatted = formatDate(date);

        String filename = date_formatted + "_drsstv_";

        if (season_str != null && episode_str != null) {
            String series_id = "S" + df.format(Short.parseShort(season_str)) + "E" + df.format(Short.parseShort(episode_str));
            if (series_id.equals("S03E17") && "Jenny Sternchen".equals(name_actress)) {
                /* Workaround for one serverside wrong episode number: http://www.drss.tv/sendung/17-07-2015/ */
                series_id = "S03E18";
            }
            filename += series_id + "_";
        }
        if (is_trailer) {
            filename += "trailer_";
        } else {
            filename += "part_" + df.format(part) + "_";
        }
        if (!inValidate(name_actress)) {
            filename += name_actress;
        } else {
            filename += title;
        }

        filename = Encoding.htmlDecode(filename).trim();
        filename = encodeUnicode(filename);

        if (DLLINK.contains("medianac.nacamar.de/")) {
            /* Make this server happy */
            br.getHeaders().put("Accept-Encoding", null);
        }
        URLConnectionAdapter con = null;
        try {
            try {
                con = br.openHeadConnection(DLLINK);
            } catch (final BrowserException e) {
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            }
            if (!con.getContentType().contains("html")) {
                link.setDownloadSize(con.getLongContentLength());
            } else {
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            }
        } finally {
            try {
                con.disconnect();
            } catch (final Throwable e) {
            }
        }
        link.setFinalFileName(filename + ".mp4");
        return AvailableStatus.TRUE;
    }

    @Override
    public void handleFree(final DownloadLink downloadLink) throws Exception, PluginException {
        requestFileInformation(downloadLink);
        doFree(downloadLink, free_resume, free_maxchunks, "free_directlink");
    }

    /*
     * Streaming example premiumlink from this site: http://www.drss.tv/club/index/
     * http://streaming.drss.tv/videos/8c25ecd576e___nothere___45640420cbeeaae9509bd/8c25ecd576e45640420cbeeaae9509bd
     * .mp4?hash=ea1d74e72b5c42d02ef7da11dd95cca9
     */
    private void doFree(final DownloadLink downloadLink, final boolean resumable, final int maxchunks, final String directlinkproperty) throws Exception, PluginException {
        dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, DLLINK, resumable, maxchunks);
        if (dl.getConnection().getContentType().contains("html")) {
            br.followConnection();
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        downloadLink.setProperty(directlinkproperty, DLLINK);
        dl.startDownload();
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

    /** Finds the name of the actress - not that easy... */
    private String getNameActress() {
        final String site_title = getSiteTitle();
        String name_actress = this.br.getRegex("property=\"og:title\" content=\"([^<>\"/]*?) zu Gast \\- Rene Schwuchow Show\"").getMatch(0);
        if (name_actress == null) {
            name_actress = this.br.getRegex("property=\"og:title\" content=\"([^<>\"/]*?) \\- Rene Schwuchow Show\"").getMatch(0);
        }
        if (inValidate(name_actress) && site_title != null && site_title.contains("Staffel") || site_title.contains("Folge")) {
            name_actress = new Regex(site_title, "(.*?) zu Gast \\- Staffel (\\d+).+Folge (\\d+).*?").getMatch(0);
            if (name_actress == null) {
                name_actress = new Regex(site_title, "(.*?) \\- Staffel (\\d+).+Folge (\\d+).*?").getMatch(0);
            }
        } else if (inValidate(name_actress)) {
            /* No name found? Then our site title is probably */
            name_actress = site_title;
        }
        return name_actress;
    }

    private String getSiteTitle() {
        return this.br.getRegex("property=\"og:title\" content=\"([^<>\"/]*?)\"").getMatch(0);
    }

    private String formatDate(final String input) {
        final long date = TimeFormatter.getMilliSeconds(input, "dd.MM.yyyy", Locale.GERMAN);
        String formattedDate = null;
        final String targetFormat = "yyyy-MM-dd";
        Date theDate = new Date(date);
        try {
            final SimpleDateFormat formatter = new SimpleDateFormat(targetFormat);
            formattedDate = formatter.format(theDate);
        } catch (final Exception e) {
            /* prevent input error killing plugin */
            formattedDate = input;
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
    private boolean inValidate(final String s) {
        if (s == null || s.matches("\\s+") || s.equals("")) {
            return true;
        } else {
            return false;
        }
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return free_maxdownloads;
    }

    @Override
    public String getDescription() {
        return "JDownloader's drss.de Plugin helps downloading videoclips and photo galleries from drss.tv.";
    }

    private void setConfigElements() {
        getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_LABEL, "Einstellungen zum Video Download:"));
        getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, getPluginConfig(), ALLOW_TRAILER, JDL.L("plugins.hoster.drsstv.grabtrailer", "Trailer auch laden, wenn eine komplette Folge verfügbar ist?")).setDefaultValue(false));
        getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, getPluginConfig(), ALLOW_TEASER_PIC, JDL.L("plugins.hoster.drsstv.grabteaserpicture", "Titelbild laden?")).setDefaultValue(false));
        getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, getPluginConfig(), ALLOW_GALLERY, JDL.L("plugins.hoster.drsstv.grabgallery", "Photogallerie laden?")).setDefaultValue(false));
        getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, getPluginConfig(), ALLOW_OTHERS, JDL.L("plugins.hoster.drsstv.grabothers", "Andere Inhalte (z.B. 'Vor der Sendung'-Videos) laden'?")).setDefaultValue(false));
    }

    @Override
    public void reset() {
    }

    @Override
    public void resetDownloadlink(DownloadLink link) {
    }

}