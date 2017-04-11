package jd.plugins.decrypter;

import java.util.ArrayList;

import jd.PluginWrapper;
import jd.controlling.ProgressController;
import jd.parser.html.HTMLParser;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.PluginForDecrypt;

import org.jdownloader.plugins.components.config.SlexyOrgPluginConfig;
import org.jdownloader.plugins.config.PluginConfigInterface;
import org.jdownloader.plugins.config.PluginJsonConfig;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "slexy.org" }, urls = { "http://(www\\.)?slexy\\.org/(?:view|raw)/[0-9A-Za-z]+" })
public class SlexyOrg extends PluginForDecrypt {

    public SlexyOrg(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public Class<? extends PluginConfigInterface> getConfigInterface() {
        return SlexyOrgPluginConfig.class;
    }

    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        final ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        final SlexyOrgPluginConfig.MODE mode = PluginJsonConfig.get(SlexyOrgPluginConfig.class).getMode();
        final String parameter = param.toString();
        if (!SlexyOrgPluginConfig.MODE.CRAWL.equals(mode)) {
            final DownloadLink link = createDownloadlink("directhttp://" + parameter.replaceAll("/view/", "/raw/"));
            link.setProperty("Referer", parameter.replaceAll("/raw/", "/view"));
            decryptedLinks.add(link);
        }
        if (!SlexyOrgPluginConfig.MODE.DOWNLOAD.equals(mode)) {
            br.setFollowRedirects(true);
            br.setCurrentURL(parameter.replaceAll("/raw/", "/view"));
            br.setLoadLimit(16 * 1024 * 1024);
            br.getPage(parameter.replaceAll("/view/", "/raw/"));
            if (br.getURL().contains("/msg/")) {
                return decryptedLinks;
            }
            final String[] urls = HTMLParser.getHttpLinks(br.getRequest().getHtmlCode(), "");
            if (urls != null) {
                for (final String url : urls) {
                    final DownloadLink link = createDownloadlink(url);
                    decryptedLinks.add(link);
                }
            }
        }
        return decryptedLinks;
    }

    /* NO OVERRIDE!! */
    public boolean hasCaptcha(CryptedLink link, jd.plugins.Account acc) {
        return false;
    }
}
