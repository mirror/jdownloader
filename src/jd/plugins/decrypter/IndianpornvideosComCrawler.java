package jd.plugins.decrypter;

import java.util.ArrayList;
import java.util.List;

import org.jdownloader.plugins.controller.LazyPlugin;

import jd.PluginWrapper;
import jd.http.Browser;
import jd.parser.Regex;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = {}, urls = {})
public class IndianpornvideosComCrawler extends PornEmbedParser {
    public IndianpornvideosComCrawler(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public LazyPlugin.FEATURE[] getFeatures() {
        return new LazyPlugin.FEATURE[] { LazyPlugin.FEATURE.XXX };
    }

    public static List<String[]> getPluginDomains() {
        final List<String[]> ret = new ArrayList<String[]>();
        // each entry in List<String[]> will result in one PluginForDecrypt, Plugin.getHost() will return String[0]->main domain
        ret.add(new String[] { "indianpornvideos.com", "indianpornvideos2.com" });
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
            ret.add("https?://(?:www\\.)?" + buildHostsPatternPart(domains) + "/(?!account|categories|contact-us|dmca|faq|feed|login|privacy|report-abuse|terms|wp-content|wp-includes|wp-json)(?:video/)?([A-Za-z0-9\\-_]+)(\\.html)?");
        }
        return ret.toArray(new String[0]);
    }

    @Override
    protected String getFileTitle(final CryptedLink param, final Browser br) {
        String titleFromURL = new Regex(param.getCryptedUrl(), this.getSupportedLinks()).getMatch(0);
        if (titleFromURL != null) {
            titleFromURL = titleFromURL.replaceAll("(-|_)", " ").trim();
            return titleFromURL;
        } else {
            return null;
        }
    }

    @Override
    protected boolean isSelfhosted(final Browser br) {
        final String directurl = jd.plugins.hoster.IndianPornVideosCom.findStreamURL(br);
        if (directurl != null) {
            return true;
        } else {
            return false;
        }
    }

    @Override
    protected boolean isOffline(final Browser br) {
        return jd.plugins.hoster.IndianPornVideosCom.isOffline(br);
    }
}
