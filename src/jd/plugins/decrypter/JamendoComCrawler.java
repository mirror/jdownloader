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
import java.util.Map;

import org.appwork.storage.JSonStorage;
import org.appwork.storage.TypeRef;
import org.appwork.utils.DebugMode;
import org.jdownloader.plugins.controller.LazyPlugin;

import jd.PluginWrapper;
import jd.controlling.ProgressController;
import jd.parser.Regex;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
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
            ret.add("https?://(?:www\\.)?" + buildHostsPatternPart(domains) + "/(?:[a-z]{2}/)?(album/\\d+|artist/.+|list/a\\d+)");
        }
        return ret.toArray(new String[0]);
    }

    private static final String                               TYPE_ALBUM      = "https?://[^/]+/(?:album/|list/a)(\\d+)";
    private static final String                               TYPE_ARTIST     = "https?://[^/]+/artist/(\\d+)";
    private static LinkedHashMap<String, Map<String, Object>> USER_INFO_CACHE = new LinkedHashMap<String, Map<String, Object>>() {
                                                                                  protected boolean removeEldestEntry(Map.Entry<String, Map<String, Object>> eldest) {
                                                                                      return size() > 200;
                                                                                  };
                                                                              };

    @SuppressWarnings("deprecation")
    @Override
    public ArrayList<DownloadLink> decryptIt(final CryptedLink param, ProgressController progress) throws Exception {
        JamendoCom.prepBR(br);
        if (param.getCryptedUrl().matches(TYPE_ALBUM)) {
            return crawlAlbum(param);
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
        JamendoCom.queryAPI(br, "/albums?id%5B%5D=" + albumID);
        final ArrayList<DownloadLink> ret = new ArrayList<DownloadLink>();
        final List<Object> ressourcelist = JSonStorage.restoreFromString(br.toString(), TypeRef.LIST);
        final Map<String, Object> album = (Map<String, Object>) ressourcelist.get(0);
        final List<Map<String, Object>> tracks = (List<Map<String, Object>>) album.get("tracks");
        for (final Map<String, Object> track : tracks) {
            final DownloadLink link = createSongDownloadlink(track.get("id").toString());
            link.setProperty(JamendoCom.PROPERTY_POSITION, track.get("position"));
            ret.add(link);
        }
        return ret;
    }

    public ArrayList<DownloadLink> crawlArtist(final CryptedLink param) throws Exception {
        if (!DebugMode.TRUE_IN_IDE_ELSE_FALSE) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        final String artistID = new Regex(param.getCryptedUrl(), TYPE_ARTIST).getMatch(0);
        if (artistID == null) {
            /* Developer mistake */
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        JamendoCom.queryAPI(br, "/artists?id%5B%5D=" + artistID);
        final ArrayList<DownloadLink> ret = new ArrayList<DownloadLink>();
        final Map<String, Object> artist = getArtistInfo(artistID);
        final List<Map<String, Object>> tracks = (List<Map<String, Object>>) artist.get("tracks");
        for (final Map<String, Object> track : tracks) {
            final DownloadLink link = createSongDownloadlink(track.get("id").toString());
            link.setProperty(JamendoCom.PROPERTY_POSITION, track.get("position"));
            ret.add(link);
        }
        return ret;
    }

    private Map<String, Object> getArtistInfo(final String artistID) throws IOException, PluginException {
        if (artistID == null) {
            /* Developer mistake */
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        if (USER_INFO_CACHE.containsKey(artistID)) {
            return USER_INFO_CACHE.get(artistID);
        } else {
            JamendoCom.queryAPI(br, "/artists?id%5B%5D=" + artistID);
            final List<Object> ressourcelist = JSonStorage.restoreFromString(br.toString(), TypeRef.LIST);
            final Map<String, Object> artist = (Map<String, Object>) ressourcelist.get(0);
            USER_INFO_CACHE.put(artistID, artist);
            return artist;
        }
    }

    private DownloadLink createSongDownloadlink(final String songID) {
        return this.createDownloadlink("http://jamen.do/t/" + songID);
    }

    public boolean hasCaptcha(final CryptedLink link, final jd.plugins.Account acc) {
        return false;
    }
}