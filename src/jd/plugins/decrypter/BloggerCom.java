package jd.plugins.decrypter;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.appwork.storage.TypeRef;

import jd.PluginWrapper;
import jd.controlling.ProgressController;
import jd.http.Browser;
import jd.nutils.encoding.Encoding;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForDecrypt;
import jd.plugins.hoster.VideoGoogle;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "blogger.com" }, urls = { "https?://([a-z0-9\\-]+\\.)?blogger\\.com/video\\.g\\?token=[a-zA-Z0-9\\-_]+" })
public class BloggerCom extends PluginForDecrypt {
    public BloggerCom(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public Browser createNewBrowserInstance() {
        final Browser br = super.createNewBrowserInstance();
        VideoGoogle.prepBR(br);
        br.setFollowRedirects(true);
        return br;
    }

    @Override
    public ArrayList<DownloadLink> decryptIt(final CryptedLink param, ProgressController progress) throws Exception {
        br.getPage(param.getCryptedUrl());
        if (br.getHttpConnection().getResponseCode() == 404) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        final String json = br.getRegex("var VIDEO_CONFIG = (\\{.+\\})").getMatch(0);
        final Map<String, Object> entries = restoreFromString(json, TypeRef.MAP);
        final List<Map<String, Object>> streams = (List<Map<String, Object>>) entries.get("streams");
        final String title = entries.get("iframe_id").toString();
        final String extDefault = ".mp4";
        final ArrayList<DownloadLink> ret = new ArrayList<DownloadLink>();
        for (final Map<String, Object> stream : streams) {
            final String play_url = stream.get("play_url").toString();
            final String format_id = stream.get("format_id").toString();
            final DownloadLink video = createDownloadlink(Encoding.unicodeDecode(play_url));
            if (streams.size() == 1) {
                video.setFinalFileName(title + extDefault);
            } else {
                video.setFinalFileName(title + "_" + format_id + extDefault);
            }
            ret.add(video);
        }
        return ret;
    }
}
