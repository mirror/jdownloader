package jd.plugins.decrypter;

import java.util.ArrayList;
import java.util.List;

import jd.PluginWrapper;
import jd.controlling.ProgressController;
import jd.parser.Regex;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForDecrypt;
import jd.plugins.hoster.GenericM3u8;

import org.appwork.utils.StringUtils;
import org.appwork.utils.encoding.URLEncode;
import org.jdownloader.plugins.components.hls.HlsContainer;
import org.jdownloader.plugins.components.hls.HlsContainer.CODEC_TYPE;
import org.jdownloader.plugins.components.hls.HlsContainer.StreamCodec;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "streaming.media.azure.net" }, urls = { "https?://[^/]+/([a-f0-9]{8}-[a-f0-9]{4}-[a-f0-9]{4}-[a-f0-9]{4}-[a-f0-9]{12})/([^/\\?]+)\\.ism" })
public class AzureMediaService extends PluginForDecrypt {
    public AzureMediaService(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public ArrayList<DownloadLink> decryptIt(CryptedLink parameter, ProgressController progress) throws Exception {
        // https://docs.microsoft.com/en-us/azure/media-services/previous/media-services-deliver-content-overview
        final String url = parameter.getCryptedUrl() + "/manifest(format=m3u8-aapl-v3)";
        final String name = StringUtils.trim(URLEncode.decodeURIComponent(new Regex(url, getSupportedLinks()).getMatch(1)));
        br.getPage(url);
        final List<HlsContainer> hls = HlsContainer.getHlsQualities(br);
        if (hls.size() == 0) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        } else {
            final ArrayList<DownloadLink> ret = new ArrayList<DownloadLink>();
            for (HlsContainer container : hls) {
                final String m3u8URL = container.getStreamURL().replaceFirst("^http", "m3u8");
                final DownloadLink downloadLink = createDownloadlink(m3u8URL);
                if (name != null) {
                    final StringBuilder details = new StringBuilder();
                    if (container.getHeight() > 0) {
                        details.append(container.getHeight()).append("p");
                    }
                    final StreamCodec audio = container.getCodecType(CODEC_TYPE.AUDIO);
                    if (audio != null) {
                        if (details.length() > 0) {
                            details.append("_");
                        }
                        details.append(audio.getCodec().getCodecName());
                    }
                    if (details.length() > 0) {
                        downloadLink.setName(name + " (" + details.toString().trim() + ")" + container.getFileExtension());
                    }
                }
                downloadLink.setProperty(GenericM3u8.PRESET_NAME_PROPERTY, name);
                downloadLink.setAvailable(true);
                ret.add(downloadLink);
            }
            return ret;
        }
    }
}
