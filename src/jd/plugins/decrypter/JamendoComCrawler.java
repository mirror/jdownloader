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
package jd.plugins.decrypter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.appwork.storage.JSonStorage;
import org.appwork.storage.TypeRef;
import org.appwork.utils.parser.UrlQuery;
import org.jdownloader.plugins.controller.LazyPlugin;
import org.jdownloader.scripting.JavaScriptEngineFactory;

import jd.PluginWrapper;
import jd.controlling.ProgressController;
import jd.http.Browser;
import jd.parser.Regex;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.plugins.LinkStatus;
import jd.plugins.PluginDependencies;
import jd.plugins.PluginException;
import jd.plugins.PluginForDecrypt;
import jd.plugins.hoster.JamendoCom;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = {}, urls = {})
@PluginDependencies(dependencies = { JamendoCom.class })
public class JamendoComCrawler extends PluginForDecrypt {
    public JamendoComCrawler(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public LazyPlugin.FEATURE[] getFeatures() {
        return new LazyPlugin.FEATURE[] { LazyPlugin.FEATURE.AUDIO_STREAMING };
    }

    public static List<String[]> getPluginDomains() {
        return JamendoCom.getPluginDomains();
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
            ret.add("https?://(?:www\\.)?" + buildHostsPatternPart(domains) + "/(?:[a-z]{2}/)?(album/\\d+(?:/[a-z0-9\\-]+)?|artist/\\d+(?:/[a-z0-9\\-]+)?|list/a\\d+|playlist/\\d+(?:/[a-z0-9\\-]+)?|user/\\d+(?:/[a-z0-9\\-]+(?:/favorites)?)?)");
        }
        return ret.toArray(new String[0]);
    }

    private static LinkedHashMap<String, Map<String, Object>> USER_INFO_CACHE     = new LinkedHashMap<String, Map<String, Object>>() {
                                                                                      protected boolean removeEldestEntry(Map.Entry<String, Map<String, Object>> eldest) {
                                                                                          return size() > 100;
                                                                                      };
                                                                                  };
    private static final String                               TYPE_ALBUM          = "https?://[^/]+/(?:[a-z]{2}/)?(?:album/|list/a)(\\d+).*?";
    private static final String                               TYPE_ARTIST         = "https?://[^/]+/(?:[a-z]{2}/)?artist/(\\d+).*?";
    private static final String                               TYPE_PLAYLIST       = "https?://[^/]+/(?:[a-z]{2}/)?playlist/(\\d+).*?";
    private static final String                               TYPE_USER           = "https?://[^/]+/(?:[a-z]{2}/)?user/(\\d+).*?";
    private static final String                               TYPE_USER_FAVORITES = "https?://[^/]+/(?:[a-z]{2}/)?user/(\\d+)/[a-z0-9\\-]+/favorites";

