package jd.plugins.decrypter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.appwork.utils.Regex;
import org.appwork.utils.StringUtils;

import jd.PluginWrapper;
import jd.plugins.DecrypterPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;

/**
 * Downloads single galleries and all per model from elitebabes.com. Re-uses functionality from SimpleHtmlBasedGalleryPlugin, but has
 * specific functionality to get the gallery urls.
 */
@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = {}, urls = {})
public class EliteBabesCom extends SimpleHtmlBasedGalleryPlugin {

    private static final SiteData SITE_DATA = new SiteData("elitebabes.com", "/(?!model/).+", "/model/.+", null);

    public EliteBabesCom(PluginWrapper wrapper) {
        super(wrapper);
    }

    public static List<String[]> getSupportedSites() {
        final List<String[]> ret = new ArrayList<String[]>();
        ret.add(new String[] { SITE_DATA.host, SITE_DATA.getUrlRegex() });
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

    @Override
    protected List<SiteData> getSiteData() {
        ArrayList<SiteData> siteData = new ArrayList<SiteData>();
        siteData.add(SITE_DATA);
        return siteData;
    }

    protected String[] getGalleryUrls(String galleryHrefRegex) throws PluginException {
        String[][] listItems = br.getRegex("<li>(.*?)</li>").getMatches();
        if (listItems.length == 0) {
            return new String[0];
        }
        ArrayList<String> urls = new ArrayList<String>(listItems.length);
        for (String[] listItem : listItems) {
            try {
                if (!listItem[0].contains("title")) {
                    continue;
                }
                String galleryUrl = new Regex(listItem[0], "href\\s*=\\s*(?:\"|')([^\"']+)(?:\"|')").getMatch(0);
                if (StringUtils.isEmpty(galleryUrl)) {
                    throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT, "no gallery match found");
                }
                urls.add(br.getURL(galleryUrl).toString());
            } catch (IOException e) {
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT, null, e);
            }
        }
        return urls.toArray(new String[0]);
    }
}
