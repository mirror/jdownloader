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
import java.text.DecimalFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import jd.PluginWrapper;
import jd.config.ConfigContainer;
import jd.config.ConfigEntry;
import jd.config.SubConfiguration;
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
import jd.utils.locale.JDL;

import org.appwork.utils.formatter.TimeFormatter;

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "bandcamp.com" }, urls = { "http://(www\\.)?[a-z0-9\\-]+\\.bandcamp\\.com/track/[a-z0-9\\-_]+" }, flags = { 2 })
public class BandCampCom extends PluginForHost {

    private String DLLINK    = null;
    private String userAgent = null;

    public BandCampCom(PluginWrapper wrapper) {
        super(wrapper);
        this.setConfigElements();
    }

    public static final String FASTLINKCHECK        = "FASTLINKCHECK";
    public static final String CUSTOM_DATE          = "CUSTOM_DATE";
    public static final String CUSTOM_FILENAME      = "CUSTOM_FILENAME";
    public static final String GRABTHUMB            = "GRABTHUMB";
    public static final String CUSTOM_PACKAGENAME   = "CUSTOM_PACKAGENAME";
    public static final String FILENAMELOWERCASE    = "FILENAMELOWERCASE";
    public static final String PACKAGENAMELOWERCASE = "PACKAGENAMELOWERCASE";
    public static final String FILENAMESPACE        = "FILENAMESPACE";
    public static final String PACKAGENAMESPACE     = "PACKAGENAMESPACE";
    public static final String CLEANPACKAGENAME     = "CLEANPACKAGENAME";

    @Override
    public String getAGBLink() {
        return "http://bandcamp.com/terms_of_use";
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return -1;
    }

    @Override
    public void handleFree(DownloadLink downloadLink) throws Exception {
        requestFileInformation(downloadLink);
        dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, DLLINK, true, 0);
        if (dl.getConnection().getContentType().contains("html")) {
            br.followConnection();
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        dl.startDownload();
    }

