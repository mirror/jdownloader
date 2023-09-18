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
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Pattern;

import org.appwork.storage.TypeRef;
import org.appwork.utils.StringUtils;
import org.appwork.utils.formatter.TimeFormatter;
import org.jdownloader.plugins.controller.LazyPlugin;
import org.jdownloader.scripting.JavaScriptEngineFactory;

import jd.PluginWrapper;
import jd.config.ConfigContainer;
import jd.config.ConfigEntry;
import jd.config.SubConfiguration;
import jd.http.Browser;
import jd.http.URLConnectionAdapter;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.plugins.AccountRequiredException;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;
import jd.plugins.decrypter.BandCampComDecrypter;

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = {}, urls = {})
public class BandCampCom extends PluginForHost {
    public BandCampCom(PluginWrapper wrapper) {
        super(wrapper);
        this.setConfigElements();
    }

    @Override
    public LazyPlugin.FEATURE[] getFeatures() {
        return new LazyPlugin.FEATURE[] { LazyPlugin.FEATURE.AUDIO_STREAMING };
    }

    public static List<String[]> getPluginDomains() {
        final List<String[]> ret = new ArrayList<String[]>();
        // each entry in List<String[]> will result in one PluginForDecrypt, Plugin.getHost() will return String[0]->main domain
        ret.add(new String[] { "bandcamp.com" });
        return ret;
    }

    public static String[] getAnnotationNames() {
        return buildAnnotationNames(getPluginDomains());
    }

    @Override
    public String[] siteSupportedNames() {
        return buildSupportedNames(getPluginDomains());
    }

    public static String[] getAnnotationUrls() {
        return buildAnnotationUrls(getPluginDomains());
    }

    public static String[] buildAnnotationUrls(final List<String[]> pluginDomains) {
        final List<String> ret = new ArrayList<String>();
        for (final String[] domains : pluginDomains) {
            ret.add("https?://(?:[a-z0-9\\-]+\\.)?" + buildHostsPatternPart(domains) + "/track/([a-z0-9\\-_]+)");
        }
        return ret.toArray(new String[0]);
    }

    public static final String FASTLINKCHECK                  = "FASTLINKCHECK_2020_06_02";
    public static final String CUSTOM_DATE_PATTERN            = "CUSTOM_DATE";
    public static final String CUSTOM_FILENAME_PATTERN        = "CUSTOM_FILENAME";
    public static final String CUSTOM_VIDEO_FILENAME_PATTERN  = "CUSTOM_VIDEO_FILENAME";
    public static final String GRABTHUMB                      = "GRABTHUMB";
    public static final String CUSTOM_PACKAGENAME             = "CUSTOM_PACKAGENAME";
    public static final String FILENAMELOWERCASE              = "FILENAMELOWERCASE";
    public static final String PACKAGENAMELOWERCASE           = "PACKAGENAMELOWERCASE";
    public static final String FILENAMESPACE                  = "FILENAMESPACE";
    public static final String PACKAGENAMESPACE               = "PACKAGENAMESPACE";
    public static final String CLEANPACKAGENAME               = "CLEANPACKAGENAME";
    public static final String PROPERTY_CONTENT_ID            = "content_id";
    public static final String PROPERTY_TITLE                 = "directname";
    public static final String PROPERTY_ARTIST                = "directartist";
    public static final String PROPERTY_ALBUM_ID              = "album_id";
    public static final String PROPERTY_ALBUM_TITLE           = "directalbum";
    public static final String PROPERTY_ALBUM_TRACK_POSITION  = "album_track_number";
    public static final String PROPERTY_ALBUM_NUMBEROF_TRACKS = "album_numberof_tracks";
    public static final String PROPERTY_VIDEO_FORMAT          = "video_format";
    public static final String PROPERTY_VIDEO_WIDTH           = "video_width";
    public static final String PROPERTY_VIDEO_HEIGHT          = "video_height";
    public static final String PROPERTY_FILE_TYPE             = "type";
    public static final String PROPERTY_DATE_TIMESTAMP        = "datetimestamp";
    private String             dllink                         = null;

    @Override
    public String getAGBLink() {
        return "https://bandcamp.com/terms_of_use";
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return -1;
    }

