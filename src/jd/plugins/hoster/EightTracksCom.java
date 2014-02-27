//jDownloader - Downloadmanager
//Copyright (C) 2010  JD-Team support@jdownloader.org
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

import jd.PluginWrapper;
import jd.config.Property;
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

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "8tracks.com" }, urls = { "http://8tracksdecrypted\\.com/\\d+" }, flags = { 0 })
public class EightTracksCom extends PluginForHost {

    public EightTracksCom(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public String getAGBLink() {
        return "http://8tracks.com/terms";
    }

    public int getMaxConcurrentProcessingInstances() {
        return 1;
    }

    private static final String  MAINPAGE                                          = "http://8tracks.com/";
    private static final String  NICE_HOST                                         = "8tracks.com";
    private static final String  NICE_HOSTproperty                                 = "8trackscom";

    // Waittimes
    private static final int     WAITTIME_SECONDS_DEFAULT                          = 300;
    private static final int     WAITTIME_SECONDS_BEFORE_TRACK_PLAYED_CONFIRMATION = 32;
    private static final int     WAITTIME_SECONDS_EXTRA                            = 5;
    private static final int     WAITTIME_SECONDS_SKIPLIMIT                        = 60;
    private static final int     WAITTIME_SECONDS_TEST_MODE                        = 10;
    private static final int     SKIPS_EXCECUTED_IN_DECRYPTER                      = 1;
    private static final int     SKIP_POSSIBLE_TILL_TRACK                          = 4 - SKIPS_EXCECUTED_IN_DECRYPTER;
    // private static final long BITRATE_SOUNDCLOUD = 11250;
    private static final long    SOURCE_8TRACKS_BITRATE                            = 5000;
    private static final String  SOURCE_8TRACKS_CLIENT_ID                          = "3904229f42df3999df223f6ebf39a8fe";

    // sets wrong waittimes to check skip_failed errorhandling
    private static final boolean TEST_MODE                                         = false;
    private static final String  TEST_MODE_TOKEN                                   = null;

    private static Object        LOCK                                              = new Object();
    private String               MAIN_LINK                                         = null;
    private String               clipData;
    private boolean              AT_END                                            = false;
    private boolean              AT_LAST_TRACK                                     = false;
    private static boolean       pluginloaded                                      = false;

    private String               currenttrackid                                    = null;

    // XML version needs API key so we use the json version
    @Override
    public AvailableStatus requestFileInformation(final DownloadLink link) throws IOException, PluginException {
        if (link.getBooleanProperty("offline", false)) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        this.setBrowserExclusive();
        prepBr();
        MAIN_LINK = link.getStringProperty("mainlink", null);
        br.getPage(MAIN_LINK);
        if (br.containsHTML(">Sorry, that page doesn\\'t exist")) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        if (br.getURL().contains("/explore/")) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        if (br.containsHTML(">The mix you\\'re looking for is currently in private mode")) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        link.setName(link.getStringProperty("tempname_with_ext", null));
        return AvailableStatus.TRUE;
    }

    @Override
    public void handleFree(final DownloadLink downloadLink) throws Exception, PluginException {
        requestFileInformation(downloadLink);

        /* Difference between dllink and finallink: dllink can also be a soundcloud API link - this is easier to re-use later */
        String finallink = checkDirectLink(downloadLink, "savedlink");
        String dllink = null;
        String ext = null;
        String filename = downloadLink.getStringProperty("final_filename", null);
        if (filename == null) filename = downloadLink.getStringProperty("tempname", null);
        final int tracknumber = (int) getLongProperty(downloadLink, "tracknumber", -1);
        /* http://8tracks.com/tracks/TRACKID */
        currenttrackid = downloadLink.getStringProperty("trackid", null);
        /* Only go in this handling if the user added a single tracks, otherwise we will get low quality 30 seconds preview files */
        if (downloadLink.getBooleanProperty("single_link", false)) {
            /* This should never happen */
            if (currenttrackid == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);

            clipData = getPage(MAINPAGE + "sets/play_track/" + currenttrackid + "?format=jsonh");
            dllink = getDllink();
            if (dllink == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            if (getFilename() != null) filename = getFilename();
            finallink = getFinalDirectlink(dllink);
            if (finallink == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        } else if (finallink == null) {
            /* This should never happen */
            if (tracknumber == -1) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);

            String sameLink = "";
            String playToken = downloadLink.getStringProperty("playtoken", null);
            if (TEST_MODE && TEST_MODE_TOKEN != null) playToken = TEST_MODE_TOKEN;
            final String mixid = downloadLink.getStringProperty("mixid", null);
            final int last_track_number = (int) getLongProperty(downloadLink, "lasttracknumber", 0);
            final boolean NEED_LAST_TRACK = (tracknumber == last_track_number);
            if (br.getRegex("name=\"csrf-token\" content=\"(.*?)\"").matches()) {
                br.getHeaders().put("X-CSRF-Token", br.getRegex("name=\"csrf-token\" content=\"(.*?)\"").getMatch(0));
            }
            if (playToken == null) {
                /* Refresh token */
                logger.info("Renewing playToken");
                br.clearCookies(MAINPAGE);
                clipData = br.getPage(MAINPAGE + "sets/new?format=jsonh");
                playToken = getClipData("play_token");
                if (playToken == null) {
                    logger.warning("renewing playToken failed!");
                    throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                }
                setCookies(playToken);
                /* Important to remember the position we were at */
                downloadLink.setProperty("playtoken", playToken);
                startPlaylist(playToken, mixid);
                currenttrackid = mixid;
            }

            // TODO: Change loop & add "startPlaylist" for first song from existing temp tracklist
            /* limit to 100 API calls per minute -> Usually we will not exceed this limit */
            for (int i = 1; i <= tracknumber; i++) {
                if (i == 1) {
                    setCookies(playToken);
                    clipData = br.getPage(MAINPAGE + "sets/" + playToken + "/tracks_played?mix_id=" + mixid + "&reverse=true&format=jsonh");
                    final String tracklist_text = br.getRegex("\\{\"tracks\":\\[(.*?)\\],\"status\"").getMatch(0);
                    String[] ids = null;
                    if (tracklist_text != null) ids = new Regex(tracklist_text, "(\\{.*?\\})").getColumn(0);
                    /* Check how many tracks we already unlocked and if our token still works */
                    if (ids != null && ids.length != 0) {
                        final int list_length = ids.length;
                        synchronized (LOCK) {
                            final int old_list_length = (int) getLongPropertyPluginCfg("tracks_played_list_length", -1);
                            logger.info("Old list length: " + old_list_length + " // current list length: " + list_length);
                            if (list_length < old_list_length) {
                                logger.info("List length doesn't match (too small) -> Maybe reset token to retry from the beginning?");
                            } else {
                                logger.info("List length is okay (same or bigger) -> Saving it!");
                                this.getPluginConfig().setProperty("tracks_played_list_length", list_length);
                            }
                        }
                        /* Check if we got a higher amount of the tracks than the track-number we need */
                        if (list_length >= tracknumber) {
                            /* Yes -> Set information for the track w need */
                            clipData = ids[tracknumber - 1];
                            currenttrackid = updateTrackID();
                            i = tracknumber;
                        } else {
                            /* No -> Set information for the latest track available and of course our start-position */
                            clipData = ids[list_length - 1];
                            currenttrackid = updateTrackID();
                            i = list_length;
                            // /* We might be out of the skip range --> We have to wait till we can access the next track */
                            // if (i >= SKIP_POSSIBLE_TILL_TRACK) force_skip_wait = true;
                            startPlaylist(playToken, mixid);
                            i++;
                        }
                    } else {
                        // /* We have no list of started tracks - let's start with track 1 */
                        startPlaylist(playToken, mixid);
                    }
                } else {
                    if (!Boolean.parseBoolean(getClipData("skip_allowed"))) {
                        logger.info("We are not allowed to skip anymore --> Waiting in between to get the next track in order to get all tracks");
                        /* Special handling to get from the penultimate track to last track */
                        if (i == last_track_number) {
                            /* Pretend to play the song */
                            this.sleep(WAITTIME_SECONDS_BEFORE_TRACK_PLAYED_CONFIRMATION * 1000l, downloadLink);
                            br.getPage(MAINPAGE + "sets/" + playToken + "/report?player=sm&include=track%5Bfaved%2Bannotation%2Bartist_details%5D&mix_id=" + mixid + "&track_id=" + currenttrackid + "&format=jsonh");
                            /* Same command to start and to get last track */
                            startPlaylist(playToken, mixid);
                        } else {
                            /* Pretend to play the song */
                            this.sleep(WAITTIME_SECONDS_BEFORE_TRACK_PLAYED_CONFIRMATION * 1000l, downloadLink);
                            br.getPage(MAINPAGE + "sets/" + playToken + "/report?player=sm&include=track%5Bfaved%2Bannotation%2Bartist_details%5D&mix_id=" + mixid + "&track_id=" + currenttrackid + "&format=jsonh");
                            /* Wait till "the song is (probably) "over" */
                            long wait_seconds = getWaitSeconds(dllink);
                            if (TEST_MODE) wait_seconds = WAITTIME_SECONDS_TEST_MODE;
                            logger.info("Waiting " + wait_seconds + " seconds from now on...");
                            this.sleep(wait_seconds * 1000l, downloadLink);
                            clipData = getPage(MAINPAGE + "sets/" + playToken + "/next?player=sm&include=track%5Bfaved%2Bannotation%2Bartist_details%5D&mix_id=" + mixid + "&track_id=" + currenttrackid + "&format=jsonh");
                        }
                    } else {
                        logger.info("We are still allowed to skip");
                        /* Skip track */
                        clipData = getPage(MAINPAGE + "sets/" + playToken + "/skip?player=sm&include=track%5Bfaved%2Bannotation%2Bartist_details%5D&mix_id=" + mixid + "&track_id=" + currenttrackid + "&format=jsonh");
                    }

                    /*
                     * If skip track fails because of too short waittime, even multiple times, we simply wait a minute and try again till we
                     * can finally get to the next track
                     */
                    if (clipData.contains("\"notices\":\"Sorry, but track skips are limited by our license.\"")) {
                        for (int skip_block = 1; skip_block <= 10; skip_block++) {
                            this.sleep(WAITTIME_SECONDS_SKIPLIMIT * 1000l, downloadLink);
                            // Maybe listened to the track -> Next track
                            clipData = getPage(MAINPAGE + "sets/" + playToken + "/next?player=sm&include=track%5Bfaved%2Bannotation%2Bartist_details%5D&mix_id=" + mixid + "&track_id=" + currenttrackid + "&format=jsonh");
                            if (clipData.contains("\"notices\":\"Sorry, but track skips are limited by our license.\"")) continue;
                            break;

                        }
                        /* In case it fails after 10 minutes */
                        if (clipData.contains("\"notices\":\"Sorry, but track skips are limited by our license.\"")) throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server error", 60 * 60 * 1000l);
                    }
                }
                logger.info("current track: " + i + " // looking for track: " + tracknumber + " // last tracknumber: " + last_track_number);
                AT_END = Boolean.parseBoolean(getClipData("at_end"));
                AT_LAST_TRACK = Boolean.parseBoolean(getClipData("at_last_track"));
                dllink = getDllink();
                currenttrackid = updateTrackID();
                if ((!AT_END && dllink == null) || (!AT_END && dllink != null && dllink.equals(sameLink))) {
                    logger.info(NICE_HOST + ": dllink is null or equals the same link as before, quitting loop");
                    break;
                } else if (dllink != null && i == tracknumber) {
                    break;
                } else {
                    sameLink = dllink;
                }
                if (AT_END) {
                    logger.info(NICE_HOST + ": AT_END --> Leaving loop");
                    break;
                } else if (AT_LAST_TRACK) {
                    logger.info(NICE_HOST + ": AT_LAST_TRACK --> Leaving loop");
                    break;
                }
            }
            if (dllink == null && AT_END) {
                /* Hmm maybe we're too far - next try we should be able to download the track we were looking for */
                logger.info(NICE_HOST + ": Reached the end of the playlist // failed to get track: " + tracknumber + " // last tracknumber: " + last_track_number);
                int timesFailed = downloadLink.getIntegerProperty(NICE_HOSTproperty + "timesfailed_toofar", 0);
                downloadLink.getLinkStatus().setRetryCount(0);
                if (timesFailed <= 2) {
                    timesFailed++;
                    logger.info(NICE_HOST + ": Re-Trying...");
                    downloadLink.setProperty(NICE_HOSTproperty + "timesfailed_toofar", timesFailed);
                    throw new PluginException(LinkStatus.ERROR_RETRY, "Failed to find desired track");
                } else {
                    downloadLink.setProperty(NICE_HOSTproperty + "timesfailed_toofar", Property.NULL);
                    logger.info(NICE_HOST + ": Download not available at the moment - disabling current host!");
                    logger.info(NICE_HOST + ": Failed...");
                    throw new PluginException(LinkStatus.ERROR_FATAL, "Failed to find desired track #1");
                }
            }
            if (dllink == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            if (NEED_LAST_TRACK && !AT_LAST_TRACK) {
                throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "This is not the last track", 5 * 60 * 1000l);
            } else if (!NEED_LAST_TRACK && AT_LAST_TRACK) {
                /* Hmm maybe we're too far - next try we should be able to download the track we were looking for */
                logger.info(NICE_HOST + ": Reached the last track of the playlist // failed to get track: " + tracknumber + " // last tracknumber: " + last_track_number);
                int timesFailed = downloadLink.getIntegerProperty(NICE_HOSTproperty + "timesfailed_toofar2", 0);
                downloadLink.getLinkStatus().setRetryCount(0);
                if (timesFailed <= 2) {
                    timesFailed++;
                    logger.info(NICE_HOST + ": Re-Trying...");
                    downloadLink.setProperty(NICE_HOSTproperty + "timesfailed_toofar2", timesFailed);
                    throw new PluginException(LinkStatus.ERROR_RETRY, "Failed to find desired track #2");
                } else {
                    downloadLink.setProperty(NICE_HOSTproperty + "timesfailed_toofar2", Property.NULL);
                    logger.info(NICE_HOST + ": Download not available at the moment - disabling current host!");
                    logger.info(NICE_HOST + ": Failed...");
                    throw new PluginException(LinkStatus.ERROR_FATAL, "Failed to find desired track");
                }
            }
            if (getFilename() != null) filename = getFilename();
            logger.info("Updating track ID the last time");
            currenttrackid = updateTrackID();
            downloadLink.setProperty("trackid", currenttrackid);
            finallink = getFinalDirectlink(dllink);
            if (finallink == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        if (filename == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        if (finallink.contains(".mp3")) ext = "mp3";
        if (ext == null && finallink.contains(".")) ext = finallink.substring(finallink.lastIndexOf(".") + 1);
        if (ext == null || ext.equals("m4a") || ext.length() > 5) ext = "m4a";
        if (tracknumber != -1) {
            filename = tracknumber + "." + filename;
            downloadLink.setProperty("final_filename", filename);
            downloadLink.setFinalFileName(filename + "." + ext);
        } else {
            downloadLink.setProperty("final_filename", filename);
            downloadLink.setFinalFileName(filename + "." + ext);
        }

        dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, finallink, true, 1);
        if (dl.getConnection().getContentType().contains("html")) {
            br.followConnection();
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        if (dllink != null) downloadLink.setProperty("savedlink", dllink);
        try {
            dl.startDownload();
        } catch (final PluginException e) {
            if (e.getLinkStatus() == LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE) {
                // downloadLink.setProperty("directlink", Property.NULL);
                throw new PluginException(LinkStatus.ERROR_RETRY, "Download error");
            }
        }
    }

    private void startPlaylist(final String playToken, final String mixid) throws IOException, PluginException {
        clipData = br.getPage(MAINPAGE + "sets/" + playToken + "/play?player=sm&include=track%5Bfaved%2Bannotation%2Bartist_details%5D&mix_id=" + mixid + "&format=jsonh");
        currenttrackid = updateTrackID();
    }

    private long getWaitSeconds(final String dllink) {
        long waitSeconds = 0;
        try {
            final Browser br2 = br.cloneBrowser();
            br2.setFollowRedirects(true);
            if (dllink.contains("soundcloud.com/")) {
                accessSoundcloudLink(br2, dllink);
                final String track_duration_millisecs = br2.getRegex("\"duration\":(\\d+)").getMatch(0);
                if (track_duration_millisecs == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                waitSeconds = Long.parseLong(track_duration_millisecs) / 1000;
            } else {
                URLConnectionAdapter con = br2.openGetConnection(dllink);
                if (con.getContentType().contains("html") || con.getResponseCode() == 404) {
                    /* No downloadable content or server error -> Unknown bitrate & waittime */
                    waitSeconds = WAITTIME_SECONDS_DEFAULT;
                } else {
                    final long contentlength = con.getLongContentLength();
                    if (contentlength > 0) {
                        if (dllink.contains("8tracks.com/")) {
                            waitSeconds = contentlength / SOURCE_8TRACKS_BITRATE;
                        } else {
                            /* Unknnown source -> unknown bitrate -> Unknown waittime */
                            waitSeconds = WAITTIME_SECONDS_DEFAULT;
                        }
                    }
                }
                con.disconnect();
            }
        } catch (final Throwable e) {
            waitSeconds = WAITTIME_SECONDS_DEFAULT;
        }
        waitSeconds = waitSeconds - WAITTIME_SECONDS_BEFORE_TRACK_PLAYED_CONFIRMATION + WAITTIME_SECONDS_EXTRA;
        return waitSeconds;
    }

    private String checkDirectLink(final DownloadLink downloadLink, final String property) throws IOException, PluginException {
        String dllink = downloadLink.getStringProperty(property);
        dllink = getFinalDirectlink(dllink);
        if (dllink != null) {
            try {
                final Browser br2 = br.cloneBrowser();
                final URLConnectionAdapter con = br2.openGetConnection(dllink);
                if (con.getContentType().contains("html") || con.getLongContentLength() == -1) {
                    downloadLink.setProperty(property, Property.NULL);
                    dllink = null;
                }
                con.disconnect();
            } catch (final Exception e) {
                downloadLink.setProperty(property, Property.NULL);
                dllink = null;
            }
        }
        return dllink;
    }

    private String getFinalDirectlink(final String dlink) throws IOException, PluginException {
        if (dlink != null && dlink.contains("soundcloud.com/")) {
            final Browser br2 = br.cloneBrowser();
            br2.setFollowRedirects(false);
            try {
                accessSoundcloudLink(br2, dlink);
            } catch (final Throwable e) {
            }
            String streamlink = br2.getRegex("\"stream_url\":\"(https?://api\\.soundcloud\\.com/tracks/\\d+/stream)\"").getMatch(0);
            if (streamlink != null) {
                streamlink = jd.plugins.hoster.SoundcloudCom.getDirectlink(streamlink);
            }
            return streamlink;
        } else {
            return dlink;
        }
    }

    private String getDllink() {
        String dllink = null;
        final String soundcloud_trackID = new Regex(clipData, "\"uid\":\"sc\\-(\\d+)\"").getMatch(0);
        if (soundcloud_trackID != null) {
            dllink = "https://api.soundcloud.com/tracks/" + soundcloud_trackID + "/stream?client_id=" + jd.plugins.hoster.SoundcloudCom.CLIENTID;
        } else {
            dllink = getClipData("track_file_stream_url");
        }
        return dllink;
    }

    private void accessSoundcloudLink(final Browser brsc, final String sclink) throws IOException {
        brsc.getPage("https://api.soundcloud.com/tracks/" + getSoundcloudTrackID(sclink) + "?client_id=" + jd.plugins.hoster.SoundcloudCom.CLIENTID + "&app_version=" + jd.plugins.hoster.SoundcloudCom.APP_VERSION + "&format=json");
    }

    private String getSoundcloudTrackID(final String sclink) {
        return new Regex(sclink, "soundcloud\\.com/tracks/(\\d+)").getMatch(0);
    }

    private String getClipData(final String tag) {
        return new Regex(clipData, "\"" + tag + "\"\\s?:\\s?\"?(.*?)\"?,").getMatch(0);
    }

    private String getFilename() {
        String filename = null;
        final Regex name_and_artist = new Regex(clipData, "\"name\":\"([^<>\"]*?)\",\"performer\":\"([^<>\"]*?)\"");
        String album = getClipData("release_name");
        String title = name_and_artist.getMatch(0);
        String artist = name_and_artist.getMatch(1);
        if (album == null || title == null) return null;
        if (album.contains(":")) album = album.substring(0, album.indexOf(":"));
        if (album.equals(title) || isEmpty(album)) album = null;
        title = encodeUnicode(Encoding.htmlDecode(title.trim()));
        artist = encodeUnicode(Encoding.htmlDecode(artist.trim()));
        if (album != null) {
            album = encodeUnicode(Encoding.htmlDecode(album.trim()));
            filename = artist + " - " + album + " - " + title;
        } else {
            filename = artist + " - " + title;
        }
        return filename;
    }

    private String updateTrackID() throws PluginException {
        return getClipData("id");
    }

    private String encodeUnicode(final String input) {
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

    private static synchronized String unescape(final String s) {
        /* we have to make sure the youtube plugin is loaded */
        if (pluginloaded == false) {
            final PluginForHost plugin = JDUtilities.getPluginForHost("youtube.com");
            if (plugin == null) throw new IllegalStateException("youtube plugin not found!");
            pluginloaded = true;
        }
        return jd.plugins.hoster.Youtube.unescape(s);
    }

    private void prepBr() {
        br.setFollowRedirects(false);
        br.setReadTimeout(90 * 1000);
        /* This UA will give us better audio quality */
        br.getHeaders().put("User-Agent", "Mozilla/5.0 (webOS/2.1.0; U; en-US) AppleWebKit/532.2 (KHTML, like Gecko) Version/1.0 Safari/532.2 Pre/1.2");
    }

    private String getPage(final String url) throws IOException {
        br.getHeaders().put("Referer", MAIN_LINK);
        br.getHeaders().put("X-Requested-With", "XMLHttpRequest");
        br.getHeaders().put("Accept", "application/json, text/javascript, */*; q=0.01");
        br.getPage(url);
        return br.toString();
    }

    private void setCookies(final String playToken) {
        br.setCookie(MAINPAGE, "play_token", playToken);
        br.setCookie(MAINPAGE, "initial_source", "");
    }

    private boolean isEmpty(final String ip) {
        return ip == null || ip.trim().length() == 0;
    }

    private long getLongProperty(final Property link, final String key, final long def) {
        try {
            return link.getLongProperty(key, def);
        } catch (final Throwable e) {
            try {
                Object r = link.getProperty(key, def);
                if (r instanceof String) {
                    r = Long.parseLong((String) r);
                } else if (r instanceof Integer) {
                    r = ((Integer) r).longValue();
                }
                final Long ret = (Long) r;
                return ret;
            } catch (final Throwable e2) {
                return def;
            }
        }
    }

    private long getLongPropertyPluginCfg(final String key, final long def) {
        try {
            return this.getPluginConfig().getLongProperty(key, def);
        } catch (final Throwable e) {
            try {
                Object r = this.getPluginConfig().getProperty(key, def);
                if (r instanceof String) {
                    r = Long.parseLong((String) r);
                } else if (r instanceof Integer) {
                    r = ((Integer) r).longValue();
                }
                final Long ret = (Long) r;
                return ret;
            } catch (final Throwable e2) {
                return def;
            }
        }
    }

    @Override
    public void reset() {
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return 1;
    }

    @Override
    public void resetDownloadlink(final DownloadLink link) {
    }

}