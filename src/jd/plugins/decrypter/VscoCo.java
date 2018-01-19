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
import java.util.List;
import java.util.Map;

import org.jdownloader.scripting.JavaScriptEngineFactory;

import jd.PluginWrapper;
import jd.controlling.ProgressController;
import jd.http.Browser;
import jd.http.Request;
import jd.parser.Regex;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.plugins.PluginForDecrypt;
import jd.plugins.components.PluginJSonUtils;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 3, names = { "vsco.co" }, urls = { "https?://(?:[^/]+\\.vsco\\.co/grid/\\d+|(?:www\\.)?vsco\\.co/[a-zA-Z0-9]+/grid/\\d+|(?:www\\.)?vsco\\.co/\\w+)" })
public class VscoCo extends PluginForDecrypt {
    public VscoCo(PluginWrapper wrapper) {
        super(wrapper);
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        final ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        final String parameter = param.toString();
        final String username = getUsername(parameter);
        br.setCurrentURL("https://vsco.co/" + username + "/images/1");
        br.getPage("https://vsco.co/content/Static/userinfo");
        final String cookie_vs = br.getCookie(this.getHost(), "vs");
        br.getPage("https://vsco.co/ajxp/" + cookie_vs + "/2.0/sites?subdomain=" + username);
        final String siteid = PluginJSonUtils.getJsonValue(br, "id");
        if (cookie_vs == null || siteid == null) {
            return null;
        }
        long amount_total = 0;
        /* More than 500 possible */
        int max_count_per_page = 500;
        int page = 1;
        final FilePackage fp = FilePackage.getInstance();
        fp.setName(username);
        do {
            if (this.isAbort()) {
                logger.info("Decryption aborted by user");
                break;
            }
            final Browser ajax = br.cloneBrowser();
            ajax.getHeaders().put("Accept", "application/json, text/javascript, */*; q=0.01");
            ajax.getHeaders().put("X-Requested-With", "XMLHttpRequest");
            ajax.getPage("/ajxp/" + cookie_vs + "/2.0/medias?site_id=" + siteid + "&page=" + page + "&size=" + max_count_per_page);
            Map<String, Object> entries = (Map<String, Object>) JavaScriptEngineFactory.jsonToJavaObject(ajax.toString());
            if (page == 1 || page > 1) {
                amount_total = JavaScriptEngineFactory.toLong(entries.get("total"), 0);
                if (page == 1 && amount_total == 0) {
                    logger.info("User has zero content!");
                    decryptedLinks.add(this.createOfflinelink(parameter));
                    return decryptedLinks;
                } else if (page > 1 && amount_total == 0) {
                    return decryptedLinks;
                }
            }
            final List<Object> resources = (List) entries.get("media");
            for (final Object resource : resources) {
                entries = (Map<String, Object>) resource;
                final String fid = (String) entries.get("_id");
                if (fid == null) {
                    return null;
                }
                final String medialink = (String) entries.get("permalink");
                String url_content = (String) entries.get("responsive_url");
                if (!(url_content.startsWith("http") || url_content.startsWith("//"))) {
                    url_content = Request.getLocation("//" + url_content, br.getRequest());
                }
                final String filename = username + "_" + fid + getFileNameExtensionFromString(url_content, ".jpg");
                final DownloadLink dl = this.createDownloadlink("directhttp://" + url_content);
                dl.setContentUrl(medialink);
                dl.setName(filename);
                dl.setAvailable(true);
                decryptedLinks.add(dl);
                fp.add(dl);
                distribute(dl);
            }
            if (resources.size() < max_count_per_page) {
                /* Fail safe */
                break;
            }
            page++;
        } while (decryptedLinks.size() < amount_total);
        if (decryptedLinks.size() == 0 && !this.isAbort()) {
            return null;
        }
        return decryptedLinks;
    }

    private String getUsername(final String parameter) {
        final String username = new Regex(parameter, "https?://([^/]+)\\.vsco\\.co/").getMatch(0);
        if (username == null) {
            return new Regex(parameter, "vsco\\.co/([a-zA-Z0-9]+)").getMatch(0);
        } else {
            return username;
        }
    }
}
