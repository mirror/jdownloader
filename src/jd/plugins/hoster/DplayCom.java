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
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;

import jd.PluginWrapper;
import jd.config.ConfigContainer;
import jd.config.ConfigEntry;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;
import jd.plugins.components.PluginJSonUtils;
import jd.utils.locale.JDL;

import org.appwork.utils.formatter.TimeFormatter;
import org.jdownloader.downloader.hls.HLSDownloader;
import org.jdownloader.plugins.components.hls.HlsContainer;
import org.jdownloader.scripting.JavaScriptEngineFactory;

@HostPlugin(revision = "$Revision$", interfaceVersion = 3, names = { "it.dplay.com", "dplay.se", "dplay.dk" }, urls = { "https?://it\\.dplay\\.com/.+|https?://it\\.dplay\\.com/\\?p=\\d+", "http://(?:www\\.)?dplay\\.se/.+|https?://(?:www\\.)?dplay\\.se/\\?p=\\d+", "http://(?:www\\.)?dplay\\.dk/.+|https?://(?:www\\.)?dplay\\.dk/\\?p=\\d+" })
public class DplayCom extends PluginForHost {

    public DplayCom(PluginWrapper wrapper) {
        super(wrapper);
        setConfigElements();
    }

    @Override
    public String getAGBLink() {
        return "http://www.dplay.com/";
    }

    private static final String           TYPE_SHORT            = "https?://[^/]+/\\?p=\\d+";

    /* The list of qualities displayed to the user */
    private final String[]                FORMATS               = new String[] { "1920x1080", "1280x720", "1024x576", "768x432", "640x360", "480x270", "320x180" };
    private final String                  SELECTED_VIDEO_FORMAT = "SELECTED_VIDEO_FORMAT";

    private String                        dllink                = null;
    private boolean                       geo_blocked           = false;
    private boolean                       server_issues         = false;
    private LinkedHashMap<String, Object> entries               = null;
    private String                        videoid               = null;
    private String                        host                  = null;

