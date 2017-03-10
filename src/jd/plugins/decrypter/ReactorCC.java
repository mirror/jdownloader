package jd.plugins.decrypter;

import java.util.ArrayList;
import java.util.HashSet;

import jd.PluginWrapper;
import jd.controlling.ProgressController;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.PluginForDecrypt;

@DecrypterPlugin(revision = "$Revision: 34675 $", interfaceVersion = 2, names = { "reactor.cc" }, urls = { "https?://([^/]*?)?reactor\\.cc/post/\\d+" })
public class ReactorCC extends PluginForDecrypt {

    public ReactorCC(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public ArrayList<DownloadLink> decryptIt(CryptedLink parameter, ProgressController progress) throws Exception {
        br.setFollowRedirects(true);
        br.getPage(parameter.getCryptedUrl());
        final String reactor[] = br.getRegex("(https?://[^/]*?reactor\\.cc/pics/post/full/.*?(jpe?g|png|gif))").getColumn(0);
        if (reactor == null || reactor.length == 0) {
            return null;
        }
        final ArrayList<DownloadLink> ret = new ArrayList<DownloadLink>();
        final HashSet<String> dups = new HashSet<String>();
        for (String url : reactor) {
            if (dups.add(url)) {
                ret.add(createDownloadlink("directhttp://" + url));
            }
        }
        return ret;
    }

}
