package jd.plugins.decrypter;

import java.util.ArrayList;

import jd.PluginWrapper;
import jd.controlling.ProgressController;
import jd.parser.Regex;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.PluginForDecrypt;

import org.appwork.utils.StringUtils;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "mega.co.nz" }, urls = { "https?://(www\\.)?mega\\.(co\\.)?nz/(file|folder)/[a-zA-Z0-9]+(#|%23)[a-zA-Z0-9_,\\-%]{16,}(/folder/[a-zA-Z0-9]+)?" })
public class RewriteMegaConz extends PluginForDecrypt {
    public RewriteMegaConz(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public ArrayList<DownloadLink> decryptIt(CryptedLink parameter, ProgressController progress) throws Exception {
        final ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        // TODO: update pattern in hoster/decrypter plugin to support this new url format
        final String parts[] = new Regex(parameter.getCryptedUrl(), "(file|folder)/([a-zA-Z0-9]+)(?:#|%23)([a-zA-Z0-9_,\\-%]{16,})(/folder/([a-zA-Z0-9]+))?").getRow(0);
        final DownloadLink link;
        if (StringUtils.equals("file", parts[0])) {
            link = createDownloadlink("https://mega.nz/#!" + parts[1] + "!" + parts[2]);
        } else {
            if (parts.length == 5 && StringUtils.isNotEmpty(parts[4])) {
                link = createDownloadlink("https://mega.nz/#F!" + parts[1] + "!" + parts[2] + "!" + parts[4]);
            } else {
                link = createDownloadlink("https://mega.nz/#F!" + parts[1] + "!" + parts[2]);
            }
        }
        decryptedLinks.add(link);
        return decryptedLinks;
    }
}
