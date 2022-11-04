package jd.plugins.decrypter;

import java.util.ArrayList;
import java.util.Map;

import jd.PluginWrapper;
import jd.controlling.ProgressController;
import jd.http.Browser;
import jd.http.requests.GetRequest;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.plugins.PluginForDecrypt;

import org.appwork.storage.TypeRef;
import org.appwork.utils.Regex;
import org.appwork.utils.StringUtils;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 3, names = { "gronkh.tv" }, urls = { "https?://(www\\.)?gronkh\\.tv/watch/stream/\\d+" })
public class GronkhTv extends PluginForDecrypt {
    public GronkhTv(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public ArrayList<DownloadLink> decryptIt(CryptedLink parameter, ProgressController progress) throws Exception {
        final String episode = new Regex(parameter.getCryptedUrl(), "/stream/(\\d+)").getMatch(0);
        final GetRequest info = br.createGetRequest("https://api.gronkh.tv/v1/video/info?episode=" + episode);
        info.getHeaders().put("Origin", "https://www.gronkh.tv");
        info.getHeaders().put("Referer", "https://www.gronkh.tv");
        br.getPage(info);
        final Map<String, Object> infoResponse = restoreFromString(info.getHtmlCode(), TypeRef.HASHMAP);
        final String title = (String) infoResponse.get("title");
        final GetRequest playlist = br.createGetRequest("https://api.gronkh.tv/v1/video/playlist?episode=" + episode);
        info.getHeaders().put("Origin", "https://www.gronkh.tv");
        info.getHeaders().put("Referer", "https://www.gronkh.tv");
        br.getPage(playlist);
        final Map<String, Object> playlistResponse = restoreFromString(playlist.getHtmlCode(), TypeRef.HASHMAP);
        final String m3u8 = (String) playlistResponse.get("playlist_url");
        // TODO: chat_replay support, multiple json requests
        if (StringUtils.isEmpty(title) || StringUtils.isEmpty(m3u8)) {
            return new ArrayList<DownloadLink>();
        } else {
            final Browser brc = br.cloneBrowser();
            brc.getPage(m3u8);
            final ArrayList<DownloadLink> ret = GenericM3u8Decrypter.parseM3U8(this, m3u8, brc, null, null, title);
            if (ret.size() > 1) {
                for (DownloadLink link : ret) {
                    link.setContentUrl(parameter.getCryptedUrl());
                }
                final FilePackage fp = FilePackage.getInstance();
                fp.setName(title);
                fp.addLinks(ret);
            }
            return ret;
        }
    }
}
