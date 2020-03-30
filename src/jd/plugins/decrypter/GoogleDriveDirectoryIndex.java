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

import org.appwork.utils.StringUtils;
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

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 3, names = { "index.gd.workers.dev" }, urls = { "https?://index\\.gd\\.workers\\.dev/(.+)" })
public class GoogleDriveDirectoryIndex extends PluginForDecrypt {
    public GoogleDriveDirectoryIndex(PluginWrapper wrapper) {
        super(wrapper);
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
            final long filesize = JavaScriptEngineFactory.toLong(entries.get("size"), -1);
            if (StringUtils.isEmpty(name)) {
                /* Skip invalid objects */
                continue;
            }
            String url = parameter;
            /* Filename is alrerady in URL if we have a single URL! */
            if (!parameter.endsWith(name)) {
                if (!parameter.endsWith("/")) {
                    url += "/";
                }
                url += name;
            }
            final DownloadLink dl = this.createDownloadlink("directhttp://" + url);
            dl.setAvailable(true);
            dl.setFinalFileName(name);
            if (filesize > 0) {
                dl.setDownloadSize(filesize);
            }
            decryptedLinks.add(dl);
        }
        final FilePackage fp = FilePackage.getInstance();
        fp.setName(Encoding.htmlDecode(urlname));
        fp.addLinks(decryptedLinks);
        return decryptedLinks;
    }
}
