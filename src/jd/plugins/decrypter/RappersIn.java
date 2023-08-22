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

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.appwork.storage.JSonStorage;
import org.appwork.storage.TypeRef;
import org.appwork.utils.StringUtils;
import org.jdownloader.scripting.JavaScriptEngineFactory;

import jd.PluginWrapper;
import jd.controlling.ProgressController;
import jd.http.Browser;
import jd.nutils.encoding.Encoding;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForDecrypt;
import jd.plugins.hoster.DirectHTTP;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = {}, urls = {})
public class RappersIn extends PluginForDecrypt {
    public RappersIn(PluginWrapper wrapper) {
        super(wrapper);
    }

    public static List<String[]> getPluginDomains() {
        final List<String[]> ret = new ArrayList<String[]>();
        // each entry in List<String[]> will result in one PluginForDecrypt, Plugin.getHost() will return String[0]->main domain
        ret.add(new String[] { "rappers.in" });
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
            ret.add("https?://(?:www\\.)?" + buildHostsPatternPart(domains) + "/(?:track|album)/[A-Za-z0-9]+");
        }
        return ret.toArray(new String[0]);
    }

    private final String TYPE_TRACK = "https?://[^/]+/track/([A-Za-z0-9]+)";
    private final String TYPE_ALBUM = "https?://[^/]+/album/([A-Za-z0-9]+)";

    public ArrayList<DownloadLink> decryptIt(final CryptedLink param, ProgressController progress) throws Exception {
        br.setFollowRedirects(true);
        if (param.getCryptedUrl().matches(TYPE_TRACK)) {
            return crawlTrack(param);
        } else if (param.getCryptedUrl().matches(TYPE_ALBUM)) {
            return this.crawlAlbum(param);
        } else {
            /* Unsupported URL --> Developer mistake */
            return null;
        }
    }

    public ArrayList<DownloadLink> crawlTrack(final CryptedLink param) throws Exception {
        return crawlTrack(param.getCryptedUrl(), null);
    }

    public ArrayList<DownloadLink> crawlTrack(final String trackURL, final FilePackage fp) throws Exception {
        ArrayList<DownloadLink> ret = new ArrayList<DownloadLink>();
        br.getPage(trackURL);
        if (isOffline(br)) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        final String json = br.getRegex("var songsObject = (\\[\\{\"name\":\".*?\\]) *;\n").getMatch(0);
        final String json2 = br.getRegex("type=\"application/ld\\+json\"[^>]*>(\\{.*?\\})</script>").getMatch(0);
        if (json == null && json2 == null) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        if (json != null) {
            final List<Object> tracksO = restoreFromString(json, TypeRef.LIST);
            for (final Object trackO : tracksO) {
                final Map<String, Object> trackInfo = (Map<String, Object>) trackO;
                final DownloadLink track = this.createDownloadlink(trackInfo.get("url").toString());
                final String finalFilename = Encoding.htmlDecode(trackInfo.get("name").toString()).trim() + ".mp3";
                track.setFinalFileName(finalFilename);
                track.setProperty(DirectHTTP.FIXNAME, finalFilename);
                track.setAvailable(true);
                if (fp != null) {
                    track._setFilePackage(fp);
                }
                ret.add(track);
            }
        }
        if (json == null) {
            /* Fallback */
            final Map<String, Object> trackInfo = JavaScriptEngineFactory.jsonToJavaMap(json2);
            final String description = (String) trackInfo.get("description");
            final DownloadLink track = this.createDownloadlink(trackInfo.get("contentUrl").toString());
            String finalFilename = Encoding.htmlDecode(trackInfo.get("name").toString()).trim();
            /* Sometimes, file-extension is already given */
            finalFilename = applyFilenameExtension(finalFilename, ".mp3");
            track.setFinalFileName(finalFilename);
            track.setProperty(DirectHTTP.FIXNAME, finalFilename);
            if (!StringUtils.isEmpty(description)) {
                track.setComment(description);
            }
            track.setAvailable(true);
            if (fp != null) {
                track._setFilePackage(fp);
            }
            ret.add(track);
        }
        return ret;
    }

    private boolean isOffline(final Browser br) {
        if (br.getHttpConnection().getResponseCode() == 404) {
            return true;
        } else if (br.getURL().endsWith("/404")) {
            return true;
        } else {
            return false;
        }
    }

    public ArrayList<DownloadLink> crawlAlbum(final CryptedLink param) throws Exception {
        br.getPage(param.getCryptedUrl());
        if (isOffline(br)) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        final ArrayList<String> trackIDsWithoutDupes = new ArrayList<String>();
        final String[] trackIDs = br.getRegex("playSong\\('([A-Za-z0-9]+)'\\)").getColumn(0);
        for (final String trackID : trackIDs) {
            if (!trackIDsWithoutDupes.contains(trackID)) {
                trackIDsWithoutDupes.add(trackID);
            }
        }
        FilePackage fp = null;
        final String artist = br.getRegex("class=\"al_artist\"><a[^>]*>([^<]+)<").getMatch(0);
        final String title = br.getRegex("data-load=\"album/[A-Za-z0-9]+\"[^>]*>([^<]+)</a>").getMatch(0);
        if (artist != null && title != null) {
            fp = FilePackage.getInstance();
            fp.setName(Encoding.htmlDecode(artist).trim() + " - " + Encoding.htmlDecode(title).trim());
        }
        int progress = 1;
        final ArrayList<DownloadLink> ret = new ArrayList<DownloadLink>();
        final int padLength = StringUtils.getPadLength(trackIDsWithoutDupes.size());
        for (final String trackID : trackIDsWithoutDupes) {
            logger.info("Crawling track " + progress + "/" + trackIDsWithoutDupes.size());
            final ArrayList<DownloadLink> tracks = this.crawlTrack(getContentURLSingleTrack(trackID), fp);
            for (final DownloadLink track : tracks) {
                if (tracks.size() == 1) {
                    /* Add position to track-filename */
                    final String newFilename = String.format(Locale.US, "%0" + padLength + "d.", progress) + track.getFinalFileName();
                    track.setFinalFileName(newFilename);
                    track.setProperty(DirectHTTP.FIXNAME, newFilename);
                }
                distribute(track);
                ret.add(track);
            }
            if (this.isAbort()) {
                /* Aborted by user */
                break;
            }
            progress++;
        }
        return ret;
    }

    private String getContentURLSingleTrack(final String trackID) {
        return "https://" + this.getHost() + "/track/" + trackID;
    }

    public boolean hasCaptcha(CryptedLink link, jd.plugins.Account acc) {
        return false;
    }
}