    @Override
    public String getLinkID(final DownloadLink link) {
        final String album_id = link.getStringProperty(PROPERTY_ALBUM_ID);
        final String contentid = link.getStringProperty(PROPERTY_CONTENT_ID);
        if (album_id != null && contentid != null) {
            return "bandcamp://album/" + album_id + "/track/" + contentid;
        } else if (contentid != null) {
            return "bandcamp://track/" + contentid;
        } else {
            return super.getLinkID(link);
        }
    }

    @Override
    public AvailableStatus requestFileInformation(final DownloadLink link) throws IOException, PluginException, ParseException {
        this.setBrowserExclusive();
        br.setFollowRedirects(true);
        URLConnectionAdapter con = null;
        try {
            /* Check if the link we got is a direct-URL. */
            con = br.openGetConnection(link.getPluginPatternMatcher());
            if (looksLikeDownloadableContent(con)) {
                dllink = link.getPluginPatternMatcher();
                if (con.getCompleteContentLength() > 0) {
                    link.setVerifiedFileSize(con.getCompleteContentLength());
                }
                final String filenameFromHeader = getFileNameFromHeader(con);
                if (filenameFromHeader != null) {
                    link.setFinalFileName(Encoding.htmlDecode(filenameFromHeader).trim());
                }
                return AvailableStatus.TRUE;
            } else {
                /* URL we have is not direct-downloadable. */
                br.followConnection(true);
            }
        } catch (final Exception e) {
            logger.log(e);
        } finally {
            try {
                con.disconnect();
            } catch (Throwable e) {
            }
        }
        if (br.containsHTML("(>\\s*Sorry\\s*,\\s*that something isn('|’)t here|>\\s*start at the beginning\\s*</a>\\s*and you'll certainly find what)") || this.br.getHttpConnection().getResponseCode() == 404) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        /* 2020-09-23: Decode html encoded json strings */
        br.getRequest().setHtmlCode(Encoding.htmlOnlyDecode(br.getRequest().getHtmlCode()));
        final String file = br.getRegex("\"file\"\\s*:\\s*(null|\".*?\"|\\{.*?\\})").getMatch(0);
        if (file == null) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        } else if ("null".equals(file)) {
            throw new AccountRequiredException();
        }
        dllink = new Regex(file, "((https?:)?//[^\"]*?)\"").getMatch(0);
        if (dllink == null) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        logger.info("dllink = " + dllink);
        dllink = Encoding.htmlDecode(dllink).replace("\\", "");
        /** Parse possibly missing metadata here. Do not overwrite existing properties with wrong- or null values!! */
        String tracknumberStr = br.getRegex("\"track_num\"\\s*:\\s*(\\d+)").getMatch(0);
        if (tracknumberStr == null) {
            tracknumberStr = br.getRegex("\"track_number\"\\s*:\\s*(\\d+)").getMatch(0);
        }
        final int tracknumber;
        if (tracknumberStr != null) {
            tracknumber = Integer.parseInt(tracknumberStr);
        } else {
            tracknumber = 1;
        }
        final String trackTitle = br.getRegex("\"title\"\\s*:\\s*\"([^<>\"]*?)\"").getMatch(0);
        final String json_album = br.getRegex("<script type=\"application/(?:json\\+ld|ld\\+json)\">\\s*(.*?)\\s*</script>").getMatch(0);
        if (json_album == null) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        final Map<String, Object> albumInfo = restoreFromString(json_album, TypeRef.MAP);
        String artist = (String) JavaScriptEngineFactory.walkJson(albumInfo, "byArtist/name");
        if (artist == null) {
            artist = br.getRegex("name\\s*=\\s*\"title\"\\s*content\\s*=\\s*\"[^\"]+,\\s*by\\s*([^<>\"]+)\\s*\"").getMatch(0);
        }
        final String dateStr = (String) JavaScriptEngineFactory.walkJson(albumInfo, "datePublished");
        String albumTitle = (String) JavaScriptEngineFactory.walkJson(albumInfo, "inAlbum/name");
        if (albumTitle == null) {
            albumTitle = br.getRegex("<title>\\s*(.*?)\\s*\\|.*?</title>").getMatch(0);
            if (albumTitle == null) {
                albumTitle = br.getRegex("<title>\\s*(.*?)\\s*</title>").getMatch(0);
            }
        }
        if (dateStr != null) {
            link.setProperty(PROPERTY_DATE_TIMESTAMP, BandCampComDecrypter.dateToTimestamp(dateStr));
        }
        if (artist != null) {
            link.setProperty(PROPERTY_ARTIST, Encoding.htmlDecode(artist).trim());
        }
        if (albumTitle != null) {
            link.setProperty(PROPERTY_ALBUM_TITLE, Encoding.htmlDecode(albumTitle).trim());
        }
        if (trackTitle != null) {
            link.setProperty(PROPERTY_TITLE, Encoding.htmlDecode(trackTitle).trim());
        }
        if (!link.hasProperty(PROPERTY_ALBUM_TRACK_POSITION)) {
            link.setProperty(PROPERTY_ALBUM_TRACK_POSITION, tracknumber);
        }
        link.setProperty(PROPERTY_FILE_TYPE, "mp3");
        final String trackID = br.getRegex("<\\!-- track id (\\d+) -->").getMatch(0);
        if (trackID != null && !link.hasProperty(PROPERTY_CONTENT_ID)) {
            link.setProperty(PROPERTY_CONTENT_ID, trackID);
        }
        final String filename = getFormattedFilename(link);
        link.setFinalFileName(filename);
        // In case the link redirects to the finallink
        try {
            final Browser br2 = br.cloneBrowser();
            br2.setFollowRedirects(true);
            /* Server does NOT like HEAD requests! */
            con = br2.openGetConnection(dllink);
            handleConnectionErrors(br, con);
            if (con.getCompleteContentLength() > 0) {
                link.setVerifiedFileSize(con.getCompleteContentLength());
            }
        } finally {
            try {
                con.disconnect();
            } catch (Throwable e) {
            }
        }
        return AvailableStatus.TRUE;
    }

    @Override
    public void handleFree(final DownloadLink link) throws Exception {
        requestFileInformation(link);
        dl = jd.plugins.BrowserAdapter.openDownload(br, link, dllink, true, 0);
        handleConnectionErrors(br, dl.getConnection());
        dl.startDownload();
    }

    private void handleConnectionErrors(final Browser br, final URLConnectionAdapter con) throws PluginException, IOException {
        if (!this.looksLikeDownloadableContent(con)) {
            br.followConnection(true);
            if (con.getResponseCode() == 403) {
                throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server error 403", 60 * 60 * 1000l);
            } else if (con.getResponseCode() == 404) {
                throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server error 404", 60 * 60 * 1000l);
            } else {
                throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Video broken?");
            }
        }
    }

    public static String getFormattedFilename(final DownloadLink link) throws ParseException {
        final String video_width = link.getStringProperty(PROPERTY_VIDEO_WIDTH);
        final String video_height = link.getStringProperty(PROPERTY_VIDEO_HEIGHT);
        final SubConfiguration cfg = SubConfiguration.getConfig("bandcamp.com");
        String formatString;
        if (video_height != null || video_width != null) {
            formatString = cfg.getStringProperty(CUSTOM_VIDEO_FILENAME_PATTERN, defaultCustomVideoFilename);
            if (StringUtils.isEmpty(formatString)) {
                formatString = defaultCustomVideoFilename;
            }
        } else {
            formatString = cfg.getStringProperty(CUSTOM_FILENAME_PATTERN, defaultCustomFilename);
            if (StringUtils.isEmpty(formatString)) {
                formatString = defaultCustomFilename;
            }
        }
        String formattedFilename = getFormattedBaseString(link, formatString);
        if (cfg.getBooleanProperty(BandCampCom.FILENAMELOWERCASE, defaultFILENAMELOWERCASE)) {
            formattedFilename = formattedFilename.toLowerCase(Locale.ENGLISH);
        }
        if (cfg.getBooleanProperty(BandCampCom.FILENAMESPACE, defaultFILENAMESPACE)) {
            formattedFilename = formattedFilename.replaceAll("\\s+", "_");
        }
        return formattedFilename;
    }

    public static String getFormattedBaseString(final DownloadLink link, String formattedBaseString) {
        final String songTitle = link.getStringProperty(PROPERTY_TITLE);
        final String tracknumberFormatted = getFormattedTrackNumber(link);
        final String artist = link.getStringProperty(PROPERTY_ARTIST);
        final String album = link.getStringProperty(PROPERTY_ALBUM_TITLE);
        final String video_width = link.getStringProperty(PROPERTY_VIDEO_WIDTH);
        final String video_height = link.getStringProperty(PROPERTY_VIDEO_HEIGHT);
        final SubConfiguration cfg = SubConfiguration.getConfig("bandcamp.com");
        String ext = link.getStringProperty(PROPERTY_FILE_TYPE);
        if (ext != null) {
            ext = "." + ext;
        } else {
            /* Fallback */
            ext = ".mp3";
        }
        final String dateFormatted = getFormattedDate(link, cfg);
        formattedBaseString = formattedBaseString.replace("*date*", StringUtils.valueOrEmpty(dateFormatted));
        formattedBaseString = formattedBaseString.replace("*tracknumber*", StringUtils.valueOrEmpty(tracknumberFormatted));
        formattedBaseString = formattedBaseString.replace("*artist*", StringUtils.valueOrEmpty(artist));
        formattedBaseString = formattedBaseString.replace("*album*", StringUtils.valueOrEmpty(album));
        formattedBaseString = formattedBaseString.replace("*video_width*", StringUtils.valueOrEmpty(video_width));
        formattedBaseString = formattedBaseString.replace("*video_height*", StringUtils.valueOrEmpty(video_height));
        formattedBaseString = formattedBaseString.replace("*ext*", ext);
        // Insert filename at the end to prevent errors with tags
        formattedBaseString = formattedBaseString.replace("*songtitle*", StringUtils.valueOrEmpty(songTitle));
        if (cfg.getBooleanProperty(BandCampCom.FILENAMELOWERCASE, defaultFILENAMELOWERCASE)) {
            formattedBaseString = formattedBaseString.toLowerCase(Locale.ENGLISH);
        }
        if (cfg.getBooleanProperty(BandCampCom.FILENAMESPACE, defaultFILENAMESPACE)) {
            formattedBaseString = formattedBaseString.replaceAll("\\s+", "_");
        }
        formattedBaseString = formattedBaseString.replaceFirst("([-\\s]+" + Pattern.quote(ext) + ")$", ext);
        return formattedBaseString;
    }

    public static String getFormattedDate(final DownloadLink link, final SubConfiguration cfg) {
        final String legacyDateString = link.getStringProperty("directdate");
        final long dateTimestamp = link.getLongProperty(PROPERTY_DATE_TIMESTAMP, -1);
        Date date = null;
        if (legacyDateString != null) {
            /* Older items added up to and including revision 48302 */
            date = TimeFormatter.parseDateString(legacyDateString);
            if (date == null) {
                try {
                    final SimpleDateFormat oldFormat = new SimpleDateFormat("yyyyMMdd");
                    date = oldFormat.parse(legacyDateString);
                } catch (final Exception ignore) {
                }
            }
        } else if (dateTimestamp != -1) {
            date = new Date(dateTimestamp);
        }
        if (date != null) {
            final String userDefinedDateFormat = cfg.getStringProperty(CUSTOM_DATE_PATTERN, defaultCUSTOM_DATE);
            String formattedDate = null;
            for (final String format : new String[] { userDefinedDateFormat, "yyyyMMdd" }) {
                if (format != null) {
                    try {
                        final SimpleDateFormat formatter = new SimpleDateFormat(userDefinedDateFormat);
                        formattedDate = formatter.format(date);
                        return formattedDate;
                    } catch (final Exception ignore) {
                    }
                }
            }
        }
        return null;
    }

    public static String getFormattedTrackNumber(final DownloadLink link) {
        final String tracknumberFormattedLegacy = link.getStringProperty("directtracknumber");
        final int trackNumber = link.getIntegerProperty(PROPERTY_ALBUM_TRACK_POSITION, 1);
        final int albumNumberofTracks = link.getIntegerProperty(PROPERTY_ALBUM_NUMBEROF_TRACKS, -1);
        if (tracknumberFormattedLegacy != null) {
            /* Older items added up to and including revision 48302 */
            return tracknumberFormattedLegacy;
        } else if (albumNumberofTracks != -1) {
            return StringUtils.formatByPadLength(StringUtils.getPadLength(albumNumberofTracks), trackNumber);
        } else {
            /* Fallback */
            return StringUtils.formatByPadLength(2, trackNumber);
        }
    }

    @Override
    public String getDescription() {
        return "JDownloader's bandcamp.com plugin helps downloading videoclips. JDownloader provides settings for the filenames.";
    }

    public static final boolean defaultGRABTHUMB            = false;
    public static final String  defaultCUSTOM_DATE          = "dd.MM.yyyy_HH-mm-ss";
    private static final String defaultCustomFilename       = "*tracknumber*.*artist* - *songtitle**ext*";
    private static final String defaultCustomVideoFilename  = "*tracknumber*.*artist* - *songtitle*-*video_height*p*ext*";
    public static final boolean defaultFILENAMELOWERCASE    = false;
    public static final boolean defaultFILENAMESPACE        = false;
    public static final String  defaultCustomPackagename    = "*artist* - *album*";
    public static final boolean defaultPACKAGENAMELOWERCASE = false;
    public static final boolean defaultPACKAGENAMESPACE     = false;
    public static final boolean defaultCLEANPACKAGENAME     = false;

    private void setConfigElements() {
        getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, getPluginConfig(), GRABTHUMB, "Grab thumbnail (.jpg)?").setDefaultValue(defaultGRABTHUMB));
        getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_SEPARATOR));
        getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_LABEL, "Customize filename properties:"));
        getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_TEXTFIELD, getPluginConfig(), CUSTOM_DATE_PATTERN, "Define how the date should look:").setDefaultValue(defaultCUSTOM_DATE));
        getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_SEPARATOR));
        getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_LABEL, "Customize the filename properties:"));
        getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_SEPARATOR));
        getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_LABEL, "Customize the filename! Example: '*artist*_*date*_*songtitle**ext*'"));
        getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_TEXTFIELD, getPluginConfig(), CUSTOM_FILENAME_PATTERN, "Define how the filenames should look:").setDefaultValue(defaultCustomFilename));
        getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_TEXTFIELD, getPluginConfig(), CUSTOM_VIDEO_FILENAME_PATTERN, "Define how the video filenames should look:").setDefaultValue(defaultCustomVideoFilename));
        final StringBuilder sb = new StringBuilder();
        sb.append("Explanation of the available tags:\r\n");
        sb.append("*artist* = artist of the album\r\n");
        sb.append("*album* = artist of the album\r\n");
        sb.append("*tracknumber* = number of the track\r\n");
        sb.append("*songtitle* = name of the song without extension\r\n");
        sb.append("*ext* = the extension of the file, in this case usually '.mp3'\r\n");
        sb.append("*date* = date when the album was released - appears in the user-defined format above");
        getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_LABEL, sb.toString()));
        getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, getPluginConfig(), FILENAMELOWERCASE, "Filename to lower case?").setDefaultValue(defaultFILENAMELOWERCASE));
        getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, getPluginConfig(), FILENAMESPACE, "Filename replace space with underscore?").setDefaultValue(defaultFILENAMESPACE));
        getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_SEPARATOR));
        getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_LABEL, "Customize the packagename for playlists and '[a-z0-9\\-]+.bandcamp.com/album/' links! Example: '*artist* - *album*':"));
        getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_TEXTFIELD, getPluginConfig(), CUSTOM_PACKAGENAME, "Define how the packagenames should look:").setDefaultValue(defaultCustomPackagename));
        final StringBuilder sbpack = new StringBuilder();
        sbpack.append("Explanation of the available tags:\r\n");
        sbpack.append("*artist* = artist of the album\r\n");
        sbpack.append("*album* = artist of the album\r\n");
        sbpack.append("*date* = date when the album was released - appears in the user-defined format above");
        getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_LABEL, sbpack.toString()));
        getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, getPluginConfig(), PACKAGENAMELOWERCASE, "Packagename to lower case?").setDefaultValue(defaultPACKAGENAMELOWERCASE));
        getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, getPluginConfig(), PACKAGENAMESPACE, "Packagename replace space with underscore?").setDefaultValue(defaultPACKAGENAMESPACE));
        getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, getPluginConfig(), CLEANPACKAGENAME, "Cleanup packagenames?").setDefaultValue(defaultCLEANPACKAGENAME));
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