    @Override
    public AvailableStatus requestFileInformation(final DownloadLink downloadLink) throws IOException, PluginException, ParseException {
        this.setBrowserExclusive();
        if (userAgent == null) {
            /* we first have to load the plugin, before we can reference it */
            JDUtilities.getPluginForHost("mediafire.com");
            userAgent = jd.plugins.hoster.MediafireCom.stringUserAgent();
        }
        br.getHeaders().put("User-Agent", userAgent);
        br.setFollowRedirects(true);
        final Browser br2 = br.cloneBrowser();
        URLConnectionAdapter con = null;
        try {
            con = br.openGetConnection(downloadLink.getDownloadURL());
            if (!con.getContentType().contains("html")) {
                DLLINK = downloadLink.getDownloadURL();
                downloadLink.setDownloadSize(con.getLongContentLength());
                downloadLink.setFinalFileName(Encoding.htmlDecode(getFileNameFromHeader(con)));
                return AvailableStatus.TRUE;
            } else {
                br.followConnection();
            }

        } catch (final Exception e) {
        }
        try {
            con.disconnect();
        } catch (Throwable e) {
        }
        if (br.containsHTML("(>Sorry, that something isn\\'t here|>start at the beginning</a> and you\\'ll certainly find what)")) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        DLLINK = br.getRegex("\"file\":.*?\"(http:.*?)\"").getMatch(0);
        logger.info("DLLINK = " + DLLINK);
        if (DLLINK == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        DLLINK = Encoding.htmlDecode(DLLINK).replace("\\", "");
        if (!downloadLink.getBooleanProperty("fromdecrypter", false)) {
            String tracknumber = br.getRegex("\"track_number\":(\\d+)").getMatch(0);
            if (tracknumber == null) tracknumber = "1";
            final int trackNum = Integer.parseInt(tracknumber);
            DecimalFormat df = new DecimalFormat("00");
            if (trackNum > 999)
                df = new DecimalFormat("0000");
            else if (trackNum > 99) df = new DecimalFormat("000");
            final String filename = br.getRegex("\"title\":\"([^<>\"]*?)\"").getMatch(0);
            String date = br.getRegex("<meta itemprop=\"datePublished\" content=\"(\\d+)\"/>").getMatch(0);
            if (date == null) {
                date = br.getRegex("\"publish_date\":\"([^<>\"]*?)\"").getMatch(0);
                if (date != null) date = Long.toString(TimeFormatter.getMilliSeconds(date, "dd MMM yyyy hh:mm:ss Z", Locale.ENGLISH));
            }
            final Regex inforegex = br.getRegex("<title>(.*?) \\| (.*?)</title>");
            final String artist = inforegex.getMatch(1);
            final String albumname = inforegex.getMatch(0);
            downloadLink.setProperty("fromdecrypter", true);
            downloadLink.setProperty("directdate", Encoding.htmlDecode(date.trim()));
            downloadLink.setProperty("directartist", Encoding.htmlDecode(artist.trim()));
            downloadLink.setProperty("directalbum", Encoding.htmlDecode(albumname.trim()));
            downloadLink.setProperty("directname", Encoding.htmlDecode(filename.trim()));
            downloadLink.setProperty("type", "mp3");
            downloadLink.setProperty("directtracknumber", df.format(trackNum));
        }
        final String filename = getFormattedFilename(downloadLink);
        // In case the link redirects to the finallink
        br2.setFollowRedirects(true);
        try {
            con = br2.openGetConnection(DLLINK);
            if (!con.getContentType().contains("html"))
                downloadLink.setDownloadSize(con.getLongContentLength());
            else
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            return AvailableStatus.TRUE;
        } finally {
            try {
                con.disconnect();
            } catch (Throwable e) {
            }
        }
    }

    public String getFormattedFilename(final DownloadLink downloadLink) throws ParseException {
        final String songTitle = downloadLink.getStringProperty("directname", null);
        final String tracknumber = downloadLink.getStringProperty("directtracknumber", null);
        final String artist = downloadLink.getStringProperty("directartist", null);
        final String album = downloadLink.getStringProperty("directalbum", null);
        final String date = downloadLink.getStringProperty("directdate", null);
        final SubConfiguration cfg = SubConfiguration.getConfig("bandcamp.com");
        String formattedFilename = cfg.getStringProperty(CUSTOM_FILENAME, defaultCustomFilename);
        if (formattedFilename == null || formattedFilename.equals("")) formattedFilename = defaultCustomFilename;
        if (!formattedFilename.contains("*songtitle*") || !formattedFilename.contains("*ext*")) formattedFilename = defaultCustomFilename;
        String ext = downloadLink.getStringProperty("type", null);
        if (ext != null)
            ext = "." + ext;
        else
            ext = ".mp3";

        String formattedDate = null;
        if (date != null && formattedFilename.contains("*date*")) {
            final String userDefinedDateFormat = cfg.getStringProperty(CUSTOM_DATE, "dd.MM.yyyy_HH-mm-ss");
            SimpleDateFormat formatter = new SimpleDateFormat("yyyyMMdd");
            Date dateStr = formatter.parse(date);

            formattedDate = formatter.format(dateStr);
            Date theDate = formatter.parse(formattedDate);

            if (userDefinedDateFormat != null) {
                try {
                    formatter = new SimpleDateFormat(userDefinedDateFormat);
                    formattedDate = formatter.format(theDate);
                } catch (Exception e) {
                    // prevent user error killing plugin.
                    formattedDate = "";
                }
            }
            if (formattedDate != null)
                formattedFilename = formattedFilename.replace("*date*", formattedDate);
            else
                formattedFilename = formattedFilename.replace("*date*", "");
        }
        if (formattedFilename.contains("*tracknumber*") && tracknumber != null) {
            formattedFilename = formattedFilename.replace("*tracknumber*", tracknumber);
        } else {
            formattedFilename = formattedFilename.replace("*tracknumber*", "");
        }
        if (formattedFilename.contains("*artist*") && artist != null) {
            formattedFilename = formattedFilename.replace("*artist*", artist);
        } else {
            formattedFilename = formattedFilename.replace("*artist*", "");
        }
        if (formattedFilename.contains("*album*") && album != null) {
            formattedFilename = formattedFilename.replace("*album*", album);
        } else {
            formattedFilename = formattedFilename.replace("*album*", "");
        }
        formattedFilename = formattedFilename.replace("*ext*", ext);
        // Insert filename at the end to prevent errors with tags
        formattedFilename = formattedFilename.replace("*songtitle*", songTitle);

        if (cfg.getBooleanProperty(jd.plugins.hoster.BandCampCom.FILENAMELOWERCASE, false)) formattedFilename = formattedFilename.toLowerCase();
        if (cfg.getBooleanProperty(jd.plugins.hoster.BandCampCom.FILENAMESPACE, false)) formattedFilename = formattedFilename.replace(" ", "_");

        return formattedFilename;
    }

    @Override
    public String getDescription() {
        return "JDownloader's bandcamp.com plugin helps downloading videoclips. JDownloader provides settings for the filenames.";
    }

    private final static String defaultCustomFilename    = "*tracknumber*.*artist* - *songtitle**ext*";
    private final static String defaultCustomPackagename = "*artist* - *album*";

    private void setConfigElements() {
        getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, getPluginConfig(), FASTLINKCHECK, JDL.L("plugins.hoster.bandcampcom.fastlinkcheck", "Activate fast linkcheck (filesize won't be shown in linkgrabber)?")).setDefaultValue(false));
        getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_SEPARATOR));
        getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, getPluginConfig(), GRABTHUMB, JDL.L("plugins.hoster.bandcampcom.grabthumb", "Grab thumbnail (.jpg)?")).setDefaultValue(false));
        getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_SEPARATOR));
        getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_LABEL, "Customize the filename properties:"));
        getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_TEXTFIELD, getPluginConfig(), CUSTOM_DATE, JDL.L("plugins.hoster.bandcampcom.customdate", "Define how the date should look.")).setDefaultValue("dd.MM.yyyy_HH-mm-ss"));
        getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_SEPARATOR));
        getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_LABEL, "Customize the filename properties:"));
        getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_SEPARATOR));
        getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_LABEL, "Customize the filename! Example: '*artist*_*date*_*songtitle**ext*'"));
        getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_TEXTFIELD, getPluginConfig(), CUSTOM_FILENAME, JDL.L("plugins.hoster.bandcampcom.customfilename", "Define how the filenames should look:")).setDefaultValue(defaultCustomFilename));
        final StringBuilder sb = new StringBuilder();
        sb.append("Explanation of the available tags:\r\n");
        sb.append("*artist* = artist of the album\r\n");
        sb.append("*album* = artist of the album\r\n");
        sb.append("*tracknumber* = number of the track\r\n");
        sb.append("*songtitle* = name of the song without extension\r\n");
        sb.append("*ext* = the extension of the file, in this case usually '.mp3'\r\n");
        sb.append("*date* = date when the album was released - appears in the user-defined format above");
        getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_LABEL, sb.toString()));
        getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_SEPARATOR));
        getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_LABEL, "Customize the packagename for playlists and '[a-z0-9\\-]+.bandcamp.com/album/' links! Example: '*artist* - *album*':"));
        getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_TEXTFIELD, getPluginConfig(), CUSTOM_PACKAGENAME, JDL.L("plugins.hoster.bandcampcom.custompackagename", "Define how the packagenames should look:")).setDefaultValue(defaultCustomPackagename));
        final StringBuilder sbpack = new StringBuilder();
        sbpack.append("Explanation of the available tags:\r\n");
        sbpack.append("*artist* = artist of the album\r\n");
        sbpack.append("*album* = artist of the album\r\n");
        sbpack.append("*date* = date when the album was released - appears in the user-defined format above");
        getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_LABEL, sbpack.toString()));
        getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, getPluginConfig(), FILENAMELOWERCASE, JDL.L("plugins.hoster.bandcamp.filenamelowercase", "filename to lower case")).setDefaultValue(true));
        getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, getPluginConfig(), PACKAGENAMELOWERCASE, JDL.L("plugins.hoster.bandcamp.packagenamelowercase", "packagename to lower case")).setDefaultValue(true));
        getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, getPluginConfig(), FILENAMESPACE, JDL.L("plugins.hoster.bandcamp.filenamespace", "filename space striper")).setDefaultValue(true));
        getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, getPluginConfig(), PACKAGENAMESPACE, JDL.L("plugins.hoster.bandcamp.packagenamespace", "packagename space striper")).setDefaultValue(true));
        getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, getPluginConfig(), CLEANPACKAGENAME, JDL.L("plugins.hoster.bandcamp.cleanpackagename", "when unchecked JD Core packagename cleanup routine is disabled!")).setDefaultValue(true));
    }

    @Override
    public void reset() {
    }

    @Override
    public void resetDownloadlink(DownloadLink link) {
    }

    @Override
    public void resetPluginGlobals() {
    }
}