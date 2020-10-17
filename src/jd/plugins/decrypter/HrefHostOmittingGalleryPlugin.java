package jd.plugins.decrypter;

import java.util.ArrayList;
import java.util.List;

import jd.PluginWrapper;
import jd.plugins.DecrypterPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;

/**
 * A plugin for downloading JPGs via href links from plain HTML. These links are relative to the host.
 */
@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = {}, urls = {})
public class HrefHostOmittingGalleryPlugin extends SimpleHtmlBasedGalleryPlugin {
    public HrefHostOmittingGalleryPlugin(PluginWrapper wrapper) {
        super(wrapper);
    }

    public static List<String[]> getSupportedSites() {
        final List<String[]> ret = new ArrayList<String[]>();
        ret.add(new String[] { "sexhd.pics", "https?://(?:www\\.)?sexhd\\.pics/gallery/.+" });
        ret.add(new String[] { "xxxporn.pics", "https?://(?:www\\.)?xxxporn\\.pics/sex/.+" });
        return ret;
    }

    public static String[] getAnnotationNames() {
        final List<String> ret = new ArrayList<String>();
        for (final String[] supportedSite : getSupportedSites()) {
            ret.add(supportedSite[0]);
        }
        return ret.toArray(new String[0]);
    }

    @Override
    public String[] siteSupportedNames() {
        return new String[] { getHost() };
    }

    public static String[] getAnnotationUrls() {
        final List<String> ret = new ArrayList<String>();
        for (final String[] supportedSite : getSupportedSites()) {
            ret.add(supportedSite[1]);
        }
        return ret.toArray(new String[0]);
    }

    protected String[] determineLinks(String url) throws PluginException {
        final String[] links = br.getRegex("href\\s*=\\s*(?:\"|')([^\"']+\\.jpg)(?:\"|')").getColumn(0);
        if (links == null || links.length == 0) {
            logger.warning("found 0 images for " + url);
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        } else {
            String host = br.getHost();
            String protocol = "http://";
            if (url.startsWith("https://")) {
                protocol = "https://";
            }
            String[] linksWithHost = new String[links.length];
            for (int i = 0; i < links.length; i++) {
                linksWithHost[i] = protocol + host + links[i];
            }
            return linksWithHost;
        }
    }
}
