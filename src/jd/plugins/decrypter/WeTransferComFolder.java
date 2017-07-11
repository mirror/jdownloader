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
import java.util.Map;

import org.appwork.utils.StringUtils;
import org.jdownloader.scripting.JavaScriptEngineFactory;

import jd.PluginWrapper;
import jd.controlling.ProgressController;
import jd.parser.Regex;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.PluginForDecrypt;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 3, names = { "wetransfer.com" }, urls = { "https?://(?:www\\.)?((?:wtrns\\.fr|we\\.tl)/[\\w\\-]+|wetransfer\\.com/downloads/[a-f0-9]{46}/[a-f0-9]{4,12}(?:/[a-f0-9]{46})?)" })
public class WeTransferComFolder extends PluginForDecrypt {

    public WeTransferComFolder(PluginWrapper wrapper) {
        super(wrapper);
    }

    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        final ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        String parameter = param.toString();
        jd.plugins.hoster.WeTransferCom.prepBR(this.br);
        if (parameter.matches("https?://(wtrns\\.fr|we\\.tl)/[\\w\\-]+")) {
            br.setFollowRedirects(false);
            br.getPage(parameter);
            parameter = br.getRedirectLocation();
            if (parameter == null) {
                return null;
            }
            if (parameter.contains("/error")) {
                decryptedLinks.add(this.createOfflinelink(parameter));
                return decryptedLinks;
            }
        }
        final Regex urlregex = new Regex(parameter, "/downloads/([a-f0-9]+)/(?:[a-f0-9]{46}/)?([a-f0-9]+)");
        final String id_main = urlregex.getMatch(0);
        final String security_hash = urlregex.getMatch(1);
        if (security_hash == null || id_main == null) {
            decryptedLinks.add(this.createOfflinelink(parameter));
            return decryptedLinks;
        }
        // Allow redirects for change to https
        br.setFollowRedirects(true);
        br.getPage(parameter);
        if (br.getHttpConnection().getResponseCode() == 410 || br.getHttpConnection().getResponseCode() == 503) {
            decryptedLinks.add(this.createOfflinelink(parameter));
            return decryptedLinks;
        }
        // String recepientID = br.getRegex("data-recipient=\"([a-z0-9]+)\"").getMatch(0);
        // if (recepientID == null) {
        // recepientID = "";
        // }
        final String json = br.getRegex(">\\s*var _preloaded_transfer_\\s*=\\s*(\\{.*?\\});\\s*</script>").getMatch(0);
        Map<String, Object> entries = (Map<String, Object>) JavaScriptEngineFactory.jsonToJavaObject(json);
        if ("invalid".equals(entries.get("state"))) {
            decryptedLinks.add(this.createOfflinelink(parameter));
            return decryptedLinks;
        }
        final ArrayList<Object> ressourcelist = entries.get("files") != null ? (ArrayList<Object>) entries.get("files") : (ArrayList) entries.get("items");
        /* TODO: Handle this case */
        final boolean per_file_download_available = ((Boolean) entries.get("per_file_download_available")).booleanValue();
        /* TODO: Handle this case */
        final boolean password_protected = ((Boolean) entries.get("password_protected")).booleanValue();
        /* E.g. okay would be "downloadable" */
        final String state = (String) entries.get("state");
        for (final Object fileo : ressourcelist) {
            entries = (Map<String, Object>) fileo;
            final String id_single = (String) entries.get("id");
            final String filename = (String) entries.get("name");
            final long filesize = JavaScriptEngineFactory.toLong(entries.get("size"), 0);
            if (StringUtils.isEmpty(id_single) || StringUtils.isEmpty(filename) || filesize == 0) {
                continue;
            }
            final DownloadLink dl = this.createDownloadlink(String.format("http://wetransferdecrypted/%s/%s/%s", id_main, security_hash, id_single));
            dl.setFinalFileName(filename);
            dl.setDownloadSize(filesize);
            dl.setContentUrl(parameter);
            dl.setAvailable(true);
            decryptedLinks.add(dl);
        }
        br.getPage(parameter);
        if (br.getHttpConnection().getResponseCode() == 404) {
            decryptedLinks.add(this.createOfflinelink(parameter));
            return decryptedLinks;
        }
        // String fpName = null;
        // final FilePackage fp = FilePackage.getInstance();
        // fp.setName(Encoding.htmlDecode(fpName.trim()));
        // fp.addLinks(decryptedLinks);
        return decryptedLinks;
    }
}
