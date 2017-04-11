//jDownloader - Downloadmanager
//Copyright (C) 2016  JD-Team support@jdownloader.org
//
//This program is free software: you can redistribute it and/or modify
//it under the terms of the GNU General Public License as published by
//the Free Software Foundation, either version 3 of the License, or
//(at your option) any later version.
//
//This program is distributed in the hope that it will be useful,
//but WITHOUT ANY WARRANTY; without even the implied warranty of
//MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
//GNU General Public License for more details.
//
//You should have received a copy of the GNU General Public License
//along with this program.  If not, see <http://www.gnu.org/licenses/>.

package jd.plugins.decrypter;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.LinkedHashMap;

import org.jdownloader.scripting.JavaScriptEngineFactory;

import jd.PluginWrapper;
import jd.controlling.ProgressController;
import jd.http.Browser;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.plugins.PluginForDecrypt;

/**
 *
 * don't fk with regex, doesn't support any pages or spanning pages.
 *
 * @author raztoki
 */
@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "thesuperficial.com" }, urls = { "https?://(?:www\\.)?thesuperficial\\.com(?!(/$|/photos/?$|/page/?|/page/\\d+|/photos/(?:hot-bodies|candid|red-carpet|sightings|most-important-people|crap-we-missed)(/?|/\\d+|/page|/page/\\d+)))(?:/[^\\s]+|/photos/[^/]+/[^\\s*]+)" })
public class TheSupCm extends PluginForDecrypt {

    @SuppressWarnings("deprecation")
    public TheSupCm(PluginWrapper wrapper) {
        super(wrapper);
    }

    @SuppressWarnings({ "deprecation", "unchecked" })
    @Override
    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        final ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        br = new Browser();
        // no https
        final String parameter = param.toString().replaceFirst("^https://", "http://");
        br.setFollowRedirects(true);
        br.getPage(parameter);
        // error handling here
        if (br.getHttpConnection().getResponseCode() == 404) {
            decryptedLinks.add(createOfflinelink(parameter));
            return decryptedLinks;
        }
        if (!parameter.matches("https?://(?:www\\.)?thesuperficial\\.com/photos/.+")) {
            // now standard posts can contain galleries under another link
            final String gal = br.getRegex("\"(https?://(?:www\\.)?thesuperficial\\.com/photos/[^/]+/[^\"/]+)\"").getMatch(0);
            if (gal != null) {
                br.getPage(gal);
            } else {
                // return since we found nudda!
                // return decryptedLinks;
            }
        }
        String fpName = null;

        final String filter = br.getRegex("var gallery_data = (.*?\\}\\});").getMatch(0);
        final LinkedHashMap<String, Object> entries = (LinkedHashMap<String, Object>) JavaScriptEngineFactory.jsonToJavaObject(filter);
        final ArrayList<Object> ressourcelist = (ArrayList) JavaScriptEngineFactory.walkJson(entries, "gallery/slides");
        final DecimalFormat df = new DecimalFormat(ressourcelist.size() < 100 ? "00" : "000");
        for (final Object resource : ressourcelist) {
            if (fpName == null) {
                fpName = (String) JavaScriptEngineFactory.walkJson(resource, "post_title");
            }
            final String image = (String) JavaScriptEngineFactory.walkJson(resource, "images/full");
            final String name = (String) JavaScriptEngineFactory.walkJson(resource, "page_title");
            final Integer index = (Integer) JavaScriptEngineFactory.walkJson(resource, "index");
            if (image != null && name != null && index != null) {
                final DownloadLink dl = createDownloadlink("directhttp://" + image.replaceFirst("-compressed\\.", "."));
                dl.setForcedFileName(df.format((index + 1)) + " - " + name.replaceFirst("(?:_?\\d+\\s+Jpg)?\\s*-\\s*\\d+\\s*$", "") + getFileNameExtensionFromString(image, ".jpg"));
                dl.setAvailable(true);
                decryptedLinks.add(dl);
            }
        }

        if (fpName != null) {
            final FilePackage fp = FilePackage.getInstance();
            fp.setName(fpName);
            fp.addLinks(decryptedLinks);
        }
        return decryptedLinks;
    }

}