//    jDownloader - Downloadmanager
//    Copyright (C) 2009  JD-Team support@jdownloader.org
//
//    This program is free software: you can redistribute it and/or modify
//    it under the terms of the GNU General Public License as published by
//    the Free Software Foundation, either version 3 of the License, or
//    (at your option) any later version.
//
//    This program is distributed in the hope that it will be useful,
//    but WITHOUT ANY WARRANTY; without even the implied warranty of
//    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
//    GNU General Public License for more details.
//
//    You should have received a copy of the GNU General Public License
//    along with this program.  If not, see <http://www.gnu.org/licenses/>.
package jd.plugins.hoster;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;

import org.appwork.utils.StringUtils;
import org.appwork.utils.formatter.TimeFormatter;
import org.jdownloader.controlling.filter.CompiledFiletypeFilter;
import org.jdownloader.downloader.hds.HDSDownloader;
import org.jdownloader.downloader.hls.HLSDownloader;
import org.jdownloader.plugins.components.config.MediathekProperties;
import org.jdownloader.plugins.components.config.TvnowConfigInterface;
import org.jdownloader.plugins.components.config.TvnowConfigInterface.Quality;
import org.jdownloader.plugins.components.hls.HlsContainer;
import org.jdownloader.plugins.config.PluginConfigInterface;
import org.jdownloader.plugins.config.PluginJsonConfig;
import org.jdownloader.scripting.JavaScriptEngineFactory;

import jd.PluginWrapper;
import jd.http.Browser;
import jd.http.Cookies;
import jd.http.requests.PostRequest;
import jd.parser.Regex;
import jd.plugins.Account;
import jd.plugins.Account.AccountType;
import jd.plugins.AccountInfo;
import jd.plugins.AccountRequiredException;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.FilePackage;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;
import jd.plugins.components.MediathekHelper;
import jd.plugins.components.PluginJSonUtils;

@HostPlugin(revision = "$Revision$", interfaceVersion = 3, names = { "tvnow.de" }, urls = { "tvnowdecrypted://.+" })
public class TvnowDe extends PluginForHost {
    public TvnowDe(final PluginWrapper wrapper) {
        super(wrapper);
        this.enablePremium("https://my.tvnow.de/registrierung");
    }

    /* Settings */
    /* Tags: rtl-interactive.de, RTL, rtlnow, rtl-now */
    private static final String           TYPE_GENERAL_ALRIGHT           = "https?://[^/]+/[^/]+/[a-z0-9\\-]+/[^/\\?]+";
    /* Old + new movie-linktype */
    public static final String            TYPE_MOVIE_OLD                 = "https?://[^/]+/[^/]+/[^/]+";
    public static final String            TYPE_MOVIE_NEW                 = "https?://[^/]+/filme/.+";
    public static final String            TYPE_SERIES_SINGLE_EPISODE_NEW = "https?://[^/]+/(?:serien|shows)/([^/]+)/(?:[^/]+/)?(?!staffel\\-\\d+)([^/]+)$";
    public static final String            TYPE_DEEPLINK                  = "^[a-z]+://link\\.[^/]+/.+";
    public static final String            API_BASE                       = "https://api.tvnow.de/v3";
    private static final String           API_NEW_BASE                   = "https://apigw.tvnow.de";
    public static final String            CURRENT_DOMAIN                 = "tvnow.de";
    private LinkedHashMap<String, Object> entries                        = null;
    private boolean                       usingNewAPI                    = false;

    public static Browser prepBRAPI(final Browser br) {
        br.getHeaders().put("Accept", "application/json, text/plain, */*");
        br.getHeaders().put("Content-Type", "application/json");
        /* 400-bad request for invalid API requests */
        br.setAllowedResponseCodes(new int[] { 400 });
        br.setFollowRedirects(false);
        return br;
    }

    public static boolean isMovie(final String url) {
        return url.matches(TYPE_MOVIE_OLD) || url.matches(TYPE_MOVIE_NEW);
    }

    public static boolean isMovie_old(final String url) {
        return url.matches(TYPE_MOVIE_OLD) && !url.matches(TYPE_MOVIE_NEW) && !isSeriesSingleEpisodeNew(url) && !isSeriesNew(url);
    }

    public static boolean isSeriesSingleEpisodeNew(final String url) {
        // return url.matches(TYPE_SERIES_SINGLE_EPISODE_NEW) && !url.matches("https?://[^/]+/(?:serien|shows)/[^/]+/staffel\\-\\d+$");
        return url.matches(TYPE_SERIES_SINGLE_EPISODE_NEW);
    }

    public static boolean isSeriesNew(final String url) {
        return (url.matches("^https?://[^/]+/(?:serien|shows)/([^/]+)(?:/staffel\\-\\d+)?") || url.matches(".+/premium\\?typ=format\\&formatname=.+") || url.matches("^https?://[^/]+/specials/[^/]+$") && !isSeriesSingleEpisodeNew(url));
    }

    @Override
    public void correctDownloadLink(final DownloadLink link) throws Exception {
        /* First lets get our source url and remove the unneeded parts */
        String urlNew;
        if (link.getPluginPatternMatcher().matches(TYPE_DEEPLINK)) {
            /* https not possible for this linktype!! */
            urlNew = link.getPluginPatternMatcher().replaceAll("tvnowdecrypted://", "http://");
        } else {
            urlNew = link.getPluginPatternMatcher().replaceAll("tvnowdecrypted://", "https://");
        }
        link.setPluginPatternMatcher(urlNew);
    }

