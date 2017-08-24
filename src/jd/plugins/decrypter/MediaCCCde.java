package jd.plugins.decrypter;

import java.util.ArrayList;

import jd.PluginWrapper;
import jd.controlling.ProgressController;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;

import org.appwork.utils.StringUtils;
import org.appwork.utils.formatter.SizeFormatter;
import org.jdownloader.plugins.components.antiDDoSForDecrypt;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 3, names = { "media.ccc.de" }, urls = { "https?://media.ccc.de/(v|c)/.+" })
public class MediaCCCde extends antiDDoSForDecrypt {
    public MediaCCCde(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public ArrayList<DownloadLink> decryptIt(CryptedLink parameter, ProgressController progress) throws Exception {
        br.setFollowRedirects(true);
        getPage(parameter.getCryptedUrl());
        final String title = br.getRegex("<meta content='([^']*?)'\\s*property='og:title'>").getMatch(0);
        final ArrayList<DownloadLink> ret = new ArrayList<DownloadLink>();
        if (StringUtils.containsIgnoreCase(parameter.getCryptedUrl(), "/v/")) {
            final String videoURLs[][] = br.getRegex("class='[^']*download[^']*'\\s*href='(https?://.*?)'>(.*?div class='size'>(.*?)<)?").getMatches();
            if (videoURLs != null) {
                for (String videoURL[] : videoURLs) {
                    final DownloadLink link = createDownloadlink("directhttp://" + videoURL[0]);
                    if (videoURL.length == 3 && videoURL[2] != null) {
                        link.setDownloadSize(SizeFormatter.getSize(videoURL[2]));
                    }
                    link.setAvailable(true);
                    ret.add(link);
                }
            }
            if (title != null) {
                FilePackage fp = FilePackage.getInstance();
                fp.setName(title);
                fp.addLinks(ret);
            }
        } else {
            final String videoURLs[][] = br.getRegex("<a href='(/v/.*?)'>").getMatches();
            if (videoURLs != null) {
                for (String videoURL[] : videoURLs) {
                    final DownloadLink link = createDownloadlink(br.getURL(videoURL[0]).toString());
                    ret.add(link);
                }
            }
        }
        return ret;
    }
}
