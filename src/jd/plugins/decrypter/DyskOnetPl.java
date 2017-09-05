//    jDownloader - Downloadmanager
//    Copyright (C) 2009  JD-Team support@jdownloader.org
//
//    This program is free software: you can redistribute it and/or modify
//    it under the terms of the GNU General Public License as published by
//    the Free Software Foundation, either version 3 of the License, or
//    (at your option) any later version.
//
//    This program is distributed in the hope that it will be useful,
//    but WITHOUT ANY WARRANTY; without even the implied warranty of
//    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
//    GNU General Public License for more details.
//
//    You should have received a copy of the GNU General Public License
//    along with this program.  If not, see <http://www.gnu.org/licenses/>.
package jd.plugins.decrypter;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map;

import jd.PluginWrapper;
import jd.controlling.ProgressController;
import jd.http.Browser;
import jd.parser.Regex;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.plugins.PluginForDecrypt;

import org.jdownloader.scripting.JavaScriptEngineFactory;

/**
 *
 * @author raztoki
 */
@DecrypterPlugin(revision = "$Revision: 20515 $", interfaceVersion = 3, names = { "dysk.onet.pl" }, urls = { "https?://(?:www\\.)?dysk\\.onet\\.pl/(?:multilink/[a-f0-9]{40}|link/[a-zA-Z0-9]{5})" })
public class DyskOnetPl extends PluginForDecrypt {
    public DyskOnetPl(PluginWrapper wrapper) {
        super(wrapper);
    }

    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        final ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        final String parameter = param.toString();
        br.setFollowRedirects(true);
        br.getPage(parameter);
        /* Error handling */
        if (br.getHttpConnection() == null || br.getHttpConnection().getResponseCode() == 404) {
            decryptedLinks.add(this.createOfflinelink(parameter));
            return decryptedLinks;
        }
        {
            Browser test = br.cloneBrowser();
            test.getHeaders().put("Accept", "*/*");
            test.getPage("//events.onet.pl/v2/me?_ac=events");
            // should set ea_uuid session cookie, but meh
        }
        final String fuid = getFuid(parameter);
        // json
        final Browser ajax = br.cloneBrowser();
        ajax.getHeaders().put("Accept", "application/json");
        ajax.getHeaders().put("Timestamp-Format", "unix");
        ajax.postPage("/api/manager/?linkinfo", "Link=" + fuid);
        LinkedHashMap<String, Object> response = (LinkedHashMap<String, Object>) JavaScriptEngineFactory.jsonToJavaObject(ajax.toString());
        response = (LinkedHashMap<String, Object>) response.get("response");
        if ((String) response.get("error") != null && "Public link not found.".equals(response.get("message"))) {
            decryptedLinks.add(createOfflinelink(parameter, fuid, null));
            return decryptedLinks;
        }
        final String description = (String) response.get("description");
        final String fpName = fuid; // description will be too long.
        final LinkedHashMap<String, Object> elements = (LinkedHashMap<String, Object>) response.get("elements");
        for (final Map.Entry<String, Object> entry : elements.entrySet()) {
            final String key = entry.getKey();
            if (key != null && key.matches("\\d+")) {
                final LinkedHashMap<String, Object> object = fuid.length() == 5 ? (LinkedHashMap<String, Object>) entry.getValue() : (LinkedHashMap<String, Object>) JavaScriptEngineFactory.walkJson(entry.getValue(), "0");
                final String type = (String) object.get("type");
                final String name = (String) object.get("displayName");
                final String size = (String) object.get("size");
                final String hash = (String) object.get("hash");
                if ("file".equals(type)) {
                    final DownloadLink dl = createDownloadlink("directhttp://https://dysk.onet.pl/api/manager/?thumbnail&link=" + fuid + "&id=" + hash + "&size=F");
                    dl.setVerifiedFileSize(Long.parseLong(size));
                    dl.setName(name);
                    dl.setComment(description);
                    dl.setAvailable(true);
                    decryptedLinks.add(dl);
                }
            }
        }
        final FilePackage fp = FilePackage.getInstance();
        fp.setName(fpName);
        fp.addLinks(decryptedLinks);
        return decryptedLinks;
    }

    private String getFuid(String parameter) {
        String fuid = new Regex(parameter, "/multilink/([a-f0-9]{40})$").getMatch(0);
        if (fuid == null) {
            fuid = new Regex(parameter, "/link/([a-zA-Z0-9]{5})$").getMatch(0);
        }
        return fuid;
    }

    /* NO OVERRIDE!! */
    public boolean hasCaptcha(CryptedLink link, jd.plugins.Account acc) {
        return false;
    }
}