    /**
     * ~2015-05-01 Available HLS AND HDS streams are DRM protected <br />
     * ~2015-07-01: HLS streams were turned off <br />
     * ~2016-01-01: RTMP(E) streams were turned off / all of them are DRM protected/crypted now<br />
     * ~2016-02-24: Summary: There is absolutely NO WAY to download from this website <br />
     * ~2016-03-15: Domainchange from nowtv.de to tvnow.de<br />
     * .2018-04-17: Big code cleanup and HLS streams were re-introduced<br />
     * .2019-01-16: Added FHD download via hlsfairplayhd, added account support <br />
     */
    @SuppressWarnings({ "unchecked" })
    @Override
    public AvailableStatus requestFileInformation(final DownloadLink link) throws Exception {
        /* In case anything serious goes wrong user should still be able to see that this is supposed to be a video-file. */
        link.setMimeHint(CompiledFiletypeFilter.VideoExtensions.MP4);
        setBrowserExclusive();
        /* Fix old urls */
        correctDownloadLink(link);
        prepBRAPI(this.br);
        /* Required to access items via API and also used as linkID */
        final String urlpart = getURLPart(link);
        // ?fields=*,format,files,manifest,breakpoints,paymentPaytypes,trailers,packages,isDrm
        /*
         * Explanation of possible but left-out parameters: "breakpoints" = timecodes when ads are delivered, "paymentPaytypes" = how can
         * this item be purchased and how much does it cost, "trailers" = trailers, "files" = old rtlnow URLs, see plugin revision 38232 and
         * earlier.
         */
        br.getPage(API_BASE + "/movies/" + urlpart + "?fields=" + getFields());
        if (br.getHttpConnection().getResponseCode() != 200) {
            logger.info("URL might be offline");
            /*
             * Some content (very rare case) can only be accessed via new API as we are sometimes unable to find the correct parameters for
             * the old API (well, or some content simply isn't accessible via old API anymore).
             */
            logger.info("Attempting special offline workaround via new API");
            /* If the content is offline (as we suspect), that function will throw ERROR_FILE_NOT_FOUND. */
            accessStreamInfoViaNewAPI(link);
            logger.info("Special offline workaround successful: Content is online");
        }
        entries = (LinkedHashMap<String, Object>) JavaScriptEngineFactory.jsonToJavaObject(br.toString());
        String tv_station = null;
        String formatTitle = null;
        if (!this.usingNewAPI) {
            final LinkedHashMap<String, Object> format = (LinkedHashMap<String, Object>) entries.get("format");
            tv_station = (String) format.get("station");
            formatTitle = (String) format.get("title");
        }
        return parseInformation(link, entries, tv_station, formatTitle, this.usingNewAPI);
    }

    /** Returns parameters for API 'fields=' key. Only request all fields we actually need. */
    public static String getFields() {
        return "*,format,isDrm";
    }

