package jd.plugins.decrypter;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

import jd.PluginWrapper;
import jd.controlling.ProgressController;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.PluginForDecrypt;

@DecrypterPlugin(revision = "$Revision: 37053 $", interfaceVersion = 3, names = { "rozhlas.cz" }, urls = { "https?://([a-z0-9]+\\.)?rozhlas.cz/.+-\\d+" })
public class RrozhlasCz extends PluginForDecrypt {
    public RrozhlasCz(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public ArrayList<DownloadLink> decryptIt(CryptedLink parameter, ProgressController progress) throws Exception {
        final ArrayList<DownloadLink> ret = new ArrayList<DownloadLink>();
        br.setFollowRedirects(true);
        br.getPage(parameter.getCryptedUrl());
        final String audioIDs[] = br.getRegex("https?://prehravac.rozhlas.cz/audio/(\\d+)").getColumn(0);
        final Set<String> dups = new HashSet<String>();
        final String title = br.getRegex("property=\"og:title\"\\s*content=\"(.*?)\"").getMatch(0);
        if (audioIDs != null) {
            for (final String audioID : audioIDs) {
                final DownloadLink link = createDownloadlink("directhttp://" + br.getURL("//media.rozhlas.cz/_audio/" + audioID + ".mp3").toString());
                if (dups.add(link.getPluginPatternMatcher())) {
                    if (title != null) {
                        link.setFinalFileName(title + ".mp3");
                    }
                    ret.add(link);
                }
            }
        }
        final String mp3s[] = br.getRegex("(https?://(?:[a-z0-9]+\\.)?rozhlas.cz/[^\"]+\\.mp3(\\?[^\"]+)?)").getColumn(0);
        if (mp3s != null) {
            for (final String mp3 : mp3s) {
                final DownloadLink link = createDownloadlink("directhttp://" + mp3);
                if (dups.add(link.getPluginPatternMatcher())) {
                    if (title != null) {
                        link.setFinalFileName(title + ".mp3");
                    }
                    ret.add(link);
                }
            }
        }
        return ret;
    }
}
