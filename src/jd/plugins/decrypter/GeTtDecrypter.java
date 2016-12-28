//jDownloader - Downloadmanager
//Copyright (C) 2012  JD-Team support@jdownloader.org
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

import jd.PluginWrapper;
import jd.controlling.ProgressController;
import jd.parser.Regex;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.PluginForDecrypt;

import org.jdownloader.scripting.JavaScriptEngineFactory;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "ge.tt" }, urls = { "https?://(?:www\\.)?ge\\.tt/(?!api)#?[A-Za-z0-9]+(?:/v/\\d+)?" })
public class GeTtDecrypter extends PluginForDecrypt {

    public GeTtDecrypter(PluginWrapper wrapper) {
        super(wrapper);
    }

    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        final ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        br.setFollowRedirects(true);
        final String parameter = param.toString().replace("#", "");

        if (parameter.matches("https?://(?:www\\.)?ge\\.tt/(?:developers|press|tools|notifications|blog|about|javascript|button|contact|terms|api|m).*?")) {
            decryptedLinks.add(this.createOfflinelink(parameter));
            return decryptedLinks;
        }

        br.setAllowedResponseCodes(410);
        br.getPage(parameter);

        if (this.br.getHttpConnection().getResponseCode() == 404 || this.br.getHttpConnection().getResponseCode() == 410 || br.containsHTML("Page not found|The page you were looking for was not found|Files removed|These files have been removed by the owner")) {
            decryptedLinks.add(this.createOfflinelink(parameter));
            return decryptedLinks;
        }

        // final String singleFile = new Regex(parameter, "/v/(\\d+)").getMatch(0);
        final String folderid = new Regex(parameter, "ge\\.tt/([A-Za-z0-9]+)(/v/\\d+)?").getMatch(0);
        br.getHeaders().put("Accept", "application/json, text/plain, */*");
        br.getPage("//api.ge.tt/1/shares/" + folderid);

        LinkedHashMap<String, Object> entries = (LinkedHashMap<String, Object>) JavaScriptEngineFactory.jsonToJavaMap(br.toString());
        final String error = (String) entries.get("error");

        if (error != null || br.containsHTML(">404 Not Found<")) {
            final DownloadLink dlink = this.createOfflinelink(parameter);
            decryptedLinks.add(dlink);
            return decryptedLinks;
        }

        final ArrayList<Object> files = (ArrayList<Object>) entries.get("files");

        for (final Object fileo : files) {
            entries = (LinkedHashMap<String, Object>) fileo;
            final String filename = (String) entries.get("filename");
            final String fileid = (String) entries.get("fileid");
            final long filesize = JavaScriptEngineFactory.toLong(entries.get("size"), 0);
            final DownloadLink dl = createDownloadlink("http://proxy.ge.tt/1/files/" + folderid + "/" + fileid + "/blob?download");
            dl.setContentUrl("http://ge.tt/" + folderid);
            dl.setName(filename);
            dl.setDownloadSize(filesize);
            dl.setAvailable(true);
            decryptedLinks.add(dl);
        }

        if (decryptedLinks == null || decryptedLinks.size() == 0) {
            logger.info("ge.tt: Folder is empty! Link: " + parameter);
            final DownloadLink dlink = this.createOfflinelink(parameter);
            decryptedLinks.add(dlink);
            return decryptedLinks;
        }
        return decryptedLinks;
    }

    /* NO OVERRIDE!! */
    public boolean hasCaptcha(CryptedLink link, jd.plugins.Account acc) {
        return false;
    }

}