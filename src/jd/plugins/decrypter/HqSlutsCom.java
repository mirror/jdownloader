package jd.plugins.decrypter;

import org.appwork.utils.Regex;

import jd.PluginWrapper;
import jd.nutils.encoding.Encoding;
import jd.plugins.DecrypterPlugin;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "hqsluts.com" }, urls = { "https?://(?:www\\.)?hqsluts\\.com/[^/]+-\\d+" })
public class HqSlutsCom extends SimpleHtmlBasedGalleryPlugin {
    public HqSlutsCom(PluginWrapper wrapper) {
        super(wrapper);
    }

    protected String getFilePackageName(String url) {
        String title = br.getRegex("<title>\\s*([^<>]+?)\\s*</title>").getMatch(0);
        String name;
        if (title == null) {
            name = new Regex(url, "hqsluts\\.com/(.+)").getMatch(0);
        } else {
            String id = new Regex(url, "hqsluts\\.com/[^/]+-(\\d+)").getMatch(0);
            name = title + " " + id;
        }
        return name != null ? Encoding.htmlDecode(name.trim()) : null;
    }
}
