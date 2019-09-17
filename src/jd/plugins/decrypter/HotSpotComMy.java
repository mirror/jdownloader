package jd.plugins.decrypter;

import java.util.ArrayList;
import java.util.Map;

import jd.PluginWrapper;
import jd.controlling.ProgressController;
import jd.nutils.SimpleFTP;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;

import org.appwork.storage.JSonStorage;
import org.appwork.storage.TypeRef;
import org.appwork.utils.Regex;
import org.jdownloader.plugins.components.antiDDoSForDecrypt;
import org.jdownloader.scripting.JavaScriptEngineFactory;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 3, names = { "hotspot.com.my" }, urls = { "https?://(www\\.)?hotspot\\.com\\.my/newsvideo/(\\d+)/(\\d+)/.+" })
public class HotSpotComMy extends antiDDoSForDecrypt {
    public HotSpotComMy(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public ArrayList<DownloadLink> decryptIt(CryptedLink parameter, ProgressController progress) throws Exception {
        br.setCurrentURL(parameter.getCryptedUrl());
        final String videoID = new Regex(parameter.getCryptedUrl(), "/\\d+/(\\d+)").getMatch(0);
        final String urlCoded = new Regex(parameter.getCryptedUrl(), "/\\d+/\\d+/(.+)").getMatch(0);
        getPage("https://api.vod.astro.com.my/rest/media/" + videoID + "/smil?output=json&key=hotspot&applicationalias=JWPlayer&callback=AP.AP" + System.currentTimeMillis());
        final String jsonString = br.toString().replaceFirst("AP\\.AP\\d+\\(", "").replaceFirst("\\)$", "");
        final Map<String, Object> map = JSonStorage.restoreFromString(jsonString, TypeRef.HASHMAP);
        final String m3u8 = (String) JavaScriptEngineFactory.walkJson(map, "smil/body/switch/{0}/m3u8/@src");
        if (m3u8 == null) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        } else {
            getPage(m3u8);
            final String name;
            if (urlCoded != null) {
                name = SimpleFTP.BestEncodingGuessingURLDecode(urlCoded);
            } else {
                name = null;
            }
            final ArrayList<DownloadLink> ret = GenericM3u8Decrypter.parseM3U8(this, parameter.getCryptedUrl(), br, parameter.getCryptedUrl(), null, null, name);
            if (name != null) {
                final FilePackage fp = FilePackage.getInstance();
                fp.setName(name);
                fp.addLinks(ret);
            }
            return ret;
        }
    }
}