    @SuppressWarnings("deprecation")
    @Override
    public ArrayList<DownloadLink> decryptIt(final CryptedLink param, ProgressController progress) throws Exception {
        JamendoCom.prepBR(br);
        if (param.getCryptedUrl().matches(TYPE_ALBUM)) {
            return crawlAlbum(param);
        } else if (param.getCryptedUrl().matches(TYPE_PLAYLIST)) {
            return this.crawlUserPlaylist(param);
        } else if (param.getCryptedUrl().matches(TYPE_USER_FAVORITES)) {
            return this.crawlUserFavorites(param);
        } else if (param.getCryptedUrl().matches(TYPE_USER)) {
            return this.crawlUser(param);
        } else if (param.getCryptedUrl().matches(TYPE_ARTIST)) {
            return this.crawlArtist(param);
        } else {
            /* Developer mistake */
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
    }

    public ArrayList<DownloadLink> crawlAlbum(final CryptedLink param) throws Exception {
        final String albumID = new Regex(param.getCryptedUrl(), TYPE_ALBUM).getMatch(0);
        if (albumID == null) {
            /* Developer mistake */
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        queryAPI(br, "/albums?id%5B%5D=" + albumID);
        final ArrayList<DownloadLink> ret = new ArrayList<DownloadLink>();
        final List<Object> ressourcelist = JSonStorage.restoreFromString(br.toString(), TypeRef.LIST);
        if (ressourcelist.isEmpty()) {
            /* Invalid albumID. */
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        final Map<String, Object> album = (Map<String, Object>) ressourcelist.get(0);
        if (!JamendoCom.isAvailable(album)) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        final Map<String, Object> artist = getArtistInfo(album.get("artistId").toString());
        final List<Map<String, Object>> tracks = (List<Map<String, Object>>) album.get("tracks");
        final FilePackage fp = FilePackage.getInstance();
        fp.setName(artist.get("name") + " - " + album.get("name").toString());
        fp.setComment((String) JavaScriptEngineFactory.walkJson(album, "description/en"));
        for (final Map<String, Object> track : tracks) {
            final DownloadLink link = createSongDownloadlink(track.get("id").toString());
            link.setProperty(JamendoCom.PROPERTY_ARTIST, artist.get("name").toString());
            link.setProperty(JamendoCom.PROPERTY_POSITION_ALBUM, track.get("position"));
            link.setContainerUrl(param.getCryptedUrl());
            link._setFilePackage(fp);
            ret.add(link);
        }
        return ret;
    }

    public ArrayList<DownloadLink> crawlUserPlaylist(final CryptedLink param) throws Exception {
        final String playlistID = new Regex(param.getCryptedUrl(), TYPE_PLAYLIST).getMatch(0);
        if (playlistID == null) {
            /* Developer mistake */
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        queryAPI(br, "/playlists?id%5B%5D=" + playlistID);
        final List<Object> ressourcelist = JSonStorage.restoreFromString(br.toString(), TypeRef.LIST);
        if (ressourcelist.isEmpty()) {
            /* Invalid playlistID. */
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        final Map<String, Object> playlist = (Map<String, Object>) ressourcelist.get(0);
        final Map<String, Object> user = getUserInfo(playlist.get("userId").toString());
        return crawlProcessUserPlaylist(user, playlist);
    }

    public ArrayList<DownloadLink> crawlProcessUserPlaylist(final Map<String, Object> user, final Map<String, Object> playlist) throws Exception {
        final ArrayList<DownloadLink> ret = new ArrayList<DownloadLink>();
        final List<Map<String, Object>> tracks = (List<Map<String, Object>>) playlist.get("tracks");
        if (tracks.isEmpty()) {
            logger.info("Empty playlist");
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        final String playlistname = playlist.get("name").toString();
        final String playlistSlug = playlistname.toLowerCase(Locale.ENGLISH).replaceAll("[^a-z0-9]", "-").replaceAll("-{2,}", "-");
        final String playlistURL = "https://www." + this.getHost() + "/playlist/" + playlist.get("id") + "/" + playlistSlug;
        final FilePackage fp = FilePackage.getInstance();
        fp.setName(user.get("dispname") + " - " + playlistname);
        fp.setComment((String) playlist.get("description"));
        boolean positionStartsAtZero = false;
        for (final Map<String, Object> track : tracks) {
            final DownloadLink link = createSongDownloadlink(track.get("id").toString());
            link.setProperty(JamendoCom.PROPERTY_USER, user.get("dispname").toString());
            final int position = ((Number) track.get("position")).intValue();
            if (position == 0) {
                positionStartsAtZero = true;
            }
            if (positionStartsAtZero) {
                /* Fix position value */
                link.setProperty(JamendoCom.PROPERTY_POSITION_PLAYLIST, position + 1);
            } else {
                link.setProperty(JamendoCom.PROPERTY_POSITION_PLAYLIST, position);
            }
            link.setContainerUrl(playlistURL);
            link._setFilePackage(fp);
            ret.add(link);
        }
        return ret;
    }

    /** Crawls all favorite tracks of a user */
    public ArrayList<DownloadLink> crawlUserFavorites(final CryptedLink param) throws Exception {
        final String userID = new Regex(param.getCryptedUrl(), TYPE_USER_FAVORITES).getMatch(0);
        if (userID == null) {
            /* Developer mistake */
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        final Map<String, Object> user = this.getUserInfo(userID);
        final int maxItemsPerPage = 20;
        final ArrayList<DownloadLink> ret = new ArrayList<DownloadLink>();
        int page = 1;
        int offset = maxItemsPerPage;
        final FilePackage fp = FilePackage.getInstance();
        fp.setName(user.get("dispname").toString());
        do {
            final UrlQuery query = new UrlQuery();
            query.add("type", "track");
            query.add("userId", userID);
            query.add("limit", Integer.toString(maxItemsPerPage));
            query.add("order", "dateFavorite_desc");
            if (page > 1) {
                query.add("offset", Integer.toString(offset));
            }
            queryAPI(br, "/favorites/track?" + query.toString());
            final List<Map<String, Object>> tracks = (List<Map<String, Object>>) JSonStorage.restoreFromString(br.toString(), TypeRef.OBJECT);
            for (final Map<String, Object> track : tracks) {
                final DownloadLink link = createSongDownloadlink(track.get("id").toString());
                link._setFilePackage(fp);
                ret.add(link);
                distribute(link);
            }
            logger.info("Crawled page " + page + " | Found results so far: " + ret.size());
            if (tracks.size() < maxItemsPerPage) {
                logger.info("Stopping because: Current page contains less items than " + maxItemsPerPage);
                break;
            } else if (this.isAbort()) {
                logger.info("Stopping because: Aborted by user");
                break;
            } else {
                page++;
                offset += maxItemsPerPage;
                continue;
            }
        } while (true);
        return ret;
    }

    /** Crawls user profile --> All playlists of user */
    public ArrayList<DownloadLink> crawlUser(final CryptedLink param) throws Exception {
        final String userID = new Regex(param.getCryptedUrl(), TYPE_USER).getMatch(0);
        if (userID == null) {
            /* Developer mistake */
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        final Map<String, Object> user = this.getUserInfo(userID);
        final int maxItemsPerPage = 16;
        final ArrayList<DownloadLink> ret = new ArrayList<DownloadLink>();
        int page = 1;
        int offset = maxItemsPerPage;
        do {
            final UrlQuery query = new UrlQuery();
            query.add("userId", userID);
            query.add("order", "dateCreated_desc");
            query.add("limit", Integer.toString(maxItemsPerPage));
            if (page > 1) {
                query.add("offset", Integer.toString(offset));
            }
            queryAPI(br, "/playlists?" + query.toString());
            final List<Map<String, Object>> playlists = (List<Map<String, Object>>) JSonStorage.restoreFromString(br.toString(), TypeRef.OBJECT);
            for (final Map<String, Object> playlist : playlists) {
                final ArrayList<DownloadLink> playlistResults = crawlProcessUserPlaylist(user, playlist);
                for (final DownloadLink link : playlistResults) {
                    ret.add(link);
                    distribute(link);
                }
            }
            logger.info("Crawled page " + page + " | Found results so far: " + ret.size());
            if (playlists.size() < maxItemsPerPage) {
                logger.info("Stopping because: Current page contains less items than " + maxItemsPerPage);
                break;
            } else if (this.isAbort()) {
                logger.info("Stopping because: Aborted by user");
                break;
            } else {
                page++;
                offset += maxItemsPerPage;
                continue;
            }
        } while (true);
        return ret;
    }

    public ArrayList<DownloadLink> crawlArtist(final CryptedLink param) throws Exception {
        final String artistID = new Regex(param.getCryptedUrl(), TYPE_ARTIST).getMatch(0);
        if (artistID == null) {
            /* Developer mistake */
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        final ArrayList<DownloadLink> ret = new ArrayList<DownloadLink>();
        final Map<String, Object> artist = getArtistInfo(artistID);
        /* Crawl all albums */
        final List<Map<String, Object>> albums = (List<Map<String, Object>>) artist.get("albums");
        for (final Map<String, Object> album : albums) {
            ret.add(createAlbumDownloadlink(album.get("id").toString()));
        }
        final FilePackage fpSingles = FilePackage.getInstance();
        fpSingles.setName(artist.get("name") + " - singles");
        /* Crawl all singles */
        final List<Map<String, Object>> singles = (List<Map<String, Object>>) artist.get("singles");
        for (final Map<String, Object> track : singles) {
            final DownloadLink link = createSongDownloadlink(track.get("id").toString());
            link.setProperty(JamendoCom.PROPERTY_ARTIST, artist.get("name").toString());
            link._setFilePackage(fpSingles);
            ret.add(link);
        }
        return ret;
    }

    private Map<String, Object> getArtistInfo(final String artistID) throws IOException, PluginException {
        final JamendoCom hostPlugin = (JamendoCom) this.getNewPluginForHostInstance(this.getHost());
        return hostPlugin.getArtistInfo(artistID);
    }

    public Map<String, Object> getUserInfo(final String userID) throws IOException, PluginException {
        if (userID == null) {
            /* Developer mistake */
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        if (USER_INFO_CACHE.containsKey(userID)) {
            return USER_INFO_CACHE.get(userID);
        } else {
            queryAPI(br, "/users?id%5B%5D=" + userID);
            final List<Object> users = JSonStorage.restoreFromString(br.toString(), TypeRef.LIST);
            if (users.isEmpty()) {
                /* User not found */
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            }
            final Map<String, Object> user = (Map<String, Object>) users.get(0);
            USER_INFO_CACHE.put(userID, user);
            return user;
        }
    }

    private void queryAPI(final Browser br, final String relativePath) throws IOException {
        JamendoCom.queryAPI(br, relativePath);
    }

    private DownloadLink createAlbumDownloadlink(final String albumID) {
        return this.createDownloadlink("https://www." + this.getHost() + "/album/" + albumID);
    }

    private DownloadLink createSongDownloadlink(final String songID) {
        return this.createDownloadlink("http://jamen.do/t/" + songID);
    }

    public boolean hasCaptcha(final CryptedLink link, final jd.plugins.Account acc) {
        return false;
    }
}