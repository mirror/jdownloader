package jd.plugins.decrypter;

import java.io.IOException;
import java.util.ArrayList;

import org.appwork.utils.Regex;
import org.appwork.utils.StringUtils;

import jd.PluginWrapper;
import jd.plugins.DecrypterPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;

//@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "elitebabes.com" }, urls = { "https?://(?:www\\.)?elitebabes\\.com/model/.+" })
public class EliteBabesCom extends SimpleHtmlBasedGalleriesPlugin {

    public EliteBabesCom(PluginWrapper wrapper) {
        super(wrapper);
    }

    protected String[] getGalleryUrls() throws PluginException {
        String[][] listItems = br.getRegex("<li>(.*?)</li>").getMatches();
        ArrayList<String> urls = new ArrayList<String>(listItems.length);
        for (String[] listItem : listItems) {
            try {
                if (!listItem[0].contains("title")) {
                    continue;
                }
                String galleryUrl = new Regex(listItem[0], "href\\s*=\\s*(?:\"|')([^\"']+)(?:\"|')").getMatch(0);
                if (StringUtils.isNotEmpty(galleryUrl)) {
                    urls.add(br.getURL(galleryUrl).toString());
                }
            } catch (IOException e) {
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT, null, e);
            }
        }
        return urls.toArray(new String[0]);
    }
}
