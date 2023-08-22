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
import java.net.URL;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.appwork.storage.JSonStorage;
import org.appwork.storage.TypeRef;
import org.appwork.utils.StringUtils;
import org.appwork.utils.parser.UrlQuery;
import org.jdownloader.plugins.controller.LazyPlugin;

import jd.PluginWrapper;
import jd.http.Browser;
import jd.nutils.JDHash;
import jd.parser.Regex;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = {}, urls = {})
public class JamendoCom extends PluginForHost {
    public static final String                                API_BASE          = "https://www.jamendo.com/api";
    private static LinkedHashMap<String, Map<String, Object>> ARTIST_INFO_CACHE = new LinkedHashMap<String, Map<String, Object>>() {
                                                                                    protected boolean removeEldestEntry(Map.Entry<String, Map<String, Object>> eldest) {
                                                                                        return size() > 100;
                                                                                    };
                                                                                };

    public JamendoCom(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public LazyPlugin.FEATURE[] getFeatures() {
        return new LazyPlugin.FEATURE[] { LazyPlugin.FEATURE.AUDIO_STREAMING };
    }

    public static List<String[]> getPluginDomains() {
        final List<String[]> ret = new ArrayList<String[]>();
        ret.add(new String[] { "jamendo.com", "jamen.do" });
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
            ret.add("https?://(?:www\\.)?" + buildHostsPatternPart(domains) + "/(?:[a-z]{2}/)?(?:track|t)/\\d+");
        }
        return ret.toArray(new String[0]);
    }

    @Override
    public String getAGBLink() {
        return "https://help-music.jamendo.com/hc/en-us";
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return -1;
    }

    @Override
    public String getLinkID(final DownloadLink link) {
        final String fid = getFID(link);
        if (fid != null) {
            return this.getHost() + "://" + fid;
        } else {
            return super.getLinkID(link);
        }
    }

    private String getFID(final DownloadLink link) {
        return getFID(link.getPluginPatternMatcher());
    }

    public static String getFID(final String url) {
        return new Regex(url, "(\\d+)$").getMatch(0);
    }

    public static String  PROPERTY_ARTIST            = "artist";           // e.g. artist who created and uploaded a song/album
    public static String  PROPERTY_USER              = "user";             // e.g. user who created a playlist
    public static String  PROPERTY_POSITION_ALBUM    = "position_album";
    public static String  PROPERTY_POSITION_PLAYLIST = "position_playlist";
    private final String  PROPERTY_DIRECTURL_MP3     = "directurl_mp3";
    // private final String PROPERTY_DIRECTURL_OGG = "directurl_ogg";
    private final boolean crawlArtistInfo            = false;

    @Override
    public boolean checkLinks(final DownloadLink[] urls) {
        if (urls == null || urls.length == 0) {
            return false;
        }
        try {
            prepBR(this.br);
            br.setCookiesExclusive(true);
            final ArrayList<DownloadLink> links = new ArrayList<DownloadLink>();
            int index = 0;
            while (true) {
                links.clear();
                while (true) {
                    /* 2022-07-11: Check max 50 items in one go. */
                    if (index == urls.length || links.size() == 50) {
                        break;
                    } else {
                        links.add(urls[index]);
                        index++;
                    }
                }
                final UrlQuery query = new UrlQuery();
                for (final DownloadLink link : links) {
                    query.add("id%5B%5D", this.getFID(link));
                }
                queryAPI(br, "/tracks?" + query.toString());
                final List<Map<String, Object>> tracks = (List<Map<String, Object>>) restoreFromString(br.toString(), TypeRef.OBJECT);
                for (final DownloadLink link : links) {
                    Map<String, Object> trackInfo = null;
                    final String trackID = this.getFID(link);
                    for (final Map<String, Object> track : tracks) {
                        if (track.get("id").toString().equals(trackID)) {
                            trackInfo = track;
                            break;
                        }
                    }
                    if (trackInfo == null) {
                        /* E.g. invalid trackID. */
                        link.setAvailable(false);
                        continue;
                    }
                    if (!link.hasProperty(PROPERTY_ARTIST) && crawlArtistInfo) {
                        final Map<String, Object> artist = this.getArtistInfo(trackInfo.get("artistId").toString());
                        link.setProperty(PROPERTY_ARTIST, artist.get("name"));
                    }
                    final String title = (String) trackInfo.get("name");
                    String position = null;
                    if (link.hasProperty(PROPERTY_POSITION_ALBUM)) {
                        position = link.getProperty(PROPERTY_POSITION_ALBUM).toString();
                    } else if (link.hasProperty(PROPERTY_POSITION_PLAYLIST)) {
                        position = link.getProperty(PROPERTY_POSITION_PLAYLIST).toString();
                    }
                    if (position != null) {
                        link.setFinalFileName(position + ". " + title + ".mp3");
                    } else {
                        link.setFinalFileName(title + ".mp3");
                    }
                    if (link.getComment() == null) {
                        final String description = (String) trackInfo.get("description");
                        final String credits = (String) trackInfo.get("credits");
                        if (!StringUtils.isEmpty(description)) {
                            link.setComment(description);
                        } else if (!StringUtils.isEmpty(credits)) {
                            link.setComment(credits);
                        }
                    }
                    link.setAvailable(isAvailable(trackInfo));
                    final Map<String, Object> download = (Map<String, Object>) trackInfo.get("download");
                    link.setProperty(PROPERTY_DIRECTURL_MP3, download.get("mp3"));
                    // link.setProperty(PROPERTY_DIRECTURL_OGG, download.get("ogg"));
                    if (((Number) trackInfo.get("isDownloadable")).intValue() != 1) {
                        logger.info("Dev: Found un-downloadable track");
                    }
                }
                if (index == urls.length) {
                    break;
                }
            }
        } catch (final Exception e) {
            logger.log(e);
            return false;
        }
        return true;
    }

