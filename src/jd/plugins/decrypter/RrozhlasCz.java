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

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 3, names = { "rozhlas.cz" }, urls = { "https?://(?:[a-z0-9]+\\.)?rozhlas\\.cz/([a-z0-9\\-]+)\\-(\\d+)" })
public class RrozhlasCz extends PluginForDecrypt {
    public RrozhlasCz(PluginWrapper wrapper) {
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
                link = this.createDownloadlink("m3u8s://croaod.cz/stream/" + mpdStreamingHash + ".m4a/chunklist.m3u8");
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
                int index = 0;
                for (final Map<String, Object> audioInfo : audioLinks) {
                    // final Number bitrate = (Number) audioInfo.get("bitrate");
                    final DownloadLink link = this.createDownloadlink(audioInfo.get("url").toString());
                    if (audioLinks.size() > 1) {
                        link.setFinalFileName(trackTitle + "_" + (index + 1) + ".mp3");
                    } else {
                        link.setFinalFileName(trackTitle + ".mp3");
                    }
                    link.setAvailable(true);
                    ret.add(link);
                    index++;
                }
            }
        }
        final FilePackage fp = FilePackage.getInstance();
        fp.setName(title);
        fp.addLinks(ret);
        return ret;
    }
}
