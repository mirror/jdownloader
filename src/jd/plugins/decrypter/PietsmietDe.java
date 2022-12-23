package jd.plugins.decrypter;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import jd.PluginWrapper;
import jd.controlling.ProgressController;
import jd.http.Browser;
import jd.http.requests.GetRequest;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForDecrypt;

import org.appwork.storage.TypeRef;
import org.appwork.utils.Regex;
import org.appwork.utils.StringUtils;
import org.appwork.utils.encoding.Base64;
import org.jdownloader.scripting.JavaScriptEngineFactory;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 3, names = { "pietsmiet.de" }, urls = { "https?://(www\\.)?pietsmiet\\.de/videos/(\\d+)" })
public class PietsmietDe extends PluginForDecrypt {
    public PietsmietDe(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public ArrayList<DownloadLink> decryptIt(CryptedLink parameter, ProgressController progress) throws Exception {
        br.setFollowRedirects(true);
        br.getPage(parameter.getCryptedUrl());
        final String X_Origin_Integrity = Base64.decodeToString(br.getRegex("\"v\"\\s*:\\s*\"(.*?)\"").getMatch(0));
        if (X_Origin_Integrity == null) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        Browser brc = br.cloneBrowser();
        final String episode = new Regex(parameter.getCryptedUrl(), "/(\\d+)").getMatch(0);
        final GetRequest info = brc.createGetRequest("https://www.pietsmiet.de/api/v1/videos/" + episode + "?include[]=playlist");
        info.getHeaders().put("X-Origin-Integrity", X_Origin_Integrity);
        info.getHeaders().put("X-Requested-With", "XMLHttpRequest");
        brc.getPage(info);
        final Map<String, Object> infoResponse = restoreFromString(info.getHtmlCode(), TypeRef.MAP);
        final String title = (String) JavaScriptEngineFactory.walkJson(infoResponse, "video/slug");
        final FilePackage fp = FilePackage.getInstance();
        fp.setName(episode + "-" + StringUtils.valueOrEmpty(title));
        final GetRequest playlist = brc.createGetRequest("https://www.pietsmiet.de/api/v1/utility/player?video=" + episode + "&preset=quality");
        playlist.getHeaders().put("X-Origin-Integrity", X_Origin_Integrity);
        playlist.getHeaders().put("X-Requested-With", "XMLHttpRequest");
        brc.getPage(playlist);
        final Map<String, Object> playlistResponse = restoreFromString(playlist.getHtmlCode(), TypeRef.MAP);
        final List<Map<String, Object>> tracks = (List<Map<String, Object>>) JavaScriptEngineFactory.walkJson(playlistResponse, "options/tracks");
        final ArrayList<DownloadLink> ret = new ArrayList<DownloadLink>();
        for (Map<String, Object> track : tracks) {
            final String full_title = (String) track.get("full_title");
            final String m3u8 = (String) JavaScriptEngineFactory.walkJson(track, "sources/hls/src");
            if (m3u8 != null) {
                brc = br.cloneBrowser();
                brc.getPage(m3u8);
                final ArrayList<DownloadLink> trackLinks = GenericM3u8Decrypter.parseM3U8(this, m3u8, brc, null, null, full_title);
                if (trackLinks.size() > 1) {
                    for (DownloadLink link : trackLinks) {
                        link.setContentUrl(parameter.getCryptedUrl());
                        ret.add(link);
                        fp.add(link);
                    }
                }
            }
        }
        return ret;
    }
}
