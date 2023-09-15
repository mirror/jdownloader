package jd.plugins.decrypter;

import java.util.ArrayList;
import java.util.List;

import org.jdownloader.plugins.controller.LazyPlugin;

import jd.PluginWrapper;
import jd.http.Browser;
import jd.plugins.DecrypterPlugin;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = {}, urls = {})
public class BurningCamelCom extends PornEmbedParser {
    public BurningCamelCom(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public LazyPlugin.FEATURE[] getFeatures() {
        return new LazyPlugin.FEATURE[] { LazyPlugin.FEATURE.XXX };
    }

    public static List<String[]> getPluginDomains() {
        final List<String[]> ret = new ArrayList<String[]>();
        // each entry in List<String[]> will result in one PluginForDecrypt, Plugin.getHost() will return String[0]->main domain
        ret.add(new String[] { "burningcamel.com", "camelstyle.net" });
        return ret;
    }

    @Override
    protected ArrayList<String> getDeadDomains() {
        final ArrayList<String> deadDomains = new ArrayList<String>();
        deadDomains.add("camelstyle.net");
        return deadDomains;
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
            ret.add("https?://(?:www\\.)?" + buildHostsPatternPart(domains) + "/video/([a-z0-9\\-]+(/\\d+)?)");
        }
        return ret.toArray(new String[0]);
    }

    protected boolean isOffline(final Browser br) {
        if (br.getHttpConnection().getResponseCode() == 404) {
            return true;
        } else if (!this.canHandle(br.getURL())) {
            return true;
        } else {
            return false;
        }
    }

    @Override
    protected boolean isSelfhosted(final Browser br) {
        if (jd.plugins.hoster.BurningCamelCom.findDirectUrl(br) != null) {
            return true;
        } else {
            return false;
        }
    }

    public static final String getContentURL(final String fid) {
        return "https://" + getPluginDomains().get(0)[0] + "/video/" + fid;
    }
}