    /** Parses API json to find important downloadlink properties. */
    public static AvailableStatus parseInformation(final DownloadLink link, LinkedHashMap<String, Object> entries, final String tv_station, String formatTitle, final boolean newAPI) {
        /* In case anything serious goes wrong user should still be able to see that this is supposed to be a video-file. */
        link.setMimeHint(CompiledFiletypeFilter.VideoExtensions.MP4);
        final boolean isFree;
        final boolean isDRM;
        // final boolean isStrictDrm1080p;
        final boolean geoBLOCKED;
        String date = null;
        final String description;
        final int season;
        if (newAPI) {
            final long code = JavaScriptEngineFactory.toLong(entries.get("code"), 0);
            final String error = (String) entries.get("error");
            // if ("User not authorized!".equalsIgnoreCase(error)) {
            if (code == 403) {
                /* Paid content - goes along with response 403, also json will not contain anything else but the thumbnail-URL. */
                /*
                 * 2019-08-12: E.g. "{"code":403,"message":"User is not allowed to see this movie with the current
                 * subscription","errorType":"premium","image":"https://CENSORED"}"
                 */
                isFree = false;
                /* Just assumptions - no way to find out without account at this stage. */
                isDRM = false;
                geoBLOCKED = false;
            } else {
                isFree = true;
                isDRM = ((Boolean) JavaScriptEngineFactory.walkJson(entries, "rights/isDrm"));
                /* TODO: Find out what this means? 1080p = DRM protected, other qualities not? */
                // isStrictDrm1080p = ((Boolean) JavaScriptEngineFactory.walkJson(entries, "rights/isStrictDrm1080p"));
                geoBLOCKED = ((Boolean) JavaScriptEngineFactory.walkJson(entries, "config/boards/geoBlocking/block"));
                entries = (LinkedHashMap<String, Object>) JavaScriptEngineFactory.walkJson(entries, "config/source");
                /* TODO: Re-Check this - this might not be the same as "broadcastDate" in the old API! */
                date = (String) entries.get("previewStart");
                if (StringUtils.isEmpty(formatTitle)) {
                    formatTitle = (String) entries.get("format");
                }
            }
            /* Not given */
            description = null;
            season = -1;
        } else {
            isFree = ((Boolean) entries.get("free")).booleanValue();
            isDRM = ((Boolean) entries.get("isDrm")).booleanValue();
            // isStrictDrm1080p = ((Boolean) entries.get("isStrictDrm1080p")).booleanValue();;
            geoBLOCKED = ((Boolean) entries.get("geoblocked"));
            date = (String) entries.get("broadcastStartDate");
            description = (String) entries.get("articleLong");
            season = (int) JavaScriptEngineFactory.toLong(entries.get("season"), -1);
        }
        final MediathekProperties data = link.bindData(MediathekProperties.class);
        final String episode_url_str = new Regex(link.getPluginPatternMatcher(), "folge\\-(\\d+)").getMatch(0);
        final String episodeStr = getEpisodeNumber(entries);
        int episode = (episodeStr != null && episodeStr.matches("\\d+")) ? Integer.parseInt(episodeStr) : -1;
        if (episode == -1 && episode_url_str != null && episode_url_str.matches("\\d+")) {
            /* Fallback which should usually not be required */
            episode = (int) Long.parseLong(episode_url_str);
        }
        /* Title or subtitle of a current series-episode */
        String filename_beginning = "";
        final AvailableStatus status;
        if (isDRM) {
            final TvnowConfigInterface cfg = PluginJsonConfig.get(org.jdownloader.plugins.components.config.TvnowConfigInterface.class);
            filename_beginning = "[DRM]";
            if (cfg.isEnableDRMOffline()) {
                /* Show as offline although it is online ... but we cannot download it anyways! */
                link.setAvailable(false);
                status = AvailableStatus.FALSE;
            } else {
                /* Show as online although we cannot download it */
                link.setAvailable(true);
                status = AvailableStatus.TRUE;
            }
        } else {
            /* Show as online as it is downloadable and online */
            link.setAvailable(true);
            status = AvailableStatus.TRUE;
        }
        /* Important: Download-routine relies in this information!! */
        link.setProperty("isFREE", isFree);
        link.setProperty("isDRM", isDRM);
        link.setProperty("isGEOBLOCKED", geoBLOCKED);
        final String final_filename = link.getFinalFileName();
        String title = (String) entries.get("title");
        /*
         * Only set filename if we're using the old API or we're using the new API and crawler did not set filename before. Especially for
         * PREMIUMONLY content, new API will return nearly no information at all - basically only status and thumbnail!
         */
        final boolean filename_available = !StringUtils.isEmpty(title) && !StringUtils.isEmpty(formatTitle) && !StringUtils.isEmpty(date);
        final boolean set_filename = (!newAPI || final_filename == null || !final_filename.endsWith(".mp4")) && filename_available;
        if (set_filename) {
            title = title.trim();
            data.setShow(formatTitle);
            if (isValidTvStation(tv_station)) {
                data.setChannel(tv_station);
            }
            data.setReleaseDate(getDateMilliseconds(date));
            if (season != -1 && episode != -1) {
                data.setSeasonNumber(season);
                data.setEpisodeNumber(episode);
                /* Episodenumber is in title --> Remove it as we insert it via 'S00E00' format so we do not need it twice! */
                /* Improve title by removing redundant episodenumber from it. */
                if (title.matches("Folge \\d+")) {
                    /* No usable title available - remove it completely! */
                    title = null;
                } else if (title.matches("Folge \\d+: .+")) {
                    title = title.replaceAll("(Folge \\d+: )", "");
                } else if (title.matches(".+ - Folge \\d+")) {
                    title = title.replaceAll("( - Folge \\d+)", "");
                }
            }
            if (!StringUtils.isEmpty(title)) {
                data.setTitle(title);
            }
            final String filename = filename_beginning + MediathekHelper.getMediathekFilename(link, data, false, false);
            try {
                if (FilePackage.isDefaultFilePackage(link.getFilePackage())) {
                    final FilePackage fp = FilePackage.getInstance();
                    fp.setName(formatTitle);
                    fp.add(link);
                }
                if (!StringUtils.isEmpty(description) && link.getComment() == null) {
                    link.setComment(description);
                }
            } catch (final Throwable e) {
            }
            link.setFinalFileName(filename);
        }
        return status;
    }

    /** Returns (modified) episodenumber (source = json) */
    public static String getEpisodeNumber(final LinkedHashMap<String, Object> entries) {
        final Object episodeO = getEpisodeNumberRAW(entries);
        String episodenumber = null;
        if (episodeO != null && episodeO instanceof String) {
            final String episodeTmp = (String) episodeO;
            if (episodeTmp.matches("\\d+")) {
                episodenumber = episodeTmp;
            } else if (episodenumberHasSpecialStringFormat(episodeO)) {
                /* 2019-02-05: Very rare case */
                episodenumber = new Regex(episodeTmp, "V(\\d+)").getMatch(0);
                // System.out.println("WTF_workarounded: " + episodeTmp);
            } else {
                /* 2019-02-05: This should never happen! */
                episodenumber = "-1";
                // System.out.println("WTF: " + episodeTmp);
            }
        } else if (episodeO != null && episodeO instanceof Integer) {
            episodenumber = Integer.toString(((Integer) episodeO).intValue());
        } else if (episodeO != null && episodeO instanceof Long) {
            episodenumber = Long.toString(((Long) episodeO).longValue());
        } else {
            episodenumber = null;
        }
        return episodenumber;
    }

