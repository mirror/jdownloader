package jd.plugins.decrypter;

import java.util.ArrayList;
import java.util.HashSet;

import jd.PluginWrapper;
import jd.controlling.ProgressController;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.PluginForDecrypt;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "reactor.cc" }, urls = { "https?://(?:joy)?reactor\\.cc/post/\\d+" })
public class ReactorCC extends PluginForDecrypt {

    @Override
    public String[] siteSupportedNames() {
        return new String[] { "reactor.cc", "joyreactor.cc" };
    }

    public ReactorCC(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public ArrayList<DownloadLink> decryptIt(CryptedLink parameter, ProgressController progress) throws Exception {
        ArrayList<DownloadLink> ret = null;
        br.setFollowRedirects(true);
        br.getPage(parameter.getCryptedUrl());
        final String reactor[] = br.getRegex("(https?://[^/]*?reactor\\.cc/pics/post/full/.*?(jpe?g|png|gif))").getColumn(0);
        if (reactor == null || reactor.length == 0) {
            // Unsafe content - only for registered users (google translate)
            if (br.containsHTML("Небезопасный контент - только для зарегистрированных пользователей|joyreactor\\.cc/images/unsafe_ru.gif")) {
                ret = new ArrayList<DownloadLink>();
                ret.add(createOfflinelink(parameter.getCryptedUrl(), "Unsafe content - only for registered users"));
            }
            return ret;
        }
        ret = new ArrayList<DownloadLink>();
        final HashSet<String> dups = new HashSet<String>();
        for (String url : reactor) {
            if (dups.add(url)) {
                ret.add(createDownloadlink("directhttp://" + url));
            }
        }
        return ret;
    }

}
