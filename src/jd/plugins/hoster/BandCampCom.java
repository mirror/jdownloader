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

import org.appwork.storage.JSonStorage;
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
    public Browser createNewBrowserInstance() {
        final Browser br = super.createNewBrowserInstance();
        BandCampComDecrypter.prepBR(br);
        return br;
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

    public static final Pattern PATTERN_SINGLE_TRACK                = Pattern.compile("(?i)https?://[^/]+/track/([a-z0-9\\-_]+)");
    /* Plugin setting properties */
    public static final String  FASTLINKCHECK                       = "FASTLINKCHECK_2020_06_02";
    public static final String  CUSTOM_DATE_PATTERN                 = "CUSTOM_DATE";
    public static final String  CUSTOM_FILENAME_PATTERN             = "CUSTOM_FILENAME";
    public static final String  CUSTOM_VIDEO_FILENAME_PATTERN       = "CUSTOM_VIDEO_FILENAME";
    public static final String  SETTING_GRAB_COVER_ART              = "GRABTHUMB";
    public static final String  CUSTOM_PACKAGENAME                  = "CUSTOM_PACKAGENAME";
    public static final String  FILENAMELOWERCASE                   = "FILENAMELOWERCASE";
    public static final String  PACKAGENAMELOWERCASE                = "PACKAGENAMELOWERCASE";
    public static final String  FILENAMESPACE                       = "FILENAMESPACE";
    public static final String  PACKAGENAMESPACE                    = "PACKAGENAMESPACE";
    public static final String  CLEANPACKAGENAME                    = "CLEANPACKAGENAME";
    public static final String  SETTING_ENABLE_FAST_LINKCHECK_ALBUM = "ENABLE_FAST_LINKCHECK_ALBUM";
    /* DownloadLink properties */
    public static final String  PROPERTY_CONTENT_ID                 = "content_id";
    public static final String  PROPERTY_TITLE                      = "directname";
    public static final String  PROPERTY_USERNAME                   = "username";
    public static final String  PROPERTY_ARTIST                     = "directartist";
    public static final String  PROPERTY_ARTIST_ALBUM               = "album_artist";
    public static final String  PROPERTY_ALBUM_ID                   = "album_id";
    public static final String  PROPERTY_ALBUM_TITLE                = "directalbum";
    public static final String  PROPERTY_ALBUM_TRACK_POSITION       = "album_track_number";
    public static final String  PROPERTY_ALBUM_DATE_TIMESTAMP       = "album_datetimestamp";
    public static final String  PROPERTY_SHOW_TRACK_POSITION        = "show_track_number";
    public static final String  PROPERTY_ALBUM_NUMBEROF_TRACKS      = "album_numberof_tracks";
    public static final String  PROPERTY_SHOW_NUMBEROF_TRACKS       = "show_numberof_tracks";
    public static final String  PROPERTY_VIDEO_FORMAT               = "video_format";
    public static final String  PROPERTY_VIDEO_WIDTH                = "video_width";
    public static final String  PROPERTY_VIDEO_HEIGHT               = "video_height";
    public static final String  PROPERTY_FILE_TYPE                  = "type";
    public static final String  PROPERTY_TRACK_DATE_TIMESTAMP       = "datetimestamp";
    public static final String  PROPERTY_DATE_DIRECTURL             = "directurl";
    private String              dllink                              = null;

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

    public AvailableStatus requestFileInformation(final DownloadLink link) throws IOException, PluginException, ParseException {
        return requestFileInformation(link, false);
    }

    private AvailableStatus requestFileInformation(final DownloadLink link, final boolean isDownload) throws IOException, PluginException, ParseException {
        final String username = BandCampComDecrypter.regexUsernameFromURL(link.getPluginPatternMatcher());
        if (username != null) {
            link.setProperty(PROPERTY_USERNAME, username);
        }
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
        link.removeProperty(PROPERTY_DATE_DIRECTURL);
        final int trackNumber = link.getIntegerProperty(PROPERTY_ALBUM_TRACK_POSITION, -1);
        final int trackIndex;
        if (trackNumber > 0) {
            trackIndex = trackNumber - 1;
        } else {
            /* Unknown track-number/index */
            trackIndex = -1;
        }
        parseAndSetSingleTrackInfo(link, br, trackIndex);
        dllink = link.getStringProperty(PROPERTY_DATE_DIRECTURL);
        final String filename = getFormattedFilename(link);
        link.setFinalFileName(filename);
        if (dllink != null && !link.isSizeSet() && !isDownload) {
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
        }
        return AvailableStatus.TRUE;
    }

    public static void parseAndSetSingleTrackInfo(final DownloadLink link, final Browser br, final int trackIndex) {
        /** Parse possibly missing metadata here. Do not overwrite existing properties with wrong- or null values!! */
        // final String albumOrTrackTitle = new Regex(htmlEntityDecoded, "\"title\"\\s*:\\s*\"([^<>\"]*?)\"").getMatch(0);
        final String json_album = br.getRegex("<script type=\"application/(?:json\\+ld|ld\\+json)\">\\s*(.*?)\\s*</script>").getMatch(0);
        final Map<String, Object> albumInfo = JSonStorage.restoreFromString(json_album, TypeRef.MAP);
        String artist = null;
        final String albumArtist = br.getRegex(">\\s*by\\s*<span>\\s*<[^>]*>([^<]+)</a>").getMatch(0);
        if (albumArtist != null) {
            link.setProperty(PROPERTY_ARTIST_ALBUM, Encoding.htmlDecode(albumArtist).trim());
        }
        String albumTitle = (String) JavaScriptEngineFactory.walkJson(albumInfo, "inAlbum/name");
        if (albumTitle == null) {
            albumTitle = br.getRegex("<title>\\s*(.*?)\\s*\\|.*?</title>").getMatch(0);
            if (albumTitle == null) {
                albumTitle = br.getRegex("<title>\\s*(.*?)\\s*</title>").getMatch(0);
            }
        }
        if (albumTitle != null) {
            link.setProperty(PROPERTY_ALBUM_TITLE, Encoding.htmlOnlyDecode(albumTitle).trim());
        }
        link.setProperty(PROPERTY_FILE_TYPE, "mp3");
        final boolean isHtmlContextOfSingleTrack;
        final String trackIDFromHTML = br.getRegex("<\\!-- track id (\\d+) -->").getMatch(0);
        if (trackIDFromHTML != null || new Regex(br.getURL(), PATTERN_SINGLE_TRACK).patternFind()) {
            isHtmlContextOfSingleTrack = true;
        } else {
            isHtmlContextOfSingleTrack = false;
        }
        if (trackIDFromHTML != null && !link.hasProperty(PROPERTY_CONTENT_ID)) {
            link.setProperty(PROPERTY_CONTENT_ID, trackIDFromHTML);
        }
        final String albumDatePublishedStr = (String) JavaScriptEngineFactory.walkJson(albumInfo, "datePublished");
        if (albumDatePublishedStr != null) {
            if (isHtmlContextOfSingleTrack) {
                link.setProperty(PROPERTY_TRACK_DATE_TIMESTAMP, BandCampComDecrypter.dateToTimestamp(albumDatePublishedStr));
            } else {
                link.setProperty(PROPERTY_ALBUM_DATE_TIMESTAMP, BandCampComDecrypter.dateToTimestamp(albumDatePublishedStr));
            }
        }
        String trackJson = br.getRegex("data-tralbum=\"([^\"]+)").getMatch(0);
        if (trackJson != null) {
            final String targetTrackID;
            if (trackIDFromHTML != null) {
                targetTrackID = trackIDFromHTML;
            } else {
                targetTrackID = link.getStringProperty(PROPERTY_CONTENT_ID);
            }
            trackJson = Encoding.htmlOnlyDecode(trackJson);
            final Map<String, Object> trackInfo0 = JSonStorage.restoreFromString(trackJson, TypeRef.MAP);
            artist = trackInfo0.get("artist").toString();
            final List<Map<String, Object>> tracklist = (List<Map<String, Object>>) trackInfo0.get("trackinfo");
            Map<String, Object> trackinfo1 = null;
            if (targetTrackID != null) {
                for (final Map<String, Object> trackInfo : tracklist) {
                    final String thisTrackID = trackInfo.get("id").toString();
                    if (thisTrackID.equals(targetTrackID)) {
                        trackinfo1 = trackInfo;
                        break;
                    }
                }
            }
            if (trackinfo1 == null && trackIndex >= 0 && trackIndex <= tracklist.size() - 1) {
                /* Get track by position (unsafer method) */
                trackinfo1 = tracklist.get(trackIndex);
            }
            String artistAndTrackTitle = null;
            if (trackinfo1 != null) {
                artist = (String) trackinfo1.get("artist");
                artistAndTrackTitle = (String) trackinfo1.get("title");
                link.setProperty(PROPERTY_CONTENT_ID, trackinfo1.get("id").toString());
                link.setProperty(PROPERTY_ALBUM_TRACK_POSITION, trackinfo1.get("track_num"));
                final String directurl = (String) JavaScriptEngineFactory.walkJson(trackinfo1, "file/mp3-128");
                if (directurl != null && directurl.startsWith("http")) {
                    link.setProperty(PROPERTY_DATE_DIRECTURL, directurl);
                }
                /* Calculate filesize */
                final int duration = ((Number) trackinfo1.get("duration")).intValue();
                if (duration > 0) {
                    link.setDownloadSize(128 * 1024l / 8 * duration);
                }
            }
            if (artist == null) {
                artist = (String) trackInfo0.get("artist");
            }
            if (artist != null) {
                artist = Encoding.htmlOnlyDecode(artist).trim();
                link.setProperty(PROPERTY_ARTIST, artist);
            }
            if (artistAndTrackTitle != null) {
                artistAndTrackTitle = Encoding.htmlOnlyDecode(artistAndTrackTitle).trim();
                if (artist != null) {
                    /* Remove artist to get track-title only. */
                    link.setProperty(PROPERTY_TITLE, artistAndTrackTitle.replaceFirst(Pattern.quote(artist) + " - ", ""));
                } else {
                    link.setProperty(PROPERTY_TITLE, artistAndTrackTitle);
                }
            }
        }
    }

    @Override
    public void handleFree(final DownloadLink link) throws Exception {
        requestFileInformation(link, true);
        if (StringUtils.isEmpty(dllink)) {
            /* Paid content e.g. audio track which can't be streamed unless user buys it. */
            throw new AccountRequiredException();
        }
        /* 2023-10-11: Allow max 1 connection per download to go easy on the Bandcamp servers. */
        dl = jd.plugins.BrowserAdapter.openDownload(br, link, dllink, true, 1);
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
                throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Stream broken?");
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
        final String content_id = link.getStringProperty(PROPERTY_CONTENT_ID);
        final String songTitle = link.getStringProperty(PROPERTY_TITLE);
        final String tracknumberFormatted = getFormattedTrackNumber(link);
        final String artistFullName = link.getStringProperty(PROPERTY_ARTIST);
        final String username = link.getStringProperty(PROPERTY_USERNAME);
        final String albumArtist = link.getStringProperty(PROPERTY_ARTIST_ALBUM);
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
        final String trackDateFormatted = getFormattedTrackDate(link, cfg);
        final String albumDateFormatted = getFormattedAlbumDate(link, cfg);
        formattedBaseString = formattedBaseString.replace("*content_id*", StringUtils.valueOrEmpty(content_id));
        formattedBaseString = formattedBaseString.replace("*date*", StringUtils.valueOrEmpty(trackDateFormatted));
        formattedBaseString = formattedBaseString.replace("*album_date*", StringUtils.valueOrEmpty(albumDateFormatted));
        formattedBaseString = formattedBaseString.replace("*tracknumber*", StringUtils.valueOrEmpty(tracknumberFormatted));
        formattedBaseString = formattedBaseString.replace("*artist*", StringUtils.valueOrEmpty(artistFullName));
        formattedBaseString = formattedBaseString.replace("*username*", StringUtils.valueOrEmpty(username));
        /* username_pretty replacement = Legacy compatibility as this is now album_artist */
        formattedBaseString = formattedBaseString.replace("*username_pretty*", StringUtils.valueOrEmpty(albumArtist)).replace("*album_artist*", StringUtils.valueOrEmpty(albumArtist));
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

    public static String getFormattedTrackDate(final DownloadLink link, final SubConfiguration cfg) {
        final String legacyDateString = link.getStringProperty("directdate");
        final long dateTimestamp = link.getLongProperty(PROPERTY_TRACK_DATE_TIMESTAMP, -1);
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
            return getFormattedDate(date, cfg);
        }
        return null;
    }

    public static String getFormattedAlbumDate(final DownloadLink link, final SubConfiguration cfg) {
        final long dateTimestamp = link.getLongProperty(PROPERTY_ALBUM_DATE_TIMESTAMP, -1);
        if (dateTimestamp != -1) {
            return getFormattedDate(new Date(dateTimestamp), cfg);
        }
        return null;
    }

    private static String getFormattedDate(final Date date, final SubConfiguration cfg) {
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
        return null;
    }

    public static String getFormattedTrackNumber(final DownloadLink link) {
        final String tracknumberFormattedLegacy = link.getStringProperty("directtracknumber");
        int trackNumber = link.getIntegerProperty(PROPERTY_SHOW_TRACK_POSITION, -1);
        if (trackNumber == -1) {
            /* Maybe track is part of album. */
            trackNumber = link.getIntegerProperty(PROPERTY_ALBUM_TRACK_POSITION, -1);
        }
        if (trackNumber == -1) {
            /* Fallback */
            trackNumber = 1;
        }
        int numberofTracks = link.getIntegerProperty(PROPERTY_SHOW_NUMBEROF_TRACKS, -1);
        if (numberofTracks == -1) {
            /* Maybe track is part of album. */
            numberofTracks = link.getIntegerProperty(PROPERTY_ALBUM_NUMBEROF_TRACKS, -1);
        }
        if (tracknumberFormattedLegacy != null) {
            /* Older items added up to and including revision 48302 */
            return tracknumberFormattedLegacy;
        } else if (numberofTracks != -1) {
            return StringUtils.formatByPadLength(StringUtils.getPadLength(numberofTracks), trackNumber);
        } else {
            /* Fallback */
            return StringUtils.formatByPadLength(2, trackNumber);
        }
    }

    @Override
    public String getDescription() {
        return "JDownloader's bandcamp.com plugin helps downloading videoclips. JDownloader provides settings for the filenames.";
    }

    public static final boolean defaultGRAB_COVER_ART           = false;
    public static final String  defaultCUSTOM_DATE              = "dd.MM.yyyy_HH-mm-ss";
    private static final String defaultCustomFilename           = "*tracknumber*.*artist* - *songtitle**ext*";
    private static final String defaultCustomVideoFilename      = "*tracknumber*.*artist* - *songtitle*-*video_height*p*ext*";
    public static final boolean defaultFILENAMELOWERCASE        = false;
    public static final boolean defaultFILENAMESPACE            = false;
    public static final String  defaultCustomPackagename        = "*artist* - *album*";
    public static final boolean defaultPACKAGENAMELOWERCASE     = false;
    public static final boolean defaultPACKAGENAMESPACE         = false;
    public static final boolean defaultCLEANPACKAGENAME         = false;
    public static final boolean defaultEnableFastLinkcheckAlbum = true;

    private void setConfigElements() {
        getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, getPluginConfig(), SETTING_GRAB_COVER_ART, "Grab cover art (.jpg)?").setDefaultValue(defaultGRAB_COVER_ART));
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
        sb.append("*content_id* = Unique ID of content, typically track_id '1234567890'\r\n");
        sb.append("*artist* = Full name of artist of content e.g. 'John Doe'\r\n");
        sb.append("*username* = Username of artist e.g. 'johndoe19'\r\n");
        sb.append("*album_artist* = Pretty username of artist e.g. 'John Doe'\r\n");
        sb.append("*album* = Title of the album\r\n");
        sb.append("*tracknumber* = Number of the track\r\n");
        sb.append("*songtitle* = Title of the song\r\n");
        sb.append("*ext* = Extension of the file, in this case usually '.mp3'\r\n");
        sb.append("*date* = Date when the song was released - appears in the user-defined format above\r\n");
        sb.append("*album_date* = Date when the album was released - appears in the user-defined format above");
        getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_LABEL, sb.toString()));
        getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, getPluginConfig(), FILENAMELOWERCASE, "Filename to lower case?").setDefaultValue(defaultFILENAMELOWERCASE));
        getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, getPluginConfig(), FILENAMESPACE, "Filename replace space with underscore?").setDefaultValue(defaultFILENAMESPACE));
        getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_SEPARATOR));
        getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_LABEL, "Customize the packagename for playlists and '[a-z0-9\\-]+.bandcamp.com/album/' links! Example: '*artist* - *album*':"));
        getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_TEXTFIELD, getPluginConfig(), CUSTOM_PACKAGENAME, "Define how the packagenames should look:").setDefaultValue(defaultCustomPackagename));
        getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_LABEL, sb.toString()));
        getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, getPluginConfig(), PACKAGENAMELOWERCASE, "Packagename to lower case?").setDefaultValue(defaultPACKAGENAMELOWERCASE));
        getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, getPluginConfig(), PACKAGENAMESPACE, "Packagename replace space with underscore?").setDefaultValue(defaultPACKAGENAMESPACE));
        getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, getPluginConfig(), CLEANPACKAGENAME, "Cleanup packagenames?").setDefaultValue(defaultCLEANPACKAGENAME));
        getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_SEPARATOR));
        getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_LABEL, "Advanced settings:"));
        getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, getPluginConfig(), SETTING_ENABLE_FAST_LINKCHECK_ALBUM, "Enable fast linkcheck for crawled album items?").setDefaultValue(defaultEnableFastLinkcheckAlbum));
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