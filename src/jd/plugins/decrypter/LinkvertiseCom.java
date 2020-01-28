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
import java.util.LinkedHashMap;
import java.util.Random;
import java.util.regex.Pattern;

import org.appwork.utils.StringUtils;
import org.jdownloader.plugins.components.antiDDoSForDecrypt;
import org.jdownloader.scripting.JavaScriptEngineFactory;

import jd.PluginWrapper;
import jd.controlling.ProgressController;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.components.PluginJSonUtils;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 3, names = {}, urls = {})
public class LinkvertiseCom extends antiDDoSForDecrypt {
    public LinkvertiseCom(PluginWrapper wrapper) {
        super(wrapper);
    }

    private static final String[] domains = { "linkvertise.com", "linkvertise.net", "link-to.net", "up-to-down.net", "direct-link.net" };

    /**
     * returns the annotation pattern array
     *
     */
    public static String[] getAnnotationUrls() {
        // construct pattern
        final String host = getHostsPattern();
        return new String[] { host + "/(\\d+)/([^/]+)" };
    }

    private static String getHostsPattern() {
        final StringBuilder pattern = new StringBuilder();
        for (final String name : domains) {
            pattern.append((pattern.length() > 0 ? "|" : "") + Pattern.quote(name));
        }
        final String hosts = "https?://(?:www\\.)?" + "(?:" + pattern.toString() + ")";
        return hosts;
    }

    /**
     * Returns the annotations names array
     *
     * @return
     */
    public static String[] getAnnotationNames() {
        return new String[] { domains[0] };
    }

    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        final ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        /* 2020-01-28: TODO: Check how to process such URLs: https://linkvertise.com/premium-redirect/1234567 */
        final String parameter = param.toString();
        final String id = new Regex(parameter, this.getSupportedLinks()).getMatch(0);
        final String target_provider = new Regex(parameter, this.getSupportedLinks()).getMatch(1);
        // getPage(parameter);
        /* See also here: https://github.com/timmyRS/Universal-Bypass/commit/85552c699fd2da89c295d054969f760146bb2728 */
        final String api_host = "linkvertise.net";
        br.getHeaders().put("origin", "https://" + this.getHost());
        br.setAllowedResponseCodes(new int[] { 500 });
        this.getPage(String.format("https://%s/api/v1/redirect/link/static/%s/%s?origin=", api_host, id, target_provider));
        if (br.getHttpConnection().getResponseCode() == 404 || br.getHttpConnection().getResponseCode() == 500) {
            decryptedLinks.add(this.createOfflinelink(parameter));
            return decryptedLinks;
        }
        // this.getPage("https://linkvertise.net/api/v1/account");
        LinkedHashMap<String, Object> entries = (LinkedHashMap<String, Object>) JavaScriptEngineFactory.jsonToJavaMap(br.toString());
        entries = (LinkedHashMap<String, Object>) JavaScriptEngineFactory.walkJson(entries, "data/link");
        final long internal_id = JavaScriptEngineFactory.toLong(entries.get("id"), 0);
        if (internal_id == 0) {
            return null;
        }
        int random = new Random().nextInt(500000);
        /* 2020-01-28: Hmm does not seem random, seems to be hardcoded */
        random = 375123;
        final String final_json = String.format("{\"timestamp\":%d,\"random\":\"%d\",\"link_id\":%d}", System.currentTimeMillis(), 375123, internal_id);
        final String final_json_b64 = Encoding.Base64Encode(final_json);
        final String url = String.format("/api/v1/redirect/link/%s/%s/target?serial=%s", id, target_provider, Encoding.urlEncode(final_json_b64));
        /* 2020-01-28: TODO: Make this work */
        this.getPage(url);
        final String finallink = PluginJSonUtils.getJson(br, "target");
        if (StringUtils.isEmpty(finallink) || !finallink.startsWith("http")) {
            logger.warning("Decrypter broken for link: " + parameter);
            return null;
        }
        decryptedLinks.add(createDownloadlink(finallink));
        return decryptedLinks;
    }
}
