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
import java.util.List;

import org.appwork.utils.StringUtils;
import org.appwork.utils.encoding.URLEncode;
import org.jdownloader.scripting.JavaScriptEngineFactory;

import jd.PluginWrapper;
import jd.controlling.ProgressController;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.plugins.PluginForDecrypt;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 3, names = {}, urls = {})
public class GoogleDriveDirectoryIndex extends PluginForDecrypt {
    public GoogleDriveDirectoryIndex(PluginWrapper wrapper) {
        super(wrapper);
    }

    /* This is a selfhosted thing: https://github.com/donwa/goindex */
    public static List<String[]> getPluginDomains() {
        final List<String[]> ret = new ArrayList<String[]>();
        // each entry in List<String[]> will result in one PluginForDecrypt, Plugin.getHost() will return String[0]->main domain
        /* Most times it's index.workers.dev */
        ret.add(new String[] { "workers.dev" });
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
            ret.add("https?://(?:[a-z0-9\\-\\.]+\\.)?" + buildHostsPatternPart(domains) + "/(.+)");
        }
        return ret.toArray(new String[0]);
    }

    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        final ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        String parameter = param.toString();
        if (parameter.contains("?")) {
            /* Remove all parameters */
            parameter = parameter.substring(0, parameter.lastIndexOf("?"));
        }
        final String urlname = new Regex(parameter, this.getSupportedLinks()).getMatch(0);
        br.setAllowedResponseCodes(new int[] { 500 });
        br.getHeaders().put("x-requested-with", "XMLHttpRequest");
        br.postPageRaw(parameter, "{\"password\":\"null\"}");
        if (br.getHttpConnection().getResponseCode() == 404 || br.getHttpConnection().getResponseCode() == 500) {
            decryptedLinks.add(this.createOfflinelink(parameter));
            return decryptedLinks;
        }
        LinkedHashMap<String, Object> entries = (LinkedHashMap<String, Object>) JavaScriptEngineFactory.jsonToJavaMap(br.toString());
        final Object filesO = entries.get("files");
        final ArrayList<Object> ressourcelist;
        if (filesO != null) {
            /* Multiple files */
            ressourcelist = (ArrayList<Object>) entries.get("files");
        } else {
            /* Probably single file */
            ressourcelist = new ArrayList<Object>();
            ressourcelist.add(entries);
        }
        for (final Object fileO : ressourcelist) {
            entries = (LinkedHashMap<String, Object>) fileO;
            final String name = (String) entries.get("name");
            final String type = (String) entries.get("mimeType");
            final long filesize = JavaScriptEngineFactory.toLong(entries.get("size"), -1);
            if (StringUtils.isEmpty(name) || StringUtils.isEmpty(type)) {
                /* Skip invalid objects */
                continue;
            }
            String url = parameter;
            /* Filename is alrerady in URL if we have a single URL! */
            if (!url.endsWith(name)) {
                if (!url.endsWith("/")) {
                    url += "/";
                }
                url += URLEncode.encodeURIComponent(name);
                url += "/";
            }
            final DownloadLink dl;
            if (type.contains("folder")) {
                dl = this.createDownloadlink(url);
                decryptedLinks.add(dl);
            } else {
                dl = this.createDownloadlink("directhttp://" + url);
                dl.setAvailable(true);
                dl.setFinalFileName(name);
                if (filesize > 0) {
                    dl.setDownloadSize(filesize);
                }
                decryptedLinks.add(dl);
            }
        }
        final FilePackage fp = FilePackage.getInstance();
        fp.setName(Encoding.htmlDecode(urlname));
        fp.addLinks(decryptedLinks);
        return decryptedLinks;
    }
}
