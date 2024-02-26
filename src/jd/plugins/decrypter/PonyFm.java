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
package jd.plugins.decrypter;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.appwork.storage.TypeRef;

import jd.PluginWrapper;
import jd.controlling.ProgressController;
import jd.http.Browser;
import jd.parser.Regex;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForDecrypt;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = {}, urls = {})
public class PonyFm extends PluginForDecrypt {
    public PonyFm(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public Browser createNewBrowserInstance() {
        final Browser br = super.createNewBrowserInstance();
        br.setFollowRedirects(true);
        return br;
    }

    public static List<String[]> getPluginDomains() {
        final List<String[]> ret = new ArrayList<String[]>();
        // each entry in List<String[]> will result in one PluginForDecrypt, Plugin.getHost() will return String[0]->main domain
        ret.add(new String[] { "pony.fm" });
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

    private static final String PATTERN_RELATIVE_TRACK    = "/tracks/(\\d+)(\\-[a-z0-9\\-]+)?";
    private static final String PATTERN_RELATIVE_PLAYLIST = "/playlist/(\\d+)(\\-[a-z0-9\\-]+)?";

    public static String[] buildAnnotationUrls(final List<String[]> pluginDomains) {
        final List<String> ret = new ArrayList<String>();
        for (final String[] domains : pluginDomains) {
            ret.add("https?://(?:www\\.)?" + buildHostsPatternPart(domains) + "(" + PATTERN_RELATIVE_TRACK + "|" + PATTERN_RELATIVE_PLAYLIST + ")");
        }
        return ret.toArray(new String[0]);
    }

    public ArrayList<DownloadLink> decryptIt(final CryptedLink param, ProgressController progress) throws Exception {
        br.getHeaders().put("Accept", "application/json, text/plain, */*");
        final Regex singletrack = new Regex(param.getCryptedUrl(), PATTERN_RELATIVE_TRACK);
        final Regex playlist = new Regex(param.getCryptedUrl(), PATTERN_RELATIVE_PLAYLIST);
        if (singletrack.patternFind()) {
            final String trackid = singletrack.getMatch(0);
            br.getPage("https://" + this.getHost() + "/api/web/tracks/" + trackid + "?log=true");
            if (br.getHttpConnection().getResponseCode() == 404 || br.containsHTML("\"Track not found")) {
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            }
            final Map<String, Object> entries = restoreFromString(br.getRequest().getHtmlCode(), TypeRef.MAP);
            final Map<String, Object> track = (Map<String, Object>) entries.get("track");
            return crawlProcessTrackJson(track);
        } else if (playlist.patternFind()) {
            final String playlistid = playlist.getMatch(0);
            br.getPage("https://" + this.getHost() + "/api/web/playlists/" + playlistid);
            if (br.getHttpConnection().getResponseCode() == 404 || br.containsHTML("\"Playlist not found")) {
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            }
            final Map<String, Object> entries = restoreFromString(br.getRequest().getHtmlCode(), TypeRef.MAP);
            final int track_count = ((Number) entries.get("track_count")).intValue();
            if (track_count == 0) {
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            }
            final ArrayList<DownloadLink> ret = new ArrayList<DownloadLink>();
            final List<Map<String, Object>> tracks = (List<Map<String, Object>>) entries.get("tracks");
            for (final Map<String, Object> track : tracks) {
                ret.addAll(crawlProcessTrackJson(track));
            }
            /* Add playlist cover */
            final String playlistTitle = entries.get("title").toString();
            final Map<String, Object> covers = (Map<String, Object>) entries.get("covers");
            final String urlCover = (String) covers.get("original");
            final String extCover = getFileNameExtensionFromString(urlCover);
            final DownloadLink dlcover = createDownloadlink(urlCover);
            dlcover.setFinalFileName(playlistTitle + "_cover" + "." + extCover);
            dlcover.setAvailable(true);
            ret.add(dlcover);
            final FilePackage fp = FilePackage.getInstance();
            fp.setName(playlistTitle);
            fp.setPackageKey("pony_fm://playlist/" + entries.get("id"));
            fp.addLinks(ret);
            return ret;
        } else {
            /* Developer mistake -> This should never happen! */
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
    }

    private ArrayList<DownloadLink> crawlProcessTrackJson(final Map<String, Object> track) {
        final ArrayList<DownloadLink> ret = new ArrayList<DownloadLink>();
        final String song_name = (String) track.get("title");
        final Map<String, Object> streams = (Map<String, Object>) track.get("streams");
        final Map<String, Object> covers = (Map<String, Object>) track.get("covers");
        final String url = (String) streams.get("mp3");
        final String ext = ".mp3";
        final DownloadLink fina = createDownloadlink(url);
        fina.setFinalFileName(song_name + "." + ext);
        fina.setAvailable(true);
        ret.add(fina);
        /* Add cover */
        final String urlCover = (String) covers.get("original");
        final String extCover = getFileNameExtensionFromString(urlCover);
        final DownloadLink dlcover = createDownloadlink(urlCover);
        dlcover.setFinalFileName(song_name + "_cover" + "." + extCover);
        dlcover.setAvailable(true);
        ret.add(dlcover);
        final FilePackage fp = FilePackage.getInstance();
        fp.setName(song_name);
        fp.setPackageKey("pony_fm://track/" + track.get("id"));
        fp.addLinks(ret);
        return ret;
    }
}
