package jd.plugins.decrypter;

import java.util.ArrayList;

import jd.PluginWrapper;
import jd.controlling.ProgressController;
import jd.parser.Regex;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForDecrypt;

import org.appwork.utils.StringUtils;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "mega.co.nz" }, urls = { "https?://(www\\.)?mega\\.(co\\.)?nz/(file|folder)/[a-zA-Z0-9]+(#|%23)[a-zA-Z0-9_,\\-%]{16,}(/(folder|file)/[a-zA-Z0-9]+)?" })
public class RewriteMegaConz extends PluginForDecrypt {
    public RewriteMegaConz(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public ArrayList<DownloadLink> decryptIt(CryptedLink parameter, ProgressController progress) throws Exception {
        final ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        // TODO: update pattern in hoster/decrypter plugin to support this new url format
        final String parts[] = new Regex(parameter.getCryptedUrl(), "(file|folder)/([a-zA-Z0-9]+)(?:#|%23)([a-zA-Z0-9_,\\-%]{16,})(/(folder|file)/([a-zA-Z0-9]+))?").getRow(0);
        final DownloadLink link;
        if (StringUtils.equals("file", parts[0])) {
            link = createDownloadlink("https://mega.nz/#!" + parts[1] + "!" + parts[2]);
        } else if (StringUtils.equals("folder", parts[0])) {
            if (parts.length == 6 && StringUtils.isNotEmpty(parts[5])) {
                if (StringUtils.equals("file", parts[4])) {
                    link = createDownloadlink("https://mega.nz/#F!" + parts[1] + "!" + parts[2] + "!" + parts[5]);
                } else if (StringUtils.equals("folder", parts[4])) {
                    link = createDownloadlink("https://mega.nz/#F!" + parts[1] + "!" + parts[2] + "!" + parts[5]);
                } else {
                    throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                }
            } else {
                link = createDownloadlink("https://mega.nz/#F!" + parts[1] + "!" + parts[2]);
            }
        } else {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        decryptedLinks.add(link);
        return decryptedLinks;
    }
}
