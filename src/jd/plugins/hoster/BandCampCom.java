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
import jd.plugins.Plugin;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = {}, urls = {})
public class BandCampCom extends PluginForHost {
    public BandCampCom(PluginWrapper wrapper) {
        super(wrapper);
        this.setConfigElements();
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

    @Override
    public LazyPlugin.FEATURE[] getFeatures() {
        return new LazyPlugin.FEATURE[] { LazyPlugin.FEATURE.AUDIO_STREAMING };
    }

    public static final String FASTLINKCHECK         = "FASTLINKCHECK_2020_06_02";
    public static final String CUSTOM_DATE           = "CUSTOM_DATE";
    public static final String CUSTOM_FILENAME       = "CUSTOM_FILENAME";
    public static final String CUSTOM_VIDEO_FILENAME = "CUSTOM_VIDEO_FILENAME";
    public static final String GRABTHUMB             = "GRABTHUMB";
    public static final String CUSTOM_PACKAGENAME    = "CUSTOM_PACKAGENAME";
    public static final String FILENAMELOWERCASE     = "FILENAMELOWERCASE";
    public static final String PACKAGENAMELOWERCASE  = "PACKAGENAMELOWERCASE";
    public static final String FILENAMESPACE         = "FILENAMESPACE";
    public static final String PACKAGENAMESPACE      = "PACKAGENAMESPACE";
    public static final String CLEANPACKAGENAME      = "CLEANPACKAGENAME";
    private String             dllink                = null;

    @Override
    public String getAGBLink() {
        return "https://bandcamp.com/terms_of_use";
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return -1;
    }

    @Override
    public AvailableStatus requestFileInformation(final DownloadLink link) throws IOException, PluginException, ParseException {
        br = new Browser();
        this.setBrowserExclusive();
        br.setFollowRedirects(true);
        URLConnectionAdapter con = null;
        try {
            con = br.openGetConnection(link.getPluginPatternMatcher());
            if (looksLikeDownloadableContent(con)) {
                dllink = link.getPluginPatternMatcher();
                if (con.getCompleteContentLength() > 0) {
                    link.setVerifiedFileSize(con.getCompleteContentLength());
                }
                link.setFinalFileName(Encoding.htmlDecode(getFileNameFromHeader(con)));
                return AvailableStatus.TRUE;
            } else {
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
        br.getRequest().setHtmlCode(Encoding.htmlOnlyDecode(br.toString()));
        final String file = br.getRegex("\"file\"\\s*:\\s*(null|\".*?\"|\\{.*?\\})").getMatch(0);
        if (file == null) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        } else if ("null".equals(file)) {
            throw new AccountRequiredException();
        }
        dllink = new Regex(file, "((https?:)?//[^\"]*?)\"").getMatch(0);
        if (dllink == null) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        } else {
            logger.info("dllink = " + dllink);
            dllink = Encoding.htmlDecode(dllink).replace("\\", "");
        }
        if (!link.getBooleanProperty("fromdecrypter", false)) {
            /* Parse possibly missing metadata here. */
            String tracknumber = br.getRegex("\"track_num\"\\s*:\\s*(\\d+)").getMatch(0);
            if (tracknumber == null) {
                tracknumber = br.getRegex("\"track_number\"\\s*:\\s*(\\d+)").getMatch(0);
                if (tracknumber == null) {
                    tracknumber = "1";
                }
            }
            final int trackNum = Integer.parseInt(tracknumber);
            final int padLength = Math.max(2, StringUtils.getPadLength(trackNum));
            final String filename = br.getRegex("\"title\"\\s*:\\s*\"([^<>\"]*?)\"").getMatch(0);
            final String json_album = br.getRegex("<script type=\"application/(?:json\\+ld|ld\\+json)\">\\s*(.*?)\\s*</script>").getMatch(0);
            if (json_album == null) {
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            final Map<String, Object> albumInfo = restoreFromString(json_album, TypeRef.MAP);
            String artist = (String) JavaScriptEngineFactory.walkJson(albumInfo, "byArtist/name");
            if (artist == null) {
                artist = br.getRegex("name\\s*=\\s*\"title\"\\s*content\\s*=\\s*\"[^\"]+,\\s*by\\s*([^<>\"]+)\\s*\"").getMatch(0);
            }
            String date = (String) JavaScriptEngineFactory.walkJson(albumInfo, "datePublished");
            if (date == null) {
                date = br.getRegex("<meta itemprop=\"datePublished\" content=\"(\\d+)\"/>").getMatch(0);
            }
            String album = (String) JavaScriptEngineFactory.walkJson(albumInfo, "inAlbum/name");
            if (album == null) {
                album = br.getRegex("<title>\\s*(.*?)\\s*\\|.*?</title>").getMatch(0);
                if (album == null) {
                    album = br.getRegex("<title>\\s*(.*?)\\s*</title>").getMatch(0);
                }
            }
            link.setProperty("directdate", Encoding.htmlDecode(date.trim()));
            link.setProperty("directartist", Encoding.htmlDecode(artist.trim()));
            link.setProperty("directalbum", Encoding.htmlDecode(album.trim()));
            link.setProperty("directname", Encoding.htmlDecode(filename.trim()));
            link.setProperty("type", "mp3");
            link.setProperty("directtracknumber", StringUtils.formatByPadLength(padLength, trackNum));
            link.setProperty("fromdecrypter", true);
        }
        final String filename = getFormattedFilename(this, link);
        link.setFinalFileName(filename);
        // In case the link redirects to the finallink
        try {
            final Browser br2 = br.cloneBrowser();
            br2.setFollowRedirects(true);
            /* Server does NOT like HEAD requests! */
            con = br2.openGetConnection(dllink);
            if (looksLikeDownloadableContent(con)) {
                if (con.getCompleteContentLength() > 0) {
                    link.setVerifiedFileSize(con.getCompleteContentLength());
                }
                return AvailableStatus.TRUE;
            } else {
                br2.followConnection(true);
                /*
                 * 2020-04-23: Chances are high that the track cannot be downloaded because user needs to purchase it first. There is no
                 * errormessage or anything on their website.
                 */
                throw new AccountRequiredException();
            }
        } finally {
            try {
                con.disconnect();
            } catch (Throwable e) {
            }
        }
    }

    @Override
    public void handleFree(final DownloadLink link) throws Exception {
        requestFileInformation(link);
        dl = jd.plugins.BrowserAdapter.openDownload(br, link, dllink, true, 0);
        if (!looksLikeDownloadableContent(dl.getConnection())) {
            br.followConnection(true);
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        dl.startDownload();
    }

    public static String getFormattedFilename(final Plugin plugin, final DownloadLink link) throws ParseException {
        final String songTitle = link.getStringProperty("directname", null);
        final String tracknumber = link.getStringProperty("directtracknumber", null);
        final String artist = link.getStringProperty("directartist", null);
        final String album = link.getStringProperty("directalbum", null);
        final String dateString = link.getStringProperty("directdate", null);
        final String video_width = link.getStringProperty("video_width", null);
        final String video_height = link.getStringProperty("video_height", null);
        final SubConfiguration cfg = SubConfiguration.getConfig("bandcamp.com");
        String formattedFilename;
        final boolean video;
        if (video_height != null || video_width != null) {
            video = true;
            formattedFilename = cfg.getStringProperty(CUSTOM_VIDEO_FILENAME, defaultCustomVideoFilename);
            if (formattedFilename == null || formattedFilename.equals("")) {
                formattedFilename = defaultCustomVideoFilename;
            }
            if (!formattedFilename.contains("*songtitle*") || !formattedFilename.contains("*ext*")) {
                formattedFilename = defaultCustomVideoFilename;
            }
        } else {
            video = false;
            formattedFilename = cfg.getStringProperty(CUSTOM_FILENAME, defaultCustomFilename);
            if (formattedFilename == null || formattedFilename.equals("")) {
                formattedFilename = defaultCustomFilename;
            }
            if (!formattedFilename.contains("*songtitle*") || !formattedFilename.contains("*ext*")) {
                formattedFilename = defaultCustomFilename;
            }
        }
        String ext = link.getStringProperty("type", null);
        if (ext != null) {
            ext = "." + ext;
        } else if (video) {
            ext = ".mp4";
        } else {
            ext = ".mp3";
        }
        if (dateString != null && formattedFilename.contains("*date*")) {
            Date date = TimeFormatter.parseDateString(dateString);
            if (date == null) {
                try {
                    final SimpleDateFormat oldFormat = new SimpleDateFormat("yyyyMMdd");
                    date = oldFormat.parse(dateString);
                } catch (Exception e) {
                    plugin.getLogger().log(e);
                }
            }
            if (date != null) {
                final String userDefinedDateFormat = cfg.getStringProperty(CUSTOM_DATE, "dd.MM.yyyy_HH-mm-ss");
                String formattedDate = null;
                for (final String format : new String[] { userDefinedDateFormat, "yyyyMMdd" }) {
                    if (format != null) {
                        try {
                            final SimpleDateFormat formatter = new SimpleDateFormat(userDefinedDateFormat);
                            formattedDate = formatter.format(date);
                            if (formattedDate != null) {
                                break;
                            }
                        } catch (Exception e) {
                            plugin.getLogger().log(e);
                        }
                    }
                }
                formattedFilename = formattedFilename.replace("*date*", StringUtils.valueOrEmpty(formattedDate));
            }
        }
        formattedFilename = formattedFilename.replace("*tracknumber*", StringUtils.valueOrEmpty(tracknumber));
        formattedFilename = formattedFilename.replace("*artist*", StringUtils.valueOrEmpty(artist));
        formattedFilename = formattedFilename.replace("*album*", StringUtils.valueOrEmpty(album));
        formattedFilename = formattedFilename.replace("*video_width*", StringUtils.valueOrEmpty(video_width));
        formattedFilename = formattedFilename.replace("*video_height*", StringUtils.valueOrEmpty(video_height));
        formattedFilename = formattedFilename.replace("*ext*", ext);
        // Insert filename at the end to prevent errors with tags
        formattedFilename = formattedFilename.replace("*songtitle*", StringUtils.valueOrEmpty(songTitle));
        if (cfg.getBooleanProperty(jd.plugins.hoster.BandCampCom.FILENAMELOWERCASE, defaultFILENAMELOWERCASE)) {
            formattedFilename = formattedFilename.toLowerCase(Locale.ENGLISH);
        }
        if (cfg.getBooleanProperty(jd.plugins.hoster.BandCampCom.FILENAMESPACE, defaultFILENAMESPACE)) {
            formattedFilename = formattedFilename.replaceAll("\\s+", "_");
        }
        formattedFilename = formattedFilename.replaceFirst("([-\\s]+" + Pattern.quote(ext) + ")$", ext);
        return formattedFilename;
    }

    @Override
    public String getDescription() {
        return "JDownloader's bandcamp.com plugin helps downloading videoclips. JDownloader provides settings for the filenames.";
    }

    public static final boolean defaultFASTLINKCHECK        = true;
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
        getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, getPluginConfig(), FASTLINKCHECK, "Activate fast linkcheck (filesize won't be shown in linkgrabber)?").setDefaultValue(defaultFASTLINKCHECK));
        getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_SEPARATOR));
        getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, getPluginConfig(), GRABTHUMB, "Grab thumbnail (.jpg)?").setDefaultValue(defaultGRABTHUMB));
        getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_SEPARATOR));
        getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_LABEL, "Customize filename properties:"));
        getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_TEXTFIELD, getPluginConfig(), CUSTOM_DATE, "Define how the date should look:").setDefaultValue(defaultCUSTOM_DATE));
        getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_SEPARATOR));
        getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_LABEL, "Customize the filename properties:"));
        getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_SEPARATOR));
        getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_LABEL, "Customize the filename! Example: '*artist*_*date*_*songtitle**ext*'"));
        getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_TEXTFIELD, getPluginConfig(), CUSTOM_FILENAME, "Define how the filenames should look:").setDefaultValue(defaultCustomFilename));
        getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_TEXTFIELD, getPluginConfig(), CUSTOM_VIDEO_FILENAME, "Define how the video filenames should look:").setDefaultValue(defaultCustomVideoFilename));
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