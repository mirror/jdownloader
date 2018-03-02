package jd.plugins.decrypter;

import java.util.ArrayList;
import java.util.List;

import jd.PluginWrapper;
import jd.controlling.ProgressController;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.plugins.PluginForDecrypt;

import org.appwork.utils.Regex;
import org.jdownloader.plugins.components.hls.HlsContainer;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "areena.yle.fi" }, urls = { "https?://(?:www\\.)?areena\\.yle\\.fi/\\d+-\\d+" })
public class AreenaYleFi extends PluginForDecrypt {
    public AreenaYleFi(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public ArrayList<DownloadLink> decryptIt(CryptedLink parameter, ProgressController progress) throws Exception {
        final ArrayList<DownloadLink> ret = new ArrayList<DownloadLink>();
        br.setFollowRedirects(true);
        br.getPage(parameter.getCryptedUrl());
        final String id = new Regex(parameter.getCryptedUrl(), "-(\\d+)$").getMatch(0);
        String downloadURL = br.getRegex("<a\\s*class\\s*=\\s*\"[^\"]*?icon-download[^\"]*?\"\\s*href\\s*=\\s*\"(https?://.*?)\"").getMatch(0);
        if (downloadURL == null) {
            downloadURL = br.getRegex("<link\\s*itemprop\\s*=\\s*\"contentUrl\"\\s*href\\s*=\\s*\"(https?://.*?)\"").getMatch(0);
        }
        String name = br.getRegex("<meta\\s*itemprop\\s*=\\s*\"name\"\\s*content\\s*=\\s*\"(.*?)\"").getMatch(0);
        if (name == null) {
            name = id;
        }
        final String videoUrl = br.getRegex("<meta\\s*property\\s*=\\s*\"og:video:url\"\\s*content\\s*=\\s*\"(.*?)\"").getMatch(0);
        if (videoUrl != null) {
            br.getPage(videoUrl);
            String dataUrl = br.getRegex("\"dataUrl\"\\s*:\\s*\"(https?.*?)\"").getMatch(0);
            if (dataUrl != null) {
                dataUrl = dataUrl.replaceAll("\\\\", "");
                if (downloadURL == null) {
                    downloadURL = dataUrl;
                }
                final String flavorIds[] = br.getRegex("\"containerFormat\"\\s*:\\s*\"isom\",.*?\"id\"\\s*:\\s*\"(.*?)\"").getColumn(0);
                String m3u8 = dataUrl.replaceAll("/url/", "/applehttp/") + (dataUrl.equals("/") ? "a.m3u8" : "/a.m3u8");
                final StringBuilder sb = new StringBuilder();
                for (String flavorId : flavorIds) {
                    if (sb.length() > 0) {
                        sb.append(",");
                    }
                    sb.append(flavorId);
                }
                if (sb.length() > 0) {
                    m3u8 = m3u8.replaceAll("/format/", "/flavorIds/" + sb + "/format/");
                }
                final List<HlsContainer> hls = HlsContainer.getHlsQualities(br.cloneBrowser(), m3u8);
                if (hls != null) {
                    for (HlsContainer container : hls) {
                        final String res = container.getResolution();
                        final int bandw = container.getBandwidth();
                        final DownloadLink dl = createDownloadlink("m3u8" + container.getDownloadurl().substring(4));
                        final String filename = name + "-" + res + "_" + bandw + "_" + container.getFileExtension();
                        dl.setFinalFileName(filename);
                        dl.setContentUrl(parameter.getCryptedUrl());
                        final String linkID = getHost() + "//" + id + "/hls/" + res + "/" + bandw;
                        dl.setLinkID(linkID);
                        dl.setAvailable(true);
                        ret.add(dl);
                    }
                }
            }
        }
        if (downloadURL != null) {
            final DownloadLink downloadLink = createDownloadlink("directhttp://" + downloadURL);
            downloadLink.setFinalFileName(name + "_download.mp4");
            downloadLink.setContentUrl(parameter.getCryptedUrl());
            final String linkID = getHost() + "//" + id + "/download";
            downloadLink.setLinkID(linkID);
            ret.add(downloadLink);
        }
        final FilePackage fp = FilePackage.getInstance();
        fp.setName(name);
        fp.addLinks(ret);
        return ret;
    }
}