    /** Returns RAW String of episodenumber from json */
    public static Object getEpisodeNumberRAW(final LinkedHashMap<String, Object> entries) {
        return entries.get("episode");
    }

    public static boolean episodenumberHasSpecialStringFormat(final Object episodeO) {
        return episodeO instanceof String && ((String) episodeO).matches("V\\d+");
    }

    public static boolean isValidTvStation(final String tv_station) {
        return !StringUtils.isEmpty(tv_station) && !tv_station.equalsIgnoreCase("none") && !tv_station.equalsIgnoreCase("tvnow");
    }

    /* Last revision with old handling: BEFORE 38232 (30393) */
    private void handleDownload(final DownloadLink downloadLink, final Account acc) throws Exception {
        final TvnowConfigInterface cfg = PluginJsonConfig.get(org.jdownloader.plugins.components.config.TvnowConfigInterface.class);
        final boolean isFree = downloadLink.getBooleanProperty("isFREE", false);
        final boolean isDRM = downloadLink.getBooleanProperty("isDRM", false);
        // final boolean isStrictDrm1080p;
        if (this.usingNewAPI) {
            /* New API was already used in availablecheck (special case) */
            /* TODO: Find out what this means? 1080p = DRM protected, other qualities not? */
            // isStrictDrm1080p = ((Boolean) JavaScriptEngineFactory.walkJson(entries, "rights/isStrictDrm1080p"));
        } else {
            /* Old API was used in availablecheck until now. */
            final String movieID = Long.toString(JavaScriptEngineFactory.toLong(entries.get("id"), -1));
            if (movieID.equals("-1")) {
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            if (isDRM) {
                /* There really is no way to download these videos and if, you will get encrypted trash data so let's just stop here. */
                throw new PluginException(LinkStatus.ERROR_FATAL, "Unsupported streaming type [DRM]");
            }
            /*
             * 2019-01-16: Usage of new API usually (not always) requires auth header --> Only use it in premium mode for now to get (higher
             * quality) stream-URLs
             */
            final boolean useNewAPI = acc != null && acc.getType() == AccountType.PREMIUM;
            if (useNewAPI) {
                accessStreamInfoViaNewAPI(downloadLink);
            } else {
                final String urlpart = getURLPart(downloadLink);
                br.getPage(API_BASE + "/movies/" + urlpart + "?fields=manifest");
            }
            entries = (LinkedHashMap<String, Object>) JavaScriptEngineFactory.jsonToJavaObject(br.toString());
        }
        String hdsMaster = null;
        String hlsMaster = null;
        try {
            /* Make sure not to fail here in case no streams are given! */
            entries = (LinkedHashMap<String, Object>) entries.get("manifest");
            /* 2018-04-18: So far I haven't seen a single http stream! */
            // final String urlHTTP = (String) entries.get("hbbtv");
            hdsMaster = (String) entries.get("hds");
            hlsMaster = (String) entries.get("hlsfairplayhd");
            if (StringUtils.isEmpty(hlsMaster) || !hlsMaster.startsWith("http")) {
                hlsMaster = (String) entries.get("hlsfairplay");
                if (StringUtils.isEmpty(hlsMaster) || !hlsMaster.startsWith("http")) {
                    hlsMaster = (String) entries.get("hlsclear");
                }
                /* 2018-05-04: Only "hls" == Always DRM */
                // if (StringUtils.isEmpty(hlsMaster)) {
                // hlsMaster = (String) entries.get("hls");
                // }
            }
        } catch (final Throwable e) {
        }
        if (!StringUtils.isEmpty(hlsMaster)) {
            hlsMaster = hlsMaster.replaceAll("(\\??filter=.*?)(&|$)", "");// show all available qualities
            /*
             * 2019-01-29: Error 404 may happen for content which is premiumonly (for ALL streaming-types, URLs are given but do not work!)
             * It may also happen that content is broken, all sources return 404 and the same happens with acount.
             */
            br.getPage(hlsMaster);
            /* Find user-preferred quality */
            final Quality preferredQuality = cfg.getPreferredQuality();
            final String preferredQualityString = selectedQualityEnumToQualityString(preferredQuality);
            final boolean preferBEST = preferredQuality == Quality.BEST;
            final List<HlsContainer> hlsQualities = HlsContainer.getHlsQualities(br);
            HlsContainer hlsDownloadCandidate = null;
            if (preferBEST) {
                hlsDownloadCandidate = HlsContainer.findBestVideoByBandwidth(hlsQualities);
            } else {
                for (final HlsContainer currentHlsQuality : hlsQualities) {
                    final String qualityStringTemp = bandwidthToQualityString(currentHlsQuality.getBandwidth());
                    if (qualityStringTemp.equalsIgnoreCase(preferredQualityString)) {
                        hlsDownloadCandidate = currentHlsQuality;
                        break;
                    }
                }
                if (hlsDownloadCandidate != null) {
                    logger.info("Found preferred quality: " + preferredQualityString);
                } else {
                    /* Fallback */
                    logger.info("Failed to find preferred quality: " + preferredQualityString);
                    hlsDownloadCandidate = HlsContainer.findBestVideoByBandwidth(hlsQualities);
                    if (hlsDownloadCandidate != null) {
                        logger.info("Downloading best quality instead: " + hlsDownloadCandidate.getResolution());
                    }
                }
            }
            if (hlsDownloadCandidate == null) {
                errorNoDownloadurlFound(acc, isFree);
            }
            if (downloadLink.getComment() == null || cfg.isShowQualityInfoInComment()) {
                downloadLink.setComment(hlsDownloadCandidate.toString());
            }
            logger.info("Downloading quality: " + hlsDownloadCandidate.toString());
            checkFFmpeg(downloadLink, "Download a HLS Stream");
            try {
                dl = new HLSDownloader(downloadLink, br, hlsDownloadCandidate.getDownloadurl());
            } catch (final Throwable e) {
                /*
                 * 2017-11-15: They've changed these URLs to redirect to image content (a pixel). Most likely we have a broken HLS url -->
                 * Download not possible, only crypted HDS available.
                 */
                throw new PluginException(LinkStatus.ERROR_FATAL, "Unsupported streaming type [DRM]", e);
            }
            dl.startDownload();
        } else {
            /* hds */
            errorNoDownloadurlFound(acc, isFree);
            // /* Now we're sure that our .mp4 availablecheck-filename is correct */
            // downloadLink.setFinalFileName(downloadLink.getName());
            // /* TODO */
            // if (true) {
            // // throw new PluginException(LinkStatus.ERROR_FATAL, "Unsupported streaming type [HDS]");
            // throw new PluginException(LinkStatus.ERROR_FATAL, "Unsupported streaming type / encrypted HDS");
            // }
            // if (url_hds.matches(this.HDSTYPE_NEW_DETAILED)) {
            // throw new PluginException(LinkStatus.ERROR_FATAL, "Unsupported streaming type / encrypted HDS");
            // }
            // if (dllink.matches(this.HDSTYPE_NEW_MANIFEST)) {
            // logger.info("2nd attempt to get final hds url");
            // /* TODO */
            // if (true) {
            // throw new PluginException(LinkStatus.ERROR_FATAL, "Unsupported streaming type / encrypted HDS");
            // }
            // final XPath xPath = xmlParser(dllink);
            // final NodeList nl = (NodeList) xPath.evaluate("/manifest/media", doc, XPathConstants.NODESET);
            // final Node n = nl.item(0);
            // dllink = n.getAttributes().getNamedItem("href").getTextContent();
            // }
            // br.getPage(dllink);
            // final String hds = parseManifest();
            // dl = new HDSDownloader(downloadLink, br, url_hds);
            // dl.startDownload();
        }
    }

    /**
     * Access stream-information via apigw.tvnow.de/module/player/<episodeID> <br />
     */
    private void accessStreamInfoViaNewAPI(final DownloadLink link) throws Exception {
        logger.info("Trying to get streams via new API");
        final String episodeID = link.getStringProperty("id_episode", null);
        if (StringUtils.isEmpty(episodeID)) {
            logger.info("id_episode is null - content is not downloadable without this id");
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        br.getPage(API_NEW_BASE + "/module/player/" + episodeID);
        if (br.getHttpConnection().getResponseCode() == 404) {
            /* Extra offline-errorhandling & rare case: {"code":404,"message":"movie.not.found"} */
            logger.info("Content offline according to new API");
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        this.usingNewAPI = true;
    }

    private void errorNoDownloadurlFound(final Account acc, final boolean isFree) throws PluginException {
        /* 2019-01-29: TODO: Check if this can also happen when logged-in */
        if (!isFree) {
            logger.info("Only downloadable via premium");
            if (acc != null && acc.getType() == AccountType.PREMIUM) {
                /*
                 * 2019-01-30: If content is not available for freeusers but also fails via premium account this is an indication of
                 * missing/broken content - this shall be a very rare case!
                 */
                throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Failed to find downloadurl: content missing/broken");
            } else if (acc != null) {
                logger.info("Account available --> WTF, maybe content has to be bought individually");
            }
            throw new AccountRequiredException();
        }
        /* Assume that no downloadable stream-type is available. This may also mean that the content is only downloadable via account! */
        throw new PluginException(LinkStatus.ERROR_FATAL, "Unsupported streaming type [DRM] or only downloadable via premium");
    }

    private String selectedQualityEnumToQualityString(final Quality selectedQuality) {
        final String qualitystring;
        switch (selectedQuality) {
        case FHD1080:
            qualitystring = "1080p";
            break;
        case HD720:
            qualitystring = "720p";
            break;
        case SD540HIGH:
            qualitystring = "540phigh";
            break;
        case SD540LOW:
            qualitystring = "540plow";
            break;
        case SD360HIGH:
            qualitystring = "360phigh";
            break;
        case SD360LOW:
            qualitystring = "360plow";
            break;
        default:
            /* BEST */
            qualitystring = null;
        }
        return qualitystring;
    }

    private String bandwidthToQualityString(final int bandwidth) {
        final String qualitystring;
        if (bandwidth > 150000 && bandwidth < 1006000) {
            /* 360p low */
            qualitystring = "360plow";
        } else if (bandwidth >= 1006000 && bandwidth < 1656000) {
            /* 360p high */
            qualitystring = "360phigh";
        } else if (bandwidth >= 1656000 && bandwidth < 3006000) {
            /* 540 low */
            qualitystring = "540plow";
        } else if (bandwidth >= 3006000 && bandwidth < 6006000) {
            /* 540 high */
            qualitystring = "540phigh";
        } else if (bandwidth >= 6006000 && bandwidth < 7000000) {
            /* 720p */
            qualitystring = "720p";
        } else if (bandwidth >= 7000000) {
            /* 1080p */
            qualitystring = "1080p";
        } else {
            qualitystring = "unknown";
        }
        return qualitystring;
    }

    private String getURLPart(final DownloadLink dl) throws PluginException, IOException {
        /* OLD rev: 39908 */
        // return new Regex(dl.getDownloadURL(), "/([a-z0-9\\-]+/[a-z0-9\\-]+)$").getMatch(0);
        /* 2018-12-12: New */
        final String regExPattern_Urlinfo = "https?://[^/]+/[^/]+/([^/]*?)/([^/]+/)?(.+)";
        Regex urlInfo = new Regex(dl.getPluginPatternMatcher(), regExPattern_Urlinfo);
        final String showname_url = urlInfo.getMatch(0);
        final String episodename_url = urlInfo.getMatch(2);
        /* 2018-12-27: TODO: Remove this old code - crawler will store all relevant information on DownloadLink via properties! */
        /* Find relevant information - first check if we've stored that before (e.g. URLs were added via decrypter) */
        String showname = dl.getStringProperty("url_showname", null);
        String episodename = dl.getStringProperty("url_episodetitle", null);
        boolean grabbed_url_info_via_website = false;
        if (StringUtils.isEmpty(showname) || StringUtils.isEmpty(episodename)) {
            /* No stored information available --> URLs have NOT been added via decrypter --> Now it might get a little bit complicated */
            final boolean showname_url_is_unsafe = showname_url == null || (new Regex(showname_url, "(\\-\\d+){1}$").matches() && !new Regex(showname_url, "(\\-\\d+){2}$").matches());
            if (!showname_url_is_unsafe) {
                /* URLs were not added via decrypter --> Try to use information in URL */
                logger.info("Using info from URL");
                showname = showname_url;
                episodename = episodename_url;
            } else if (StringUtils.isEmpty(showname) || StringUtils.isEmpty(episodename)) {
                final String url_old;
                if (dl.getPluginPatternMatcher().matches(TYPE_DEEPLINK)) {
                    /* 2018-12-20: Code unused at the moment */
                    /* TYPE_DEEPLINK --> old_url --> new_url */
                    logger.info("TYPE_DEEPLINK --> old_url");
                    br.getPage(dl.getPluginPatternMatcher());
                    if (br.getHttpConnection().getResponseCode() == 404) {
                        throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
                    }
                    url_old = br.getRegex("webLink = \\'(https?://[^<>\"\\']+)\\'").getMatch(0);
                } else {
                    /* old_url --> new_url */
                    url_old = dl.getPluginPatternMatcher();
                }
                if (url_old == null) {
                    logger.warning("Failed to find old_url");
                    throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                }
                logger.info("Expecting redirect from old linktype to new linktype");
                final boolean follow_redirects_setting_before = br.isFollowingRedirects();
                br.setFollowRedirects(false);
                br.getPage(url_old);
                /* Old linkformat should redirect to new linkformat */
                final String redirecturl = br.getRedirectLocation();
                /*
                 * We accessed the main-URL so it makes sense to at least check for a 404 at this stage to avoid requestion potentially dead
                 * URLs again via API!
                 */
                if (br.getHttpConnection().getResponseCode() == 404) {
                    throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
                } else if (redirecturl == null) {
                    logger.warning("Redirect to new linktype failed");
                    throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                }
                br.setFollowRedirects(follow_redirects_setting_before);
                logger.info("URL_old: " + dl.getPluginPatternMatcher() + " | URL_new: " + redirecturl);
                /* Cleanup for API requests if values haven't been set in crawler before */
                urlInfo = new Regex(redirecturl, regExPattern_Urlinfo);
                showname = urlInfo.getMatch(0);
                episodename = urlInfo.getMatch(2);
                if (StringUtils.isEmpty(showname) || StringUtils.isEmpty(episodename)) {
                    logger.warning("Failed to extract urlInfo from URL_new");
                    throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                }
                grabbed_url_info_via_website = true;
            }
            if (StringUtils.isEmpty(showname) || StringUtils.isEmpty(episodename)) {
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            showname = cleanupShowTitle(showname);
            episodename = cleanupEpisodeTitle(episodename);
            if (grabbed_url_info_via_website) {
                /* Store information so we do not have to access that URL without API ever again. */
                storeUrlPartInfo(dl, showname, episodename, null, null, null);
                /* Store this info for future error cases */
                dl.setProperty("grabbed_url_info_via_website", true);
            }
        }
        final String urlpart = showname + "/" + episodename;
        return urlpart;
    }

    public static void storeUrlPartInfo(final DownloadLink dl, final String showname, final String episodename, final String thisStationName, final String formatID, final String episodeID) {
        dl.setProperty("url_showname", showname);
        dl.setProperty("url_episodetitle", episodename);
        if (thisStationName != null) {
            /* 2018-12-18: Not required to store at the moment but this might be relevant in the future */
            dl.setProperty("tv_station_name", thisStationName);
        }
        if (formatID != null && episodeID != null) {
            /* Even movies have a formatID and episodeID - both of these IDs are ALWAYS given! */
            dl.setProperty("id_format", formatID);
            dl.setProperty("id_episode", episodeID);
            /* Important: Make sure that crawler- and hosterplugin always set correct linkids! */
            dl.setLinkID("tvnow.de" + "://" + formatID + "/" + episodeID);
        }
    }

    /**
     * Removes parts of the show-title which are not allowed for API requests e.g. the showID. <br />
     * Keep ind mind that this may fail for Strings which end with numbers that are NOT a showID e.g. BAD case: "koeln-1337". Good case:
     * "koeln-1337-506928"
     */
    public static String cleanupShowTitle(String showname) {
        if (showname == null) {
            return null;
        }
        showname = showname.replaceAll("\\-\\d+$", "");
        return showname;
    }

    /** Removes parts of the episode-title which are not allowed for API requests e.g. the contentID. */
    public static String cleanupEpisodeTitle(String episodename) {
        if (episodename == null) {
            return null;
        }
        episodename = episodename.replaceAll("^episode\\-\\d+\\-", "");
        /*
         * This part is tricky - we have to filter-out the contentID but the endings of some titles look like contentIDs but are NOT - we
         * have to detect this and only remove these numbers if we're sure that that is the contentID!
         */
        /* Examples: which shall NOT be modified: "super-8-kamera-von-1965", "folge-w-05" */
        if (!episodename.matches(".*?(folge|teil)\\-\\d+$") && !episodename.matches(".+\\d{4}\\-\\d{2}-\\d{2}-\\d{2}-\\d{2}-\\d{2}$") && !episodename.matches(".+\\-[12]\\d{3}")) {
            episodename = episodename.replaceAll("\\-\\d+$", "");
        }
        return episodename;
    }

    @Override
    public String getAGBLink() {
        return "http://rtl-now.rtl.de/nutzungsbedingungen";
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return getMaxSimultaneousDownloads();
    }

    public int getMaxSimultaneousDownloads() {
        final TvnowConfigInterface cfg = PluginJsonConfig.get(org.jdownloader.plugins.components.config.TvnowConfigInterface.class);
        if (cfg.isEnableUnlimitedSimultaneousDownloads()) {
            return -1;
        } else {
            return 1;
        }
    }

    @Override
    public void handleFree(final DownloadLink downloadLink) throws Exception {
        requestFileInformation(downloadLink);
        /* TODO: Fix this! */
        // final String ageCheck = br.getRegex("(Aus Jugendschutzgründen nur zwischen \\d+ und \\d+ Uhr abrufbar\\!)").getMatch(0);
        // if (ageCheck != null) {
        // throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, ageCheck, 10 * 60 * 60 * 1000l);
        // }
        handleDownload(downloadLink, null);
    }

    private void login(final Account account, final boolean force) throws Exception {
        synchronized (account) {
            try {
                br.setFollowRedirects(true);
                br.setCookiesExclusive(true);
                final Cookies cookies = account.loadCookies("");
                String authtoken = account.getStringProperty("authtoken", null);
                String userID = account.getStringProperty("userid", null);
                /* Always try to re-use sessions! */
                if (cookies != null && authtoken != null && userID != null) {
                    this.br.setCookies(this.getHost(), cookies);
                    setLoginHeaders(this.br, authtoken);
                    /* Only request the fields we need to verify whether stored headers&cookies are valid or not. */
                    br.getPage(API_BASE + "/users/" + userID + "/transactions?fields=id,status");
                    final String useridTmp = PluginJSonUtils.getJson(br, "id");
                    if (useridTmp != null && useridTmp.matches("\\d+") && br.getHttpConnection().getResponseCode() != 401) {
                        return;
                    }
                    /* Full login required - cleanup old cookies / headers */
                    br = new Browser();
                }
                /* 2019-01-16: This is skippable */
                // br.getPage("https://my." + this.getHost() + "/login");
                prepBRAPI(br);
                br.getHeaders().put("Origin", "https://my.tvnow.de");
                /* 2019-03-04: Workaround for backslashes inside passwords */
                final String postdata = "{\"email\":\"" + account.getUser() + "\",\"password\":\"" + account.getPass().replace("\\", "\\\\") + "\"}";
                final PostRequest loginReq = br.createJSonPostRequest(API_BASE + "/backend/login?fields=[%22*%22,%22user%22,[%22receiveInsiderEmails%22,%22receiveMarketingEmails%22,%22marketingsettingsDone%22,%22receiveGroupMarketingEmails%22,%22receiveRTLIIMarketingEmails%22]]", postdata);
                br.openRequestConnection(loginReq);
                br.loadConnection(null);
                /*
                 * This token is a set of base64 strings separated by dots which contains more json which contains some information about
                 * the account and some more tokens (again base64)
                 */
                authtoken = PluginJSonUtils.getJson(br, "token");
                userID = PluginJSonUtils.getJson(br, "id");
                final String clientID = PluginJSonUtils.getJson(br, "clientId");
                final String ck = PluginJSonUtils.getJson(br, "ck");
                if (authtoken == null || userID == null || clientID == null || ck == null) {
                    /* E.g. wrong logindata: {"error":{"code":401,"message":"backend.user.authentication.failed"}} */
                    throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_DISABLE);
                }
                /* Important header! */
                setLoginHeaders(this.br, authtoken);
                account.setProperty("userid", userID);
                account.setProperty("authtoken", authtoken);
                account.saveCookies(this.br.getCookies(this.getHost()), "");
            } catch (final PluginException e) {
                if (e.getLinkStatus() == LinkStatus.ERROR_PREMIUM) {
                    account.clearCookies("");
                }
                throw e;
            }
        }
    }

    private void setLoginHeaders(final Browser br, final String authtoken) {
        br.getHeaders().put("x-auth-token", authtoken);
    }

    @Override
    public AccountInfo fetchAccountInfo(final Account account) throws Exception {
        final AccountInfo ai = new AccountInfo();
        login(account, true);
        final String userID = account.getStringProperty("userid", null);
        br.getPage(API_BASE + "/users/" + userID + "/transactions?fields=*,paymentPaytype.*,paymentPaytype.format.*,paymentTransaction.*,paymentTransaction.paymentProvider&filter=%7B%22ContainerId%22:0%7D");
        /** We can get A LOT of information here ... but we really only want to know if we have a free- or a premium account. */
        LinkedHashMap<String, Object> entries = (LinkedHashMap<String, Object>) JavaScriptEngineFactory.jsonToJavaMap(br.toString());
        entries = (LinkedHashMap<String, Object>) JavaScriptEngineFactory.walkJson(entries, "items/{0}");
        long expiredateTimestamp = 0;
        if (entries != null) {
            final String expiredateStr = (String) entries.get("endDate");
            final String createdateStr = (String) entries.get("created");
            expiredateTimestamp = !StringUtils.isEmpty(expiredateStr) ? TimeFormatter.getMilliSeconds(expiredateStr, "yyyy-MM-dd HH:mm:ss", Locale.GERMANY) : 0;
            if (!StringUtils.isEmpty(createdateStr)) {
                ai.setCreateTime(TimeFormatter.getMilliSeconds(createdateStr, "yyyy-MM-dd HH:mm:ss", Locale.GERMANY));
            }
            final String nextBillingDate = (String) entries.get("nextBillingDate");
            if (StringUtils.isNotEmpty(nextBillingDate)) {
                final long nextBilling = TimeFormatter.getMilliSeconds(nextBillingDate, "yyyy-MM-dd HH:mm:ss", Locale.GERMANY);
                expiredateTimestamp = Math.max(expiredateTimestamp, nextBilling);
            }
        }
        if (expiredateTimestamp < System.currentTimeMillis()) {
            account.setType(AccountType.FREE);
            /*
             * 2019-02-05: Free accounts do not have any advantages over anonymous streaming - also, login is not used for downloading
             * anyways (only for premium accounts)!
             */
            ai.setTrafficLeft(0);
            /* free accounts can still have captcha */
            account.setConcurrentUsePossible(false);
            ai.setStatus("Registered (free) user");
        } else {
            account.setType(AccountType.PREMIUM);
            ai.setValidUntil(expiredateTimestamp);
            ai.setUnlimitedTraffic();
            account.setConcurrentUsePossible(true);
            final String cancelleddateStr = (String) entries.get("cancelled");
            final long cancelleddateTimestamp = !StringUtils.isEmpty(cancelleddateStr) ? TimeFormatter.getMilliSeconds(cancelleddateStr, "yyyy-MM-dd HH:mm:ss", Locale.GERMANY) : 0;
            if (cancelleddateTimestamp > 0) {
                ai.setStatus("Premium account (subscription cancelled)");
            } else {
                ai.setStatus("Premium account (subscription active)");
            }
        }
        return ai;
    }

    @Override
    public void handlePremium(final DownloadLink link, final Account account) throws Exception {
        requestFileInformation(link);
        login(account, false);
        /* 2019-01-16: At the moment, account implementation is not used at all for downloading as it is simply not required. */
        handleDownload(link, account);
    }

    @Override
    public boolean hasCaptcha(DownloadLink link, jd.plugins.Account acc) {
        /* This provider has no captchas at all */
        return false;
    }

    @Override
    public int getMaxSimultanPremiumDownloadNum() {
        return getMaxSimultaneousDownloads();
    }

    @Override
    public void reset() {
    }

    @Override
    public void resetDownloadlink(final DownloadLink link) {
        if (link != null) {
            link.removeProperty(HDSDownloader.RESUME_FRAGMENT);
        }
    }

    @Override
    public void resetPluginGlobals() {
    }

    /** Formats the existing date to the 'general' date used for german TV online services: yyyy-MM-dd */
    public static long getDateMilliseconds(final String input) {
        if (input == null) {
            return -1;
        }
        return TimeFormatter.getMilliSeconds(input, "yyyy-MM-dd HH:mm:ss", Locale.GERMAN);
    }

    @Override
    public Class<? extends PluginConfigInterface> getConfigInterface() {
        return TvnowConfigInterface.class;
    }
}