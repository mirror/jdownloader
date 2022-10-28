package jd.plugins.decrypter;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.appwork.storage.JSonStorage;
import org.appwork.storage.TypeRef;

import jd.PluginWrapper;
import jd.controlling.ProgressController;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForDecrypt;
import jd.plugins.hoster.GenericM3u8;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 3, names = { "rozhlas.cz" }, urls = { "https?://(?:[a-z0-9]+\\.)?rozhlas\\.cz/([a-z0-9\\-]+)\\-(\\d+)" })
public class RozhlasCz extends PluginForDecrypt {
    public RozhlasCz(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public ArrayList<DownloadLink> decryptIt(CryptedLink parameter, ProgressController progress) throws Exception {
        final ArrayList<DownloadLink> ret = new ArrayList<DownloadLink>();
        br.setFollowRedirects(true);
        br.getPage(parameter.getCryptedUrl());
        final String audioIDs[] = br.getRegex("https?://prehravac\\.rozhlas\\.cz/audio/(\\d+)").getColumn(0);
        String title = br.getRegex("property=\"og:title\"\\s*content=\"(.*?)\"").getMatch(0);
        if (title == null) {
            /* Fallback */
            title = new Regex(parameter.getCryptedUrl(), this.getSupportedLinks()).getMatch(0);
        }
        title = Encoding.htmlDecode(title).trim();
        if (audioIDs != null) {
            final Set<String> dupes = new HashSet<String>();
            for (final String audioID : audioIDs) {
                final DownloadLink link = createDownloadlink("directhttp://" + br.getURL("//media.rozhlas.cz/_audio/" + audioID + ".mp3").toString());
                if (dupes.add(link.getPluginPatternMatcher())) {
                    if (title != null) {
                        link.setFinalFileName(title + ".mp3");
                    }
                    ret.add(link);
                }
            }
        }
        final String[] htmls = br.getRegex("(<li><div part=.*?</div></li>)").getColumn(0);
        for (final String html : htmls) {
            final String trackNumber = new Regex(html, "part=\"(\\d+)\"").getMatch(0);
            String trackTitle = new Regex(html, "title=\"([^\"]+)\"").getMatch(0);
            /* 2022-01-24: They've switched to MPD/HLS streaming but some tracks are still streamed via old http streaming. */
            final String httpStreamingURL = new Regex(html, " href=\"(https?://[^\"]+\\.mp3[^\"]*)\"").getMatch(0);
            final String mpdStreamingHash = new Regex(html, "https?://cros\\d+://([a-f0-9\\-]+)").getMatch(0);
            if (trackNumber == null || trackTitle == null || (httpStreamingURL == null && mpdStreamingHash == null)) {
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            trackTitle = Encoding.htmlDecode(trackTitle).trim();
            final DownloadLink link;
            final String ext;
            if (httpStreamingURL != null) {
                link = this.createDownloadlink("directhttp://" + httpStreamingURL);
                ext = "mp3";
            } else {
                /* 2022-01-24: Website uses MPD, we use HLS */
                // mpd master: https://croaod.cz/stream/<someHash>.m4a/manifest.mpd
                // hls master: https://croaod.cz/stream/<someHash>.m4a/master.m3u8
                // hls chunklist (usually there is only one quality available): https://croaod.cz/stream/<someHash>.m4a/chunklist.m3u8
                link = this.createDownloadlink(GenericM3u8.createURLForThisPlugin("https://croaod.cz/stream/" + mpdStreamingHash + ".m4a/chunklist.m3u8"));
                ext = "m4a";
            }
            link.setFinalFileName(trackNumber + "." + trackTitle + "." + ext);
            link.setAvailable(true);
            ret.add(link);
        }
        /* 2022-02-28 */
        final String json = br.getRegex("data-player='(\\{.*?\\})'").getMatch(0);
        if (json != null) {
            final Map<String, Object> entries = JSonStorage.restoreFromString(json, TypeRef.HASHMAP);
            final Map<String, Object> data = (Map<String, Object>) entries.get("data");
            final List<Map<String, Object>> playlist = (List<Map<String, Object>>) data.get("playlist");
            for (final Map<String, Object> audio : playlist) {
                final String trackTitle = (String) audio.get("title");
                // final long duration = ((Number) audio.get("duration")).longValue();
                final List<Map<String, Object>> audioLinks = (List<Map<String, Object>>) audio.get("audioLinks");
                if (audioLinks.isEmpty()) {
                    logger.warning("Failed to find any downloadurls for item : " + trackTitle);
                }
                final ArrayList<String> validAudioLinks = new ArrayList<String>();
                for (final Map<String, Object> audioInfo : audioLinks) {
                    final String url = audioInfo.get("url").toString();
                    // final Number bitrate = (Number) audioInfo.get("bitrate");
                    /* 2022-03-01: Skip DASH streams */
                    if (url.endsWith(".mpd")) {
                        continue;
                    } else {
                        validAudioLinks.add(url);
                    }
                }
                if (audioLinks.isEmpty()) {
                    logger.warning("Failed to find any valid downloadurls for item: " + trackTitle);
                    continue;
                }
                int index = 0;
                for (final String validAudioLink : validAudioLinks) {
                    final boolean isHLSStreaming = validAudioLink.contains(".m3u8");
                    final DownloadLink link = this.createDownloadlink(validAudioLink);
                    final String ext;
                    if (isHLSStreaming) {
                        ext = ".m4a";
                    } else {
                        ext = ".mp3";
                    }
                    if (validAudioLinks.size() > 1) {
                        link.setFinalFileName(trackTitle + "_" + (index + 1) + ext);
                    } else {
                        link.setFinalFileName(trackTitle + ext);
                    }
                    /*
                     * Do not set availablestatus for HLS items otherwise they won't be processed by the generic HLS crawler and this won't
                     * appear in the linkgrabber.
                     */
                    if (!isHLSStreaming) {
                        link.setAvailable(true);
                    }
                    ret.add(link);
                    index++;
                }
            }
        }
        /*
         * Look for single audio podcast e.g.:
         * https://dvojka.rozhlas.cz/radost-mi-dela-pohled-z-terasy-pripadam-si-jako-knezna-libuse-omeletky-o-radosti-8689401
         */
        final String directurl = br.getRegex("\"(https?://dvojka\\.rozhlas\\.cz/sites/default/files/audios/[a-f0-9]+\\.mp3[^\"]*)\"").getMatch(0);
        if (directurl != null) {
            final DownloadLink dl = this.createDownloadlink(directurl);
            dl.setFinalFileName(title + ".mp3");
            dl.setAvailable(true);
            ret.add(dl);
        }
        final FilePackage fp = FilePackage.getInstance();
        fp.setName(title);
        fp.addLinks(ret);
        return ret;
    }
}