    /**
     * Example hds: <br />
     * // * http://dplayit-vh.akamaihd.net/z/bc/dplayit/4026928142001/201511/4026928142001,_4631567816001,_4631576081001,_4631576666001,
     * _4631579343001,_4631582324001,_4631584222001,_4631588747001,_4631514944001.mp4.csmil/manifest.f4m<br />
     * hls:<br />
     * http://dplayit-vh.akamaihd.net/i/bc/dplayit/4026928142001/201511/4026928142001,_4631567816001,_4631576081001,_4631576666001,
     * _4631579343001,_4631582324001,_4631584222001,_4631588747001,_4631514944001.mp4.csmil/master.m3u8<br />
     * */
    @SuppressWarnings({ "deprecation", "unchecked" })
    @Override
    public AvailableStatus requestFileInformation(final DownloadLink link) throws Exception {
        dllink = null;
        geo_blocked = false;
        server_issues = false;
        br.setAllowedResponseCodes(new int[] { 400 });

        this.setBrowserExclusive();
        br.setFollowRedirects(true);
        host = new Regex(link.getDownloadURL(), "https?://([^/]+)/").getMatch(0);

        if (link.getDownloadURL().matches(TYPE_SHORT)) {
            videoid = new Regex(link.getDownloadURL(), "(\\d+)$").getMatch(0);
        } else {
            br.getPage(link.getDownloadURL());
            if (this.br.getHttpConnection().getResponseCode() == 404) {
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            }
            /* Try to find the old videoID anyways */
            videoid = this.br.getRegex("data\\-video\\-id=\"(\\d+)\"").getMatch(0);
            if (videoid == null) {
                videoid = this.br.getRegex("page page\\-id\\-(\\d+)").getMatch(0);
            }
            if (videoid == null) {
                videoid = this.br.getRegex("dplay\\.com/\\?p=(\\d+)\\'").getMatch(0);
            }
        }
        String filename = null;
        String date;
        String date_formatted;
        if (videoid != null && this.dllink == null) {
            /* 2017-03-10: Still active for dplay.dk and dplay.se */
            br.setCookie(this.br.getHost(), "notifications_cookie-notification", "submitted");
            this.br.getHeaders().put("X-Requested-With", "XMLHttpRequest");
            /*
             * Using only their API we can get around the brightcove player limitations and bypass geo-blocks from it.dplay.com (e.g. for
             * Germany) - sadly this doesn't work for dplay.dk and dplay.se.
             */
            this.br.getPage("http://" + host + "/api/v2/ajax/videos?video_id=" + videoid + "&page=0&items=500");
            if (br.getHttpConnection().getResponseCode() != 200) {
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            }
            entries = (LinkedHashMap<String, Object>) JavaScriptEngineFactory.jsonToJavaObject(br.toString());

            final long numberof_items = JavaScriptEngineFactory.toLong(entries.get("items"), 0);
            if (numberof_items == 0) {
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            }

            entries = (LinkedHashMap<String, Object>) JavaScriptEngineFactory.walkJson(entries, "data/{0}");
            final String video_metadata_type = (String) entries.get("video_metadata_type");
            date = (String) entries.get("created");
            final DecimalFormat df = new DecimalFormat("00");
            short season;
            short episode;
            String season_str = (String) entries.get("video_metadata_season");
            if (season_str == null) {
                season_str = (String) entries.get("season");
            }
            String episode_str = (String) entries.get("video_metadata_episode");
            if (episode_str == null) {
                episode_str = (String) entries.get("episode");
            }
            final String show = (String) entries.get("video_metadata_show");
            final String description = (String) entries.get("video_metadata_longDescription");
            if (inValidate(show) || inValidate(date) || inValidate(video_metadata_type)) {
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            date_formatted = formatDate(date);

            filename = date_formatted + "_dplay_" + show;

            if (video_metadata_type.equals("episode") && !inValidate(season_str) && !inValidate(episode_str)) {
                season = Short.parseShort(season_str);
                episode = Short.parseShort(episode_str);
                filename += "_S" + df.format(season) + "E" + df.format(episode);
            }
            filename += ".mp4";

            filename = Encoding.htmlDecode(filename);
            filename = encodeUnicode(filename);

            if (!inValidate(description)) {
                link.setComment(description);
            }
        } else {
            /* 2017-03-10: For it.dplay.com there seems to be no way around other than using the Brightcove player. */
            String title = this.br.getRegex("property=\"og:title\"\\s*?content=\"([^<>\"]+) \\| Dplay\"").getMatch(0);
            if (title == null) {
                title = new Regex(this.br.getURL(), "([^/]+)/?$").getMatch(0);
            }
            if (title == null) {
                /* Final fallback */
                title = new Regex(link.getDownloadURL(), "https?://[^/]+/(.+)").getMatch(0).replace("/", "_");
            }
            date = this.br.getRegex("poster=\"https?://[^/]+/(\\d{4}/\\d{2}/\\d{2})/").getMatch(0);
            date_formatted = formatDate(date);
            if (date_formatted != null) {
                filename = date_formatted + "_";
            }
            filename += "dplay_" + title + ".mp4";
            filename = Encoding.htmlDecode(filename);

            /* Find downloadlink / make sure this actually is a video! */
            String accountid = jd.plugins.decrypter.BrightcoveDecrypter.brightcoveEdgeRegexIDAccount(this.br);
            if (accountid == null) {
                /* Fallback - use static value */
                accountid = "4026928142001";
            }
            final String policyKey = jd.plugins.decrypter.BrightcoveDecrypter.getPolicyKey(this.br, accountid);
            if (policyKey == null) {
                /* Probably not a video */
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            }
            final String authorizationValue;
            /* 2017-03-16: Try hardcoded header ... */
            final boolean useStaticAuthorizationValue = true;
            if (useStaticAuthorizationValue) {
                authorizationValue = "Bearer eyJ0eXAiOiJKV1QiLCJhbGciOiJIUzI1NiJ9.eyJzdWIiOiJVU0VSSUQ6ZHBsYXlpdDo2ZTRhOTIyNS1iNzk2LTQ3ZjItYTg5Zi1mZmFhYjBiNmMxZjUiLCJhbm9ueW1vdXMiOnRydWUsImlhdCI6MTQ4OTYxNzgzNCwianRpIjoidG9rZW4tYTcyYWJiZmEtNjRkZC00MzQxLTg5MTUtMGU0MGZmYjM3Zjk3In0=.7YxUHBznpIYHTe2V5cGu_5uw0vsJjDv3Qs0vKIwUk2w=";
            } else {
                authorizationValue = "Bearer " + policyKey;
            }
            // this.br.getHeaders().put("Accept", "Bearer " + policyKey);
            this.br.getHeaders().put("Accept", "*/*");
            this.br.getHeaders().put("Authorization", authorizationValue);
            final String api_url = this.br.getRegex("(https?://[^/]+/playback/videoPlaybackInfo/\\d+)").getMatch(0);
            if (api_url == null) {
                /* Probably not a video */
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            }
            this.br.getPage(api_url);
            if (this.br.getHttpConnection().getResponseCode() == 400) {
                server_issues = true;
            } else if (this.br.getHttpConnection().getResponseCode() == 403) {
                geo_blocked = true;
            } else {
                dllink = PluginJSonUtils.getJsonValue(this.br, "url");
            }
        }
        link.setFinalFileName(filename);
        return AvailableStatus.TRUE;
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    @Override
    public void handleFree(final DownloadLink downloadLink) throws Exception, PluginException {
        requestFileInformation(downloadLink);
        if (this.entries != null) {
            boolean isFree = false;
            try {
                String video_metadata_string = null;
                final Object video_metadata_packageo = entries.get("video_metadata_package");
                if (video_metadata_packageo == null) {
                    /* No package-information given --> Video should be free */
                    isFree = true;
                } else if (video_metadata_packageo instanceof String) {
                    video_metadata_string = (String) video_metadata_packageo;
                    if (video_metadata_string.equals("Packages-Free")) {
                        isFree = true;
                    }
                } else {
                    final ArrayList<Object> video_metadata_package = (ArrayList) video_metadata_packageo;
                    for (final Object video_metadata_object : video_metadata_package) {
                        video_metadata_string = (String) video_metadata_object;
                        if (video_metadata_string.equals("Packages-Free")) {
                            isFree = true;
                        }
                    }
                }
            } catch (final Throwable e) {
            }
            if (!isFree) {
                logger.info("This content is only available for premium users");
                throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_ONLY);
            }
            /*
             * E.g. single renditions via quality-IDs inside main HLS/HDS URLs:
             * http://c.brightcove.com/services/mobile/streaming/index/rendition.m3u8?assetId=4489207274001
             */
            /* E.g. 4631514944001 */
            final String brightcove_videoid = (String) entries.get("video_metadata_id");
            String hls_main = null;
            if (host.equalsIgnoreCase("it.dplay.com")) {
                hls_main = (String) entries.get("hls");
            } else {
                final String country_needed = this.host.substring(host.lastIndexOf(".") + 1).toUpperCase();
                // br.setCookie("dplay.se", "s_fid", "06BC08F6EC982DC7-27BEDB5190979A80");
                br.setCookie(this.br.getHost(), "dsc-geo", "%7B%22countryCode%22%3A%22" + country_needed + "%22%2C%22expiry%22%3A" + (System.currentTimeMillis() + 24 * 60 * 60 * 1000) + "%7D");
                this.br.getPage("https://secure." + host.replace("www.", "") + "/secure/api/v2/user/authorization/stream/" + videoid + "?stream_type=hls");
                entries = (LinkedHashMap<String, Object>) JavaScriptEngineFactory.jsonToJavaObject(br.toString());
                try {
                    /* Not necessarily needed */
                    this.br.getPage("https://secure." + host + "/secure/api/v2/user/authorization/stream/" + videoid + "?authorisation_pulse=1");
                } catch (final Throwable e) {
                }
                hls_main = (String) entries.get("hls");
            }
            if (inValidate(hls_main) && !inValidate(brightcove_videoid)) {
                /* Fallback to mobile brightcove hls (also has ALL renditions available!) - this should NEVER be needed! */
                hls_main = jd.plugins.decrypter.BrightcoveDecrypter.getHlsMasterHttp(brightcove_videoid);
            }
            if (inValidate(hls_main)) {
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            br.getPage(hls_main);

            if (this.br.getHttpConnection().getResponseCode() == 403) {
                throw new PluginException(LinkStatus.ERROR_FATAL, "This content is not available in your country");
            }

            HlsContainer hlsSelected = null;
            final String selectedResolution = getConfiguredVideoFormat();
            final List<HlsContainer> allContainers = HlsContainer.getHlsQualities(this.br);
            for (final HlsContainer currentContainer : allContainers) {
                final String currResolution = currentContainer.getResolution();
                if (currResolution.equals(selectedResolution)) {
                    /* We found users' selected videoresolution. */
                    hlsSelected = currentContainer;
                    break;
                }
            }
            if (hlsSelected == null) {
                logger.info("Failed to find user selected videoresolution - trying to download highest resolution possible instead ...");
                hlsSelected = HlsContainer.findBestVideoByBandwidth(allContainers);
            }
            if (hlsSelected == null) {
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            this.dllink = hlsSelected.getDownloadurl();
        } else {
            if (this.geo_blocked) {
                throw new PluginException(LinkStatus.ERROR_FATAL, "GEO-blocked");
            } else if (server_issues) {
                throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Unknown server error", 10 * 60 * 1000l);
            } else if (dllink == null) {
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            this.br.getPage(dllink);
            if (this.br.getHttpConnection().getResponseCode() == 403) {
                /* 2nd GEO-blocked check! */
                throw new PluginException(LinkStatus.ERROR_FATAL, "GEO-blocked");
            }
            final List<HlsContainer> allContainers = HlsContainer.getHlsQualities(this.br);
            final HlsContainer hlsbest = HlsContainer.findBestVideoByBandwidth(allContainers);
            this.dllink = hlsbest.getDownloadurl();
        }
        checkFFmpeg(downloadLink, "Download a HLS Stream");
        dl = new HLSDownloader(downloadLink, br, this.dllink);
        dl.startDownload();
    }

    private String getConfiguredVideoFormat() {
        final int selection = this.getPluginConfig().getIntegerProperty(SELECTED_VIDEO_FORMAT, 0);
        return FORMATS[selection];
    }

    private String formatDate(final String input) {
        if (input == null) {
            return null;
        }
        final long date;
        if (input.matches("\\d{4}/\\d{2}/\\d{2}")) {
            date = TimeFormatter.getMilliSeconds(input, "yyyy/MM/dd", Locale.ENGLISH);
        } else {
            date = TimeFormatter.getMilliSeconds(input, "yyyy-MM-dd HH:mm:ss", Locale.ENGLISH);
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

    private void setConfigElements() {
        getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_COMBOBOX_INDEX, getPluginConfig(), SELECTED_VIDEO_FORMAT, FORMATS, JDL.L("plugins.hoster.DplayCom.prefer_format", "Select preferred videoresolution:")).setDefaultValue(0));
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