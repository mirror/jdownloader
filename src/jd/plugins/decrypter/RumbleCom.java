//jDownloader - Downloadmanager
//Copyright (C) 2009  JD-Team support@jdownloader.org
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

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.appwork.storage.JSonStorage;
import org.appwork.storage.TypeRef;
import org.appwork.utils.StringUtils;
import org.jdownloader.scripting.JavaScriptEngineFactory;

import jd.PluginWrapper;
import jd.controlling.ProgressController;
import jd.parser.Regex;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.plugins.PluginForDecrypt;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 3, names = {}, urls = {})
public class RumbleCom extends PluginForDecrypt {
    public RumbleCom(PluginWrapper wrapper) {
        super(wrapper);
    }

    public static List<String[]> getPluginDomains() {
        final List<String[]> ret = new ArrayList<String[]>();
        // each entry in List<String[]> will result in one PluginForDecrypt, Plugin.getHost() will return String[0]->main domain
        ret.add(new String[] { "rumble.com" });
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
            ret.add("https?://(?:www\\.)?" + buildHostsPatternPart(domains) + "/[^/]+\\.html");
        }
        return ret.toArray(new String[0]);
    }

    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        final ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        final String parameter = param.toString();
        br.getPage(parameter);
        final String videoID = br.getRegex("\"video\":\"([a-z0-9]+)\"").getMatch(0);
        if (br.getHttpConnection().getResponseCode() == 404) {
            decryptedLinks.add(this.createOfflinelink(parameter));
            return decryptedLinks;
        } else if (videoID == null) {
            logger.info("Failed to find any downloadable content");
            decryptedLinks.add(this.createOfflinelink(parameter));
            return decryptedLinks;
        }
        br.getPage("/embedJS/u3/?request=video&v=" + videoID);
        if (br.getHttpConnection().getResponseCode() == 404) {
            decryptedLinks.add(this.createOfflinelink(parameter));
            return decryptedLinks;
        }
        Map<String, Object> entries = JSonStorage.restoreFromString(br.toString(), TypeRef.HASHMAP);
        String dateFormatted = null;
        final String dateStr = (String) entries.get("pubDate");
        if (!StringUtils.isEmpty(dateStr)) {
            dateFormatted = new Regex(dateStr, "^(\\d{4}-\\d{2}-\\d{2})").getMatch(0);
        }
        final String uploaderName = (String) JavaScriptEngineFactory.walkJson(entries, "author/name");
        String title = (String) entries.get("title");
        String baseTitle = title;
        if (StringUtils.isEmpty(baseTitle)) {
            /* Fallback */
            baseTitle = videoID;
        }
        if (!StringUtils.isEmpty(uploaderName)) {
            baseTitle = uploaderName + " - " + baseTitle;
        }
        if (dateFormatted != null) {
            baseTitle = dateFormatted + "_" + baseTitle;
        }
        final FilePackage fp = FilePackage.getInstance();
        fp.setName(baseTitle);
        entries = (Map<String, Object>) entries.get("ua");
        final Iterator<Entry<String, Object>> iterator = entries.entrySet().iterator();
        while (iterator.hasNext()) {
            final Entry<String, Object> entry = iterator.next();
            final String qualityModifier = entry.getKey();
            final List<Object> qualityInfoArray = (List<Object>) entry.getValue();
            final String url = (String) qualityInfoArray.get(0);
            if (StringUtils.isEmpty(url) || StringUtils.isEmpty(qualityModifier)) {
                /* Skip invalid items */
                continue;
            }
            final DownloadLink dl = this.createDownloadlink(url);
            dl.setContentUrl(parameter);
            dl.setForcedFileName(baseTitle + "_" + qualityModifier + ".mp4");
            dl.setAvailable(true);
            dl._setFilePackage(fp);
            decryptedLinks.add(dl);
        }
        return decryptedLinks;
    }
}
