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

import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
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

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 3, names = { "tenlua.vn" }, urls = { "https?://(?:www\\.)?tenlua\\.vn/(fm/folder/[a-f0-9]{18}/[^<>\"]+)" })
public class TenluaVnFolder extends PluginForDecrypt {

    public TenluaVnFolder(PluginWrapper wrapper) {
        super(wrapper);
    }

    private String  folder  = "(?i-)https?://(?:www\\.)?tenlua\\.vn/fm/folder/[a-f0-9]{18}/[^<>\"]+";

    private Browser ajax    = null;
    private long    req_num = 0;

    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        final ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        final String parameter = param.toString().replace("http://", "https://");
        final String fid = new Regex(parameter, "(?:(?:#|/)download|/folder)/?([a-z0-9]+)").getMatch(0);
        if (fid == null) {
            decryptedLinks.add(createOfflinelink(parameter));
            return decryptedLinks;
        }
        br.getPage(parameter);
        // folder
        if (parameter.matches(folder)) {
            // [{"a":"filemanager_gettree","p":"1037e12fe70e6a0115","download":1}]
            postPageRaw("//api2.tenlua.vn/", "[{\"a\":\"filemanager_gettree\",\"p\":\"" + fid + "\",\"download\":1}]");
            handleFolder(decryptedLinks, parameter);
        }
        return decryptedLinks;
    }

    private void handleFolder(final ArrayList<DownloadLink> decryptedLinks, final String parameter) throws Exception {
        String fpName = null;
        ArrayList<Object> array = (ArrayList) JavaScriptEngineFactory.jsonToJavaObject(ajax.toString());
        array = (ArrayList) JavaScriptEngineFactory.walkJson(array, "{0}/f");
        for (int i = 0; i != array.size(); i++) {
            final LinkedHashMap<String, Object> entries = (LinkedHashMap<String, Object>) array.get(i);
            if (i == 0) {
                // about this folder
                fpName = (String) entries.get("n");
            } else {
                // entries in this folder
                final String hash = (String) entries.get("h");
                final String name = (String) entries.get("n");
                final String ns = (String) entries.get("ns");
                final String size = (String) entries.get("s");
                final DownloadLink dl = createDownloadlink(Request.getLocation("/download/" + hash + "/" + ns, br.getRequest()));
                dl.setDownloadSize(Long.parseLong(size));
                dl.setName(name);
                dl.setAvailable(true);
                decryptedLinks.add(dl);
            }
        }
        final FilePackage fp = FilePackage.getInstance();
        fp.setName(fpName);
        fp.addLinks(decryptedLinks);

    }

    private void postPageRaw(final String url, final String postData) throws IOException {
        ajax = this.br.cloneBrowser();
        if (req_num == 0) {
            req_num = (long) Math.ceil(Math.random() * 1000000000);
        } else {
            req_num++;
        }
        ajax.getHeaders().put("TENLUA-Chrome-Antileak", "/?id=" + req_num);
        ajax.getHeaders().put("Accept", "application/json, text/plain, */*");
        ajax.postPageRaw(url, postData);
    }
}
