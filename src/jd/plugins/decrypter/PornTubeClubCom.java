package jd.plugins.decrypter;

import java.util.ArrayList;

import jd.PluginWrapper;
import jd.controlling.ProgressController;
import jd.parser.Regex;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.hoster.DirectHTTP;

import org.jdownloader.plugins.components.antiDDoSForDecrypt;

@DecrypterPlugin(revision = "$Revision: 39909 $", interfaceVersion = 3, names = { "porn-tube-club.com" }, urls = { "https?://porn-tube-club.com/(v\\d+/\\d+|play/\\d+)" })
public class PornTubeClubCom extends antiDDoSForDecrypt {
    public PornTubeClubCom(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        final ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        br.setFollowRedirects(true);
        getPage(param.toString());
        final String title = br.getRegex("\"og:title\"\\s*content\\s*=\\s*\"\\s*(.*?)\\s*\"").getMatch(0);
        final String video = br.getRegex("\"og:video\"\\s*content\\s*=\\s*\"\\s*(https?://.*?\\.mp4)\\s*\"").getMatch(0);
        if (video != null) {
            final DownloadLink downloadLink = createDownloadlink(video);
            downloadLink.setProperty("Referer", param.getCryptedUrl());
            if (title != null) {
                downloadLink.setFinalFileName(title + ".mp4");
                downloadLink.setProperty(DirectHTTP.FIXNAME, downloadLink.getFinalFileName());
            }
            final String access = new Regex(video, "(\\d+)\\.mp4").getMatch(0);
            if (access != null) {
                downloadLink.setProperty("cookies", "access=" + access);
            }
            downloadLink.setContentUrl(param.getCryptedUrl());
            decryptedLinks.add(downloadLink);
        }
        return decryptedLinks;
    }
}