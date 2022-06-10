package jd.plugins.decrypter;

import java.util.ArrayList;
import java.util.List;

import org.jdownloader.plugins.components.antiDDoSForDecrypt;

import jd.PluginWrapper;
import jd.controlling.ProgressController;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = {}, urls = {})
public class ThirteenDlNet extends antiDDoSForDecrypt {
    public ThirteenDlNet(PluginWrapper wrapper) {
        super(wrapper);
    }

    public static List<String[]> getPluginDomains() {
        final List<String[]> ret = new ArrayList<String[]>();
        // each entry in List<String[]> will result in one PluginForDecrypt, Plugin.getHost() will return String[0]->main domain
        ret.add(new String[] { "13dl.net", "13dl.link", "13dl.to" });
        return ret;
    }

    public static String[] getAnnotationNames() {
        return buildAnnotationNames(getPluginDomains());
    }

    @Override
    public String[] siteSupportedNames() {
        return buildSupportedNames(getPluginDomains());
    }

    public static String[] getAnnotationUrls() {
        return buildAnnotationUrls(getPluginDomains());
    }

    public static String[] buildAnnotationUrls(final List<String[]> pluginDomains) {
        final List<String> ret = new ArrayList<String>();
        for (final String[] domains : pluginDomains) {
            ret.add("https?://(?:www\\.)?" + buildHostsPatternPart(domains) + "/wp/(link/.*?url=.+|zip/.*?url=.+)");
        }
        return ret.toArray(new String[0]);
    }

    @Override
    public ArrayList<DownloadLink> decryptIt(final CryptedLink parameter, ProgressController progress) throws Exception {
        getPage(parameter.getCryptedUrl());
        int loop = 4;
        while (loop >= 0) {
            final String redirect = br.getRedirectLocation();
            if (redirect != null) {
                if (redirect.matches(getMatcher().pattern().pattern())) {
                    getPage(redirect);
                    loop--;
                } else {
                    break;
                }
            } else {
                break;
            }
        }
        final String redirect = br.getRedirectLocation();
        if (redirect != null) {
            if (!redirect.matches(getMatcher().pattern().pattern())) {
                final ArrayList<DownloadLink> ret = new ArrayList<DownloadLink>();
                ret.add(createDownloadlink(redirect));
                return ret;
            }
        }
        return null;
    }
}
