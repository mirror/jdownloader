package jd.plugins.decrypter;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.appwork.storage.TypeRef;
import org.jdownloader.plugins.controller.LazyPlugin;

import jd.PluginWrapper;
import jd.controlling.ProgressController;
import jd.nutils.encoding.Encoding;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForDecrypt;
import jd.plugins.hoster.DirectHTTP;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 3, names = {}, urls = {})
public class GermanTVChannelMediathek extends PluginForDecrypt {
    public GermanTVChannelMediathek(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public LazyPlugin.FEATURE[] getFeatures() {
        return new LazyPlugin.FEATURE[] { LazyPlugin.FEATURE.VIDEO_STREAMING };
    }

    @Override
    public ArrayList<DownloadLink> decryptIt(CryptedLink parameter, ProgressController progress) throws Exception {
        br.setFollowRedirects(true);
        br.getPage(parameter.getCryptedUrl());
        if (br.getHttpConnection().getResponseCode() == 404) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        final String json = br.getRegex("\"application/ld\\+json\"\\s*>\\s*(\\{.*?\\})\\s*</script>").getMatch(0);
        if (json == null) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        final Map<String, Object> map = restoreFromString(json, TypeRef.MAP);
        final DownloadLink link = createDownloadlink("directhttp://" + (String) map.get("contentUrl"), false);
        link.setFinalFileName(Encoding.htmlDecode((String) map.get("name")).trim() + ".mp4");
        final String customHost = br.getHost(true).replaceFirst("(?i)www.", "");
        link.setProperty(DirectHTTP.PROPERTY_CUSTOM_HOST, customHost);
        link.setContentUrl(br.getURL());
        link.setAvailable(true);
        final ArrayList<DownloadLink> ret = new ArrayList<DownloadLink>();
        ret.add(link);
        return ret;
    }

    public static List<String[]> getPluginDomains() {
        final List<String[]> ret = new ArrayList<String[]>();
        // each entry in List<String[]> will result in one PluginForDecrypt, Plugin.getHost() will return String[0]->main domain
        ret.add(new String[] { "frankenfernsehen.tv" });
        ret.add(new String[] { "baden-tv.com" });
        ret.add(new String[] { "muenchen.tv" });
        ret.add(new String[] { "tvingolstadt.de" });
        ret.add(new String[] { "niederbayerntv.de" });
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
            // final String annotationName = domains[0];
            ret.add("https?://(?:[a-z0-9]+\\.)?" + buildHostsPatternPart(domains) + "/mediathek/video/[^/]*(-\\d{2}-\\d{2}-\\d{4}|-\\d+|$|/)");
        }
        return ret.toArray(new String[0]);
    }
}
