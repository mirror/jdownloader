package jd.plugins.decrypter;

import java.util.ArrayList;

import jd.PluginWrapper;
import jd.controlling.ProgressController;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;

import org.appwork.utils.Regex;
import org.jdownloader.plugins.components.antiDDoSForDecrypt;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 3, names = { "artstation.com" }, urls = { "https?://[a-z0-9\\-]+\\.artstation\\.com/(projects/[A-Z0-9]+|$)" })
public class ArtistArtstationCom extends antiDDoSForDecrypt {
    public ArtistArtstationCom(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public ArrayList<DownloadLink> decryptIt(CryptedLink parameter, ProgressController progress) throws Exception {
        final String artwork = new Regex(parameter.getCryptedUrl(), "/projects/([A-Z0-9]+)").getMatch(0);
        final ArrayList<DownloadLink> ret = new ArrayList<DownloadLink>();
        if (artwork != null) {
            ret.add(createDownloadlink("https://www.artstation.com/artwork/" + artwork));
        } else {
            br.setFollowRedirects(true);
            getPage(parameter.getCryptedUrl());
            final String[] projects = br.getRegex("/projects/([A-Z0-9]+)").getColumn(0);
            for (final String project : projects) {
                ret.add(createDownloadlink("https://www.artstation.com/artwork/" + project));
            }
        }
        return ret;
    }
}