    public static final boolean isAvailable(final Map<String, Object> jamendoMap) {
        final Map<String, Object> status = (Map<String, Object>) jamendoMap.get("status");
        if (status.get("nonAvailabilityReason") != null) {
            return false;
        } else {
            return true;
        }
    }

    public static void queryAPI(final Browser br, final String relativePath) throws IOException {
        final String url = API_BASE + relativePath;
        final String path = new URL(url).getPath();
        final long number = System.currentTimeMillis();
        br.getHeaders().put("X-Jam-Call", "$" + JDHash.getSHA1(path + number) + "*" + number + "~");
        br.getPage(url);
    }

    @Override
    public AvailableStatus requestFileInformation(final DownloadLink link) throws Exception {
        checkLinks(new DownloadLink[] { link });
        if (!link.isAvailabilityStatusChecked()) {
            return AvailableStatus.UNCHECKED;
        }
        if (link.isAvailabilityStatusChecked() && !link.isAvailable()) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        return AvailableStatus.TRUE;
    }

    @Override
    public void handleFree(final DownloadLink link) throws Exception {
        requestFileInformation(link);
        final String dlurl = link.getStringProperty(PROPERTY_DIRECTURL_MP3);
        if (StringUtils.isEmpty(dlurl)) {
            /* This should never happen */
            throw new PluginException(LinkStatus.ERROR_FATAL, "This track is not downloadable");
        }
        dl = jd.plugins.BrowserAdapter.openDownload(br, link, dlurl, true, 1);
        if (!this.looksLikeDownloadableContent(dl.getConnection())) {
            br.followConnection();
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        dl.startDownload();
    }

    public Map<String, Object> getArtistInfo(final String artistID) throws IOException, PluginException {
        if (artistID == null) {
            /* Developer mistake */
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        if (ARTIST_INFO_CACHE.containsKey(artistID)) {
            return ARTIST_INFO_CACHE.get(artistID);
        } else {
            queryAPI(br, "/artists?id%5B%5D=" + artistID);
            final List<Object> artists = restoreFromString(br.toString(), TypeRef.LIST);
            if (artists.isEmpty()) {
                /* Artist not found */
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            }
            final Map<String, Object> artist = (Map<String, Object>) artists.get(0);
            ARTIST_INFO_CACHE.put(artistID, artist);
            return artist;
        }
    }

    public static Browser prepBR(final Browser br) {
        final String host = getPluginDomains().get(0)[0];
        br.setCookie(host, "jamAcceptCookie", "en");
        br.setCookie(host, "jammusiclang", "en");
        br.setCookie(host, "jamapplication", "true");
        br.getHeaders().put("x-jam-version", "2udos4");
        br.getHeaders().put("X-Requested-With", "XMLHttpRequest");
        br.setFollowRedirects(true);
        return br;
    }

    @Override
    public void reset() {
    }

    @Override
    public void resetDownloadlink(DownloadLink link) {
    }
}