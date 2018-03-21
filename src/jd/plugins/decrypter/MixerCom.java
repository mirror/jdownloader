package jd.plugins.decrypter;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import jd.PluginWrapper;
import jd.controlling.ProgressController;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForDecrypt;

import org.appwork.storage.JSonStorage;
import org.appwork.storage.TypeRef;
import org.appwork.utils.Regex;
import org.appwork.utils.StringUtils;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 3, names = { "mixer.com" }, urls = { "https?://(?:www\\.)?mixer\\.com/[^/]*\\?vod=\\d+" })
public class MixerCom extends PluginForDecrypt {
    public MixerCom(final PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public ArrayList<DownloadLink> decryptIt(CryptedLink parameter, ProgressController progress) throws Exception {
        final ArrayList<DownloadLink> ret = new ArrayList<DownloadLink>();
        final String vodID = new Regex(parameter.getCryptedUrl(), "vod=(\\d+)").getMatch(0);
        br.getPage("https://mixer.com/api/v1/recordings/" + vodID + "?noCount=1");
        final Map<String, Object> response = JSonStorage.restoreFromString(br.toString(), TypeRef.HASHMAP);
        final String state = (String) response.get("state");
        if ("AVAILABLE".equals(state)) {
            final String name = (String) response.get("name");
            final String token = (String) ((Map<String, Object>) response.get("channel")).get("token");
            final List<Map<String, Object>> vods = (List<Map<String, Object>>) response.get("vods");
            if (vods != null) {
                for (Map<String, Object> vod : vods) {
                    final String format = (String) vod.get("format");
                    final String baseUrl = (String) vod.get("baseUrl");
                    if (StringUtils.equalsIgnoreCase(format, "hls") && baseUrl != null) {
                        final DownloadLink link = createDownloadlink((baseUrl + (baseUrl.endsWith("/") ? "manifest.m3u8" : "/manifest.m3u8")).replaceFirst("^http", "m3u8"));
                        link.setName(token + "-" + name);
                        link.setContainerUrl(parameter.getCryptedUrl());
                        ret.add(link);
                    }
                }
            } else {
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
        }
        return ret;
    }
}
