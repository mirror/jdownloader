package jd.plugins.decrypter;

import java.util.ArrayList;
import java.util.List;

import org.appwork.utils.Regex;
import org.jdownloader.plugins.components.hls.HlsContainer;

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
import jd.plugins.hoster.GenericM3u8;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "rtl.be" }, urls = { "https?://(?:www\\.)?rtl\\.be/.*/video/(\\d+)(\\.aspx)?" })
public class RtlBe extends PluginForDecrypt {
    public RtlBe(final PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public ArrayList<DownloadLink> decryptIt(final CryptedLink param, ProgressController progress) throws Exception {
        final ArrayList<DownloadLink> ret = new ArrayList<DownloadLink>();
        br.setFollowRedirects(true);
        br.getPage(param.getCryptedUrl());
        if (br.getHttpConnection().getResponseCode() == 404) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        String title = br.getRegex("<Title>\\s*(.*?)\\s*<").getMatch(0);
        String m3u8 = br.getRegex("\"contentUrl\":\\s*\"(https?://[^\"]+)\"").getMatch(0);
        if (m3u8 == null) {
            /* Old handling */
            final String id = new Regex(param.getCryptedUrl(), this.getSupportedLinks()).getMatch(0);
            br.getPage("https://www.rtl.be/videos/vpo_video.asmx?action=getinfos&id=" + id);
            if (br.getHttpConnection().getResponseCode() == 404) {
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            }
            if (title == null) {
                title = br.getRegex("<Title>\\s*(.*?)\\s*<").getMatch(0);
            }
            title = correctTitle(title);
            m3u8 = br.getRegex("<URL_UNICORN>\\s*(https?://.*?)\\s*<").getMatch(0);
            final String mp3 = br.getRegex("<URL>\\s*(https?://.*?\\.mp3)\\s*<").getMatch(0);
            if (mp3 != null) {
                final DownloadLink link = createDownloadlink(mp3);
                if (title != null) {
                    link.setFinalFileName(title + ".mp3");
                }
                ret.add(link);
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
        }
        if (m3u8 != null) {
            final Browser brc = br.cloneBrowser();
            brc.getPage(m3u8);
            final List<HlsContainer> allHlsContainers = HlsContainer.getHlsQualities(brc);
            for (final HlsContainer hlscontainer : allHlsContainers) {
                final DownloadLink link = new DownloadLink(null, null, getHost(), "m3u8" + hlscontainer.getDownloadurl().substring(4), true);
                link.setProperty(GenericM3u8.PRESET_NAME_PROPERTY, title);
                if (hlscontainer.getBandwidth() > 0) {
                    link.setProperty(GenericM3u8.PROPERTY_BANDWIDTH, hlscontainer.getBandwidth());
                }
                ret.add(link);
            }
        }
        if (ret.isEmpty()) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        final FilePackage fp = FilePackage.getInstance();
        if (title != null) {
            title = correctTitle(title);
            fp.setName(title);
        }
        for (final DownloadLink link : ret) {
            link._setFilePackage(fp);
            link.setContentUrl(param.getCryptedUrl());
        }
        return ret;
    }

    private String correctTitle(String title) {
        if (title == null) {
            return null;
        }
        title = Encoding.htmlDecode(title).trim();
        return title;
    }
}
