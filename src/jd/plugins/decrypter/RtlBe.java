package jd.plugins.decrypter;

import java.util.ArrayList;
import java.util.List;

import jd.PluginWrapper;
import jd.controlling.ProgressController;
import jd.http.Browser;
import jd.nutils.encoding.Encoding;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.plugins.PluginForDecrypt;

import org.appwork.utils.Regex;
import org.jdownloader.plugins.components.hls.HlsContainer;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "rtl.be" }, urls = { "https?://(?:www\\.)?rtl\\.be/info/video/\\d+" })
public class RtlBe extends PluginForDecrypt {
    public RtlBe(final PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public ArrayList<DownloadLink> decryptIt(CryptedLink parameter, ProgressController progress) throws Exception {
        final ArrayList<DownloadLink> ret = new ArrayList<DownloadLink>();
        final String id = new Regex(parameter.getCryptedUrl(), "/(\\d+)").getMatch(0);
        br.getPage("http://www.rtl.be/videos/vpo_video.asmx?action=getinfos&id=" + id);
        final String title = br.getRegex("<Title>\\s*(.*?)\\s*<").getMatch(0);
        final String m3u8 = br.getRegex("<URL_UNICORN>\\s*(https?://.*?)\\s*<").getMatch(0);
        if (m3u8 != null) {
            final Browser brc = br.cloneBrowser();
            brc.getPage(m3u8);
            brc.followRedirect();
            final List<HlsContainer> allHlsContainers = HlsContainer.getHlsQualities(brc);
            for (final HlsContainer hlscontainer : allHlsContainers) {
                final DownloadLink link = new DownloadLink(null, null, getHost(), "m3u8" + hlscontainer.getDownloadurl().substring(4), true);
                link.setProperty("preSetName", title);
                if (hlscontainer.getBandwidth() > 0) {
                    link.setProperty("hlsBandwidth", hlscontainer.getBandwidth());
                }
                ret.add(link);
            }
        }
        final String mp4 = br.getRegex("<URL_MP4>\\s*(https?://.*?\\.mp4)\\s*<").getMatch(0);
        if (mp4 != null) {
            final DownloadLink link = createDownloadlink(mp4);
            if (title != null) {
                link.setFinalFileName(title + ".mp4");
            }
            ret.add(link);
        }
        final String f4m = br.getRegex("<URL>\\s*(https?://.*?\\.f4m.*?)\\s*<").getMatch(0);
        if (f4m != null) {
            final Browser brc = br.cloneBrowser();
            ret.addAll(GenericF4MDecrypter.parse(this, brc, f4m, title, null, null));
        }
        if (title != null) {
            FilePackage fp = FilePackage.getInstance();
            fp.setName(Encoding.htmlDecode(title));
            fp.addLinks(ret);
        }
        return ret;
    }
}
