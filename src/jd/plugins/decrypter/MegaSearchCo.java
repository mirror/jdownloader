package jd.plugins.decrypter;

import java.util.ArrayList;

import jd.PluginWrapper;
import jd.controlling.ProgressController;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;

import org.appwork.utils.Regex;
import org.jdownloader.plugins.components.antiDDoSForDecrypt;

@DecrypterPlugin(revision = "$Revision: 34719 $", interfaceVersion = 2, names = { "megasearch.co" }, urls = { "https?://megasearch\\.co/link/\\d+-[a-zA-Z0-9\\-]+" })
public class MegaSearchCo extends antiDDoSForDecrypt {
    public MegaSearchCo(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public ArrayList<DownloadLink> decryptIt(CryptedLink parameter, ProgressController progress) throws Exception {
        final String id = new Regex(parameter.getCryptedUrl(), "/link/(\\d+)").getMatch(0);
        final String rest = new Regex(parameter.getCryptedUrl(), "/link/\\d+-(.+)").getMatch(0);
        if (id == null || rest == null) {
            return null;
        }
        getPage(parameter.getCryptedUrl());
        br.setFollowRedirects(false);
        getPage("http://megasearch.co/goto?id=" + id + "&slug=" + rest);
        if (br.getRedirectLocation() != null) {
            final ArrayList<DownloadLink> ret = new ArrayList<DownloadLink>();
            ret.add(createDownloadlink(br.getRedirectLocation()));
            return ret;
        } else {
            return null;
        }
    }
}